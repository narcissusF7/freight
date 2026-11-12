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
 * 结算日汇总（日终任务写入，看板与月结用）。
 */
@TableName("st_summary")
public class StSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_date")
    private LocalDate bizDate;

    @TableField("org_id")
    private Long orgId;

    @TableField("biz_type")
    private String bizType;

    @TableField("settle_count")
    private Integer settleCount;

    @TableField("settle_amount")
    private BigDecimal settleAmount;

    @TableField("paid_count")
    private Integer paidCount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Integer getSettleCount() { return settleCount; }
    public void setSettleCount(Integer settleCount) { this.settleCount = settleCount; }
    public BigDecimal getSettleAmount() { return settleAmount; }
    public void setSettleAmount(BigDecimal settleAmount) { this.settleAmount = settleAmount; }
    public Integer getPaidCount() { return paidCount; }
    public void setPaidCount(Integer paidCount) { this.paidCount = paidCount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
