package com.lukachangtong.freight.customer.dto;

/**
 * 投诉建单入参。
 *
 * 被投诉对象可以只传 `waybillId`：服务端会从运单解析出司机/货主，避免前端猜 ID 猜错人。
 * 金额、时限、状态一律服务端生成——投诉单一旦能由前端指定 SLA，24 小时考核就形同虚设。
 */
public class ComplaintCreateRequest {

    /** DRIVER_APP / SHIPPER_APP / WEB / PHONE，默认 WEB */
    /** 所属机构；不关联运单时必须显式传，否则无法确定归属 */
    private Long orgId;
    private String source;
    /** DRIVER / SHIPPER / OTHER */
    private String complainantType;
    private Long complainantId;
    private String complainantName;
    private String complainantPhone;
    /** DRIVER / SHIPPER / PLATFORM */
    private String targetType;
    private Long targetId;
    private String targetName;
    /** 相关运单，可不填 */
    private Long waybillId;
    /** DELAY / DAMAGE / ATTITUDE / FEE / LOST / OTHER */
    private String complaintType;
    /** NORMAL / URGENT */
    private String level;
    private String content;
    /** 逗号分隔 */
    private String imageUrls;
    private String operator;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getComplainantType() { return complainantType; }
    public void setComplainantType(String complainantType) { this.complainantType = complainantType; }
    public Long getComplainantId() { return complainantId; }
    public void setComplainantId(Long complainantId) { this.complainantId = complainantId; }
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    public String getComplainantPhone() { return complainantPhone; }
    public void setComplainantPhone(String complainantPhone) { this.complainantPhone = complainantPhone; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { this.complaintType = complaintType; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
