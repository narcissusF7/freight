package com.lukachangtong.freight.settlement.service;

import com.lukachangtong.freight.settlement.dto.InvoiceIssueRequest;
import com.lukachangtong.freight.settlement.entity.StInvoice;

import java.util.List;

/** 电子发票：按结算单开票，红冲置 VOID。 */
public interface InvoiceService {

    /** 幂等：同一结算单只会开出一张票（request_id 唯一）。 */
    StInvoice issue(InvoiceIssueRequest request);

    StInvoice voidInvoice(Long invoiceId, String reason, String operator);

    List<StInvoice> list(Long orgId, String status, Long settlementId, Integer limit);
}
