package com.lukachangtong.freight.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.customer.dto.CreditAdjustRequest;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.entity.CsCredit;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.mapper.CsCreditMapper;
import com.lukachangtong.freight.customer.mapper.CsCreditSubjectMapper;
import com.lukachangtong.freight.customer.service.CreditService;
import com.lukachangtong.freight.customer.support.CreditRule;
import com.lukachangtong.freight.customer.vo.CreditChangeVo;
import com.lukachangtong.freight.customer.vo.CreditDetailVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信用评分实现。
 *
 * 写入顺序：先插流水（唯一索引挡重复），再原子加减分。
 * 反过来先改分再插流水，一旦流水插入冲突回滚，分已经涨上去了——这里宁可先失败。
 */
@Service
public class CreditServiceImpl implements CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditServiceImpl.class);

    @Resource
    private CsCreditMapper creditMapper;
    @Resource
    private CsCreditSubjectMapper subjectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVo applyRating(CsRating rating) {
        if (rating == null || rating.getTargetId() == null || rating.getScore() == null) {
            return new CreditChangeVo();
        }
        int star = rating.getScore();
        String bizType;
        int delta;
        int goodInc = 0;
        int neutralInc = 0;
        int badInc = 0;
        if (CreditRule.isGood(star)) {
            bizType = CsCredit.BIZ_RATING_GOOD;
            delta = CreditRule.DELTA_RATING_GOOD;
            goodInc = 1;
        } else if (CreditRule.isBad(star)) {
            bizType = CsCredit.BIZ_RATING_BAD;
            delta = CreditRule.DELTA_RATING_BAD;
            badInc = 1;
        } else {
            bizType = CsCredit.BIZ_RATING_NEUTRAL;
            delta = CreditRule.DELTA_RATING_NEUTRAL;
            neutralInc = 1;
        }
        Increment inc = new Increment();
        inc.totalInc = 1;
        inc.goodInc = goodInc;
        inc.neutralInc = neutralInc;
        inc.badInc = badInc;
        return applyInternal(rating.getTargetType(), rating.getTargetId(), rating.getTargetName(),
                bizType, rating.getRatingNo(), delta,
                "运单 " + rating.getWaybillNo() + " 评价 " + star + " 星", rating.getRaterName(), inc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVo applyComplaint(CsComplaint complaint) {
        if (complaint == null || complaint.getTargetId() == null) {
            return new CreditChangeVo();
        }
        Increment inc = new Increment();
        inc.complaintInc = 1;
        return applyInternal(complaint.getTargetType(), complaint.getTargetId(), complaint.getTargetName(),
                CsCredit.BIZ_COMPLAINT, complaint.getComplaintNo(),
                CreditRule.DELTA_COMPLAINT_CONFIRMED,
                "投诉成立：" + complaint.getComplaintType() + "（" + complaint.getComplaintNo() + "）",
                complaint.getUpdateBy(), inc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditChangeVo adjust(CreditAdjustRequest request) {
        if (request == null || request.getSubjectId() == null || request.getSubjectType() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "主体类型与主体 ID 不能为空");
        }
        if (request.getDelta() == null || request.getDelta() == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "调账分值不能为 0");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "人工调账必须写明原因");
        }
        // 调账单号带时间戳：允许对同一主体多次调账，但每次都是一条独立流水
        String bizNo = "ADJ" + LocalDateTime.now().toString();
        return applyInternal(request.getSubjectType().toUpperCase(), request.getSubjectId(),
                request.getSubjectName(), CsCredit.BIZ_ADJUST, bizNo,
                request.getDelta(), request.getReason(), request.getOperator(), new Increment());
    }

    @Override
    public CreditDetailVo detail(String subjectType, Long subjectId) {
        CreditDetailVo vo = new CreditDetailVo();
        if (subjectType == null || subjectId == null) {
            return vo;
        }
        CsCreditSubject subject = subjectMapper.selectBySubject(subjectType.toUpperCase(), subjectId);
        vo.setSubject(subject);
        QueryWrapper<CsCredit> qw = new QueryWrapper<>();
        qw.eq("subject_type", subjectType.toUpperCase()).eq("subject_id", subjectId)
                .orderByDesc("id").last("LIMIT 200");
        vo.setFlows(creditMapper.selectList(qw));
        return vo;
    }

    @Override
    public List<CsCreditSubject> ranking(String subjectType, String level, Integer limit) {
        QueryWrapper<CsCreditSubject> qw = new QueryWrapper<>();
        if (subjectType != null && !subjectType.trim().isEmpty()) {
            qw.eq("subject_type", subjectType.toUpperCase());
        }
        if (level != null && !level.trim().isEmpty()) {
            qw.eq("level", level.toUpperCase());
        }
        // 低分优先：风控先看需要干预的人，而不是给高分的人发奖
        qw.orderByAsc("score").orderByDesc("update_time");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return subjectMapper.selectList(qw);
    }

    // ------------------------------------------------------------------ 内部

    private CreditChangeVo applyInternal(String subjectType, Long subjectId, String subjectName,
                                         String bizType, String bizNo, int delta,
                                         String reason, String operator, Increment inc) {
        CreditChangeVo vo = new CreditChangeVo();
        vo.setSubjectType(subjectType);
        vo.setSubjectId(subjectId);
        vo.setBizType(bizType);
        vo.setBizNo(bizNo);

        CsCreditSubject subject = getOrCreate(subjectType, subjectId, subjectName);
        int before = subject.getScore() == null ? CreditRule.BASE_SCORE : subject.getScore();
        String levelBefore = CreditRule.levelOf(before);
        vo.setScoreBefore(before);
        vo.setLevelBefore(levelBefore);

        CsCredit flow = new CsCredit();
        flow.setSubjectType(subjectType);
        flow.setSubjectId(subjectId);
        flow.setSubjectName(subjectName == null ? subject.getSubjectName() : subjectName);
        flow.setBizType(bizType);
        flow.setBizNo(bizNo);
        flow.setDelta(delta);
        flow.setScoreBefore(before);
        flow.setScoreAfter(CreditRule.clamp(before + delta));
        flow.setLevelBefore(levelBefore);
        flow.setLevelAfter(CreditRule.levelOf(flow.getScoreAfter()));
        flow.setReason(truncate(reason, 255));
        flow.setCreateBy(operator);
        flow.setCreateTime(LocalDateTime.now());
        try {
            creditMapper.insert(flow);
        } catch (DuplicateKeyException e) {
            // 幂等命中：同一事件已计过分。返回已有结果，不重复改分。
            CsCredit existing = creditMapper.selectOne(new QueryWrapper<CsCredit>()
                    .eq("subject_type", subjectType).eq("subject_id", subjectId)
                    .eq("biz_type", bizType).eq("biz_no", bizNo).last("LIMIT 1"));
            vo.setChanged(false);
            if (existing != null) {
                vo.setScoreBefore(existing.getScoreBefore());
                vo.setScoreAfter(existing.getScoreAfter());
                vo.setLevelBefore(existing.getLevelBefore());
                vo.setLevelAfter(existing.getLevelAfter());
                vo.setDelta(existing.getDelta());
            }
            log.info("信用计分幂等命中：{}:{} {}:{}", subjectType, subjectId, bizType, bizNo);
            return vo;
        }

        subjectMapper.applyDelta(subject.getId(), delta,
                inc.goodInc, inc.neutralInc, inc.badInc, inc.complaintInc, inc.totalInc);

        CsCreditSubject latest = subjectMapper.selectById(subject.getId());
        vo.setChanged(true);
        vo.setSubjectName(latest == null ? flow.getSubjectName() : latest.getSubjectName());
        vo.setScoreAfter(latest == null ? flow.getScoreAfter() : latest.getScore());
        vo.setLevelAfter(latest == null ? flow.getLevelAfter() : latest.getLevel());
        vo.setDelta(delta);
        return vo;
    }

    private CsCreditSubject getOrCreate(String subjectType, Long subjectId, String subjectName) {
        CsCreditSubject subject = subjectMapper.selectBySubject(subjectType, subjectId);
        if (subject != null) {
            return subject;
        }
        CsCreditSubject created = new CsCreditSubject();
        created.setSubjectType(subjectType);
        created.setSubjectId(subjectId);
        created.setSubjectName(subjectName);
        created.setScore(CreditRule.BASE_SCORE);
        created.setLevel(CreditRule.levelOf(CreditRule.BASE_SCORE));
        created.setGoodCount(0);
        created.setNeutralCount(0);
        created.setBadCount(0);
        created.setComplaintCount(0);
        created.setTotalCount(0);
        created.setCreateTime(LocalDateTime.now());
        created.setUpdateTime(LocalDateTime.now());
        created.setVersion(0);
        try {
            subjectMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发首次创建：读回已存在的那条
            return subjectMapper.selectBySubject(subjectType, subjectId);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 计数增量：一次事件可能同时影响「总数」与「好/中/差评数」。 */
    private static class Increment {
        private int goodInc;
        private int neutralInc;
        private int badInc;
        private int complaintInc;
        private int totalInc;
    }
}
