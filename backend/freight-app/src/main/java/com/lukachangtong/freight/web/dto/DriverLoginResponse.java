package com.lukachangtong.freight.web.dto;

import java.util.List;

/**
 * 司机端登录响应。
 *
 * 与后台 {@code LoginResponse} 分开：司机端要额外带 driverId、准入状态、名下车辆，
 * 后台登录塞这些字段会变成「谁的 App 都能读的公共结构」。
 */
public class DriverLoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;

    private Long driverId;
    private String name;
    private String phone;
    private Long orgId;
    /** 0 待审核 1 通过 2 驳回 */
    private Integer auditStatus;
    private String auditRemark;
    private Integer onlineStatus;
    private String plateNo;
    private List<Long> vehicleIds;
    /** 证件到期提醒条数（登录即提示，避免司机带过期证件接单） */
    private int certAlerts;
    private List<String> certAlertMessages;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public Integer getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(Integer onlineStatus) { this.onlineStatus = onlineStatus; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public List<Long> getVehicleIds() { return vehicleIds; }
    public void setVehicleIds(List<Long> vehicleIds) { this.vehicleIds = vehicleIds; }
    public int getCertAlerts() { return certAlerts; }
    public void setCertAlerts(int certAlerts) { this.certAlerts = certAlerts; }
    public List<String> getCertAlertMessages() { return certAlertMessages; }
    public void setCertAlertMessages(List<String> certAlertMessages) { this.certAlertMessages = certAlertMessages; }
}
