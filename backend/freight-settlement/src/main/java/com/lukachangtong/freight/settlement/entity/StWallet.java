package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 资金账户。balance 为总余额，frozen 为提现处理中冻结，
 * 可用余额 = balance - frozen。所有变动都必须配一条 st_fund_flow，
 * 余额可以由流水重算校验（对账的 WALLET_BALANCE 项就是干这个）。
 */
@TableName("st_wallet")
public class StWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String OWNER_DRIVER = "DRIVER";
    public static final String OWNER_SHIPPER = "SHIPPER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    /** DRIVER / SHIPPER / CARRIER */
    @TableField("owner_type")
    private String ownerType;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_name")
    private String ownerName;

    /** 总余额 */
    @TableField("balance")
    private BigDecimal balance;

    /** 冻结金额 */
    @TableField("frozen")
    private BigDecimal frozen;

    /** 累计入账 */
    @TableField("total_in")
    private BigDecimal totalIn;

    /** 累计出账 */
    @TableField("total_out")
    private BigDecimal totalOut;

    /** ACTIVE / FROZEN */
    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getFrozen() { return frozen; }
    public void setFrozen(BigDecimal frozen) { this.frozen = frozen; }
    public BigDecimal getTotalIn() { return totalIn; }
    public void setTotalIn(BigDecimal totalIn) { this.totalIn = totalIn; }
    public BigDecimal getTotalOut() { return totalOut; }
    public void setTotalOut(BigDecimal totalOut) { this.totalOut = totalOut; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
