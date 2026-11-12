package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.stat.support.RegionParser;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M5 统计域：预聚合口径与实表明细必须一致。
 *
 * 统计最容易出的错不是「接口报错」，而是「数字不对」——
 * 页面照样出图，没人发现它跟业务表差了三成。所以这里的断言全部拿预聚合结果去核对源表：
 * `stat_daily` 的新建单量 == 当天 `wb_waybill` 的实际条数，一条不差。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M5StatTest {

    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private static final LocalDate TODAY = LocalDate.now();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();

    private static long orgId;
    private static String waybillNo;

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

    // ------------------------------------------------------------------ 1. 省域解析

    @Test
    @Order(1)
    void regionParserHandlesCommonInputs() {
        assertEquals("上海市", RegionParser.provinceOf("上海市浦东新区外高桥保税区"));
        assertEquals("上海市", RegionParser.provinceOf("上海浦东外高桥"));
        assertEquals("福建省", RegionParser.provinceOf("厦门北"));
        assertEquals("福建省", RegionParser.provinceOf("福建省厦门市湖里区"));
        assertEquals("山西省", RegionParser.provinceOf("山西大同"));
        assertEquals("浙江省", RegionParser.provinceOf("宁波北仑港区"));
        assertEquals("内蒙古自治区", RegionParser.provinceOf("内蒙古呼和浩特市"));
        // 认不出来就归「其他」，不猜
        assertEquals("其他", RegionParser.provinceOf("A"));
        assertEquals("其他", RegionParser.provinceOf("match-1794235945"));
        assertEquals("其他", RegionParser.provinceOf(null));
    }

    // ------------------------------------------------------------------ 2. 造数据

    @Test
    @Order(2)
    void prepareWaybillToday() throws Exception {
        String token = adminToken();
        orgId = call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();

        long gsId = call(HttpMethod.POST, "/api/goods-sources?operator=tester",
                "{\"orgId\":" + orgId + ",\"shipperId\":1,\"goodsName\":\"统计测试货" + SUFFIX + "\","
                        + "\"weight\":9,\"freightPrice\":4321,"
                        + "\"loadAddr\":\"上海市浦东新区外高桥保税区\",\"unloadAddr\":\"杭州市钱塘区下沙\"}",
                token).path("data").asLong();
        long wbId = call(HttpMethod.POST, "/api/waybills",
                "{\"orgId\":" + orgId + ",\"goodsSourceId\":" + gsId + ",\"operateBy\":\"tester\"}",
                token).path("data").asLong();
        waybillNo = call(HttpMethod.GET, "/api/waybills/" + wbId, null, token)
                .path("data").path("waybillNo").asText();
        assertTrue(waybillNo.startsWith("WB"), waybillNo);
    }

    // ------------------------------------------------------------------ 3. 重算与口径一致性

    @Test
    @Order(3)
    void rebuildMatchesSourceTables() throws Exception {
        String token = adminToken();
        JsonNode rebuilt = call(HttpMethod.POST,
                "/api/stat/rebuild?from=" + TODAY + "&to=" + TODAY + "&operator=tester", null, token);
        assertEquals("00000", rebuilt.path("code").asText(), rebuilt.toString());
        assertEquals(1, rebuilt.path("data").asInt(), "重算 1 天");

        // 源表口径（与 StatReadMapper 完全一致的 where 条件）
        Long expectCreated = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wb_waybill WHERE deleted = 0 "
                        + "AND create_time >= ? AND create_time < ?", Long.class,
                TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay());
        Long expectSigned = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wb_waybill WHERE deleted = 0 "
                        + "AND sign_time >= ? AND sign_time < ?", Long.class,
                TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay());

        JsonNode daily = call(HttpMethod.GET, "/api/stat/daily?from=" + TODAY + "&to=" + TODAY, null, token);
        assertEquals(1, daily.path("data").size(), "同一天只应有一行（重算是覆盖写）");
        JsonNode row = daily.path("data").get(0);
        assertEquals(expectCreated.longValue(), row.path("waybillCreated").asLong(),
                "预聚合的新建单量必须与源表一致");
        assertEquals(expectSigned.longValue(), row.path("waybillSigned").asLong(),
                "签收量必须与源表一致");
        assertTrue(row.path("waybillAmount").asDouble() >= 4321.0, "金额应包含本次 4321 元的运单");

        // 重算幂等：再跑一次不产生第二行
        call(HttpMethod.POST, "/api/stat/rebuild?from=" + TODAY + "&to=" + TODAY, null, token);
        JsonNode again = call(HttpMethod.GET, "/api/stat/daily?from=" + TODAY + "&to=" + TODAY, null, token);
        assertEquals(1, again.path("data").size());
        assertEquals(row.path("waybillCreated").asLong(), again.path("data").get(0).path("waybillCreated").asLong());
    }

    // ------------------------------------------------------------------ 4. 总览与趋势

    @Test
    @Order(4)
    void overviewAndTrendAreConsistent() throws Exception {
        String token = adminToken();
        JsonNode ov = call(HttpMethod.GET,
                "/api/stat/overview?from=" + TODAY + "&to=" + TODAY, null, token).path("data");
        JsonNode daily = call(HttpMethod.GET, "/api/stat/daily?from=" + TODAY + "&to=" + TODAY, null, token)
                .path("data").get(0);
        assertEquals(daily.path("waybillCreated").asLong(), ov.path("waybillCreated").asLong(),
                "总览应等于日汇总之和");
        assertEquals(daily.path("waybillSigned").asLong(), ov.path("waybillSigned").asLong());
        assertTrue(ov.path("completionRate").asDouble() >= 0 && ov.path("completionRate").asDouble() <= 1);

        JsonNode day = call(HttpMethod.GET,
                "/api/stat/trend?from=" + TODAY + "&to=" + TODAY + "&granularity=DAY", null, token);
        assertEquals(1, day.path("data").size());
        assertEquals(TODAY.toString(), day.path("data").get(0).path("period").asText());

        JsonNode month = call(HttpMethod.GET,
                "/api/stat/trend?from=" + TODAY + "&to=" + TODAY + "&granularity=MONTH", null, token);
        assertEquals(1, month.path("data").size());
        assertEquals(TODAY.toString().substring(0, 7), month.path("data").get(0).path("period").asText());

        JsonNode week = call(HttpMethod.GET,
                "/api/stat/trend?from=" + TODAY + "&to=" + TODAY + "&granularity=WEEK", null, token);
        assertTrue(week.path("data").get(0).path("period").asText().contains("-W"));
    }

    // ------------------------------------------------------------------ 5. 维度与穿透

    @Test
    @Order(5)
    void dimsCoverProvincesAndShareSumsToOne() throws Exception {
        String token = adminToken();
        JsonNode dims = call(HttpMethod.GET,
                "/api/stat/dims?from=" + TODAY + "&to=" + TODAY
                        + "&dimType=LOAD_PROVINCE&limit=20", null, token).path("data");
        assertTrue(dims.size() > 0, "应有省份分布");

        double shareSum = 0;
        boolean hasShanghai = false;
        for (JsonNode d : dims) {
            shareSum += d.path("share").asDouble();
            if ("上海市".equals(d.path("dimKey").asText())) {
                hasShanghai = true;
                assertTrue(d.path("waybillCount").asInt() >= 1, "刚建的上海货源应计入");
            }
        }
        assertTrue(hasShanghai, "上海市应出现在分布里: " + dims);
        assertTrue(Math.abs(shareSum - 1.0) < 0.01, "占比之和应接近 1，实际 " + shareSum);
    }

    @Test
    @Order(6)
    void drillReturnsRealBillsAndFiltersByDim() throws Exception {
        String token = adminToken();
        JsonNode all = call(HttpMethod.GET,
                "/api/stat/drill?from=" + TODAY + "&to=" + TODAY + "&metric=WAYBILL_CREATED&limit=200",
                null, token).path("data");
        boolean found = false;
        for (JsonNode r : all) {
            assertEquals("WAYBILL", r.path("bizType").asText());
            if (waybillNo.equals(r.path("bizNo").asText())) {
                found = true;
                assertTrue(r.path("amount").asDouble() >= 4321.0);
            }
        }
        assertTrue(found, "穿透明细应包含刚创建的运单 " + waybillNo);

        // 按维度过滤：只给上海，不应混进别的省
        JsonNode filtered = call(HttpMethod.GET,
                "/api/stat/drill?from=" + TODAY + "&to=" + TODAY
                        + "&metric=WAYBILL_CREATED&dimType=LOAD_PROVINCE&dimKey=上海市&limit=200",
                null, token).path("data");
        assertTrue(filtered.size() >= 1);
        assertTrue(filtered.size() <= all.size(), "过滤后不会变多");

        // 其他指标也能穿透
        JsonNode alarm = call(HttpMethod.GET,
                "/api/stat/drill?from=" + TODAY + "&to=" + TODAY + "&metric=ALARM&limit=5", null, token);
        assertEquals("00000", alarm.path("code").asText());
        JsonNode settle = call(HttpMethod.GET,
                "/api/stat/drill?from=" + TODAY + "&to=" + TODAY + "&metric=SETTLEMENT&limit=5", null, token);
        assertEquals("00000", settle.path("code").asText());
    }

    @Test
    @Order(7)
    void invalidRequestsAreRejected() throws Exception {
        String token = adminToken();
        JsonNode badMetric = call(HttpMethod.GET,
                "/api/stat/drill?metric=NOT_A_METRIC", null, token);
        assertEquals("A0001", badMetric.path("code").asText(), "未知指标应报参数错误");

        JsonNode tooWide = call(HttpMethod.GET,
                "/api/stat/overview?from=2024-01-01&to=2026-12-31", null, token);
        assertEquals("A0001", tooWide.path("code").asText(), "区间过宽应被拒绝");

        JsonNode reverse = call(HttpMethod.GET,
                "/api/stat/overview?from=" + TODAY + "&to=" + TODAY.minusDays(1), null, token);
        assertEquals("A0001", reverse.path("code").asText(), "开始晚于结束应被拒绝");

        JsonNode metrics = call(HttpMethod.GET, "/api/stat/metrics", null, token);
        assertEquals(7, metrics.path("data").size(), "指标清单应有 7 项");
    }
}
