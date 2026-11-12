package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 四方对账结果快照。
 *
 * 四方：结算单（PAID 应付）、支付单（SUCCESS）、资金流水（SETTLE_IN）、钱包余额变动。
 * 另外与银行对账文件比笔数与金额。`(biz_date, scene)` 唯一，重跑覆盖——
 * 对账是可重算的快照，不是流水，重复跑不应堆出多份结果。
 */
@TableName("st_reconciliation")
public class StReconciliation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_MATCH = "MATCH";
    public static final String STATUS_DIFF = "DIFF";
    public static final String SCENE_DAILY = "DAILY";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_date")
    private LocalDate bizDate;

    /** DAILY */
    @TableField("scene")
    private String scene;

    @TableField("settlement_count")
    private Integer settlementCount;

    @TableField("settlement_amount")
    private BigDecimal settlementAmount;

    @TableField("payment_count")
    private Integer paymentCount;

    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    @TableField("flow_count")
    private Integer flowCount;

    @TableField("flow_amount")
    private BigDecimal flowAmount;

    /** 当日钱包净变动 */
    @TableField("wallet_delta")
    private BigDecimal walletDelta;

    @TableField("bank_count")
    private Integer bankCount;

    @TableField("bank_amount")
    private BigDecimal bankAmount;

    @TableField("diff_count")
    private Integer diffCount;

    @TableField("diff_amount")
    private BigDecimal diffAmount;

    /** MATCH / DIFF */
    @TableField("status")
    private String status;

    @TableField("run_by")
    private String runBy;

    @TableField("run_time")
    private LocalDateTime runTime;

    @TableField("remark")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public Integer getSettlementCount() { return settlementCount; }
    public void setSettlementCount(Integer settlementCount) { this.settlementCount = settlementCount; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }
    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public Integer getFlowCount() { return flowCount; }
    public void setFlowCount(Integer flowCount) { this.flowCount = flowCount; }
    public BigDecimal getFlowAmount() { return flowAmount; }
    public void setFlowAmount(BigDecimal flowAmount) { this.flowAmount = flowAmount; }
    public BigDecimal getWalletDelta() { return walletDelta; }
    public void setWalletDelta(BigDecimal walletDelta) { this.walletDelta = walletDelta; }
    public Integer getBankCount() { return bankCount; }
    public void setBankCount(Integer bankCount) { this.bankCount = bankCount; }
    public BigDecimal getBankAmount() { return bankAmount; }
    public void setBankAmount(BigDecimal bankAmount) { this.bankAmount = bankAmount; }
    public Integer getDiffCount() { return diffCount; }
    public void setDiffCount(Integer diffCount) { this.diffCount = diffCount; }
    public BigDecimal getDiffAmount() { return diffAmount; }
    public void setDiffAmount(BigDecimal diffAmount) { this.diffAmount = diffAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRunBy() { return runBy; }
    public void setRunBy(String runBy) { this.runBy = runBy; }
    public LocalDateTime getRunTime() { return runTime; }
    public void setRunTime(LocalDateTime runTime) { this.runTime = runTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
