package com.lukachangtong.freight.settlement.dto;

/** 开票申请。金额与税额一律取结算单，前端不可传。 */
public class InvoiceIssueRequest {

    private Long settlementId;
    private String title;
    private String taxNo;
    private String operator;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
