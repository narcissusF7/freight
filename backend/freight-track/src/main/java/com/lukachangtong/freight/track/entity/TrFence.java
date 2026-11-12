package com.lukachangtong.freight.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子围栏。圆形（装卸点/园区）与多边形（禁行区）两种形状，
 * 多边形顶点以 JSON 文本存放，判定在服务端做（见 GeoUtils），不依赖 GIS 扩展。
 */
@TableName("tr_fence")
public class TrFence implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SHAPE_CIRCLE = "CIRCLE";
    public static final String SHAPE_POLYGON = "POLYGON";

    public static final String TRIGGER_IN = "IN";
    public static final String TRIGGER_OUT = "OUT";
    public static final String TRIGGER_BOTH = "BOTH";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("fence_code")
    private String fenceCode;

    @TableField("fence_name")
    private String fenceName;

    /** LOAD 装货 / UNLOAD 卸货 / FORBIDDEN 禁行 / CUSTOM 自定义 */
    @TableField("fence_type")
    private String fenceType;

    @TableField("shape")
    private String shape;

    @TableField("center_lat")
    private BigDecimal centerLat;

    @TableField("center_lng")
    private BigDecimal centerLng;

    @TableField("radius_m")
    private Integer radiusM;

    /** [[lat,lng],...] */
    @TableField("vertices")
    private String vertices;

    @TableField("trigger_type")
    private String triggerType;

    @TableField("enabled")
    private Integer enabled;

    @TableField("remark")
    private String remark;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
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
    public String getVertices() { return vertices; }
    public void setVertices(String v) { this.vertices = v; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String v) { this.triggerType = v; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer v) { this.enabled = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String v) { this.createBy = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String v) { this.updateBy = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
}
