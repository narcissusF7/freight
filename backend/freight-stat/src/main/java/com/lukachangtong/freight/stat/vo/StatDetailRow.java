package com.lukachangtong.freight.stat.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据穿透明细行。
 *
 * `bizType` 让前端知道这条该跳到哪个详情页（运单/结算/支付/投诉/评价/预警），
 * 统计页的「点数字下钻」靠它把「多少单」变成「哪几单」。
 */
public class StatDetailRow {

    /** WAYBILL / SETTLEMENT / PAYMENT / COMPLAINT / RATING / ALARM */
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Long orgId;
    /** 相关方（司机/货主/对方） */
    private String partner;
    private java.math.BigDecimal amount;
    private String status;
    private java.time.LocalDateTime bizTime;
    /** 补充说明 */
    private String extra;
    private String loadAddr;
    private String unloadAddr;
    private String cargoName;
    private Long driverId;
    private String driverName;

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getPartner() { return partner; }
    public void setPartner(String partner) { this.partner = partner; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.LocalDateTime getBizTime() { return bizTime; }
    public void setBizTime(java.time.LocalDateTime bizTime) { this.bizTime = bizTime; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
    public String getCargoName() { return cargoName; }
    public void setCargoName(String cargoName) { this.cargoName = cargoName; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
}
