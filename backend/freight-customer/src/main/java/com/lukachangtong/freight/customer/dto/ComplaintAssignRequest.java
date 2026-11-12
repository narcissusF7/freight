package com.lukachangtong.freight.customer.dto;

/**
 * 分派受理人：PENDING → PROCESSING。
 */
public class ComplaintAssignRequest {

    /** 处理人姓名/工号 */
    private String assignTo;
    private String remark;
    private String operator;

    public String getAssignTo() { return assignTo; }
    public void setAssignTo(String assignTo) { this.assignTo = assignTo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
