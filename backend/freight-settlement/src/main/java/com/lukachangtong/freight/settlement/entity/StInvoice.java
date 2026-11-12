package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 电子发票。
 *
 * `request_id` 唯一，保证同一张结算单点十次「开票」只出一张票。
 * 红冲不物理删除，置 VOID 并留原因——发票是税务凭证，不能删。
 */
@TableName("st_invoice")
public class StInvoice implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ISSUED = "ISSUED";
    public static final String STATUS_VOID = "VOID";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发票号码 */
    @TableField("invoice_no")
    private String invoiceNo;

    @TableField("invoice_code")
    private String invoiceCode;

    @TableField("org_id")
    private Long orgId;

    /** PAYABLE / RECEIVABLE */
    @TableField("biz_type")
    private String bizType;

    @TableField("settlement_id")
    private Long settlementId;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("waybill_no")
    private String waybillNo;

    /** 发票抬头 */
    @TableField("title")
    private String title;

    /** 纳税人识别号 */
    @TableField("tax_no")
    private String taxNo;

    /** 含税金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 不含税金额 */
    @TableField("net_amount")
    private BigDecimal netAmount;

    /** 税额 */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    @TableField("tax_rate")
    private BigDecimal taxRate;

    /** PENDING/ISSUED/VOID/FAILED */
    @TableField("status")
    private String status;

    @TableField("pdf_url")
    private String pdfUrl;

    /** 开票幂等键 */
    @TableField("request_id")
    private String requestId;

    @TableField("issued_time")
    private LocalDateTime issuedTime;

    @TableField("void_time")
    private LocalDateTime voidTime;

    @TableField("void_reason")
    private String voidReason;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getIssuedTime() { return issuedTime; }
    public void setIssuedTime(LocalDateTime issuedTime) { this.issuedTime = issuedTime; }
    public LocalDateTime getVoidTime() { return voidTime; }
    public void setVoidTime(LocalDateTime voidTime) { this.voidTime = voidTime; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
