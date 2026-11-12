package com.lukachangtong.freight.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息发送记录。
 *
 * 状态机：PENDING →（投递成功）SENT；PENDING →（失败且未达上限）FAILED →（到期重试）PENDING…
 * 达到最大重试次数仍未成功 → GIVEN_UP（不再重试，但保留记录供人工核对）。
 * 站内信没有外部网关，投递即成功，同样走这条链路，保证四种通道行为一致。
 */
@TableName("msg_record")
public class MsgRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_GIVEN_UP = "GIVEN_UP";

    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_PUSH = "PUSH";
    public static final String CHANNEL_INBOX = "INBOX";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_key")
    private String bizKey;

    @TableField("template_code")
    private String templateCode;

    @TableField("channel")
    private String channel;

    @TableField("receiver_type")
    private String receiverType;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("receiver")
    private String receiver;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("sent_time")
    private LocalDateTime sentTime;

    @TableField("fail_reason")
    private String failReason;

    @TableField("provider_msg_id")
    private String providerMsgId;

    /** 站内信已读标志（其他通道恒 0） */
    @TableField("read_flag")
    private Integer readFlag;

    @TableField("dedup_key")
    private String dedupKey;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public String getBizType() { return bizType; }
    public void setBizType(String v) { this.bizType = v; }
    public String getBizKey() { return bizKey; }
    public void setBizKey(String v) { this.bizKey = v; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String v) { this.templateCode = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long v) { this.receiverId = v; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String v) { this.receiver = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer v) { this.retryCount = v; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime v) { this.nextRetryTime = v; }
    public LocalDateTime getSentTime() { return sentTime; }
    public void setSentTime(LocalDateTime v) { this.sentTime = v; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String v) { this.failReason = v; }
    public String getProviderMsgId() { return providerMsgId; }
    public void setProviderMsgId(String v) { this.providerMsgId = v; }
    public Integer getReadFlag() { return readFlag; }
    public void setReadFlag(Integer v) { this.readFlag = v; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String v) { this.dedupKey = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
