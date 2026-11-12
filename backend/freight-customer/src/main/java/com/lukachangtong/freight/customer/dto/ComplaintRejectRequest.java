package com.lukachangtong.freight.customer.dto;

/**
 * 驳回：PENDING/PROCESSING → REJECTED（投诉不成立）。
 */
public class ComplaintRejectRequest {

    private String reason;
    private String operator;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
