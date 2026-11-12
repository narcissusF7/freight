package com.lukachangtong.freight.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 上报补偿队列：失败退避重试。
 *
 * 一条上报记录失败后进补偿，按指数退避（1min → 5min → 30min → …）重试，
 * 超过最大次数标记 ABANDONED，由人工介入——不会无限重试打爆监管接口。
 */
@TableName("rp_retry")
public class RpRetry {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_ABANDONED = "ABANDONED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("record_id")
    private Long recordId;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("max_retry")
    private Integer maxRetry;
    @TableField("fail_reason")
    private String failReason;
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;
    @TableField("status")
    private String status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
