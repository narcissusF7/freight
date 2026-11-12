package com.lukachangtong.freight.settlement.vo;

import com.lukachangtong.freight.settlement.entity.StReconDiff;
import com.lukachangtong.freight.settlement.entity.StReconciliation;

import java.util.ArrayList;
import java.util.List;

/** 对账运行结果：汇总 + 差异明细。前端「对账中心」一屏展示。 */
public class ReconResultVo {

    private StReconciliation reconciliation;
    private List<StReconDiff> diffs = new ArrayList<StReconDiff>();

    public ReconResultVo() {
    }

    public ReconResultVo(StReconciliation reconciliation, List<StReconDiff> diffs) {
        this.reconciliation = reconciliation;
        this.diffs = diffs;
    }

    public StReconciliation getReconciliation() { return reconciliation; }
    public void setReconciliation(StReconciliation reconciliation) { this.reconciliation = reconciliation; }
    public List<StReconDiff> getDiffs() { return diffs; }
    public void setDiffs(List<StReconDiff> diffs) { this.diffs = diffs; }
}
