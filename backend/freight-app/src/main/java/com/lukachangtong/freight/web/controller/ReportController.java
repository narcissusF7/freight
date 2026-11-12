package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.report.entity.RpReportRecord;
import com.lukachangtong.freight.report.service.ReportService;
import com.lukachangtong.freight.report.vo.ReportStatVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 监管上报：看板 / 扫描上报 / 手动上报 / 补偿重试。
 *
 * 上报是合规关键路径，接口设计上把「扫描」「重试」都暴露成手动入口——
 * 生产由调度器驱动，手动入口用于演示、排障与联调。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Resource
    private ReportService reportService;

    @GetMapping
    public Result<List<RpReportRecord>> list(@RequestParam(required = false) Long orgId,
                                             @RequestParam(required = false) String docType,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Integer limit) {
        return Result.ok(reportService.list(orgId, docType, status, limit));
    }

    @GetMapping("/stats")
    public Result<ReportStatVo> stats(@RequestParam(required = false) Long orgId) {
        return Result.ok(reportService.stats(orgId));
    }

    /** 手动扫描上报（生产由调度器驱动，这里用于演示与联调）。 */
    @PostMapping("/scan")
    public Result<Integer> scan(@RequestParam(required = false, defaultValue = "100") int limit) {
        return Result.ok(reportService.scanAndReport(limit));
    }

    /** 补偿重试。 */
    @PostMapping("/retry")
    public Result<Integer> retry(@RequestParam(required = false, defaultValue = "50") int limit) {
        return Result.ok(reportService.retryFailed(limit));
    }

    /** 手动上报某条业务数据（运单号/流水号/车牌/身份证）。 */
    @PostMapping("/one")
    public Result<RpReportRecord> reportOne(@RequestParam String docType,
                                            @RequestParam String bizNo,
                                            @RequestParam(required = false) String operator) {
        return Result.ok(reportService.reportOne(docType, bizNo, operator));
    }

    /** 手动重试某条失败记录。 */
    @PostMapping("/records/{id}/retry")
    public Result<RpReportRecord> retryRecord(@PathVariable Long id,
                                              @RequestParam(required = false) String operator) {
        return Result.ok(reportService.retryRecord(id, operator));
    }
}
