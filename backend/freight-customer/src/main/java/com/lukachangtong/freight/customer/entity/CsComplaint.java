package com.lukachangtong.freight.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 投诉单（24 小时时效）。
 *
 * 状态机：PENDING（待受理）→ PROCESSING（处理中）→ RESOLVED（已处理待确认）→ CLOSED（办结）；
 * PENDING / PROCESSING → REJECTED（投诉不成立）。
 *
 * `deadline_time` 在建单时按「受理 + 24h」落库，而不是查询时用 create_time 现算：
 * 时限是监管口径，必须可被索引、可被批量扫描，也必须在投诉改期时留痕。
 * `liable` 决定是否扣信用分——只有责任成立才扣，否则投诉会变成打击对手的工具。
 */
@TableName("cs_complaint")
public class CsComplaint implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String LEVEL_NORMAL = "NORMAL";
    public static final String LEVEL_URGENT = "URGENT";
    public static final String TARGET_DRIVER = "DRIVER";
    public static final String TARGET_SHIPPER = "SHIPPER";
    public static final String TARGET_PLATFORM = "PLATFORM";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 投诉单号 CP... */
    @TableField("complaint_no")
    private String complaintNo;

    @TableField("org_id")
    private Long orgId;

    /** DRIVER_APP / SHIPPER_APP / WEB / PHONE */
    @TableField("source")
    private String source;

    /** DRIVER / SHIPPER / OTHER */
    @TableField("complainant_type")
    private String complainantType;

    @TableField("complainant_id")
    private Long complainantId;

    @TableField("complainant_name")
    private String complainantName;

    @TableField("complainant_phone")
    private String complainantPhone;

    /** DRIVER / SHIPPER / PLATFORM */
    @TableField("target_type")
    private String targetType;

    /** 被投诉对象 ID */
    @TableField("target_id")
    private Long targetId;

    @TableField("target_name")
    private String targetName;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    /** DELAY / DAMAGE / ATTITUDE / FEE / LOST / OTHER */
    @TableField("complaint_type")
    private String complaintType;

    /** NORMAL / URGENT */
    @TableField("level")
    private String level;

    @TableField("content")
    private String content;

    /** 凭证图片，逗号分隔 */
    @TableField("image_urls")
    private String imageUrls;

    /** PENDING/PROCESSING/RESOLVED/REJECTED/CLOSED */
    @TableField("status")
    private String status;

    /** 受理时间（SLA 起算点） */
    @TableField("accept_time")
    private LocalDateTime acceptTime;

    /** 处理截止时间 */
    @TableField("deadline_time")
    private LocalDateTime deadlineTime;

    /** 处理人 */
    @TableField("assign_to")
    private String assignTo;

    @TableField("assign_time")
    private LocalDateTime assignTime;

    @TableField("resolved_time")
    private LocalDateTime resolvedTime;

    /** 处理结论（给投诉人的反馈内容） */
    @TableField("resolve_result")
    private String resolveResult;

    /** 责任是否成立：1 成立（扣信用分）/ 0 不成立 */
    @TableField("liable")
    private Integer liable;

    /** 投诉人满意度 1~5 */
    @TableField("satisfaction")
    private Integer satisfaction;

    @TableField("close_time")
    private LocalDateTime closeTime;

    @TableField("close_remark")
    private String closeRemark;

    /** 1 = 已逾期 */
    @TableField("overdue_flag")
    private Integer overdueFlag;

    /** 升级次数 */
    @TableField("escalate_level")
    private Integer escalateLevel;

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
    public void setId(Long id) { this.id = id; }
    public String getComplaintNo() { return complaintNo; }
    public void setComplaintNo(String complaintNo) { this.complaintNo = complaintNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getComplainantType() { return complainantType; }
    public void setComplainantType(String complainantType) { this.complainantType = complainantType; }
    public Long getComplainantId() { return complainantId; }
    public void setComplainantId(Long complainantId) { this.complainantId = complainantId; }
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    public String getComplainantPhone() { return complainantPhone; }
    public void setComplainantPhone(String complainantPhone) { this.complainantPhone = complainantPhone; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { this.complaintType = complaintType; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAcceptTime() { return acceptTime; }
    public void setAcceptTime(LocalDateTime acceptTime) { this.acceptTime = acceptTime; }
    public LocalDateTime getDeadlineTime() { return deadlineTime; }
    public void setDeadlineTime(LocalDateTime deadlineTime) { this.deadlineTime = deadlineTime; }
    public String getAssignTo() { return assignTo; }
    public void setAssignTo(String assignTo) { this.assignTo = assignTo; }
    public LocalDateTime getAssignTime() { return assignTime; }
    public void setAssignTime(LocalDateTime assignTime) { this.assignTime = assignTime; }
    public LocalDateTime getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }
    public String getResolveResult() { return resolveResult; }
    public void setResolveResult(String resolveResult) { this.resolveResult = resolveResult; }
    public Integer getLiable() { return liable; }
    public void setLiable(Integer liable) { this.liable = liable; }
    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public String getCloseRemark() { return closeRemark; }
    public void setCloseRemark(String closeRemark) { this.closeRemark = closeRemark; }
    public Integer getOverdueFlag() { return overdueFlag; }
    public void setOverdueFlag(Integer overdueFlag) { this.overdueFlag = overdueFlag; }
    public Integer getEscalateLevel() { return escalateLevel; }
    public void setEscalateLevel(Integer escalateLevel) { this.escalateLevel = escalateLevel; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
