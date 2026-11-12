package com.lukachangtong.freight.payment.dto;

import java.math.BigDecimal;

/**
 * 创建支付单指令。金额由业务方（结算域）算好传入，
 * 支付域**不接受前端直接传金额**——这是资金安全的底线。
 */
public class CreatePaymentCommand {

    private Long orgId;
    /** SETTLE / WITHDRAW */
    private String payType;
    private Long bizId;
    private String bizNo;
    private String payeeType;
    private Long payeeId;
    private String payeeName;
    private String payeeAccount;
    private String bankCode;
    private BigDecimal amount;
    private String operator;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getPayeeType() { return payeeType; }
    public void setPayeeType(String payeeType) { this.payeeType = payeeType; }
    public Long getPayeeId() { return payeeId; }
    public void setPayeeId(Long payeeId) { this.payeeId = payeeId; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public String getPayeeAccount() { return payeeAccount; }
    public void setPayeeAccount(String payeeAccount) { this.payeeAccount = payeeAccount; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
