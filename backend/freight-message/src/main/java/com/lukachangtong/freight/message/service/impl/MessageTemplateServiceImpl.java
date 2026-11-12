package com.lukachangtong.freight.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.message.dto.TemplateSaveRequest;
import com.lukachangtong.freight.message.entity.MsgTemplate;
import com.lukachangtong.freight.message.mapper.MsgTemplateMapper;
import com.lukachangtong.freight.message.service.MessageTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static final List<String> CHANNELS = Arrays.asList("SMS", "WECHAT", "PUSH", "INBOX");
    private static final List<String> RECEIVER_TYPES = Arrays.asList("DRIVER", "SHIPPER", "EMPLOYEE", "ORG");

    @Resource
    private MsgTemplateMapper templateMapper;

    @Override
    public List<MsgTemplate> list(Long orgId, String bizType, String channel) {
        QueryWrapper<MsgTemplate> qw = new QueryWrapper<>();
        if (orgId != null) {
            // 同时展示平台默认与机构自定义，便于运维对比覆盖关系
            qw.and(w -> w.eq("org_id", orgId).or().eq("org_id", MsgTemplate.PLATFORM_ORG));
        }
        if (bizType != null && !bizType.trim().isEmpty()) {
            qw.eq("biz_type", bizType.trim());
        }
        if (channel != null && !channel.trim().isEmpty()) {
            qw.eq("channel", channel.trim().toUpperCase());
        }
        qw.orderByAsc("biz_type").orderByAsc("channel").orderByAsc("org_id");
        return templateMapper.selectList(qw);
    }

    @Override
    public MsgTemplate get(Long id) {
        MsgTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "模板不存在：" + id);
        }
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TemplateSaveRequest req) {
        MsgTemplate t = new MsgTemplate();
        apply(req, t, true);
        t.setCreateBy(req.getOperateBy());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(t);
        return t.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TemplateSaveRequest req) {
        MsgTemplate t = get(id);
        apply(req, t, false);
        t.setUpdateBy(req.getOperateBy());
        t.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        get(id);
        templateMapper.deleteById(id);
    }

    private void apply(TemplateSaveRequest req, MsgTemplate t, boolean isCreate) {
        String channel = req.getChannel() == null ? "" : req.getChannel().trim().toUpperCase();
        if (!CHANNELS.contains(channel)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "channel 仅支持 SMS/WECHAT/PUSH/INBOX");
        }
        String receiverType = req.getReceiverType() == null ? "DRIVER" : req.getReceiverType().trim().toUpperCase();
        if (!RECEIVER_TYPES.contains(receiverType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "receiverType 仅支持 DRIVER/SHIPPER/EMPLOYEE/ORG");
        }
        if (req.getBizType() == null || req.getBizType().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "bizType 不能为空");
        }
        if (req.getContentTpl() == null || req.getContentTpl().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "contentTpl 不能为空");
        }
        if (isCreate) {
            if (req.getTemplateCode() == null || req.getTemplateCode().trim().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "templateCode 不能为空");
            }
            t.setTemplateCode(req.getTemplateCode().trim());
            t.setOrgId(req.getOrgId() == null ? MsgTemplate.PLATFORM_ORG : req.getOrgId());
        }
        t.setTemplateName(req.getTemplateName() == null ? req.getTemplateCode() : req.getTemplateName());
        t.setBizType(req.getBizType().trim().toUpperCase());
        t.setChannel(channel);
        t.setReceiverType(receiverType);
        t.setTitleTpl(req.getTitleTpl());
        t.setContentTpl(req.getContentTpl());
        t.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        t.setRemark(req.getRemark());
    }
}
