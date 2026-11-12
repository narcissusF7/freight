package com.lukachangtong.freight.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息模板：一行 = 一个业务节点在某通道上的配置。
 *
 * 这就是「消息节点配置化」的落点：业务只发事件（TRACK_ALARM + 变量），
 * 走哪些通道、发给哪类接收方、文案长什么样，全由这张表决定，改配置不用发版。
 */
@TableName("msg_template")
public class MsgTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long PLATFORM_ORG = 0L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 0 = 平台默认，机构可建同 code 模板覆盖 */
    @TableField("org_id")
    private Long orgId;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("biz_type")
    private String bizType;

    /** SMS / WECHAT / PUSH / INBOX */
    @TableField("channel")
    private String channel;

    /** DRIVER / SHIPPER / EMPLOYEE / ORG */
    @TableField("receiver_type")
    private String receiverType;

    @TableField("title_tpl")
    private String titleTpl;

    @TableField("content_tpl")
    private String contentTpl;

    @TableField("enabled")
    private Integer enabled;

    @TableField("remark")
    private String remark;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
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
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String v) { this.createBy = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String v) { this.updateBy = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
}
