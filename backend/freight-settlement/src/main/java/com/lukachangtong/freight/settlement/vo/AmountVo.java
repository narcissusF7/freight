package com.lukachangtong.freight.settlement.vo;

import java.math.BigDecimal;

/** 计价结果（含税拆分）。金额字段一律 2 位小数。 */
public class AmountVo {

    private BigDecimal amount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;

    public AmountVo() {
    }

    public AmountVo(BigDecimal amount, BigDecimal netAmount, BigDecimal taxAmount, BigDecimal taxRate) {
        this.amount = amount;
        this.netAmount = netAmount;
        this.taxAmount = taxAmount;
        this.taxRate = taxRate;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
}
