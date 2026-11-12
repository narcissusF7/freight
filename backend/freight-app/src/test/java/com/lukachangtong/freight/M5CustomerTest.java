package com.lukachangtong.freight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import com.lukachangtong.freight.customer.service.CreditService;
import com.lukachangtong.freight.customer.vo.CreditChangeVo;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M5 客服域：投诉 24h 时效 → 评价资格与幂等 → 信用联动 → 逾期升级。
 *
 * 重点不是「接口能调通」，而是四条容易被绕过的规则：
 * 1. 投诉建单必须落 24h 截止时间；
 * 2. 逾期扫描只能升级一次；
 * 3. 同一运单同一方向只能评一次（重复提交返回 B0001）；
 * 4. 同一业务事件对同一主体只扣一次分（信用流水唯一键）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M5CustomerTest {

    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CreditService creditService;

    private final ObjectMapper mapper = new ObjectMapper();

    private static long orgId;
    private static long driverId;
    private static long waybillId;
    private static long signedWaybillId;
    private static long complaintId;
    private static String complaintNo;

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

    private void changeStatus(String token, long wbId, String to) throws Exception {
        JsonNode r = call(HttpMethod.POST, "/api/waybills/" + wbId + "/status",
                "{\"toStatus\":\"" + to + "\",\"operateBy\":\"tester\"}", token);
        assertEquals("00000", r.path("code").asText(), "流转到 " + to + " 失败: " + r);
    }

    // ------------------------------------------------------------------ 1. 准备运单

    @Test
    @Order(1)
    void prepareSignedWaybill() throws Exception {
        String token = adminToken();
        orgId = call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();

        driverId = call(HttpMethod.POST, "/api/drivers",
                "{\"orgId\":" + orgId + ",\"name\":\"客服测试司机" + SUFFIX + "\","
                        + "\"phone\":\"13966" + SUFFIX.substring(0, 6) + "\"}", token).path("data").asLong();
        long vehicleId = call(HttpMethod.POST, "/api/vehicles",
                "{\"orgId\":" + orgId + ",\"plateNo\":\"沪G" + SUFFIX.substring(0, 5) + "\","
                        + "\"vehicleType\":\"厢式\",\"capacityTon\":30,\"driverId\":" + driverId + "}",
                token).path("data").asLong();
        call(HttpMethod.POST, "/api/drivers/" + driverId + "/audit?status=1", null, token);
        call(HttpMethod.POST, "/api/vehicles/" + vehicleId + "/audit?status=1", null, token);

        long gsId = call(HttpMethod.POST, "/api/goods-sources?operator=tester",
                "{\"orgId\":" + orgId + ",\"shipperId\":1,\"goodsName\":\"客服测试货" + SUFFIX + "\","
                        + "\"weight\":12,\"freightPrice\":3000,"
                        + "\"loadAddr\":\"上海市浦东新区外高桥保税区\",\"unloadAddr\":\"杭州市钱塘区下沙\","
                        + "\"loadLat\":31.3558,\"loadLng\":121.5817,\"unloadLat\":30.3086,\"unloadLng\":120.3415}",
                token).path("data").asLong();
        waybillId = call(HttpMethod.POST, "/api/waybills",
                "{\"orgId\":" + orgId + ",\"goodsSourceId\":" + gsId + ",\"driverId\":" + driverId
                        + ",\"vehicleId\":" + vehicleId + ",\"operateBy\":\"tester\"}",
                token).path("data").asLong();

        // 未签收的运单不能被评价（先记下这张，后面验证）
        assertTrue(waybillId > 0);

        for (String s : new String[]{"PUBLISHED", "ACCEPTED", "LOADED", "IN_TRANSIT", "UNLOADED", "SIGNED"}) {
            changeStatus(token, waybillId, s);
        }
        signedWaybillId = waybillId;
        JsonNode wb = call(HttpMethod.GET, "/api/waybills/" + signedWaybillId, null, token).path("data");
        assertEquals("SIGNED", wb.path("status").asText());
        assertNotNull(wb.path("signTime").asText(), "签收时间应落库，统计按它归集完成量");
    }

    // ------------------------------------------------------------------ 2. 投诉建单与 24h 时限

    @Test
    @Order(2)
    void createComplaintWithSlaDeadline() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/complaints",
                "{\"orgId\":" + orgId + ",\"source\":\"SHIPPER_APP\",\"complainantType\":\"SHIPPER\","
                        + "\"complainantName\":\"测试货主\",\"complainantPhone\":\"13800000000\","
                        + "\"targetType\":\"DRIVER\",\"waybillId\":" + signedWaybillId + ","
                        + "\"complaintType\":\"DELAY\",\"level\":\"URGENT\","
                        + "\"content\":\"约定 8 点到厂，实际 11 点才到，产线停线。\",\"operator\":\"tester\"}",
                token);
        assertEquals("00000", r.path("code").asText(), r.toString());
        JsonNode c = r.path("data");
        complaintId = c.path("id").asLong();
        complaintNo = c.path("complaintNo").asText();

        assertEquals("PENDING", c.path("status").asText());
        // 被投诉人由运单解析，不接受前端乱填
        assertEquals(driverId, c.path("targetId").asLong());
        assertTrue(c.path("targetName").asText().contains("客服测试司机"), "应解析出司机姓名: " + c);

        LocalDateTime deadline = LocalDateTime.parse(c.path("deadlineTime").asText());
        LocalDateTime accept = LocalDateTime.parse(c.path("acceptTime").asText());
        assertEquals(24, java.time.Duration.between(accept, deadline).toHours(), "时限必须是受理后 24 小时");

        JsonNode detail = call(HttpMethod.GET, "/api/complaints/" + complaintId, null, token).path("data");
        assertEquals(1, detail.path("handles").size(), "建单应有 1 条处理记录");
        assertTrue(detail.path("remainHours").asLong() >= 23, "剩余小时数应接近 24");
        assertEquals(false, detail.path("slaBreached").asBoolean());

        // 建单即通知调度台（站内信落 PENDING，等调度器投递）
        Integer msg = jdbc.queryForObject(
                "SELECT COUNT(*) FROM msg_record WHERE biz_type='COMPLAINT_NEW' AND biz_key=?", Integer.class, complaintNo);
        assertEquals(1, msg.intValue(), "投诉建单应产生一条站内信");
    }

    @Test
    @Order(3)
    void illegalTransitionsAreRejected() throws Exception {
        String token = adminToken();
        // 未分派就直接处理
        JsonNode r = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/resolve",
                "{\"result\":\"跳过受理直接办结\",\"operator\":\"tester\"}", token);
        assertEquals("B0002", r.path("code").asText(), "PENDING 不能直接 resolve: " + r);

        // 缺少处理人不能分派
        JsonNode r2 = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/assign",
                "{\"operator\":\"tester\"}", token);
        assertEquals("A0001", r2.path("code").asText(), "分派必须有处理人: " + r2);
    }

    @Test
    @Order(4)
    void assignResolveCloseAndPenalizeCredit() throws Exception {
        String token = adminToken();
        JsonNode assigned = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/assign",
                "{\"assignTo\":\"客服-李静\",\"remark\":\"已联系货主\",\"operator\":\"tester\"}", token);
        assertEquals("PROCESSING", assigned.path("data").path("complaint").path("status").asText());

        JsonNode handled = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/follow",
                "{\"content\":\"电话联系司机核实，车辆故障未报备\",\"operator\":\"tester\"}", token);
        assertEquals(3, handled.path("data").path("handles").size(), "跟进应追加处理记录");

        JsonNode resolved = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/resolve",
                "{\"result\":\"责任成立，扣减当单运费 200 元并致歉\",\"liable\":true,\"operator\":\"tester\"}", token);
        assertEquals("RESOLVED", resolved.path("data").path("complaint").path("status").asText());
        assertEquals(1, resolved.path("data").path("complaint").path("liable").asInt());

        // 责任成立：信用 -10（100 → 90），等级仍为 A
        CsCreditSubject subject = creditService.detail("DRIVER", driverId).getSubject();
        assertEquals(90, subject.getScore().intValue());
        assertEquals("A", subject.getLevel());
        assertEquals(1, subject.getComplaintCount().intValue());

        JsonNode closed = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/close",
                "{\"satisfaction\":4,\"remark\":\"货主接受\",\"operator\":\"tester\"}", token);
        assertEquals("CLOSED", closed.path("data").path("complaint").path("status").asText());
        assertEquals(4, closed.path("data").path("complaint").path("satisfaction").asInt());

        // 重复办结被状态机挡住（也意味着不会重复扣分）
        JsonNode again = call(HttpMethod.POST, "/api/complaints/" + complaintId + "/close",
                "{\"satisfaction\":5}", token);
        assertEquals("B0002", again.path("code").asText());
    }

    // ------------------------------------------------------------------ 3. 逾期扫描

    @Test
    @Order(5)
    void overdueScanEscalatesOnce() throws Exception {
        String token = adminToken();
        JsonNode created = call(HttpMethod.POST, "/api/complaints",
                "{\"orgId\":" + orgId + ",\"targetType\":\"DRIVER\",\"targetId\":" + driverId + ","
                        + "\"complaintType\":\"ATTITUDE\",\"content\":\"沟通态度恶劣，拒接电话。\",\"operator\":\"tester\"}",
                token);
        long id = created.path("data").path("id").asLong();

        // 把截止时间改到过去，模拟「已经压了 30 小时」
        jdbc.update("UPDATE cs_complaint SET accept_time = DATE_SUB(NOW(), INTERVAL 30 HOUR), "
                + "deadline_time = DATE_SUB(NOW(), INTERVAL 6 HOUR) WHERE id = ?", id);

        JsonNode scan = call(HttpMethod.POST, "/api/complaints/scan-overdue?operator=tester", null, token);
        assertTrue(scan.path("data").asInt() >= 1, "应至少升级 1 条: " + scan);

        JsonNode detail = call(HttpMethod.GET, "/api/complaints/" + id, null, token).path("data");
        assertEquals(1, detail.path("complaint").path("overdueFlag").asInt());
        assertTrue(detail.path("slaBreached").asBoolean(), "已超过 24h 应判定超时");
        assertTrue(detail.path("remainHours").asLong() < 0, "剩余小时数应为负");

        // 幂等：再扫一次不再升级同一条
        JsonNode scan2 = call(HttpMethod.POST, "/api/complaints/scan-overdue?operator=tester", null, token);
        assertEquals(0, scan2.path("data").asInt(), "重复扫描不应重复升级: " + scan2);
    }

    // ------------------------------------------------------------------ 4. 评价

    @Test
    @Order(6)
    void ratingRequiresSignedWaybill() throws Exception {
        String token = adminToken();
        long gsId = call(HttpMethod.POST, "/api/goods-sources?operator=tester",
                "{\"orgId\":" + orgId + ",\"goodsName\":\"未签收货" + SUFFIX + "\",\"weight\":5,"
                        + "\"freightPrice\":800,\"loadAddr\":\"厦门\",\"unloadAddr\":\"上海\"}",
                token).path("data").asLong();
        long draft = call(HttpMethod.POST, "/api/waybills",
                "{\"orgId\":" + orgId + ",\"goodsSourceId\":" + gsId + ",\"operateBy\":\"tester\"}",
                token).path("data").asLong();

        JsonNode r = call(HttpMethod.POST, "/api/ratings",
                "{\"waybillId\":" + draft + ",\"direction\":\"SHIPPER_TO_DRIVER\",\"score\":5}", token);
        assertEquals("B0002", r.path("code").asText(), "未签收运单不能评价: " + r);
    }

    @Test
    @Order(7)
    void badRatingDeductsCreditAndCannotRepeat() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/ratings",
                "{\"waybillId\":" + signedWaybillId + ",\"direction\":\"SHIPPER_TO_DRIVER\",\"score\":2,"
                        + "\"punctuality\":2,\"service\":3,\"cargo\":5,\"attitude\":2,"
                        + "\"tags\":\"迟到,沟通不及时\",\"content\":\"到场比计划晚三小时\",\"operator\":\"tester\"}",
                token);
        assertEquals("00000", r.path("code").asText(), r.toString());
        JsonNode rating = r.path("data");
        assertEquals(driverId, rating.path("targetId").asLong());
        assertEquals("DRIVER", rating.path("targetType").asText());
        assertEquals(2, rating.path("score").asInt());

        // 差评 -5：90 → 85
        CsCreditSubject subject = creditService.detail("DRIVER", driverId).getSubject();
        assertEquals(85, subject.getScore().intValue());
        assertEquals(1, subject.getBadCount().intValue());
        assertEquals(1, subject.getTotalCount().intValue());

        // 同方向重复评价 → B0001
        JsonNode dup = call(HttpMethod.POST, "/api/ratings",
                "{\"waybillId\":" + signedWaybillId + ",\"direction\":\"SHIPPER_TO_DRIVER\",\"score\":5}", token);
        assertEquals("B0001", dup.path("code").asText(), "重复评价应被拒绝: " + dup);

        // 反向评价（司机评货主）不被上面那条挡住
        JsonNode reverse = call(HttpMethod.POST, "/api/ratings",
                "{\"waybillId\":" + signedWaybillId + ",\"direction\":\"DRIVER_TO_SHIPPER\",\"score\":5,"
                        + "\"tags\":\"装卸快\",\"content\":\"装卸利索\",\"operator\":\"tester\"}", token);
        assertEquals("00000", reverse.path("code").asText(), reverse.toString());

        // 评价人不能冒名：传一个不属于该运单的 raterId
        JsonNode forged = call(HttpMethod.POST, "/api/ratings",
                "{\"waybillId\":" + signedWaybillId + ",\"direction\":\"SHIPPER_TO_DRIVER\","
                        + "\"raterId\":999999,\"score\":5}", token);
        assertEquals("A0003", forged.path("code").asText(), "冒名评价应被拒绝: " + forged);
    }

    @Test
    @Order(8)
    void ratingSummaryAndList() throws Exception {
        String token = adminToken();
        JsonNode sum = call(HttpMethod.GET, "/api/ratings/summary?targetType=DRIVER&targetId=" + driverId,
                null, token).path("data");
        assertEquals(1, sum.path("count").asInt());
        assertEquals(2.0, sum.path("avgScore").asDouble(), 0.001);
        assertEquals(0.0, sum.path("goodRate").asDouble(), 0.001);
        assertEquals(1, sum.path("distribution").get(1).asInt(), "2 星应落在下标 1");

        JsonNode list = call(HttpMethod.GET, "/api/ratings?targetType=DRIVER&targetId=" + driverId, null, token);
        assertEquals(1, list.path("data").size());

        JsonNode low = call(HttpMethod.GET, "/api/ratings?scoreBelow=3", null, token);
        assertTrue(low.path("data").size() >= 1, "按星级筛选应能查到差评");
    }

    // ------------------------------------------------------------------ 5. 信用

    @Test
    @Order(9)
    void creditIsIdempotentAndAdjustable() throws Exception {
        // 同一投诉再计一次分：被唯一键挡住，分值不变（幂等）
        CsComplaint c = new CsComplaint();
        c.setComplaintNo(complaintNo);
        c.setTargetType("DRIVER");
        c.setTargetId(driverId);
        c.setComplaintType("DELAY");
        c.setUpdateBy("tester");
        int before = creditService.detail("DRIVER", driverId).getSubject().getScore();
        CreditChangeVo again = creditService.applyComplaint(c);
        assertEquals(false, again.isChanged(), "同一投诉不应重复扣分");
        assertEquals(before, creditService.detail("DRIVER", driverId).getSubject().getScore());

        String token = adminToken();
        JsonNode adj = call(HttpMethod.POST, "/api/credits/adjust",
                "{\"subjectType\":\"DRIVER\",\"subjectId\":" + driverId + ",\"subjectName\":\"客服测试司机\","
                        + "\"delta\":10,\"reason\":\"线下核实：延误因高速封路，非司机责任\",\"operator\":\"tester\"}", token);
        assertEquals("00000", adj.path("code").asText(), adj.toString());
        assertEquals(95, adj.path("data").path("scoreAfter").asInt());
        assertEquals("A", adj.path("data").path("levelAfter").asText());

        // 调账必须写原因
        JsonNode noReason = call(HttpMethod.POST, "/api/credits/adjust",
                "{\"subjectType\":\"DRIVER\",\"subjectId\":" + driverId + ",\"delta\":-5}", token);
        assertEquals("A0001", noReason.path("code").asText());

        JsonNode rank = call(HttpMethod.GET, "/api/credits?subjectType=DRIVER&limit=50", null, token);
        assertTrue(rank.path("data").size() >= 1);
    }
}
