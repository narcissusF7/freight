package com.lukachangtong.freight.report.vo;

/**
 * 上报看板统计。
 */
public class ReportStatVo {

    private long total;
    private long success;
    private long failed;
    private long pending;
    private long waitingRetry;
    /** 待上报的运单数（已签收但未上报） */
    private long pendingWaybill;
    /** 待上报的资金流水数 */
    private long pendingFundFlow;
    /** 待上报的车辆数 */
    private long pendingVehicle;
    /** 待上报的驾驶员数 */
    private long pendingDriver;
    /** 上报成功率（成功 / 已完成） */
    private double successRate;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getSuccess() { return success; }
    public void setSuccess(long success) { this.success = success; }
    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public long getWaitingRetry() { return waitingRetry; }
    public void setWaitingRetry(long waitingRetry) { this.waitingRetry = waitingRetry; }
    public long getPendingWaybill() { return pendingWaybill; }
    public void setPendingWaybill(long pendingWaybill) { this.pendingWaybill = pendingWaybill; }
    public long getPendingFundFlow() { return pendingFundFlow; }
    public void setPendingFundFlow(long pendingFundFlow) { this.pendingFundFlow = pendingFundFlow; }
    public long getPendingVehicle() { return pendingVehicle; }
    public void setPendingVehicle(long pendingVehicle) { this.pendingVehicle = pendingVehicle; }
    public long getPendingDriver() { return pendingDriver; }
    public void setPendingDriver(long pendingDriver) { this.pendingDriver = pendingDriver; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
}
