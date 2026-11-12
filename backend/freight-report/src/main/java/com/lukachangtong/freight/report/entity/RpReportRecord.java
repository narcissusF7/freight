package com.lukachangtong.freight.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 上报记录：一次「业务单据 → 报文 → 回执」的完整留痕。
 *
 * 上报是审计关键链路，一条记录对应一次尝试；失败重试更新同一条记录的重试字段
 * （retry_count/next_retry_time），而不是每次都插新行，便于回答「这单到底上报了几次」。
 */
@TableName("rp_report_record")
public class RpReportRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    /** 单据类型：运单 */
    public static final String DOC_WAYBILL = "WAYBILL";
    /** 单据类型：资金流水 */
    public static final String DOC_FUND_FLOW = "FUND_FLOW";
    /** 单据类型：车辆 */
    public static final String DOC_VEHICLE = "VEHICLE";
    /** 单据类型：驾驶员 */
    public static final String DOC_DRIVER = "DRIVER";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("org_id")
    private Long orgId;
    @TableField("message_ref_no")
    private String messageRefNo;
    @TableField("doc_type")
    private String docType;
    @TableField("biz_no")
    private String bizNo;
    @TableField("biz_id")
    private Long bizId;
    @TableField("action_type")
    private String actionType;
    @TableField("payload")
    private String payload;
    @TableField("status")
    private String status;
    @TableField("receipt")
    private String receipt;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;
    @TableField("send_time")
    private LocalDateTime sendTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getMessageRefNo() { return messageRefNo; }
    public void setMessageRefNo(String messageRefNo) { this.messageRefNo = messageRefNo; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
