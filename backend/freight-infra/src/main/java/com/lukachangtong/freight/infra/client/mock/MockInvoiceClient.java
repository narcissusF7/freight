package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.InvoiceClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电子发票本地模拟：按 requestId 幂等开票，返回模拟票号与 PDF 地址。
 * 真实税控 SDK 接入时替换本实现，接口与 DTO 不变。
 */
public class MockInvoiceClient implements InvoiceClient {

    private final Map<String, InvoiceResult> issued = new ConcurrentHashMap<String, InvoiceResult>();

    @Override
    public InvoiceResult issueInvoice(InvoiceRequest request) {
        if (request == null || request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            return InvoiceResult.fail(null, "requestId is required");
        }
        String requestId = request.getRequestId();
        InvoiceResult existing = issued.get(requestId);
        if (existing != null) {
            return existing;
        }
        InvoiceResult result = InvoiceResult.ok(requestId,
                "MOCK-INV-" + requestId, "044032300111",
                "mock://invoice/" + requestId + ".pdf");
        issued.put(requestId, result);
        return result;
    }

    @Override
    public InvoiceResult queryInvoice(String invoiceNo) {
        for (InvoiceResult result : issued.values()) {
            if (invoiceNo != null && invoiceNo.equals(result.getInvoiceNo())) {
                return result;
            }
        }
        return InvoiceResult.fail(invoiceNo, "no mock invoice record");
    }
}
