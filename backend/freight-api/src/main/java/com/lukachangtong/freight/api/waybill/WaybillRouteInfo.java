package com.lukachangtong.freight.api.waybill;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 运单路线快照：只暴露轨迹/预警需要的字段，不暴露整张运单。
 * 目的：让 track 模块能判断「偏航」而不依赖 waybill 模块，避免两个业务域互相引用。
 */
public class WaybillRouteInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long waybillId;
    private Long orgId;
    private Long driverId;
    private Long vehicleId;
    /** 装货地（GCJ-02），未选点时为 null */
    private BigDecimal loadLat;
    private BigDecimal loadLng;
    /** 卸货地（GCJ-02），未选点时为 null */
    private BigDecimal unloadLat;
    private BigDecimal unloadLng;
    private String status;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long v) { this.driverId = v; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public BigDecimal getLoadLat() { return loadLat; }
    public void setLoadLat(BigDecimal v) { this.loadLat = v; }
    public BigDecimal getLoadLng() { return loadLng; }
    public void setLoadLng(BigDecimal v) { this.loadLng = v; }
    public BigDecimal getUnloadLat() { return unloadLat; }
    public void setUnloadLat(BigDecimal v) { this.unloadLat = v; }
    public BigDecimal getUnloadLng() { return unloadLng; }
    public void setUnloadLng(BigDecimal v) { this.unloadLng = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    /** 起终点坐标齐全才可做偏航判定（直线近似）。 */
    public boolean hasRoute() {
        return loadLat != null && loadLng != null && unloadLat != null && unloadLng != null;
    }
}
