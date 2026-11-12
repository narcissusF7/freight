package com.lukachangtong.freight.customer.dto;

/**
 * 评价提交入参。
 *
 * `direction` 决定谁是评价人：SHIPPER_TO_DRIVER 时 rater 必须是该运单货主，
 * DRIVER_TO_SHIPPER 时必须是该运单司机。服务端按运单校验，不接受前端自说自话。
 */
public class RatingSubmitRequest {

    private Long waybillId;
    /** SHIPPER_TO_DRIVER / DRIVER_TO_SHIPPER */
    private String direction;
    /** 不填则按 direction 从运单推导 */
    private Long raterId;
    private String raterName;
    /** 总体星级 1~5 */
    private Integer score;
    /** 1~5 */
    private Integer punctuality;
    /** 1~5 */
    private Integer service;
    /** 1~5 */
    private Integer cargo;
    /** 1~5 */
    private Integer attitude;
    /** 逗号分隔 */
    private String tags;
    private String content;
    private Boolean anonymous;
    private String operator;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Long getRaterId() { return raterId; }
    public void setRaterId(Long raterId) { this.raterId = raterId; }
    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getPunctuality() { return punctuality; }
    public void setPunctuality(Integer punctuality) { this.punctuality = punctuality; }
    public Integer getService() { return service; }
    public void setService(Integer service) { this.service = service; }
    public Integer getCargo() { return cargo; }
    public void setCargo(Integer cargo) { this.cargo = cargo; }
    public Integer getAttitude() { return attitude; }
    public void setAttitude(Integer attitude) { this.attitude = attitude; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
