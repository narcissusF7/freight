package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 提现申请。
 *
 * 状态机：APPLIED →（审核通过）APPROVED →（支付成功）PAID；APPLIED → REJECTED。
 * 申请时只冻结（frozen += amount），**不扣余额**——钱还在账上，只是不能再用；
 * 支付成功才真正扣减余额并写 OUT 流水，驳回则解冻。
 */
@TableName("st_withdraw")
public class StWithdraw implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PAID = "PAID";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("withdraw_no")
    private String withdrawNo;

    @TableField("org_id")
    private Long orgId;

    @TableField("owner_type")
    private String ownerType;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_name")
    private String ownerName;

    /** 申请金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 手续费 */
    @TableField("fee")
    private BigDecimal fee;

    /** 实际到账 */
    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("bank_name")
    private String bankName;

    /** APPLIED/APPROVED/REJECTED/PAID */
    @TableField("status")
    private String status;

    @TableField("audit_by")
    private String auditBy;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("pay_no")
    private String payNo;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWithdrawNo() { return withdrawNo; }
    public void setWithdrawNo(String withdrawNo) { this.withdrawNo = withdrawNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAuditBy() { return auditBy; }
    public void setAuditBy(String auditBy) { this.auditBy = auditBy; }
    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
