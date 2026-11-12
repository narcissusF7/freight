package com.lukachangtong.freight.settlement.service;

import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.entity.StWallet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 钱包与记账。
 *
 * 这是整个财务域唯一允许改 `st_wallet.balance` 的地方：
 * 别处直接 update 余额，就等于绕过了流水，日终对账必然报差异，且无法追溯是谁改的。
 */
public interface WalletService {

    /** 取或建账户（同一 owner 唯一），并发建同一账户由唯一索引兜底后重查。 */
    StWallet ensure(String ownerType, Long ownerId, Long orgId, String ownerName);

    StWallet byOwner(String ownerType, Long ownerId);

    /**
     * 入账：余额增加 + 写 IN 流水，同一事务。
     * 幂等：`(bizType, bizNo)` 已入账则直接返回原流水。
     */
    StFundFlow credit(String ownerType, Long ownerId, Long orgId, String ownerName,
                      String bizType, String bizNo, BigDecimal amount, String remark);

    /**
     * 出账：余额减少 + 写 OUT 流水，同一事务。
     * 可用余额（balance - frozen）不足直接抛业务异常。
     */
    StFundFlow debit(String ownerType, Long ownerId, String bizType, String bizNo,
                     BigDecimal amount, String remark);

    /** 冻结（提现申请时）：只动 frozen，不动 balance。 */
    void freeze(Long walletId, BigDecimal amount);

    void unfreeze(Long walletId, BigDecimal amount);

    List<StFundFlow> flows(String ownerType, Long ownerId, Integer limit);

    List<StWallet> list(Long orgId);

    /** 对账取数：某方向某类型某日的流水。 */
    List<StFundFlow> flowsOn(LocalDate bizDate, String direction, String bizType);
}
