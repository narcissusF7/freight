package com.lukachangtong.freight;

import com.lukachangtong.freight.auth.dto.LoginResponse;
import com.lukachangtong.freight.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web 层冒烟测试：验证登录端点可放行并返回令牌，且受保护端点无令牌时返回 401。
 * AuthService 被 Mock，不触碰数据库；安全链路与 JWT 过滤器为真实装配。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("unittest")
class AuthWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void login_success_returns_token() throws Exception {
        LoginResponse resp = new LoginResponse();
        resp.setToken("mock-jwt-token");
        resp.setUsername("admin");
        resp.setExpiresIn(7200L);
        resp.setRoles(Arrays.asList("SUPER_ADMIN"));
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    void secured_endpoint_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/orgs/tree"))
                .andExpect(status().isUnauthorized());
    }
}
