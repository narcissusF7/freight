package com.lukachangtong.freight.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 上报配置（机构级）：交换代码 / 接口地址 / 令牌。
 *
 * 令牌由「身份验证接口」换取，规范有效期 2 小时，过期自动重取；
 * 配置与令牌都不进代码，避免测试账号密码泄入仓库（大纲硬性约束）。
 */
@TableName("rp_config")
public class RpConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("org_id")
    private Long orgId;
    @TableField("sender_code")
    private String senderCode;
    @TableField("receiver_code")
    private String receiverCode;
    @TableField("endpoint")
    private String endpoint;
    @TableField("auth_endpoint")
    private String authEndpoint;
    @TableField("public_key")
    private String publicKey;
    @TableField("token")
    private String token;
    @TableField("token_expire_time")
    private LocalDateTime tokenExpireTime;
    @TableField("enabled")
    private Integer enabled;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getSenderCode() { return senderCode; }
    public void setSenderCode(String senderCode) { this.senderCode = senderCode; }
    public String getReceiverCode() { return receiverCode; }
    public void setReceiverCode(String receiverCode) { this.receiverCode = receiverCode; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAuthEndpoint() { return authEndpoint; }
    public void setAuthEndpoint(String authEndpoint) { this.authEndpoint = authEndpoint; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
