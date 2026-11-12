package com.lukachangtong.freight.track.service;

import com.lukachangtong.freight.track.vo.ArchiveReportVo;
import com.lukachangtong.freight.track.vo.PartitionEnsureVo;
import com.lukachangtong.freight.track.vo.PartitionVo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 轨迹分区运维：预建下月分区 + 冷数据归档删除。
 *
 * 为什么不写在 Flyway 里：分区是滚动增长的，Flyway 版本化脚本无法表达「每月自动加一个」；
 * 用定时任务 + 幂等 DDL 才不会把迁移历史撑成上百个文件。
 */
public interface TrackMaintenanceService {

    List<PartitionVo> partitions();

    /** 幂等：已存在则返回 EXISTS，不重复执行 DDL。 */
    PartitionEnsureVo ensure(YearMonth month);

    PartitionEnsureVo ensureNextMonth();

    /**
     * 归档并清理早于 cutoff 的整月分区。
     *
     * @param dryRun true 只产出清单与行数，不导出、不删分区（上线前先看影响面）
     */
    ArchiveReportVo archiveBefore(LocalDate cutoff, boolean dryRun);
}
