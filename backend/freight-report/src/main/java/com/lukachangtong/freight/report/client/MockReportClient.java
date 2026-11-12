package com.lukachangtong.freight.report.client;

import com.lukachangtong.freight.infra.client.mock.support.CryptoSimulator;
import com.lukachangtong.freight.report.dto.ReportEnvelope;
import com.lukachangtong.freight.report.dto.ReportReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监管上报本地模拟。
 *
 * 关键点：
 * 1. **国密模拟**：用 CryptoSimulator 探测 JCE 能力并完成「签名/加密」闭环，
 *    不假装有真实 SM2/SM4，而是明确标注 mock，真实国密由 RealClientStub 在 M6 联调前替换；
 * 2. **幂等**：同一 messageRefNo 重复上传返回首次回执；
 * 3. **确定性失败**：业务单号含 `FAIL` 时返回失败，驱动「上报失败 → 补偿重试 → 成功」链路可验证；
 * 4. 令牌：mock 直接返回固定 token，但记录换取时间，便于验证「过期重取」逻辑。
 */
public class MockReportClient implements ReportClient {

    private static final Logger log = LoggerFactory.getLogger(MockReportClient.class);

    private final Map<String, ReportReceipt> ledger = new ConcurrentHashMap<String, ReportReceipt>();

    @Override
    public String authenticate(String senderCode, String authEndpoint) {
        // mock 令牌：真实环境由省监测系统身份验证接口返回
        String token = "MOCK-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("mock 身份验证：sender={} endpoint={} -> token={}", senderCode, authEndpoint, token);
        return token;
    }

    @Override
    public ReportReceipt upload(ReportEnvelope envelope, String token) {
        if (envelope == null || envelope.getMessageRefNo() == null || envelope.getMessageRefNo().isEmpty()) {
            return ReportReceipt.fail(null, "9001", "报文参考号不能为空");
        }
        String refNo = envelope.getMessageRefNo();

        // 幂等：同一报文号重复上传返回首次结果
        ReportReceipt existing = ledger.get(refNo);
        if (existing != null) {
            return existing;
        }

        // 国密能力前置检查（模拟真实平台的加密握手前置条件）
        CryptoSimulator.Capability cap = CryptoSimulator.probe();
        if (!cap.isPassed()) {
            return ReportReceipt.fail(refNo, "9002", "国密能力未就绪：" + cap.getDetail());
        }

        // 模拟加密：真实环境用 SM2 加密随机 KEY + SM4 加密内容，这里用 JCE 签名走通闭环
        try {
            envelope.setEncryptedKey(Base64.getEncoder().encodeToString(
                    ("MOCK-SM2-KEY:" + refNo).getBytes("UTF-8")));
            // CryptoSimulator.sign 返回 Base64 签名串，直接作为「加密内容」占位
            envelope.setEncryptedContent(CryptoSimulator.sign(
                    envelope.getBizNo() == null ? refNo : envelope.getBizNo()));
        } catch (Exception e) {
            return ReportReceipt.fail(refNo, "9003", "报文加密失败：" + e.getMessage());
        }

        // 确定性失败：业务单号含 FAIL 时拒绝，驱动补偿重试链路
        if (envelope.getBizNo() != null && envelope.getBizNo().contains("FAIL")) {
            ReportReceipt fail = ReportReceipt.fail(refNo, "2001", "单据校验失败（mock：业务单号含 FAIL）");
            ledger.put(refNo, fail);
            return fail;
        }

        ReportReceipt ok = ReportReceipt.ok(refNo, "RECEIPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ledger.put(refNo, ok);
        log.info("mock 上报成功：refNo={} docType={} bizNo={}", refNo, envelope.getDocType(), envelope.getBizNo());
        return ok;
    }
}
