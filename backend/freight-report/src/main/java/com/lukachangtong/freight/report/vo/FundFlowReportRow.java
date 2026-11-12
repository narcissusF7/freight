package com.lukachangtong.freight.report.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 资金流水上报视图：组装「资金流水单」报文所需的业务字段。 */
public class FundFlowReportRow {

    private Long id;
    private Long orgId;
    private String flowNo;
    private String bizNo;
    private String payeeName;
    private BigDecimal amount;
    private LocalDateTime payTime;
    private String payeeType;
    private String payType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getFlowNo() { return flowNo; }
    public void setFlowNo(String flowNo) { this.flowNo = flowNo; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public String getPayeeType() { return payeeType; }
    public void setPayeeType(String payeeType) { this.payeeType = payeeType; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
}
