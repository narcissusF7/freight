package com.lukachangtong.freight.track.service;

import com.lukachangtong.freight.track.dto.FenceSaveRequest;
import com.lukachangtong.freight.track.entity.TrFence;

import java.util.List;

/** 电子围栏维护。 */
public interface FenceService {

    List<TrFence> list(Long orgId, String fenceType, Integer enabled);

    TrFence get(Long id);

    Long create(FenceSaveRequest request);

    void update(Long id, FenceSaveRequest request);

    void delete(Long id);

    /** 预警分析用：只取启用的围栏。 */
    List<TrFence> listEnabled(Long orgId);
}
