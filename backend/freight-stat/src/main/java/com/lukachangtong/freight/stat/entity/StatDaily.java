package com.lukachangtong.freight.stat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 统计日汇总（预聚合宽表）。
 *
 * 为什么预聚合：统计页要按日/周/月看趋势，若每次都 `GROUP BY DATE(create_time)` 扫全表，
 * 上线半年后这个接口会慢到没人用。宽表一次算好，查询只读几十到几百行。
 *
 * `org_id = 0` 表示全平台口径；按机构看时用它自己那行。
 * 表里的数字是**可重算的快照**：`rebuild` 按 `(biz_date, org_id)` 覆盖写，
 * 所以「昨天算错了」只需重跑，不需要人工修数。
 */
@TableName("stat_daily")
public class StatDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    @TableField("biz_date")
    private LocalDate bizDate;

    /** 0 = 全平台 */
    @TableField("org_id")
    private Long orgId;

    @TableField("waybill_created")
    private Integer waybillCreated;

    /** 新建运单约定运费合计 */
    @TableField("waybill_amount")
    private BigDecimal waybillAmount;

    @TableField("waybill_signed")
    private Integer waybillSigned;

    @TableField("signed_amount")
    private BigDecimal signedAmount;

    @TableField("waybill_exception")
    private Integer waybillException;

    @TableField("settle_count")
    private Integer settleCount;

    @TableField("settle_amount")
    private BigDecimal settleAmount;

    @TableField("pay_count")
    private Integer payCount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("complaint_count")
    private Integer complaintCount;

    @TableField("complaint_overdue")
    private Integer complaintOverdue;

    @TableField("rating_count")
    private Integer ratingCount;

    /** 星级合计，均分 = rating_sum / rating_count */
    @TableField("rating_sum")
    private Integer ratingSum;

    @TableField("alarm_count")
    private Integer alarmCount;

    @TableField("calc_time")
    private LocalDateTime calcTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Integer getWaybillCreated() { return waybillCreated; }
    public void setWaybillCreated(Integer waybillCreated) { this.waybillCreated = waybillCreated; }
    public BigDecimal getWaybillAmount() { return waybillAmount; }
    public void setWaybillAmount(BigDecimal waybillAmount) { this.waybillAmount = waybillAmount; }
    public Integer getWaybillSigned() { return waybillSigned; }
    public void setWaybillSigned(Integer waybillSigned) { this.waybillSigned = waybillSigned; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public Integer getWaybillException() { return waybillException; }
    public void setWaybillException(Integer waybillException) { this.waybillException = waybillException; }
    public Integer getSettleCount() { return settleCount; }
    public void setSettleCount(Integer settleCount) { this.settleCount = settleCount; }
    public BigDecimal getSettleAmount() { return settleAmount; }
    public void setSettleAmount(BigDecimal settleAmount) { this.settleAmount = settleAmount; }
    public Integer getPayCount() { return payCount; }
    public void setPayCount(Integer payCount) { this.payCount = payCount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public Integer getComplaintCount() { return complaintCount; }
    public void setComplaintCount(Integer complaintCount) { this.complaintCount = complaintCount; }
    public Integer getComplaintOverdue() { return complaintOverdue; }
    public void setComplaintOverdue(Integer complaintOverdue) { this.complaintOverdue = complaintOverdue; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public Integer getRatingSum() { return ratingSum; }
    public void setRatingSum(Integer ratingSum) { this.ratingSum = ratingSum; }
    public Integer getAlarmCount() { return alarmCount; }
    public void setAlarmCount(Integer alarmCount) { this.alarmCount = alarmCount; }
    public LocalDateTime getCalcTime() { return calcTime; }
    public void setCalcTime(LocalDateTime calcTime) { this.calcTime = calcTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
