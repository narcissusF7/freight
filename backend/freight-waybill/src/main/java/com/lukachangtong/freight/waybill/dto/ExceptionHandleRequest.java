package com.lukachangtong.freight.waybill.dto;

/** 异常件处理请求。 */
public class ExceptionHandleRequest {

    private String handleRemark;
    private String operateBy;

    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String v) { this.handleRemark = v; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String v) { this.operateBy = v; }
}
