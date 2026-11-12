package com.lukachangtong.freight.waybill.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 生成运单请求：由货源 + 指派司机/车辆生成运单（初始状态 DRAFT）。 */
public class WaybillCreateRequest {

    private Long orgId;
    private Long goodsSourceId;
    private Long driverId;
    private Long vehicleId;
    private String cargoName;
    private BigDecimal weight;
    private String loadAddr;
    private String unloadAddr;
    /** 可选：地图选点覆盖货源的经纬度（GCJ-02），为空则沿用货源坐标 */
    private BigDecimal loadLat;
    private BigDecimal loadLng;
    private BigDecimal unloadLat;
    private BigDecimal unloadLng;
    private LocalDateTime planLoadTime;
    private String operateBy;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getGoodsSourceId() { return goodsSourceId; }
    public void setGoodsSourceId(Long goodsSourceId) { this.goodsSourceId = goodsSourceId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getCargoName() { return cargoName; }
    public void setCargoName(String cargoName) { this.cargoName = cargoName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
    public BigDecimal getLoadLat() { return loadLat; }
    public void setLoadLat(BigDecimal loadLat) { this.loadLat = loadLat; }
    public BigDecimal getLoadLng() { return loadLng; }
    public void setLoadLng(BigDecimal loadLng) { this.loadLng = loadLng; }
    public BigDecimal getUnloadLat() { return unloadLat; }
    public void setUnloadLat(BigDecimal unloadLat) { this.unloadLat = unloadLat; }
    public BigDecimal getUnloadLng() { return unloadLng; }
    public void setUnloadLng(BigDecimal unloadLng) { this.unloadLng = unloadLng; }
    public LocalDateTime getPlanLoadTime() { return planLoadTime; }
    public void setPlanLoadTime(LocalDateTime planLoadTime) { this.planLoadTime = planLoadTime; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
}
