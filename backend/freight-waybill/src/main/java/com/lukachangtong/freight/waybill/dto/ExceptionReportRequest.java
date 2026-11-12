package com.lukachangtong.freight.waybill.dto;

/** 异常件上报请求。 */
public class ExceptionReportRequest {

    /** CARGO_DAMAGE / DELAY / ACCIDENT / ADDRESS / OTHER */
    private String exceptionType;
    private String description;
    private String imageUrls;
    private String operateBy;

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String v) { this.exceptionType = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String v) { this.imageUrls = v; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String v) { this.operateBy = v; }
}
