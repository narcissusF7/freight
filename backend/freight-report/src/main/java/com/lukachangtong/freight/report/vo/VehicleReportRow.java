package com.lukachangtong.freight.report.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 车辆上报视图：组装「车辆信息单」报文所需的业务字段。 */
public class VehicleReportRow {

    private Long id;
    private Long orgId;
    private String plateNo;
    private String vehicleType;
    private BigDecimal capacityTon;
    private Integer axleCount;
    private String ownerName;
    private LocalDateTime createTime;
    private String driverName;
    private String ownerIdCard;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public BigDecimal getCapacityTon() { return capacityTon; }
    public void setCapacityTon(BigDecimal capacityTon) { this.capacityTon = capacityTon; }
    public Integer getAxleCount() { return axleCount; }
    public void setAxleCount(Integer axleCount) { this.axleCount = axleCount; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getOwnerIdCard() { return ownerIdCard; }
    public void setOwnerIdCard(String ownerIdCard) { this.ownerIdCard = ownerIdCard; }
}
