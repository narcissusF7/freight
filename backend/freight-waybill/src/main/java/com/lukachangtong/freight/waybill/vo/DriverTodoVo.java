package com.lukachangtong.freight.waybill.vo;

import com.lukachangtong.freight.waybill.entity.WbWaybill;

import java.util.List;

/**
 * 司机端「我的运单」概览 + 待办汇总。
 * 待办数量直接决定 APP 首页红点，必须与列表口径一致（同一份 scope 定义）。
 */
public class DriverTodoVo {

    /** 待装货：已接单未装车 */
    private int pendingLoad;
    /** 运输中 */
    private int inTransit;
    /** 待签收：已卸货未签收 */
    private int pendingSign;
    /** 未读消息 */
    private int unreadMessage;
    /** 证件/车辆到期提醒条数 */
    private int certAlerts;

    private List<WbWaybill> ongoing;

    public int getPendingLoad() { return pendingLoad; }
    public void setPendingLoad(int pendingLoad) { this.pendingLoad = pendingLoad; }
    public int getInTransit() { return inTransit; }
    public void setInTransit(int inTransit) { this.inTransit = inTransit; }
    public int getPendingSign() { return pendingSign; }
    public void setPendingSign(int pendingSign) { this.pendingSign = pendingSign; }
    public int getUnreadMessage() { return unreadMessage; }
    public void setUnreadMessage(int unreadMessage) { this.unreadMessage = unreadMessage; }
    public int getCertAlerts() { return certAlerts; }
    public void setCertAlerts(int certAlerts) { this.certAlerts = certAlerts; }
    public List<WbWaybill> getOngoing() { return ongoing; }
    public void setOngoing(List<WbWaybill> ongoing) { this.ongoing = ongoing; }
}
