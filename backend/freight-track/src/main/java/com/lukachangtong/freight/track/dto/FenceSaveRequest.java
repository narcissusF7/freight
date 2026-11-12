package com.lukachangtong.freight.track.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 围栏保存请求。圆形只填 centerLat/centerLng/radiusM；
 * 多边形只填 vertices（[[lat,lng],...]，至少 3 点）。两种形状互斥，由服务端校验。
 */
public class FenceSaveRequest {

    private Long orgId;
    private String fenceCode;
    private String fenceName;
    /** LOAD / UNLOAD / FORBIDDEN / CUSTOM */
    private String fenceType;
    /** CIRCLE / POLYGON */
    private String shape;
    private BigDecimal centerLat;
    private BigDecimal centerLng;
    private Integer radiusM;
    private List<List<Double>> vertices;
    /** IN / OUT / BOTH */
    private String triggerType;
    private Integer enabled;
    private String remark;
    private String operateBy;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public String getFenceCode() { return fenceCode; }
    public void setFenceCode(String v) { this.fenceCode = v; }
    public String getFenceName() { return fenceName; }
    public void setFenceName(String v) { this.fenceName = v; }
    public String getFenceType() { return fenceType; }
    public void setFenceType(String v) { this.fenceType = v; }
    public String getShape() { return shape; }
    public void setShape(String v) { this.shape = v; }
    public BigDecimal getCenterLat() { return centerLat; }
    public void setCenterLat(BigDecimal v) { this.centerLat = v; }
    public BigDecimal getCenterLng() { return centerLng; }
    public void setCenterLng(BigDecimal v) { this.centerLng = v; }
    public Integer getRadiusM() { return radiusM; }
    public void setRadiusM(Integer v) { this.radiusM = v; }
    public List<List<Double>> getVertices() { return vertices; }
    public void setVertices(List<List<Double>> v) { this.vertices = v; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String v) { this.triggerType = v; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer v) { this.enabled = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String v) { this.operateBy = v; }
}
