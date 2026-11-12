package com.lukachangtong.freight.web.dto;

/** 首次开通司机端账号：手机号 + 短信验证码 + 新密码。 */
public class DriverPasswordInitRequest {

    private String phone;
    private String smsCode;
    private String newPassword;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSmsCode() { return smsCode; }
    public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
