package com.lukachangtong.freight.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.api.message.MessageNotifyPort;
import com.lukachangtong.freight.api.message.NotifyReceiver;
import com.lukachangtong.freight.api.message.ReceiverAddressPort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.message.channel.MessageChannel;
import com.lukachangtong.freight.message.entity.MsgRecord;
import com.lukachangtong.freight.message.entity.MsgTemplate;
import com.lukachangtong.freight.message.mapper.MsgRecordMapper;
import com.lukachangtong.freight.message.mapper.MsgTemplateMapper;
import com.lukachangtong.freight.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息服务实现。
 *
 * 三条关键约束：
 * 1. **幂等**：`dedup_key = bizType:bizKey:channel:receiverType:receiverId`，唯一索引兜底，
 *    同一业务事件重复通知（例如预警重算）不会给司机重复发短信；
 * 2. **不抛异常**：对外是 {@link MessageNotifyPort}，通知失败不能回滚主业务；
 * 3. **失败可重试**：指数退避 1/5/15/60/360 分钟，5 次后置 GIVEN_UP 并保留原因。
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final int MAX_RETRY = 5;
    private static final int[] BACKOFF_MINUTES = {1, 5, 15, 60, 360};
    private static final int TITLE_MAX = 120;
    private static final int CONTENT_MAX = 500;
    private static final int RECEIVER_MAX = 120;
    private static final int DEDUP_MAX = 160;

    private final Map<String, MessageChannel> channels = new HashMap<>();

    @Resource
    private MsgRecordMapper recordMapper;
    @Resource
    private MsgTemplateMapper templateMapper;
    /** 地址解析端口可能没有实现（例如只跑 message 模块单测），用 Provider 避免启动即失败 */
    @Resource
    private ObjectProvider<ReceiverAddressPort> addressPortProvider;

    public MessageServiceImpl(List<MessageChannel> channelList) {
        for (MessageChannel c : channelList) {
            channels.put(c.channel(), c);
        }
    }

    @Override
    public int notify(Long orgId, String bizType, String bizKey, Map<String, Object> vars,
                      List<NotifyReceiver> receivers) {
        try {
            return doNotify(orgId, bizType, bizKey, vars, receivers);
        } catch (Exception e) {
            // 通知是旁路能力：任何异常都不允许冒泡回业务事务
            log.warn("消息通知失败（已忽略，不影响主业务）bizType={} bizKey={} err={}", bizType, bizKey, e.getMessage());
            return 0;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int doNotify(Long orgId, String bizType, String bizKey, Map<String, Object> vars,
                        List<NotifyReceiver> receivers) {
        if (bizType == null || bizType.trim().isEmpty() || bizKey == null || bizKey.trim().isEmpty()) {
            return 0;
        }
        if (receivers == null || receivers.isEmpty()) {
            return 0;
        }
        List<MsgTemplate> templates = resolveTemplates(orgId, bizType);
        if (templates.isEmpty()) {
            return 0;
        }
        Map<String, Object> merged = vars == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<>(vars);
        LocalDateTime now = LocalDateTime.now();
        int created = 0;

        for (MsgTemplate tpl : templates) {
            for (NotifyReceiver receiver : receivers) {
                if (receiver == null || receiver.getType() == null) {
                    continue;
                }
                if (!tpl.getReceiverType().equalsIgnoreCase(receiver.getType())) {
                    continue;
                }
                String address = resolveAddress(tpl.getChannel(), receiver);
                MsgRecord record = new MsgRecord();
                record.setOrgId(orgId);
                record.setBizType(bizType);
                record.setBizKey(bizKey);
                record.setTemplateCode(tpl.getTemplateCode());
                record.setChannel(tpl.getChannel());
                record.setReceiverType(receiver.getType());
                record.setReceiverId(receiver.getId());
                record.setReceiver(truncate(address, RECEIVER_MAX));
                record.setTitle(truncate(render(tpl.getTitleTpl(), merged), TITLE_MAX));
                record.setContent(truncate(render(tpl.getContentTpl(), merged), CONTENT_MAX));
                record.setRetryCount(0);
                record.setReadFlag(0);
                record.setCreateTime(now);
                record.setUpdateTime(now);
                record.setDedupKey(dedupKey(bizType, bizKey, tpl.getChannel(), receiver));

                if (!MsgRecord.CHANNEL_INBOX.equals(tpl.getChannel()) && (address == null || address.trim().isEmpty())) {
                    // 没有可用地址：直接终态，避免无意义地反复重试
                    record.setStatus(MsgRecord.STATUS_GIVEN_UP);
                    record.setFailReason("未找到接收地址（" + receiver.getType() + ":" + receiver.getId() + "）");
                } else {
                    record.setStatus(MsgRecord.STATUS_PENDING);
                }
                try {
                    recordMapper.insert(record);
                    created++;
                } catch (DuplicateKeyException e) {
                    // 幂等：同一事件同一通道同一收件人已发过，跳过
                }
            }
        }
        return created;
    }

    @Override
    public int dispatchDue(int limit) {
        int cap = limit <= 0 ? 100 : Math.min(limit, 500);
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<MsgRecord> qw = new QueryWrapper<>();
        qw.in("status", MsgRecord.STATUS_PENDING, MsgRecord.STATUS_FAILED)
                .and(w -> w.isNull("next_retry_time").or().le("next_retry_time", now))
                .orderByAsc("id")
                .last("LIMIT " + cap);
        List<MsgRecord> due = recordMapper.selectList(qw);
        int handled = 0;
        for (MsgRecord record : due) {
            if (dispatchOne(record)) {
                handled++;
            }
        }
        return handled;
    }

    @Override
    public MsgRecord resend(Long recordId) {
        MsgRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息记录不存在：" + recordId);
        }
        // 注意：不清零 retryCount。否则一个坏号码可以被无限次手工重发、永远到不了 GIVEN_UP；
        // 只把 next_retry_time 提前到当下，立刻投一次。
        record.setStatus(MsgRecord.STATUS_PENDING);
        record.setNextRetryTime(LocalDateTime.now());
        record.setFailReason(null);
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(record);
        dispatchOne(record);
        return recordMapper.selectById(recordId);
    }

    @Override
    public List<MsgRecord> list(String status, String channel, String bizType, String bizKey, Integer limit) {
        QueryWrapper<MsgRecord> qw = new QueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        if (channel != null && !channel.trim().isEmpty()) {
            qw.eq("channel", channel.trim().toUpperCase());
        }
        if (bizType != null && !bizType.trim().isEmpty()) {
            qw.eq("biz_type", bizType.trim().toUpperCase());
        }
        if (bizKey != null && !bizKey.trim().isEmpty()) {
            qw.eq("biz_key", bizKey.trim());
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 200 : Math.min(limit, 1000);
        qw.last("LIMIT " + cap);
        return recordMapper.selectList(qw);
    }

    @Override
    public List<MsgRecord> inbox(Long orgId, Boolean unread, Integer limit) {
        QueryWrapper<MsgRecord> qw = new QueryWrapper<>();
        qw.eq("channel", MsgRecord.CHANNEL_INBOX)
                .eq("status", MsgRecord.STATUS_SENT);
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (Boolean.TRUE.equals(unread)) {
            qw.eq("read_flag", 0);
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return recordMapper.selectList(qw);
    }

    @Override
    public List<MsgRecord> inboxByReceiver(String receiverType, Long receiverId, Boolean unread, Integer limit) {
        if (receiverType == null || receiverId == null) {
            return new ArrayList<MsgRecord>();
        }
        QueryWrapper<MsgRecord> qw = new QueryWrapper<>();
        qw.eq("channel", MsgRecord.CHANNEL_INBOX)
                .eq("status", MsgRecord.STATUS_SENT)
                .eq("receiver_type", receiverType)
                .eq("receiver_id", receiverId);
        if (Boolean.TRUE.equals(unread)) {
            qw.eq("read_flag", 0);
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        qw.last("LIMIT " + cap);
        return recordMapper.selectList(qw);
    }

    @Override
    public int unreadCount(String receiverType, Long receiverId) {
        if (receiverType == null || receiverId == null) {
            return 0;
        }
        QueryWrapper<MsgRecord> qw = new QueryWrapper<>();
        qw.eq("channel", MsgRecord.CHANNEL_INBOX)
                .eq("status", MsgRecord.STATUS_SENT)
                .eq("read_flag", 0)
                .eq("receiver_type", receiverType)
                .eq("receiver_id", receiverId);
        Long n = recordMapper.selectCount(qw);
        return n == null ? 0 : n.intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markInboxRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        MsgRecord patch = new MsgRecord();
        patch.setReadFlag(1);
        patch.setUpdateTime(LocalDateTime.now());
        int n = 0;
        for (Long id : ids) {
            MsgRecord existing = recordMapper.selectById(id);
            if (existing == null || !MsgRecord.CHANNEL_INBOX.equals(existing.getChannel())) {
                continue;
            }
            patch.setId(id);
            recordMapper.updateById(patch);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 模板解析：机构模板覆盖平台默认模板。
     * 同 channel 上机构自建与平台默认同时存在时，只保留机构那条（避免一条事件发两遍）。
     */
    private List<MsgTemplate> resolveTemplates(Long orgId, String bizType) {
        QueryWrapper<MsgTemplate> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("enabled", 1);
        if (orgId != null && orgId > 0) {
            qw.and(w -> w.eq("org_id", orgId).or().eq("org_id", MsgTemplate.PLATFORM_ORG));
        } else {
            qw.eq("org_id", MsgTemplate.PLATFORM_ORG);
        }
        qw.orderByDesc("org_id");
        List<MsgTemplate> all = templateMapper.selectList(qw);
        Map<String, MsgTemplate> byChannel = new LinkedHashMap<>();
        for (MsgTemplate t : all) {
            byChannel.putIfAbsent(t.getChannel(), t);
        }
        return new ArrayList<>(byChannel.values());
    }

    private String resolveAddress(String channel, NotifyReceiver receiver) {
        if (MsgRecord.CHANNEL_INBOX.equals(channel)) {
            return null;
        }
        if (receiver.getAddress() != null && !receiver.getAddress().trim().isEmpty()) {
            return receiver.getAddress().trim();
        }
        ReceiverAddressPort port = addressPortProvider.getIfAvailable();
        return port == null ? null : port.resolve(receiver.getType(), receiver.getId());
    }

    /** 单条投递：成功 SENT；失败按退避安排下次重试，超限 GIVEN_UP。 */
    private boolean dispatchOne(MsgRecord record) {
        MessageChannel channel = channels.get(record.getChannel());
        if (channel == null) {
            record.setStatus(MsgRecord.STATUS_GIVEN_UP);
            record.setFailReason("不支持的通道：" + record.getChannel());
            record.setUpdateTime(LocalDateTime.now());
            recordMapper.updateById(record);
            return false;
        }
        MessageResult result;
        try {
            result = channel.send(record);
        } catch (Exception e) {
            // 真实网关异常（超时、证书失效等）同样进入重试，不能把记录卡在 PENDING
            result = MessageResult.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        LocalDateTime now = LocalDateTime.now();
        record.setUpdateTime(now);
        if (result != null && result.isSuccess()) {
            record.setStatus(MsgRecord.STATUS_SENT);
            record.setSentTime(now);
            record.setProviderMsgId(result.getProviderMsgId());
            record.setFailReason(null);
            record.setNextRetryTime(null);
        } else {
            int attempt = (record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1;
            record.setRetryCount(attempt);
            record.setFailReason(truncate(result == null ? "通道无返回" : result.getMessage(), 250));
            if (attempt >= MAX_RETRY) {
                record.setStatus(MsgRecord.STATUS_GIVEN_UP);
                record.setNextRetryTime(null);
            } else {
                record.setStatus(MsgRecord.STATUS_FAILED);
                record.setNextRetryTime(now.plusMinutes(BACKOFF_MINUTES[Math.min(attempt, BACKOFF_MINUTES.length) - 1]));
            }
        }
        recordMapper.updateById(record);
        return true;
    }

    private static String dedupKey(String bizType, String bizKey, String channel, NotifyReceiver receiver) {
        String who = receiver.getId() != null ? String.valueOf(receiver.getId())
                : (receiver.getAddress() == null ? "NA" : receiver.getAddress());
        String key = bizType + ":" + bizKey + ":" + channel + ":" + receiver.getType() + ":" + who;
        if (key.length() <= DEDUP_MAX) {
            return key;
        }
        // 业务键过长（例如拼接了长文本）时收缩中间段，保留可读头尾，靠哈希防碰撞
        String head = key.substring(0, 60);
        String tail = key.substring(key.length() - 40);
        return head + ":" + Integer.toHexString(key.hashCode()) + ":" + tail;
    }

    /** 模板渲染：${var} 替换；渲染后仍残留的占位符直接清空，避免把 ${x} 发给司机。 */
    static String render(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        String out = template;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : String.valueOf(e.getValue()));
        }
        return out.replaceAll("\\$\\{[^}]+}", "");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 供测试与运维排查：已注册的通道列表。 */
    public List<String> channelNames() {
        return Collections.unmodifiableList(new ArrayList<>(channels.keySet()));
    }
}
