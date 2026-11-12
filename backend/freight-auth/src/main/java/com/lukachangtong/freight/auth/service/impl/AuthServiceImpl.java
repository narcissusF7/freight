package com.lukachangtong.freight.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.dto.LoginRequest;
import com.lukachangtong.freight.auth.dto.LoginResponse;
import com.lukachangtong.freight.auth.dto.UserInfo;
import com.lukachangtong.freight.auth.entity.SysRole;
import com.lukachangtong.freight.auth.entity.SysUser;
import com.lukachangtong.freight.auth.entity.SysUserRole;
import com.lukachangtong.freight.auth.mapper.SysRoleMapper;
import com.lukachangtong.freight.auth.mapper.SysUserMapper;
import com.lukachangtong.freight.auth.mapper.SysUserRoleMapper;
import com.lukachangtong.freight.auth.security.JwtTokenProvider;
import com.lukachangtong.freight.auth.service.AuthService;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private JwtTokenProvider jwtTokenProvider;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${freight.jwt.expiration-seconds:7200}")
    private long expirationSeconds;

    private static final String BAD_CREDENTIALS = "用户名或密码错误";

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", request.getUsername()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        List<String> roles = loadRoleCodes(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return buildResponse(user, roles);
    }

    @Override
    public LoginResponse refresh(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!jwtTokenProvider.validate(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        String username = jwtTokenProvider.parse(token).getSubject();
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在或已禁用");
        }
        return buildResponse(user, loadRoleCodes(user.getId()));
    }

    @Override
    public UserInfo me(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!jwtTokenProvider.validate(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        String username = jwtTokenProvider.parse(token).getSubject();
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        UserInfo info = new UserInfo();
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setOrgId(user.getOrgId());
        info.setRoles(loadRoleCodes(user.getId()));
        return info;
    }

    private LoginResponse buildResponse(SysUser user, List<String> roles) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtTokenProvider.generate(user.getUsername(), user.getOrgId(), roles));
        resp.setExpiresIn(expirationSeconds);
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setOrgId(user.getOrgId());
        resp.setRoles(roles);
        return resp;
    }

    private List<String> loadRoleCodes(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new QueryWrapper<SysUserRole>().eq("user_id", userId));
        if (relations.isEmpty()) {
            return new ArrayList<String>();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    private String stripBearer(String bearerToken) {
        if (bearerToken == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少令牌");
        }
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length()).trim();
        }
        return bearerToken.trim();
    }
}
