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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3 消息域：四通道模板化发送、幂等、失败重试、站内信收件箱、模板覆盖、预警联动。
 *
 * 走真实 MySQL 8；短信/微信/推送走 Mock 客户端（FAIL 永久失败 / RETRY 首次失败），
 * 这样「失败→退避重试→成功」的链路是真实被验证的，不是靠断言假状态。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class M3MessageTest {

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

    private JsonNode messages(String token, String bizKey) throws Exception {
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/messages?bizType=TRACK_ALARM&bizKey=" + bizKey + "&limit=20", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data");
    }

    private int notify(String token, long orgId, String bizKey, String driverAddress) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"bizType\":\"TRACK_ALARM\",\"bizKey\":\"%s\","
                        + "\"vars\":{\"alarmType\":\"OVERSPEED\",\"alarmLevel\":\"HIGH\",\"waybillId\":9001,"
                        + "\"alarmTime\":\"2026-11-09T23:00:00\",\"content\":\"超速：连续 3 个点 98km/h\"},"
                        + "\"receivers\":[{\"type\":\"DRIVER\",\"id\":9001,\"address\":\"%s\"},{\"type\":\"ORG\",\"id\":%d}]}",
                orgId, bizKey, driverAddress, orgId);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/messages/notify", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("00000", json.path("code").asText(), "notify 失败: " + resp.getBody());
        return json.path("data").asInt();
    }

    private int dispatch(String token) throws Exception {
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/messages/dispatch", HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        return mapper.readTree(resp.getBody()).path("data").asInt();
    }

    private JsonNode resend(String token, long id) throws Exception {
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/messages/" + id + "/resend", HttpMethod.POST,
                new HttpEntity<>(null, jsonToken(token)), String.class);
        return mapper.readTree(resp.getBody()).path("data");
    }

    private String statusOf(JsonNode list, String channel) {
        for (JsonNode n : list) {
            if (channel.equals(n.path("channel").asText())) {
                return n.path("status").asText();
            }
        }
        return null;
    }

    /** 四通道各落一条记录：短信/微信/推送给司机，站内信给调度台（机构）。 */
    @Test
    void notify_creates_four_channel_records_and_dispatches() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "t-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        int created = notify(token, orgId, bizKey, "13800138000");
        assertEquals(4, created, "应生成 4 条记录（SMS/WECHAT/PUSH/INBOX）");

        JsonNode before = messages(token, bizKey);
        Set<String> channels = new HashSet<>();
        for (JsonNode n : before) {
            channels.add(n.path("channel").asText());
            assertEquals("PENDING", n.path("status").asText(), "投递前应全部为 PENDING");
            assertFalse(n.path("content").asText().contains("${"), "模板占位符必须渲染掉：" + n.path("content"));
        }
        assertEquals(4, channels.size(), "通道应覆盖四种，实际 " + channels);
        assertTrue(before.get(0).path("content").asText().contains("98km/h"), "变量应代入正文");

        dispatch(token);
        JsonNode after = messages(token, bizKey);
        for (JsonNode n : after) {
            assertEquals("SENT", n.path("status").asText(), n.path("channel").asText() + " 应已发送");
            assertFalse(n.path("providerMsgId").asText().isEmpty(), "应有服务商消息号");
        }
    }

    /** 幂等：同一 bizKey 重复通知不产生新记录（预警重算不会给司机重复发短信）。 */
    @Test
    void notify_is_idempotent_per_biz_key() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "idem-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        assertEquals(4, notify(token, orgId, bizKey, "13800138001"));
        assertEquals(0, notify(token, orgId, bizKey, "13800138001"), "同一 bizKey 不应重复落库");
        assertEquals(4, messages(token, bizKey).size());
    }

    /** 失败退避 + 手动重发：RETRY 号码首次失败、重发后成功。 */
    @Test
    void failed_send_is_retried_and_succeeds_on_resend() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "retry-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        notify(token, orgId, bizKey, "1390000RETRY");
        dispatch(token);
        JsonNode list = messages(token, bizKey);
        JsonNode sms = findChannel(list, "SMS");
        assertEquals("FAILED", sms.path("status").asText());
        assertEquals(1, sms.path("retryCount").asInt());
        assertFalse(sms.path("nextRetryTime").isNull(), "失败后应安排下次重试时间");
        assertFalse(sms.path("failReason").asText().isEmpty(), "应记录失败原因");

        JsonNode resent = resend(token, sms.path("id").asLong());
        assertEquals("SENT", resent.path("status").asText(), "重发后应成功（Mock 第二次放行）");
        assertFalse(resent.path("providerMsgId").asText().isEmpty());
    }

    /** 永久失败达到上限后置 GIVEN_UP，不再无限重试。 */
    @Test
    void permanent_failure_stops_after_max_retry() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "dead-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        notify(token, orgId, bizKey, "1390000FAIL");
        dispatch(token);
        JsonNode sms = findChannel(messages(token, bizKey), "SMS");
        assertEquals("FAILED", sms.path("status").asText());

        JsonNode current = sms;
        for (int i = 0; i < 4; i++) {
            current = resend(token, sms.path("id").asLong());
        }
        assertEquals("GIVEN_UP", current.path("status").asText(), "连续失败 5 次应放弃重试");
        assertEquals(5, current.path("retryCount").asInt());
    }

    /** 站内信：机构收件箱可见、未读筛选、标记已读生效。 */
    @Test
    void inbox_shows_unread_and_can_be_marked_read() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "inbox-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        notify(token, orgId, bizKey, "13800138002");
        dispatch(token);

        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/messages/inbox?orgId=" + orgId + "&unread=true&limit=100", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        JsonNode unread = mapper.readTree(resp.getBody()).path("data");
        long inboxId = 0;
        boolean found = false;
        for (JsonNode n : unread) {
            if (bizKey.equals(n.path("bizKey").asText())) {
                inboxId = n.path("id").asLong();
                found = true;
            }
        }
        assertTrue(found, "未读站内信应包含本条");

        ResponseEntity<String> read = rest.exchange(baseUrl() + "/api/messages/inbox/read", HttpMethod.POST,
                new HttpEntity<>("{\"ids\":[" + inboxId + "]}", jsonToken(token)), String.class);
        assertEquals(1, mapper.readTree(read.getBody()).path("data").asInt());

        ResponseEntity<String> again = rest.exchange(
                baseUrl() + "/api/messages/inbox?orgId=" + orgId + "&unread=true&limit=100", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        for (JsonNode n : mapper.readTree(again.getBody()).path("data")) {
            assertFalse(bizKey.equals(n.path("bizKey").asText()), "标记已读后不应再出现在未读列表");
        }
    }

    /** 机构级模板覆盖平台默认：停用后该节点不再发消息。 */
    @Test
    void org_template_overrides_platform_and_can_be_disabled() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizType = "TEST_BIZ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        String code = bizType + "_INBOX";

        String body = String.format(
                "{\"orgId\":%d,\"templateCode\":\"%s\",\"templateName\":\"测试模板\",\"bizType\":\"%s\","
                        + "\"channel\":\"INBOX\",\"receiverType\":\"ORG\",\"titleTpl\":\"节点提醒\","
                        + "\"contentTpl\":\"自定义文案 ${tag}\",\"enabled\":1,\"operateBy\":\"tester\"}",
                orgId, code, bizType);
        ResponseEntity<String> created = rest.exchange(baseUrl() + "/api/message-templates", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        long tplId = mapper.readTree(created.getBody()).path("data").asLong();
        assertTrue(tplId > 0, "模板创建失败: " + created.getBody());

        String bizKey = "tpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String notifyBody = String.format(
                "{\"orgId\":%d,\"bizType\":\"%s\",\"bizKey\":\"%s\",\"vars\":{\"tag\":\"A1\"},"
                        + "\"receivers\":[{\"type\":\"ORG\",\"id\":%d}]}", orgId, bizType, bizKey, orgId);
        ResponseEntity<String> sent = rest.exchange(baseUrl() + "/api/messages/notify", HttpMethod.POST,
                new HttpEntity<>(notifyBody, jsonToken(token)), String.class);
        assertEquals(1, mapper.readTree(sent.getBody()).path("data").asInt());

        ResponseEntity<String> list = rest.exchange(
                baseUrl() + "/api/messages?bizType=" + bizType + "&bizKey=" + bizKey, HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        JsonNode records = mapper.readTree(list.getBody()).path("data");
        assertEquals("自定义文案 A1", records.get(0).path("content").asText(), "应使用机构模板渲染");

        String disable = String.format(
                "{\"templateName\":\"测试模板\",\"bizType\":\"%s\",\"channel\":\"INBOX\",\"receiverType\":\"ORG\","
                        + "\"contentTpl\":\"自定义文案 ${tag}\",\"enabled\":0,\"operateBy\":\"tester\"}", bizType);
        rest.exchange(baseUrl() + "/api/message-templates/" + tplId, HttpMethod.PUT,
                new HttpEntity<>(disable, jsonToken(token)), String.class);

        String bizKey2 = bizKey + "-2";
        ResponseEntity<String> sent2 = rest.exchange(baseUrl() + "/api/messages/notify", HttpMethod.POST,
                new HttpEntity<>(notifyBody.replace(bizKey, bizKey2), jsonToken(token)), String.class);
        assertEquals(0, mapper.readTree(sent2.getBody()).path("data").asInt(), "模板停用后不应再发消息");

        // 清理测试模板（逻辑删除），避免污染后续的模板列表
        rest.exchange(baseUrl() + "/api/message-templates/" + tplId, HttpMethod.DELETE,
                new HttpEntity<>(jsonToken(token)), String.class);
    }

    /** 接收方没有可用地址时直接终态 GIVEN_UP，不进入无意义重试。 */
    @Test
    void missing_address_is_terminal_not_retried() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String bizKey = "noaddr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String body = String.format(
                "{\"orgId\":%d,\"bizType\":\"TRACK_ALARM\",\"bizKey\":\"%s\","
                        + "\"vars\":{\"alarmType\":\"DEVIATE\",\"content\":\"偏航 41km\",\"waybillId\":9002},"
                        + "\"receivers\":[{\"type\":\"DRIVER\",\"id\":999999}]}", orgId, bizKey);
        rest.exchange(baseUrl() + "/api/messages/notify", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);

        JsonNode list = messages(token, bizKey);
        assertTrue(list.size() >= 3, "短信/微信/推送三条都应落库（记录可见）");
        for (JsonNode n : list) {
            assertEquals("GIVEN_UP", n.path("status").asText());
            assertTrue(n.path("failReason").asText().contains("未找到接收地址"),
                    "失败原因应指明地址缺失：" + n.path("failReason").asText());
            assertEquals(0, n.path("retryCount").asInt(), "地址缺失不应重试");
        }
    }

    /** 预警 → 消息联动：运单产生预警后自动生成站内信，投递后可见。 */
    @Test
    void track_alarm_triggers_inbox_message() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        // 建带起终点坐标的运单（无司机，因此只有机构站内信这一个接收方）
        String gsBody = String.format(
                "{\"orgId\":%d,\"goodsName\":\"消息联动%s\",\"weight\":10.0,\"freightPrice\":3000.00,"
                        + "\"loadAddr\":\"厦门\",\"loadLat\":24.480000,\"loadLng\":118.090000,"
                        + "\"unloadAddr\":\"泉州\",\"unloadLat\":24.600000,\"unloadLng\":118.090000,"
                        + "\"operateBy\":\"tester\"}", orgId, suffix);
        ResponseEntity<String> gs = rest.exchange(baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(gsBody, jsonToken(token)), String.class);
        long gsId = mapper.readTree(gs.getBody()).path("data").asLong();
        ResponseEntity<String> wb = rest.exchange(baseUrl() + "/api/waybills", HttpMethod.POST,
                new HttpEntity<>(String.format("{\"orgId\":%d,\"goodsSourceId\":%d,\"operateBy\":\"tester\"}",
                        orgId, gsId), jsonToken(token)), String.class);
        long wbId = mapper.readTree(wb.getBody()).path("data").asLong();

        LocalDateTime base = LocalDateTime.now().minusMinutes(30).withNano(0);
        String points = String.format("{\"points\":["
                        + "{\"waybillId\":%d,\"lat\":24.481,\"lng\":118.090,\"speed\":95,\"accuracy\":10.0,\"gpsTime\":\"%s\",\"source\":\"TEST\"},"
                        + "{\"waybillId\":%d,\"lat\":24.482,\"lng\":118.090,\"speed\":96,\"accuracy\":10.0,\"gpsTime\":\"%s\",\"source\":\"TEST\"}]}",
                wbId, base.format(ISO), wbId, base.plusMinutes(1).format(ISO));
        rest.exchange(baseUrl() + "/api/tracks/batch", HttpMethod.POST,
                new HttpEntity<>(points, jsonToken(token)), String.class);

        ResponseEntity<String> msgResp = rest.exchange(
                baseUrl() + "/api/messages?bizType=TRACK_ALARM&channel=INBOX&limit=100", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        JsonNode inbox = mapper.readTree(msgResp.getBody()).path("data");
        boolean hit = false;
        for (JsonNode n : inbox) {
            if (n.path("content").asText().contains("超速") && n.path("bizKey").asText().startsWith("alarm:")) {
                hit = true;
            }
        }
        assertTrue(hit, "预警应通过 MessageNotifyPort 落到站内信");

        dispatch(token);
        ResponseEntity<String> sentResp = rest.exchange(
                baseUrl() + "/api/messages?bizType=TRACK_ALARM&channel=INBOX&limit=100", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        boolean sent = false;
        for (JsonNode n : mapper.readTree(sentResp.getBody()).path("data")) {
            if (n.path("content").asText().contains("超速") && "SENT".equals(n.path("status").asText())) {
                sent = true;
            }
        }
        assertTrue(sent, "站内信投递后状态应为 SENT");
    }

    private JsonNode findChannel(JsonNode list, String channel) {
        for (JsonNode n : list) {
            if (channel.equals(n.path("channel").asText())) {
                return n;
            }
        }
        throw new AssertionError("未找到通道记录：" + channel);
    }
}
