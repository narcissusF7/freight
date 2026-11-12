package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.auth.dto.LoginRequest;
import com.lukachangtong.freight.auth.dto.LoginResponse;
import com.lukachangtong.freight.auth.dto.UserInfo;
import com.lukachangtong.freight.auth.service.AuthService;
import com.lukachangtong.freight.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader("Authorization") String authorization) {
        return Result.ok(authService.refresh(authorization));
    }

    @GetMapping("/me")
    public Result<UserInfo> me(@RequestHeader("Authorization") String authorization) {
        return Result.ok(authService.me(authorization));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // 无状态 JWT：服务端不维护会话，前端丢弃令牌即可；保留端点便于未来加黑名单
        return Result.ok(null);
    }
}
