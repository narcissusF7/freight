package com.lukachangtong.freight.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.stat.entity.StatDaily;
import com.lukachangtong.freight.stat.entity.StatDim;
import com.lukachangtong.freight.stat.mapper.StatDailyMapper;
import com.lukachangtong.freight.stat.mapper.StatDimMapper;
import com.lukachangtong.freight.stat.mapper.StatReadMapper;
import com.lukachangtong.freight.stat.service.StatService;
import com.lukachangtong.freight.stat.support.RegionParser;
import com.lukachangtong.freight.stat.support.StatMetric;
import com.lukachangtong.freight.stat.vo.AggRow;
import com.lukachangtong.freight.stat.vo.StatDetailRow;
import com.lukachangtong.freight.stat.vo.StatDimVo;
import com.lukachangtong.freight.stat.vo.StatOverviewVo;
import com.lukachangtong.freight.stat.vo.StatTrendVo;
import com.lukachangtong.freight.stat.vo.WaybillRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计实现。
 *
 * 几个刻意的取舍：
 * 1. **重算而不是增量**：区间重算最多几百次聚合查询，而增量更新只要漏一次事件，
 *    数字就永久错了——统计错了没人会发现，这比慢更可怕；
 * 2. **口径集中在服务端**：完成率、平均星级、占比都在这里算，前端只展示；
 * 3. **穿透做服务端过滤**：维度过滤（省域）依赖地址解析，SQL 里没法 group by 自由文本，
 *    所以先按日期捞明细再过滤，并对结果集设上限。
 */
@Service
public class StatServiceImpl implements StatService {

    private static final Logger log = LoggerFactory.getLogger(StatServiceImpl.class);

    private static final int MAX_RANGE_DAYS = 366;
    /** 自动补算的保护上限：超过这个天数要求显式触发重算，避免一次查询打出上千条 SQL */
    private static final int AUTO_REBUILD_DAYS = 31;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private StatReadMapper readMapper;
    @Resource
    private StatDailyMapper dailyMapper;
    @Resource
    private StatDimMapper dimMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuild(LocalDate from, LocalDate to, Long orgId, String operator) {
        LocalDate start = from == null ? LocalDate.now().minusDays(1) : from;
        LocalDate end = to == null ? start : to;
        checkRange(start, end);
        long oid = orgId == null ? 0L : orgId;

        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyMapper.upsert(aggregateDay(d, oid));
            rebuildDims(d, oid);
            days++;
        }
        log.info("统计重算完成 org={} {}~{} 共 {} 天 operator={}", oid, start, end, days, operator);
        return days;
    }

    @Override
    public StatOverviewVo overview(LocalDate from, LocalDate to, Long orgId) {
        LocalDate start = from == null ? LocalDate.now().minusDays(6) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        long oid = orgId == null ? 0L : orgId;

        List<StatDaily> rows = queryDaily(start, end, oid);
        String mode = "预聚合";
        if (rows.isEmpty()) {
            long span = end.toEpochDay() - start.toEpochDay() + 1;
            if (span <= AUTO_REBUILD_DAYS) {
                rebuild(start, end, orgId, "AUTO");
                rows = queryDaily(start, end, oid);
                mode = "实时补算";
            } else {
                mode = "未预聚合（区间超过 " + AUTO_REBUILD_DAYS + " 天，请先重算）";
            }
        }

        StatOverviewVo vo = new StatOverviewVo();
        vo.setFrom(start.format(DAY_FMT));
        vo.setTo(end.format(DAY_FMT));
        vo.setCalcTime(mode + " · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        long created = 0;
        long signed = 0;
        BigDecimal createdAmount = BigDecimal.ZERO;
        BigDecimal signedAmount = BigDecimal.ZERO;
        long settleCount = 0;
        long payCount = 0;
        BigDecimal settleAmount = BigDecimal.ZERO;
        BigDecimal payAmount = BigDecimal.ZERO;
        long complaint = 0;
        long overdue = 0;
        long ratingCount = 0;
        long ratingSum = 0;
        long alarm = 0;

        for (StatDaily r : rows) {
            created += nz(r.getWaybillCreated());
            signed += nz(r.getWaybillSigned());
            createdAmount = createdAmount.add(bdz(r.getWaybillAmount()));
            signedAmount = signedAmount.add(bdz(r.getSignedAmount()));
            settleCount += nz(r.getSettleCount());
            payCount += nz(r.getPayCount());
            settleAmount = settleAmount.add(bdz(r.getSettleAmount()));
            payAmount = payAmount.add(bdz(r.getPayAmount()));
            complaint += nz(r.getComplaintCount());
            overdue += nz(r.getComplaintOverdue());
            ratingCount += nz(r.getRatingCount());
            ratingSum += nz(r.getRatingSum());
            alarm += nz(r.getAlarmCount());
        }

        vo.setWaybillCreated(created);
        vo.setWaybillSigned(signed);
        vo.setWaybillAmount(createdAmount);
        vo.setSignedAmount(signedAmount);
        vo.setCompletionRate(created == 0 ? 0 : round4((double) signed / created));
        vo.setSettleCount(settleCount);
        vo.setSettleAmount(settleAmount);
        vo.setPayCount(payCount);
        vo.setPayAmount(payAmount);
        vo.setComplaintCount(complaint);
        vo.setComplaintOverdue(overdue);
        vo.setRatingCount(ratingCount);
        vo.setAvgRating(ratingCount == 0 ? 0 : round2((double) ratingSum / ratingCount));
        vo.setAlarmCount(alarm);
        return vo;
    }

    @Override
    public List<StatTrendVo> trend(LocalDate from, LocalDate to, Long orgId, String granularity) {
        LocalDate start = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        long oid = orgId == null ? 0L : orgId;
        String g = granularity == null ? "DAY" : granularity.trim().toUpperCase();

        List<StatDaily> rows = queryDaily(start, end, oid);
        if (rows.isEmpty()) {
            long span = end.toEpochDay() - start.toEpochDay() + 1;
            if (span <= AUTO_REBUILD_DAYS) {
                rebuild(start, end, orgId, "AUTO");
                rows = queryDaily(start, end, oid);
            }
        }

        // 按周期聚合，保持日期顺序（LinkedHashMap 保序，前端画折线不需要再排序）
        Map<String, StatTrendVo> buckets = new LinkedHashMap<String, StatTrendVo>();
        Map<String, long[]> ratingAcc = new LinkedHashMap<String, long[]>();
        for (StatDaily r : rows) {
            String key = periodOf(r.getBizDate(), g);
            StatTrendVo point = buckets.get(key);
            if (point == null) {
                point = new StatTrendVo();
                point.setPeriod(key);
                point.setSettleAmount(BigDecimal.ZERO);
                point.setPayAmount(BigDecimal.ZERO);
                buckets.put(key, point);
                ratingAcc.put(key, new long[]{0, 0});
            }
            point.setWaybillCreated(point.getWaybillCreated() + nz(r.getWaybillCreated()));
            point.setWaybillSigned(point.getWaybillSigned() + nz(r.getWaybillSigned()));
            point.setSettleAmount(point.getSettleAmount().add(bdz(r.getSettleAmount())));
            point.setPayAmount(point.getPayAmount().add(bdz(r.getPayAmount())));
            point.setComplaintCount(point.getComplaintCount() + nz(r.getComplaintCount()));
            point.setComplaintOverdue(point.getComplaintOverdue() + nz(r.getComplaintOverdue()));
            point.setAlarmCount(point.getAlarmCount() + nz(r.getAlarmCount()));
            long[] acc = ratingAcc.get(key);
            acc[0] += nz(r.getRatingCount());
            acc[1] += nz(r.getRatingSum());
        }
        for (Map.Entry<String, StatTrendVo> e : buckets.entrySet()) {
            long[] acc = ratingAcc.get(e.getKey());
            StatTrendVo point = e.getValue();
            point.setRatingCount(acc[0]);
            point.setAvgRating(acc[0] == 0 ? 0 : round2((double) acc[1] / acc[0]));
        }
        return new ArrayList<StatTrendVo>(buckets.values());
    }

    @Override
    public List<StatDimVo> dims(LocalDate from, LocalDate to, Long orgId, String dimType, Integer limit) {
        LocalDate start = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        long oid = orgId == null ? 0L : orgId;
        String type = dimType == null ? StatDim.DIM_LOAD_PROVINCE : dimType.trim().toUpperCase();

        QueryWrapper<StatDim> qw = new QueryWrapper<StatDim>();
        qw.eq("org_id", oid).eq("dim_type", type)
                .ge("biz_date", start).le("biz_date", end);
        List<StatDim> rows = dimMapper.selectList(qw);
        if (rows.isEmpty() && end.toEpochDay() - start.toEpochDay() < AUTO_REBUILD_DAYS) {
            rebuild(start, end, orgId, "AUTO");
            rows = dimMapper.selectList(qw);
        }

        Map<String, StatDimVo> merged = new LinkedHashMap<String, StatDimVo>();
        long total = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (StatDim d : rows) {
            StatDimVo vo = merged.get(d.getDimKey());
            if (vo == null) {
                vo = new StatDimVo();
                vo.setDimType(d.getDimType());
                vo.setDimKey(d.getDimKey());
                vo.setDimLabel(d.getDimLabel() == null ? d.getDimKey() : d.getDimLabel());
                merged.put(d.getDimKey(), vo);
            }
            vo.setWaybillCount(vo.getWaybillCount() + nz(d.getWaybillCount()));
            vo.setAmount(bdz(vo.getAmount()).add(bdz(d.getAmount())));
            total += nz(d.getWaybillCount());
            totalAmount = totalAmount.add(bdz(d.getAmount()));
        }
        List<StatDimVo> list = new ArrayList<StatDimVo>(merged.values());
        for (StatDimVo vo : list) {
            vo.setShare(total == 0 ? 0 : round4((double) vo.getWaybillCount() / total));
            vo.setAmountShare(totalAmount.signum() == 0 ? 0
                    : round4(vo.getAmount().doubleValue() / totalAmount.doubleValue()));
        }
        list.sort(Comparator.comparingLong(StatDimVo::getWaybillCount).reversed());
        int cap = (limit == null || limit <= 0) ? 15 : Math.min(limit, 50);
        return list.size() <= cap ? list : list.subList(0, cap);
    }

    @Override
    public List<StatDetailRow> drill(LocalDate from, LocalDate to, Long orgId, String metric,
                                     String dimType, String dimKey, Integer limit) {
        LocalDate start = from == null ? LocalDate.now().minusDays(6) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        StatMetric m;
        try {
            m = StatMetric.of(metric);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, e.getMessage());
        }
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        LocalDateTime fromTs = start.atStartOfDay();
        LocalDateTime toTs = end.plusDays(1).atStartOfDay();

        List<StatDetailRow> rows;
        switch (m) {
            case WAYBILL_CREATED:
                rows = readMapper.drillWaybillCreated(fromTs, toTs, orgId, cap * 5);
                break;
            case WAYBILL_SIGNED:
                rows = readMapper.drillWaybillSigned(fromTs, toTs, orgId, cap * 5);
                break;
            case SETTLEMENT:
                rows = readMapper.drillSettlement(start, end.plusDays(1), orgId, cap);
                break;
            case PAYMENT:
                rows = readMapper.drillPayment(fromTs, toTs, orgId, cap);
                break;
            case COMPLAINT:
                rows = readMapper.drillComplaint(fromTs, toTs, orgId, cap);
                break;
            case RATING:
                rows = readMapper.drillRating(fromTs, toTs, orgId, cap);
                break;
            case ALARM:
            default:
                rows = readMapper.drillAlarm(fromTs, toTs, orgId, cap);
                break;
        }

        List<StatDetailRow> filtered = filterByDim(rows, dimType, dimKey);
        return filtered.size() <= cap ? filtered : new ArrayList<StatDetailRow>(filtered.subList(0, cap));
    }

    @Override
    public List<StatDaily> daily(LocalDate from, LocalDate to, Long orgId) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        checkRange(start, end);
        return queryDaily(start, end, orgId == null ? 0L : orgId);
    }

    // ------------------------------------------------------------------ 内部

    /** 单日聚合：一天 9 条聚合 SQL，比扫明细表算趋势便宜得多。 */
    private StatDaily aggregateDay(LocalDate day, long orgId) {
        LocalDateTime fromTs = day.atStartOfDay();
        LocalDateTime toTs = day.plusDays(1).atStartOfDay();
        Long oid = orgId == 0L ? null : orgId;

        StatDaily d = new StatDaily();
        d.setBizDate(day);
        d.setOrgId(orgId);

        AggRow created = readMapper.aggWaybillCreated(fromTs, toTs, oid);
        d.setWaybillCreated(intOf(created == null ? null : created.getCnt()));
        d.setWaybillAmount(created == null ? BigDecimal.ZERO : bdz(created.getAmt()));

        AggRow signed = readMapper.aggWaybillSigned(fromTs, toTs, oid);
        d.setWaybillSigned(intOf(signed == null ? null : signed.getCnt()));
        d.setSignedAmount(signed == null ? BigDecimal.ZERO : bdz(signed.getAmt()));

        AggRow ex = readMapper.aggException(fromTs, toTs, oid);
        d.setWaybillException(intOf(ex == null ? null : ex.getCnt()));

        AggRow settle = readMapper.aggSettlement(day, day.plusDays(1), oid);
        d.setSettleCount(intOf(settle == null ? null : settle.getCnt()));
        d.setSettleAmount(settle == null ? BigDecimal.ZERO : bdz(settle.getAmt()));

        AggRow pay = readMapper.aggPayment(fromTs, toTs, oid);
        d.setPayCount(intOf(pay == null ? null : pay.getCnt()));
        d.setPayAmount(pay == null ? BigDecimal.ZERO : bdz(pay.getAmt()));

        AggRow complaint = readMapper.aggComplaint(fromTs, toTs, oid);
        d.setComplaintCount(intOf(complaint == null ? null : complaint.getCnt()));
        AggRow overdue = readMapper.aggComplaintOverdue(fromTs, toTs, oid);
        d.setComplaintOverdue(intOf(overdue == null ? null : overdue.getCnt()));

        AggRow rating = readMapper.aggRating(fromTs, toTs, oid);
        d.setRatingCount(intOf(rating == null ? null : rating.getCnt()));
        d.setRatingSum(rating == null || rating.getAmt() == null ? 0 : rating.getAmt().intValue());

        AggRow alarm = readMapper.aggAlarm(fromTs, toTs, oid);
        d.setAlarmCount(intOf(alarm == null ? null : alarm.getCnt()));

        d.setCalcTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        return d;
    }

    /** 维度重建：一天四类维度，先清后写。 */
    private void rebuildDims(LocalDate day, long orgId) {
        dimMapper.deleteByDay(day, orgId);
        Long oid = orgId == 0L ? null : orgId;
        List<WaybillRow> waybills = readMapper.waybillsForDim(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay(), oid);
        if (waybills.isEmpty()) {
            return;
        }
        Map<String, StatDim> bucket = new LinkedHashMap<String, StatDim>();

        for (WaybillRow w : waybills) {
            String loadProvince = RegionParser.provinceOf(w.getLoadAddr());
            String unloadProvince = RegionParser.provinceOf(w.getUnloadAddr());
            accum(bucket, day, orgId, StatDim.DIM_LOAD_PROVINCE, loadProvince, loadProvince, w);
            accum(bucket, day, orgId, StatDim.DIM_UNLOAD_PROVINCE, unloadProvince, unloadProvince, w);
            if (w.getCargoName() != null && !w.getCargoName().trim().isEmpty()) {
                String cargo = w.getCargoName().trim();
                accum(bucket, day, orgId, StatDim.DIM_CARGO, cargo, cargo, w);
            }
            if (w.getDriverId() != null) {
                String key = String.valueOf(w.getDriverId());
                accum(bucket, day, orgId, StatDim.DIM_DRIVER, key,
                        w.getDriverName() == null ? "司机#" + key : w.getDriverName(), w);
            }
        }
        for (StatDim d : bucket.values()) {
            dimMapper.insert(d);
        }
    }

    private void accum(Map<String, StatDim> bucket, LocalDate day, long orgId,
                       String dimType, String dimKey, String dimLabel, WaybillRow w) {
        String composite = dimType + "|" + dimKey;
        StatDim d = bucket.get(composite);
        if (d == null) {
            d = new StatDim();
            d.setBizDate(day);
            d.setOrgId(orgId);
            d.setDimType(dimType);
            d.setDimKey(dimKey);
            d.setDimLabel(dimLabel);
            d.setWaybillCount(0);
            d.setAmount(BigDecimal.ZERO);
            d.setComplaintCount(0);
            d.setAlarmCount(0);
            bucket.put(composite, d);
        }
        d.setWaybillCount(d.getWaybillCount() + 1);
        d.setAmount(d.getAmount().add(bdz(w.getAmount())));
    }

    private List<StatDetailRow> filterByDim(List<StatDetailRow> rows, String dimType, String dimKey) {
        if (dimType == null || dimType.trim().isEmpty() || dimKey == null || dimKey.trim().isEmpty()) {
            return rows;
        }
        String type = dimType.trim().toUpperCase();
        String key = dimKey.trim();
        List<StatDetailRow> out = new ArrayList<StatDetailRow>();
        for (StatDetailRow r : rows) {
            boolean match;
            if (StatDim.DIM_LOAD_PROVINCE.equals(type)) {
                match = key.equals(RegionParser.provinceOf(r.getLoadAddr()));
            } else if (StatDim.DIM_UNLOAD_PROVINCE.equals(type)) {
                match = key.equals(RegionParser.provinceOf(r.getUnloadAddr()));
            } else if (StatDim.DIM_CARGO.equals(type)) {
                match = key.equals(r.getCargoName());
            } else if (StatDim.DIM_DRIVER.equals(type)) {
                match = r.getDriverId() != null && key.equals(String.valueOf(r.getDriverId()));
            } else {
                match = true;
            }
            if (match) {
                out.add(r);
            }
        }
        return out;
    }

    private List<StatDaily> queryDaily(LocalDate start, LocalDate end, long oid) {
        QueryWrapper<StatDaily> qw = new QueryWrapper<StatDaily>();
        qw.eq("org_id", oid).ge("biz_date", start).le("biz_date", end).orderByAsc("biz_date");
        return dailyMapper.selectList(qw);
    }

    private String periodOf(LocalDate date, String granularity) {
        if ("MONTH".equals(granularity)) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        if ("WEEK".equals(granularity)) {
            WeekFields wf = WeekFields.ISO;
            int week = date.get(wf.weekOfWeekBasedYear());
            return date.get(wf.weekBasedYear()) + "-W" + (week < 10 ? "0" + week : String.valueOf(week));
        }
        return date.format(DAY_FMT);
    }

    private void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        long days = to.toEpochDay() - from.toEpochDay() + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "统计区间不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private static long nz(Integer v) {
        return v == null ? 0L : v.longValue();
    }

    private static int intOf(Long v) {
        return v == null ? 0 : v.intValue();
    }

    private static BigDecimal bdz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
