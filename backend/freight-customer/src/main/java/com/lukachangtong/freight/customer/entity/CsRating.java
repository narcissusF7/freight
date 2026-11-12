package com.lukachangtong.freight.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 双向星级评价。
 *
 * `(waybill_id, direction)` 唯一：一张运单每个方向只能评一次。刷分的典型手法是「提交—差评—再提交」，
 * 唯一索引是唯一可靠的拦截点（应用层判断在校验与插入之间永远有窗口）。
 * 违规内容置 HIDDEN 而不是删除：评价是纠纷证据，删了就没法回溯。
 */
@TableName("cs_rating")
public class CsRating implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DIR_SHIPPER_TO_DRIVER = "SHIPPER_TO_DRIVER";
    public static final String DIR_DRIVER_TO_SHIPPER = "DRIVER_TO_SHIPPER";
    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_HIDDEN = "HIDDEN";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("rating_no")
    private String ratingNo;

    @TableField("org_id")
    private Long orgId;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    /** SHIPPER_TO_DRIVER / DRIVER_TO_SHIPPER */
    @TableField("direction")
    private String direction;

    /** DRIVER / SHIPPER */
    @TableField("rater_type")
    private String raterType;

    @TableField("rater_id")
    private Long raterId;

    @TableField("rater_name")
    private String raterName;

    /** 被评价方类型 */
    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("target_name")
    private String targetName;

    /** 总体星级 1~5 */
    @TableField("score")
    private Integer score;

    /** 准时 */
    @TableField("punctuality")
    private Integer punctuality;

    /** 服务 */
    @TableField("service")
    private Integer service;

    /** 货物完好 */
    @TableField("cargo")
    private Integer cargo;

    /** 沟通态度 */
    @TableField("attitude")
    private Integer attitude;

    /** 标签，逗号分隔 */
    @TableField("tags")
    private String tags;

    @TableField("content")
    private String content;

    /** 1 = 匿名展示 */
    @TableField("anonymous")
    private Integer anonymous;

    /** VALID / HIDDEN */
    @TableField("status")
    private String status;

    /** 被评价方回复 */
    @TableField("reply_content")
    private String replyContent;

    @TableField("reply_time")
    private LocalDateTime replyTime;

    @TableField("reply_by")
    private String replyBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRatingNo() { return ratingNo; }
    public void setRatingNo(String ratingNo) { this.ratingNo = ratingNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getWaybillNo() { return waybillNo; }
    public void setWaybillNo(String waybillNo) { this.waybillNo = waybillNo; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getRaterType() { return raterType; }
    public void setRaterType(String raterType) { this.raterType = raterType; }
    public Long getRaterId() { return raterId; }
    public void setRaterId(Long raterId) { this.raterId = raterId; }
    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
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
    public Integer getAnonymous() { return anonymous; }
    public void setAnonymous(Integer anonymous) { this.anonymous = anonymous; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }
    public LocalDateTime getReplyTime() { return replyTime; }
    public void setReplyTime(LocalDateTime replyTime) { this.replyTime = replyTime; }
    public String getReplyBy() { return replyBy; }
    public void setReplyBy(String replyBy) { this.replyBy = replyBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
