package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3 剩余部分：电子围栏、四类在途预警（超速/偏航/异常停留/围栏进出）、
 * 预警幂等与处理、分区预建、冷数据归档 dryRun。
 *
 * 走真实 MySQL 8：围栏与预警表是普通表，轨迹仍在月分区表里。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class M3AlarmTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() { return "http://localhost:" + port; }

    private HttpHeaders jsonToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return h;
    }

    private String login() throws Exception {
        ResponseEntity<String> resp = rest.postForEntity(baseUrl() + "/api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", jsonToken(null)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data").path("token").asText();
    }

    private long orgIdOf(String token) throws Exception {
        ResponseEntity<String> me = rest.exchange(baseUrl() + "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        return mapper.readTree(me.getBody()).path("data").path("orgId").asLong();
    }

    /** 建一条带起终点坐标的运单（偏航判定依赖这两个点）。 */
    private long newWaybillWithRoute(String token, long orgId, String suffix) throws Exception {
        String gsBody = String.format(
                "{\"orgId\":%d,\"goodsName\":\"预警货%s\",\"weight\":12.0,\"freightPrice\":5000.00,"
                        + "\"loadAddr\":\"厦门北\",\"loadLat\":24.480000,\"loadLng\":118.090000,"
                        + "\"unloadAddr\":\"泉州北\",\"unloadLat\":24.600000,\"unloadLng\":118.090000,"
                        + "\"operateBy\":\"tester\"}", orgId, suffix);
        ResponseEntity<String> gs = rest.exchange(baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(gsBody, jsonToken(token)), String.class);
        long gsId = mapper.readTree(gs.getBody()).path("data").asLong();

        String wbBody = String.format(
                "{\"orgId\":%d,\"goodsSourceId\":%d,\"operateBy\":\"tester\"}", orgId, gsId);
        ResponseEntity<String> wb = rest.exchange(baseUrl() + "/api/waybills", HttpMethod.POST,
                new HttpEntity<>(wbBody, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(wb.getBody()).path("code").asText(), "建运单失败: " + wb.getBody());
        return mapper.readTree(wb.getBody()).path("data").asLong();
    }

    private long createFence(String token, long orgId, String code, int radiusM) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"fenceCode\":\"%s\",\"fenceName\":\"测试装货围栏%s\",\"fenceType\":\"LOAD\","
                        + "\"shape\":\"CIRCLE\",\"centerLat\":24.480000,\"centerLng\":118.090000,"
                        + "\"radiusM\":%d,\"triggerType\":\"BOTH\",\"enabled\":1,\"operateBy\":\"tester\"}",
                orgId, code, code, radiusM);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/fences", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(resp.getBody()).path("code").asText(), "建围栏失败: " + resp.getBody());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    /**
     * 清理测试围栏（逻辑删除）。围栏是机构级配置，不清理会污染后续运单的预警结果，
     * 也会让测试数据越积越多。
     */
    private void deleteFence(String token, long fenceId) {
        rest.exchange(baseUrl() + "/api/fences/" + fenceId, HttpMethod.DELETE,
                new HttpEntity<>(jsonToken(token)), String.class);
    }

    private String point(long waybillId, double lat, double lng, LocalDateTime gpsTime, int speed) {
        return String.format(
                "{\"waybillId\":%d,\"lat\":%.6f,\"lng\":%.6f,\"speed\":%d,\"accuracy\":10.0,"
                        + "\"gpsTime\":\"%s\",\"source\":\"TEST\"}",
                waybillId, lat, lng, speed, gpsTime.format(ISO));
    }

    private int report(String token, String... points) throws Exception {
        String body = "{\"points\":[" + String.join(",", points) + "]}";
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/tracks/batch", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("00000", json.path("code").asText(), "上报失败: " + resp.getBody());
        return json.path("data").asInt();
    }

    private JsonNode alarms(String token, long waybillId) throws Exception {
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/tracks/alarms?waybillId=" + waybillId + "&limit=100", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data");
    }

    private Set<String> alarmTypes(JsonNode list) {
        Set<String> types = new HashSet<>();
        for (JsonNode n : list) {
            types.add(n.path("alarmType").asText());
        }
        return types;
    }

    private int analyze(String token, long waybillId) throws Exception {        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/tracks/analyze?waybillId=" + waybillId, HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("00000", json.path("code").asText(), "分析失败: " + resp.getBody());
        return json.path("data").asInt();
    }

    private List<String> partitionNames(String token) throws Exception {
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/tracks/maintenance/partitions",
                HttpMethod.GET, new HttpEntity<>(jsonToken(token)), String.class);
        JsonNode data = mapper.readTree(resp.getBody()).path("data");
        List<String> names = new ArrayList<>();
        for (JsonNode n : data) {
            names.add(n.path("partitionName").asText());
        }
        return names;
    }

    /**
     * 一条轨迹串起四类预警：
     * 围栏内 → 出围栏(超速) → 偏离线路 41km → 原地停留 40min → 回到围栏内。
     * 阈值用默认值（90km/h、30min、30km）。
     */
    @Test
    void track_events_produce_five_alarm_types_and_are_idempotent() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybillWithRoute(token, orgId, s);
        long fenceId = createFence(token, orgId, "F" + s, 3000);
        assertTrue(fenceId > 0);

        LocalDateTime base = LocalDateTime.now().minusHours(2).withNano(0);
        int inserted = report(token,
                // 围栏内（3km 半径），停着
                point(wbId, 24.480000, 118.090000, base, 0),
                point(wbId, 24.481000, 118.090000, base.plusMinutes(5), 0),
                // 出围栏 + 连续两点超速（95/96 > 90）
                point(wbId, 24.530000, 118.090000, base.plusMinutes(10), 95),
                point(wbId, 24.540000, 118.090000, base.plusMinutes(11), 96),
                // 向东偏 41km（计划线路是 lng=118.09 的直线）
                point(wbId, 24.530000, 118.500000, base.plusMinutes(15), 60),
                // 原地停留 40 分钟（阈值 30）
                point(wbId, 24.530000, 118.500000, base.plusMinutes(20), 0),
                point(wbId, 24.530000, 118.500000, base.plusMinutes(40), 0),
                point(wbId, 24.530000, 118.500000, base.plusMinutes(60), 0),
                // 回到围栏内
                point(wbId, 24.481000, 118.090000, base.plusMinutes(70), 40));
        assertEquals(9, inserted, "9 个点都应入库");

        JsonNode list = alarms(token, wbId);
        Set<String> types = alarmTypes(list);
        assertTrue(types.contains("FENCE_OUT"), "应产生出围栏预警，实际 " + types);
        assertTrue(types.contains("OVERSPEED"), "应产生超速预警，实际 " + types);
        assertTrue(types.contains("DEVIATE"), "应产生偏航预警，实际 " + types);
        assertTrue(types.contains("LONG_STAY"), "应产生异常停留预警，实际 " + types);
        assertTrue(types.contains("FENCE_IN"), "应产生进围栏预警，实际 " + types);

        int before = list.size();
        assertEquals(0, analyze(token, wbId), "同一批轨迹重复分析不应新增预警（幂等）");
        assertEquals(before, alarms(token, wbId).size(), "预警条数不应变化");

        // 处理第一条预警：状态置为已处理并留痕
        long alarmId = list.get(0).path("id").asLong();
        String handleBody = "{\"handleBy\":\"tester\",\"handleRemark\":\"已电话提醒司机\",\"status\":1}";
        ResponseEntity<String> handled = rest.exchange(
                baseUrl() + "/api/tracks/alarms/" + alarmId + "/handle", HttpMethod.POST,
                new HttpEntity<>(handleBody, jsonToken(token)), String.class);
        JsonNode h = mapper.readTree(handled.getBody()).path("data");
        assertEquals(1, h.path("status").asInt(), "处理状态应为已处理");
        assertEquals("已电话提醒司机", h.path("handleRemark").asText());
        assertFalse(h.path("handleTime").asText().isEmpty(), "应记录处理时间");
        deleteFence(token, fenceId);
    }

    /** 围栏参数校验：圆形缺半径应被拒绝（A0001），不能静默建出一个永不满意的围栏。 */
    @Test
    void fence_validation_rejects_incomplete_circle() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String code = "BAD" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        String body = String.format(
                "{\"orgId\":%d,\"fenceCode\":\"%s\",\"fenceName\":\"坏围栏\",\"shape\":\"CIRCLE\","
                        + "\"centerLat\":24.48,\"centerLng\":118.09,\"operateBy\":\"tester\"}", orgId, code);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/fences", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals("A0001", mapper.readTree(resp.getBody()).path("code").asText());
    }

    /** 多边形围栏：三角形包含/不包含判定（纯服务端射线法，不依赖 GIS 扩展）。 */
    @Test
    void polygon_fence_triggers_enter_and_leave() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybillWithRoute(token, orgId, s);

        // 以 (24.50,118.10) 为中心的三角形，边长约 20km
        String fenceBody = String.format(
                "{\"orgId\":%d,\"fenceCode\":\"PG%s\",\"fenceName\":\"三角禁行区\",\"fenceType\":\"FORBIDDEN\","
                        + "\"shape\":\"POLYGON\",\"vertices\":[[24.60,118.10],[24.45,117.99],[24.45,118.21]],"
                        + "\"triggerType\":\"BOTH\",\"enabled\":1,\"operateBy\":\"tester\"}", orgId, s);
        ResponseEntity<String> fenceResp = rest.exchange(baseUrl() + "/api/fences", HttpMethod.POST,
                new HttpEntity<>(fenceBody, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(fenceResp.getBody()).path("code").asText(),
                "建多边形围栏失败: " + fenceResp.getBody());
        long fenceId = mapper.readTree(fenceResp.getBody()).path("data").asLong();

        LocalDateTime base = LocalDateTime.now().minusMinutes(40).withNano(0);
        report(token,
                point(wbId, 24.500000, 118.100000, base, 40),                    // 界内
                point(wbId, 24.500000, 118.140000, base.plusMinutes(5), 40),       // 界内
                point(wbId, 24.500000, 118.400000, base.plusMinutes(10), 40),      // 界外 → OUT
                point(wbId, 24.500000, 118.100000, base.plusMinutes(15), 40));     // 界内 → IN

        Set<String> types = alarmTypes(alarms(token, wbId));
        assertTrue(types.contains("FENCE_OUT"), "多边形围栏应产生出围栏预警，实际 " + types);
        assertTrue(types.contains("FENCE_IN"), "多边形围栏应产生进围栏预警，实际 " + types);
        deleteFence(token, fenceId);
    }

    /**
     * 装卸围栏只对「装卸点就在围栏附近」的运单生效：
     * 远在 600km 外的装货围栏不应给本运单产生任何进出预警（否则同机构几十个装卸点会互相刷屏）。
     */
    @Test
    void load_fence_far_from_waybill_is_ignored() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybillWithRoute(token, orgId, s);

        String farFence = String.format(
                "{\"orgId\":%d,\"fenceCode\":\"FAR%s\",\"fenceName\":\"异地装货区\",\"fenceType\":\"LOAD\","
                        + "\"shape\":\"CIRCLE\",\"centerLat\":30.000000,\"centerLng\":120.000000,"
                        + "\"radiusM\":3000,\"triggerType\":\"BOTH\",\"enabled\":1,\"operateBy\":\"tester\"}",
                orgId, s);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/fences", HttpMethod.POST,
                new HttpEntity<>(farFence, jsonToken(token)), String.class);
        long fenceId = mapper.readTree(resp.getBody()).path("data").asLong();

        LocalDateTime base = LocalDateTime.now().minusMinutes(30).withNano(0);
        report(token,
                point(wbId, 24.480000, 118.090000, base, 30),
                point(wbId, 24.500000, 118.090000, base.plusMinutes(5), 30));

        for (JsonNode n : alarms(token, wbId)) {
            assertFalse(n.path("fenceId").asLong() == fenceId,
                    "异地装货围栏不应参与本运单判定，却产生了 " + n.path("alarmType").asText());
        }
        deleteFence(token, fenceId);
    }

    /** 分区预建：幂等；已存在的月份返回 EXISTS，新建月份从 pmax 拆出。 */
    @Test
    void partition_ensure_is_idempotent() throws Exception {
        String token = login();
        // 选一个未来月份（现有分区只铺到 2027-12），避免与手工预建冲突
        YearMonth target = YearMonth.now().plusMonths(14);
        String url = baseUrl() + "/api/tracks/maintenance/partition?month=" + target;

        ResponseEntity<String> first = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        JsonNode firstData = mapper.readTree(first.getBody()).path("data");
        String firstAction = firstData.path("action").asText();
        assertTrue("CREATED".equals(firstAction) || "EXISTS".equals(firstAction),
                "首次预建应为 CREATED 或幂等 EXISTS，实际 " + firstAction + " / " + first.getBody());
        String partitionName = firstData.path("partitionName").asText();
        assertTrue(partitionNames(token).contains(partitionName), "分区应出现在分区列表中");

        ResponseEntity<String> second = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        assertEquals("EXISTS", mapper.readTree(second.getBody()).path("data").path("action").asText(),
                "重复预建应返回 EXISTS");
    }

    /** 归档 dryRun：只统计、不导出、不删分区。 */
    @Test
    void archive_dry_run_keeps_partitions() throws Exception {
        String token = login();
        List<String> before = partitionNames(token);
        assertTrue(before.contains("pmax"), "分区表应含 pmax 兜底分区");

        String url = baseUrl() + "/api/tracks/maintenance/archive?before=2027-01-01&dryRun=true";
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        JsonNode data = mapper.readTree(resp.getBody()).path("data");
        assertTrue(data.path("dryRun").asBoolean(), "应标记为 dryRun");
        assertTrue(data.path("items").size() >= 2, "2026-11/2026-12 两个整月分区都应进入清单");

        for (JsonNode item : data.path("items")) {
            assertEquals("DRY_RUN", item.path("status").asText());
        }
        assertEquals(before, partitionNames(token), "dryRun 不允许改动任何分区");
    }
}
