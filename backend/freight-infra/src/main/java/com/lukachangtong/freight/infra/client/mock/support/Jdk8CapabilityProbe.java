package com.lukachangtong.freight.infra.client.mock.support;

import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceResult;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentResult;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignResult;
import com.lukachangtong.freight.infra.client.mock.MockBankClient;
import com.lukachangtong.freight.infra.client.mock.MockCaSignClient;
import com.lukachangtong.freight.infra.client.mock.MockInvoiceClient;

import java.math.BigDecimal;

/**
 * 本地 M0/M1 验收探针：不依赖 Maven、不依赖 Spring，直接用本机 JDK 8 运行。
 * 验证 JDK 8u261+ 补丁线、TLS 能力、JCE/签章闭环，以及三家 SDK 模拟链路与幂等。
 *
 * 运行：scripts/verify-jdk8.sh（Git Bash）或 scripts/verify-jdk8.cmd（CMD）
 */
public final class Jdk8CapabilityProbe {

    private Jdk8CapabilityProbe() {
    }

    public static void main(String[] args) {
        boolean allPassed = true;

        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        int update = parseUpdate(javaVersion);
        boolean jdk8 = javaVersion.startsWith("1.8");
        boolean patchOk = jdk8 && update >= 261;
        System.out.println("[ENV]  java.version = " + javaVersion);
        System.out.println("[ENV]  java.vendor  = " + javaVendor);
        System.out.println("[ENV]  update       = " + update + " (require >= 261 for TLS 1.3)");
        allPassed &= printCheck("JDK 8u261+ patch level", patchOk,
                jdk8 ? "update=" + update : "not a JDK 8 runtime");

        TlsSimulator.HandshakeResult tls = TlsSimulator.probe();
        System.out.println("[TLS]  supported    = " + tls.getSupportedProtocols());
        System.out.println("[TLS]  preferred    = " + tls.getPreferredProtocol());
        allPassed &= printCheck("TLSv1.2 available (bank minimum)", tls.isTls12Supported(), tls.getDetail());
        System.out.println("[TLS]  TLSv1.3      = " + (tls.isTls13Supported() ? "YES" : "NO")
                + " (expected YES on 8u261+)");

        CryptoSimulator.Capability crypto = CryptoSimulator.probe();
        System.out.println("[JCE]  max AES key  = " + crypto.getMaxAesKeyLength());
        allPassed &= printCheck("AES-256 available (8u161+)", crypto.isAes256Available(), crypto.getDetail());
        allPassed &= printCheck("SHA256withRSA sign/verify", crypto.isSignVerifyPassed(), crypto.getDetail());

        // --- 银行：TLS 前置 + requestId 幂等 ---
        MockBankClient bank = new MockBankClient();
        PaymentRequest payment = new PaymentRequest();
        payment.setRequestId("PROBE-PAY-001");
        payment.setSettlementNo("ST-0001");
        payment.setPayeeAccount("6222****1234");
        payment.setAmount(new BigDecimal("12800.50"));
        payment.setBankCode("MOCK-BANK");
        PaymentResult first = bank.pay(payment);
        PaymentResult second = bank.pay(payment);
        boolean bankIdempotent = first.isSuccess()
                && first.getBankSerialNo() != null
                && first.getBankSerialNo().equals(second.getBankSerialNo());
        System.out.println("[BANK] protocol     = " + first.getTlsProtocol()
                + ", serial=" + first.getBankSerialNo());
        allPassed &= printCheck("bank mock payment + idempotency", bankIdempotent, first.getMessage());

        // --- 发票：模拟开票 ---
        MockInvoiceClient invoiceClient = new MockInvoiceClient();
        InvoiceRequest invoice = new InvoiceRequest();
        invoice.setRequestId("PROBE-INV-001");
        invoice.setWaybillNo("WB-0001");
        invoice.setInvoiceTitle("厦门示例货主");
        invoice.setTaxNo("91350200MOCK0001");
        invoice.setAmount(new BigDecimal("12800.50"));
        InvoiceResult invoiceResult = invoiceClient.issueInvoice(invoice);
        allPassed &= printCheck("invoice mock issue", invoiceResult.isSuccess(),
                String.valueOf(invoiceResult.getInvoiceNo()));

        // --- CA：真实签名/验签闭环 + 篡改必须被拒 ---
        MockCaSignClient ca = new MockCaSignClient();
        SignRequest signRequest = new SignRequest();
        signRequest.setRequestId("PROBE-SIGN-001");
        signRequest.setContractNo("CT-0001");
        signRequest.setContentHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        signRequest.setSignerId("DRIVER-0001");
        SignResult signResult = ca.sign(signRequest);
        boolean verifyOk = ca.verify(signRequest.getContentHash(), signResult.getSignature());
        boolean tamperRejected = !ca.verify(signRequest.getContentHash() + "tampered",
                signResult.getSignature());
        System.out.println("[CA]   algorithm    = " + signResult.getAlgorithm()
                + ", cert=" + signResult.getCertSerialNo());
        allPassed &= printCheck("CA mock sign/verify", signResult.isSuccess() && verifyOk,
                signResult.getMessage());
        allPassed &= printCheck("CA rejects tampered content", tamperRejected, "verify must fail");

        System.out.println("----------------------------------------------------------");
        System.out.println(allPassed ? "RESULT: ALL CHECKS PASSED (local JDK 8 + mock SDKs ready)"
                                    : "RESULT: SOME CHECKS FAILED - see above");
        if (!allPassed) {
            System.exit(1);
        }
    }

    private static boolean printCheck(String name, boolean passed, String detail) {
        System.out.println((passed ? "[PASS] " : "[FAIL] ") + name + " :: " + detail);
        return passed;
    }

    private static int parseUpdate(String version) {
        int idx = version == null ? -1 : version.indexOf('_');
        if (idx < 0) {
            return -1;
        }
        String tail = version.substring(idx + 1);
        String[] parts = tail.split("[^0-9]");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
