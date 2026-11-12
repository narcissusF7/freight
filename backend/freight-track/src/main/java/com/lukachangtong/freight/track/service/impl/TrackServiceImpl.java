package com.lukachangtong.freight.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.track.dto.TrackPointDto;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import com.lukachangtong.freight.track.mapper.TrTrackPointMapper;
import com.lukachangtong.freight.track.service.TrackService;
import com.lukachangtong.freight.track.vo.TrackSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TrackServiceImpl implements TrackService {

    /** 定位精度差于此值（米）视为漂移，直接丢弃，不参与里程 */
    private static final double MAX_ACCURACY_M = 100D;
    /** 相邻点推算速度超过此值（km/h）视为跳点，不参与里程 */
    private static final double MAX_PLAUSIBLE_SPEED = 200D;
    private static final int DEFAULT_MAX_POINTS = 2000;
    private static final int SUMMARY_MAX_POINTS = 20000;

    @Resource
    private TrTrackPointMapper trackPointMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int report(List<TrackPointDto> points) {
        if (points == null || points.isEmpty()) {
            return 0;
        }
        // 1) 校验 + 剔除漂移点
        List<TrackPointDto> valid = new ArrayList<>();
        for (TrackPointDto p : points) {
            if (p == null || p.getWaybillId() == null || p.getLat() == null
                    || p.getLng() == null || p.getGpsTime() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "轨迹点缺少必填字段（waybillId/lat/lng/gpsTime）");
            }
            if (p.getAccuracy() != null && p.getAccuracy().doubleValue() > MAX_ACCURACY_M) {
                continue;
            }
            valid.add(p);
        }
        if (valid.isEmpty()) {
            return 0;
        }

        // 2) 按运单取已存在的时间点，做 (waybillId, gpsTime) 幂等去重
        Map<Long, Set<String>> existing = new HashMap<>();
        for (TrackPointDto p : valid) {
            existing.computeIfAbsent(p.getWaybillId(), k -> new HashSet<>());
        }
        for (Map.Entry<Long, Set<String>> e : existing.entrySet()) {
            LocalDateTime min = null;
            LocalDateTime max = null;
            for (TrackPointDto p : valid) {
                if (!e.getKey().equals(p.getWaybillId())) {
                    continue;
                }
                if (min == null || p.getGpsTime().isBefore(min)) {
                    min = p.getGpsTime();
                }
                if (max == null || p.getGpsTime().isAfter(max)) {
                    max = p.getGpsTime();
                }
            }
            QueryWrapper<TrTrackPoint> qw = new QueryWrapper<>();
            qw.select("gps_time").eq("waybill_id", e.getKey())
                    .ge("gps_time", min).le("gps_time", max);
            for (TrTrackPoint t : trackPointMapper.selectList(qw)) {
                e.getValue().add(t.getGpsTime().toString());
            }
        }

        // 3) 入库（同一批内也去重）
        int inserted = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TrackPointDto p : valid) {
            Set<String> times = existing.get(p.getWaybillId());
            if (!times.add(p.getGpsTime().toString())) {
                continue;
            }
            TrTrackPoint t = new TrTrackPoint();
            t.setWaybillId(p.getWaybillId());
            t.setDriverId(p.getDriverId());
            t.setVehicleId(p.getVehicleId());
            t.setLat(p.getLat());
            t.setLng(p.getLng());
            t.setSpeed(p.getSpeed());
            t.setDirection(p.getDirection());
            t.setAltitude(p.getAltitude());
            t.setAccuracy(p.getAccuracy());
            t.setGpsTime(p.getGpsTime());
            t.setReportTime(now);
            t.setSource(p.getSource() == null || p.getSource().trim().isEmpty() ? "APP" : p.getSource());
            trackPointMapper.insert(t);
            inserted++;
        }
        return inserted;
    }

    @Override
    public List<TrTrackPoint> query(Long waybillId, LocalDateTime from, LocalDateTime to, Integer maxPoints) {
        if (waybillId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "waybillId 不能为空");
        }
        QueryWrapper<TrTrackPoint> qw = new QueryWrapper<>();
        qw.eq("waybill_id", waybillId);
        if (from != null) {
            qw.ge("gps_time", from);
        }
        if (to != null) {
            qw.le("gps_time", to);
        }
        qw.orderByAsc("gps_time");
        List<TrTrackPoint> all = trackPointMapper.selectList(qw);
        return sample(all, maxPoints == null || maxPoints <= 0 ? DEFAULT_MAX_POINTS : maxPoints);
    }

    @Override
    public TrackSummaryVo summary(Long waybillId, LocalDateTime from, LocalDateTime to) {
        List<TrTrackPoint> points = query(waybillId, from, to, SUMMARY_MAX_POINTS);
        TrackSummaryVo vo = new TrackSummaryVo();
        vo.setWaybillId(waybillId);
        vo.setPointCount(points.size());
        if (points.isEmpty()) {
            vo.setDistanceKm(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            vo.setAvgSpeed(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            vo.setMaxSpeed(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            return vo;
        }

        vo.setStartTime(points.get(0).getGpsTime());
        vo.setEndTime(points.get(points.size() - 1).getGpsTime());
        vo.setStartLat(points.get(0).getLat());
        vo.setStartLng(points.get(0).getLng());
        vo.setEndLat(points.get(points.size() - 1).getLat());
        vo.setEndLng(points.get(points.size() - 1).getLng());

        double distanceKm = 0D;
        int drift = 0;
        double maxSpeed = 0D;
        for (int i = 1; i < points.size(); i++) {
            TrTrackPoint prev = points.get(i - 1);
            TrTrackPoint cur = points.get(i);
            double d = haversineKm(prev.getLat().doubleValue(), prev.getLng().doubleValue(),
                    cur.getLat().doubleValue(), cur.getLng().doubleValue());
            long seconds = Duration.between(prev.getGpsTime(), cur.getGpsTime()).getSeconds();
            if (seconds <= 0) {
                continue;
            }
            double impliedSpeed = d / (seconds / 3600D);
            if (impliedSpeed > MAX_PLAUSIBLE_SPEED) {
                drift++;
                continue;
            }
            distanceKm += d;
            if (cur.getSpeed() != null) {
                maxSpeed = Math.max(maxSpeed, cur.getSpeed().doubleValue());
            } else {
                maxSpeed = Math.max(maxSpeed, impliedSpeed);
            }
        }
        long totalSeconds = Duration.between(points.get(0).getGpsTime(),
                points.get(points.size() - 1).getGpsTime()).getSeconds();

        vo.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
        vo.setMaxSpeed(BigDecimal.valueOf(maxSpeed).setScale(1, RoundingMode.HALF_UP));
        vo.setAvgSpeed(totalSeconds > 0
                ? BigDecimal.valueOf(distanceKm / (totalSeconds / 3600D)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        vo.setDriftPoints(drift);
        return vo;
    }

    /** 等间隔抽稀：始终保留首尾点，保证回放起终点不丢。 */
    private List<TrTrackPoint> sample(List<TrTrackPoint> all, int maxPoints) {
        if (all.size() <= maxPoints || maxPoints < 2) {
            return all;
        }
        List<TrTrackPoint> out = new ArrayList<>(maxPoints);
        double step = (all.size() - 1D) / (maxPoints - 1D);
        for (int i = 0; i < maxPoints - 1; i++) {
            out.add(all.get((int) Math.round(i * step)));
        }
        out.add(all.get(all.size() - 1));
        return out;
    }

    /** 球面距离（Haversine），返回公里。 */
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final int r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.min(1D, Math.sqrt(a)));
    }
}
