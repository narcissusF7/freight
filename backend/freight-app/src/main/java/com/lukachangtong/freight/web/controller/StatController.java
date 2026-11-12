package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.stat.entity.StatDaily;
import com.lukachangtong.freight.stat.service.StatService;
import com.lukachangtong.freight.stat.support.StatMetric;
import com.lukachangtong.freight.stat.vo.StatDetailRow;
import com.lukachangtong.freight.stat.vo.StatDimVo;
import com.lukachangtong.freight.stat.vo.StatOverviewVo;
import com.lukachangtong.freight.stat.vo.StatTrendVo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计中心：总览 / 趋势 / 维度分布 / 数据穿透 / 重算。
 *
 * 所有查询都带 from/to，默认区间由服务端兜底（不传不会报错，返回最近 7 天/30 天），
 * 前端切页面时不必先算好日期。
 */
@RestController
@RequestMapping("/api/stat")
public class StatController {

    @Resource
    private StatService statService;

    @GetMapping("/overview")
    public Result<StatOverviewVo> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId) {
        return Result.ok(statService.overview(from, to, orgId));
    }

    @GetMapping("/trend")
    public Result<List<StatTrendVo>> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false, defaultValue = "DAY") String granularity) {
        return Result.ok(statService.trend(from, to, orgId, granularity));
    }

    @GetMapping("/dims")
    public Result<List<StatDimVo>> dims(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false, defaultValue = "LOAD_PROVINCE") String dimType,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(statService.dims(from, to, orgId, dimType, limit));
    }

    @GetMapping("/drill")
    public Result<List<StatDetailRow>> drill(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId,
            @RequestParam String metric,
            @RequestParam(required = false) String dimType,
            @RequestParam(required = false) String dimKey,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(statService.drill(from, to, orgId, metric, dimType, dimKey, limit));
    }

    @PostMapping("/rebuild")
    public Result<Integer> rebuild(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String operator) {
        return Result.ok(statService.rebuild(from, to, orgId, operator));
    }

    @GetMapping("/daily")
    public Result<List<StatDaily>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long orgId) {
        return Result.ok(statService.daily(from, to, orgId));
    }

    /** 指标清单：前端下拉框与图例用它，避免前端硬编码中文与 key 对不上。 */
    @GetMapping("/metrics")
    public Result<List<Map<String, String>>> metrics() {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (StatMetric m : StatMetric.values()) {
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("code", m.name());
            item.put("label", m.getLabel());
            item.put("unit", m.getUnit());
            item.put("source", m.getSource());
            list.add(item);
        }
        return Result.ok(list);
    }
}
