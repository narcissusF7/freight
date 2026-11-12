package com.lukachangtong.freight.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.entity.StWallet;
import com.lukachangtong.freight.settlement.mapper.StFundFlowMapper;
import com.lukachangtong.freight.settlement.mapper.StWalletMapper;
import com.lukachangtong.freight.settlement.service.WalletService;
import com.lukachangtong.freight.settlement.support.FinNo;
import com.lukachangtong.freight.settlement.support.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 钱包实现。
 *
 * 记账三件套必须同事务：**改余额 → 写流水 → （必要时）改业务单状态**。
 * 余额用乐观锁（version）更新，冲突时重试 3 次——资金路径上宁可失败也不能丢更新。
 */
@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    private static final int RETRY = 3;

    @Resource
    private StWalletMapper walletMapper;
    @Resource
    private StFundFlowMapper flowMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StWallet ensure(String ownerType, Long ownerId, Long orgId, String ownerName) {
        StWallet wallet = byOwner(ownerType, ownerId);
        if (wallet != null) {
            return wallet;
        }
        StWallet created = new StWallet();
        created.setOrgId(orgId);
        created.setOwnerType(ownerType);
        created.setOwnerId(ownerId);
        created.setOwnerName(ownerName);
        created.setBalance(Money.ZERO);
        created.setFrozen(Money.ZERO);
        created.setTotalIn(Money.ZERO);
        created.setTotalOut(Money.ZERO);
        created.setStatus(StWallet.STATUS_ACTIVE);
        created.setCreateTime(LocalDateTime.now());
        created.setUpdateTime(LocalDateTime.now());
        created.setVersion(0);
        try {
            walletMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发开户：唯一索引兜底，重查即可
            return byOwner(ownerType, ownerId);
        }
    }

    @Override
    public StWallet byOwner(String ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return null;
        }
        return walletMapper.selectOne(new QueryWrapper<StWallet>()
                .eq("owner_type", ownerType).eq("owner_id", ownerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StFundFlow credit(String ownerType, Long ownerId, Long orgId, String ownerName,
                             String bizType, String bizNo, BigDecimal amount, String remark) {
        if (!Money.positive(amount)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "入账金额必须大于 0");
        }
        StFundFlow exists = findFlow(bizType, bizNo);
        if (exists != null) {
            // 幂等命中：重复回调不再加钱
            log.info("资金流水已存在，跳过入账 bizType={} bizNo={}", bizType, bizNo);
            return exists;
        }
        StWallet wallet = ensure(ownerType, ownerId, orgId, ownerName);
        BigDecimal amt = Money.scale(amount);

        for (int i = 0; i < RETRY; i++) {
            StWallet current = walletMapper.selectById(wallet.getId());
            BigDecimal before = Money.nz(current.getBalance());
            BigDecimal after = Money.scale(before.add(amt));
            int rows = walletMapper.update(null, new UpdateWrapper<StWallet>()
                    .set("balance", after)
                    .set("total_in", Money.scale(Money.nz(current.getTotalIn()).add(amt)))
                    .set("update_time", LocalDateTime.now())
                    .set("version", (current.getVersion() == null ? 0 : current.getVersion()) + 1)
                    .eq("id", current.getId())
                    .eq("version", current.getVersion()));
            if (rows > 0) {
                return insertFlow(wallet, bizType, bizNo, StFundFlow.DIR_IN, amt, before, after, remark);
            }
            // 版本冲突：说明有并发记账，重读重试
        }
        throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "账户记账冲突，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StFundFlow debit(String ownerType, Long ownerId, String bizType, String bizNo,
                            BigDecimal amount, String remark) {
        if (!Money.positive(amount)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "出账金额必须大于 0");
        }
        StFundFlow exists = findFlow(bizType, bizNo);
        if (exists != null) {
            return exists;
        }
        StWallet wallet = byOwner(ownerType, ownerId);
        if (wallet == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账户不存在");
        }
        BigDecimal amt = Money.scale(amount);

        for (int i = 0; i < RETRY; i++) {
            StWallet current = walletMapper.selectById(wallet.getId());
            BigDecimal before = Money.nz(current.getBalance());
            BigDecimal available = before.subtract(Money.nz(current.getFrozen()));
            if (available.compareTo(amt) < 0) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "可用余额不足：可用 " + available + " 元，需 " + amt + " 元");
            }
            BigDecimal after = Money.scale(before.subtract(amt));
            int rows = walletMapper.update(null, new UpdateWrapper<StWallet>()
                    .set("balance", after)
                    .set("total_out", Money.scale(Money.nz(current.getTotalOut()).add(amt)))
                    .set("update_time", LocalDateTime.now())
                    .set("version", (current.getVersion() == null ? 0 : current.getVersion()) + 1)
                    .eq("id", current.getId())
                    .eq("version", current.getVersion()));
            if (rows > 0) {
                return insertFlow(current, bizType, bizNo, StFundFlow.DIR_OUT, amt, before, after, remark);
            }
        }
        throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "账户记账冲突，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freeze(Long walletId, BigDecimal amount) {
        StWallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账户不存在");
        }
        BigDecimal available = Money.nz(wallet.getBalance()).subtract(Money.nz(wallet.getFrozen()));
        if (available.compareTo(Money.scale(amount)) < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "可用余额不足，无法提现 " + amount + " 元");
        }
        walletMapper.update(null, new UpdateWrapper<StWallet>()
                .set("frozen", Money.scale(Money.nz(wallet.getFrozen()).add(amount)))
                .set("update_time", LocalDateTime.now())
                .set("version", (wallet.getVersion() == null ? 0 : wallet.getVersion()) + 1)
                .eq("id", walletId)
                .eq("version", wallet.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(Long walletId, BigDecimal amount) {
        StWallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            return;
        }
        BigDecimal next = Money.nz(wallet.getFrozen()).subtract(Money.scale(amount));
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            next = BigDecimal.ZERO;
        }
        walletMapper.update(null, new UpdateWrapper<StWallet>()
                .set("frozen", next)
                .set("update_time", LocalDateTime.now())
                .set("version", (wallet.getVersion() == null ? 0 : wallet.getVersion()) + 1)
                .eq("id", walletId)
                .eq("version", wallet.getVersion()));
    }

    @Override
    public List<StFundFlow> flows(String ownerType, Long ownerId, Integer limit) {
        StWallet wallet = byOwner(ownerType, ownerId);
        if (wallet == null) {
            return Collections.emptyList();
        }
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        return flowMapper.selectList(new QueryWrapper<StFundFlow>()
                .eq("wallet_id", wallet.getId()).orderByDesc("id").last("LIMIT " + cap));
    }

    @Override
    public List<StWallet> list(Long orgId) {
        QueryWrapper<StWallet> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("balance");
        return walletMapper.selectList(qw);
    }

    @Override
    public List<StFundFlow> flowsOn(LocalDate bizDate, String direction, String bizType) {
        if (bizDate == null) {
            return Collections.emptyList();
        }
        QueryWrapper<StFundFlow> qw = new QueryWrapper<>();
        qw.ge("create_time", bizDate.atStartOfDay())
                .lt("create_time", bizDate.plusDays(1).atStartOfDay())
                .orderByAsc("id");
        if (direction != null) {
            qw.eq("direction", direction);
        }
        if (bizType != null) {
            qw.eq("biz_type", bizType);
        }
        return flowMapper.selectList(qw);
    }

    // ------------------------------------------------------------------ 内部

    private StFundFlow findFlow(String bizType, String bizNo) {
        return flowMapper.selectOne(new QueryWrapper<StFundFlow>()
                .eq("biz_type", bizType).eq("biz_no", bizNo));
    }

    private StFundFlow insertFlow(StWallet wallet, String bizType, String bizNo, String direction,
                                  BigDecimal amount, BigDecimal before, BigDecimal after, String remark) {
        StFundFlow flow = new StFundFlow();
        flow.setFlowNo(FinNo.flow());
        flow.setOrgId(wallet.getOrgId());
        flow.setWalletId(wallet.getId());
        flow.setOwnerType(wallet.getOwnerType());
        flow.setOwnerId(wallet.getOwnerId());
        flow.setBizType(bizType);
        flow.setBizNo(bizNo);
        flow.setDirection(direction);
        flow.setAmount(amount);
        flow.setBalanceBefore(Money.scale(before));
        flow.setBalanceAfter(Money.scale(after));
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        flowMapper.insert(flow);
        return flow;
    }
}
