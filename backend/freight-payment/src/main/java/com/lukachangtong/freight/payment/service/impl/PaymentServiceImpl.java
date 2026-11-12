package com.lukachangtong.freight.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.api.finance.PaymentPostingCommand;
import com.lukachangtong.freight.api.finance.PaymentPostingPort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.client.BankClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentResult;
import com.lukachangtong.freight.payment.dto.CreatePaymentCommand;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.payment.mapper.StPaymentOrderMapper;
import com.lukachangtong.freight.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现。
 *
 * 关键实现点（改之前请读完整段）：
 * 1. **三段事务**：markPaying（事务A提交）→ 调银行（无事务）→ markSuccess/markFailed（事务B）。
 *    银行是外部系统，把它包进事务会出现「钱付了但 DB 回滚」或「DB 提交了但银行超时」两头都不一致的问题。
 * 2. **幂等靠 payNo**：payNo 就是银行 requestId，重试不换号，银行侧幂等返回首次结果。
 * 3. **入账在事务B内**：支付单 SUCCESS 与资金流水、钱包余额、结算单状态同一个事务，
 *    中间任何一步失败整笔回滚，不会出现「钱出去了但没记账」。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private StPaymentOrderMapper orderMapper;
    @Resource
    private BankClient bankClient;
    @Resource
    private PlatformTransactionManager transactionManager;
    /** 结算域实现；裁剪部署（只跑支付模块）时可能不存在 */
    @Resource
    private ObjectProvider<PaymentPostingPort> postingPortProvider;

    @Override
    public StPaymentOrder createOrGet(CreatePaymentCommand cmd) {
        validate(cmd);
        StPaymentOrder existing = findOpen(cmd.getPayType(), cmd.getBizId());
        if (existing != null) {
            return existing;
        }
        StPaymentOrder order = new StPaymentOrder();
        order.setPayNo("PY" + LocalDateTime.now().format(TS)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        order.setOrgId(cmd.getOrgId());
        order.setPayType(cmd.getPayType());
        order.setBizId(cmd.getBizId());
        order.setBizNo(cmd.getBizNo());
        order.setPayeeType(cmd.getPayeeType());
        order.setPayeeId(cmd.getPayeeId());
        order.setPayeeName(cmd.getPayeeName());
        order.setPayeeAccount(cmd.getPayeeAccount());
        order.setBankCode(cmd.getBankCode() == null ? "MOCK" : cmd.getBankCode());
        order.setAmount(cmd.getAmount());
        order.setStatus(StPaymentOrder.STATUS_PENDING);
        order.setRetryCount(0);
        order.setCreateBy(cmd.getOperator());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    @Override
    public StPaymentOrder execute(String payNo) {
        StPaymentOrder order = requireOrder(payNo);
        if (StPaymentOrder.STATUS_SUCCESS.equals(order.getStatus())) {
            // 幂等：已成功不再重复付款
            return order;
        }
        if (StPaymentOrder.STATUS_CLOSED.equals(order.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "支付单已关闭，不能执行");
        }

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            orderMapper.update(null, new UpdateWrapper<StPaymentOrder>()
                    .set("status", StPaymentOrder.STATUS_PAYING)
                    .set("execute_time", LocalDateTime.now())
                    .set("update_time", LocalDateTime.now())
                    .eq("id", order.getId()));
            return null;
        });
        order.setStatus(StPaymentOrder.STATUS_PAYING);

        // ---- 事务外调银行 ----
        PaymentRequest request = new PaymentRequest();
        request.setRequestId(order.getPayNo());
        request.setSettlementNo(order.getBizNo());
        request.setPayeeAccount(order.getPayeeAccount());
        request.setAmount(order.getAmount());
        request.setBankCode(order.getBankCode());

        PaymentResult result;
        try {
            result = bankClient.pay(request);
        } catch (Exception e) {
            result = PaymentResult.fail(order.getPayNo(), "银行调用异常：" + e.getMessage());
        }

        if (result == null || !result.isSuccess()) {
            String reason = result == null ? "银行无响应" : result.getMessage();
            final String failReason = reason;
            tx.execute(status -> {
                orderMapper.update(null, new UpdateWrapper<StPaymentOrder>()
                        .set("status", StPaymentOrder.STATUS_FAILED)
                        .set("fail_reason", truncate(failReason))
                        .set("callback_time", LocalDateTime.now())
                        .set("update_time", LocalDateTime.now())
                        .set("retry_count", (order.getRetryCount() == null ? 0 : order.getRetryCount()) + 1)
                        .eq("id", order.getId()));
                return null;
            });
            throw new BizException(ErrorCode.INTEGRATION_FAILED, "支付失败：" + reason);
        }

        // ---- 事务B：置成功 + 入账（同一事务）----
        final PaymentResult ok = result;
        tx.execute(status -> {
            PaymentPostingCommand posting = new PaymentPostingCommand();
            posting.setPayType(order.getPayType());
            posting.setBizId(order.getBizId());
            posting.setBizNo(order.getBizNo());
            posting.setPayNo(order.getPayNo());
            posting.setOrgId(order.getOrgId());
            posting.setPayeeType(order.getPayeeType());
            posting.setPayeeId(order.getPayeeId());
            posting.setPayeeName(order.getPayeeName());
            posting.setAmount(order.getAmount());

            PaymentPostingPort port = postingPortProvider.getIfAvailable();
            if (port == null) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "财务入账组件未装配，支付不能落账（拒绝产生无账资金）");
            }
            PaymentPostingPort.PostingResult posted = port.post(posting);
            if (posted == null || !posted.isSuccess()) {
                // 入账失败必须回滚，宁可支付单停在 PAYING 等补偿，也不能出现「钱出去没账」
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "入账失败：" + (posted == null ? "无返回" : posted.getMessage()));
            }

            orderMapper.update(null, new UpdateWrapper<StPaymentOrder>()
                    .set("status", StPaymentOrder.STATUS_SUCCESS)
                    .set("bank_serial_no", ok.getBankSerialNo())
                    .set("tls_protocol", ok.getTlsProtocol())
                    .set("callback_time", LocalDateTime.now())
                    .set("fail_reason", null)
                    .set("update_time", LocalDateTime.now())
                    .eq("id", order.getId()));
            return null;
        });

        log.info("支付成功 payNo={} amount={} bankSerial={}", order.getPayNo(), order.getAmount(),
                ok.getBankSerialNo());
        return orderMapper.selectById(order.getId());
    }

    @Override
    public StPaymentOrder queryFromBank(String payNo) {
        StPaymentOrder order = requireOrder(payNo);
        PaymentResult result = bankClient.queryPayment(payNo);
        if (result != null && result.isSuccess()
                && !StPaymentOrder.STATUS_SUCCESS.equals(order.getStatus())) {
            log.warn("银行侧已成功但本地未成功，需要补偿 payNo={} local={} bank={}",
                    payNo, order.getStatus(), result.getStatus());
        }
        return order;
    }

    @Override
    public StPaymentOrder byPayNo(String payNo) {
        return requireOrder(payNo);
    }

    @Override
    public List<StPaymentOrder> list(Long orgId, String status, String payType, Integer limit) {
        QueryWrapper<StPaymentOrder> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        if (payType != null && !payType.trim().isEmpty()) {
            qw.eq("pay_type", payType.trim().toUpperCase());
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return orderMapper.selectList(qw);
    }

    @Override
    public StPaymentOrder close(String payNo, String reason, String operator) {
        StPaymentOrder order = requireOrder(payNo);
        if (StPaymentOrder.STATUS_SUCCESS.equals(order.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "已成功的支付单不能关闭");
        }
        if (StPaymentOrder.STATUS_CLOSED.equals(order.getStatus())) {
            return order;
        }
        orderMapper.update(null, new UpdateWrapper<StPaymentOrder>()
                .set("status", StPaymentOrder.STATUS_CLOSED)
                .set("fail_reason", truncate(reason))
                .set("update_time", LocalDateTime.now())
                .eq("id", order.getId()));
        return orderMapper.selectById(order.getId());
    }

    @Override
    public List<StPaymentOrder> succeededOn(LocalDate bizDate) {
        if (bizDate == null) {
            return Collections.emptyList();
        }
        QueryWrapper<StPaymentOrder> qw = new QueryWrapper<>();
        qw.eq("status", StPaymentOrder.STATUS_SUCCESS)
                .ge("callback_time", bizDate.atStartOfDay())
                .lt("callback_time", bizDate.plusDays(1).atStartOfDay())
                .orderByAsc("id");
        return orderMapper.selectList(qw);
    }

    @Override
    public String bankReconciliationFile(String bizDate) {
        return bankClient.fetchReconciliationFile(bizDate);
    }

    // ------------------------------------------------------------------ 内部

    private void validate(CreatePaymentCommand cmd) {
        if (cmd == null || cmd.getAmount() == null || cmd.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "支付金额必须大于 0");
        }
        if (cmd.getPayType() == null || cmd.getBizId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "支付类型与业务单不能为空");
        }
        if (cmd.getOrgId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "机构不能为空");
        }
    }

    private StPaymentOrder findOpen(String payType, Long bizId) {
        QueryWrapper<StPaymentOrder> qw = new QueryWrapper<>();
        qw.eq("pay_type", payType).eq("biz_id", bizId)
                .in("status", StPaymentOrder.STATUS_PENDING, StPaymentOrder.STATUS_PAYING,
                        StPaymentOrder.STATUS_SUCCESS, StPaymentOrder.STATUS_FAILED)
                .orderByDesc("id").last("LIMIT 1");
        return orderMapper.selectOne(qw);
    }

    private StPaymentOrder requireOrder(String payNo) {
        StPaymentOrder order = orderMapper.selectOne(
                new QueryWrapper<StPaymentOrder>().eq("pay_no", payNo));
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "支付单不存在：" + payNo);
        }
        return order;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}
