package com.lukachangtong.freight.waybill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运单（运输核心域）。状态存 {@code WaybillStatus} 枚举名，所有流转必须经状态机校验，
 * 禁止前端直接指定目标状态（大纲 1.2 / 13-12）。
 */
@TableName("wb_waybill")
public class WbWaybill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    /** 业务单号，唯一索引 */
    @TableField("waybill_no")
    private String waybillNo;

    @TableField("goods_source_id")
    private Long goodsSourceId;

    @TableField("shipper_id")
    private Long shipperId;

    @TableField("driver_id")
    private Long driverId;

    @TableField("vehicle_id")
    private Long vehicleId;

    @TableField("cargo_name")
    private String cargoName;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("load_addr")
    private String loadAddr;

    /** 装货地纬度（GCJ-02），供轨迹与里程计算 */
    @TableField("load_lat")
    private BigDecimal loadLat;

    /** 装货地经度（GCJ-02） */
    @TableField("load_lng")
    private BigDecimal loadLng;

    @TableField("unload_addr")
    private String unloadAddr;

    /** 卸货地纬度（GCJ-02） */
    @TableField("unload_lat")
    private BigDecimal unloadLat;

    /** 卸货地经度（GCJ-02） */
    @TableField("unload_lng")
    private BigDecimal unloadLng;

    /** 约定运费（含税，元），建单/接单时从货源报价继承；结算的计价基准 */
    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @TableField("plan_load_time")
    private LocalDateTime planLoadTime;

    /** 接单时间（首次进入 ACCEPTED），统计按此归集「接单量」 */
    @TableField("accept_time")
    private LocalDateTime acceptTime;

    /** 签收时间（首次进入 SIGNED），统计按此归集「完成量」 */
    @TableField("sign_time")
    private LocalDateTime signTime;

    /** 运单状态：WaybillStatus 枚举名 */
    @TableField("status")
    private String status;

    /** 上报状态：0 未上报 1 已上报 2 上报失败 */
    @TableField("report_status")
    private Integer reportStatus;

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
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public Long getGoodsSourceId() { return goodsSourceId; }
    public void setGoodsSourceId(Long goodsSourceId) { this.goodsSourceId = goodsSourceId; }
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
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public BigDecimal getLoadLat() { return loadLat; }
    public void setLoadLat(BigDecimal loadLat) { this.loadLat = loadLat; }
    public BigDecimal getLoadLng() { return loadLng; }
    public void setLoadLng(BigDecimal loadLng) { this.loadLng = loadLng; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
    public BigDecimal getUnloadLat() { return unloadLat; }
    public void setUnloadLat(BigDecimal unloadLat) { this.unloadLat = unloadLat; }
    public BigDecimal getUnloadLng() { return unloadLng; }
    public void setUnloadLng(BigDecimal unloadLng) { this.unloadLng = unloadLng; }
    public BigDecimal getFreightAmount() { return freightAmount; }
    public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }
    public LocalDateTime getAcceptTime() { return acceptTime; }
    public void setAcceptTime(LocalDateTime acceptTime) { this.acceptTime = acceptTime; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }
    public LocalDateTime getPlanLoadTime() { return planLoadTime; }
    public void setPlanLoadTime(LocalDateTime planLoadTime) { this.planLoadTime = planLoadTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getReportStatus() { return reportStatus; }
    public void setReportStatus(Integer reportStatus) { this.reportStatus = reportStatus; }
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
