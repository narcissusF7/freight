package com.lukachangtong.freight.stat.vo;

import java.math.BigDecimal;

/**
 * 统计总览（卡片区）。
 *
 * 比率都在服务端算好：完成率、24h 投诉达标率、平均星级。
 * 前端只负责展示，避免每个页面各算一遍、口径还各不相同。
 */
public class StatOverviewVo {

    private String from;
    private String to;
    /** 新建运单 */
    private long waybillCreated = 0;
    /** 新建运费 */
    private java.math.BigDecimal waybillAmount;
    /** 签收完成 */
    private long waybillSigned = 0;
    /** 签收运费 */
    private java.math.BigDecimal signedAmount;
    /** 签收/新建 */
    private double completionRate = 0;
    private long settleCount = 0;
    /** 应结合计 */
    private java.math.BigDecimal settleAmount;
    private long payCount = 0;
    /** 支付合计 */
    private java.math.BigDecimal payAmount;
    private long complaintCount = 0;
    private long complaintOverdue = 0;
    private long ratingCount = 0;
    /** 平均星级 */
    private double avgRating = 0;
    private long alarmCount = 0;
    /** 本次查询口径：预聚合 or 实时补算 */
    private String calcTime;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public long getWaybillCreated() { return waybillCreated; }
    public void setWaybillCreated(long waybillCreated) { this.waybillCreated = waybillCreated; }
    public java.math.BigDecimal getWaybillAmount() { return waybillAmount; }
    public void setWaybillAmount(java.math.BigDecimal waybillAmount) { this.waybillAmount = waybillAmount; }
    public long getWaybillSigned() { return waybillSigned; }
    public void setWaybillSigned(long waybillSigned) { this.waybillSigned = waybillSigned; }
    public java.math.BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(java.math.BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    public long getSettleCount() { return settleCount; }
    public void setSettleCount(long settleCount) { this.settleCount = settleCount; }
    public java.math.BigDecimal getSettleAmount() { return settleAmount; }
    public void setSettleAmount(java.math.BigDecimal settleAmount) { this.settleAmount = settleAmount; }
    public long getPayCount() { return payCount; }
    public void setPayCount(long payCount) { this.payCount = payCount; }
    public java.math.BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(java.math.BigDecimal payAmount) { this.payAmount = payAmount; }
    public long getComplaintCount() { return complaintCount; }
    public void setComplaintCount(long complaintCount) { this.complaintCount = complaintCount; }
    public long getComplaintOverdue() { return complaintOverdue; }
    public void setComplaintOverdue(long complaintOverdue) { this.complaintOverdue = complaintOverdue; }
    public long getRatingCount() { return ratingCount; }
    public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }
    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public long getAlarmCount() { return alarmCount; }
    public void setAlarmCount(long alarmCount) { this.alarmCount = alarmCount; }
    public String getCalcTime() { return calcTime; }
    public void setCalcTime(String calcTime) { this.calcTime = calcTime; }
}
