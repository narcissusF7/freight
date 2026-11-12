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
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3 轨迹测试：批量上报幂等去重、漂移过滤、时间窗查询、抽稀上限、里程/速度摘要。
 * 走真实 MySQL 分区表（tr_track_point），验证分区键 gps_time 与复合主键下的写入与查询。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class M3TrackTest {

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

    /** 建一条最小运单（轨迹只需要 waybillId，司机/车辆可空） */
    private long newWaybill(String token, long orgId, String suffix) throws Exception {
        String gsBody = String.format(
                "{\"orgId\":%d,\"goodsName\":\"轨迹货%s\",\"weight\":10.0,\"freightPrice\":3000.00,"
                        + "\"loadAddr\":\"厦门\",\"unloadAddr\":\"泉州\",\"operateBy\":\"tester\"}", orgId, suffix);
        ResponseEntity<String> gs = rest.exchange(baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(gsBody, jsonToken(token)), String.class);
        long gsId = mapper.readTree(gs.getBody()).path("data").asLong();

        String wbBody = String.format(
                "{\"orgId\":%d,\"goodsSourceId\":%d,\"operateBy\":\"tester\"}", orgId, gsId);
        ResponseEntity<String> wb = rest.exchange(baseUrl() + "/api/waybills", HttpMethod.POST,
                new HttpEntity<>(wbBody, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(wb.getBody()).path("code").asText(),
                "建运单失败: " + wb.getBody());
        return mapper.readTree(wb.getBody()).path("data").asLong();
    }

    private String point(long waybillId, double lat, double lng, LocalDateTime gpsTime, double accuracy, int speed) {
        return String.format(
                "{\"waybillId\":%d,\"lat\":%.6f,\"lng\":%.6f,\"speed\":%d,\"accuracy\":%.1f,"
                        + "\"gpsTime\":\"%s\",\"source\":\"TEST\"}",
                waybillId, lat, lng, speed, accuracy, gpsTime.format(ISO));
    }

    private int report(String token, String... points) throws Exception {
        String body = "{\"points\":[" + String.join(",", points) + "]}";
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/tracks/batch", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("00000", json.path("code").asText(), "上报失败: " + resp.getBody());
        return json.path("data").asInt();
    }

    private JsonNode query(String token, long waybillId, Integer maxPoints) throws Exception {
        String url = baseUrl() + "/api/tracks?waybillId=" + waybillId
                + (maxPoints == null ? "" : "&maxPoints=" + maxPoints);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data");
    }

    @Test
    void report_is_idempotent_and_filters_drift() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybill(token, orgId, s);

        LocalDateTime base = LocalDateTime.now().minusMinutes(10).withNano(0);
        // 5 个正常点（30s 间隔，约 150m）+ 1 个精度 500m 的漂移点
        int inserted = report(token,
                point(wbId, 24.480000, 118.090000, base, 12, 40),
                point(wbId, 24.481000, 118.091000, base.plusSeconds(30), 12, 45),
                point(wbId, 24.482000, 118.092000, base.plusSeconds(60), 12, 48),
                point(wbId, 24.483000, 118.093000, base.plusSeconds(90), 12, 50),
                point(wbId, 24.484000, 118.094000, base.plusSeconds(120), 12, 42),
                point(wbId, 30.000000, 120.000000, base.plusSeconds(150), 500, 0));
        assertEquals(5, inserted, "漂移点（精度>100m）应被丢弃，只入库 5 点");

        // 完全重复提交：幂等，不新增、不报错
        int again = report(token,
                point(wbId, 24.480000, 118.090000, base, 12, 40),
                point(wbId, 24.481000, 118.091000, base.plusSeconds(30), 12, 45));
        assertEquals(0, again, "重复时间点应被幂等去重");

        JsonNode points = query(token, wbId, null);
        assertEquals(5, points.size());
        assertTrue(points.get(0).path("gpsTime").asText().compareTo(points.get(4).path("gpsTime").asText()) < 0,
                "查询结果应按 gpsTime 升序");
    }

    @Test
    void summary_computes_distance_and_speed() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybill(token, orgId, s);

        LocalDateTime base = LocalDateTime.now().minusMinutes(5).withNano(0);
        // 每步约 1.1km / 60s → 约 66km/h
        report(token,
                point(wbId, 24.480000, 118.090000, base, 10, 60),
                point(wbId, 24.490000, 118.090000, base.plusSeconds(60), 10, 65),
                point(wbId, 24.500000, 118.090000, base.plusSeconds(120), 10, 63),
                point(wbId, 24.510000, 118.090000, base.plusSeconds(180), 10, 61));

        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/tracks/summary?waybillId=" + wbId, HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode sum = mapper.readTree(resp.getBody()).path("data");

        assertEquals(4, sum.path("pointCount").asInt());
        double km = sum.path("distanceKm").asDouble();
        assertTrue(km > 3.0 && km < 3.8, "3 段约 1.11km/段，总里程应约 3.3km，实际 " + km);
        double avg = sum.path("avgSpeed").asDouble();
        assertTrue(avg > 50 && avg < 80, "均速应在 50~80km/h，实际 " + avg);
        assertTrue(sum.path("maxSpeed").asDouble() >= 60, "最高速应不低于 60");
        assertNotNull(sum.path("startTime").asText());
    }

    @Test
    void query_sampling_keeps_endpoints() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long wbId = newWaybill(token, orgId, s);

        LocalDateTime base = LocalDateTime.now().minusHours(2).withNano(0);
        String[] pts = new String[60];
        for (int i = 0; i < 60; i++) {
            pts[i] = point(wbId, 24.48 + i * 0.001, 118.09 + i * 0.001,
                    base.plusSeconds(i * 30L), 10, 50);
        }
        assertEquals(60, report(token, pts));

        JsonNode sampled = query(token, wbId, 10);
        assertEquals(10, sampled.size(), "应抽稀到 maxPoints=10");
        assertEquals(ptsFirstTime(base), sampled.get(0).path("gpsTime").asText(), "首点必须保留");
        assertEquals(ptsFirstTime(base.plusSeconds(59 * 30L)),
                sampled.get(9).path("gpsTime").asText(), "末点必须保留");
    }

    private String ptsFirstTime(LocalDateTime t) {
        return t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
}
