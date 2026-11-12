package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.CaSignClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignResult;
import com.lukachangtong.freight.infra.client.mock.support.CryptoSimulator;

/**
 * CA 电子签章本地模拟。
 * 不是简单返回假字符串：用 JDK 自带 SHA256withRSA 做真实签名/验签闭环，
 * 并先确认 JCE 无限强度加密可用（8u161+），提前暴露 Illegal key size 类问题。
 */
public class MockCaSignClient implements CaSignClient {

    @Override
    public SignResult sign(SignRequest request) {
        if (request == null || request.getRequestId() == null || request.getContentHash() == null) {
            return SignResult.fail(null, "requestId and contentHash are required");
        }
        CryptoSimulator.Capability capability = CryptoSimulator.probe();
        if (!capability.isPassed()) {
            return SignResult.fail(request.getRequestId(),
                    "JCE capability not ready: " + capability.getDetail());
        }
        try {
            String signature = CryptoSimulator.sign(request.getContentHash());
            return SignResult.ok(request.getRequestId(), signature,
                    CryptoSimulator.SIGN_ALGORITHM, CryptoSimulator.MOCK_CERT_SERIAL_NO);
        } catch (Exception e) {
            return SignResult.fail(request.getRequestId(),
                    "mock sign failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean verify(String contentHash, String signatureBase64) {
        if (contentHash == null || signatureBase64 == null) {
            return false;
        }
        return CryptoSimulator.verify(contentHash, signatureBase64);
    }

    @Override
    public String queryCertExpiry(String certSerialNo) {
        // 模拟证书有效期，供 M3 的证件到期预警链路使用
        return "2027-12-31";
    }
}
