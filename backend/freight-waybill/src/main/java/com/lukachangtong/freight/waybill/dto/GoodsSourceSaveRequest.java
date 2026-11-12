package com.lukachangtong.freight.waybill.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 发布货源请求参数。 */
public class GoodsSourceSaveRequest {

    private Long orgId;
    private Long shipperId;
    private String goodsName;
    private BigDecimal weight;
    private BigDecimal volume;
    private String loadAddr;
    /** 装货地纬度（GCJ-02，地图选点回填） */
    private BigDecimal loadLat;
    private BigDecimal loadLng;
    private String unloadAddr;
    /** 卸货地纬度（GCJ-02，地图选点回填） */
    private BigDecimal unloadLat;
    private BigDecimal unloadLng;
    private LocalDateTime loadTime;
    private BigDecimal freightPrice;
    private String contactName;
    private String contactPhone;
    private String remark;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
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
    public LocalDateTime getLoadTime() { return loadTime; }
    public void setLoadTime(LocalDateTime loadTime) { this.loadTime = loadTime; }
    public BigDecimal getFreightPrice() { return freightPrice; }
    public void setFreightPrice(BigDecimal freightPrice) { this.freightPrice = freightPrice; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
