package com.lukachangtong.freight.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 在途预警。dedup_key 有唯一索引：同一运单同一事件重复分析只留一条，
 * 保证「上报后自动分析 + 人工补分析」不会产生重复告警。
 */
@TableName("tr_track_alarm")
public class TrTrackAlarm implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_OVERSPEED = "OVERSPEED";
    public static final String TYPE_DEVIATE = "DEVIATE";
    public static final String TYPE_LONG_STAY = "LONG_STAY";
    public static final String TYPE_FENCE_IN = "FENCE_IN";
    public static final String TYPE_FENCE_OUT = "FENCE_OUT";

    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_IGNORED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("driver_id")
    private Long driverId;

    @TableField("vehicle_id")
    private Long vehicleId;

    @TableField("alarm_type")
    private String alarmType;

    @TableField("alarm_level")
    private String alarmLevel;

    @TableField("alarm_time")
    private LocalDateTime alarmTime;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("fence_id")
    private Long fenceId;

    @TableField("content")
    private String content;

    @TableField("dedup_key")
    private String dedupKey;

    @TableField("status")
    private Integer status;

    @TableField("handle_by")
    private String handleBy;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long v) { this.driverId = v; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String v) { this.alarmType = v; }
    public String getAlarmLevel() { return alarmLevel; }
    public void setAlarmLevel(String v) { this.alarmLevel = v; }
    public LocalDateTime getAlarmTime() { return alarmTime; }
    public void setAlarmTime(LocalDateTime v) { this.alarmTime = v; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal v) { this.lat = v; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal v) { this.lng = v; }
    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long v) { this.fenceId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String v) { this.dedupKey = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String v) { this.handleBy = v; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime v) { this.handleTime = v; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String v) { this.handleRemark = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
