package com.lukachangtong.freight.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 信用主体当前分值。
 *
 * 分值是流水累加的结果，这张表只是缓存快照（`score = 100 + sum(delta)`）。
 * 之所以要缓存：查询「信用排行 / 低于 60 分名单」时不可能每次都去聚合流水；
 * 之所以能缓存：流水表是唯一真相，快照与流水不一致时以流水为准重算（对账思路同财务域）。
 */
@TableName("cs_credit_subject")
public class CsCreditSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SUBJECT_DRIVER = "DRIVER";
    public static final String SUBJECT_SHIPPER = "SHIPPER";
    public static final String LEVEL_A = "A";
    public static final String LEVEL_B = "B";
    public static final String LEVEL_C = "C";
    public static final String LEVEL_D = "D";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** DRIVER / SHIPPER */
    @TableField("subject_type")
    private String subjectType;

    @TableField("subject_id")
    private Long subjectId;

    @TableField("subject_name")
    private String subjectName;

    /** 信用分，满分 100 */
    @TableField("score")
    private Integer score;

    /** A≥90 / B≥75 / C≥60 / D<60 */
    @TableField("level")
    private String level;

    /** 好评数（≥4 星） */
    @TableField("good_count")
    private Integer goodCount;

    /** 中评（3 星） */
    @TableField("neutral_count")
    private Integer neutralCount;

    /** 差评（≤2 星） */
    @TableField("bad_count")
    private Integer badCount;

    /** 责任成立投诉次数 */
    @TableField("complaint_count")
    private Integer complaintCount;

    /** 累计评价数 */
    @TableField("total_count")
    private Integer totalCount;

    @TableField("last_change_time")
    private LocalDateTime lastChangeTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getGoodCount() { return goodCount; }
    public void setGoodCount(Integer goodCount) { this.goodCount = goodCount; }
    public Integer getNeutralCount() { return neutralCount; }
    public void setNeutralCount(Integer neutralCount) { this.neutralCount = neutralCount; }
    public Integer getBadCount() { return badCount; }
    public void setBadCount(Integer badCount) { this.badCount = badCount; }
    public Integer getComplaintCount() { return complaintCount; }
    public void setComplaintCount(Integer complaintCount) { this.complaintCount = complaintCount; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public LocalDateTime getLastChangeTime() { return lastChangeTime; }
    public void setLastChangeTime(LocalDateTime lastChangeTime) { this.lastChangeTime = lastChangeTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
