package com.lukachangtong.freight.message.dto;

/** 模板保存请求。 */
public class TemplateSaveRequest {

    private Long orgId;
    private String templateCode;
    private String templateName;
    private String bizType;
    private String channel;
    private String receiverType;
    private String titleTpl;
    private String contentTpl;
    private Integer enabled;
    private String remark;
    private String operateBy;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String v) { this.templateCode = v; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String v) { this.templateName = v; }
    public String getBizType() { return bizType; }
    public void setBizType(String v) { this.bizType = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public String getTitleTpl() { return titleTpl; }
    public void setTitleTpl(String v) { this.titleTpl = v; }
    public String getContentTpl() { return contentTpl; }
    public void setContentTpl(String v) { this.contentTpl = v; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer v) { this.enabled = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String v) { this.operateBy = v; }
}
