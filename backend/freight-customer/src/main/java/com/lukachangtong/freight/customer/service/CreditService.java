package com.lukachangtong.freight.customer.service;

import com.lukachangtong.freight.customer.dto.CreditAdjustRequest;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.vo.CreditChangeVo;
import com.lukachangtong.freight.customer.vo.CreditDetailVo;

import java.util.List;

/**
 * 信用评分。
 *
 * 唯一写入口：任何加减分都必须走这里，因为只有这里会同时做三件事——
 * 写流水、更新快照、按 `(主体, 事件)` 幂等。别处直接改 `cs_credit_subject.score`
 * 会让流水与分值对不上，信用分就失去可解释性（司机申诉时无法回答「为什么扣我这 5 分」）。
 */
public interface CreditService {

    /** 评价计分：好评 +2 / 中评 0 / 差评 -5，并累加评价计数。 */
    CreditChangeVo applyRating(CsRating rating);

    /** 投诉计分：责任成立 -10（同一投诉只扣一次）。 */
    CreditChangeVo applyComplaint(CsComplaint complaint);

    /** 人工调账：必须写原因，同样留流水。 */
    CreditChangeVo adjust(CreditAdjustRequest request);

    CreditDetailVo detail(String subjectType, Long subjectId);

    /** 信用排行：低分优先（风控视角，先看需要干预的）。 */
    List<CsCreditSubject> ranking(String subjectType, String level, Integer limit);
}
