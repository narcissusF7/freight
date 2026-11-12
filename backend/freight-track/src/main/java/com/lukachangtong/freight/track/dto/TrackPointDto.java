package com.lukachangtong.freight.track.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个轨迹点上报载荷。
 * gpsTime 是设备定位时间（不是上报时间），服务端用它做幂等键与分区依据。
 */
public class TrackPointDto {

    private Long waybillId;
    private Long driverId;
    private Long vehicleId;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal speed;
    private Integer direction;
    private BigDecimal altitude;
    private BigDecimal accuracy;
    private LocalDateTime gpsTime;
    /** APP / DEVICE / DEMO，默认 APP */
    private String source;

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
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
}
