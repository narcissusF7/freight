package com.lukachangtong.freight.customer.dto;

/**
 * 处理完成：PROCESSING → RESOLVED。
 *
 * `liable` 是投诉与信用的唯一连接点：只有责任成立才扣分。
 * 不填按「不成立」处理——默认不冤枉人，是投诉系统应有的默认值。
 */
public class ComplaintResolveRequest {

    /** 处理结论，会作为反馈内容发给投诉人 */
    private String result;
    /** 责任是否成立 */
    private Boolean liable;
    private String remark;
    private String operator;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Boolean getLiable() { return liable; }
    public void setLiable(Boolean liable) { this.liable = liable; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
