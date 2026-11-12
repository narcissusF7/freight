package com.lukachangtong.freight.api.waybill;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运单参与方视图（投诉 / 评价 / 信用三个场景共用）。
 *
 * 只带「谁承运、谁是货主、是否已具备评价资格」这几项，不带金额与货物明细：
 * 客服域不该能读到运价，否则投诉处理人有机会拿运价去和投诉人「协商」。
 */
public class WaybillParty implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long waybillId;
    private String waybillNo;
    private Long orgId;
    private Long driverId;
    private String driverName;
    private Long shipperId;
    private String shipperName;
    /** 运单状态（评价资格判定用） */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime signTime;

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
    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }
}
