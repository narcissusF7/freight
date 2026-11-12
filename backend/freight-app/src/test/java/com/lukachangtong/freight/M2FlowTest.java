package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.infra.mapper.OutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 真实联调测试：承运准入 + 运单主线。沿用 DevIntegrationTest 的真实 MySQL + Redis 环境，
 * 验证司机/车辆 CRUD 与审核、发布货源、生成运单、状态机合法/非法跃迁、节点留痕、Outbox 同事务写入。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class M2FlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() { return "http://localhost:" + port; }

    private HttpHeaders jsonToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private String login() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> resp = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(body, jsonToken(null)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").path("token").asText();
    }

    private long orgIdOf(String token) throws Exception {
        ResponseEntity<String> me = rest.exchange(
                baseUrl() + "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        return mapper.readTree(me.getBody()).path("data").path("orgId").asLong();
    }

    private long createDriver(String token, long orgId, String suffix) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"name\":\"司机%s\",\"phone\":\"1380000%s\",\"idCardNo\":\"350000%s\",\"driverLicenseNo\":\"D%s\"}",
                orgId, suffix, suffix, suffix, suffix);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/drivers", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private long createVehicle(String token, long orgId, long driverId, String suffix) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"plateNo\":\"MINI%s\",\"vehicleType\":\"高栏\",\"ownerName\":\"车主%s\",\"capacityTon\":10.5,\"axleCount\":3,\"driverId\":%d}",
                orgId, suffix, suffix, driverId);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/vehicles", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private long publishGoods(String token, long orgId, String suffix) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"goodsName\":\"煤炭%s\",\"freightPrice\":3500.00,\"loadAddr\":\"山西\",\"unloadAddr\":\"江苏\"}",
                orgId, suffix);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private long createWaybill(String token, long orgId, long gs, long drv, long veh) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"goodsSourceId\":%d,\"driverId\":%d,\"vehicleId\":%d,\"operateBy\":\"tester\"}",
                orgId, gs, drv, veh);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/waybills", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private void changeStatus(String token, long wbId, String toStatus) {
        String body = String.format("{\"toStatus\":\"%s\",\"operateBy\":\"tester\",\"remark\":\"step\"}", toStatus);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/waybills/" + wbId + "/status", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "状态流转 " + toStatus + " 应成功");
    }

    private String waybillStatus(String token, long wbId) throws Exception {
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/waybills/" + wbId, HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").path("status").asText();
    }

    @Test
    void carrier_and_waybill_full_flow() throws Exception {
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String token = login();
        long orgId = orgIdOf(token);
        assertTrue(orgId > 0);

        // 承运准入：司机 + 车辆 创建并审核通过
        long drv = createDriver(token, orgId, s);
        long veh = createVehicle(token, orgId, drv, s);
        // 审核通过
        rest.postForEntity(baseUrl() + "/api/drivers/" + drv + "/audit?status=1&operator=tester",
                new HttpEntity<>(jsonToken(token)), String.class);
        rest.postForEntity(baseUrl() + "/api/vehicles/" + veh + "/audit?status=1&operator=tester",
                new HttpEntity<>(jsonToken(token)), String.class);
        // 列表应含新建记录
        ResponseEntity<String> drvList = rest.exchange(baseUrl() + "/api/drivers", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertTrue(mapper.readTree(drvList.getBody()).path("data").size() > 0);

        // 发布货源
        long gs = publishGoods(token, orgId, s);
        assertTrue(gs > 0);

        // 生成运单（草稿）
        long wb = createWaybill(token, orgId, gs, drv, veh);
        assertTrue(wb > 0);
        assertEquals("DRAFT", waybillStatus(token, wb));

        // 状态机主线合法流转：DRAFT -> ... -> COMPLETED
        String[] chain = {"PUBLISHED", "ACCEPTED", "LOADED", "IN_TRANSIT", "UNLOADED",
                "SIGNED", "RECEIPT_UPLOADED", "SETTLED", "REPORTED", "COMPLETED"};
        for (String st : chain) {
            changeStatus(token, wb, st);
            assertEquals(st, waybillStatus(token, wb));
        }

        // 节点留痕：初始草稿节点(1) + 10 次流转 = 11
        ResponseEntity<String> nodesResp = rest.exchange(
                baseUrl() + "/api/waybills/" + wb + "/nodes", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, nodesResp.getStatusCode());
        assertEquals(11, mapper.readTree(nodesResp.getBody()).path("data").size());

        // Outbox 同事务写入：状态变更事件 >= 10
        long outboxCount = outboxEventMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        assertTrue(outboxCount >= 10, "状态流转应写入 >=10 条 outbox 事件，实际 " + outboxCount);
    }

    @Test
    void illegal_status_transfer_rejected() throws Exception {
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String token = login();
        long orgId = orgIdOf(token);
        long drv = createDriver(token, orgId, s);
        long veh = createVehicle(token, orgId, drv, s);
        long gs = publishGoods(token, orgId, s);
        long wb = createWaybill(token, orgId, gs, drv, veh);

        // DRAFT 直接跳到 SIGNED 属非法跃迁 -> 应被拒绝（B0002）
        String body = "{\"toStatus\":\"SIGNED\",\"operateBy\":\"tester\",\"remark\":\"illegal\"}";
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/waybills/" + wb + "/status", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "http 仍 200（业务码承载错误）");
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("B0002", json.path("code").asText(), "非法状态流转应返回 B0002");
        // 状态不应改变
        assertEquals("DRAFT", waybillStatus(token, wb));
    }
}
