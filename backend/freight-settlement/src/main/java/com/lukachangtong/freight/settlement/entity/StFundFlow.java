package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 资金流水（只追加）。
 *
 * `(biz_type, biz_no)` 唯一：同一业务单同一动作只入账一次，
 * 支付回调重放、人工重复点「入账」都不会多给钱。
 * balance_before / balance_after 落库存证，钱包余额可由此重算。
 */
@TableName("st_fund_flow")
public class StFundFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_SETTLE_IN = "SETTLE_IN";
    public static final String TYPE_WITHDRAW_OUT = "WITHDRAW_OUT";
    public static final String TYPE_ADJUST = "ADJUST";
    public static final String DIR_IN = "IN";
    public static final String DIR_OUT = "OUT";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("flow_no")
    private String flowNo;

    @TableField("org_id")
    private Long orgId;

    @TableField("wallet_id")
    private Long walletId;

    @TableField("owner_type")
    private String ownerType;

    @TableField("owner_id")
    private Long ownerId;

    /** SETTLE_IN / WITHDRAW_OUT / ADJUST */
    @TableField("biz_type")
    private String bizType;

    /** 业务单号 */
    @TableField("biz_no")
    private String bizNo;

    /** IN / OUT */
    @TableField("direction")
    private String direction;

    @TableField("amount")
    private BigDecimal amount;

    /** 记账前余额 */
    @TableField("balance_before")
    private BigDecimal balanceBefore;

    /** 记账后余额 */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlowNo() { return flowNo; }
    public void setFlowNo(String flowNo) { this.flowNo = flowNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
