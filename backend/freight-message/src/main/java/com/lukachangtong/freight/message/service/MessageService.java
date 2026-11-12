package com.lukachangtong.freight.message.service;

import com.lukachangtong.freight.api.message.MessageNotifyPort;
import com.lukachangtong.freight.message.entity.MsgRecord;

import java.util.List;

/**
 * 消息服务：模板渲染 + 落库 + 四通道投递 + 失败重试 + 站内信收件箱。
 *
 * 投递是**异步**的（先落 PENDING 再由调度器/手动触发），原因：
 * 业务在事务里不能同步调外部网关——网关超时会拖住运单流转事务，还会因为回滚导致「消息已发但业务失败」。
 */
public interface MessageService extends MessageNotifyPort {

    /** 投递到期（含首次 PENDING 与到点的 FAILED）消息，返回处理条数。 */
    int dispatchDue(int limit);

    /**
     * 手动重发：把 next_retry_time 提前到当下并立即投递一次。
     * **不清零重试计数**——重发同样计入上限，避免坏号码被无限重发。
     */
    MsgRecord resend(Long recordId);

    List<MsgRecord> list(String status, String channel, String bizType, String bizKey, Integer limit);

    /** 站内信收件箱（INBOX 通道）。 */
    List<MsgRecord> inbox(Long orgId, Boolean unread, Integer limit);

    /**
     * 按收件人查收件箱。APP 端只应看到「发给自己的」站内信，
     * 不能像调度台那样按 orgId 拉整个机构的信。
     */
    List<MsgRecord> inboxByReceiver(String receiverType, Long receiverId, Boolean unread, Integer limit);

    int unreadCount(String receiverType, Long receiverId);

    int markInboxRead(List<Long> ids);
}
