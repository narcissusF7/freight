package com.lukachangtong.freight.track.service;

import com.lukachangtong.freight.track.entity.TrTrackAlarm;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 在途预警。
 *
 * 规则全部是「基于轨迹点的事后判定」，不做流式复杂事件处理：
 * 上报后按最近 24 小时窗口跑一次，人工也可对任意时间窗重跑；
 * 幂等键保证重跑不产生重复告警。
 */
public interface AlarmService {

    /**
     * 分析一个运单在时间窗内的轨迹，生成缺失的预警。
     *
     * @return 新增预警条数（已存在的不计）
     */
    int analyze(Long waybillId, LocalDateTime from, LocalDateTime to);

    /**
     * 上报后自动分析：异常不得影响轨迹入库，因此逐个运单 try/catch，失败只记日志。
     */
    void analyzeQuietly(Collection<Long> waybillIds);

    List<TrTrackAlarm> list(Long orgId, Long waybillId, Integer status, String alarmType, Integer limit);

    TrTrackAlarm handle(Long id, String handleBy, String handleRemark, Integer status);
}
