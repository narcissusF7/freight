package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.settlement.dto.InvoiceIssueRequest;
import com.lukachangtong.freight.settlement.entity.StInvoice;
import com.lukachangtong.freight.settlement.service.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/** 电子发票：开票 / 红冲 / 查询。 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Resource
    private InvoiceService invoiceService;

    @GetMapping
    public Result<List<StInvoice>> list(@RequestParam(required = false) Long orgId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long settlementId,
                                        @RequestParam(required = false) Integer limit) {
        return Result.ok(invoiceService.list(orgId, status, settlementId, limit));
    }

    @PostMapping
    public Result<StInvoice> issue(@RequestBody InvoiceIssueRequest request) {
        return Result.ok(invoiceService.issue(request));
    }

    @PostMapping("/{id}/void")
    public Result<StInvoice> voidInvoice(@PathVariable Long id,
                                         @RequestParam String reason,
                                         @RequestParam(required = false) String operator) {
        return Result.ok(invoiceService.voidInvoice(id, reason, operator));
    }
}
