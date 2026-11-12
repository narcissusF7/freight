package com.lukachangtong.freight.waybill.service;

import com.lukachangtong.freight.waybill.vo.MatchVo;

import java.util.List;

/** 车货匹配：按货源要求给已审核通过的司机+车辆打分排序，输出候选建议。 */
public interface MatchingService {

    /**
     * @param goodsSourceId 货源 id
     * @param limit         返回条数上限（默认 5）
     */
    List<MatchVo> match(Long goodsSourceId, Integer limit);
}
