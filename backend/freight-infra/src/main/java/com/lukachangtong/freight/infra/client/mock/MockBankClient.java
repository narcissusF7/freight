package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.BankClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentResult;
import com.lukachangtong.freight.infra.client.mock.support.TlsSimulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 银企直联本地模拟。
 * 关键点：① 先探测 TLS 能力，模拟真实 SDK 的握手前置条件；② requestId 幂等；
 * ③ 收款账号含 `FAIL` 时确定性失败——与 MockSmsClient 的 FAIL 约定一致，
 *    让「支付失败 → 支付单 FAILED → 重试 → 成功」这条链路可被自动化验证；
 * ④ 生成本地对账文件，供 M4 四方对账比对笔数与金额。
 * 本类不依赖 Spring，可用纯 javac 编译验证。
 */
public class MockBankClient implements BankClient {

    private final Map<String, PaymentResult> ledger = new ConcurrentHashMap<String, PaymentResult>();

    @Override
    public PaymentResult pay(PaymentRequest request) {
        if (request == null || request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            return PaymentResult.fail(null, "requestId is required");
        }
        String requestId = request.getRequestId();

        TlsSimulator.HandshakeResult tls = TlsSimulator.probe();
        if (!tls.isPassed()) {
            return PaymentResult.fail(requestId, "TLS capability not ready: " + tls.getDetail());
        }

        // 确定性失败：方便测试失败分支与重试，不需要真的去改银行配置
        if (request.getPayeeAccount() != null && request.getPayeeAccount().contains("FAIL")) {
            return PaymentResult.fail(requestId, "账户状态异常（mock：收款账号含 FAIL）");
        }

        PaymentResult existing = ledger.get(requestId);
        if (existing != null) {
            PaymentResult hit = PaymentResult.ok(requestId, existing.getBankSerialNo(),
                    tls.getPreferredProtocol());
            hit.setMessage("idempotent hit: returning first payment result");
            return hit;
        }

        PaymentResult result = PaymentResult.ok(requestId, "MOCK-BANK-" + requestId,
                tls.getPreferredProtocol());
        ledger.put(requestId, result);
        return result;
    }

    @Override
    public PaymentResult queryPayment(String requestId) {
        PaymentResult result = ledger.get(requestId);
        return result == null ? PaymentResult.fail(requestId, "no mock payment record") : result;
    }

    @Override
    public String fetchReconciliationFile(String bizDate) {
        StringBuilder sb = new StringBuilder("bizDate=").append(bizDate).append("\n");
        for (Map.Entry<String, PaymentResult> entry : ledger.entrySet()) {
            sb.append(entry.getKey()).append(",")
              .append(entry.getValue().getBankSerialNo()).append(",")
              .append(entry.getValue().getStatus()).append("\n");
        }
        return sb.toString();
    }
}
