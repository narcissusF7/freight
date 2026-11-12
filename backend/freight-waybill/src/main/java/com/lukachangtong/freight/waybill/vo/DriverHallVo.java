package com.lukachangtong.freight.waybill.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 司机端货源大厅条目。
 *
 * 与调度台的车货匹配（{@link MatchVo}）打分口径一致（载重 40 / 证照 25 / 线路 20 / 运价 15），
 * 只是方向反过来：这里是「这条货适不适合我」，所以线路维度用「我当前位置到装货地的距离」。
 *
 * 接单前**不返回联系人完整手机号**（只回 138****1234），避免货源信息被爬走绕开平台交易。
 */
public class DriverHallVo {

    private Long goodsSourceId;
    private String goodsName;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal freightPrice;
    /** 折吨价（元/吨），司机最关心的比较口径 */
    private BigDecimal pricePerTon;

    private String loadAddr;
    private BigDecimal loadLat;
    private BigDecimal loadLng;
    private String unloadAddr;
    private BigDecimal unloadLat;
    private BigDecimal unloadLng;
    private LocalDateTime loadTime;

    private String contactName;
    /** 脱敏手机号 */
    private String contactPhone;

    private int score;
    private int capacityScore;
    private int certScore;
    private int routeScore;
    private int priceScore;
    /** 载重是否满足；false 时前端标记「超载」且不允许接单 */
    private boolean capacityFits;
    /** 我当前位置到装货地的直线距离（km），无定位时 null */
    private BigDecimal distanceToLoadKm;
    private List<String> reasons = new ArrayList<String>();

    public Long getGoodsSourceId() { return goodsSourceId; }
    public void setGoodsSourceId(Long goodsSourceId) { this.goodsSourceId = goodsSourceId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public BigDecimal getFreightPrice() { return freightPrice; }
    public void setFreightPrice(BigDecimal freightPrice) { this.freightPrice = freightPrice; }
    public BigDecimal getPricePerTon() { return pricePerTon; }
    public void setPricePerTon(BigDecimal pricePerTon) { this.pricePerTon = pricePerTon; }
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
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCapacityScore() { return capacityScore; }
    public void setCapacityScore(int capacityScore) { this.capacityScore = capacityScore; }
    public int getCertScore() { return certScore; }
    public void setCertScore(int certScore) { this.certScore = certScore; }
    public int getRouteScore() { return routeScore; }
    public void setRouteScore(int routeScore) { this.routeScore = routeScore; }
    public int getPriceScore() { return priceScore; }
    public void setPriceScore(int priceScore) { this.priceScore = priceScore; }
    public boolean isCapacityFits() { return capacityFits; }
    public void setCapacityFits(boolean capacityFits) { this.capacityFits = capacityFits; }
    public BigDecimal getDistanceToLoadKm() { return distanceToLoadKm; }
    public void setDistanceToLoadKm(BigDecimal distanceToLoadKm) { this.distanceToLoadKm = distanceToLoadKm; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
