package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.payment.service.PaymentService;
import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.entity.StReconDiff;
import com.lukachangtong.freight.settlement.entity.StReconciliation;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.entity.StSummary;
import com.lukachangtong.freight.settlement.entity.StWallet;
import com.lukachangtong.freight.settlement.mapper.StFundFlowMapper;
import com.lukachangtong.freight.settlement.mapper.StReconDiffMapper;
import com.lukachangtong.freight.settlement.mapper.StReconciliationMapper;
import com.lukachangtong.freight.settlement.mapper.StSettlementMapper;
import com.lukachangtong.freight.settlement.mapper.StSummaryMapper;
import com.lukachangtong.freight.settlement.mapper.StWalletMapper;
import com.lukachangtong.freight.settlement.service.ReconciliationService;
import com.lukachangtong.freight.settlement.service.WalletService;
import com.lukachangtong.freight.settlement.support.Money;
import com.lukachangtong.freight.settlement.vo.ReconResultVo;
import com.lukachangtong.freight.settlement.vo.WalletNetVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 四方对账实现。
 *
 * 为什么要有四方，而不是「结算对支付」两边比：
 * - 结算单是**业务口径**（该付多少）；
 * - 支付单是**外部口径**（银行说付了多少）；
 * - 资金流水是**记账口径**（账上记了多少）；
 * - 钱包余额是**存量口径**（钱还剩多少）。
 * 只比其中两方，都会漏掉「付了没记账」「记账了但余额被绕过」这类问题。
 *
 * 差异不自动调账：财务系统里自动改钱是事故来源，这里只出清单，由人工处理并留痕。
 */
@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    @Resource
    private StSettlementMapper settlementMapper;
    @Resource
    private StReconciliationMapper reconMapper;
    @Resource
    private StReconDiffMapper diffMapper;
    @Resource
    private StSummaryMapper summaryMapper;
    @Resource
    private StWalletMapper walletMapper;
    @Resource
    private StFundFlowMapper flowMapper;
    @Resource
    private WalletService walletService;
    @Resource
    private PaymentService paymentService;

    /**
     * 银行文件严格核对开关。
     * 生产必须 true：银行文件里查不到的单子，就是「钱可能没出去」。
     * 本地 mock 银行账本存在内存里，进程重启就丢，dev 环境会刷出一堆假差异，
     * 所以 dev 关闭严格模式（仍统计笔数金额，只是不据此判 DIFF）。
     */
    @Value("${freight.reconciliation.bank-file.strict:true}")
    private boolean bankFileStrict;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconResultVo run(LocalDate bizDate, String operator) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;

        // ---- 1. 结算单（业务口径）：当日支付成功的应付单
        List<StSettlement> settlements = settlementMapper.selectList(new QueryWrapper<StSettlement>()
                .eq("biz_type", StSettlement.TYPE_PAYABLE)
                .in("status", StSettlement.STATUS_PAID, StSettlement.STATUS_INVOICED, StSettlement.STATUS_CLOSED)
                .ge("paid_time", date.atStartOfDay())
                .lt("paid_time", date.plusDays(1).atStartOfDay()));
        BigDecimal settlementAmount = sumSettlement(settlements);

        // ---- 2. 支付单（外部口径）
        List<StPaymentOrder> payments = new ArrayList<StPaymentOrder>();
        BigDecimal paymentAmount = Money.ZERO;
        for (StPaymentOrder p : paymentService.succeededOn(date)) {
            if (StPaymentOrder.TYPE_SETTLE.equals(p.getPayType())) {
                payments.add(p);
                paymentAmount = paymentAmount.add(Money.nz(p.getAmount()));
            }
        }
        paymentAmount = Money.scale(paymentAmount);

        // ---- 3. 资金流水（记账口径）
        List<StFundFlow> settleInFlows = walletService.flowsOn(date, StFundFlow.DIR_IN, StFundFlow.TYPE_SETTLE_IN);
        BigDecimal flowAmount = Money.ZERO;
        for (StFundFlow f : settleInFlows) {
            flowAmount = flowAmount.add(Money.nz(f.getAmount()));
        }
        flowAmount = Money.scale(flowAmount);

        // 当日钱包净变动（含提现出金）
        List<StFundFlow> allFlows = walletService.flowsOn(date, null, null);
        BigDecimal walletDelta = Money.ZERO;
        for (StFundFlow f : allFlows) {
            walletDelta = StFundFlow.DIR_IN.equals(f.getDirection())
                    ? walletDelta.add(Money.nz(f.getAmount()))
                    : walletDelta.subtract(Money.nz(f.getAmount()));
        }
        walletDelta = Money.scale(walletDelta);

        // ---- 4. 银行对账文件（只核对单号是否都在，文件不含金额）
        Set<String> bankPayNos = parseBankFile(date);
        int bankCount = 0;
        BigDecimal bankAmount = Money.ZERO;
        for (StPaymentOrder p : payments) {
            if (bankPayNos.contains(p.getPayNo())) {
                bankCount++;
                bankAmount = bankAmount.add(Money.nz(p.getAmount()));
            }
        }
        bankAmount = Money.scale(bankAmount);

        List<StReconDiff> diffs = new ArrayList<StReconDiff>();

        // 结算 ≠ 支付：可能付了两次，或结算单状态没回写
        if (settlementAmount.compareTo(paymentAmount) != 0) {
            diffs.add(diff(StReconDiff.TYPE_SETTLE_VS_PAY, null, settlementAmount, paymentAmount,
                    "结算单合计 " + settlementAmount + "，支付单合计 " + paymentAmount));
        }
        // 支付 ≠ 流水：钱出去了没记账（最危险的一类）
        if (paymentAmount.compareTo(flowAmount) != 0) {
            diffs.add(diff(StReconDiff.TYPE_PAY_VS_FLOW, null, paymentAmount, flowAmount,
                    "支付合计 " + paymentAmount + "，入账流水合计 " + flowAmount));
        }
        // 银行文件缺失（严格模式才判差异，见字段注释）
        if (bankFileStrict) {
            for (StPaymentOrder p : payments) {
                if (!bankPayNos.contains(p.getPayNo())) {
                    diffs.add(diff(StReconDiff.TYPE_BANK_FILE, p.getPayNo(), Money.nz(p.getAmount()), Money.ZERO,
                            "银行对账文件中缺少该笔支付"));
                }
            }
        }
        // 钱包余额 = 流水净额（全量校验，不只看当日）
        diffs.addAll(checkWalletBalance());

        // ---- 5. 落库（同日同场景覆盖）
        StReconciliation recon = reconMapper.selectOne(new QueryWrapper<StReconciliation>()
                .eq("biz_date", date).eq("scene", StReconciliation.SCENE_DAILY));
        if (recon == null) {
            recon = new StReconciliation();
            recon.setBizDate(date);
            recon.setScene(StReconciliation.SCENE_DAILY);
            recon.setRunTime(LocalDateTime.now());
        } else {
            diffMapper.delete(new QueryWrapper<StReconDiff>().eq("recon_id", recon.getId()));
        }
        BigDecimal diffAmount = Money.ZERO;
        for (StReconDiff d : diffs) {
            diffAmount = diffAmount.add(Money.nz(d.getDiffAmount()).abs());
        }
        recon.setSettlementCount(settlements.size());
        recon.setSettlementAmount(settlementAmount);
        recon.setPaymentCount(payments.size());
        recon.setPaymentAmount(paymentAmount);
        recon.setFlowCount(settleInFlows.size());
        recon.setFlowAmount(flowAmount);
        recon.setWalletDelta(walletDelta);
        recon.setBankCount(bankCount);
        recon.setBankAmount(bankAmount);
        recon.setDiffCount(diffs.size());
        recon.setDiffAmount(Money.scale(diffAmount));
        recon.setStatus(diffs.isEmpty() ? StReconciliation.STATUS_MATCH : StReconciliation.STATUS_DIFF);
        recon.setRunBy(operator);
        recon.setRunTime(LocalDateTime.now());
        recon.setRemark(diffs.isEmpty() ? "四方一致" : "存在 " + diffs.size() + " 项差异，待人工处理");
        if (recon.getId() == null) {
            reconMapper.insert(recon);
        } else {
            reconMapper.updateById(recon);
        }
        for (StReconDiff d : diffs) {
            d.setReconId(recon.getId());
            diffMapper.insert(d);
        }

        writeSummary(date, settlements);
        log.info("日终对账 {} 结算/支付/流水={}/{}/{} 差异={} 状态={}", date, settlementAmount,
                paymentAmount, flowAmount, diffs.size(), recon.getStatus());
        return new ReconResultVo(recon, diffs);
    }

    @Override
    public StReconciliation latest(LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        return reconMapper.selectOne(new QueryWrapper<StReconciliation>()
                .eq("biz_date", date).eq("scene", StReconciliation.SCENE_DAILY));
    }

    @Override
    public List<StReconDiff> diffs(Long reconId) {
        if (reconId == null) {
            return new ArrayList<StReconDiff>();
        }
        return diffMapper.selectList(new QueryWrapper<StReconDiff>()
                .eq("recon_id", reconId).orderByDesc("id"));
    }

    @Override
    public int handleDiff(Long diffId, String handleBy, String remark) {
        StReconDiff diff = diffId == null ? null : diffMapper.selectById(diffId);
        if (diff == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "差异记录不存在");
        }
        diffMapper.update(null, new UpdateWrapper<StReconDiff>()
                .set("handled", 1)
                .set("handle_by", handleBy)
                .set("handle_remark", remark)
                .eq("id", diffId));
        return 1;
    }

    // ------------------------------------------------------------------ 内部

    private BigDecimal sumSettlement(List<StSettlement> list) {
        BigDecimal total = Money.ZERO;
        for (StSettlement s : list) {
            total = total.add(Money.nz(s.getAmount()));
        }
        return Money.scale(total);
    }

    /**
     * 解析银行对账文件：每行 `payNo,bankSerialNo,status`，首行为 bizDate。
     * 真实环境是银行下发的定长/CSV 文件，这里保持同样的「逐单核对」思路。
     */
    private Set<String> parseBankFile(LocalDate date) {
        Set<String> set = new HashSet<String>();
        try {
            String content = paymentService.bankReconciliationFile(date.toString());
            if (content == null) {
                return set;
            }
            for (String line : content.split("\n")) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 3 && "SUCCESS".equalsIgnoreCase(parts[2].trim())) {
                    set.add(parts[0].trim());
                }
            }
        } catch (Exception e) {
            log.warn("获取银行对账文件失败（按缺失处理）date={} err={}", date, e.getMessage());
        }
        return set;
    }

    /** 钱包余额 vs 流水净额。任何绕过流水改余额的实现都会在这里爆出来。 */
    private List<StReconDiff> checkWalletBalance() {
        List<StReconDiff> diffs = new ArrayList<StReconDiff>();
        Map<Long, BigDecimal> netByWallet = new HashMap<Long, BigDecimal>();
        for (WalletNetVo vo : flowMapper.sumNetByWallet()) {
            netByWallet.put(vo.getWalletId(), Money.nz(vo.getNet()));
        }
        for (StWallet w : walletMapper.selectList(new QueryWrapper<StWallet>())) {
            BigDecimal expect = Money.scale(netByWallet.get(w.getId()));
            BigDecimal actual = Money.scale(w.getBalance());
            if (expect.compareTo(actual) != 0) {
                diffs.add(diff(StReconDiff.TYPE_WALLET_BALANCE, "WALLET-" + w.getId(), expect, actual,
                        "钱包余额与流水净额不一致：" + w.getOwnerType() + ":" + w.getOwnerId()));
            }
        }
        return diffs;
    }

    private StReconDiff diff(String type, String bizNo, BigDecimal expect, BigDecimal actual, String remark) {
        StReconDiff d = new StReconDiff();
        d.setDiffType(type);
        d.setBizNo(bizNo);
        d.setExpectAmount(Money.scale(expect));
        d.setActualAmount(Money.scale(actual));
        d.setDiffAmount(Money.scale(Money.nz(expect).subtract(Money.nz(actual))));
        d.setRemark(remark);
        d.setHandled(0);
        d.setCreateTime(LocalDateTime.now());
        return d;
    }

    /** 结算汇总（按机构 + 类型），看板与月结用。 */
    private void writeSummary(LocalDate date, List<StSettlement> paid) {
        Map<String, int[]> counts = new HashMap<String, int[]>();
        Map<String, BigDecimal[]> amounts = new HashMap<String, BigDecimal[]>();
        for (StSettlement s : paid) {
            String key = s.getOrgId() + ":" + s.getBizType();
            counts.computeIfAbsent(key, k -> new int[2])[1]++;
            BigDecimal[] arr = amounts.computeIfAbsent(key, k -> new BigDecimal[]{Money.ZERO});
            arr[0] = arr[0].add(Money.nz(s.getAmount()));
        }
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            String[] parts = e.getKey().split(":");
            Long orgId = Long.valueOf(parts[0]);
            String bizType = parts[1];
            StSummary summary = summaryMapper.selectOne(new QueryWrapper<StSummary>()
                    .eq("biz_date", date).eq("org_id", orgId).eq("biz_type", bizType));
            if (summary == null) {
                summary = new StSummary();
                summary.setBizDate(date);
                summary.setOrgId(orgId);
                summary.setBizType(bizType);
                summary.setCreateTime(LocalDateTime.now());
            }
            summary.setPaidCount(e.getValue()[1]);
            summary.setPaidAmount(Money.scale(amounts.get(e.getKey())[0]));
            if (summary.getId() == null) {
                summaryMapper.insert(summary);
            } else {
                summaryMapper.updateById(summary);
            }
        }
    }
}
