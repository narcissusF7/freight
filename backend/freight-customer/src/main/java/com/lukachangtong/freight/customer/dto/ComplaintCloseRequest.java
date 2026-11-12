package com.lukachangtong.freight.customer.dto;

/**
 * 办结：RESOLVED → CLOSED，收集投诉人满意度（1~5）。
 */
public class ComplaintCloseRequest {

    /** 1~5 */
    private Integer satisfaction;
    private String remark;
    private String operator;

    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
