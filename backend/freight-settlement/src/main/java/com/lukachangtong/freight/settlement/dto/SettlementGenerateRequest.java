package com.lukachangtong.freight.settlement.dto;

import java.math.BigDecimal;

/**
 * 生成结算单入参。
 * 只有「附加费/扣款」允许人工填，基础运费一律取自运单约定运价——
 * 前端能改基础运价，就等于能给自己发钱。
 */
public class SettlementGenerateRequest {

    private Long waybillId;
    /** PAYABLE 应付司机（默认）/ RECEIVABLE 应收货主 */
    private String bizType;
    private BigDecimal loadFee;
    private BigDecimal unloadFee;
    private BigDecimal waitFee;
    private BigDecimal oilSubsidy;
    private BigDecimal deductAmount;
    private BigDecimal taxRate;
    private String remark;
    private String operator;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public BigDecimal getLoadFee() { return loadFee; }
    public void setLoadFee(BigDecimal loadFee) { this.loadFee = loadFee; }
    public BigDecimal getUnloadFee() { return unloadFee; }
    public void setUnloadFee(BigDecimal unloadFee) { this.unloadFee = unloadFee; }
    public BigDecimal getWaitFee() { return waitFee; }
    public void setWaitFee(BigDecimal waitFee) { this.waitFee = waitFee; }
    public BigDecimal getOilSubsidy() { return oilSubsidy; }
    public void setOilSubsidy(BigDecimal oilSubsidy) { this.oilSubsidy = oilSubsidy; }
    public BigDecimal getDeductAmount() { return deductAmount; }
    public void setDeductAmount(BigDecimal deductAmount) { this.deductAmount = deductAmount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
