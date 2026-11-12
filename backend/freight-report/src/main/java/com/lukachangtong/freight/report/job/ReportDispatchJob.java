package com.lukachangtong.freight.report.job;

import com.lukachangtong.freight.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 上报调度器：扫描待上报数据 + 补偿重试。
 *
 * 默认关闭（`freight.report.job.enabled=false`）：单测加载上下文时不能让后台线程抢跑；
 * 生产用 XXL-Job 或打开开关，接口 `/api/reports/scan` 也可手动触发（便于演示与排障）。
 *
 * 上报是「业务完成后必须报」的合规动作，扫描间隔不宜过长（默认 30s），
 * 保证运单签收后能及时上报（规范要求实时上传）。
 */
@Component
@ConditionalOnProperty(name = "freight.report.job.enabled", havingValue = "true")
public class ReportDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(ReportDispatchJob.class);

    @Resource
    private ReportService reportService;

    @Scheduled(fixedDelayString = "${freight.report.job.scan-interval-ms:30000}")
    public void scan() {
        try {
            int n = reportService.scanAndReport(100);
            if (n > 0) {
                log.info("上报扫描完成：处理 {} 条", n);
            }
        } catch (Exception e) {
            log.error("上报扫描任务异常（下轮重试）", e);
        }
    }

    @Scheduled(fixedDelayString = "${freight.report.job.retry-interval-ms:60000}")
    public void retry() {
        try {
            int n = reportService.retryFailed(50);
            if (n > 0) {
                log.info("上报补偿完成：重试 {} 条", n);
            }
        } catch (Exception e) {
            log.error("上报补偿任务异常（下轮重试）", e);
        }
    }
}
