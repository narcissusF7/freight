package com.lukachangtong.freight.waybill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运单回单（签收凭证）。回单是结算与监管上报的前置材料，
 * 上传成功且运单处于 SIGNED 时自动推进到 RECEIPT_UPLOADED。
 */
@TableName("wb_receipt")
public class WbReceipt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("file_key")
    private String fileKey;

    @TableField("file_name")
    private String fileName;

    @TableField("file_url")
    private String fileUrl;

    @TableField("upload_by")
    private String uploadBy;

    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /** 0 待核验 1 已核验 */
    @TableField("verified")
    private Integer verified;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long v) { this.waybillId = v; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String v) { this.fileKey = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public String getUploadBy() { return uploadBy; }
    public void setUploadBy(String v) { this.uploadBy = v; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime v) { this.uploadTime = v; }
    public Integer getVerified() { return verified; }
    public void setVerified(Integer v) { this.verified = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
