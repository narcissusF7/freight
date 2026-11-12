package com.lukachangtong.freight.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.settlement.entity.StFundFlow;
import com.lukachangtong.freight.settlement.vo.WalletNetVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StFundFlowMapper extends BaseMapper<StFundFlow> {

    /**
     * 全量流水净额（按钱包聚合）。对账用它校验「钱包余额 = 流水净额」——
     * 这是资金系统最硬的一条自证，任何绕过流水直接改余额的代码都会在这里暴露。
     */
    @Select("SELECT wallet_id AS walletId, "
            + "SUM(CASE WHEN direction = 'IN' THEN amount ELSE -amount END) AS net "
            + "FROM st_fund_flow GROUP BY wallet_id")
    List<WalletNetVo> sumNetByWallet();
}
