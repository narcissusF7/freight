package com.lukachangtong.freight.waybill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.mapper.GoodsSourceMapper;
import com.lukachangtong.freight.waybill.mapper.WaybillMapper;
import com.lukachangtong.freight.waybill.service.MatchingService;
import com.lukachangtong.freight.waybill.vo.MatchVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车货匹配打分（0~100）：
 * 1. 载重适配 40 —— 超载直接标记，不允许派单；
 * 2. 证件有效 25 —— 运输证 13 + 保险 12，过期即 0（合规硬约束）；
 * 3. 线路经验 20 —— 同装货地历史运单数，反映该司机熟路程度；
 * 4. 运价吸引 15 —— 折吨价越高越容易被接单。
 *
 * 只推荐 auditStatus=1 的司机与车辆；车辆未绑定司机的不进入候选（无责任人可追）。
 */
@Service
public class MatchingServiceImpl implements MatchingService {

    private static final int MAX_CAPACITY_SCORE = 40;
    private static final int MAX_CERT_SCORE = 25;
    private static final int MAX_EXP_SCORE = 20;
    private static final int MAX_PRICE_SCORE = 15;
    /** 折吨价基准：元/吨，仅用于演示打分 */
    private static final BigDecimal BENCHMARK_PRICE_PER_TON = new BigDecimal("300");

    @Resource
    private GoodsSourceMapper goodsSourceMapper;
    @Resource
    private CrDriverMapper driverMapper;
    @Resource
    private CrVehicleMapper vehicleMapper;
    @Resource
    private WaybillMapper waybillMapper;

    @Override
    public List<MatchVo> match(Long goodsSourceId, Integer limit) {
        WbGoodsSource gs = goodsSourceMapper.selectById(goodsSourceId);
        if (gs == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "货源不存在");
        }
        int topN = (limit == null || limit <= 0) ? 5 : limit;

        QueryWrapper<CrVehicle> vq = new QueryWrapper<>();
        vq.eq("audit_status", 1).isNotNull("driver_id").eq("org_id", gs.getOrgId());
        List<CrVehicle> vehicles = vehicleMapper.selectList(vq);
        if (vehicles.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CrDriver> driverMap = new HashMap<>();
        QueryWrapper<CrDriver> dq = new QueryWrapper<>();
        dq.eq("audit_status", 1);
        for (CrDriver d : driverMapper.selectList(dq)) {
            driverMap.put(d.getId(), d);
        }

        List<MatchVo> result = new ArrayList<>();
        for (CrVehicle v : vehicles) {
            CrDriver d = driverMap.get(v.getDriverId());
            if (d == null) {
                continue; // 司机未通过审核：不推荐
            }
            MatchVo vo = new MatchVo();
            vo.setDriverId(d.getId());
            vo.setDriverName(d.getName());
            vo.setDriverPhone(d.getPhone());
            vo.setVehicleId(v.getId());
            vo.setPlateNo(v.getPlateNo());
            vo.setVehicleType(v.getVehicleType());
            vo.setCapacityTon(v.getCapacityTon());

            int capacity = scoreCapacity(gs, v, vo);
            int cert = scoreCert(d, v, vo);
            int exp = scoreExperience(gs, d, vo);
            int price = scorePrice(gs, vo);

            vo.setCapacityScore(capacity);
            vo.setCertScore(cert);
            vo.setExperienceScore(exp);
            vo.setPriceScore(price);
            vo.setScore(capacity + cert + exp + price);
            result.add(vo);
        }

        result.sort(Comparator.comparingInt(MatchVo::getScore).reversed()
                .thenComparing(MatchVo::getVehicleId));
        return result.size() > topN ? new ArrayList<>(result.subList(0, topN)) : result;
    }

    private int scoreCapacity(WbGoodsSource gs, CrVehicle v, MatchVo vo) {
        BigDecimal weight = gs.getWeight();
        BigDecimal cap = v.getCapacityTon();
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setCapacityFits(true);
            vo.getReasons().add("货源未填重量，按标准载重处理");
            return MAX_CAPACITY_SCORE * 3 / 4;
        }
        if (cap == null || cap.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setCapacityFits(false);
            vo.getReasons().add("车辆未登记核定载重，无法判断是否超载");
            return 0;
        }
        if (cap.compareTo(weight) >= 0) {
            vo.setCapacityFits(true);
            vo.getReasons().add("载重满足：" + cap + "吨 ≥ 货重" + weight + "吨");
            return MAX_CAPACITY_SCORE;
        }
        vo.setCapacityFits(false);
        vo.getReasons().add("超载风险：核定" + cap + "吨 < 货重" + weight + "吨");
        BigDecimal ratio = cap.divide(weight, 4, RoundingMode.HALF_UP);
        return ratio.multiply(new BigDecimal(MAX_CAPACITY_SCORE))
                .setScale(0, RoundingMode.DOWN).intValue();
    }

    private int scoreCert(CrDriver d, CrVehicle v, MatchVo vo) {
        int score = 0;
        LocalDate today = LocalDate.now();

        // 道路运输证 13
        if (v.getVehicleLicenseExpireDate() == null) {
            score += 6;
            vo.getReasons().add("道路运输证有效期未登记");
        } else if (v.getVehicleLicenseExpireDate().isBefore(today)) {
            vo.getReasons().add("道路运输证已过期（" + v.getVehicleLicenseExpireDate() + "）");
        } else {
            score += 13;
        }

        // 保险 12
        if (v.getInsuranceExpireDate() == null) {
            score += 6;
            vo.getReasons().add("保险有效期未登记");
        } else if (v.getInsuranceExpireDate().isBefore(today)) {
            vo.getReasons().add("车辆保险已过期（" + v.getInsuranceExpireDate() + "）");
        } else {
            score += 12;
        }

        // 司机驾驶证过期：证照维度整体封顶 5 分
        if (d.getLicenseExpireDate() != null && d.getLicenseExpireDate().isBefore(today)) {
            vo.getReasons().add("司机驾驶证已过期（" + d.getLicenseExpireDate() + "）");
            score = Math.min(score, 5);
        }
        return Math.min(score, MAX_CERT_SCORE);
    }

    private int scoreExperience(WbGoodsSource gs, CrDriver d, MatchVo vo) {
        if (gs.getLoadAddr() == null) {
            return MAX_EXP_SCORE / 3;
        }
        QueryWrapper<WbWaybill> qw = new QueryWrapper<>();
        qw.eq("driver_id", d.getId()).eq("load_addr", gs.getLoadAddr());
        long count = waybillMapper.selectCount(qw);
        if (count >= 3) {
            vo.getReasons().add("该司机在「" + gs.getLoadAddr() + "」有 " + count + " 单历史，熟路");
            return MAX_EXP_SCORE;
        }
        if (count > 0) {
            vo.getReasons().add("该司机在「" + gs.getLoadAddr() + "」有 " + count + " 单历史");
            return MAX_EXP_SCORE * 7 / 10;
        }
        vo.getReasons().add("该司机无此线路历史，属新线路");
        return MAX_EXP_SCORE * 3 / 10;
    }

    private int scorePrice(WbGoodsSource gs, MatchVo vo) {
        BigDecimal weight = gs.getWeight();
        BigDecimal price = gs.getFreightPrice();
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0
                || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return MAX_PRICE_SCORE / 2;
        }
        BigDecimal perTon = price.divide(weight, 2, RoundingMode.HALF_UP);
        BigDecimal ratio = perTon.divide(BENCHMARK_PRICE_PER_TON, 4, RoundingMode.HALF_UP);
        int score = ratio.multiply(new BigDecimal(MAX_PRICE_SCORE))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        score = Math.min(score, MAX_PRICE_SCORE);
        vo.getReasons().add("折吨价 " + perTon + " 元/吨");
        return score;
    }
}
