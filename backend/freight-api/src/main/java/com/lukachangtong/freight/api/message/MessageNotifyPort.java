package com.lukachangtong.freight.api.message;

import java.util.List;
import java.util.Map;

/**
 * 业务事件通知端口。
 *
 * 调用方（运单、预警、证件到期…）只表达「哪个业务节点的什么事件，发给谁」，
 * 具体走短信/微信/APP 推送/站内信由 message 模块按模板配置决定，业务侧零感知。
 *
 * 实现约定：**永不抛异常**。通知失败不能影响主业务事务；消息落库为 PENDING 后由调度器重试。
 */
public interface MessageNotifyPort {

    /**
     * @param orgId     机构 ID（用于匹配机构级模板，0 表示平台默认模板）
     * @param bizType   业务节点，如 TRACK_ALARM / WAYBILL_STATUS / CERT_EXPIRY
     * @param bizKey    业务唯一键（同一键只发一次，做幂等）
     * @param vars      模板变量，如 {"waybillNo":"WB123","content":"…"}
     * @param receivers 接收方列表
     * @return 新落库的消息条数（已存在的幂等跳过不计）
     */
    int notify(Long orgId, String bizType, String bizKey, Map<String, Object> vars, List<NotifyReceiver> receivers);
}
