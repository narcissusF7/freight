package com.lukachangtong.freight.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具。密钥与有效期来自配置；生产必须由外部密钥管理下发，禁止硬编码。
 * 令牌荷载仅放非敏感标识（用户名、机构、角色），不放手机号等 PII。
 */
@Component
public class JwtTokenProvider {

    @Value("${freight.jwt.secret}")
    private String secret;

    @Value("${freight.jwt.expiration-seconds:7200}")
    private long expirationSeconds;

    private SecretKey key;

    @javax.annotation.PostConstruct
    public void init() {
        // HS256 要求密钥 >= 256 bit；secret 不足会在签发时报错，便于尽早暴露配置问题
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username, Long orgId, List<String> roles) {
        return generate(username, orgId, roles, null);
    }

    /**
     * 带扩展荷载的签发。司机端需要 driverId（用于鉴权后校验运单归属），
     * 这类标识放荷载是为了避免每个司机端接口都回查一次 cr_driver。
     */
    public String generate(String username, Long orgId, List<String> roles, Map<String, Object> extraClaims) {
        return generate(username, orgId, roles, extraClaims, expirationSeconds);
    }

    /**
     * 指定有效期的签发。司机端在途连续作业，令牌需要比后台长（默认 7 天），
     * 否则司机跑到半路会被踢回登录页。
     */
    public String generate(String username, Long orgId, List<String> roles,
                           Map<String, Object> extraClaims, long ttlSeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlSeconds * 1000L);
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("orgId", orgId)
                .claim("roles", String.join(",", roles))
                .setIssuedAt(now)
                .setExpiration(exp);
        if (extraClaims != null) {
            for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
                if (e.getValue() != null) {
                    builder.claim(e.getKey(), e.getValue());
                }
            }
        }
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
