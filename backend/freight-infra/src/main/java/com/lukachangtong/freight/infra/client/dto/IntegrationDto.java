package com.lukachangtong.freight.infra.client.dto;

import java.math.BigDecimal;

/**
 * 外部集成 DTO（本地模拟阶段）。
 * 真实 SDK 接入时保持这些字段不变，只替换 client 实现，业务模块不受影响。
 * 金额一律 BigDecimal，禁止 double。
 */
public final class IntegrationDto {

    private IntegrationDto() {
    }

    public static class PaymentRequest {
        private String requestId;          // 幂等键，同一键重复提交必须返回首次结果
        private String settlementNo;
        private String payeeAccount;
        private BigDecimal amount;
        private String bankCode;

        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getSettlementNo() { return settlementNo; }
        public void setSettlementNo(String v) { this.settlementNo = v; }
        public String getPayeeAccount() { return payeeAccount; }
        public void setPayeeAccount(String v) { this.payeeAccount = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
        public String getBankCode() { return bankCode; }
        public void setBankCode(String v) { this.bankCode = v; }
    }

    public static class PaymentResult {
        private boolean success;
        private String requestId;
        private String bankSerialNo;
        private String status;
        private String tlsProtocol;        // 记录本次模拟握手使用的协议，便于联调对照
        private String message;

        public static PaymentResult ok(String requestId, String serialNo, String tlsProtocol) {
            PaymentResult r = new PaymentResult();
            r.success = true;
            r.requestId = requestId;
            r.bankSerialNo = serialNo;
            r.tlsProtocol = tlsProtocol;
            r.status = "SUCCESS";
            r.message = "mock payment succeeded";
            return r;
        }

        public static PaymentResult fail(String requestId, String message) {
            PaymentResult r = new PaymentResult();
            r.success = false;
            r.requestId = requestId;
            r.status = "FAILED";
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getBankSerialNo() { return bankSerialNo; }
        public void setBankSerialNo(String v) { this.bankSerialNo = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getTlsProtocol() { return tlsProtocol; }
        public void setTlsProtocol(String v) { this.tlsProtocol = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }

    public static class InvoiceRequest {
        private String requestId;
        private String waybillNo;
        private String invoiceTitle;
        private String taxNo;
        private BigDecimal amount;

        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getWaybillNo() { return waybillNo; }
        public void setWaybillNo(String v) { this.waybillNo = v; }
        public String getInvoiceTitle() { return invoiceTitle; }
        public void setInvoiceTitle(String v) { this.invoiceTitle = v; }
        public String getTaxNo() { return taxNo; }
        public void setTaxNo(String v) { this.taxNo = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
    }

    public static class InvoiceResult {
        private boolean success;
        private String requestId;
        private String invoiceNo;
        private String invoiceCode;
        private String pdfUrl;
        private String message;

        public static InvoiceResult ok(String requestId, String invoiceNo, String code, String url) {
            InvoiceResult r = new InvoiceResult();
            r.success = true;
            r.requestId = requestId;
            r.invoiceNo = invoiceNo;
            r.invoiceCode = code;
            r.pdfUrl = url;
            r.message = "mock invoice issued";
            return r;
        }

        public static InvoiceResult fail(String requestId, String message) {
            InvoiceResult r = new InvoiceResult();
            r.success = false;
            r.requestId = requestId;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getInvoiceNo() { return invoiceNo; }
        public void setInvoiceNo(String v) { this.invoiceNo = v; }
        public String getInvoiceCode() { return invoiceCode; }
        public void setInvoiceCode(String v) { this.invoiceCode = v; }
        public String getPdfUrl() { return pdfUrl; }
        public void setPdfUrl(String v) { this.pdfUrl = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }

    public static class SignRequest {
        private String requestId;
        private String contractNo;
        private String contentHash;        // 业务侧算好的合同摘要，签章方不接收明文合同
        private String signerId;

        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String v) { this.contractNo = v; }
        public String getContentHash() { return contentHash; }
        public void setContentHash(String v) { this.contentHash = v; }
        public String getSignerId() { return signerId; }
        public void setSignerId(String v) { this.signerId = v; }
    }

    public static class SignResult {
        private boolean success;
        private String requestId;
        private String signature;          // Base64(SHA256withRSA)
        private String algorithm;
        private String certSerialNo;
        private String message;

        public static SignResult ok(String requestId, String signature, String algorithm, String certSerialNo) {
            SignResult r = new SignResult();
            r.success = true;
            r.requestId = requestId;
            r.signature = signature;
            r.algorithm = algorithm;
            r.certSerialNo = certSerialNo;
            r.message = "mock signing succeeded";
            return r;
        }

        public static SignResult fail(String requestId, String message) {
            SignResult r = new SignResult();
            r.success = false;
            r.requestId = requestId;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getSignature() { return signature; }
        public void setSignature(String v) { this.signature = v; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String v) { this.algorithm = v; }
        public String getCertSerialNo() { return certSerialNo; }
        public void setCertSerialNo(String v) { this.certSerialNo = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}
