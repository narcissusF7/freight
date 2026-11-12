package com.lukachangtong.freight.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.common.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 司机端令牌的作用域约束。
 *
 * 为什么需要它：`hasRole('DRIVER')` 只能保证「司机能进司机端」，
 * 但任何一个已认证用户默认都能调后台的 /api/waybills/**——司机拿自己的令牌
 * 就能改任意运单状态、看全机构数据。这在验收和等保测评里都是直接违规项。
 *
 * 所以这里做反向限制：带 ROLE_DRIVER 的令牌只允许访问司机端接口与文件下载，
 * 其它路径一律 403。后台角色不受影响。
 */
@Component
public class DriverScopeFilter extends OncePerRequestFilter {

    private static final String ROLE_DRIVER = "ROLE_DRIVER";

    /** 司机令牌允许访问的前缀：自己的接口 + 文件（回单/凭证图片下载） */
    private static final List<String> ALLOWED_PREFIXES = Arrays.asList(
            "/api/driver/", "/api/files/");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && isDriverOnly(auth)) {
            String path = request.getRequestURI();
            boolean allowed = false;
            for (String prefix : ALLOWED_PREFIXES) {
                if (path.startsWith(prefix)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.fail(ErrorCode.FORBIDDEN, "司机端账号无权访问该接口")));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDriverOnly(Authentication auth) {
        List<String> roles = new ArrayList<String>();
        for (GrantedAuthority a : auth.getAuthorities()) {
            roles.add(a.getAuthority());
        }
        return roles.contains(ROLE_DRIVER);
    }
}
