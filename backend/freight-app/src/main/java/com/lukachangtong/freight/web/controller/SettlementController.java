package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.settlement.dto.SettlementGenerateRequest;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.service.SettlementService;
import com.lukachangtong.freight.settlement.vo.PayResultVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 结算单：生成 → 确认 → 支付 → 作废。
 *
 * 所有金额均由服务端算，接口不接受「改金额」——运营要调价只能通过附加费/扣款字段，
 * 且结算单生成后不可改，必须作废重开，保证审计链完整。
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    @Resource
    private SettlementService settlementService;

    @GetMapping
    public Result<List<StSettlement>> list(@RequestParam(required = false) Long orgId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String bizType,
                                           @RequestParam(required = false) Integer limit) {
        return Result.ok(settlementService.list(orgId, status, bizType, limit));
    }

    @GetMapping("/{id}")
    public Result<StSettlement> detail(@PathVariable Long id) {
        return Result.ok(settlementService.detail(id));
    }

    @GetMapping("/by-waybill")
    public Result<StSettlement> byWaybill(@RequestParam Long waybillId,
                                          @RequestParam(required = false) String bizType) {
        return Result.ok(settlementService.byWaybill(waybillId, bizType));
    }

    @PostMapping
    public Result<StSettlement> generate(@RequestBody SettlementGenerateRequest request) {
        return Result.ok(settlementService.generate(request));
    }

    @PostMapping("/{id}/confirm")
    public Result<StSettlement> confirm(@PathVariable Long id,
                                        @RequestParam(required = false) String operator) {
        return Result.ok(settlementService.confirm(id, operator));
    }

    @PostMapping("/{id}/pay")
    public Result<PayResultVo> pay(@PathVariable Long id,
                                   @RequestParam(required = false) String operator) {
        return Result.ok(settlementService.pay(id, operator));
    }

    @PostMapping("/{id}/void")
    public Result<StSettlement> voidSettlement(@PathVariable Long id,
                                               @RequestParam String reason,
                                               @RequestParam(required = false) String operator) {
        return Result.ok(settlementService.voidSettlement(id, reason, operator));
    }
}
