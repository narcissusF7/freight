package com.lukachangtong.freight.auth.service;

import com.lukachangtong.freight.auth.dto.LoginRequest;
import com.lukachangtong.freight.auth.dto.LoginResponse;
import com.lukachangtong.freight.auth.dto.UserInfo;

/**
 * 认证服务：登录签发 JWT、刷新、当前用户。
 * 仅做无状态 JWT，会话不落地；权限校验由 JwtAuthenticationFilter + Security 完成。
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(String bearerToken);

    UserInfo me(String bearerToken);
}
