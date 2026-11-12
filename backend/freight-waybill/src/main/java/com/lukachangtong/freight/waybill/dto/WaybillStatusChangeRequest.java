package com.lukachangtong.freight.waybill.dto;

/** 运单状态流转请求。toStatus 为目标状态枚举名，必须经状态机校验。 */
public class WaybillStatusChangeRequest {

    private String toStatus;
    private String operateBy;
    private String remark;

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
