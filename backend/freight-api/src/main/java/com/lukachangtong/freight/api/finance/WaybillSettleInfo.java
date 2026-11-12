package com.lukachangtong.freight.api.finance;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算所需的运单财务视图。
 *
 * 为什么不是直接返回 `WbWaybill`：结算域不需要知道运单的 20 多个字段，
 * 只需要「谁承运、约定运价多少、当前是否可结算」。给一个窄接口，
 * 运单表以后加字段（比如多段运输）不会波及结算。
 */
public class WaybillSettleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long waybillId;
    private String waybillNo;
    private Long orgId;
    private Long driverId;
    private String driverName;
    /** 货主 ID（生成应收结算单用） */
    private Long shipperId;
    private Long vehicleId;
    /** 约定运费（含税，元）；可能为空（历史运单未记录运价） */
    private BigDecimal freightAmount;
    /** 货重（吨），运价为 0 时按吨位保底计价用 */
    private BigDecimal weight;
    private String status;
    private String loadAddr;
    private String unloadAddr;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public BigDecimal getFreightAmount() { return freightAmount; }
    public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
}
