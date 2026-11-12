package com.lukachangtong.freight.stat.service;

import com.lukachangtong.freight.stat.entity.StatDaily;
import com.lukachangtong.freight.stat.vo.StatDetailRow;
import com.lukachangtong.freight.stat.vo.StatDimVo;
import com.lukachangtong.freight.stat.vo.StatOverviewVo;
import com.lukachangtong.freight.stat.vo.StatTrendVo;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计中心：预聚合 → 趋势 → 维度分布 → 数据穿透。
 *
 * 统计域是**只读**的：它不产生业务数据，只回答「过去发生了什么」。
 * 所以这里没有写业务表的接口，唯一的写操作是 `rebuild` 重算自己的预聚合表。
 */
public interface StatService {

    /**
     * 重算指定日期区间的日汇总与维度聚合（可重复执行，结果覆盖）。
     *
     * @param orgId null 表示全平台口径（落 org_id = 0）
     * @return 实际重算的天数
     */
    int rebuild(LocalDate from, LocalDate to, Long orgId, String operator);

    /**
     * 总览卡片。若区间内没有预聚合数据，会自动补算（上限 31 天），
     * 避免「刚部署完打开统计页一片 0」。
     */
    StatOverviewVo overview(LocalDate from, LocalDate to, Long orgId);

    /** 趋势：granularity = DAY / WEEK / MONTH。 */
    List<StatTrendVo> trend(LocalDate from, LocalDate to, Long orgId, String granularity);

    /** 维度分布：LOAD_PROVINCE / UNLOAD_PROVINCE / CARGO / DRIVER。 */
    List<StatDimVo> dims(LocalDate from, LocalDate to, Long orgId, String dimType, Integer limit);

    /**
     * 数据穿透：把汇总数字展开成具体单据。
     *
     * @param metric  StatMetric 名称
     * @param dimType 可选，配合 dimKey 做维度过滤（省域/货物/司机）
     */
    List<StatDetailRow> drill(LocalDate from, LocalDate to, Long orgId, String metric,
                              String dimType, String dimKey, Integer limit);

    /** 原始日汇总（排障与测试用）。 */
    List<StatDaily> daily(LocalDate from, LocalDate to, Long orgId);
}
