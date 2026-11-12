package com.lukachangtong.freight.stat.vo;

import java.math.BigDecimal;

/**
 * 聚合结果行（Mapper 直接映射，避免为每个指标建一个 DTO）。
 */
public class AggRow {

    private Long cnt;
    /** 金额或合计 */
    private java.math.BigDecimal amt;
    /** 均值（如星级） */
    private Double avgVal;

    public Long getCnt() { return cnt; }
    public void setCnt(Long cnt) { this.cnt = cnt; }
    public java.math.BigDecimal getAmt() { return amt; }
    public void setAmt(java.math.BigDecimal amt) { this.amt = amt; }
    public Double getAvgVal() { return avgVal; }
    public void setAvgVal(Double avgVal) { this.avgVal = avgVal; }
}
