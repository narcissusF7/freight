package com.lukachangtong.freight.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 无状态安全配置：关闭 CSRF 与会话，JWT 过滤器前置。
 *
 * 两条独立的入口：
 * - 后台：/api/auth/login + 其余 /api/**（任意已认证角色）；
 * - 司机端：/api/driver/login（放行）+ /api/driver/**（必须 ROLE_DRIVER）。
 *
 * 再加 DriverScopeFilter 做反向限制，避免司机令牌越权调后台接口。
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DriverScopeFilter driverScopeFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          DriverScopeFilter driverScopeFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.driverScopeFilter = driverScopeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                    .antMatchers("/api/auth/login").permitAll()
                    .antMatchers("/api/driver/login", "/api/driver/password/init").permitAll()
                    .antMatchers("/api/driver/**").hasRole("DRIVER")
                    .antMatchers("/api/files/**").authenticated()
                    .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint((request, response, authEx) ->
                            response.sendError(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                    .accessDeniedHandler((request, response, deniedEx) ->
                            response.sendError(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(driverScopeFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
