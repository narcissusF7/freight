package com.lukachangtong.freight.report.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运单上报视图：组装「电子运单」报文所需的业务字段。
 * 只读，不带无关明细（上报不需要节点、异常件等）。
 */
public class WaybillReportRow {

    private Long id;
    private Long orgId;
    private String waybillNo;
    private Long shipperId;
    private Long driverId;
    private Long vehicleId;
    private String cargoName;
    private BigDecimal weight;
    private BigDecimal amount;
    private String loadAddr;
    private String unloadAddr;
    private LocalDateTime createTime;
    private LocalDateTime signTime;
    private String status;
    private String driverName;
    private String plateNo;
    private String shipperName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getCargoName() { return cargoName; }
    public void setCargoName(String cargoName) { this.cargoName = cargoName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }
}
