package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 支付单：查询 / 手动执行 / 反查银行 / 关闭。
 *
 * 「执行」是独立入口：生产环境结算按 auto-execute=false 落 PENDING，
 * 由调度器或财务复核后调这里真正出款，出款动作永远可追溯、可重试。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @GetMapping
    public Result<List<StPaymentOrder>> list(@RequestParam(required = false) Long orgId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String payType,
                                             @RequestParam(required = false) Integer limit) {
        return Result.ok(paymentService.list(orgId, status, payType, limit));
    }

    @GetMapping("/{payNo}")
    public Result<StPaymentOrder> detail(@PathVariable String payNo) {
        return Result.ok(paymentService.byPayNo(payNo));
    }

    @PostMapping("/{payNo}/execute")
    public Result<StPaymentOrder> execute(@PathVariable String payNo) {
        return Result.ok(paymentService.execute(payNo));
    }

    /** 向银行反查：本地显示失败但银行实际成功时，这是唯一可信来源。 */
    @GetMapping("/{payNo}/bank")
    public Result<StPaymentOrder> queryBank(@PathVariable String payNo) {
        return Result.ok(paymentService.queryFromBank(payNo));
    }

    @PostMapping("/{payNo}/close")
    public Result<StPaymentOrder> close(@PathVariable String payNo,
                                        @RequestParam(required = false) String reason,
                                        @RequestParam(required = false) String operator) {
        return Result.ok(paymentService.close(payNo, reason, operator));
    }

    /** 银行对账文件原文（排障用，正常对账走 /api/reconciliation/run）。 */
    @GetMapping("/bank-file")
    public Result<String> bankFile(@RequestParam String bizDate) {
        return Result.ok(paymentService.bankReconciliationFile(bizDate));
    }
}
