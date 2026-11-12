package com.lukachangtong.freight.track.service;

import com.lukachangtong.freight.track.dto.TrackPointDto;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import com.lukachangtong.freight.track.vo.TrackSummaryVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轨迹域服务。
 *
 * 采集与存储约定（详见大纲 M3）：
 * - 在途 ≤30s 或 ≥200m 打点，停驶 300s 心跳，关键节点强制打点；
 * - 服务端按 (waybillId, gpsTime) 幂等去重，定位精度 >100m 视为漂移直接丢弃；
 * - 查询永远带时间窗（走月分区裁剪），结果按最大点数抽稀后再下发前端回放。
 */
public interface TrackService {

    /**
     * 批量上报。
     *
     * @return 实际新增入库点数（重复点、漂移点不计）
     */
    int report(List<TrackPointDto> points);

    /**
     * 时间窗查询（升序）。时间窗为空则默认取该运单全部，但仍受 maxPoints 抽稀约束。
     */
    List<TrTrackPoint> query(Long waybillId, LocalDateTime from, LocalDateTime to, Integer maxPoints);

    /** 摘要：点数、里程、时长、均速/最高速、漂移剔除数。 */
    TrackSummaryVo summary(Long waybillId, LocalDateTime from, LocalDateTime to);
}
