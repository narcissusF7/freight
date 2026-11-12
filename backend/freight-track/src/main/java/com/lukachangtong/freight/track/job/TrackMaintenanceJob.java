package com.lukachangtong.freight.track.job;

import com.lukachangtong.freight.track.service.TrackMaintenanceService;
import com.lukachangtong.freight.track.vo.ArchiveReportVo;
import com.lukachangtong.freight.track.vo.PartitionEnsureVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 轨迹分区运维定时任务。
 *
 * 默认关闭（`freight.track.job.enabled=false`），原因：
 * 1. 单元测试会启动应用上下文，不能真去改表结构；
 * 2. 生产应由统一的调度中心（XXL-Job）触发，本地 @Scheduled 只是没有调度中心时的兜底。
 *
 * 打开方式：`freight.track.job.enabled=true`（见 application-prod.yml）。
 */
@Component
@ConditionalOnProperty(name = "freight.track.job.enabled", havingValue = "true")
public class TrackMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(TrackMaintenanceJob.class);

    @Resource
    private TrackMaintenanceService maintenanceService;

    /** 每月 25 日 02:30 预建下月分区：留 5 天缓冲，避免月初写入落到 pmax 后无法再拆。 */
    @Scheduled(cron = "${freight.track.job.partition-cron:0 30 2 25 * ?}")
    public void prepareNextMonthPartition() {
        try {
            PartitionEnsureVo vo = maintenanceService.ensureNextMonth();
            log.info("定时预建轨迹分区：{} {} {}", vo.getPartitionName(), vo.getAction(), vo.getMessage());
        } catch (Exception e) {
            // 分区预建失败会导致下月数据堆进 pmax（后续无法直接拆分），必须显式告警
            log.error("定时预建轨迹分区失败，需人工介入", e);
        }
    }

    /** 每天 03:30 归档 6 个月前的分区（先导出 OBS，成功后才 DROP）。 */
    @Scheduled(cron = "${freight.track.job.archive-cron:0 30 3 * * ?}")
    public void archiveColdPartitions() {
        try {
            ArchiveReportVo report = maintenanceService.archiveBefore(LocalDate.now().minusMonths(6), false);
            if (report.getItems().isEmpty()) {
                log.info("轨迹归档检查完成：无到期分区");
            } else {
                log.info("轨迹归档完成：{} 个分区，共 {} 行", report.getItems().size(), report.getTotalRows());
            }
        } catch (Exception e) {
            log.error("轨迹归档任务失败，分区已保留（不会丢数据）", e);
        }
    }
}
