package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3 司机端 APP：账号开通 → 登录 → 令牌作用域 → 找货打分 → 抢单 → 节点确认 → 越权/距离拦截 → 消息与轨迹。
 *
 * 走真实 MySQL，全链路通过 HTTP 接口验证，不直接调 Service——
 * 这样角色限制、令牌作用域、参数绑定这些「只有走 Web 层才暴露」的问题才能被测到。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M3DriverAppTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String PWD = "Driver@2026";

    /** 装货地：上海外高桥；卸货地：杭州下沙 */
    private static final double LOAD_LAT = 31.355800;
    private static final double LOAD_LNG = 121.581700;
    private static final double UNLOAD_LAT = 30.308600;
    private static final double UNLOAD_LNG = 120.341500;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    private static String suffix;
    private static Long driverId;
    private static Long driver2Id;
    private static Long vehicleId;
    private static String driverToken;
    private static String driver2Token;
    private static Long goodsSourceId;
    private static Long waybillId;

    private String base() { return "http://localhost:" + port; }

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
        assertEquals(HttpStatus.OK, resp.getStatusCode(), path + " 非 200: " + resp.getBody());
        return mapper.readTree(resp.getBody());
    }

    private String adminToken() throws Exception {
        JsonNode r = call(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"admin123\"}", null);
        return r.path("data").path("token").asText();
    }

    private long adminOrgId(String token) throws Exception {
        return call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();
    }

    private static String sfx() {
        if (suffix == null) {
            suffix = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        }
        return suffix;
    }

    @Test
    @Order(1)
    void setupDriverVehicleAndGoodsSource() throws Exception {
        String token = adminToken();
        long orgId = adminOrgId(token);

        // 司机 1（本单承运人）
        JsonNode d1 = call(HttpMethod.POST, "/api/drivers",
                "{\"orgId\":" + orgId + ",\"name\":\"司机甲" + sfx() + "\",\"phone\":\"13911" + sfx() + "\","
                        + "\"idCardNo\":\"31010119900101" + sfx() + "\",\"driverLicenseNo\":\"D" + sfx() + "\"}",
                token);
        driverId = d1.path("data").asLong();
        assertTrue(driverId > 0, "司机创建失败: " + d1);

        // 司机 2（越权验证用）
        JsonNode d2 = call(HttpMethod.POST, "/api/drivers",
                "{\"orgId\":" + orgId + ",\"name\":\"司机乙" + sfx() + "\",\"phone\":\"13922" + sfx() + "\"}",
                token);
        driver2Id = d2.path("data").asLong();

        JsonNode v = call(HttpMethod.POST, "/api/vehicles",
                "{\"orgId\":" + orgId + ",\"plateNo\":\"沪A" + sfx() + "\",\"vehicleType\":\"厢式\","
                        + "\"capacityTon\":30,\"driverId\":" + driverId + "}",
                token);
        vehicleId = v.path("data").asLong();
        assertTrue(vehicleId > 0, "车辆创建失败: " + v);

        // 审核通过：司机端登录与接单都要求 audit_status=1
        call(HttpMethod.POST, "/api/drivers/" + driverId + "/audit?status=1", null, token);
        call(HttpMethod.POST, "/api/drivers/" + driver2Id + "/audit?status=1", null, token);
        call(HttpMethod.POST, "/api/vehicles/" + vehicleId + "/audit?status=1", null, token);

        // 货源：30 吨以内、带装卸坐标与报价
        String loadTime = LocalDateTime.now().plusHours(3).format(ISO);
        JsonNode gs = call(HttpMethod.POST, "/api/goods-sources?operator=test",
                "{\"orgId\":" + orgId + ",\"shipperId\":1,\"goodsName\":\"钢材" + sfx() + "\","
                        + "\"weight\":18,\"volume\":30,"
                        + "\"loadAddr\":\"上海市浦东新区外高桥保税区\",\"loadLat\":" + LOAD_LAT + ",\"loadLng\":" + LOAD_LNG + ","
                        + "\"unloadAddr\":\"杭州市钱塘区下沙经济开发区\",\"unloadLat\":" + UNLOAD_LAT + ",\"unloadLng\":" + UNLOAD_LNG + ","
                        + "\"loadTime\":\"" + loadTime + "\",\"freightPrice\":5400,"
                        + "\"contactName\":\"张经理\",\"contactPhone\":\"13800001234\"}",
                token);
        goodsSourceId = gs.path("data").asLong();
        assertTrue(goodsSourceId > 0, "货源创建失败: " + gs);
    }

    @Test
    @Order(2)
    void initPasswordAndLogin() throws Exception {
        String phone = driverPhone(1);
        // 首次开通：手机号 + 6 位验证码 + 新密码
        JsonNode init = call(HttpMethod.POST, "/api/driver/password/init",
                "{\"phone\":\"" + phone + "\",\"smsCode\":\"123456\",\"newPassword\":\"" + PWD + "\"}", null);
        assertEquals("00000", init.path("code").asText(), "开通失败: " + init);

        JsonNode login = call(HttpMethod.POST, "/api/driver/login",
                "{\"phone\":\"" + phone + "\",\"password\":\"" + PWD + "\"}", null);
        assertEquals("00000", login.path("code").asText(), "登录失败: " + login);
        driverToken = login.path("data").path("token").asText();
        assertFalse(driverToken.isEmpty());
        assertEquals(driverId.longValue(), login.path("data").path("driverId").asLong());
        assertTrue(login.path("data").path("expiresIn").asLong() > 7200,
                "司机端令牌有效期应长于后台（在途连续作业）");

        // 密码错误：A0002，且不能泄露「手机号是否存在」
        JsonNode bad = call(HttpMethod.POST, "/api/driver/login",
                "{\"phone\":\"" + phone + "\",\"password\":\"wrong-pass\"}", null);
        assertEquals("A0002", bad.path("code").asText());
    }

    @Test
    @Order(3)
    void driverTokenIsScopedToDriverApi() throws Exception {
        // 司机令牌调后台接口必须 403（DriverScopeFilter）
        ResponseEntity<String> resp = rest.exchange(base() + "/api/waybills", HttpMethod.GET,
                new HttpEntity<>(json(driverToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(), "司机令牌不应能访问后台运单接口");

        // 后台令牌调司机端接口也必须被拒（hasRole DRIVER）
        ResponseEntity<String> back = rest.exchange(base() + "/api/driver/todo", HttpMethod.GET,
                new HttpEntity<>(json(adminToken())), String.class);
        assertNotEquals(HttpStatus.OK, back.getStatusCode(), "后台令牌不应能访问司机端接口");

        JsonNode profile = call(HttpMethod.GET, "/api/driver/profile", null, driverToken);
        assertEquals("00000", profile.path("code").asText());
        assertEquals(driverId.longValue(), profile.path("data").path("driverId").asLong());
    }

    @Test
    @Order(4)
    void hallRanksByFitAndMasksPhone() throws Exception {
        // 带定位查大厅：距装货地 6km 左右
        double lat = LOAD_LAT + 0.05;
        double lng = LOAD_LNG + 0.05;
        JsonNode hall = call(HttpMethod.GET,
                "/api/driver/goods-sources?lat=" + lat + "&lng=" + lng + "&limit=50", null, driverToken);
        assertEquals("00000", hall.path("code").asText(), hall.toString());
        JsonNode list = hall.path("data");
        assertTrue(list.size() > 0, "货源大厅为空");

        JsonNode mine = null;
        for (JsonNode n : list) {
            if (n.path("goodsSourceId").asLong() == goodsSourceId) {
                mine = n;
                break;
            }
        }
        assertTrue(mine != null, "本单货源未出现在大厅");
        assertTrue(mine.path("capacityFits").asBoolean(), "30 吨车应能承运 18 吨");
        assertTrue(mine.path("score").asInt() > 0 && mine.path("score").asInt() <= 100);
        assertEquals("138****1234", mine.path("contactPhone").asText(), "接单前必须脱敏手机号");
        assertTrue(mine.path("distanceToLoadKm").asDouble() < 20, "距离计算异常");

        // 分数降序：第一条分数不应低于最后一条
        assertTrue(list.get(0).path("score").asInt() >= list.get(list.size() - 1).path("score").asInt());
    }

    @Test
    @Order(5)
    void acceptCreatesWaybillAndBlocksDoubleAccept() throws Exception {
        JsonNode acc = call(HttpMethod.POST, "/api/driver/goods-sources/" + goodsSourceId + "/accept",
                "{\"vehicleId\":" + vehicleId + "}", driverToken);
        assertEquals("00000", acc.path("code").asText(), "接单失败: " + acc);
        waybillId = acc.path("data").path("waybill").path("id").asLong();
        assertTrue(waybillId > 0);
        assertEquals("ACCEPTED", acc.path("data").path("waybill").path("status").asText());
        assertEquals(driverId.longValue(), acc.path("data").path("waybill").path("driverId").asLong());
        assertEquals("ARRIVE_LOAD", acc.path("data").path("nextAction").asText(),
                "接单后下一步应是「到达装货地」");

        // 同一货源再抢：B0001
        JsonNode again = call(HttpMethod.POST, "/api/driver/goods-sources/" + goodsSourceId + "/accept",
                "{\"vehicleId\":" + vehicleId + "}", driverToken);
        assertEquals("B0001", again.path("code").asText(), "重复接单应被拦截");
    }

    @Test
    @Order(6)
    void otherDriverCannotTouchWaybill() throws Exception {
        String phone2 = driverPhone(2);
        call(HttpMethod.POST, "/api/driver/password/init",
                "{\"phone\":\"" + phone2 + "\",\"smsCode\":\"123456\",\"newPassword\":\"" + PWD + "\"}", null);
        JsonNode login2 = call(HttpMethod.POST, "/api/driver/login",
                "{\"phone\":\"" + phone2 + "\",\"password\":\"" + PWD + "\"}", null);
        driver2Token = login2.path("data").path("token").asText();

        JsonNode detail = call(HttpMethod.GET, "/api/driver/waybills/" + waybillId, null, driver2Token);
        assertEquals("A0003", detail.path("code").asText(), "他人运单必须按无权限处理");

        JsonNode confirm = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_LOAD\",\"lat\":" + LOAD_LAT + ",\"lng\":" + LOAD_LNG + "}", driver2Token);
        assertEquals("A0003", confirm.path("code").asText());
    }

    @Test
    @Order(7)
    void arrivalOutsideRadiusIsRejected() throws Exception {
        // 距装货地 100km 左右点「到达装货地」：必须被拦
        JsonNode far = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_LOAD\",\"lat\":32.2,\"lng\":121.0}", driverToken);
        assertEquals("A0001", far.path("code").asText(), "超出半径应拒绝: " + far);
        assertTrue(far.path("message").asText().contains("装货地"), far.path("message").asText());

        // 无定位也拒绝
        JsonNode noGeo = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_LOAD\"}", driverToken);
        assertEquals("A0001", noGeo.path("code").asText());

        // 状态乱序：未装货就想发车
        JsonNode wrong = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"DEPART\"}", driverToken);
        assertEquals("B0002", wrong.path("code").asText(), "非法跃迁应被状态机拦截");
    }

    @Test
    @Order(8)
    void fullNodeChainDrivesWaybillToSigned() throws Exception {
        // 1. 到达装货地（打点，不改状态）
        JsonNode arrive = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_LOAD\",\"lat\":" + (LOAD_LAT + 0.001) + ",\"lng\":" + (LOAD_LNG + 0.001)
                        + ",\"address\":\"外高桥保税区 3 号门\",\"photoUrls\":[\"http://file/load1.jpg\"],"
                        + "\"remark\":\"已到厂排队\"}", driverToken);
        assertEquals("00000", arrive.path("code").asText(), arrive.toString());
        assertEquals("ACCEPTED", arrive.path("data").path("waybill").path("status").asText(),
                "「到达」只是打点，不该改变运单状态");
        assertEquals("LOADED", arrive.path("data").path("nextAction").asText());

        // 重复打点：幂等，不产生第二条节点
        int nodesBefore = arrive.path("data").path("nodes").size();
        JsonNode dup = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_LOAD\",\"lat\":" + (LOAD_LAT + 0.001) + ",\"lng\":" + (LOAD_LNG + 0.001) + "}",
                driverToken);
        assertEquals(nodesBefore, dup.path("data").path("nodes").size(), "重复到达不应新增节点");

        // 2. 装货完成
        JsonNode loaded = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"LOADED\",\"lat\":" + LOAD_LAT + ",\"lng\":" + LOAD_LNG + ","
                        + "\"photoUrls\":[\"http://file/load-over.jpg\"]}", driverToken);
        assertEquals("LOADED", loaded.path("data").path("waybill").path("status").asText());
        assertEquals("DEPART", loaded.path("data").path("nextAction").asText());

        // 3. 发车（不强制定位）
        JsonNode depart = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"DEPART\",\"lat\":" + LOAD_LAT + ",\"lng\":" + LOAD_LNG + "}", driverToken);
        assertEquals("IN_TRANSIT", depart.path("data").path("waybill").path("status").asText());

        // 4. 到达卸货地
        JsonNode arriveUnload = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"ARRIVE_UNLOAD\",\"lat\":" + UNLOAD_LAT + ",\"lng\":" + UNLOAD_LNG + "}", driverToken);
        assertEquals("IN_TRANSIT", arriveUnload.path("data").path("waybill").path("status").asText());
        assertEquals("UNLOADED", arriveUnload.path("data").path("nextAction").asText());

        // 5. 卸货完成
        JsonNode unloaded = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"UNLOADED\",\"lat\":" + UNLOAD_LAT + ",\"lng\":" + UNLOAD_LNG + ","
                        + "\"photoUrls\":[\"http://file/unload.jpg\"]}", driverToken);
        assertEquals("UNLOADED", unloaded.path("data").path("waybill").path("status").asText());
        assertEquals("SIGNED", unloaded.path("data").path("nextAction").asText());

        // 6. 签收
        JsonNode signed = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"SIGNED\",\"lat\":" + UNLOAD_LAT + ",\"lng\":" + UNLOAD_LNG + ","
                        + "\"photoUrls\":[\"http://file/sign.jpg\"],\"remark\":\"王收货\"}", driverToken);
        assertEquals("SIGNED", signed.path("data").path("waybill").path("status").asText());

        // 签收后重试：幂等返回，不报错
        JsonNode again = call(HttpMethod.POST, "/api/driver/waybills/" + waybillId + "/nodes",
                "{\"action\":\"SIGNED\",\"lat\":" + UNLOAD_LAT + ",\"lng\":" + UNLOAD_LNG + "}", driverToken);
        assertEquals("00000", again.path("code").asText());
        assertEquals("SIGNED", again.path("data").path("waybill").path("status").asText());

        // 节点留痕：定位与照片必须落库（监管核查真实性）
        JsonNode nodes = signed.path("data").path("nodes");
        boolean foundEvidence = false;
        for (JsonNode n : nodes) {
            if ("LOADED".equals(n.path("toStatus").asText())
                    && n.path("lat").isNumber() && n.path("evidenceUrls").asText().contains("load-over")) {
                foundEvidence = true;
            }
        }
        assertTrue(foundEvidence, "装货节点必须留存定位与照片");
    }

    @Test
    @Order(9)
    void todoAndMessagesAndTrack() throws Exception {
        JsonNode todo = call(HttpMethod.GET, "/api/driver/todo", null, driverToken);
        assertEquals("00000", todo.path("code").asText());

        // 接单回执应进 APP 消息中心（INBOX 需先投递）
        call(HttpMethod.POST, "/api/messages/dispatch", null, adminToken());
        JsonNode msgs = call(HttpMethod.GET, "/api/driver/messages?limit=50", null, driverToken);
        assertEquals("00000", msgs.path("code").asText());
        boolean hasAccept = false;
        long firstId = 0;
        for (JsonNode m : msgs.path("data")) {
            if ("WAYBILL_ACCEPTED".equals(m.path("bizType").asText())) {
                hasAccept = true;
                firstId = m.path("id").asLong();
            }
        }
        assertTrue(hasAccept, "接单回执未进入司机消息中心: " + msgs);

        // 标记已读
        if (firstId > 0) {
            JsonNode read = call(HttpMethod.POST, "/api/driver/messages/read", "[" + firstId + "]", driverToken);
            assertTrue(read.path("data").asInt() >= 1);
        }

        // 轨迹上报：只能给自己的运单
        String gps = LocalDateTime.now().format(ISO);
        String points = "[{\"waybillId\":" + waybillId + ",\"lat\":" + UNLOAD_LAT + ",\"lng\":" + UNLOAD_LNG
                + ",\"speed\":60,\"accuracy\":10,\"gpsTime\":\"" + gps + "\",\"source\":\"APP\"}]";
        JsonNode track = call(HttpMethod.POST, "/api/driver/tracks/batch",
                "{\"points\":" + points + "}", driverToken);
        assertEquals("00000", track.path("code").asText(), track.toString());
        assertEquals(1, track.path("data").asInt());
    }

    private String driverPhone(int index) {
        // setup 里用的是 13911/13922 + 同一个 6 位后缀
        return (index == 1 ? "13911" : "13922") + sfx();
    }
}
