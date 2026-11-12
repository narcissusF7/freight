package com.lukachangtong.freight.contract.dto;

/** 创建合同请求。content 留空时由服务端按要素拼装，摘要基于拼装后的正文计算。 */
public class ContractSaveRequest {

    private Long orgId;
    /** FRAMEWORK / TRANSPORT */
    private String contractType;
    private Long waybillId;
    private String partyA;
    private String partyB;
    private String content;
    private String operateBy;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long v) { this.orgId = v; }
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
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String v) { this.operateBy = v; }
}
