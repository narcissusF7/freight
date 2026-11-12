package com.lukachangtong.freight.web.security;

import com.lukachangtong.freight.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 无状态 JWT 鉴权过滤器：从 Authorization 头解析令牌并写入 SecurityContext。
 * 登录入口（/api/auth/login）在 SecurityConfig 中放行，因此未携带令牌时直接放行由后续拒绝。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            if (jwtTokenProvider.validate(token)) {
                try {
                    Claims claims = jwtTokenProvider.parse(token);
                    String username = claims.getSubject();
                    String rolesStr = claims.get("roles", String.class);
                    List<String> roles = (rolesStr == null || rolesStr.isEmpty())
                            ? new ArrayList<String>()
                            : Arrays.asList(rolesStr.split(","));
                    List<GrantedAuthority> authorities = roles.stream()
                            .filter(r -> r != null && !r.isEmpty())
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception ignored) {
                    // 解析异常则视为未认证，由 Security 链路拒绝
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
