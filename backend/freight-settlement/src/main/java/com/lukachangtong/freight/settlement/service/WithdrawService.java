package com.lukachangtong.freight.settlement.service;

import com.lukachangtong.freight.settlement.dto.WithdrawApplyRequest;
import com.lukachangtong.freight.settlement.entity.StWithdraw;

import java.util.List;

/**
 * 提现：申请（冻结）→ 审核（通过则发起银行付款）→ 支付成功（扣减余额 + 出账流水）。
 *
 * 审核通过与发起付款绑在一起，是因为网络货运的司机提现金额小、笔数多，
 * 拆成两步会让人工审核变成瓶颈；驳回时解冻，钱立刻回到可用余额。
 */
public interface WithdrawService {

    StWithdraw apply(WithdrawApplyRequest request);

    /** @param pass true 通过并付款；false 驳回解冻 */
    StWithdraw audit(Long id, boolean pass, String remark, String operator);

    /** 审核通过但付款失败时的重试入口。 */
    StWithdraw retryPay(Long id, String operator);

    List<StWithdraw> list(String ownerType, Long ownerId, String status, Integer limit);

    StWithdraw detail(Long id);
}
