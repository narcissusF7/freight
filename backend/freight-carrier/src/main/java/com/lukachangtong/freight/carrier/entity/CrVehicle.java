package com.lukachangtong.freight.carrier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 车辆准入信息（承运信息-车辆管理）。 */
@TableName("cr_vehicle")
public class CrVehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("plate_no")
    private String plateNo;

    @TableField("vehicle_type")
    private String vehicleType;

    /** 道路运输证有效期至（到期预警） */
    @TableField("vehicle_license_expire_date")
    private LocalDate vehicleLicenseExpireDate;

    /** 保险有效期至（到期预警） */
    @TableField("insurance_expire_date")
    private LocalDate insuranceExpireDate;

    @TableField("owner_name")
    private String ownerName;

    /** 核定载重（吨） */
    @TableField("capacity_ton")
    private BigDecimal capacityTon;

    @TableField("axle_count")
    private Integer axleCount;

    /** 关联司机（可选） */
    @TableField("driver_id")
    private Long driverId;

    /** 0 待审核 1 审核通过 2 驳回 */
    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public LocalDate getVehicleLicenseExpireDate() { return vehicleLicenseExpireDate; }
    public void setVehicleLicenseExpireDate(LocalDate v) { this.vehicleLicenseExpireDate = v; }
    public LocalDate getInsuranceExpireDate() { return insuranceExpireDate; }
    public void setInsuranceExpireDate(LocalDate v) { this.insuranceExpireDate = v; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public BigDecimal getCapacityTon() { return capacityTon; }
    public void setCapacityTon(BigDecimal capacityTon) { this.capacityTon = capacityTon; }
    public Integer getAxleCount() { return axleCount; }
    public void setAxleCount(Integer axleCount) { this.axleCount = axleCount; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
