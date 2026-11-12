package com.lukachangtong.freight.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轨迹点（分区表 tr_track_point，按月 RANGE 分区）。
 *
 * 注意两点：
 * 1. 表主键是 (id, gps_time) 复合主键（MySQL 要求唯一键包含分区列），MyBatis-Plus 不支持复合主键，
 *    因此本实体**只用于 insert 与按时间窗 selectList**，禁止 updateById / deleteById。
 * 2. gps_time 是设备定位时间，report_time 是服务端接收时间；补传时二者会明显不同，
 *    幂等键用 (waybill_id, gps_time) 而不是 report_time。
 */
@TableName("tr_track_point")
public class TrTrackPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("driver_id")
    private Long driverId;

    @TableField("vehicle_id")
    private Long vehicleId;

    /** GCJ-02 */
    @TableField("lat")
    private BigDecimal lat;

    /** GCJ-02 */
    @TableField("lng")
    private BigDecimal lng;

    /** km/h */
    @TableField("speed")
    private BigDecimal speed;

    /** 航向 0-359 */
    @TableField("direction")
    private Integer direction;

    @TableField("altitude")
    private BigDecimal altitude;

    /** 定位精度（米），>100 视为漂移点 */
    @TableField("accuracy")
    private BigDecimal accuracy;

    /** 设备定位时间，分区键 */
    @TableField("gps_time")
    private LocalDateTime gpsTime;

    @TableField("report_time")
    private LocalDateTime reportTime;

    /** APP / DEVICE / DEMO */
    @TableField("source")
    private String source;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long v) { this.driverId = v; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal v) { this.lat = v; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal v) { this.lng = v; }
    public BigDecimal getSpeed() { return speed; }
    public void setSpeed(BigDecimal v) { this.speed = v; }
    public Integer getDirection() { return direction; }
    public void setDirection(Integer v) { this.direction = v; }
    public BigDecimal getAltitude() { return altitude; }
    public void setAltitude(BigDecimal v) { this.altitude = v; }
    public BigDecimal getAccuracy() { return accuracy; }
    public void setAccuracy(BigDecimal v) { this.accuracy = v; }
    public LocalDateTime getGpsTime() { return gpsTime; }
    public void setGpsTime(LocalDateTime v) { this.gpsTime = v; }
    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime v) { this.reportTime = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
}
