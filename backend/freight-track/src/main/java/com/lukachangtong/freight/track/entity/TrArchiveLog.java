package com.lukachangtong.freight.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 轨迹冷数据归档审计。每次 dryRun/归档/删除分区都留一条，便于事后回答
 * 「某年某月的数据去哪了」——监管抽查时必须能定位到归档文件。
 */
@TableName("tr_archive_log")
public class TrArchiveLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRY_RUN = "DRY_RUN";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_PURGED = "PURGED";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("partition_name")
    private String partitionName;

    @TableField("period_start")
    private LocalDate periodStart;

    @TableField("period_end")
    private LocalDate periodEnd;

    @TableField("row_count")
    private Long rowCount;

    @TableField("file_key")
    private String fileKey;

    @TableField("status")
    private String status;

    @TableField("message")
    private String message;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getPartitionName() { return partitionName; }
    public void setPartitionName(String v) { this.partitionName = v; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate v) { this.periodStart = v; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate v) { this.periodEnd = v; }
    public Long getRowCount() { return rowCount; }
    public void setRowCount(Long v) { this.rowCount = v; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String v) { this.fileKey = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
