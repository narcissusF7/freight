package com.lukachangtong.freight.auth.dto;

import java.util.List;

/** 员工（账号）创建/更新请求；密码为明文，服务端以 BCrypt 哈希存储。 */
public class EmployeeSaveRequest {

    private Long id;
    private Long orgId;
    private String username;
    private String password;
    private String realName;
    private String phoneEncrypted;
    private Integer status;
    private List<Long> roleIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhoneEncrypted() { return phoneEncrypted; }
    public void setPhoneEncrypted(String phoneEncrypted) { this.phoneEncrypted = phoneEncrypted; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
