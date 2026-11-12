package com.lukachangtong.freight.stat.job;

import com.lukachangtong.freight.stat.service.StatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 统计预聚合任务：每天凌晨补算「昨天」。
 *
 * 默认关闭（`freight.stat.job.enabled=false`）：单测加载上下文时不能有后台线程写库。
 * 生产用 XXL-Job 统一调度，或打开该开关；`POST /api/stat/rebuild` 也可手动补算历史区间。
 *
 * 只算昨天不重算全量：全量重算是运维动作，定时任务每天把所有历史重跑一遍，
 * 数据库会被自己的调度器拖死。
 */
@Component
@ConditionalOnProperty(name = "freight.stat.job.enabled", havingValue = "true")
public class StatAggregateJob {

    private static final Logger log = LoggerFactory.getLogger(StatAggregateJob.class);

    @Resource
    private StatService statService;

    @Scheduled(cron = "${freight.stat.job.cron:0 10 1 * * ?}")
    public void aggregateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            int days = statService.rebuild(yesterday, yesterday, null, "JOB");
            log.info("统计预聚合任务完成：{} 共 {} 天", yesterday, days);
        } catch (Exception e) {
            log.error("统计预聚合任务失败（明日重跑会覆盖）", e);
        }
    }
}
