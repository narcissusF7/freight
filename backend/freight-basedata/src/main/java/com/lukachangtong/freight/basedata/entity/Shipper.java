package com.lukachangtong.freight.basedata.entity;

/**
 * M1 骨架实体：托运人（货主），字段与 V1__init.sql 的 bd_shipper 对齐。
 * TODO(M2): 补营业执照 OCR 识别结果、审核流与准入状态。
 */
public class Shipper {

    private Long id;
    private Long orgId;
    private String name;
    private String creditCode;
    private String legalPersonEncrypted;
    private Integer auditStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getLegalPersonEncrypted() { return legalPersonEncrypted; }
    public void setLegalPersonEncrypted(String legalPersonEncrypted) { this.legalPersonEncrypted = legalPersonEncrypted; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
}
