package com.lukachangtong.freight.customer.vo;

/**
 * 投诉时效看板：监管看的是「24h 内处理率」，不是「处理了多少」。
 */
public class ComplaintStatVo {

    private long total = 0;
    /** 待受理 */
    private long pending = 0;
    /** 处理中 */
    private long processing = 0;
    /** 已处理待确认 */
    private long resolved = 0;
    /** 已办结 */
    private long closed = 0;
    /** 已驳回 */
    private long rejected = 0;
    /** 当前逾期 */
    private long overdue = 0;
    /** 今日新增 */
    private long todayNew = 0;
    /** 24h 内办结数 */
    private long closedInSla = 0;
    /** 24h 达标率（0~1） */
    private double onTimeRate = 0;
    /** 平均处理时长（小时） */
    private double avgHandleHours = 0;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public long getProcessing() { return processing; }
    public void setProcessing(long processing) { this.processing = processing; }
    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }
    public long getClosed() { return closed; }
    public void setClosed(long closed) { this.closed = closed; }
    public long getRejected() { return rejected; }
    public void setRejected(long rejected) { this.rejected = rejected; }
    public long getOverdue() { return overdue; }
    public void setOverdue(long overdue) { this.overdue = overdue; }
    public long getTodayNew() { return todayNew; }
    public void setTodayNew(long todayNew) { this.todayNew = todayNew; }
    public long getClosedInSla() { return closedInSla; }
    public void setClosedInSla(long closedInSla) { this.closedInSla = closedInSla; }
    public double getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(double onTimeRate) { this.onTimeRate = onTimeRate; }
    public double getAvgHandleHours() { return avgHandleHours; }
    public void setAvgHandleHours(double avgHandleHours) { this.avgHandleHours = avgHandleHours; }
}
