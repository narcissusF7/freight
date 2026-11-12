package com.lukachangtong.freight.report.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 驾驶员上报视图：组装「驾驶员信息单」报文所需的业务字段。 */
public class DriverReportRow {

    private Long id;
    private Long orgId;
    private String name;
    private String idCardNo;
    private String driverLicenseNo;
    private String phone;
    private LocalDate licenseExpireDate;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public String getDriverLicenseNo() { return driverLicenseNo; }
    public void setDriverLicenseNo(String driverLicenseNo) { this.driverLicenseNo = driverLicenseNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getLicenseExpireDate() { return licenseExpireDate; }
    public void setLicenseExpireDate(LocalDate licenseExpireDate) { this.licenseExpireDate = licenseExpireDate; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
