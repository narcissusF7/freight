package com.lukachangtong.freight.track.dto;

/** 预警处理请求：status 1 已处理 / 2 已忽略（默认 1）。 */
public class AlarmHandleRequest {

    private String handleBy;
    private String handleRemark;
    private Integer status;

    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String v) { this.handleBy = v; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String v) { this.handleRemark = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
}
