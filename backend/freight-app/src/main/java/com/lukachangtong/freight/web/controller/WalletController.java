package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.settlement.dto.WithdrawApplyRequest;
import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.entity.StWallet;
import com.lukachangtong.freight.settlement.entity.StWithdraw;
import com.lukachangtong.freight.settlement.service.WalletService;
import com.lukachangtong.freight.settlement.service.WithdrawService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/** 钱包、资金流水与提现。 */
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Resource
    private WalletService walletService;
    @Resource
    private WithdrawService withdrawService;

    @GetMapping
    public Result<List<StWallet>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(walletService.list(orgId));
    }

    @GetMapping("/detail")
    public Result<StWallet> byOwner(@RequestParam String ownerType,
                                    @RequestParam Long ownerId) {
        return Result.ok(walletService.byOwner(ownerType.toUpperCase(), ownerId));
    }

    @GetMapping("/flows")
    public Result<List<StFundFlow>> flows(@RequestParam String ownerType,
                                          @RequestParam Long ownerId,
                                          @RequestParam(required = false) Integer limit) {
        return Result.ok(walletService.flows(ownerType.toUpperCase(), ownerId, limit));
    }

    // ---------------------------------------------------------------- 提现

    @PostMapping("/withdrawals")
    public Result<StWithdraw> apply(@RequestBody WithdrawApplyRequest request) {
        return Result.ok(withdrawService.apply(request));
    }

    @GetMapping("/withdrawals")
    public Result<List<StWithdraw>> withdrawals(@RequestParam(required = false) String ownerType,
                                                @RequestParam(required = false) Long ownerId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Integer limit) {
        return Result.ok(withdrawService.list(ownerType, ownerId, status, limit));
    }

    @PostMapping("/withdrawals/{id}/audit")
    public Result<StWithdraw> audit(@PathVariable Long id,
                                    @RequestParam boolean pass,
                                    @RequestParam(required = false) String remark,
                                    @RequestParam(required = false) String operator) {
        return Result.ok(withdrawService.audit(id, pass, remark, operator));
    }

    @PostMapping("/withdrawals/{id}/retry")
    public Result<StWithdraw> retry(@PathVariable Long id,
                                    @RequestParam(required = false) String operator) {
        return Result.ok(withdrawService.retryPay(id, operator));
    }
}
