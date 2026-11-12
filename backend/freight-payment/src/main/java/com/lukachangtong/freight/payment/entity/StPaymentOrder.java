package com.lukachangtong.freight.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银企直联支付单。
 *
 * 状态机：PENDING → PAYING → SUCCESS；PAYING → FAILED →（重试）PAYING…
 * CLOSED 为人工终止。
 *
 * `pay_no` 同时作为银行侧的幂等 requestId——重试同一笔支付时不能换号，
 * 否则银行会当两笔付出去。
 */
@TableName("st_payment_order")
public class StPaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String TYPE_SETTLE = "SETTLE";
    public static final String TYPE_WITHDRAW = "WITHDRAW";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("pay_no")
    private String payNo;

    @TableField("org_id")
    private Long orgId;

    @TableField("pay_type")
    private String payType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("biz_no")
    private String bizNo;

    @TableField("payee_type")
    private String payeeType;

    @TableField("payee_id")
    private Long payeeId;

    @TableField("payee_name")
    private String payeeName;

    @TableField("payee_account")
    private String payeeAccount;

    @TableField("bank_code")
    private String bankCode;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("status")
    private String status;

    @TableField("bank_serial_no")
    private String bankSerialNo;

    @TableField("tls_protocol")
    private String tlsProtocol;

    @TableField("fail_reason")
    private String failReason;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("execute_time")
    private LocalDateTime executeTime;

    @TableField("callback_time")
    private LocalDateTime callbackTime;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBankSerialNo() { return bankSerialNo; }
    public void setBankSerialNo(String bankSerialNo) { this.bankSerialNo = bankSerialNo; }
    public String getTlsProtocol() { return tlsProtocol; }
    public void setTlsProtocol(String tlsProtocol) { this.tlsProtocol = tlsProtocol; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }
    public LocalDateTime getCallbackTime() { return callbackTime; }
    public void setCallbackTime(LocalDateTime callbackTime) { this.callbackTime = callbackTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
