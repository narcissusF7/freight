package com.lukachangtong.freight.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.FileUploadResult;
import com.lukachangtong.freight.track.entity.TrArchiveLog;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import com.lukachangtong.freight.track.mapper.TrArchiveLogMapper;
import com.lukachangtong.freight.track.mapper.TrTrackPointMapper;
import com.lukachangtong.freight.track.mapper.TrackMaintenanceMapper;
import com.lukachangtong.freight.track.service.TrackMaintenanceService;
import com.lukachangtong.freight.track.vo.ArchiveReportVo;
import com.lukachangtong.freight.track.vo.PartitionEnsureVo;
import com.lukachangtong.freight.track.vo.PartitionVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 分区运维实现。
 *
 * 安全点：DDL 里分区名无法用占位符绑定，因此**所有分区名都来自代码生成的 pYYYYMM**
 * （正则校验）或 information_schema 的返回值，绝不接受外部原始字符串直拼。
 */
@Service
public class TrackMaintenanceServiceImpl implements TrackMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(TrackMaintenanceServiceImpl.class);

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String PMAX = "pmax";
    private static final int EXPORT_PAGE_LIMIT = 500000;

    @Resource
    private TrackMaintenanceMapper maintenanceMapper;
    @Resource
    private TrTrackPointMapper trackPointMapper;
    @Resource
    private TrArchiveLogMapper archiveLogMapper;
    @Resource
    private FileClient fileClient;

    @Override
    public List<PartitionVo> partitions() {
        return maintenanceMapper.listPartitions();
    }

    @Override
    public PartitionEnsureVo ensure(YearMonth month) {
        if (month == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "month 不能为空");
        }
        String name = "p" + month.format(YM);
        if (!name.matches("^p\\d{6}$")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法分区名：" + name);
        }
        LocalDate boundaryDate = month.plusMonths(1).atDay(1);
        String boundary = boundaryDate.toString();

        PartitionEnsureVo vo = new PartitionEnsureVo();
        vo.setRequestedMonth(month.toString());
        vo.setPartitionName(name);
        vo.setBoundary(boundary);

        List<PartitionVo> existing = maintenanceMapper.listPartitions();
        boolean hasPmax = false;
        LocalDate maxBoundary = null;
        for (PartitionVo p : existing) {
            if (name.equalsIgnoreCase(p.getPartitionName())) {
                vo.setAction("EXISTS");
                vo.setMessage("分区已存在，跳过（幂等）");
                return vo;
            }
            if (PMAX.equalsIgnoreCase(p.getPartitionName())) {
                hasPmax = true;
            } else {
                LocalDate d = parseBoundary(p.getPartitionDescription());
                if (d != null && (maxBoundary == null || d.isAfter(maxBoundary))) {
                    maxBoundary = d;
                }
            }
        }
        if (!hasPmax) {
            throw new BizException(ErrorCode.PARAM_INVALID, "tr_track_point 不是分区表或缺少 pmax 兜底分区");
        }
        // MySQL 要求分区边界递增：要建的月份必须晚于现有最大分区边界，否则 REORGANIZE 会报错
        if (maxBoundary != null && !boundaryDate.isAfter(maxBoundary)) {
            vo.setAction("SKIPPED");
            vo.setMessage("目标边界 " + boundary + " 不晚于现有最大边界 " + maxBoundary + "，无需预建");
            return vo;
        }
        maintenanceMapper.reorganizePmax(name, boundary);
        vo.setAction("CREATED");
        vo.setMessage("已从 pmax 拆分出 " + name + "（VALUES LESS THAN '" + boundary + "'）");
        log.info("轨迹分区预建完成：{} < {}", name, boundary);
        return vo;
    }

    @Override
    public PartitionEnsureVo ensureNextMonth() {
        return ensure(YearMonth.now().plusMonths(1));
    }

    @Override
    public ArchiveReportVo archiveBefore(LocalDate cutoff, boolean dryRun) {
        if (cutoff == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "cutoff 不能为空");
        }
        ArchiveReportVo report = new ArchiveReportVo();
        report.setDryRun(dryRun);
        report.setCutoff(cutoff.toString());

        List<PartitionVo> partitions = maintenanceMapper.listPartitions();
        long total = 0;
        for (PartitionVo p : partitions) {
            if (PMAX.equalsIgnoreCase(p.getPartitionName())) {
                continue;
            }
            // RANGE COLUMNS 的 PARTITION_DESCRIPTION 带单引号（如 '2026-12-01'），必须先剥掉再解析
            LocalDate upper = parseBoundary(p.getPartitionDescription());
            if (upper == null || upper.isAfter(cutoff)) {
                continue; // 解析不了或还没整体过期，一律不动
            }
            LocalDate periodStart = upper.minusMonths(1);
            LocalDate periodEnd = upper.minusDays(1);
            long rows = maintenanceMapper.countInPartition(p.getPartitionName());
            total += rows;

            ArchiveReportVo.Item item = new ArchiveReportVo.Item();
            item.setPartitionName(p.getPartitionName());
            item.setPeriodStart(periodStart.toString());
            item.setPeriodEnd(periodEnd.toString());
            item.setRowCount(rows);

            if (dryRun) {
                item.setStatus(TrArchiveLog.STATUS_DRY_RUN);
                item.setMessage("dryRun：仅统计，未导出也未删除分区");
                report.getItems().add(item);
                continue;
            }
            try {
                String fileKey = exportAndUpload(p.getPartitionName(), periodStart, upper);
                item.setFileKey(fileKey);
                // 导出成功后才删分区：宁可多留一个月，也不能归档失败就丢数据
                maintenanceMapper.dropPartition(p.getPartitionName());
                item.setStatus(TrArchiveLog.STATUS_PURGED);
                item.setMessage("已归档并删除分区");
            } catch (Exception e) {
                item.setStatus(TrArchiveLog.STATUS_FAILED);
                item.setMessage("归档失败，分区保留：" + e.getMessage());
                log.error("轨迹归档失败 partition={}", p.getPartitionName(), e);
            }
            report.getItems().add(item);
            writeLog(item);
        }
        report.setTotalRows(total);
        return report;
    }

    /** 导出单分区数据为 CSV.gz 并上传对象存储，返回对象 key（不落公网直链）。 */
    private String exportAndUpload(String partition, LocalDate from, LocalDate toExclusive) throws IOException {
        QueryWrapper<TrTrackPoint> qw = new QueryWrapper<>();
        qw.ge("gps_time", from.atStartOfDay())
                .lt("gps_time", toExclusive.atStartOfDay())
                .orderByAsc("gps_time")
                .last("LIMIT " + EXPORT_PAGE_LIMIT);
        List<TrTrackPoint> rows = trackPointMapper.selectList(qw);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(bos, 8192), StandardCharsets.UTF_8)) {
            w.write("id,waybill_id,driver_id,vehicle_id,lat,lng,speed,direction,accuracy,gps_time,source\n");
            for (TrTrackPoint t : rows) {
                w.write(String.valueOf(t.getId()));
                w.write(',');
                w.write(String.valueOf(t.getWaybillId()));
                w.write(',');
                w.write(t.getDriverId() == null ? "" : String.valueOf(t.getDriverId()));
                w.write(',');
                w.write(t.getVehicleId() == null ? "" : String.valueOf(t.getVehicleId()));
                w.write(',');
                w.write(String.valueOf(t.getLat()));
                w.write(',');
                w.write(String.valueOf(t.getLng()));
                w.write(',');
                w.write(t.getSpeed() == null ? "" : String.valueOf(t.getSpeed()));
                w.write(',');
                w.write(t.getDirection() == null ? "" : String.valueOf(t.getDirection()));
                w.write(',');
                w.write(t.getAccuracy() == null ? "" : String.valueOf(t.getAccuracy()));
                w.write(',');
                w.write(String.valueOf(t.getGpsTime()));
                w.write(',');
                w.write(t.getSource() == null ? "" : t.getSource());
                w.write('\n');
            }
        } catch (IOException e) {
            throw new IOException("CSV 生成失败：" + e.getMessage(), e);
        }
        byte[] payload = bos.toByteArray();
        String fileName = "tr_track_point_" + partition + ".csv.gz";
        FileUploadResult result = fileClient.upload(new ByteArrayInputStream(payload), fileName, "application/gzip");
        log.info("轨迹归档上传完成 partition={} rows={} bytes={} key={}",
                partition, rows.size(), payload.length, result.getFileKey());
        return result.getFileKey();
    }

    private void writeLog(ArchiveReportVo.Item item) {
        TrArchiveLog logRow = new TrArchiveLog();
        logRow.setPartitionName(item.getPartitionName());
        logRow.setPeriodStart(item.getPeriodStart() == null ? null : LocalDate.parse(item.getPeriodStart()));
        logRow.setPeriodEnd(item.getPeriodEnd() == null ? null : LocalDate.parse(item.getPeriodEnd()));
        logRow.setRowCount(item.getRowCount());
        logRow.setFileKey(item.getFileKey());
        logRow.setStatus(item.getStatus());
        logRow.setMessage(item.getMessage() == null ? null
                : (item.getMessage().length() > 255 ? item.getMessage().substring(0, 255) : item.getMessage()));
        logRow.setCreateTime(LocalDateTime.now());
        archiveLogMapper.insert(logRow);
    }

    /** 仅供测试/运维排查：把分区名列表化，便于断言。 */
    public List<String> partitionNames() {
        List<String> names = new ArrayList<>();
        for (PartitionVo p : maintenanceMapper.listPartitions()) {
            names.add(p.getPartitionName());
        }
        return names;
    }

    /** 解析分区上界：RANGE COLUMNS 返回带引号的字符串（'2027-01-01'），pmax 返回 MAXVALUE。 */
    private static LocalDate parseBoundary(String description) {
        if (description == null) {
            return null;
        }
        String s = description.trim().replace("'", "").replace("\"", "");
        if (s.isEmpty() || "MAXVALUE".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
