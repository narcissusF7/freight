package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.api.finance.WaybillSettleInfo;
import com.lukachangtong.freight.api.finance.WaybillSettlePort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.payment.dto.CreatePaymentCommand;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.payment.service.PaymentService;
import com.lukachangtong.freight.settlement.dto.SettlementGenerateRequest;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.mapper.StSettlementMapper;
import com.lukachangtong.freight.settlement.service.SettlementService;
import com.lukachangtong.freight.settlement.support.FinNo;
import com.lukachangtong.freight.settlement.support.Money;
import com.lukachangtong.freight.settlement.vo.PayResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 结算实现。
 *
 * 编排顺序（顺序即业务规则）：
 * 1. 计价取运单约定运价，**不接受前端传基础运价**；
 * 2. 生成 DRAFT，财务确认后 CONFIRMED；
 * 3. 支付前把结算单置 PAYING（事务A）→ 调银行（事务外）→ 支付域在事务B里入账并回调 PAID；
 * 4. 失败退 CONFIRMED 允许重试，不留下半张 PAYING 的僵尸单。
 */
@Service
public class SettlementServiceImpl implements SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);

    /** 可结算的运单状态：回单已上传之后 */
    private static final List<String> SETTLEABLE = Arrays.asList(
            "RECEIPT_UPLOADED", "SETTLED", "REPORTED", "COMPLETED");

    /** 运单没记运价时的兜底单价（元/吨），用于历史数据补录 */
    private static final BigDecimal FALLBACK_PRICE_PER_TON = new BigDecimal("300");

    @Resource
    private StSettlementMapper settlementMapper;
    @Resource
    private WaybillSettlePort waybillSettlePort;
    @Resource
    private PaymentService paymentService;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Value("${freight.payment.auto-execute:true}")
    private boolean autoExecute;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StSettlement generate(SettlementGenerateRequest req) {
        if (req == null || req.getWaybillId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不能为空");
        }
        String bizType = normalizeType(req.getBizType());
        WaybillSettleInfo info = waybillSettlePort.loadInfo(req.getWaybillId());
        if (info == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        if (!SETTLEABLE.contains(info.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "运单状态「" + info.getStatus() + "」不能结算，需先完成签收并上传回单");
        }

        StSettlement existing = byWaybill(info.getWaybillId(), bizType);
        if (existing != null && !StSettlement.STATUS_VOID.equals(existing.getStatus())) {
            // 幂等：重复点「生成结算」返回原单，不重复计价
            return existing;
        }

        BigDecimal base = Money.scale(info.getFreightAmount());
        String baseRemark = null;
        if (!Money.positive(base)) {
            // 历史运单没有运价：按吨位兜底，并且在 remark 里写明，避免财务误以为是实际运价
            BigDecimal weight = Money.nz(info.getWeight());
            base = Money.scale(weight.multiply(FALLBACK_PRICE_PER_TON));
            baseRemark = "运单未记录运价，按 " + FALLBACK_PRICE_PER_TON + " 元/吨 × " + weight + " 吨估算";
        }

        BigDecimal loadFee = Money.scale(req.getLoadFee());
        BigDecimal unloadFee = Money.scale(req.getUnloadFee());
        BigDecimal waitFee = Money.scale(req.getWaitFee());
        BigDecimal oil = Money.scale(req.getOilSubsidy());
        BigDecimal deduct = Money.scale(req.getDeductAmount());
        BigDecimal amount = Money.scale(base.add(loadFee).add(unloadFee).add(waitFee).add(oil).subtract(deduct));
        if (!Money.positive(amount)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结算金额必须大于 0（基础 " + base + "，扣款 " + deduct + "）");
        }
        BigDecimal taxRate = req.getTaxRate() == null ? Money.DEFAULT_TAX_RATE : req.getTaxRate();
        BigDecimal net = Money.netOf(amount, taxRate);
        BigDecimal tax = Money.taxOf(amount, net);

        StSettlement st = new StSettlement();
        st.setSettlementNo(FinNo.settlement());
        st.setOrgId(info.getOrgId());
        st.setBizType(bizType);
        st.setWaybillId(info.getWaybillId());
        st.setWaybillNo(info.getWaybillNo());
        if (StSettlement.TYPE_RECEIVABLE.equals(bizType)) {
            st.setPartnerType("SHIPPER");
            st.setPartnerId(info.getShipperId());
            st.setPartnerName("货主");
        } else {
            st.setPartnerType("DRIVER");
            st.setPartnerId(info.getDriverId());
            st.setPartnerName(info.getDriverName());
        }
        st.setBaseAmount(base);
        st.setLoadFee(loadFee);
        st.setUnloadFee(unloadFee);
        st.setWaitFee(waitFee);
        st.setOilSubsidy(oil);
        st.setDeductAmount(deduct);
        st.setTaxRate(taxRate);
        st.setAmount(amount);
        st.setNetAmount(net);
        st.setTaxAmount(tax);
        st.setStatus(StSettlement.STATUS_DRAFT);
        st.setSettledDate(LocalDate.now());
        st.setRemark(joinRemark(req.getRemark(), baseRemark));
        st.setCreateBy(req.getOperator());
        st.setCreateTime(LocalDateTime.now());
        st.setUpdateTime(LocalDateTime.now());
        st.setVersion(0);
        settlementMapper.insert(st);
        log.info("生成结算单 {} 运单 {} 金额 {} 元（含税）", st.getSettlementNo(), st.getWaybillNo(), amount);
        return st;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StSettlement confirm(Long id, String operator) {
        StSettlement st = require(id);
        if (StSettlement.STATUS_CONFIRMED.equals(st.getStatus())
                || StSettlement.STATUS_PAYING.equals(st.getStatus())
                || StSettlement.STATUS_PAID.equals(st.getStatus())
                || StSettlement.STATUS_INVOICED.equals(st.getStatus())) {
            // 幂等：已确认过就直接返回，不报错
            return st;
        }
        if (StSettlement.STATUS_VOID.equals(st.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "已作废的结算单不能确认");
        }
        settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                .set("status", StSettlement.STATUS_CONFIRMED)
                .set("settled_date", LocalDate.now())
                .set("update_by", operator)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
        return require(id);
    }

    @Override
    public PayResultVo pay(Long id, String operator) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        final StPaymentOrder[] holder = new StPaymentOrder[1];

        // 事务A：状态置 PAYING + 建支付单（PENDING），保证「有单可查」
        tx.execute(status -> {
            StSettlement st = require(id);
            if (StSettlement.STATUS_PAYING.equals(st.getStatus())) {
                throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "该结算单正在支付处理中，请稍后刷新");
            }
            if (StSettlement.STATUS_PAID.equals(st.getStatus())
                    || StSettlement.STATUS_INVOICED.equals(st.getStatus())) {
                throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "该结算单已支付，请勿重复付款");
            }
            if (StSettlement.STATUS_DRAFT.equals(st.getStatus())) {
                throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "请先确认结算单再发起支付");
            }
            if (!StSettlement.STATUS_CONFIRMED.equals(st.getStatus())) {
                throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                        "当前状态「" + st.getStatus() + "」不能发起支付");
            }

            CreatePaymentCommand cmd = new CreatePaymentCommand();
            cmd.setOrgId(st.getOrgId());
            cmd.setPayType(StPaymentOrder.TYPE_SETTLE);
            cmd.setBizId(st.getId());
            cmd.setBizNo(st.getSettlementNo());
            cmd.setPayeeType(st.getPartnerType());
            cmd.setPayeeId(st.getPartnerId());
            cmd.setPayeeName(st.getPartnerName());
            // 收款账号：真实环境取司机银行卡；mock 环境用结算单号占位，账号含 FAIL 可模拟失败
            cmd.setPayeeAccount("6222" + st.getPartnerId() + "0000");
            cmd.setBankCode("MOCK");
            cmd.setAmount(st.getAmount());
            cmd.setOperator(operator);
            holder[0] = paymentService.createOrGet(cmd);

            settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                    .set("status", StSettlement.STATUS_PAYING)
                    .set("update_by", operator)
                    .set("update_time", LocalDateTime.now())
                    .eq("id", id));
            return null;
        });

        String payNo = holder[0].getPayNo();
        if (!autoExecute) {
            return new PayResultVo(require(id), paymentService.byPayNo(payNo), "支付单已提交，等待银行回执");
        }

        try {
            StPaymentOrder order = paymentService.execute(payNo);
            return new PayResultVo(require(id), order, "支付成功，款项已入司机钱包");
        } catch (RuntimeException e) {
            // 支付失败：把结算单退回 CONFIRMED，允许重新发起；不留下 PAYING 僵尸单
            markPayFailed(id, operator);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StSettlement voidSettlement(Long id, String reason, String operator) {
        StSettlement st = require(id);
        if (StSettlement.STATUS_PAID.equals(st.getStatus())
                || StSettlement.STATUS_INVOICED.equals(st.getStatus())
                || StSettlement.STATUS_PAYING.equals(st.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "已支付/开票的结算单不能作废，请走红冲流程");
        }
        if (StSettlement.STATUS_VOID.equals(st.getStatus())) {
            return st;
        }
        settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                .set("status", StSettlement.STATUS_VOID)
                .set("remark", joinRemark(st.getRemark(), "作废：" + reason))
                .set("update_by", operator)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
        return require(id);
    }

    @Override
    public StSettlement detail(Long id) {
        return require(id);
    }

    @Override
    public List<StSettlement> list(Long orgId, String status, String bizType, Integer limit) {
        QueryWrapper<StSettlement> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        if (bizType != null && !bizType.trim().isEmpty()) {
            qw.eq("biz_type", bizType.trim().toUpperCase());
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return settlementMapper.selectList(qw);
    }

    @Override
    public StSettlement byWaybill(Long waybillId, String bizType) {
        if (waybillId == null) {
            return null;
        }
        return settlementMapper.selectOne(new QueryWrapper<StSettlement>()
                .eq("waybill_id", waybillId)
                .eq("biz_type", normalizeType(bizType)));
    }

    // ------------------------------------------------------------------ 内部

    /** 支付失败回退：PAYING → CONFIRMED。用条件更新防止把已成功的单子改回去。 */
    private void markPayFailed(Long id, String operator) {
        try {
            settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                    .set("status", StSettlement.STATUS_CONFIRMED)
                    .set("update_by", operator)
                    .set("update_time", LocalDateTime.now())
                    .eq("id", id)
                    .eq("status", StSettlement.STATUS_PAYING));
        } catch (Exception e) {
            log.error("结算单支付失败回退异常 id={}", id, e);
        }
    }

    private StSettlement require(Long id) {
        StSettlement st = id == null ? null : settlementMapper.selectById(id);
        if (st == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结算单不存在：" + id);
        }
        return st;
    }

    private String normalizeType(String bizType) {
        if (bizType == null || bizType.trim().isEmpty()) {
            return StSettlement.TYPE_PAYABLE;
        }
        String v = bizType.trim().toUpperCase();
        if (!StSettlement.TYPE_PAYABLE.equals(v) && !StSettlement.TYPE_RECEIVABLE.equals(v)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知的结算类型：" + bizType);
        }
        return v;
    }

    private String joinRemark(String a, String b) {
        if (a == null || a.trim().isEmpty()) {
            return b;
        }
        if (b == null || b.trim().isEmpty()) {
            return a;
        }
        return a + "；" + b;
    }
}
