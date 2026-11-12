package com.lukachangtong.freight.waybill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运单异常件（只追加，处理结果回写同一行）。
 * 上报异常会把运单状态推进到 EXCEPTION（状态机允许时），并留节点与 Outbox 事件。
 */
@TableName("wb_exception")
public class WbException implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("waybill_id")
    private Long waybillId;

    /** CARGO_DAMAGE / DELAY / ACCIDENT / ADDRESS / OTHER */
    @TableField("exception_type")
    private String exceptionType;

    @TableField("description")
    private String description;

    /** 逗号分隔的图片 URL */
    @TableField("image_urls")
    private String imageUrls;

    @TableField("report_by")
    private String reportBy;

    @TableField("report_time")
    private LocalDateTime reportTime;

    /** 0 待处理 1 已处理 */
    @TableField("status")
    private Integer status;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("handle_by")
    private String handleBy;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String v) { this.exceptionType = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String v) { this.imageUrls = v; }
    public String getReportBy() { return reportBy; }
    public void setReportBy(String v) { this.reportBy = v; }
    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime v) { this.reportTime = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String v) { this.handleRemark = v; }
    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String v) { this.handleBy = v; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime v) { this.handleTime = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
