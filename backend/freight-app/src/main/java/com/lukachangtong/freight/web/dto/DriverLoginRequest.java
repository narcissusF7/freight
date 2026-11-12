package com.lukachangtong.freight.web.dto;

/** 司机端登录入参：手机号 + 密码（验证码登录留作后续接短信网关）。 */
public class DriverLoginRequest {

    private String phone;
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
