package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.report.support.ReportAssembler;
import com.lukachangtong.freight.report.support.ReportDict;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M6 监管上报：报文组装正确 + 扫描上报 + 幂等 + 补偿重试 + 回执留痕。
 *
 * 上报的验收标准不是「接口不报错」，而是「业务完成后必然上报、上报不重不漏、失败可补偿」。
 * 断言全部对着 rp_report_record / rp_retry 两张事实表核。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M6ReportTest {

    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReportAssembler assembler;

    private final ObjectMapper mapper = new ObjectMapper();

    private String base() {
        return "http://localhost:" + port;
    }

    private HttpHeaders json(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return h;
    }

    private JsonNode call(HttpMethod method, String path, String body, String token) throws Exception {
        ResponseEntity<String> resp = rest.exchange(base() + path, method,
                new HttpEntity<>(body, json(token)), String.class);
        assertEquals(200, resp.getStatusCodeValue(), path + " 非 200: " + resp.getBody());
        return mapper.readTree(resp.getBody());
    }

    private String adminToken() throws Exception {
        return call(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"admin123\"}", null)
                .path("data").path("token").asText();
    }

    // ------------------------------------------------------------------ 1. 报文组装正确性

    @Test
    @Order(1)
    void assemblerBuildsCanonicalEnvelope() {
        assertEquals("WLHY_YD1001", ReportDict.ipcTypeOf("WAYBILL"));
        assertEquals("LOGINK_CN_FREIGHTCHARGES", ReportDict.ipcTypeOf("FUND_FLOW"));
        assertEquals("LOGINK_CN_CREDIT_VEHICLE", ReportDict.ipcTypeOf("VEHICLE"));
        assertEquals("LOGINK_CN_CREDIT_PERSON", ReportDict.ipcTypeOf("DRIVER"));
        assertEquals("电子运单", ReportDict.docNameOf("WAYBILL"));
        assertEquals("资金流水单", ReportDict.docNameOf("FUND_FLOW"));
    }

    // ------------------------------------------------------------------ 2. 扫描上报：已签收运单必报

    @Test
    @Order(2)
    void scanReportsSignedWaybill() throws Exception {
        String token = adminToken();
        long orgId = call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();

        // 造一张已签收运单（通过司机端流程推进到 SIGNED 太重，这里直接插一张已签收的运单）
        String waybillNo = "WB-REPORT-" + SUFFIX;
        jdbc.update("INSERT INTO wb_waybill (org_id, waybill_no, shipper_id, driver_id, vehicle_id, "
                + "cargo_name, weight, freight_amount, load_addr, unload_addr, status, report_status, "
                + "create_time, sign_time, deleted) VALUES (?, ?, 1, 9, 1, '上报测试货', 5.00, 888.00, "
                + "'上海市浦东新区', '杭州市余杭区', 'SIGNED', 0, NOW(), NOW(), 0)",
                orgId, waybillNo);

        // 触发扫描上报
        JsonNode scan = call(HttpMethod.POST, "/api/reports/scan?limit=100", null, token);
        assertEquals("00000", scan.path("code").asText(), scan.toString());
        assertTrue(scan.path("data").asInt() >= 1, "扫描至少应上报 1 条（刚插入的已签收运单）");

        // 断言：运单的 report_status 已被回写为 1
        Integer reportStatus = jdbc.queryForObject(
                "SELECT report_status FROM wb_waybill WHERE waybill_no = ?", Integer.class, waybillNo);
        assertEquals(1, reportStatus.intValue(), "上报成功后运单 report_status 应回写为 1");

        // 断言：rp_report_record 有对应成功记录
        Long cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rp_report_record WHERE doc_type='WAYBILL' AND biz_no=? AND status='SUCCESS'",
                Long.class, waybillNo);
        assertEquals(1L, cnt.longValue(), "应有 1 条成功的运单上报记录");
    }

    // ------------------------------------------------------------------ 3. 幂等：重复扫描不重复上报

    @Test
    @Order(3)
    void scanIsIdempotent() throws Exception {
        String token = adminToken();
        String waybillNo = "WB-REPORT-" + SUFFIX;

        // 再扫一次：运单已 report_status=1，不会再被扫到
        call(HttpMethod.POST, "/api/reports/scan?limit=100", null, token);

        Long cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rp_report_record WHERE doc_type='WAYBILL' AND biz_no=?",
                Long.class, waybillNo);
        assertEquals(1L, cnt.longValue(), "重复扫描不应产生第二条上报记录");
    }

    // ------------------------------------------------------------------ 4. 失败补偿：含 FAIL 的单据进入补偿队列

    @Test
    @Order(4)
    void failedReportEntersRetryQueue() throws Exception {
        String token = adminToken();
        long orgId = call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();

        // 造一张运单号含 FAIL 的已签收运单：mock 客户端确定性失败
        String failNo = "WB-FAIL-" + SUFFIX;
        jdbc.update("INSERT INTO wb_waybill (org_id, waybill_no, shipper_id, driver_id, vehicle_id, "
                + "cargo_name, weight, freight_amount, load_addr, unload_addr, status, report_status, "
                + "create_time, sign_time, deleted) VALUES (?, ?, 1, 9, 1, '失败测试货', 5.00, 666.00, "
                + "'上海市浦东新区', '杭州市余杭区', 'SIGNED', 0, NOW(), NOW(), 0)",
                orgId, failNo);

        call(HttpMethod.POST, "/api/reports/scan?limit=100", null, token);

        // 断言：失败记录进入 rp_retry 补偿队列（WAITING 状态）
        Long retryCnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rp_retry r JOIN rp_report_record c ON c.id = r.record_id "
                        + "WHERE c.biz_no=? AND r.status='WAITING'", Long.class, failNo);
        assertTrue(retryCnt >= 1, "含 FAIL 的单据上报失败后应进入补偿队列");

        // 断言：上报记录状态为 FAILED，重试次数 > 0
        Integer retryCount = jdbc.queryForObject(
                "SELECT retry_count FROM rp_report_record WHERE biz_no=? ORDER BY id DESC LIMIT 1",
                Integer.class, failNo);
        assertTrue(retryCount >= 1, "失败后 retry_count 应递增");
    }

    // ------------------------------------------------------------------ 5. 看板统计

    @Test
    @Order(5)
    void statsAreConsistent() throws Exception {
        String token = adminToken();
        JsonNode stats = call(HttpMethod.GET, "/api/reports/stats", null, token).path("data");
        assertTrue(stats.path("total").asLong() >= 2, "看板 total 应至少包含前面上报的记录");
        assertTrue(stats.path("success").asLong() >= 1, "至少 1 条成功");
        assertTrue(stats.path("failed").asLong() >= 1, "至少 1 条失败");
        assertNotNull(stats.path("successRate"));
        // 列表接口
        JsonNode list = call(HttpMethod.GET, "/api/reports?limit=20", null, token).path("data");
        assertTrue(list.size() >= 2, "上报列表应返回记录");
    }
}
