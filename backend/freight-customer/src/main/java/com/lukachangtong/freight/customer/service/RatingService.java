package com.lukachangtong.freight.customer.service;

import com.lukachangtong.freight.customer.dto.RatingReplyRequest;
import com.lukachangtong.freight.customer.dto.RatingSubmitRequest;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.vo.RatingSummaryVo;

import java.util.List;

/**
 * 双向星级评价：货主评司机、司机评货主。
 *
 * 两条资格约束：运单必须已签收（没干完活不能互相点评），一张运单每个方向只能评一次。
 */
public interface RatingService {

    CsRating submit(RatingSubmitRequest request);

    CsRating reply(Long id, RatingReplyRequest request);

    /** 违规内容隐藏（不删除，评价是纠纷证据）。 */
    CsRating hide(Long id, String reason, String operator);

    List<CsRating> list(Long orgId, String direction, String targetType, Long targetId,
                        Integer scoreBelow, Integer limit);

    RatingSummaryVo summary(String targetType, Long targetId);

    /**
     * 按运单 + 方向查已有评价，没有返回 null。
     * 司机端要区分「已评价 / 待评价」，不能靠异常来判断。
     */
    CsRating byWaybill(Long waybillId, String direction);
}
