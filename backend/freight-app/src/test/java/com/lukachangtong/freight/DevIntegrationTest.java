package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实联调测试：以 dev 环境启动完整 Spring Boot 应用（随机端口真实 Tomcat），
 * 全程连接本机运行的 MySQL 8（freight 库，Flyway 建表 + DataInitializer 注入 admin）
 * 与 Redis 7（Redisson 客户端），通过真实 HTTP 链路验证 M1 业务骨架。
 *
 * 区别于 AuthWebTest（MockMvc + Mock AuthService + H2）：本测试不 Mock 任何业务组件，
 * 真正落库 MySQL、真正连接 Redis、真正走 JWT 过滤器与安全链路。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DevIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    /** 需要鉴权的 JSON 请求：Content-Type=JSON + Bearer 令牌。 */
    private HttpEntity<String> authJson(String token, String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    @Test
    void full_real_integration_against_mysql_and_redis() throws Exception {
        // 唯一后缀，保证多次运行不撞唯一键（org_code / username / role_code）
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 1) 登录：dev 环境 DataInitializer 已注入 admin/admin123
        String loginBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> loginResp = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(loginBody, jsonHeaders()), String.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode(), "login http 200");
        JsonNode loginJson = mapper.readTree(loginResp.getBody());
        assertEquals("00000", loginJson.path("code").asText(), "login 业务码 00000");
        String token = loginJson.path("data").path("token").asText();
        assertTrue(token != null && !token.isEmpty(), "登录应返回 JWT 令牌");
        assertEquals("admin", loginJson.path("data").path("username").asText());

        // 2) 无令牌访问受保护端点 -> 401（验证 SecurityConfig 显式 401 入口点）
        ResponseEntity<String> noToken = rest.getForEntity(baseUrl() + "/api/auth/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noToken.getStatusCode(), "无令牌应返回 401");

        // 3) 带令牌访问 /me -> 200 且身份正确
        ResponseEntity<String> meResp = rest.exchange(
                baseUrl() + "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertEquals(HttpStatus.OK, meResp.getStatusCode());
        JsonNode meJson = mapper.readTree(meResp.getBody());
        assertEquals("admin", meJson.path("data").path("username").asText());

        // 4) 创建机构（落 MySQL）
        String orgCode = "ORG_" + s;
        String orgBody = String.format(
                "{\"orgName\":\"测试机构%s\",\"orgCode\":\"%s\",\"status\":1,\"parentId\":0}", s, orgCode);
        ResponseEntity<String> orgResp = rest.exchange(
                baseUrl() + "/api/orgs", HttpMethod.POST,
                authJson(token, orgBody), String.class);
        assertEquals(HttpStatus.OK, orgResp.getStatusCode());
        long orgId = mapper.readTree(orgResp.getBody()).path("data").asLong();
        assertTrue(orgId > 0, "机构创建返回自增主键");

        // 5) 创建角色（落 MySQL）
        String roleCode = "ROLE_" + s;
        String roleBody = String.format(
                "{\"orgId\":%d,\"roleCode\":\"%s\",\"roleName\":\"测试角色%s\"}", orgId, roleCode, s);
        ResponseEntity<String> roleResp = rest.exchange(
                baseUrl() + "/api/roles", HttpMethod.POST,
                authJson(token, roleBody), String.class);
        assertEquals(HttpStatus.OK, roleResp.getStatusCode());
        long roleId = mapper.readTree(roleResp.getBody()).path("data").asLong();
        assertTrue(roleId > 0, "角色创建返回自增主键");

        // 6) 角色授权菜单（空集合也应成功）
        ResponseEntity<String> assignResp = rest.exchange(
                baseUrl() + "/api/roles/" + roleId + "/menus", HttpMethod.POST,
                authJson(token, "{\"menuIds\":[]}"), String.class);
        assertEquals(HttpStatus.OK, assignResp.getStatusCode());

        // 7) 创建菜单（落 MySQL）
        String menuBody = "{\"parentId\":0,\"menuName\":\"测试菜单" + s + "\",\"menuType\":\"M\",\"status\":1}";
        ResponseEntity<String> menuResp = rest.exchange(
                baseUrl() + "/api/menus", HttpMethod.POST,
                authJson(token, menuBody), String.class);
        assertEquals(HttpStatus.OK, menuResp.getStatusCode());
        long menuId = mapper.readTree(menuResp.getBody()).path("data").asLong();
        assertTrue(menuId > 0, "菜单创建返回自增主键");

        // 8) 创建员工账号并绑定角色（落 MySQL，密码 BCrypt 哈希）
        String username = "emp_" + s;
        String empBody = String.format(
                "{\"orgId\":%d,\"username\":\"%s\",\"password\":\"test123\",\"realName\":\"测试员工%s\",\"status\":1,\"roleIds\":[%d]}",
                orgId, username, s, roleId);
        ResponseEntity<String> empResp = rest.exchange(
                baseUrl() + "/api/employees", HttpMethod.POST,
                authJson(token, empBody), String.class);
        assertEquals(HttpStatus.OK, empResp.getStatusCode());
        long empId = mapper.readTree(empResp.getBody()).path("data").asLong();
        assertTrue(empId > 0, "员工创建返回自增主键");

        // 9) 文件上传：dev 环境 integration.mode=mock，走 MockFileClient，索引落 file_record
        MultiValueMap<String, Object> fileMap = new LinkedMultiValueMap<>();
        fileMap.add("orgId", orgId);
        fileMap.add("bizType", "CERT");
        byte[] content = "hello-integration".getBytes(StandardCharsets.UTF_8);
        ByteArrayResource fileRes = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };
        fileMap.add("file", fileRes);
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setBearerAuth(token);
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> fileResp = rest.postForEntity(
                baseUrl() + "/api/files/upload",
                new HttpEntity<>(fileMap, fileHeaders), String.class);
        assertEquals(HttpStatus.OK, fileResp.getStatusCode(), "文件上传 http 200");
        long fileId = mapper.readTree(fileResp.getBody()).path("data").asLong();
        assertTrue(fileId > 0, "文件索引返回自增主键");

        // 10) 取回文件访问地址
        ResponseEntity<String> urlResp = rest.exchange(
                baseUrl() + "/api/files/" + fileId + "/url", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertEquals(HttpStatus.OK, urlResp.getStatusCode());

        // 11) 刷新令牌链路
        ResponseEntity<String> refreshResp = rest.exchange(
                baseUrl() + "/api/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);
        assertEquals(HttpStatus.OK, refreshResp.getStatusCode());
        JsonNode refreshJson = mapper.readTree(refreshResp.getBody());
        assertTrue(refreshJson.path("data").path("token").asText().length() > 0, "刷新应返回新令牌");
    }
}
