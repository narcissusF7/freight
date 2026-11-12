package com.lukachangtong.freight.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.api.waybill.WaybillParty;
import com.lukachangtong.freight.api.waybill.WaybillPartyPort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.customer.dto.RatingReplyRequest;
import com.lukachangtong.freight.customer.dto.RatingSubmitRequest;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.mapper.CsRatingMapper;
import com.lukachangtong.freight.customer.service.CreditService;
import com.lukachangtong.freight.customer.service.RatingService;
import com.lukachangtong.freight.customer.support.CreditRule;
import com.lukachangtong.freight.customer.support.CustomerNo;
import com.lukachangtong.freight.customer.support.CustomerNotifier;
import com.lukachangtong.freight.customer.vo.RatingSummaryVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价实现。
 *
 * 两个容易被忽略的点：
 * 1. **评价资格按运单判定**：只有签收及之后的状态才能评。否则运输还在路上，
 *    货主一星、司机一星，评分体系第二天就没人信了；
 * 2. **重复评价靠唯一索引兜底**：应用层先查一次是为了给出友好提示，
 *    真正的拦截是 `uk_rating_waybill_direction`——两个请求同时通过查询时只有一条能插进去。
 */
@Service
public class RatingServiceImpl implements RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingServiceImpl.class);

    /** 可评价状态：签收及之后。RECEIPT_UPLOADED 之后只是财务流程，评价资格不受影响。 */
    private static final List<String> RATABLE_STATUS = Arrays.asList(
            "SIGNED", "RECEIPT_UPLOADED", "SETTLED", "REPORTED", "COMPLETED");

    @Resource
    private CsRatingMapper ratingMapper;
    @Resource
    private CreditService creditService;
    @Resource
    private CustomerNotifier notifier;
    @Resource
    private ObjectProvider<WaybillPartyPort> partyPortProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsRating submit(RatingSubmitRequest request) {
        if (request == null || request.getWaybillId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请指定要评价的运单");
        }
        if (request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            throw new BizException(ErrorCode.PARAM_INVALID, "总体星级只能是 1~5");
        }
        checkSubScores(request);

        WaybillPartyPort port = partyPortProvider.getIfAvailable();
        WaybillParty party = port == null ? null : port.load(request.getWaybillId());
        if (party == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在：" + request.getWaybillId());
        }
        if (!RATABLE_STATUS.contains(party.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "运单尚未签收（当前：" + party.getStatus() + "），暂不能评价");
        }

        String direction = upper(request.getDirection());
        if (direction == null) {
            direction = CsRating.DIR_SHIPPER_TO_DRIVER;
        }
        boolean shipperSide = CsRating.DIR_SHIPPER_TO_DRIVER.equals(direction);
        if (!shipperSide && !CsRating.DIR_DRIVER_TO_SHIPPER.equals(direction)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "评价方向不合法：" + request.getDirection());
        }

        // 评价人与被评价人由运单决定，不接受前端随意指定（否则可以给别人的运单刷差评）
        Long raterId = shipperSide ? party.getShipperId() : party.getDriverId();
        String raterType = shipperSide ? "SHIPPER" : "DRIVER";
        Long targetId = shipperSide ? party.getDriverId() : party.getShipperId();
        String targetType = shipperSide ? "DRIVER" : "SHIPPER";
        String raterName = shipperSide ? party.getShipperName() : party.getDriverName();
        String targetName = shipperSide ? party.getDriverName() : party.getShipperName();
        if (raterId == null || targetId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单缺少货主或司机信息，无法评价");
        }
        if (request.getRaterId() != null && !request.getRaterId().equals(raterId)) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "评价人与该运单不符：运单" + (shipperSide ? "货主" : "司机") + "为 " + raterId);
        }
        if (targetName == null) {
            // 货主档案没维护姓名时给个可读兜底，别把 "SHIPPER#1" 直接丢到界面上
            targetName = ("SHIPPER".equals(targetType) ? "货主#" : "司机#") + targetId;
        }

        CsRating existing = ratingMapper.selectOne(new QueryWrapper<CsRating>()
                .eq("waybill_id", party.getWaybillId()).eq("direction", direction).last("LIMIT 1"));
        if (existing != null) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT,
                    "该运单已评价过（" + existing.getRatingNo() + "），不能重复评价");
        }

        LocalDateTime now = LocalDateTime.now();
        CsRating r = new CsRating();
        r.setRatingNo(CustomerNo.rating());
        r.setOrgId(party.getOrgId());
        r.setWaybillId(party.getWaybillId());
        r.setWaybillNo(party.getWaybillNo());
        r.setDirection(direction);
        r.setRaterType(raterType);
        r.setRaterId(raterId);
        r.setRaterName(raterName);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setTargetName(targetName);
        r.setScore(request.getScore());
        r.setPunctuality(request.getPunctuality());
        r.setService(request.getService());
        r.setCargo(request.getCargo());
        r.setAttitude(request.getAttitude());
        r.setTags(request.getTags());
        r.setContent(trim(request.getContent()));
        r.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        r.setStatus(CsRating.STATUS_VALID);
        r.setCreateTime(now);
        r.setUpdateTime(now);
        try {
            ratingMapper.insert(r);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求都通过了前置查询，唯一索引挡住第二条
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "该运单已评价过，不能重复评价");
        }

        creditService.applyRating(r);

        if (CreditRule.isBad(r.getScore())) {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("ratingNo", r.getRatingNo());
            vars.put("targetName", targetName);
            vars.put("score", r.getScore());
            vars.put("content", r.getContent() == null ? "（未填写文字评价）" : r.getContent());
            vars.put("waybillNo", r.getWaybillNo());
            notifier.notifyOrg(r.getOrgId(), "RATING_BAD", r.getRatingNo(), vars);
        }
        log.info("评价提交 {} 运单={} 方向={} {}星 被评={}#{}",
                r.getRatingNo(), r.getWaybillNo(), direction, r.getScore(), targetType, targetId);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsRating reply(Long id, RatingReplyRequest request) {
        CsRating r = must(id);
        if (request == null || trim(request.getContent()) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请填写回复内容");
        }
        if (r.getReplyContent() != null) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "该评价已回复过，不能重复回复");
        }
        CsRating upd = new CsRating();
        upd.setId(id);
        upd.setReplyContent(trim(request.getContent()));
        upd.setReplyBy(request.getOperator());
        upd.setReplyTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        ratingMapper.updateById(upd);
        return ratingMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsRating hide(Long id, String reason, String operator) {
        CsRating r = must(id);
        if (CsRating.STATUS_HIDDEN.equals(r.getStatus())) {
            return r;
        }
        CsRating upd = new CsRating();
        upd.setId(id);
        upd.setStatus(CsRating.STATUS_HIDDEN);
        upd.setUpdateTime(LocalDateTime.now());
        ratingMapper.updateById(upd);
        // 隐藏不回退信用分：分是「当时发生过的评价」的结果，回退会让分数随运营动作漂移。
        // 确需纠错走信用人工调账（有原因、有流水、可审计）。
        log.info("评价隐藏 {} 原因={} 操作人={}", r.getRatingNo(), reason, operator);
        return ratingMapper.selectById(id);
    }

    @Override
    public List<CsRating> list(Long orgId, String direction, String targetType, Long targetId,
                               Integer scoreBelow, Integer limit) {
        QueryWrapper<CsRating> qw = new QueryWrapper<CsRating>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (trim(direction) != null) {
            qw.eq("direction", upper(direction));
        }
        if (trim(targetType) != null) {
            qw.eq("target_type", upper(targetType));
        }
        if (targetId != null) {
            qw.eq("target_id", targetId);
        }
        if (scoreBelow != null) {
            qw.le("score", scoreBelow);
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return ratingMapper.selectList(qw);
    }

    @Override
    public RatingSummaryVo summary(String targetType, Long targetId) {
        RatingSummaryVo vo = new RatingSummaryVo();
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        if (targetType == null || targetId == null) {
            return vo;
        }
        List<Map<String, Object>> rows = ratingMapper.countByScore(targetType.toUpperCase(), targetId);
        long total = 0;
        long sum = 0;
        long good = 0;
        long bad = 0;
        long[] dist = new long[5];
        for (Map<String, Object> row : rows) {
            int score = ((Number) row.get("score")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            if (score >= 1 && score <= 5) {
                dist[score - 1] += cnt;
            }
            total += cnt;
            sum += (long) score * cnt;
            if (score >= 4) {
                good += cnt;
            }
            if (score <= 2) {
                bad += cnt;
            }
        }
        vo.setCount(total);
        vo.setDistribution(dist);
        vo.setAvgScore(total == 0 ? 0 : round2((double) sum / total));
        vo.setGoodRate(total == 0 ? 0 : round4((double) good / total));
        vo.setBadCount(bad);
        return vo;
    }

    @Override
    public CsRating byWaybill(Long waybillId, String direction) {
        if (waybillId == null || direction == null) {
            return null;
        }
        return ratingMapper.selectOne(new QueryWrapper<CsRating>()
                .eq("waybill_id", waybillId)
                .eq("direction", direction.trim().toUpperCase())
                .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 内部

    private CsRating must(Long id) {
        CsRating r = ratingMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "评价不存在：" + id);
        }
        return r;
    }

    private void checkSubScores(RatingSubmitRequest request) {
        Integer[] subs = {request.getPunctuality(), request.getService(), request.getCargo(), request.getAttitude()};
        for (Integer s : subs) {
            if (s != null && (s < 1 || s > 5)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "分项评分只能是 1~5");
            }
        }
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String upper(String s) {
        String t = trim(s);
        return t == null ? null : t.toUpperCase();
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
