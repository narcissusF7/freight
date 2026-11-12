package com.lukachangtong.freight.carrier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 司机准入信息（承运信息-司机管理）。
 * 敏感字段身份证/驾驶证仅作 M2 骨架存储，生产须加密存储 + 脱敏展示（见大纲 1.2 硬性合规约束）。
 */
@TableName("cr_driver")
public class CrDriver implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    @TableField("id_card_no")
    private String idCardNo;

    @TableField("driver_license_no")
    private String driverLicenseNo;

    /** 驾驶证有效期至（到期预警） */
    @TableField("license_expire_date")
    private LocalDate licenseExpireDate;

    /** 身份证有效期至（到期预警） */
    @TableField("id_card_expire_date")
    private LocalDate idCardExpireDate;

    /** 0 待审核 1 审核通过 2 驳回 */
    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    /** 司机端登录密码（BCrypt），null 表示未激活司机端账号 */
    @TableField("password_hash")
    private String passwordHash;

    @TableField("push_client_id")
    private String pushClientId;

    /** 接单在线状态 0 离线 1 在线（≠ 轨迹在线） */
    @TableField("online_status")
    private Integer onlineStatus;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("login_fail_count")
    private Integer loginFailCount;

    @TableField("lock_until")
    private LocalDateTime lockUntil;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public String getDriverLicenseNo() { return driverLicenseNo; }
    public void setDriverLicenseNo(String driverLicenseNo) { this.driverLicenseNo = driverLicenseNo; }
    public LocalDate getLicenseExpireDate() { return licenseExpireDate; }
    public void setLicenseExpireDate(LocalDate licenseExpireDate) { this.licenseExpireDate = licenseExpireDate; }
    public LocalDate getIdCardExpireDate() { return idCardExpireDate; }
    public void setIdCardExpireDate(LocalDate idCardExpireDate) { this.idCardExpireDate = idCardExpireDate; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPushClientId() { return pushClientId; }
    public void setPushClientId(String pushClientId) { this.pushClientId = pushClientId; }
    public Integer getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(Integer onlineStatus) { this.onlineStatus = onlineStatus; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public Integer getLoginFailCount() { return loginFailCount; }
    public void setLoginFailCount(Integer loginFailCount) { this.loginFailCount = loginFailCount; }
    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
