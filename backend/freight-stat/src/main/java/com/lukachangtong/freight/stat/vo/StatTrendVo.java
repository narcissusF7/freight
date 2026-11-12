package com.lukachangtong.freight.stat.vo;

import java.math.BigDecimal;

/**
 * 趋势点。一个点带全部指标，前端切换指标不用重新请求。
 */
public class StatTrendVo {

    /** 标签：2026-11-10 / 2026-11 / 2026-W45 */
    private String period;
    private long waybillCreated = 0;
    private long waybillSigned = 0;
    private java.math.BigDecimal settleAmount;
    private java.math.BigDecimal payAmount;
    private long complaintCount = 0;
    private long complaintOverdue = 0;
    private long ratingCount = 0;
    private double avgRating = 0;
    private long alarmCount = 0;

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public long getWaybillCreated() { return waybillCreated; }
    public void setWaybillCreated(long waybillCreated) { this.waybillCreated = waybillCreated; }
    public long getWaybillSigned() { return waybillSigned; }
    public void setWaybillSigned(long waybillSigned) { this.waybillSigned = waybillSigned; }
    public java.math.BigDecimal getSettleAmount() { return settleAmount; }
    public void setSettleAmount(java.math.BigDecimal settleAmount) { this.settleAmount = settleAmount; }
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
}
