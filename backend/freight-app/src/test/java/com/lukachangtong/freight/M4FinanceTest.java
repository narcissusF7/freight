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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4 财务域：计价含税拆分 → 结算单状态机 → 银企直联支付 → 钱包/流水入账 → 发票 → 提现 → 四方对账。
 *
 * 走真实 MySQL + mock 银行/税控，金额一律用字符串比较到分，
 * 因为「差一分钱」在这条链路上就是事故。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M4FinanceTest {

    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    /** 货源报价 5400 元（含税），用于验证 tax = 5400 - 4954.13 = 495.87 */
    private static final String FREIGHT_PRICE = "5400.00";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();

    private static Long driverId;
    private static Long waybillId;
    private static Long settlementId;
    private static Long invoiceId;
    private static Long withdrawId;
    private static String driverToken;

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
        assertEquals(200, resp.getStatusCodeValue(), path + " 非 200: " + resp.getBody());
        return mapper.readTree(resp.getBody());
    }

    private String adminToken() throws Exception {
        return call(HttpMethod.POST, "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"admin123\"}", null)
                .path("data").path("token").asText();
    }

    private long orgId(String token) throws Exception {
        return call(HttpMethod.GET, "/api/auth/me", null, token).path("data").path("orgId").asLong();
    }

    private void changeStatus(String token, long wbId, String to) throws Exception {
        JsonNode r = call(HttpMethod.POST, "/api/waybills/" + wbId + "/status",
                "{\"toStatus\":\"" + to + "\",\"operateBy\":\"tester\"}", token);
        assertEquals("00000", r.path("code").asText(), "流转到 " + to + " 失败: " + r);
    }

    // ------------------------------------------------------------------ 1. 造一张可结算的运单

    @Test
    @Order(1)
    void prepareSettleableWaybill() throws Exception {
        String token = adminToken();
        long org = orgId(token);

        driverId = call(HttpMethod.POST, "/api/drivers",
                "{\"orgId\":" + org + ",\"name\":\"财务测试司机" + SUFFIX + "\",\"phone\":\"13977" + SUFFIX.substring(0, 6) + "\"}",
                token).path("data").asLong();
        long vehicleId = call(HttpMethod.POST, "/api/vehicles",
                "{\"orgId\":" + org + ",\"plateNo\":\"沪F" + SUFFIX.substring(0, 5) + "\",\"vehicleType\":\"厢式\","
                        + "\"capacityTon\":30,\"driverId\":" + driverId + "}",
                token).path("data").asLong();
        call(HttpMethod.POST, "/api/drivers/" + driverId + "/audit?status=1", null, token);
        call(HttpMethod.POST, "/api/vehicles/" + vehicleId + "/audit?status=1", null, token);

        long gsId = call(HttpMethod.POST, "/api/goods-sources?operator=tester",
                "{\"orgId\":" + org + ",\"shipperId\":1,\"goodsName\":\"财务测试货" + SUFFIX + "\",\"weight\":18,"
                        + "\"freightPrice\":" + FREIGHT_PRICE + ",\"loadAddr\":\"上海\",\"unloadAddr\":\"杭州\"}",
                token).path("data").asLong();
        waybillId = call(HttpMethod.POST, "/api/waybills",
                "{\"orgId\":" + org + ",\"goodsSourceId\":" + gsId + ",\"driverId\":" + driverId
                        + ",\"vehicleId\":" + vehicleId + ",\"operateBy\":\"tester\"}",
                token).path("data").asLong();
        assertTrue(waybillId > 0);

        // 运单必须继承货源运价，否则结算只能拍脑袋
        JsonNode wb = call(HttpMethod.GET, "/api/waybills/" + waybillId, null, token).path("data");
        assertEquals(0, new BigDecimal(FREIGHT_PRICE).compareTo(new BigDecimal(wb.path("freightAmount").asText())),
                "运单应继承货源运费: " + wb);

        // 推到 RECEIPT_UPLOADED（结算前置条件）
        for (String s : new String[]{"PUBLISHED", "ACCEPTED", "LOADED", "IN_TRANSIT", "UNLOADED", "SIGNED"}) {
            changeStatus(token, waybillId, s);
        }
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        h.setBearerAuth(token);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("receipt".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "receipt.jpg";
            }
        });
        ResponseEntity<String> up = rest.postForEntity(
                base() + "/api/waybills/" + waybillId + "/receipts?operator=tester",
                new HttpEntity<>(form, h), String.class);
        assertEquals("00000", mapper.readTree(up.getBody()).path("code").asText(), up.getBody());
        assertEquals("RECEIPT_UPLOADED",
                call(HttpMethod.GET, "/api/waybills/" + waybillId, null, token).path("data").path("status").asText());
    }

    // ------------------------------------------------------------------ 2. 计价与幂等

    @Test
    @Order(2)
    void generateSettlementSplitsTaxCorrectly() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/settlements?operator=tester",
                "{\"waybillId\":" + waybillId + ",\"loadFee\":200,\"unloadFee\":100,"
                        + "\"waitFee\":0,\"oilSubsidy\":0,\"deductAmount\":300}",
                token);
        assertEquals("00000", r.path("code").asText(), r.toString());
        JsonNode st = r.path("data");
        settlementId = st.path("id").asLong();

        // 5400 + 200 + 100 - 300 = 5400（含税）；不含税 5400/1.09 = 4954.13；税额差额 445.87
        assertAmount("5400.00", st.path("amount"));
        assertAmount("4954.13", st.path("netAmount"));
        assertAmount("445.87", st.path("taxAmount"));
        assertEquals("DRAFT", st.path("status").asText());
        assertEquals("DRIVER", st.path("partnerType").asText());
        assertEquals(driverId.longValue(), st.path("partnerId").asLong());

        // 幂等：再点一次生成，返回同一张单
        JsonNode again = call(HttpMethod.POST, "/api/settlements?operator=tester",
                "{\"waybillId\":" + waybillId + "}", token);
        assertEquals(settlementId.longValue(), again.path("data").path("id").asLong(), "同一运单不应生成第二张结算单");
    }

    @Test
    @Order(3)
    void cannotSettleWaybillWithoutReceipt() throws Exception {
        String token = adminToken();
        long org = orgId(token);
        long gsId = call(HttpMethod.POST, "/api/goods-sources?operator=tester",
                "{\"orgId\":" + org + ",\"goodsName\":\"未回单货" + SUFFIX + "\",\"weight\":10,"
                        + "\"freightPrice\":1000,\"loadAddr\":\"A\",\"unloadAddr\":\"B\"}",
                token).path("data").asLong();
        long wbId = call(HttpMethod.POST, "/api/waybills",
                "{\"orgId\":" + org + ",\"goodsSourceId\":" + gsId + ",\"operateBy\":\"tester\"}",
                token).path("data").asLong();
        JsonNode r = call(HttpMethod.POST, "/api/settlements?operator=tester",
                "{\"waybillId\":" + wbId + "}", token);
        assertEquals("B0002", r.path("code").asText(), "未上传回单的运单不能结算: " + r);
    }

    // ------------------------------------------------------------------ 3. 状态机 + 支付入账

    @Test
    @Order(4)
    void mustConfirmBeforePay() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/settlements/" + settlementId + "/pay?operator=tester", null, token);
        assertEquals("B0002", r.path("code").asText(), "DRAFT 直接支付应被拒绝: " + r);

        JsonNode c = call(HttpMethod.POST, "/api/settlements/" + settlementId + "/confirm?operator=tester", null, token);
        assertEquals("CONFIRMED", c.path("data").path("status").asText());
    }

    @Test
    @Order(5)
    void payDrivesSettlementWalletFlowAndWaybill() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/settlements/" + settlementId + "/pay?operator=tester", null, token);
        assertEquals("00000", r.path("code").asText(), "支付失败: " + r);
        JsonNode st = r.path("data").path("settlement");
        JsonNode pay = r.path("data").path("payment");
        assertEquals("PAID", st.path("status").asText());
        assertEquals("SUCCESS", pay.path("status").asText());
        assertNotNull(pay.path("bankSerialNo").asText());
        assertNotEquals("", pay.path("bankSerialNo").asText());

        // 运单推进到已结算
        assertEquals("SETTLED",
                call(HttpMethod.GET, "/api/waybills/" + waybillId, null, token).path("data").path("status").asText());

        // 钱包入账 5400，流水一条 SETTLE_IN
        JsonNode wallet = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("5400.00", wallet.path("balance"));
        assertAmount("0.00", wallet.path("frozen"));

        JsonNode flows = call(HttpMethod.GET,
                "/api/wallets/flows?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertEquals(1, flows.size());
        assertEquals("SETTLE_IN", flows.get(0).path("bizType").asText());
        assertEquals("IN", flows.get(0).path("direction").asText());
        assertAmount("5400.00", flows.get(0).path("amount"));
        assertAmount("5400.00", flows.get(0).path("balanceAfter"));

        // 重复支付必须拦住
        JsonNode again = call(HttpMethod.POST, "/api/settlements/" + settlementId + "/pay?operator=tester", null, token);
        assertEquals("B0001", again.path("code").asText(), "已支付的结算单不应重复付款: " + again);
    }

    // ------------------------------------------------------------------ 4. 发票

    @Test
    @Order(6)
    void issueInvoiceIdempotentlyAndVoidIt() throws Exception {
        String token = adminToken();
        JsonNode r = call(HttpMethod.POST, "/api/invoices",
                "{\"settlementId\":" + settlementId + ",\"title\":\"上海某某物流有限公司\","
                        + "\"taxNo\":\"91310000MA1K000000\",\"operator\":\"tester\"}",
                token);
        assertEquals("00000", r.path("code").asText(), "开票失败: " + r);
        JsonNode inv = r.path("data");
        invoiceId = inv.path("id").asLong();
        assertEquals("ISSUED", inv.path("status").asText());
        assertAmount("5400.00", inv.path("amount"));
        assertAmount("445.87", inv.path("taxAmount"), "发票税额必须与结算单一致");
        assertNotNull(inv.path("invoiceNo").asText());

        // 结算单自动置为已开票
        assertEquals("INVOICED",
                call(HttpMethod.GET, "/api/settlements/" + settlementId, null, token)
                        .path("data").path("status").asText());

        // 幂等：重复开票返回同一张
        JsonNode again = call(HttpMethod.POST, "/api/invoices",
                "{\"settlementId\":" + settlementId + ",\"title\":\"重复开票\"}", token);
        assertEquals(invoiceId.longValue(), again.path("data").path("id").asLong(), "同一结算单只应有一张票");

        // 红冲
        JsonNode v = call(HttpMethod.POST, "/api/invoices/" + invoiceId + "/void?reason=抬头有误&operator=tester", null, token);
        assertEquals("VOID", v.path("data").path("status").asText());
    }

    // ------------------------------------------------------------------ 5. 提现（冻结 / 驳回 / 付款 / 失败重试）

    @Test
    @Order(7)
    void withdrawFreezesThenPaysOrRejects() throws Exception {
        String token = adminToken();

        // 申请 1000 → 冻结，余额不变
        JsonNode apply = call(HttpMethod.POST, "/api/wallets/withdrawals",
                "{\"ownerType\":\"DRIVER\",\"ownerId\":" + driverId + ",\"amount\":1000,"
                        + "\"bankAccount\":\"6222020200001234\",\"bankName\":\"工商银行\",\"operator\":\"tester\"}",
                token);
        assertEquals("00000", apply.path("code").asText(), apply.toString());
        withdrawId = apply.path("data").path("id").asLong();
        JsonNode w1 = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("5400.00", w1.path("balance"));
        assertAmount("1000.00", w1.path("frozen"));

        // 驳回 → 解冻
        JsonNode rej = call(HttpMethod.POST,
                "/api/wallets/withdrawals/" + withdrawId + "/audit?pass=false&remark=信息不全&operator=tester", null, token);
        assertEquals("REJECTED", rej.path("data").path("status").asText());
        JsonNode w2 = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("0.00", w2.path("frozen"));

        // 再申请 1000 → 审核通过即付款 → 余额 4400，出账流水
        long wid = call(HttpMethod.POST, "/api/wallets/withdrawals",
                "{\"ownerType\":\"DRIVER\",\"ownerId\":" + driverId + ",\"amount\":1000,"
                        + "\"bankAccount\":\"6222020200001234\",\"operator\":\"tester\"}", token)
                .path("data").path("id").asLong();
        JsonNode ok = call(HttpMethod.POST,
                "/api/wallets/withdrawals/" + wid + "/audit?pass=true&operator=tester", null, token);
        assertEquals("PAID", ok.path("data").path("status").asText(), "提现失败: " + ok);

        JsonNode w3 = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("4400.00", w3.path("balance"));
        assertAmount("0.00", w3.path("frozen"));
        assertAmount("1000.00", w3.path("totalOut"));

        JsonNode flows = call(HttpMethod.GET,
                "/api/wallets/flows?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertEquals(2, flows.size());
        assertEquals("WITHDRAW_OUT", flows.get(0).path("bizType").asText());
        assertEquals("OUT", flows.get(0).path("direction").asText());
    }

    @Test
    @Order(8)
    void failedWithdrawKeepsMoneyAndStaysRetryable() throws Exception {
        String token = adminToken();
        // 收款账号含 FAIL → mock 银行确定性失败（与短信 mock 同一约定）
        long wid = call(HttpMethod.POST, "/api/wallets/withdrawals",
                "{\"ownerType\":\"DRIVER\",\"ownerId\":" + driverId + ",\"amount\":500,"
                        + "\"bankAccount\":\"FAIL-6222\",\"operator\":\"tester\"}", token)
                .path("data").path("id").asLong();
        JsonNode r = call(HttpMethod.POST,
                "/api/wallets/withdrawals/" + wid + "/audit?pass=true&operator=tester", null, token);
        assertEquals("C0002", r.path("code").asText(), "银行失败应返回 C0002: " + r);

        // 钱不能少、也不能解冻：状态停在 APPROVED 等重试
        JsonNode w = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("4400.00", w.path("balance"));
        assertAmount("500.00", w.path("frozen"));
        JsonNode detail = call(HttpMethod.GET,
                "/api/wallets/withdrawals?ownerId=" + driverId + "&status=APPROVED", null, token).path("data");
        assertTrue(detail.size() >= 1, "失败后应保留 APPROVED 待重试");

        // 驳回清理冻结，余额回到可用
        call(HttpMethod.POST, "/api/wallets/withdrawals/" + wid + "/audit?pass=false&remark=账号异常&operator=tester",
                null, token);
        JsonNode w2 = call(HttpMethod.GET,
                "/api/wallets/detail?ownerType=DRIVER&ownerId=" + driverId, null, token).path("data");
        assertAmount("4400.00", w2.path("balance"));
        assertAmount("0.00", w2.path("frozen"));
    }

    // ------------------------------------------------------------------ 6. 四方对账

    @Test
    @Order(9)
    void reconciliationIsBalancedAndDetectsTampering() throws Exception {
        String token = adminToken();
        String today = LocalDate.now().toString();

        JsonNode run = call(HttpMethod.POST, "/api/reconciliation/run?operator=tester", null, token);
        assertEquals("00000", run.path("code").asText(), run.toString());
        JsonNode recon = run.path("data").path("reconciliation");
        assertEquals("MATCH", recon.path("status").asText(),
                "四方应一致，差异：" + run.path("data").path("diffs"));
        assertEquals(0, recon.path("diffCount").asInt());
        // 当日结算/支付/流水三方金额必须完全相同
        assertEquals(0, new BigDecimal(recon.path("settlementAmount").asText())
                .compareTo(new BigDecimal(recon.path("paymentAmount").asText())), "结算与支付金额不一致");
        assertEquals(0, new BigDecimal(recon.path("paymentAmount").asText())
                .compareTo(new BigDecimal(recon.path("flowAmount").asText())), "支付与流水金额不一致");

        // 人为把钱包余额改掉 1 分 → 对账必须报 WALLET_BALANCE 差异
        Long walletId = jdbc.queryForObject(
                "select id from st_wallet where owner_type='DRIVER' and owner_id=?", Long.class, driverId);
        jdbc.update("update st_wallet set balance = balance + 0.01 where id = ?", walletId);
        JsonNode bad = call(HttpMethod.POST, "/api/reconciliation/run?operator=tester&bizDate=" + today, null, token);
        JsonNode badRecon = bad.path("data").path("reconciliation");
        assertEquals("DIFF", badRecon.path("status").asText(), "篡改余额后对账必须报差异");
        assertTrue(badRecon.path("diffCount").asInt() >= 1);
        boolean walletDiff = false;
        for (JsonNode d : bad.path("data").path("diffs")) {
            if ("WALLET_BALANCE".equals(d.path("diffType").asText())) {
                walletDiff = true;
                // 差异可人工标记处理并留痕
                call(HttpMethod.POST, "/api/reconciliation/diffs/" + d.path("id").asLong()
                        + "/handle?handleBy=tester&remark=已核实为手工调账", null, token);
            }
        }
        assertTrue(walletDiff, "应检出钱包余额与流水净额不一致: " + bad.path("data").path("diffs"));

        // 还原后重跑：覆盖为 MATCH（验证对账是可重算的快照，不堆垃圾）
        jdbc.update("update st_wallet set balance = balance - 0.01 where id = ?", walletId);
        JsonNode good = call(HttpMethod.POST, "/api/reconciliation/run?operator=tester&bizDate=" + today, null, token);
        assertEquals("MATCH", good.path("data").path("reconciliation").path("status").asText());
    }

    // ------------------------------------------------------------------ 7. 越权

    @Test
    @Order(10)
    void driverTokenCannotTouchFinanceApi() throws Exception {
        // 复用司机端登录：dev 环境已给审核通过的司机初始化密码
        JsonNode login = call(HttpMethod.POST, "/api/driver/login",
                "{\"phone\":\"13977" + SUFFIX.substring(0, 6) + "\",\"password\":\"123456\"}", null);
        if (!"00000".equals(login.path("code").asText())) {
            call(HttpMethod.POST, "/api/driver/password/init",
                    "{\"phone\":\"13977" + SUFFIX.substring(0, 6) + "\",\"smsCode\":\"123456\",\"newPassword\":\"123456\"}",
                    null);
            login = call(HttpMethod.POST, "/api/driver/login",
                    "{\"phone\":\"13977" + SUFFIX.substring(0, 6) + "\",\"password\":\"123456\"}", null);
        }
        driverToken = login.path("data").path("token").asText();

        ResponseEntity<String> resp = rest.exchange(base() + "/api/settlements", HttpMethod.GET,
                new HttpEntity<>(json(driverToken)), String.class);
        assertEquals(403, resp.getStatusCodeValue(), "司机令牌不能访问财务接口");
    }

    private void assertAmount(String expect, JsonNode actual) {
        assertAmount(expect, actual, null);
    }

    private void assertAmount(String expect, JsonNode actual, String message) {
        BigDecimal e = new BigDecimal(expect);
        BigDecimal a = new BigDecimal(actual.asText());
        assertEquals(0, e.compareTo(a), (message == null ? "" : message + " ") + "金额不符：期望 " + expect + "，实际 " + a);
    }
}
