package com.lukachangtong.freight.message.dto;

import com.lukachangtong.freight.api.message.NotifyReceiver;

import java.util.List;
import java.util.Map;

/** 手工发消息/演示用请求体：业务节点 + 业务键 + 模板变量 + 收件人。 */
public class MessageNotifyRequest {

    private Long orgId;
    private String bizType;
    private String bizKey;
    private Map<String, Object> vars;
    private List<NotifyReceiver> receivers;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public String getBizType() { return bizType; }
    public void setBizType(String v) { this.bizType = v; }
    public String getBizKey() { return bizKey; }
    public void setBizKey(String v) { this.bizKey = v; }
    public Map<String, Object> getVars() { return vars; }
    public void setVars(Map<String, Object> v) { this.vars = v; }
    public List<NotifyReceiver> getReceivers() { return receivers; }
    public void setReceivers(List<NotifyReceiver> v) { this.receivers = v; }
}
