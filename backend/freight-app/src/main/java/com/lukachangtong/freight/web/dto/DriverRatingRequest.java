package com.lukachangtong.freight.web.dto;

/**
 * 司机端评价货主入参。
 *
 * 没有 `driverId` / `direction` 字段：司机身份从令牌取，方向固定 DRIVER_TO_SHIPPER。
 * 让前端传这两个值，等于允许司机给别人的运单打分。
 */
public class DriverRatingRequest {

    private Long waybillId;
    private Integer score;
    private Integer punctuality;
    private Integer service;
    private Integer cargo;
    private Integer attitude;
    private String tags;
    private String content;
    private Boolean anonymous;

    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
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
}
