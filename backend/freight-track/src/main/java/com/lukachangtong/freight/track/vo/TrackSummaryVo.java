package com.lukachangtong.freight.track.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 轨迹摘要：用于运单详情页里程/时长展示，以及监管上报的里程字段。 */
public class TrackSummaryVo {

    private Long waybillId;
    private int pointCount;
    /** 里程（公里），已剔除 GPS 漂移跳点 */
    private BigDecimal distanceKm;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 有速度数据的点中最大速度（km/h） */
    private BigDecimal maxSpeed;
    /** 平均速度 = 里程 / 时长（km/h） */
    private BigDecimal avgSpeed;
    /** 因漂移被剔除的点数 */
    private int driftPoints;
    /** 首个与最后一个定位点坐标，便于回放直接定视 */
    private BigDecimal startLat;
    private BigDecimal startLng;
    private BigDecimal endLat;
    private BigDecimal endLng;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public int getPointCount() { return pointCount; }
    public void setPointCount(int v) { this.pointCount = v; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal v) { this.distanceKm = v; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime v) { this.startTime = v; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime v) { this.endTime = v; }
    public BigDecimal getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(BigDecimal v) { this.maxSpeed = v; }
    public BigDecimal getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(BigDecimal v) { this.avgSpeed = v; }
    public int getDriftPoints() { return driftPoints; }
    public void setDriftPoints(int v) { this.driftPoints = v; }
    public BigDecimal getStartLat() { return startLat; }
    public void setStartLat(BigDecimal v) { this.startLat = v; }
    public BigDecimal getStartLng() { return startLng; }
    public void setStartLng(BigDecimal v) { this.startLng = v; }
    public BigDecimal getEndLat() { return endLat; }
    public void setEndLat(BigDecimal v) { this.endLat = v; }
    public BigDecimal getEndLng() { return endLng; }
    public void setEndLng(BigDecimal v) { this.endLng = v; }
}
