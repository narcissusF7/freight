package com.lukachangtong.freight.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电子合同：托运框架协议（FRAMEWORK）/ 司机运输合同（TRANSPORT）。
 * 签章只对 contentHash（SHA-256）签名，不向 CA 传合同明文；
 * 验签时重算摘要，摘要不一致即判定内容被篡改（大纲 1.2 证据链要求）。
 */
@TableName("ct_contract")
public class CtContract implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_FRAMEWORK = "FRAMEWORK";
    public static final String TYPE_TRANSPORT = "TRANSPORT";

    /** 0 待签 1 甲方已签 2 双方已签 3 已作废 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PARTY_A_SIGNED = 1;
    public static final int STATUS_BOTH_SIGNED = 2;
    public static final int STATUS_VOID = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("contract_no")
    private String contractNo;

    @TableField("contract_type")
    private String contractType;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("party_a")
    private String partyA;

    @TableField("party_b")
    private String partyB;

    @TableField("content")
    private String content;

    @TableField("content_hash")
    private String contentHash;

    @TableField("sign_status")
    private Integer signStatus;

    @TableField("signature_a")
    private String signatureA;

    @TableField("signature_b")
    private String signatureB;

    @TableField("cert_serial_no")
    private String certSerialNo;

    @TableField("sign_time")
    private LocalDateTime signTime;

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

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String v) { this.contractNo = v; }
    public String getContractType() { return contractType; }
    public void setContractType(String v) { this.contractType = v; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public String getPartyA() { return partyA; }
    public void setPartyA(String v) { this.partyA = v; }
    public String getPartyB() { return partyB; }
    public void setPartyB(String v) { this.partyB = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String v) { this.contentHash = v; }
    public Integer getSignStatus() { return signStatus; }
    public void setSignStatus(Integer v) { this.signStatus = v; }
    public String getSignatureA() { return signatureA; }
    public void setSignatureA(String v) { this.signatureA = v; }
    public String getSignatureB() { return signatureB; }
    public void setSignatureB(String v) { this.signatureB = v; }
    public String getCertSerialNo() { return certSerialNo; }
    public void setCertSerialNo(String v) { this.certSerialNo = v; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime v) { this.signTime = v; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String v) { this.createBy = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String v) { this.updateBy = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
}
