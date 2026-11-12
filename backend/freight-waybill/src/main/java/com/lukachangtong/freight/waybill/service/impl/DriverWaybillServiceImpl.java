package com.lukachangtong.freight.waybill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.api.message.MessageNotifyPort;
import com.lukachangtong.freight.api.message.NotifyReceiver;
import com.lukachangtong.freight.api.waybill.WaybillStatus;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.carrier.service.DriverService;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.common.geo.GeoDistance;
import com.lukachangtong.freight.infra.mapper.OutboxEventMapper;
import com.lukachangtong.freight.infra.outbox.OutboxEvent;
import com.lukachangtong.freight.waybill.dto.DriverNodeConfirmRequest;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;
import com.lukachangtong.freight.waybill.entity.WbNode;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.mapper.GoodsSourceMapper;
import com.lukachangtong.freight.waybill.mapper.NodeMapper;
import com.lukachangtong.freight.waybill.mapper.ReceiptMapper;
import com.lukachangtong.freight.waybill.mapper.WaybillMapper;
import com.lukachangtong.freight.waybill.service.DriverWaybillService;
import com.lukachangtong.freight.waybill.support.BizNoGenerator;
import com.lukachangtong.freight.waybill.support.DriverNodeAction;
import com.lukachangtong.freight.waybill.vo.DriverHallVo;
import com.lukachangtong.freight.waybill.vo.DriverTodoVo;
import com.lukachangtong.freight.waybill.vo.DriverWaybillDetailVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 司机端业务实现。
 *
 * 三条硬约束（都是被真实运营打出来的）：
 * 1. **归属校验**：任何运单操作先比 driverId，越权直接 A0003，绝不返回「运单不存在」——
 *    否则可以通过报错差异探测别人有哪些运单；
 * 2. **抢单原子性**：接单是 `UPDATE wb_goods_source SET status=1 WHERE id=? AND status=0`，
 *    受影响行数为 0 就是被抢了。不用「先查再更新」，那是超卖的经典写法；
 * 3. **到厂校验**：到厂类动作必须在装卸货地半径内，防止司机在服务区点「已到达」。
 *    校验可配置关闭（`freight.driver.geo-check.enabled`），关闭时仍记录距离供人工复核。
 */
@Service
public class DriverWaybillServiceImpl implements DriverWaybillService {

    private static final Logger log = LoggerFactory.getLogger(DriverWaybillServiceImpl.class);

    private static final int MAX_CAPACITY_SCORE = 40;
    private static final int MAX_CERT_SCORE = 25;
    private static final int MAX_ROUTE_SCORE = 20;
    private static final int MAX_PRICE_SCORE = 15;
    private static final BigDecimal BENCHMARK_PRICE_PER_TON = new BigDecimal("300");
    private static final int HALL_SCAN_LIMIT = 300;

    private static final List<String> SCOPE_PENDING_LOAD = Arrays.asList(WaybillStatus.ACCEPTED.name());
    private static final List<String> SCOPE_IN_TRANSIT = Arrays.asList(
            WaybillStatus.LOADED.name(), WaybillStatus.IN_TRANSIT.name());
    private static final List<String> SCOPE_PENDING_SIGN = Arrays.asList(WaybillStatus.UNLOADED.name());
    private static final List<String> SCOPE_DONE = Arrays.asList(
            WaybillStatus.SIGNED.name(), WaybillStatus.RECEIPT_UPLOADED.name(),
            WaybillStatus.SETTLED.name(), WaybillStatus.REPORTED.name(),
            WaybillStatus.COMPLETED.name());

    @Resource
    private WaybillMapper waybillMapper;
    @Resource
    private GoodsSourceMapper goodsSourceMapper;
    @Resource
    private NodeMapper nodeMapper;
    @Resource
    private ReceiptMapper receiptMapper;
    @Resource
    private CrVehicleMapper vehicleMapper;
    @Resource
    private DriverService driverService;
    @Resource
    private OutboxEventMapper outboxEventMapper;
    /** 消息实现可能在裁剪部署中缺失（只跑 waybill 模块），用 Provider 保证可启动 */
    @Resource
    private ObjectProvider<MessageNotifyPort> notifyPortProvider;

    @Value("${freight.driver.geo-check.enabled:true}")
    private boolean geoCheckEnabled;
    @Value("${freight.driver.geo-check.radius-meters:5000}")
    private int geoRadiusMeters;
    @Value("${freight.driver.capacity-check.enabled:true}")
    private boolean capacityCheckEnabled;

    // ------------------------------------------------------------------ 货源大厅

    @Override
    public List<DriverHallVo> hall(Long driverId, BigDecimal lat, BigDecimal lng, Integer limit,
                                   Integer maxDistanceKm) {
        CrDriver driver = requireDriver(driverId);
        CrVehicle vehicle = primaryVehicle(driverId);
        int cap = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);

        QueryWrapper<WbGoodsSource> qw = new QueryWrapper<>();
        qw.eq("status", 0).eq("org_id", driver.getOrgId())
                .orderByAsc("load_time").orderByDesc("id")
                .last("LIMIT " + HALL_SCAN_LIMIT);
        List<WbGoodsSource> sources = goodsSourceMapper.selectList(qw);

        List<DriverHallVo> result = new ArrayList<DriverHallVo>();
        for (WbGoodsSource gs : sources) {
            DriverHallVo vo = new DriverHallVo();
            vo.setGoodsSourceId(gs.getId());
            vo.setGoodsName(gs.getGoodsName());
            vo.setWeight(gs.getWeight());
            vo.setVolume(gs.getVolume());
            vo.setFreightPrice(gs.getFreightPrice());
            vo.setLoadAddr(gs.getLoadAddr());
            vo.setLoadLat(gs.getLoadLat());
            vo.setLoadLng(gs.getLoadLng());
            vo.setUnloadAddr(gs.getUnloadAddr());
            vo.setUnloadLat(gs.getUnloadLat());
            vo.setUnloadLng(gs.getUnloadLng());
            vo.setLoadTime(gs.getLoadTime());
            vo.setContactName(gs.getContactName());
            vo.setContactPhone(BizNoGenerator.maskPhone(gs.getContactPhone()));

            BigDecimal distanceKm = null;
            if (lat != null && lng != null && gs.getLoadLat() != null && gs.getLoadLng() != null) {
                double km = GeoDistance.km(lat.doubleValue(), lng.doubleValue(),
                        gs.getLoadLat().doubleValue(), gs.getLoadLng().doubleValue());
                distanceKm = BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP);
                vo.setDistanceToLoadKm(distanceKm);
                if (maxDistanceKm != null && maxDistanceKm > 0 && km > maxDistanceKm) {
                    continue; // 超出司机可接受的回程半径，不展示
                }
            }

            int capacity = scoreCapacity(gs, vehicle, vo);
            int cert = scoreCert(driver, vehicle, vo);
            int route = scoreRoute(distanceKm, vo);
            int price = scorePrice(gs, vo);
            vo.setCapacityScore(capacity);
            vo.setCertScore(cert);
            vo.setRouteScore(route);
            vo.setPriceScore(price);
            vo.setScore(capacity + cert + route + price);
            result.add(vo);
        }

        // 先按载重可承运分组，再按分数排：超载货源排到最后但仍展示，
        // 让司机看到「为什么接不了」，比直接隐藏更透明
        result.sort(Comparator
                .comparing((DriverHallVo v) -> v.isCapacityFits() ? 1 : 0, Comparator.reverseOrder())
                .thenComparing(DriverHallVo::getScore, Comparator.reverseOrder())
                .thenComparing(DriverHallVo::getGoodsSourceId));
        return result.size() > cap ? new ArrayList<DriverHallVo>(result.subList(0, cap)) : result;
    }

    // ------------------------------------------------------------------ 接单

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long accept(Long goodsSourceId, Long driverId, Long vehicleId, String clientIp) {
        CrDriver driver = requireDriver(driverId);
        if (driver.getAuditStatus() == null || driver.getAuditStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "司机准入未审核通过，暂不能接单");
        }
        WbGoodsSource gs = goodsSourceMapper.selectById(goodsSourceId);
        if (gs == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "货源不存在或已下架");
        }
        if (gs.getStatus() == null || gs.getStatus() != 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "手慢了，该货源已被接单");
        }
        CrVehicle vehicle = resolveVehicle(driverId, vehicleId);
        if (capacityCheckEnabled && !fits(gs.getWeight(), vehicle.getCapacityTon())) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "核定载重 " + vehicle.getCapacityTon() + " 吨小于货重 " + gs.getWeight()
                            + " 吨，不允许承运（超载）");
        }

        // 原子占位：只有把 status 从 0 改成 1 成功的那个请求才算抢到
        int claimed = goodsSourceMapper.update(null, new UpdateWrapper<WbGoodsSource>()
                .set("status", 1)
                .set("update_by", operator(driverId))
                .set("update_time", LocalDateTime.now())
                .eq("id", goodsSourceId)
                .eq("status", 0));
        if (claimed == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "手慢了，该货源已被其他司机接单");
        }

        WbWaybill wb = new WbWaybill();
        wb.setOrgId(gs.getOrgId());
        wb.setGoodsSourceId(gs.getId());
        wb.setShipperId(gs.getShipperId());
        wb.setDriverId(driverId);
        wb.setVehicleId(vehicle.getId());
        wb.setCargoName(gs.getGoodsName());
        wb.setWeight(gs.getWeight());
        wb.setLoadAddr(gs.getLoadAddr());
        wb.setLoadLat(gs.getLoadLat());
        wb.setLoadLng(gs.getLoadLng());
        wb.setUnloadAddr(gs.getUnloadAddr());
        wb.setUnloadLat(gs.getUnloadLat());
        wb.setUnloadLng(gs.getUnloadLng());
        wb.setFreightAmount(gs.getFreightPrice());
        wb.setPlanLoadTime(gs.getLoadTime());
        wb.setStatus(WaybillStatus.ACCEPTED.name());
        wb.setReportStatus(0);
        wb.setWaybillNo(BizNoGenerator.waybillNo());
        wb.setCreateBy(operator(driverId));
        // 司机接单直接产生 ACCEPTED 运单，接单时间就是建单时间，统计的「接单量」才不会漏
        wb.setAcceptTime(LocalDateTime.now());
        wb.setCreateTime(LocalDateTime.now());
        wb.setUpdateTime(LocalDateTime.now());
        waybillMapper.insert(wb);

        // 运单由接单动作直接产生（ACCEPTED），不经过 DRAFT/PUBLISHED，节点里如实记录来源
        appendNode(wb, null, WaybillStatus.ACCEPTED.name(), driverId, "司机接单（司机端 APP）",
                null, null, null, null, clientIp);
        writeOutbox(wb, WaybillStatus.ACCEPTED);

        notifyQuietly(gs.getOrgId(), "WAYBILL_STATUS", wb.getWaybillNo() + ":ACCEPTED:ORG",
                vars(wb.getWaybillNo(), "司机已接单：" + driver.getName(),
                        wb.getLoadAddr(), wb.getUnloadAddr(), wb.getPlanLoadTime()),
                Arrays.asList(new NotifyReceiver(NotifyReceiver.TYPE_ORG, gs.getOrgId())));
        notifyQuietly(gs.getOrgId(), "WAYBILL_ACCEPTED", wb.getWaybillNo() + ":ACCEPTED:DRIVER",
                vars(wb.getWaybillNo(), "接单成功", wb.getLoadAddr(), wb.getUnloadAddr(), wb.getPlanLoadTime()),
                Arrays.asList(new NotifyReceiver(NotifyReceiver.TYPE_DRIVER, driverId)));
        return wb.getId();
    }

    // ------------------------------------------------------------------ 我的运单

    @Override
    public List<WbWaybill> myWaybills(Long driverId, String scope) {
        QueryWrapper<WbWaybill> qw = new QueryWrapper<>();
        qw.eq("driver_id", driverId);
        List<String> statuses = scopeOf(scope);
        if (statuses != null) {
            qw.in("status", statuses);
        }
        qw.orderByDesc("id");
        return waybillMapper.selectList(qw);
    }

    @Override
    public DriverTodoVo todo(Long driverId) {
        DriverTodoVo vo = new DriverTodoVo();
        vo.setPendingLoad(countByStatus(driverId, SCOPE_PENDING_LOAD));
        vo.setInTransit(countByStatus(driverId, SCOPE_IN_TRANSIT));
        vo.setPendingSign(countByStatus(driverId, SCOPE_PENDING_SIGN));

        List<String> ongoing = new ArrayList<String>();
        ongoing.addAll(SCOPE_PENDING_LOAD);
        ongoing.addAll(SCOPE_IN_TRANSIT);
        ongoing.addAll(SCOPE_PENDING_SIGN);
        QueryWrapper<WbWaybill> qw = new QueryWrapper<>();
        qw.eq("driver_id", driverId).in("status", ongoing).orderByAsc("plan_load_time");
        vo.setOngoing(waybillMapper.selectList(qw));
        return vo;
    }

    // ------------------------------------------------------------------ 详情

    @Override
    public DriverWaybillDetailVo detail(Long waybillId, Long driverId, BigDecimal lat, BigDecimal lng) {
        WbWaybill wb = requireOwnedWaybill(waybillId, driverId);
        DriverWaybillDetailVo vo = new DriverWaybillDetailVo();
        vo.setWaybill(wb);
        vo.setStatusText(statusText(wb.getStatus()));

        if (wb.getVehicleId() != null) {
            CrVehicle v = vehicleMapper.selectById(wb.getVehicleId());
            vo.setPlateNo(v == null ? null : v.getPlateNo());
        }
        if (wb.getGoodsSourceId() != null) {
            WbGoodsSource gs = goodsSourceMapper.selectById(wb.getGoodsSourceId());
            if (gs != null) {
                vo.setContactName(gs.getContactName());
                // 接单后可以看到完整联系方式——司机必须能联系上装卸货现场
                vo.setContactPhone(gs.getContactPhone());
            }
        }

        QueryWrapper<WbNode> nq = new QueryWrapper<WbNode>();
        nq.eq("waybill_id", waybillId).orderByAsc("id");
        vo.setNodes(nodeMapper.selectList(nq));

        QueryWrapper<WbReceipt> rq = new QueryWrapper<WbReceipt>();
        rq.eq("waybill_id", waybillId).orderByDesc("id");
        vo.setReceipts(receiptMapper.selectList(rq));

        if (lat != null && lng != null) {
            vo.setDistanceToLoadM(distanceM(lat, lng, wb.getLoadLat(), wb.getLoadLng()));
            vo.setDistanceToUnloadM(distanceM(lat, lng, wb.getUnloadLat(), wb.getUnloadLng()));
        }

        WaybillStatus current = parse(wb.getStatus());
        for (DriverNodeAction action : DriverNodeAction.values()) {
            if (action.getRequiredFrom() != current) {
                continue;
            }
            // 已完成的一次性打点不再作为「可执行动作」下发：到达装货地做完，下一步就该是装货完成，
            // 否则 APP 首页会一直提示「到达装货地」，司机反复点。
            if (action.isCheckpointOnly() && checkpointDone(waybillId, current, action.getLabel())) {
                continue;
            }
            vo.getAvailableActions().add(new DriverWaybillDetailVo.ActionVo(
                    action.name(), action.getLabel(), action.isLocationRequired()));
        }
        if (!vo.getAvailableActions().isEmpty()) {
            DriverNodeAction next = DriverNodeAction.valueOf(vo.getAvailableActions().get(0).getAction());
            vo.setNextAction(next.name());
            vo.setNextActionLabel(next.getLabel());
        } else if (current == WaybillStatus.SIGNED) {
            vo.setTip("签收完成，请拍摄并上传回单照片");
        }
        return vo;
    }

    // ------------------------------------------------------------------ 节点确认

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DriverWaybillDetailVo confirm(Long waybillId, Long driverId,
                                         DriverNodeConfirmRequest request, String clientIp) {
        WbWaybill wb = requireOwnedWaybill(waybillId, driverId);
        DriverNodeAction action;
        try {
            action = DriverNodeAction.of(request.getAction());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知的节点动作: " + request.getAction());
        }
        WaybillStatus current = parse(wb.getStatus());

        // 幂等：流转类动作已经在目标状态，直接回详情（弱网重试、用户连点都不会报错）
        if (!action.isCheckpointOnly() && current == action.getTarget()) {
            DriverWaybillDetailVo vo = detail(waybillId, driverId, request.getLat(), request.getLng());
            vo.setTip("该节点已确认过，无需重复提交");
            return vo;
        }
        if (current != action.getRequiredFrom()) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "当前状态「" + statusText(current.name()) + "」不能执行「" + action.getLabel()
                            + "」，请刷新后重试");
        }

        // 定位与到厂校验：只有「到厂类」动作做半径校验；发车是从装货地离开，不做半径限制，
        // 但仍记录距装货地的距离（用于事后核查是否真的从厂区发车）。
        Integer distance = null;
        boolean isLoadSide = action != DriverNodeAction.ARRIVE_UNLOAD
                && action != DriverNodeAction.UNLOADED
                && action != DriverNodeAction.SIGNED;
        BigDecimal refLat = isLoadSide ? wb.getLoadLat() : wb.getUnloadLat();
        BigDecimal refLng = isLoadSide ? wb.getLoadLng() : wb.getUnloadLng();
        String refName = isLoadSide ? "装货地" : "卸货地";
        if (request.getLat() != null && request.getLng() != null) {
            distance = distanceM(request.getLat(), request.getLng(), refLat, refLng);
            if (action.isLocationRequired() && geoCheckEnabled && distance != null && distance > geoRadiusMeters) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "当前位置距" + refName + " " + formatKm(distance) + "，超出 " + formatKm(geoRadiusMeters)
                                + " 确认范围，请到达" + refName + "后再确认");
            }
        } else if (action.isLocationRequired()) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "「" + action.getLabel() + "」需要定位信息，请开启定位后重试");
        }

        // 打点类动作去重：同一状态的同一动作只留一条，避免连点刷出重复节点
        if (action.isCheckpointOnly() && lastNodeIs(wb.getId(), current, action.getLabel())) {
            DriverWaybillDetailVo vo = detail(waybillId, driverId, request.getLat(), request.getLng());
            vo.setTip("「" + action.getLabel() + "」已记录，无需重复提交");
            return vo;
        }

        String remark = action.getLabel()
                + (request.getRemark() == null || request.getRemark().trim().isEmpty()
                ? "" : "：" + request.getRemark().trim());

        if (action.isCheckpointOnly()) {
            appendNode(wb, current.name(), current.name(), driverId, remark,
                    request.getLat(), request.getLng(), request.getAddress(),
                    joinPhotos(request.getPhotoUrls()), clientIp);
        } else {
            advance(wb, current, action.getTarget(), driverId, remark,
                    request.getLat(), request.getLng(), request.getAddress(),
                    joinPhotos(request.getPhotoUrls()), clientIp);
        }

        DriverWaybillDetailVo vo = detail(waybillId, driverId, request.getLat(), request.getLng());
        vo.setTip("「" + action.getLabel() + "」已提交"
                + (distance == null ? "" : "（距" + refName + " " + formatKm(distance) + "）"));
        return vo;
    }

    // ------------------------------------------------------------------ 内部

    /** 状态推进：乐观锁（status 必须还是读到的那个值）+ 节点留痕 + Outbox。 */
    private void advance(WbWaybill wb, WaybillStatus from, WaybillStatus target, Long driverId,
                         String remark, BigDecimal lat, BigDecimal lng, String address,
                         String photos, String clientIp) {
        int rows = waybillMapper.update(null, new UpdateWrapper<WbWaybill>()
                .set("status", target.name())
                .set("update_by", operator(driverId))
                .set("update_time", LocalDateTime.now())
                .eq("id", wb.getId())
                .eq("status", from.name()));
        if (rows == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "运单状态已被其他端更新，请刷新后重试");
        }
        appendNode(wb, from.name(), target.name(), driverId, remark, lat, lng, address, photos, clientIp);
        writeOutbox(wb, target);
        notifyQuietly(wb.getOrgId(), "WAYBILL_STATUS", wb.getWaybillNo() + ":" + target.name() + ":ORG",
                vars(wb.getWaybillNo(), statusText(target.name()),
                        wb.getLoadAddr(), wb.getUnloadAddr(), wb.getPlanLoadTime()),
                Arrays.asList(new NotifyReceiver(NotifyReceiver.TYPE_ORG, wb.getOrgId())));
    }

    private void appendNode(WbWaybill wb, String from, String to, Long driverId, String remark,
                            BigDecimal lat, BigDecimal lng, String address, String photos, String clientIp) {
        WbNode node = new WbNode();
        node.setWaybillId(wb.getId());
        node.setFromStatus(from);
        node.setToStatus(to);
        node.setOperateBy(operator(driverId));
        node.setOperateTime(LocalDateTime.now());
        node.setRemark(remark);
        node.setLat(lat);
        node.setLng(lng);
        node.setAddress(address);
        node.setEvidenceUrls(photos);
        node.setSource("DRIVER_APP");
        node.setClientIp(clientIp);
        nodeMapper.insert(node);
    }

    private boolean lastNodeIs(Long waybillId, WaybillStatus current, String label) {
        WbNode last = nodeMapper.selectOne(new QueryWrapper<WbNode>()
                .eq("waybill_id", waybillId).orderByDesc("id").last("LIMIT 1"));
        if (last == null) {
            return false;
        }
        return current.name().equals(last.getToStatus())
                && last.getRemark() != null && last.getRemark().startsWith(label);
    }

    /** 该状态下是否已经打过这个点（到达装货地/到达卸货地）。 */
    private boolean checkpointDone(Long waybillId, WaybillStatus current, String label) {
        List<WbNode> nodes = nodeMapper.selectList(new QueryWrapper<WbNode>()
                .eq("waybill_id", waybillId).eq("to_status", current.name())
                .likeRight("remark", label));
        return nodes != null && !nodes.isEmpty();
    }

    private void writeOutbox(WbWaybill wb, WaybillStatus target) {
        OutboxEvent event = new OutboxEvent();
        event.setEventKey(wb.getWaybillNo() + ":" + target.name() + ":" + System.currentTimeMillis());
        event.setEventType("WAYBILL_STATUS_CHANGED");
        event.setPayload("{\"waybillNo\":\"" + wb.getWaybillNo() + "\",\"status\":\"" + target.name() + "\"}");
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        outboxEventMapper.insert(event);
    }

    private void notifyQuietly(Long orgId, String bizType, String bizKey,
                               Map<String, Object> vars, List<NotifyReceiver> receivers) {
        MessageNotifyPort port = notifyPortProvider == null ? null : notifyPortProvider.getIfAvailable();
        if (port == null) {
            return;
        }
        try {
            port.notify(orgId, bizType, bizKey, vars, receivers);
        } catch (Exception e) {
            // 通知是旁路，失败不能回滚接单/确认事务
            log.warn("司机端消息通知失败 bizType={} bizKey={} err={}", bizType, bizKey, e.getMessage());
        }
    }

    private Map<String, Object> vars(String waybillNo, String status, String loadAddr,
                                     String unloadAddr, LocalDateTime loadTime) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("waybillNo", waybillNo);
        m.put("status", status);
        m.put("loadAddr", loadAddr == null ? "" : loadAddr);
        m.put("unloadAddr", unloadAddr == null ? "" : unloadAddr);
        m.put("loadTime", loadTime == null ? ""
                : loadTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        m.put("waybillId", waybillNo);
        return m;
    }

    private CrDriver requireDriver(Long driverId) {
        CrDriver driver = driverService.findById(driverId);
        if (driver == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "司机不存在或已离职");
        }
        return driver;
    }

    private WbWaybill requireOwnedWaybill(Long waybillId, Long driverId) {
        WbWaybill wb = waybillMapper.selectById(waybillId);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        if (driverId == null || !driverId.equals(wb.getDriverId())) {
            // 不暴露「运单存在但不属于你」的信息，统一按无权限处理
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该运单");
        }
        return wb;
    }

    private CrVehicle primaryVehicle(Long driverId) {
        List<CrVehicle> list = vehicleMapper.selectList(new QueryWrapper<CrVehicle>()
                .eq("driver_id", driverId).eq("audit_status", 1).orderByDesc("id"));
        return list.isEmpty() ? null : list.get(0);
    }

    private CrVehicle resolveVehicle(Long driverId, Long vehicleId) {
        if (vehicleId != null) {
            CrVehicle v = vehicleMapper.selectById(vehicleId);
            if (v == null || !driverId.equals(v.getDriverId())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "车辆不存在或不属于当前司机");
            }
            if (v.getAuditStatus() == null || v.getAuditStatus() != 1) {
                throw new BizException(ErrorCode.FORBIDDEN, "车辆未审核通过，不能承运");
            }
            return v;
        }
        CrVehicle v = primaryVehicle(driverId);
        if (v == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "名下没有审核通过的车辆，请先在承运管理中登记并审核");
        }
        return v;
    }

    private boolean fits(BigDecimal weight, BigDecimal capacity) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return capacity.compareTo(weight) >= 0;
    }

    private int scoreCapacity(WbGoodsSource gs, CrVehicle v, DriverHallVo vo) {
        if (v == null) {
            vo.setCapacityFits(false);
            vo.getReasons().add("名下暂无审核通过的车辆，接单前请先登记车辆");
            return 0;
        }
        BigDecimal weight = gs.getWeight();
        BigDecimal cap = v.getCapacityTon();
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setCapacityFits(true);
            vo.getReasons().add("货源未填重量");
            return MAX_CAPACITY_SCORE * 3 / 4;
        }
        if (cap == null || cap.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setCapacityFits(false);
            vo.getReasons().add("车辆未登记核定载重");
            return 0;
        }
        if (cap.compareTo(weight) >= 0) {
            vo.setCapacityFits(true);
            // 越贴合越高分：空放浪费运力，只给 70% 分
            BigDecimal ratio = weight.divide(cap, 4, RoundingMode.HALF_UP);
            int base = ratio.compareTo(new BigDecimal("0.6")) >= 0
                    ? MAX_CAPACITY_SCORE
                    : MAX_CAPACITY_SCORE * 7 / 10;
            vo.getReasons().add("本车 " + v.getPlateNo() + " 核定 " + cap + " 吨，可承运 " + weight + " 吨");
            return base;
        }
        vo.setCapacityFits(false);
        vo.getReasons().add("超载：" + cap + " 吨 < 货重 " + weight + " 吨，不可接单");
        BigDecimal ratio = cap.divide(weight, 4, RoundingMode.HALF_UP);
        return ratio.multiply(new BigDecimal(MAX_CAPACITY_SCORE)).setScale(0, RoundingMode.DOWN).intValue();
    }

    private int scoreCert(CrDriver d, CrVehicle v, DriverHallVo vo) {
        LocalDate today = LocalDate.now();
        if (v == null) {
            return 0;
        }
        int score = 0;
        if (v.getVehicleLicenseExpireDate() == null) {
            score += 6;
            vo.getReasons().add("道路运输证有效期未登记");
        } else if (v.getVehicleLicenseExpireDate().isBefore(today)) {
            vo.getReasons().add("道路运输证已过期");
        } else {
            score += 13;
        }
        if (v.getInsuranceExpireDate() == null) {
            score += 6;
            vo.getReasons().add("保险有效期未登记");
        } else if (v.getInsuranceExpireDate().isBefore(today)) {
            vo.getReasons().add("车辆保险已过期");
        } else {
            score += 12;
        }
        if (d.getLicenseExpireDate() != null && d.getLicenseExpireDate().isBefore(today)) {
            vo.getReasons().add("驾驶证已过期，请尽快换证");
            score = Math.min(score, 5);
        }
        return Math.min(score, MAX_CERT_SCORE);
    }

    private int scoreRoute(BigDecimal distanceKm, DriverHallVo vo) {
        if (distanceKm == null) {
            vo.getReasons().add("未获取定位，无法判断距装货地距离");
            return MAX_ROUTE_SCORE / 2;
        }
        double km = distanceKm.doubleValue();
        vo.getReasons().add("距装货地约 " + distanceKm + " km");
        if (km <= 30) {
            return MAX_ROUTE_SCORE;
        }
        if (km <= 100) {
            return MAX_ROUTE_SCORE * 7 / 10;
        }
        if (km <= 300) {
            return MAX_ROUTE_SCORE * 4 / 10;
        }
        return MAX_ROUTE_SCORE / 5;
    }

    private int scorePrice(WbGoodsSource gs, DriverHallVo vo) {
        BigDecimal weight = gs.getWeight();
        BigDecimal price = gs.getFreightPrice();
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0
                || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            vo.getReasons().add("运价信息不完整");
            return MAX_PRICE_SCORE / 2;
        }
        BigDecimal perTon = price.divide(weight, 2, RoundingMode.HALF_UP);
        vo.setPricePerTon(perTon);
        vo.getReasons().add("折吨价 " + perTon + " 元/吨");
        int score = perTon.divide(BENCHMARK_PRICE_PER_TON, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(MAX_PRICE_SCORE)).setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.min(score, MAX_PRICE_SCORE);
    }

    private int countByStatus(Long driverId, List<String> statuses) {
        Long n = waybillMapper.selectCount(new QueryWrapper<WbWaybill>()
                .eq("driver_id", driverId).in("status", statuses));
        return n == null ? 0 : n.intValue();
    }

    private List<String> scopeOf(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return null;
        }
        String s = scope.trim().toUpperCase();
        if ("PENDING_LOAD".equals(s)) {
            return SCOPE_PENDING_LOAD;
        }
        if ("IN_TRANSIT".equals(s)) {
            return SCOPE_IN_TRANSIT;
        }
        if ("PENDING_SIGN".equals(s)) {
            return SCOPE_PENDING_SIGN;
        }
        if ("DONE".equals(s)) {
            return SCOPE_DONE;
        }
        return null; // ALL
    }

    private WaybillStatus parse(String status) {
        try {
            return WaybillStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "运单状态异常: " + status);
        }
    }

    private Integer distanceM(BigDecimal lat, BigDecimal lng, BigDecimal refLat, BigDecimal refLng) {
        if (lat == null || lng == null || refLat == null || refLng == null) {
            return null;
        }
        double m = GeoDistance.meters(lat.doubleValue(), lng.doubleValue(),
                refLat.doubleValue(), refLng.doubleValue());
        return (int) Math.round(m);
    }

    private String formatKm(Integer meters) {
        if (meters == null) {
            return "-";
        }
        if (meters < 1000) {
            return meters + " 米";
        }
        return BigDecimal.valueOf(meters).divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP) + " 公里";
    }

    private String joinPhotos(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String u : urls) {
            if (u == null || u.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(u.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String operator(Long driverId) {
        return "DRIVER:" + driverId;
    }

    private String statusText(String status) {
        if (status == null) {
            return "-";
        }
        switch (status) {
            case "DRAFT": return "草稿";
            case "PUBLISHED": return "已发布待接单";
            case "ACCEPTED": return "已接单待装货";
            case "LOADED": return "已装货待发车";
            case "IN_TRANSIT": return "运输中";
            case "UNLOADED": return "已卸货待签收";
            case "SIGNED": return "已签收待回单";
            case "RECEIPT_UPLOADED": return "回单已上传待结算";
            case "SETTLED": return "已结算待上报";
            case "REPORTED": return "已上报监管";
            case "COMPLETED": return "已完成";
            case "EXCEPTION": return "异常处理中";
            case "WITHDRAWN": return "已撤回";
            case "VOID": return "已作废";
            default: return status;
        }
    }
}
