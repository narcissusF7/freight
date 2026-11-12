package com.lukachangtong.freight.customer.dto;

/**
 * 被评价方回复（不改变评分，只补充说明）。
 */
public class RatingReplyRequest {

    private String content;
    private String operator;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
