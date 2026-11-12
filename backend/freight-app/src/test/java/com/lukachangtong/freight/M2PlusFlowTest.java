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
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2+ 真实联调测试：证照 OCR、证件到期预警、车货匹配、电子合同签署/验签、异常件与回单。
 * 与 M2FlowTest 共用 dev 环境（真实 MySQL 8 + Redis 7），全程走真实 HTTP。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class M2PlusFlowTest {

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

    // ---------------- 1. OCR + 到期预警 ----------------

    @Test
    void ocr_recognizes_license_and_expiry_alert_works() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // OCR：同一张图必须得到同一结果（mock 按内容哈希派生，可断言）
        JsonNode first = ocr(token, orgId, ("driver-license-" + s).getBytes(StandardCharsets.UTF_8), "DRIVER_LICENSE");
        JsonNode second = ocr(token, orgId, ("driver-license-" + s).getBytes(StandardCharsets.UTF_8), "DRIVER_LICENSE");
        assertTrue(first.path("success").asBoolean(), "OCR 应识别成功");
        assertFalse(first.path("name").asText().isEmpty(), "应返回姓名供预填");
        assertFalse(first.path("validTo").asText().isEmpty(), "应返回证件有效期");
        assertTrue(first.path("confidence").asDouble() > 0.5);
        assertEquals(first.path("name").asText(), second.path("name").asText(), "同图同结果（可复现）");
        assertFalse(first.path("fileUrl").asText().isEmpty(), "证照原件应留档并返回访问地址");

        // 到期预警：建一个 10 天后驾驶证到期的司机
        String expireDate = LocalDate.now().plusDays(10).toString();
        String body = String.format(
                "{\"orgId\":%d,\"name\":\"临期司机%s\",\"phone\":\"1380000%s\","
                        + "\"driverLicenseNo\":\"DL%s\",\"licenseExpireDate\":\"%s\"}",
                orgId, s, s, s, expireDate);
        ResponseEntity<String> created = rest.exchange(baseUrl() + "/api/drivers", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        long driverId = mapper.readTree(created.getBody()).path("data").asLong();

        ResponseEntity<String> alertResp = rest.exchange(
                baseUrl() + "/api/carrier/expiring?orgId=" + orgId + "&days=30", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, alertResp.getStatusCode());
        JsonNode alerts = mapper.readTree(alertResp.getBody()).path("data");
        JsonNode hit = null;
        for (JsonNode a : alerts) {
            if (a.path("bizId").asLong() == driverId && "驾驶证".equals(a.path("docType").asText())) {
                hit = a;
            }
        }
        assertNotNull(hit, "应能查到刚建的临期驾驶证预警");
        assertEquals("URGENT", hit.path("level").asText(), "10 天内到期应为 URGENT");
        assertTrue(hit.path("daysLeft").asLong() <= 30);
    }

    private JsonNode ocr(String token, long orgId, byte[] bytes, String type) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        h.setBearerAuth(token);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "license.jpg";
            }
        });
        ResponseEntity<String> resp = rest.postForEntity(
                baseUrl() + "/api/carrier/ocr?type=" + type + "&orgId=" + orgId,
                new HttpEntity<>(form, h), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody()).path("data");
    }

    // ---------------- 2. 车货匹配 ----------------

    @Test
    void matching_prefers_capacity_fit() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String validTo = LocalDate.now().plusYears(1).toString();

        // 货源：30 吨，装货地唯一，避免历史线路干扰
        String loadAddr = "match-" + s;
        String gsBody = String.format(
                "{\"orgId\":%d,\"goodsName\":\"匹配货%s\",\"weight\":30.0,\"freightPrice\":12000.00,"
                        + "\"loadAddr\":\"%s\",\"unloadAddr\":\"南京\"}",
                orgId, s, loadAddr);
        ResponseEntity<String> gsResp = rest.exchange(baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(gsBody, jsonToken(token)), String.class);
        long gsId = mapper.readTree(gsResp.getBody()).path("data").asLong();

        // 大车（35 吨，满足）+ 小车（20 吨，超载）
        long bigDriver = createDriver(token, orgId, "big" + s);
        long bigVehicle = createVehicle(token, orgId, bigDriver, "BIG" + s, 35.0, validTo);
        long smallDriver = createDriver(token, orgId, "sml" + s);
        long smallVehicle = createVehicle(token, orgId, smallDriver, "SML" + s, 20.0, validTo);

        // limit 取大些：库中会累积历史测试车辆，limit 过小会把本次的小车挤出候选
        ResponseEntity<String> matchResp = rest.exchange(
                baseUrl() + "/api/waybills/match?goodsSourceId=" + gsId + "&limit=200", HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, matchResp.getStatusCode());
        JsonNode list = mapper.readTree(matchResp.getBody()).path("data");
        assertTrue(list.size() >= 2, "应至少返回两个候选，实际 " + list.size());

        JsonNode big = null;
        JsonNode small = null;
        int prevScore = Integer.MAX_VALUE;
        for (JsonNode m : list) {
            // 结果必须按分数降序
            assertTrue(m.path("score").asInt() <= prevScore, "候选应按分数降序");
            prevScore = m.path("score").asInt();
            if (m.path("vehicleId").asLong() == bigVehicle) {
                big = m;
            }
            if (m.path("vehicleId").asLong() == smallVehicle) {
                small = m;
            }
        }
        assertNotNull(big, "大车应在候选中");
        assertNotNull(small, "小车应在候选中");
        assertTrue(big.path("capacityFits").asBoolean(), "35 吨车载 30 吨货应判定满足");
        assertFalse(small.path("capacityFits").asBoolean(), "20 吨车载 30 吨货应判定超载");
        assertTrue(big.path("score").asInt() > small.path("score").asInt(), "满足载重的车评分应更高");
        assertTrue(big.path("score").asInt() <= 100);
    }

    private long createDriver(String token, long orgId, String suffix) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"name\":\"司机%s\",\"phone\":\"138%08d\",\"driverLicenseNo\":\"D%s\"}",
                orgId, suffix, Math.abs(suffix.hashCode() % 100000000), suffix);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/drivers", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        long id = mapper.readTree(resp.getBody()).path("data").asLong();
        rest.postForEntity(baseUrl() + "/api/drivers/" + id + "/audit?status=1&operator=tester",
                new HttpEntity<>(jsonToken(token)), String.class);
        return id;
    }

    private long createVehicle(String token, long orgId, long driverId, String plate, double capacity,
                               String licenseExpire) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"plateNo\":\"%s\",\"vehicleType\":\"高栏\",\"capacityTon\":%.1f,"
                        + "\"axleCount\":3,\"driverId\":%d,\"vehicleLicenseExpireDate\":\"%s\","
                        + "\"insuranceExpireDate\":\"%s\"}",
                orgId, plate, capacity, driverId, licenseExpire, licenseExpire);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/vehicles", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        long id = mapper.readTree(resp.getBody()).path("data").asLong();
        rest.postForEntity(baseUrl() + "/api/vehicles/" + id + "/audit?status=1&operator=tester",
                new HttpEntity<>(jsonToken(token)), String.class);
        return id;
    }

    // ---------------- 3. 电子合同签署与验签 ----------------

    @Test
    void contract_sign_verify_and_tamper_rejected() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String createBody = String.format(
                "{\"orgId\":%d,\"contractType\":\"TRANSPORT\",\"partyA\":\"鹭卡畅通%s\",\"partyB\":\"司机%s\","
                        + "\"operateBy\":\"tester\"}", orgId, s, s);
        ResponseEntity<String> created = rest.exchange(baseUrl() + "/api/contracts", HttpMethod.POST,
                new HttpEntity<>(createBody, jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        long id = mapper.readTree(created.getBody()).path("data").asLong();

        // 双方签署
        JsonNode afterA = sign(token, id, "A");
        assertEquals(1, afterA.path("signStatus").asInt(), "仅甲方签署后状态应为 1");
        assertFalse(afterA.path("signatureA").asText().isEmpty(), "应记录甲方签名");

        JsonNode afterB = sign(token, id, "B");
        assertEquals(2, afterB.path("signStatus").asInt(), "双方签署后状态应为 2");
        assertFalse(afterB.path("contentHash").asText().isEmpty(), "应有内容摘要");

        // 验签：按库内正文 -> 通过
        assertTrue(verify(token, id, null), "库内正文验签应通过");
        // 验签：正文被篡改 -> 失败
        assertFalse(verify(token, id, "被篡改的合同正文"), "篡改正文后验签必须失败");

        // 重复签署应被拒绝（幂等保护）
        ResponseEntity<String> again = rest.exchange(baseUrl() + "/api/contracts/" + id + "/sign?party=A&operator=tester",
                HttpMethod.POST, new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, again.getStatusCode());
        assertEquals("B0001", mapper.readTree(again.getBody()).path("code").asText(),
                "重复签署应返回幂等冲突码 B0001");
    }

    private JsonNode sign(String token, long id, String party) throws Exception {
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/api/contracts/" + id + "/sign?party=" + party + "&operator=tester",
                HttpMethod.POST, new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("00000", json.path("code").asText(), "签署失败: " + json.path("message").asText());
        return json.path("data");
    }

    private boolean verify(String token, long id, String content) {
        StringBuilder url = new StringBuilder(baseUrl() + "/api/contracts/" + id + "/verify?party=A");
        if (content != null) {
            url.append("&content=").append(enc(content));
        }
        ResponseEntity<String> resp = rest.exchange(url.toString(), HttpMethod.POST,
                new HttpEntity<>(jsonToken(token)), String.class);
        try {
            return mapper.readTree(resp.getBody()).path("data").asBoolean();
        } catch (Exception e) {
            throw new IllegalStateException("验签响应解析失败: " + resp.getBody(), e);
        }
    }

    private static String enc(String v) {
        try {
            return java.net.URLEncoder.encode(v, "UTF-8");
        } catch (Exception e) {
            return v;
        }
    }

    // ---------------- 4. 异常件 + 回单 ----------------

    @Test
    void exception_report_and_receipt_upload_drive_status_machine() throws Exception {
        String token = login();
        long orgId = orgIdOf(token);
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        long driverId = createDriver(token, orgId, "ex" + s);
        long vehicleId = createVehicle(token, orgId, driverId, "EX" + s, 35.0,
                LocalDate.now().plusYears(1).toString());
        long gsId = publishGoods(token, orgId, s);
        long wbId = createWaybill(token, orgId, gsId, driverId, vehicleId);
        assertTrue(wbId > 0);

        changeStatus(token, wbId, "PUBLISHED");
        changeStatus(token, wbId, "ACCEPTED");

        // 上报异常：ACCEPTED -> EXCEPTION
        String exBody = "{\"exceptionType\":\"DELAY\",\"description\":\"高速封路延误\",\"operateBy\":\"tester\"}";
        ResponseEntity<String> exResp = rest.exchange(baseUrl() + "/api/waybills/" + wbId + "/exceptions",
                HttpMethod.POST, new HttpEntity<>(exBody, jsonToken(token)), String.class);
        long exId = mapper.readTree(exResp.getBody()).path("data").asLong();
        assertTrue(exId > 0);
        assertEquals("EXCEPTION", statusOf(token, wbId), "上报异常后运单应为 EXCEPTION");

        // 处理异常
        ResponseEntity<String> handled = rest.exchange(
                baseUrl() + "/api/waybills/exceptions/" + exId + "/handle", HttpMethod.POST,
                new HttpEntity<>("{\"handleRemark\":\"已协调绕行\",\"operateBy\":\"tester\"}", jsonToken(token)),
                String.class);
        assertEquals("00000", mapper.readTree(handled.getBody()).path("code").asText());
        ResponseEntity<String> exList = rest.exchange(baseUrl() + "/api/waybills/" + wbId + "/exceptions",
                HttpMethod.GET, new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(1, mapper.readTree(exList.getBody()).path("data").get(0).path("status").asInt(),
                "异常应标记为已处理");

        // 异常恢复后继续推进到 SIGNED
        changeStatus(token, wbId, "ACCEPTED");
        changeStatus(token, wbId, "LOADED");
        changeStatus(token, wbId, "IN_TRANSIT");
        changeStatus(token, wbId, "UNLOADED");
        changeStatus(token, wbId, "SIGNED");

        // 上传回单 -> 自动推进到 RECEIPT_UPLOADED
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        h.setBearerAuth(token);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("receipt-content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "receipt.jpg";
            }
        });
        ResponseEntity<String> upResp = rest.postForEntity(
                baseUrl() + "/api/waybills/" + wbId + "/receipts?operator=tester&remark=现场签收",
                new HttpEntity<>(form, h), String.class);
        long receiptId = mapper.readTree(upResp.getBody()).path("data").asLong();
        assertTrue(receiptId > 0, "回单应上传成功: " + upResp.getBody());
        assertEquals("RECEIPT_UPLOADED", statusOf(token, wbId), "回单上传后应自动流转到 RECEIPT_UPLOADED");

        // 回单核验
        ResponseEntity<String> verifyResp = rest.exchange(
                baseUrl() + "/api/waybills/receipts/" + receiptId + "/verify?operator=tester", HttpMethod.POST,
                new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(verifyResp.getBody()).path("code").asText());
        ResponseEntity<String> recList = rest.exchange(baseUrl() + "/api/waybills/" + wbId + "/receipts",
                HttpMethod.GET, new HttpEntity<>(jsonToken(token)), String.class);
        assertEquals(1, mapper.readTree(recList.getBody()).path("data").get(0).path("verified").asInt());
    }

    private long publishGoods(String token, long orgId, String suffix) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"goodsName\":\"货物%s\",\"weight\":28.0,\"freightPrice\":9000.00,"
                        + "\"loadAddr\":\"厦门\",\"unloadAddr\":\"南昌\"}", orgId, suffix);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/goods-sources", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private long createWaybill(String token, long orgId, long gsId, long driverId, long vehicleId) throws Exception {
        String body = String.format(
                "{\"orgId\":%d,\"goodsSourceId\":%d,\"driverId\":%d,\"vehicleId\":%d,\"operateBy\":\"tester\"}",
                orgId, gsId, driverId, vehicleId);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/waybills", HttpMethod.POST,
                new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(resp.getBody()).path("code").asText(),
                "生成运单失败: " + resp.getBody());
        return mapper.readTree(resp.getBody()).path("data").asLong();
    }

    private void changeStatus(String token, long wbId, String toStatus) throws Exception {
        String body = String.format("{\"toStatus\":\"%s\",\"operateBy\":\"tester\"}", toStatus);
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/waybills/" + wbId + "/status",
                HttpMethod.POST, new HttpEntity<>(body, jsonToken(token)), String.class);
        assertEquals("00000", mapper.readTree(resp.getBody()).path("code").asText(),
                "状态流转 " + toStatus + " 失败: " + resp.getBody());
    }

    private String statusOf(String token, long wbId) throws Exception {
        ResponseEntity<String> resp = rest.exchange(baseUrl() + "/api/waybills/" + wbId, HttpMethod.GET,
                new HttpEntity<>(jsonToken(token)), String.class);
        return mapper.readTree(resp.getBody()).path("data").path("status").asText();
    }
}
