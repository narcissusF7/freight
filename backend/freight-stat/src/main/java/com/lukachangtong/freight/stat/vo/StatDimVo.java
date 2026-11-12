package com.lukachangtong.freight.stat.vo;

import java.math.BigDecimal;

/**
 * 维度分布行（省域/货物/司机），`share` 为占比，直接用于画条形图。
 */
public class StatDimVo {

    private String dimType;
    private String dimKey;
    private String dimLabel;
    private long waybillCount = 0;
    private java.math.BigDecimal amount;
    /** 占比 0~1（按单数） */
    private double share = 0;
    /** 金额占比 0~1（按金额） */
    private double amountShare = 0;
    private long complaintCount = 0;
    private long alarmCount = 0;

    public String getDimType() { return dimType; }
    public void setDimType(String dimType) { this.dimType = dimType; }
    public String getDimKey() { return dimKey; }
    public void setDimKey(String dimKey) { this.dimKey = dimKey; }
    public String getDimLabel() { return dimLabel; }
    public void setDimLabel(String dimLabel) { this.dimLabel = dimLabel; }
    public long getWaybillCount() { return waybillCount; }
    public void setWaybillCount(long waybillCount) { this.waybillCount = waybillCount; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public double getShare() { return share; }
    public void setShare(double share) { this.share = share; }
    public double getAmountShare() { return amountShare; }
    public void setAmountShare(double amountShare) { this.amountShare = amountShare; }
    public long getComplaintCount() { return complaintCount; }
    public void setComplaintCount(long complaintCount) { this.complaintCount = complaintCount; }
    public long getAlarmCount() { return alarmCount; }
    public void setAlarmCount(long alarmCount) { this.alarmCount = alarmCount; }
}
