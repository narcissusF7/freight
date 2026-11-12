package com.lukachangtong.freight.payment.service;

import com.lukachangtong.freight.payment.dto.CreatePaymentCommand;
import com.lukachangtong.freight.payment.entity.StPaymentOrder;

import java.time.LocalDate;
import java.util.List;

/**
 * 银企直联支付服务。
 *
 * 职责边界：**只管「钱有没有出去」**，不碰结算单与钱包。
 * 入账由结算域通过 {@code PaymentPostingPort} 在支付成功后的同一事务内完成——
 * 这样支付域不依赖财务域，两边都不担心循环依赖。
 */
public interface PaymentService {

    /**
     * 创建支付单（幂等）：同一 `(payType, bizId)` 已有未关闭的单子时直接返回原单，
     * 避免重复点击生成两笔付款。
     */
    StPaymentOrder createOrGet(CreatePaymentCommand command);

    /**
     * 执行支付：置 PAYING（独立事务提交）→ 调银行（事务外）→ 置 SUCCESS/FAILED 并入账（新事务）。
     *
     * 分三段事务的原因：银行调用不能包在事务里，否则「钱付了、事务回滚」无法自洽；
     * 先落 PAYING 也保证进程崩溃后还能查到「有笔支付在途」并由人工/定时补偿。
     */
    StPaymentOrder execute(String payNo);

    /** 向银行反查（排障与对账用），不修改本地状态。 */
    StPaymentOrder queryFromBank(String payNo);

    StPaymentOrder byPayNo(String payNo);

    List<StPaymentOrder> list(Long orgId, String status, String payType, Integer limit);

    /** 人工终止：仅 PENDING / FAILED 可关闭。 */
    StPaymentOrder close(String payNo, String reason, String operator);

    /** 某业务日成功的支付单（对账取数）。 */
    List<StPaymentOrder> succeededOn(LocalDate bizDate);

    /** 银行下发的对账文件内容（透传，供四方对账比对）。 */
    String bankReconciliationFile(String bizDate);
}
