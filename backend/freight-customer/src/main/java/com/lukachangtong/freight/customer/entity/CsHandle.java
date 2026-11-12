package com.lukachangtong.freight.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 投诉处理记录（只追加）。
 *
 * 投诉的价值一半在结论、一半在过程：谁在几点接的单、几点联系了投诉人、几点给出方案，
 * 这些是监管检查「是否 24 小时内处理并反馈」的直接证据，因此不做更新、只做插入。
 */
@TableName("cs_handle")
public class CsHandle implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_CREATE = "CREATE";
    public static final String TYPE_ASSIGN = "ASSIGN";
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_RESOLVE = "RESOLVE";
    public static final String TYPE_REJECT = "REJECT";
    public static final String TYPE_CLOSE = "CLOSE";
    public static final String TYPE_ESCALATE = "ESCALATE";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("complaint_id")
    private Long complaintId;

    /** ACCEPT/ASSIGN/FOLLOW/RESOLVE/REJECT/CLOSE/ESCALATE */
    @TableField("handle_type")
    private String handleType;

    @TableField("content")
    private String content;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("handle_by")
    private String handleBy;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public String getHandleType() { return handleType; }
    public void setHandleType(String handleType) { this.handleType = handleType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String handleBy) { this.handleBy = handleBy; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }
}
