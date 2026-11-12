package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.payment.service.PaymentService;
import com.lukachangtong.freight.settlement.dto.WithdrawApplyRequest;
import com.lukachangtong.freight.settlement.entity.StWallet;
import com.lukachangtong.freight.settlement.entity.StWithdraw;
import com.lukachangtong.freight.settlement.mapper.StWithdrawMapper;
import com.lukachangtong.freight.settlement.service.WalletService;
import com.lukachangtong.freight.settlement.service.WithdrawService;
import com.lukachangtong.freight.settlement.support.FinNo;
import com.lukachangtong.freight.settlement.support.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提现实现。
 *
 * 冻结与解冻是提现正确性的关键：
 * 申请时 `frozen += amount`（钱还在账上，但不能再用），
 * 支付成功时先解冻再扣余额（否则会被自己的冻结挡住），驳回时只解冻。
 */
@Service
public class WithdrawServiceImpl implements WithdrawService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawServiceImpl.class);

    @Resource
    private StWithdrawMapper withdrawMapper;
    @Resource
    private WalletService walletService;
    @Resource
    private PaymentService paymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StWithdraw apply(WithdrawApplyRequest req) {
        if (req == null || req.getOwnerId() == null || !Money.positive(req.getAmount())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提现金额必须大于 0");
        }
        String ownerType = req.getOwnerType() == null ? StWallet.OWNER_DRIVER : req.getOwnerType().toUpperCase();
        StWallet wallet = walletService.byOwner(ownerType, req.getOwnerId());
        if (wallet == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账户不存在，无法提现");
        }
        BigDecimalGuard.check(req.getAmount());

        // 先冻结：后续审核期间这笔钱不能被别的提现重复占用
        walletService.freeze(wallet.getId(), Money.scale(req.getAmount()));

        StWithdraw wd = new StWithdraw();
        wd.setWithdrawNo(FinNo.withdraw());
        wd.setOrgId(wallet.getOrgId());
        wd.setOwnerType(ownerType);
        wd.setOwnerId(req.getOwnerId());
        wd.setOwnerName(wallet.getOwnerName());
        wd.setAmount(Money.scale(req.getAmount()));
        wd.setFee(Money.ZERO);
        wd.setActualAmount(Money.scale(req.getAmount()));
        wd.setBankAccount(req.getBankAccount());
        wd.setBankName(req.getBankName());
        wd.setStatus(StWithdraw.STATUS_APPLIED);
        wd.setCreateBy(req.getOperator());
        wd.setCreateTime(LocalDateTime.now());
        wd.setUpdateTime(LocalDateTime.now());
        withdrawMapper.insert(wd);
        log.info("提现申请 {} 金额 {}", wd.getWithdrawNo(), wd.getAmount());
        return wd;
    }

    @Override
    public StWithdraw audit(Long id, boolean pass, String remark, String operator) {
        StWithdraw wd = require(id);
        if (StWithdraw.STATUS_PAID.equals(wd.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "该提现已到账，不能重复审核");
        }
        if (!StWithdraw.STATUS_APPLIED.equals(wd.getStatus()) && !StWithdraw.STATUS_APPROVED.equals(wd.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "当前状态「" + wd.getStatus() + "」不能审核");
        }

        if (!pass) {
            reject(wd, remark, operator);
            return require(id);
        }

        StWallet wallet = walletService.byOwner(wd.getOwnerType(), wd.getOwnerId());
        if (wallet == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提现账户不存在");
        }
        withdrawMapper.update(null, new UpdateWrapper<StWithdraw>()
                .set("status", StWithdraw.STATUS_APPROVED)
                .set("audit_by", operator)
                .set("audit_time", LocalDateTime.now())
                .set("audit_remark", remark)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));

        return payOut(require(id));
    }

    @Override
    public StWithdraw retryPay(Long id, String operator) {
        StWithdraw wd = require(id);
        if (!StWithdraw.STATUS_APPROVED.equals(wd.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "只有审核通过的提现单可以重试付款");
        }
        return payOut(wd);
    }

    @Override
    public List<StWithdraw> list(String ownerType, Long ownerId, String status, Integer limit) {
        QueryWrapper<StWithdraw> qw = new QueryWrapper<>();
        if (ownerType != null && !ownerType.trim().isEmpty()) {
            qw.eq("owner_type", ownerType.trim().toUpperCase());
        }
        if (ownerId != null) {
            qw.eq("owner_id", ownerId);
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return withdrawMapper.selectList(qw);
    }

    @Override
    public StWithdraw detail(Long id) {
        return require(id);
    }

    // ------------------------------------------------------------------ 内部

    /** 发起银行付款。失败不回滚审核结果（钱还在冻结里），保留 APPROVED 供重试。 */
    private StWithdraw payOut(StWithdraw wd) {
        StPaymentOrder order;
        try {
            com.lukachangtong.freight.payment.dto.CreatePaymentCommand cmd =
                    new com.lukachangtong.freight.payment.dto.CreatePaymentCommand();
            cmd.setOrgId(wd.getOrgId());
            cmd.setPayType(StPaymentOrder.TYPE_WITHDRAW);
            cmd.setBizId(wd.getId());
            cmd.setBizNo(wd.getWithdrawNo());
            cmd.setPayeeType(wd.getOwnerType());
            cmd.setPayeeId(wd.getOwnerId());
            cmd.setPayeeName(wd.getOwnerName());
            cmd.setPayeeAccount(wd.getBankAccount());
            cmd.setBankCode("MOCK");
            cmd.setAmount(wd.getActualAmount());
            cmd.setOperator("system");
            order = paymentService.createOrGet(cmd);
            withdrawMapper.update(null, new UpdateWrapper<StWithdraw>()
                    .set("pay_no", order.getPayNo())
                    .set("update_time", LocalDateTime.now())
                    .eq("id", wd.getId()));
            order = paymentService.execute(order.getPayNo());
        } catch (RuntimeException e) {
            log.warn("提现付款失败，保留审核通过待重试 withdrawNo={} err={}", wd.getWithdrawNo(), e.getMessage());
            throw e;
        }
        return require(wd.getId());
    }

    private void reject(StWithdraw wd, String remark, String operator) {
        StWallet wallet = walletService.byOwner(wd.getOwnerType(), wd.getOwnerId());
        if (wallet != null) {
            walletService.unfreeze(wallet.getId(), wd.getAmount());
        }
        withdrawMapper.update(null, new UpdateWrapper<StWithdraw>()
                .set("status", StWithdraw.STATUS_REJECTED)
                .set("audit_by", operator)
                .set("audit_time", LocalDateTime.now())
                .set("audit_remark", remark)
                .set("update_time", LocalDateTime.now())
                .eq("id", wd.getId()));
    }

    private StWithdraw require(Long id) {
        StWithdraw wd = id == null ? null : withdrawMapper.selectById(id);
        if (wd == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "提现单不存在：" + id);
        }
        return wd;
    }

    /** 提现金额下限/上限校验（单笔上限可按机构配置）。 */
    private static final class BigDecimalGuard {
        private static final java.math.BigDecimal MIN = new java.math.BigDecimal("1.00");
        private static final java.math.BigDecimal MAX = new java.math.BigDecimal("200000.00");

        static void check(java.math.BigDecimal amount) {
            if (amount.compareTo(MIN) < 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单笔提现不能低于 " + MIN + " 元");
            }
            if (amount.compareTo(MAX) > 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单笔提现不能超过 " + MAX + " 元");
            }
        }
    }
}
