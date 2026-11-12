package com.lukachangtong.freight.customer.dto;

/**
 * 跟进记录：不改状态，只追加一条处理过程（电话联系、补充材料等）。
 */
public class ComplaintFollowRequest {

    private String content;
    private String imageUrls;
    private String operator;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
