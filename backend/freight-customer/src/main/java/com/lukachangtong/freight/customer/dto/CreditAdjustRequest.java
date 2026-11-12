package com.lukachangtong.freight.customer.dto;

/**
 * 人工调账：仅用于线下核实后的纠错，必须写原因。
 */
public class CreditAdjustRequest {

    /** DRIVER / SHIPPER */
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    /** 正数加分、负数扣分 */
    private Integer delta;
    private String reason;
    private String operator;

    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
