package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.track.dto.AlarmHandleRequest;
import com.lukachangtong.freight.track.dto.TrackBatchRequest;
import com.lukachangtong.freight.track.dto.TrackPointDto;
import com.lukachangtong.freight.track.entity.TrTrackAlarm;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import com.lukachangtong.freight.track.service.AlarmService;
import com.lukachangtong.freight.track.service.TrackMaintenanceService;
import com.lukachangtong.freight.track.service.TrackService;
import com.lukachangtong.freight.track.vo.ArchiveReportVo;
import com.lukachangtong.freight.track.vo.PartitionEnsureVo;
import com.lukachangtong.freight.track.vo.PartitionVo;
import com.lukachangtong.freight.track.vo.TrackSummaryVo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 轨迹：上报 / 查询 / 摘要 / 预警 / 分区运维。
 *
 * 上报后自动跑一次近 24h 的预警分析（失败不影响入库），
 * 也可以用 /analyze 对任意时间窗人工补算——历史运单的预警不需要重放全量上报。
 */
@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    @Resource
    private TrackService trackService;
    @Resource
    private AlarmService alarmService;
    @Resource
    private TrackMaintenanceService maintenanceService;

    @PostMapping("/batch")
    public Result<Integer> report(@RequestBody TrackBatchRequest request) {
        int inserted = trackService.report(request.getPoints());
        if (inserted > 0) {
            List<Long> waybillIds = new ArrayList<>();
            for (TrackPointDto p : request.getPoints()) {
                if (p.getWaybillId() != null && !waybillIds.contains(p.getWaybillId())) {
                    waybillIds.add(p.getWaybillId());
                }
            }
            alarmService.analyzeQuietly(waybillIds);
        }
        return Result.ok(inserted);
    }

    @GetMapping
    public Result<List<TrTrackPoint>> query(
            @RequestParam Long waybillId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer maxPoints) {
        return Result.ok(trackService.query(waybillId, from, to, maxPoints));
    }

    @GetMapping("/summary")
    public Result<TrackSummaryVo> summary(
            @RequestParam Long waybillId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.ok(trackService.summary(waybillId, from, to));
    }

    /** 人工补算：传时间窗可对历史运单回溯生成预警（幂等，不会重复插入）。 */
    @PostMapping("/analyze")
    public Result<Integer> analyze(
            @RequestParam Long waybillId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.ok(alarmService.analyze(waybillId, from, to));
    }

    @GetMapping("/alarms")
    public Result<List<TrTrackAlarm>> alarms(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long waybillId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(alarmService.list(orgId, waybillId, status, alarmType, limit));
    }

    @PostMapping("/alarms/{id}/handle")
    public Result<TrTrackAlarm> handle(@PathVariable Long id, @RequestBody AlarmHandleRequest request) {
        return Result.ok(alarmService.handle(id, request.getHandleBy(), request.getHandleRemark(), request.getStatus()));
    }

    // ------------------------------------------------------------ 分区运维（管理员）

    @GetMapping("/maintenance/partitions")
    public Result<List<PartitionVo>> partitions() {
        return Result.ok(maintenanceService.partitions());
    }

    /** 预建分区：不传 month 则预建下个月（定时任务同款逻辑，幂等）。 */
    @PostMapping("/maintenance/partition")
    public Result<PartitionEnsureVo> ensurePartition(
            @RequestParam(required = false) String month) {
        return Result.ok(month == null || month.trim().isEmpty()
                ? maintenanceService.ensureNextMonth()
                : maintenanceService.ensure(YearMonth.parse(month.trim())));
    }

    /** 冷数据归档：默认 dryRun=true，必须显式传 false 才真正导出并删分区。 */
    @PostMapping("/maintenance/archive")
    public Result<ArchiveReportVo> archive(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        return Result.ok(maintenanceService.archiveBefore(before, dryRun));
    }
}
