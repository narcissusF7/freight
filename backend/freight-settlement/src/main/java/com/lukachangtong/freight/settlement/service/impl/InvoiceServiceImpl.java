package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.client.InvoiceClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceResult;
import com.lukachangtong.freight.settlement.dto.InvoiceIssueRequest;
import com.lukachangtong.freight.settlement.entity.StInvoice;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.mapper.StInvoiceMapper;
import com.lukachangtong.freight.settlement.mapper.StSettlementMapper;
import com.lukachangtong.freight.settlement.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票实现。
 *
 * 两条税务约束：
 * 1. 只有已确认（含已付款）的结算单能开票——「钱没结清先开票」在税务稽查里说不通；
 * 2. 金额、税额一律复制结算单，**不接受前端传值**，否则发票金额与账不符。
 *
 * 红冲只做本地作废标记：真实税控要调用红字发票接口，mock 环境无此能力，
 * 这里显式留了 TODO 而不是假装成功。
 */
@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    @Resource
    private StInvoiceMapper invoiceMapper;
    @Resource
    private StSettlementMapper settlementMapper;
    @Resource
    private InvoiceClient invoiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StInvoice issue(InvoiceIssueRequest req) {
        if (req == null || req.getSettlementId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结算单不能为空");
        }
        StSettlement st = settlementMapper.selectById(req.getSettlementId());
        if (st == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结算单不存在");
        }
        if (StSettlement.STATUS_DRAFT.equals(st.getStatus()) || StSettlement.STATUS_VOID.equals(st.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "结算单未确认或已作废，不能开票");
        }
        if (StSettlement.STATUS_PAYING.equals(st.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "结算单支付处理中，请支付完成后再开票");
        }

        String requestId = st.getSettlementNo() + ":INV";
        StInvoice existing = invoiceMapper.selectOne(new QueryWrapper<StInvoice>()
                .eq("request_id", requestId));
        if (existing != null && !StInvoice.STATUS_VOID.equals(existing.getStatus())) {
            // 幂等：同一结算单只出一张票
            return existing;
        }

        String title = req.getTitle() == null || req.getTitle().trim().isEmpty()
                ? defaultTitle(st) : req.getTitle().trim();

        InvoiceRequest ir = new InvoiceRequest();
        ir.setRequestId(requestId);
        ir.setWaybillNo(st.getWaybillNo());
        ir.setInvoiceTitle(title);
        ir.setTaxNo(req.getTaxNo());
        ir.setAmount(st.getAmount());

        InvoiceResult result;
        try {
            result = invoiceClient.issueInvoice(ir);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTEGRATION_FAILED, "开票调用失败：" + e.getMessage());
        }
        if (result == null || !result.isSuccess()) {
            throw new BizException(ErrorCode.INTEGRATION_FAILED,
                    "开票失败：" + (result == null ? "税控无响应" : result.getMessage()));
        }

        StInvoice inv = existing == null ? new StInvoice() : existing;
        inv.setInvoiceNo(result.getInvoiceNo());
        inv.setInvoiceCode(result.getInvoiceCode());
        inv.setOrgId(st.getOrgId());
        inv.setBizType(st.getBizType());
        inv.setSettlementId(st.getId());
        inv.setSettlementNo(st.getSettlementNo());
        inv.setWaybillNo(st.getWaybillNo());
        inv.setTitle(title);
        inv.setTaxNo(req.getTaxNo());
        inv.setAmount(st.getAmount());
        inv.setNetAmount(st.getNetAmount());
        inv.setTaxAmount(st.getTaxAmount());
        inv.setTaxRate(st.getTaxRate());
        inv.setStatus(StInvoice.STATUS_ISSUED);
        inv.setPdfUrl(result.getPdfUrl());
        inv.setRequestId(requestId);
        inv.setIssuedTime(LocalDateTime.now());
        inv.setVoidTime(null);
        inv.setVoidReason(null);
        inv.setCreateBy(req.getOperator());
        inv.setCreateTime(existing == null ? LocalDateTime.now() : existing.getCreateTime());
        inv.setUpdateTime(LocalDateTime.now());
        if (inv.getId() == null) {
            invoiceMapper.insert(inv);
        } else {
            invoiceMapper.updateById(inv);
        }

        if (StSettlement.STATUS_PAID.equals(st.getStatus())) {
            settlementMapper.update(null, new UpdateWrapper<StSettlement>()
                    .set("status", StSettlement.STATUS_INVOICED)
                    .set("update_by", req.getOperator())
                    .set("update_time", LocalDateTime.now())
                    .eq("id", st.getId())
                    .eq("status", StSettlement.STATUS_PAID));
        }
        log.info("开票成功 {} 结算单 {} 金额 {}", inv.getInvoiceNo(), st.getSettlementNo(), st.getAmount());
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StInvoice voidInvoice(Long invoiceId, String reason, String operator) {
        StInvoice inv = invoiceId == null ? null : invoiceMapper.selectById(invoiceId);
        if (inv == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "发票不存在");
        }
        if (StInvoice.STATUS_VOID.equals(inv.getStatus())) {
            return inv;
        }
        if (!StInvoice.STATUS_ISSUED.equals(inv.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "只有已开具的发票可以红冲");
        }
        // TODO 真实环境需调用税控「红字发票开具」接口，拿到红字发票号后再本地置 VOID
        invoiceMapper.update(null, new UpdateWrapper<StInvoice>()
                .set("status", StInvoice.STATUS_VOID)
                .set("void_time", LocalDateTime.now())
                .set("void_reason", reason)
                .set("update_time", LocalDateTime.now())
                .eq("id", invoiceId));
        return invoiceMapper.selectById(invoiceId);
    }

    @Override
    public List<StInvoice> list(Long orgId, String status, Long settlementId, Integer limit) {
        QueryWrapper<StInvoice> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        if (settlementId != null) {
            qw.eq("settlement_id", settlementId);
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return invoiceMapper.selectList(qw);
    }

    private String defaultTitle(StSettlement st) {
        return StSettlement.TYPE_RECEIVABLE.equals(st.getBizType()) ? "货主运费发票" : "司机运费发票";
    }
}
