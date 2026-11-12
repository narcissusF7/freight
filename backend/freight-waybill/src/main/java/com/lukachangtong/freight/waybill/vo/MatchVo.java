package com.lukachangtong.freight.waybill.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 车货匹配候选结果。
 * 分数是**调度建议**而非承诺：最终承运关系仍由运营确认后调用生成运单接口落库，
 * 避免"系统自动把货派给某司机"这种无法追责的行为。
 */
public class MatchVo {

    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Long vehicleId;
    private String plateNo;
    private String vehicleType;
    private BigDecimal capacityTon;

    /** 0~100 */
    private int score;
    /** 载重适配 / 证件有效 / 线路经验 / 运价吸引 四项明细 */
    private int capacityScore;
    private int certScore;
    private int experienceScore;
    private int priceScore;

    /** 是否满足载重（false 表示超载，不建议派单） */
    private boolean capacityFits;
    private List<String> reasons = new ArrayList<>();

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long v) { this.driverId = v; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String v) { this.driverName = v; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String v) { this.driverPhone = v; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String v) { this.plateNo = v; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String v) { this.vehicleType = v; }
    public BigDecimal getCapacityTon() { return capacityTon; }
    public void setCapacityTon(BigDecimal v) { this.capacityTon = v; }
    public int getScore() { return score; }
    public void setScore(int v) { this.score = v; }
    public int getCapacityScore() { return capacityScore; }
    public void setCapacityScore(int v) { this.capacityScore = v; }
    public int getCertScore() { return certScore; }
    public void setCertScore(int v) { this.certScore = v; }
    public int getExperienceScore() { return experienceScore; }
    public void setExperienceScore(int v) { this.experienceScore = v; }
    public int getPriceScore() { return priceScore; }
    public void setPriceScore(int v) { this.priceScore = v; }
    public boolean isCapacityFits() { return capacityFits; }
    public void setCapacityFits(boolean v) { this.capacityFits = v; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> v) { this.reasons = v; }
}
