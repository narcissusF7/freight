package com.lukachangtong.freight.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.api.waybill.WaybillParty;
import com.lukachangtong.freight.api.waybill.WaybillPartyPort;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.customer.dto.ComplaintAssignRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCloseRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCreateRequest;
import com.lukachangtong.freight.customer.dto.ComplaintFollowRequest;
import com.lukachangtong.freight.customer.dto.ComplaintRejectRequest;
import com.lukachangtong.freight.customer.dto.ComplaintResolveRequest;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.entity.CsHandle;
import com.lukachangtong.freight.customer.mapper.CsComplaintMapper;
import com.lukachangtong.freight.customer.mapper.CsHandleMapper;
import com.lukachangtong.freight.customer.service.ComplaintService;
import com.lukachangtong.freight.customer.service.CreditService;
import com.lukachangtong.freight.customer.support.ComplaintDict;
import com.lukachangtong.freight.customer.support.CustomerNo;
import com.lukachangtong.freight.customer.support.CustomerNotifier;
import com.lukachangtong.freight.customer.vo.ComplaintDetailVo;
import com.lukachangtong.freight.customer.vo.ComplaintStatVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 投诉实现。
 *
 * 三条关键约束：
 * 1. **时限落库**：`deadline_time = 受理 + 24h` 在建单时写死，逾期扫描按它索引，
 *    不在查询时用 `create_time + 24h` 现算（现算没法建索引，也没法解释「当时到底算不算逾期」）；
 * 2. **责任成立才扣信用**：`liable` 决定是否调 {@link CreditService}，驳回不扣分；
 * 3. **处理过程只追加**：每次状态变化都写 `cs_handle`，投诉人质疑「你们根本没联系我」时能自证。
 */
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);

    /** 监管口径：24 小时内处理并反馈 */
    private static final int SLA_HOURS = 24;
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private CsComplaintMapper complaintMapper;
    @Resource
    private CsHandleMapper handleMapper;
    @Resource
    private CreditService creditService;
    @Resource
    private CustomerNotifier notifier;
    /** 运单端口可能不存在（单独跑客服模块时），用 Provider 兜底 */
    @Resource
    private ObjectProvider<WaybillPartyPort> partyPortProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsComplaint create(ComplaintCreateRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "投诉内容不能为空");
        }
        String content = trim(request.getContent());
        if (content == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请填写投诉内容");
        }
        String type = upper(request.getComplaintType());
        if (!ComplaintDict.validType(type)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "投诉类型不合法：" + request.getComplaintType());
        }

        WaybillParty party = null;
        WaybillPartyPort port = partyPortProvider.getIfAvailable();
        if (request.getWaybillId() != null) {
            party = port == null ? null : port.load(request.getWaybillId());
            if (party == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "关联运单不存在：" + request.getWaybillId());
            }
        }

        String targetType = upper(request.getTargetType());
        if (targetType == null) {
            targetType = CsComplaint.TARGET_DRIVER;
        }
        Long targetId = request.getTargetId();
        String targetName = trim(request.getTargetName());
        if (targetId == null && party != null) {
            if (CsComplaint.TARGET_DRIVER.equals(targetType)) {
                targetId = party.getDriverId();
                targetName = targetName == null ? party.getDriverName() : targetName;
            } else if (CsComplaint.TARGET_SHIPPER.equals(targetType)) {
                targetId = party.getShipperId();
                targetName = targetName == null ? party.getShipperName() : targetName;
            }
        }
        if (targetId == null && !CsComplaint.TARGET_PLATFORM.equals(targetType)) {
            // 平台投诉（投诉平台本身）才允许没有主体；否则必须落到具体人
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "请指定被投诉对象，或关联运单以便按运单识别承运方");
        }
        if (targetName == null) {
            targetName = CsComplaint.TARGET_PLATFORM.equals(targetType) ? "平台"
                    : (CsComplaint.TARGET_SHIPPER.equals(targetType) ? "货主#" : "司机#") + targetId;
        }

        Long orgId = request.getOrgId() != null ? request.getOrgId()
                : (party == null ? null : party.getOrgId());
        if (orgId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请指定所属机构（未关联运单时必填）");
        }

        LocalDateTime now = LocalDateTime.now();
        CsComplaint c = new CsComplaint();
        c.setComplaintNo(CustomerNo.complaint());
        c.setOrgId(orgId);
        c.setSource(upper(request.getSource()) == null ? "WEB" : upper(request.getSource()));
        c.setComplainantType(upper(request.getComplainantType()) == null
                ? "OTHER" : upper(request.getComplainantType()));
        c.setComplainantId(request.getComplainantId());
        c.setComplainantName(trim(request.getComplainantName()) == null
                ? defaultComplainantName(party, c.getComplainantType()) : trim(request.getComplainantName()));
        c.setComplainantPhone(trim(request.getComplainantPhone()));
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        c.setTargetName(targetName);
        c.setWaybillId(request.getWaybillId());
        c.setWaybillNo(party == null ? null : party.getWaybillNo());
        c.setComplaintType(type);
        c.setLevel(CsComplaint.LEVEL_URGENT.equals(upper(request.getLevel()))
                ? CsComplaint.LEVEL_URGENT : CsComplaint.LEVEL_NORMAL);
        c.setContent(content);
        c.setImageUrls(request.getImageUrls());
        c.setStatus(CsComplaint.STATUS_PENDING);
        c.setAcceptTime(now);
        c.setDeadlineTime(now.plusHours(SLA_HOURS));
        c.setLiable(0);
        c.setOverdueFlag(0);
        c.setEscalateLevel(0);
        c.setCreateBy(request.getOperator());
        c.setCreateTime(now);
        c.setUpdateBy(request.getOperator());
        c.setUpdateTime(now);
        complaintMapper.insert(c);

        appendHandle(c.getId(), CsHandle.TYPE_CREATE,
                "投诉建单：" + ComplaintDict.typeLabel(type) + "（" + ComplaintDict.levelLabel(c.getLevel()) + "），时限 "
                        + c.getDeadlineTime().format(MINUTE), null, request.getOperator());

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("complaintNo", c.getComplaintNo());
        vars.put("complainantName", c.getComplainantName());
        vars.put("targetName", c.getTargetName());
        vars.put("complaintType", ComplaintDict.typeLabel(type));
        vars.put("level", ComplaintDict.levelLabel(c.getLevel()));
        vars.put("deadline", c.getDeadlineTime().format(MINUTE));
        notifier.notifyOrg(orgId, "COMPLAINT_NEW", c.getComplaintNo(), vars);

        log.info("投诉建单 {} 类型={} 被诉={}#{} 截止={}",
                c.getComplaintNo(), type, targetType, targetId, c.getDeadlineTime());
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintDetailVo assign(Long id, ComplaintAssignRequest request) {
        CsComplaint c = must(id);
        requireStatus(c, CsComplaint.STATUS_PENDING, "分派");
        String assignTo = request == null ? null : trim(request.getAssignTo());
        if (assignTo == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请指定处理人");
        }
        LocalDateTime now = LocalDateTime.now();
        CsComplaint upd = new CsComplaint();
        upd.setId(id);
        upd.setStatus(CsComplaint.STATUS_PROCESSING);
        upd.setAssignTo(assignTo);
        upd.setAssignTime(now);
        upd.setUpdateBy(request.getOperator());
        upd.setUpdateTime(now);
        complaintMapper.updateById(upd);

        appendHandle(id, CsHandle.TYPE_ASSIGN,
                "受理分派给 " + assignTo + (trim(request.getRemark()) == null ? "" : "：" + trim(request.getRemark())),
                null, request.getOperator());
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintDetailVo follow(Long id, ComplaintFollowRequest request) {
        CsComplaint c = must(id);
        if (CsComplaint.STATUS_CLOSED.equals(c.getStatus()) || CsComplaint.STATUS_REJECTED.equals(c.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "投诉已结束，不能再追加跟进记录");
        }
        if (request == null || trim(request.getContent()) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请填写跟进内容");
        }
        appendHandle(id, CsHandle.TYPE_FOLLOW, trim(request.getContent()),
                request.getImageUrls(), request.getOperator());
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintDetailVo resolve(Long id, ComplaintResolveRequest request) {
        CsComplaint c = must(id);
        requireStatus(c, CsComplaint.STATUS_PROCESSING, "处理");
        String result = request == null ? null : trim(request.getResult());
        if (result == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请填写处理结论（会作为反馈内容发给投诉人）");
        }
        boolean liable = request.getLiable() != null && request.getLiable();
        LocalDateTime now = LocalDateTime.now();

        CsComplaint upd = new CsComplaint();
        upd.setId(id);
        upd.setStatus(CsComplaint.STATUS_RESOLVED);
        upd.setResolvedTime(now);
        upd.setResolveResult(result);
        upd.setLiable(liable ? 1 : 0);
        upd.setUpdateBy(request.getOperator());
        upd.setUpdateTime(now);
        complaintMapper.updateById(upd);

        appendHandle(id, CsHandle.TYPE_RESOLVE,
                result + (liable ? "（责任成立，扣信用分）" : "（责任不成立，不扣分）"),
                null, request.getOperator());

        // 只有责任成立才动信用分；同一投诉单号在流水里有唯一键，重复处理不会扣两次
        if (liable) {
            c.setStatus(CsComplaint.STATUS_RESOLVED);
            c.setLiable(1);
            c.setUpdateBy(request.getOperator());
            creditService.applyComplaint(c);
        }

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("complaintNo", c.getComplaintNo());
        vars.put("result", result);
        vars.put("satisfaction", "-");
        notifier.notifyOrg(c.getOrgId(), "COMPLAINT_RESOLVED", c.getComplaintNo() + ":RESOLVED", vars);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintDetailVo close(Long id, ComplaintCloseRequest request) {
        CsComplaint c = must(id);
        requireStatus(c, CsComplaint.STATUS_RESOLVED, "办结");
        Integer satisfaction = request == null ? null : request.getSatisfaction();
        if (satisfaction != null && (satisfaction < 1 || satisfaction > 5)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "满意度只能是 1~5 星");
        }
        LocalDateTime now = LocalDateTime.now();
        CsComplaint upd = new CsComplaint();
        upd.setId(id);
        upd.setStatus(CsComplaint.STATUS_CLOSED);
        upd.setCloseTime(now);
        upd.setSatisfaction(satisfaction);
        upd.setCloseRemark(request == null ? null : trim(request.getRemark()));
        upd.setUpdateBy(request == null ? null : request.getOperator());
        upd.setUpdateTime(now);
        complaintMapper.updateById(upd);

        appendHandle(id, CsHandle.TYPE_CLOSE,
                "投诉办结" + (satisfaction == null ? "" : "，满意度 " + satisfaction + " 星"),
                null, request == null ? null : request.getOperator());
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintDetailVo reject(Long id, ComplaintRejectRequest request) {
        CsComplaint c = must(id);
        if (!CsComplaint.STATUS_PENDING.equals(c.getStatus())
                && !CsComplaint.STATUS_PROCESSING.equals(c.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "只有待受理/处理中的投诉可以驳回，当前：" + ComplaintDict.statusLabel(c.getStatus()));
        }
        String reason = request == null ? null : trim(request.getReason());
        if (reason == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请填写驳回原因");
        }
        LocalDateTime now = LocalDateTime.now();
        CsComplaint upd = new CsComplaint();
        upd.setId(id);
        upd.setStatus(CsComplaint.STATUS_REJECTED);
        upd.setResolveResult(reason);
        upd.setCloseTime(now);
        upd.setLiable(0);
        upd.setUpdateBy(request.getOperator());
        upd.setUpdateTime(now);
        complaintMapper.updateById(upd);

        appendHandle(id, CsHandle.TYPE_REJECT, "投诉驳回：" + reason, null, request.getOperator());
        return detail(id);
    }

    @Override
    public ComplaintDetailVo detail(Long id) {
        CsComplaint c = must(id);
        ComplaintDetailVo vo = new ComplaintDetailVo();
        vo.setComplaint(c);
        QueryWrapper<CsHandle> qw = new QueryWrapper<CsHandle>();
        qw.eq("complaint_id", id).orderByAsc("id");
        vo.setHandles(handleMapper.selectList(qw));

        LocalDateTime baseline = c.getResolvedTime() != null ? c.getResolvedTime() : LocalDateTime.now();
        long remainMinutes = Duration.between(baseline, c.getDeadlineTime()).toMinutes();
        vo.setRemainHours(remainMinutes / 60);
        boolean breached = c.getResolvedTime() != null
                ? c.getResolvedTime().isAfter(c.getDeadlineTime())
                : LocalDateTime.now().isAfter(c.getDeadlineTime());
        vo.setSlaBreached(breached);
        return vo;
    }

    @Override
    public List<CsComplaint> list(Long orgId, String status, String complaintType, String targetType,
                                  Long targetId, Boolean overdue, Integer limit) {
        QueryWrapper<CsComplaint> qw = new QueryWrapper<CsComplaint>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (trim(status) != null) {
            qw.eq("status", upper(status));
        }
        if (trim(complaintType) != null) {
            qw.eq("complaint_type", upper(complaintType));
        }
        if (trim(targetType) != null) {
            qw.eq("target_type", upper(targetType));
        }
        if (targetId != null) {
            qw.eq("target_id", targetId);
        }
        if (Boolean.TRUE.equals(overdue)) {
            qw.eq("overdue_flag", 1);
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return complaintMapper.selectList(qw);
    }

    @Override
    public ComplaintStatVo stats(Long orgId) {
        ComplaintStatVo vo = new ComplaintStatVo();
        vo.setTotal(count(orgId, null, null));
        vo.setPending(count(orgId, CsComplaint.STATUS_PENDING, null));
        vo.setProcessing(count(orgId, CsComplaint.STATUS_PROCESSING, null));
        vo.setResolved(count(orgId, CsComplaint.STATUS_RESOLVED, null));
        vo.setClosed(count(orgId, CsComplaint.STATUS_CLOSED, null));
        vo.setRejected(count(orgId, CsComplaint.STATUS_REJECTED, null));

        QueryWrapper<CsComplaint> overdueQw = new QueryWrapper<CsComplaint>();
        if (orgId != null) {
            overdueQw.eq("org_id", orgId);
        }
        overdueQw.in("status", CsComplaint.STATUS_PENDING, CsComplaint.STATUS_PROCESSING)
                .lt("deadline_time", LocalDateTime.now());
        vo.setOverdue(complaintMapper.selectCount(overdueQw));

        QueryWrapper<CsComplaint> todayQw = new QueryWrapper<CsComplaint>();
        if (orgId != null) {
            todayQw.eq("org_id", orgId);
        }
        todayQw.ge("create_time", LocalDate.now().atStartOfDay());
        vo.setTodayNew(complaintMapper.selectCount(todayQw));

        long handled = complaintMapper.countHandled(orgId);
        long inSla = complaintMapper.countHandledInSla(orgId);
        vo.setClosedInSla(inSla);
        vo.setOnTimeRate(handled == 0 ? 0 : round4((double) inSla / handled));
        vo.setAvgHandleHours(round2(complaintMapper.avgHandleMinutes(orgId) / 60.0));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdue(String operator) {
        QueryWrapper<CsComplaint> qw = new QueryWrapper<CsComplaint>();
        qw.in("status", CsComplaint.STATUS_PENDING, CsComplaint.STATUS_PROCESSING)
                .eq("overdue_flag", 0)
                .lt("deadline_time", LocalDateTime.now());
        List<CsComplaint> list = complaintMapper.selectList(qw);
        int n = 0;
        for (CsComplaint c : list) {
            CsComplaint upd = new CsComplaint();
            upd.setId(c.getId());
            upd.setOverdueFlag(1);
            upd.setEscalateLevel((c.getEscalateLevel() == null ? 0 : c.getEscalateLevel()) + 1);
            upd.setUpdateBy(operator == null ? "SYSTEM" : operator);
            upd.setUpdateTime(LocalDateTime.now());
            complaintMapper.updateById(upd);

            appendHandle(c.getId(), CsHandle.TYPE_ESCALATE,
                    "超过 24 小时时限仍未办结，自动升级（原截止 "
                            + c.getDeadlineTime().format(MINUTE) + "）", null, operator);

            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("complaintNo", c.getComplaintNo());
            vars.put("deadline", c.getDeadlineTime().format(MINUTE));
            notifier.notifyOrg(c.getOrgId(), "COMPLAINT_OVERDUE",
                    c.getComplaintNo() + ":OVERDUE", vars);
            n++;
        }
        if (n > 0) {
            log.warn("投诉逾期扫描：本次升级 {} 条", n);
        }
        return n;
    }

    // ------------------------------------------------------------------ 内部

    private CsComplaint must(Long id) {
        CsComplaint c = complaintMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "投诉不存在：" + id);
        }
        return c;
    }

    private void requireStatus(CsComplaint c, String expected, String action) {
        if (!expected.equals(c.getStatus())) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "投诉当前状态「" + ComplaintDict.statusLabel(c.getStatus()) + "」不能" + action);
        }
    }

    private void appendHandle(Long complaintId, String type, String content, String images, String operator) {
        CsHandle h = new CsHandle();
        h.setComplaintId(complaintId);
        h.setHandleType(type);
        h.setContent(truncate(content, 1000));
        h.setImageUrls(images);
        h.setHandleBy(operator);
        h.setHandleTime(LocalDateTime.now());
        handleMapper.insert(h);
    }

    private long count(Long orgId, String status, String type) {
        QueryWrapper<CsComplaint> qw = new QueryWrapper<CsComplaint>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        if (type != null) {
            qw.eq("complaint_type", type);
        }
        return complaintMapper.selectCount(qw);
    }

    private String defaultComplainantName(WaybillParty party, String complainantType) {
        if (party == null) {
            return "匿名";
        }
        if ("DRIVER".equals(complainantType)) {
            return party.getDriverName();
        }
        if ("SHIPPER".equals(complainantType)) {
            return party.getShipperName();
        }
        return "匿名";
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

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
