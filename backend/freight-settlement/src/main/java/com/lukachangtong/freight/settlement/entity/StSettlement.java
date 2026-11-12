package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 结算单（应付司机 / 应收货主）。
 *
 * 状态机：DRAFT → CONFIRMED → PAYING → PAID；PAID → INVOICED → CLOSED；任意非 PAID 可 VOID。
 * 金额一致性：amount = base + load + unload + wait + oil - deduct（含税）；
 * net = amount / (1 + taxRate)；tax = amount - net。**税额由差额得出**，不是各自四舍五入再相减，
 * 否则财务账与发票账会差几分钱，日终对账永远平不了。
 */
@TableName("st_settlement")
public class StSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_INVOICED = "INVOICED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_VOID = "VOID";
    public static final String TYPE_PAYABLE = "PAYABLE";
    public static final String TYPE_RECEIVABLE = "RECEIVABLE";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 结算单号，全局唯一 */
    @TableField("settlement_no")
    private String settlementNo;

    @TableField("org_id")
    private Long orgId;

    /** PAYABLE 应付司机 / RECEIVABLE 应收货主 */
    @TableField("biz_type")
    private String bizType;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    /** DRIVER / SHIPPER / CARRIER */
    @TableField("partner_type")
    private String partnerType;

    /** 结算对象 ID */
    @TableField("partner_id")
    private Long partnerId;

    @TableField("partner_name")
    private String partnerName;

    /** 基础运费（含税） */
    @TableField("base_amount")
    private BigDecimal baseAmount;

    /** 装货费 */
    @TableField("load_fee")
    private BigDecimal loadFee;

    /** 卸货费 */
    @TableField("unload_fee")
    private BigDecimal unloadFee;

    /** 等待/压车费 */
    @TableField("wait_fee")
    private BigDecimal waitFee;

    /** 油补 */
    @TableField("oil_subsidy")
    private BigDecimal oilSubsidy;

    /** 扣款，正数参与扣减 */
    @TableField("deduct_amount")
    private BigDecimal deductAmount;

    /** 增值税率 */
    @TableField("tax_rate")
    private BigDecimal taxRate;

    /** 结算总额（含税） */
    @TableField("amount")
    private BigDecimal amount;

    /** 不含税金额 */
    @TableField("net_amount")
    private BigDecimal netAmount;

    /** 税额 */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /** DRAFT/CONFIRMED/PAYING/PAID/INVOICED/CLOSED/VOID */
    @TableField("status")
    private String status;

    /** 结算业务日期，日终对账按此归集 */
    @TableField("settled_date")
    private LocalDate settledDate;

    @TableField("paid_time")
    private LocalDateTime paidTime;

    @TableField("remark")
    private String remark;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getSettledDate() { return settledDate; }
    public void setSettledDate(LocalDate settledDate) { this.settledDate = settledDate; }
    public LocalDateTime getPaidTime() { return paidTime; }
    public void setPaidTime(LocalDateTime paidTime) { this.paidTime = paidTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
