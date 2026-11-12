package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.settlement.entity.StReconDiff;
import com.lukachangtong.freight.settlement.entity.StReconciliation;
import com.lukachangtong.freight.settlement.service.ReconciliationService;
import com.lukachangtong.freight.settlement.vo.ReconResultVo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 四方对账。`run` 可重复执行（同日覆盖），前端「对账中心」先跑再展示差异清单。
 */
@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    @Resource
    private ReconciliationService reconciliationService;

    @PostMapping("/run")
    public Result<ReconResultVo> run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate,
            @RequestParam(required = false) String operator) {
        return Result.ok(reconciliationService.run(bizDate, operator));
    }

    @GetMapping
    public Result<StReconciliation> latest(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        return Result.ok(reconciliationService.latest(bizDate));
    }

    @GetMapping("/{reconId}/diffs")
    public Result<List<StReconDiff>> diffs(@PathVariable Long reconId) {
        return Result.ok(reconciliationService.diffs(reconId));
    }

    @PostMapping("/diffs/{diffId}/handle")
    public Result<Integer> handle(@PathVariable Long diffId,
                                  @RequestParam(required = false) String handleBy,
                                  @RequestParam(required = false) String remark) {
        return Result.ok(reconciliationService.handleDiff(diffId, handleBy, remark));
    }
}
