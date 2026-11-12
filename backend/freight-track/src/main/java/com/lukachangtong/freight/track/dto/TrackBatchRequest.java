package com.lukachangtong.freight.track.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量上报轨迹（APP 本地队列攒批，建议 ≤100 点/请求）。
 * 服务端按 (waybillId, gpsTime) 幂等去重：重复提交不会产生重复点，也不会报错。
 */
public class TrackBatchRequest {

    private List<TrackPointDto> points = new ArrayList<>();

    public List<TrackPointDto> getPoints() { return points; }
    public void setPoints(List<TrackPointDto> v) { this.points = v; }
}
