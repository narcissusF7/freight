package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.api.finance.PaymentPostingCommand;
import com.lukachangtong.freight.api.finance.PaymentPostingPort;
import com.lukachangtong.freight.api.finance.WaybillSettlePort;
import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.entity.StWallet;
import com.lukachangtong.freight.settlement.entity.StWithdraw;
import com.lukachangtong.freight.settlement.mapper.StFundFlowMapper;
import com.lukachangtong.freight.settlement.mapper.StSettlementMapper;
import com.lukachangtong.freight.settlement.mapper.StWithdrawMapper;
import com.lukachangtong.freight.settlement.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功后的入账实现（由 payment 模块通过 {@link PaymentPostingPort} 调用）。
 *
 * **必须与支付单置 SUCCESS 在同一个事务里**：钱出去了但没记账，日终对账永远不平；
 * 记账失败时这里抛异常，支付单事务整体回滚，停在 PAYING 等补偿，不会产生无账资金。
 */
@Service
public class PaymentPostingServiceImpl implements PaymentPostingPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentPostingServiceImpl.class);

    @Resource
    private StSettlementMapper settlementMapper;
    @Resource
    private StWithdrawMapper withdrawMapper;
    @Resource
    private StFundFlowMapper flowMapper;
    @Resource
    private WalletService walletService;
    @Resource
    private WaybillSettlePort waybillSettlePort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostingResult post(PaymentPostingCommand cmd) {
        if (cmd == null || cmd.getPayType() == null) {
            return PostingResult.fail("入账指令为空");
        }
        if (PaymentPostingCommand.PAY_TYPE_SETTLE.equals(cmd.getPayType())) {
            return postSettle(cmd);
        }
        if (PaymentPostingCommand.PAY_TYPE_WITHDRAW.equals(cmd.getPayType())) {
            return postWithdraw(cmd);
        }
        return PostingResult.fail("未知支付类型：" + cmd.getPayType());
    }

    // ------------------------------------------------------------------ 结算付款入账

    private PostingResult postSettle(PaymentPostingCommand cmd) {
        StSettlement st = settlementMapper.selectById(cmd.getBizId());
        if (st == null) {
            return PostingResult.fail("结算单不存在：" + cmd.getBizId());
        }
        if (StSettlement.STATUS_PAID.equals(st.getStatus())
                || StSettlement.STATUS_INVOICED.equals(st.getStatus())
                || StSettlement.STATUS_CLOSED.equals(st.getStatus())) {
            // 幂等：已经入过账，返回既有流水，不重复加钱
            StFundFlow existing = findFlow(StFundFlow.TYPE_SETTLE_IN, st.getSettlementNo());
            return PostingResult.ok(existing == null ? null : existing.getFlowNo(),
                    existing == null ? null : existing.getBalanceAfter());
        }

        StFundFlow flow = walletService.credit(
                StWallet.OWNER_DRIVER, st.getPartnerId(), st.getOrgId(), st.getPartnerName(),
                StFundFlow.TYPE_SETTLE_IN, st.getSettlementNo(), cmd.getAmount(),
                "运单 " + st.getWaybillNo() + " 运费结算入账");

        settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                .set("status", StSettlement.STATUS_PAID)
                .set("paid_time", LocalDateTime.now())
                .set("update_time", LocalDateTime.now())
                .eq("id", st.getId())
                .eq("status", StSettlement.STATUS_PAYING));

        // 运单 RECEIPT_UPLOADED → SETTLED；这一步失败要回滚整个入账，否则运单永远停在未结算
        boolean advanced = waybillSettlePort.markSettled(st.getWaybillId(), st.getSettlementNo());
        log.info("结算入账完成 {} 金额 {} 运单推进={}", st.getSettlementNo(), cmd.getAmount(), advanced);
        return PostingResult.ok(flow.getFlowNo(), flow.getBalanceAfter());
    }

    // ------------------------------------------------------------------ 提现出账

    private PostingResult postWithdraw(PaymentPostingCommand cmd) {
        StWithdraw wd = withdrawMapper.selectById(cmd.getBizId());
        if (wd == null) {
            return PostingResult.fail("提现单不存在：" + cmd.getBizId());
        }
        if (StWithdraw.STATUS_PAID.equals(wd.getStatus())) {
            StFundFlow existing = findFlow(StFundFlow.TYPE_WITHDRAW_OUT, wd.getWithdrawNo());
            return PostingResult.ok(existing == null ? null : existing.getFlowNo(),
                    existing == null ? null : existing.getBalanceAfter());
        }
        StWallet wallet = walletService.byOwner(wd.getOwnerType(), wd.getOwnerId());
        if (wallet == null) {
            return PostingResult.fail("提现账户不存在");
        }
        BigDecimal amount = cmd.getAmount() == null ? wd.getAmount() : cmd.getAmount();

        // 提现申请时已经冻结，扣款前必须先解冻，否则可用余额不足会被自己的冻结挡住
        walletService.unfreeze(wallet.getId(), amount);
        StFundFlow flow = walletService.debit(wd.getOwnerType(), wd.getOwnerId(),
                StFundFlow.TYPE_WITHDRAW_OUT, wd.getWithdrawNo(), amount, "提现到账");

        withdrawMapper.update(null, new UpdateWrapper<StWithdraw>()
                .set("status", StWithdraw.STATUS_PAID)
                .set("pay_no", cmd.getPayNo())
                .set("update_time", LocalDateTime.now())
                .eq("id", wd.getId()));
        log.info("提现出账完成 {} 金额 {}", wd.getWithdrawNo(), amount);
        return PostingResult.ok(flow.getFlowNo(), flow.getBalanceAfter());
    }

    /** 幂等回查：同一业务单同一类型只可能有一条流水。 */
    private StFundFlow findFlow(String bizType, String bizNo) {
        return flowMapper.selectOne(new QueryWrapper<StFundFlow>()
                .eq("biz_type", bizType).eq("biz_no", bizNo));
    }
}
