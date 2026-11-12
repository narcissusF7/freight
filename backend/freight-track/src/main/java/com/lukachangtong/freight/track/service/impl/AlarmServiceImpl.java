package com.lukachangtong.freight.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.api.message.MessageNotifyPort;
import com.lukachangtong.freight.api.message.NotifyReceiver;
import com.lukachangtong.freight.api.waybill.WaybillRouteInfo;
import com.lukachangtong.freight.api.waybill.WaybillRoutePort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.mapper.OutboxEventMapper;
import com.lukachangtong.freight.infra.outbox.OutboxEvent;
import com.lukachangtong.freight.track.entity.TrFence;
import com.lukachangtong.freight.track.entity.TrTrackAlarm;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import com.lukachangtong.freight.track.mapper.TrFenceMapper;
import com.lukachangtong.freight.track.mapper.TrTrackAlarmMapper;
import com.lukachangtong.freight.track.service.AlarmService;
import com.lukachangtong.freight.track.service.TrackService;
import com.lukachangtong.freight.track.support.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在途预警实现。
 *
 * 设计取舍：
 * 1. **不做流式规则引擎**。单库单体、单量在千级，用「上报后按窗口重算 + 幂等键去重」足够，
 *    而且可以随时对历史运单补算，比引入 Flink/CEP 务实得多。
 * 2. **阈值全部可配**（`freight.track.alarm.*`），不同线路/车型限速不同，硬编码是运维灾难。
 * 3. **上报失败不影响入库**：分析异常在 {@link #analyzeQuietly} 里吞掉并记日志，
 *    轨迹是事实数据，不能因为规则算错而丢点。
 * 4. **预警写入同时落 Outbox**：后续 M3 的 Message 四通道与监管上报共用同一条出站通道。
 */
@Service
public class AlarmServiceImpl implements AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmServiceImpl.class);

    private static final int ANALYZE_MAX_POINTS = 20000;
    /** 自动分析的滑动窗口：只看最近 24h，避免每批上报都全量重算历史轨迹 */
    private static final int AUTO_WINDOW_HOURS = 24;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private TrTrackAlarmMapper alarmMapper;
    @Resource
    private TrFenceMapper fenceMapper;
    @Resource
    private TrackService trackService;
    @Resource
    private WaybillRoutePort waybillRoutePort;
    @Resource
    private OutboxEventMapper outboxEventMapper;
    /** 消息通知端口：预警落库后交给 message 模块按模板分发（实现永不抛异常） */
    @Resource
    private ObjectProvider<MessageNotifyPort> messageNotifyPortProvider;

    /** 限速阈值（km/h）；高速/国道差异大，按机构配置是后续项 */
    @Value("${freight.track.alarm.overspeed-kmh:90}")
    private double overspeedKmh;
    /** 判定「停驶」的速度上限（km/h） */
    @Value("${freight.track.alarm.stay-speed-kmh:5}")
    private double staySpeedKmh;
    /** 停驶超过该分钟数才告警（阈值过低会被红绿灯、收费站淹没） */
    @Value("${freight.track.alarm.stay-minutes:30}")
    private int stayMinutes;
    /** 偏航阈值：距装货地→卸货地直线超过该公里数 */
    @Value("${freight.track.alarm.deviate-km:30}")
    private double deviateKm;
    /**
     * 装卸围栏与运单的匹配半径（公里）。
     *
     * 语义区分很关键：**禁行/自定义围栏是机构级的**（所有运单都判），
     * 而装货/卸货围栏只对「装卸点就在该围栏附近」的运单有意义——
     * 否则同机构下几十个装卸围栏会互相判进出，一条运单能刷出十几条无意义预警。
     */
    @Value("${freight.track.alarm.fence-match-km:10}")
    private double fenceMatchKm;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int analyze(Long waybillId, LocalDateTime from, LocalDateTime to) {
        if (waybillId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "waybillId 不能为空");
        }
        List<TrTrackPoint> points = trackService.query(waybillId, from, to, ANALYZE_MAX_POINTS);
        if (points.isEmpty()) {
            return 0;
        }
        WaybillRouteInfo route = waybillRoutePort.load(waybillId);

        List<TrTrackAlarm> candidates = new ArrayList<>();
        detectOverspeed(route, points, candidates);
        detectLongStay(route, points, candidates);
        detectDeviation(route, points, candidates);
        detectFenceTransitions(route, points, candidates);

        if (candidates.isEmpty()) {
            return 0;
        }
        Set<String> existing = existingDedupKeys(waybillId);
        int created = 0;
        for (TrTrackAlarm alarm : candidates) {
            if (!existing.add(alarm.getDedupKey())) {
                continue;
            }
            alarmMapper.insert(alarm);
            writeOutbox(alarm);
            notifyChannels(alarm);
            created++;
        }
        if (created > 0) {
            log.info("运单 {} 新增在途预警 {} 条（候选 {} 条）", waybillId, created, candidates.size());
        }
        return created;
    }

    @Override
    public void analyzeQuietly(Collection<Long> waybillIds) {
        if (waybillIds == null || waybillIds.isEmpty()) {
            return;
        }
        LocalDateTime to = LocalDateTime.now().plusMinutes(5);
        LocalDateTime from = to.minusHours(AUTO_WINDOW_HOURS);
        for (Long id : new HashSet<>(waybillIds)) {
            try {
                analyze(id, from, to);
            } catch (Exception e) {
                log.warn("运单 {} 预警分析失败（不影响轨迹入库）：{}", id, e.getMessage());
            }
        }
    }

    @Override
    public List<TrTrackAlarm> list(Long orgId, Long waybillId, Integer status, String alarmType, Integer limit) {
        QueryWrapper<TrTrackAlarm> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (waybillId != null) {
            qw.eq("waybill_id", waybillId);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        if (alarmType != null && !alarmType.trim().isEmpty()) {
            qw.eq("alarm_type", alarmType.trim().toUpperCase());
        }
        qw.orderByDesc("alarm_time").orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 200 : Math.min(limit, 1000);
        qw.last("LIMIT " + cap);
        return alarmMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrTrackAlarm handle(Long id, String handleBy, String handleRemark, Integer status) {
        TrTrackAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预警不存在：" + id);
        }
        int target = status == null ? TrTrackAlarm.STATUS_HANDLED : status;
        if (target != TrTrackAlarm.STATUS_HANDLED && target != TrTrackAlarm.STATUS_IGNORED) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 1 已处理 / 2 已忽略");
        }
        alarm.setStatus(target);
        alarm.setHandleBy(handleBy);
        alarm.setHandleTime(LocalDateTime.now());
        alarm.setHandleRemark(handleRemark);
        alarmMapper.updateById(alarm);
        return alarm;
    }

    // ------------------------------------------------------------------ 规则

    /** 超速：连续 ≥2 个点超过限速才告警，单点超速多为 GPS 抖动。 */
    private void detectOverspeed(WaybillRouteInfo route, List<TrTrackPoint> points, List<TrTrackAlarm> out) {
        int i = 0;
        int n = points.size();
        while (i < n) {
            Double speed = speedOf(points.get(i));
            if (speed == null || speed <= overspeedKmh) {
                i++;
                continue;
            }
            int start = i;
            double peak = speed;
            while (i + 1 < n) {
                Double next = speedOf(points.get(i + 1));
                if (next == null || next <= overspeedKmh) {
                    break;
                }
                i++;
                peak = Math.max(peak, next);
            }
            int count = i - start + 1;
            if (count >= 2) {
                TrTrackPoint first = points.get(start);
                String content = String.format("超速：连续 %d 个点速度最高 %.0f km/h，超过限值 %.0f km/h",
                        count, peak, overspeedKmh);
                out.add(build(route, TrTrackAlarm.TYPE_OVERSPEED, TrTrackAlarm.LEVEL_HIGH,
                        first, null, content, "OVERSPEED:" + first.getWaybillId() + ":" + first.getGpsTime()));
            }
            i++;
        }
    }

    /** 异常停留：连续低速点跨越的时长超过阈值。只在「低速段起点」报一次，不刷屏。 */
    private void detectLongStay(WaybillRouteInfo route, List<TrTrackPoint> points, List<TrTrackAlarm> out) {
        int i = 0;
        int n = points.size();
        while (i < n) {
            Double speed = speedOf(points.get(i));
            if (speed == null || speed > staySpeedKmh) {
                i++;
                continue;
            }
            int start = i;
            while (i + 1 < n) {
                Double next = speedOf(points.get(i + 1));
                if (next == null || next > staySpeedKmh) {
                    break;
                }
                i++;
            }
            long minutes = Duration.between(points.get(start).getGpsTime(), points.get(i).getGpsTime()).toMinutes();
            if (minutes >= stayMinutes) {
                TrTrackPoint first = points.get(start);
                String content = String.format("异常停留：累计停留 %d 分钟（阈值 %d 分钟），位置 %.5f,%.5f",
                        minutes, stayMinutes, first.getLat().doubleValue(), first.getLng().doubleValue());
                out.add(build(route, TrTrackAlarm.TYPE_LONG_STAY, TrTrackAlarm.LEVEL_MEDIUM,
                        first, null, content, "LONG_STAY:" + first.getWaybillId() + ":" + first.getGpsTime()));
            }
            i++;
        }
    }

    /**
     * 偏航：点到「装货地→卸货地」直线段的距离超过阈值。
     *
     * 这里用直线而不是真实路径，是因为路径规划要接地图服务（有配额与费用）；
     * 对「明显绕路/走错方向」这类预警，直线偏离已经足够，后续接入路线规划再把基线换成折线即可。
     * 一段连续偏航只报一条，避免几十公里偏航刷出上百条告警。
     */
    private void detectDeviation(WaybillRouteInfo route, List<TrTrackPoint> points, List<TrTrackAlarm> out) {
        if (route == null || !route.hasRoute()) {
            return;
        }
        double aLat = route.getLoadLat().doubleValue();
        double aLng = route.getLoadLng().doubleValue();
        double bLat = route.getUnloadLat().doubleValue();
        double bLng = route.getUnloadLng().doubleValue();

        int i = 0;
        int n = points.size();
        while (i < n) {
            TrTrackPoint p = points.get(i);
            double d = GeoUtils.distanceToSegmentKm(p.getLat().doubleValue(), p.getLng().doubleValue(), aLat, aLng, bLat, bLng);
            if (d <= deviateKm) {
                i++;
                continue;
            }
            int start = i;
            double max = d;
            while (i + 1 < n) {
                TrTrackPoint next = points.get(i + 1);
                double nd = GeoUtils.distanceToSegmentKm(next.getLat().doubleValue(), next.getLng().doubleValue(),
                        aLat, aLng, bLat, bLng);
                if (nd <= deviateKm) {
                    break;
                }
                i++;
                max = Math.max(max, nd);
            }
            TrTrackPoint first = points.get(start);
            String content = String.format("偏航：最大偏离计划线路 %.1f km，超出阈值 %.1f km", max, deviateKm);
            out.add(build(route, TrTrackAlarm.TYPE_DEVIATE, TrTrackAlarm.LEVEL_MEDIUM,
                    first, null, content, "DEVIATE:" + first.getWaybillId() + ":" + first.getGpsTime()));
            i++;
        }
    }

    /** 围栏进出：以第一个点建立基线，之后只报状态变化；首点即触发不算「进入」。 */
    private void detectFenceTransitions(WaybillRouteInfo route, List<TrTrackPoint> points, List<TrTrackAlarm> out) {
        if (route == null || route.getOrgId() == null) {
            return;
        }
        QueryWrapper<TrFence> qw = new QueryWrapper<>();
        qw.eq("org_id", route.getOrgId()).eq("enabled", 1);
        List<TrFence> fences = fenceMapper.selectList(qw);
        if (fences.isEmpty()) {
            return;
        }
        for (TrFence fence : fences) {
            if (!fenceApplies(fence, route)) {
                continue;
            }
            boolean circle = !TrFence.SHAPE_POLYGON.equalsIgnoreCase(fence.getShape());
            List<double[]> polygon = circle ? null : FenceServiceImpl.parseVertices(objectMapper, fence.getVertices());
            if (circle && (fence.getCenterLat() == null || fence.getCenterLng() == null
                    || fence.getRadiusM() == null)) {
                continue;
            }
            if (!circle && polygon.size() < 3) {
                continue;
            }
            Boolean prevInside = null;
            for (TrTrackPoint p : points) {
                boolean inside = inside(fence, circle, polygon, p);
                if (prevInside != null && inside != prevInside) {
                    String type = inside ? TrTrackAlarm.TYPE_FENCE_IN : TrTrackAlarm.TYPE_FENCE_OUT;
                    if (triggerAllows(fence.getTriggerType(), type)) {
                        String content = (inside ? "进入围栏：" : "离开围栏：") + fence.getFenceName()
                                + "（" + fence.getFenceCode() + "）";
                        String dedup = type + ":" + p.getWaybillId() + ":" + fence.getId() + ":" + p.getGpsTime();
                        out.add(build(route, type,
                                inside ? TrTrackAlarm.LEVEL_LOW : TrTrackAlarm.LEVEL_MEDIUM,
                                p, fence.getId(), content, dedup));
                    }
                }
                prevInside = inside;
            }
        }
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 围栏是否适用于该运单：
     * 禁行/自定义 → 机构级，全部适用；
     * 装货 → 运单装货点须落在围栏中心 fenceMatchKm 内；
     * 卸货 → 运单卸货点同上（多边形取顶点均值近似中心）。
     * 运单没有选点坐标时无法关联，直接跳过装卸围栏——宁可少报，不可误报。
     */
    private boolean fenceApplies(TrFence fence, WaybillRouteInfo route) {
        String type = fence.getFenceType();
        boolean isLoad = "LOAD".equalsIgnoreCase(type);
        boolean isUnload = "UNLOAD".equalsIgnoreCase(type);
        if (!isLoad && !isUnload) {
            return true;
        }
        if (route == null || !route.hasRoute()) {
            return false;
        }
        BigDecimal refLat = isLoad ? route.getLoadLat() : route.getUnloadLat();
        BigDecimal refLng = isLoad ? route.getLoadLng() : route.getUnloadLng();
        double[] center = fenceCenter(fence);
        if (center == null) {
            return false;
        }
        return GeoUtils.haversineKm(refLat.doubleValue(), refLng.doubleValue(), center[0], center[1]) <= fenceMatchKm;
    }

    /** 圆心直接用；多边形取顶点均值作为近似中心（只用于关联，不用于进出判定）。 */
    private double[] fenceCenter(TrFence fence) {
        if (fence.getCenterLat() != null && fence.getCenterLng() != null) {
            return new double[]{fence.getCenterLat().doubleValue(), fence.getCenterLng().doubleValue()};
        }
        List<double[]> vs = FenceServiceImpl.parseVertices(objectMapper, fence.getVertices());
        if (vs.size() < 3) {
            return null;
        }
        double lat = 0, lng = 0;
        for (double[] v : vs) {
            lat += v[0];
            lng += v[1];
        }
        return new double[]{lat / vs.size(), lng / vs.size()};
    }

    private boolean inside(TrFence fence, boolean circle, List<double[]> polygon, TrTrackPoint p) {
        double lat = p.getLat().doubleValue();
        double lng = p.getLng().doubleValue();
        if (circle) {
            return GeoUtils.inCircle(lat, lng, fence.getCenterLat().doubleValue(),
                    fence.getCenterLng().doubleValue(), fence.getRadiusM());
        }
        return GeoUtils.inPolygon(lat, lng, polygon);
    }

    private boolean triggerAllows(String triggerType, String alarmType) {
        String t = triggerType == null ? TrFence.TRIGGER_BOTH : triggerType;
        if (TrFence.TRIGGER_BOTH.equalsIgnoreCase(t)) {
            return true;
        }
        return TrTrackAlarm.TYPE_FENCE_IN.equals(alarmType)
                ? TrFence.TRIGGER_IN.equalsIgnoreCase(t)
                : TrFence.TRIGGER_OUT.equalsIgnoreCase(t);
    }

    private Double speedOf(TrTrackPoint p) {
        return p.getSpeed() == null ? null : p.getSpeed().doubleValue();
    }

    private TrTrackAlarm build(WaybillRouteInfo route, String type, String level, TrTrackPoint p,
                               Long fenceId, String content, String dedupKey) {
        TrTrackAlarm a = new TrTrackAlarm();
        a.setWaybillId(p.getWaybillId());
        a.setOrgId(route == null ? null : route.getOrgId());
        a.setDriverId(route == null ? null : route.getDriverId());
        a.setVehicleId(route == null ? null : route.getVehicleId());
        a.setAlarmType(type);
        a.setAlarmLevel(level);
        a.setAlarmTime(p.getGpsTime());
        a.setLat(p.getLat());
        a.setLng(p.getLng());
        a.setFenceId(fenceId);
        a.setContent(content.length() > 255 ? content.substring(0, 255) : content);
        a.setDedupKey(dedupKey);
        a.setStatus(TrTrackAlarm.STATUS_PENDING);
        a.setCreateTime(LocalDateTime.now());
        return a;
    }

    private Set<String> existingDedupKeys(Long waybillId) {
        QueryWrapper<TrTrackAlarm> qw = new QueryWrapper<>();
        qw.select("dedup_key").eq("waybill_id", waybillId);
        Set<String> keys = new HashSet<>();
        for (TrTrackAlarm a : alarmMapper.selectList(qw)) {
            keys.add(a.getDedupKey());
        }
        return keys;
    }

    /** 预警同时写入本地消息表：后续监管上报等异步消费方从 Outbox 取，不依赖在线业务。 */
    private void writeOutbox(TrTrackAlarm alarm) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alarmId", alarm.getId());
        payload.put("waybillId", alarm.getWaybillId());
        payload.put("alarmType", alarm.getAlarmType());
        payload.put("alarmLevel", alarm.getAlarmLevel());
        payload.put("alarmTime", String.valueOf(alarm.getAlarmTime()));
        payload.put("content", alarm.getContent());
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            json = "{\"alarmId\":" + alarm.getId() + "}";
        }
        OutboxEvent event = new OutboxEvent();
        event.setEventKey("track-alarm:" + alarm.getId());
        event.setEventType("TRACK_ALARM");
        event.setPayload(json);
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        outboxEventMapper.insert(event);
    }

    /**
     * 预警通知：交给 message 模块按「业务节点 + 通道」模板分发（短信/微信/APP 推送给司机，站内信给调度台）。
     *
     * 这里只发事件，不关心通道；message 侧落 PENDING 后由调度器投递，因此预警事务里没有外部调用。
     * 端口实现被约定为「永不抛异常」，保险起见这里再兜一层，通知绝不能影响预警落库。
     */
    private void notifyChannels(TrTrackAlarm alarm) {
        MessageNotifyPort port = messageNotifyPortProvider.getIfAvailable();
        if (port == null) {
            return;
        }
        try {
            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("alarmType", alarm.getAlarmType());
            vars.put("alarmLevel", alarm.getAlarmLevel());
            vars.put("alarmTime", String.valueOf(alarm.getAlarmTime()));
            vars.put("content", alarm.getContent());
            vars.put("waybillId", alarm.getWaybillId());

            List<NotifyReceiver> receivers = new ArrayList<>();
            if (alarm.getDriverId() != null) {
                receivers.add(new NotifyReceiver(NotifyReceiver.TYPE_DRIVER, alarm.getDriverId()));
            }
            if (alarm.getOrgId() != null) {
                receivers.add(new NotifyReceiver(NotifyReceiver.TYPE_ORG, alarm.getOrgId()));
            }
            if (receivers.isEmpty()) {
                return;
            }
            port.notify(alarm.getOrgId(), "TRACK_ALARM", "alarm:" + alarm.getId(), vars, receivers);
        } catch (Exception e) {
            log.warn("预警 {} 通知分发失败（已忽略）：{}", alarm.getId(), e.getMessage());
        }
    }
}
