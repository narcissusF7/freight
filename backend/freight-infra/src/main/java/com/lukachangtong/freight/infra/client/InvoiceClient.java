package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceResult;

/**
 * 电子发票客户端抽象：货主电子发票在线开具、线下发票归档查询。
 */
public interface InvoiceClient {

    InvoiceResult issueInvoice(InvoiceRequest request);

    InvoiceResult queryInvoice(String invoiceNo);
}
