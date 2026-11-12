package com.lukachangtong.freight.settlement.dto;

import java.math.BigDecimal;

/** 提现申请。金额只能来自司机自己的钱包，不接受外部指定余额。 */
public class WithdrawApplyRequest {

    private String ownerType;
    private Long ownerId;
    private BigDecimal amount;
    private String bankAccount;
    private String bankName;
    private String operator;

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
