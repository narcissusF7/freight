package com.lukachangtong.freight.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 信用变动流水。
 *
 * 每个业务的加减分都留一条：`(subject_type, subject_id, biz_type, biz_no)` 唯一，
 * 所以「投诉办结」「评价提交」被重复调用时不会把分扣两遍——幂等靠索引，不靠应用层判断。
 */
@TableName("cs_credit")
public class CsCredit implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BIZ_COMPLAINT = "COMPLAINT_CONFIRMED";
    public static final String BIZ_RATING_GOOD = "RATING_GOOD";
    public static final String BIZ_RATING_NEUTRAL = "RATING_NEUTRAL";
    public static final String BIZ_RATING_BAD = "RATING_BAD";
    public static final String BIZ_ADJUST = "ADJUST";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** DRIVER / SHIPPER */
    @TableField("subject_type")
    private String subjectType;

    @TableField("subject_id")
    private Long subjectId;

    @TableField("subject_name")
    private String subjectName;

    /** COMPLAINT_CONFIRMED / RATING_GOOD / RATING_NEUTRAL / RATING_BAD / ADJUST */
    @TableField("biz_type")
    private String bizType;

    /** 业务单号 */
    @TableField("biz_no")
    private String bizNo;

    /** 本次加减分 */
    @TableField("delta")
    private Integer delta;

    @TableField("score_before")
    private Integer scoreBefore;

    @TableField("score_after")
    private Integer scoreAfter;

    @TableField("level_before")
    private String levelBefore;

    @TableField("level_after")
    private String levelAfter;

    @TableField("reason")
    private String reason;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public Integer getScoreBefore() { return scoreBefore; }
    public void setScoreBefore(Integer scoreBefore) { this.scoreBefore = scoreBefore; }
    public Integer getScoreAfter() { return scoreAfter; }
    public void setScoreAfter(Integer scoreAfter) { this.scoreAfter = scoreAfter; }
    public String getLevelBefore() { return levelBefore; }
    public void setLevelBefore(String levelBefore) { this.levelBefore = levelBefore; }
    public String getLevelAfter() { return levelAfter; }
    public void setLevelAfter(String levelAfter) { this.levelAfter = levelAfter; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
