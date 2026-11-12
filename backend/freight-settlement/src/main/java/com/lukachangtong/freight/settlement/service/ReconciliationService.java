package com.lukachangtong.freight.settlement.service;

import com.lukachangtong.freight.settlement.entity.StReconDiff;
import com.lukachangtong.freight.settlement.entity.StReconciliation;
import com.lukachangtong.freight.settlement.vo.ReconResultVo;

import java.time.LocalDate;
import java.util.List;

/**
 * 四方对账：结算单 / 支付单 / 资金流水 / 钱包余额，另与银行对账文件核对单号。
 *
 * 结果按 `(biz_date, scene)` 唯一，重跑覆盖——对账是可重算的快照，不是流水。
 */
public interface ReconciliationService {

    ReconResultVo run(LocalDate bizDate, String operator);

    StReconciliation latest(LocalDate bizDate);

    List<StReconDiff> diffs(Long reconId);

    /** 人工标记差异已处理（留痕，不自动调账）。 */
    int handleDiff(Long diffId, String handleBy, String remark);
}
