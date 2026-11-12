package com.lukachangtong.freight.api.finance;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付成功后的入账指令。
 *
 * 两类业务共用一张支付单表，但入账语义不同：
 * - SETTLE：平台付司机运费 → 司机钱包**加钱**（结算入账）
 * - WITHDRAW：司机提现出金 → 司机钱包**扣钱**（提现出账，钱此时才真正离开）
 *
 * 所以入账动作由持有钱包的结算域执行，支付域只负责「钱有没有出去」。
 */
public class PaymentPostingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PAY_TYPE_SETTLE = "SETTLE";
    public static final String PAY_TYPE_WITHDRAW = "WITHDRAW";

    private String payType;
    private Long bizId;
    private String bizNo;
    private String payNo;
    private Long orgId;
    private String payeeType;
    private Long payeeId;
    private String payeeName;
    private BigDecimal amount;

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getPayeeType() { return payeeType; }
    public void setPayeeType(String payeeType) { this.payeeType = payeeType; }
    public Long getPayeeId() { return payeeId; }
    public void setPayeeId(Long payeeId) { this.payeeId = payeeId; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
