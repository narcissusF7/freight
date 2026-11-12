package com.lukachangtong.freight.customer.service;

import com.lukachangtong.freight.customer.dto.ComplaintAssignRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCloseRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCreateRequest;
import com.lukachangtong.freight.customer.dto.ComplaintFollowRequest;
import com.lukachangtong.freight.customer.dto.ComplaintRejectRequest;
import com.lukachangtong.freight.customer.dto.ComplaintResolveRequest;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.vo.ComplaintDetailVo;
import com.lukachangtong.freight.customer.vo.ComplaintStatVo;

import java.util.List;

/**
 * 投诉受理：建单 → 分派 → 处理 → 办结（或驳回），全程 24 小时时限。
 *
 * 状态机：
 * <pre>
 * PENDING ──assign──▶ PROCESSING ──resolve──▶ RESOLVED ──close──▶ CLOSED
 *    │                    │
 *    └──────reject────────┴──▶ REJECTED
 * </pre>
 * 「RESOLVED」与「CLOSED」分开是刻意的：处理完不等于投诉人认可，
 * 中间这段时间用来收集满意度、必要时二次处理，而不是一点「处理完成」就销单。
 */
public interface ComplaintService {

    CsComplaint create(ComplaintCreateRequest request);

    ComplaintDetailVo assign(Long id, ComplaintAssignRequest request);

    /** 跟进记录（不改状态）。 */
    ComplaintDetailVo follow(Long id, ComplaintFollowRequest request);

    ComplaintDetailVo resolve(Long id, ComplaintResolveRequest request);

    ComplaintDetailVo close(Long id, ComplaintCloseRequest request);

    ComplaintDetailVo reject(Long id, ComplaintRejectRequest request);

    ComplaintDetailVo detail(Long id);

    List<CsComplaint> list(Long orgId, String status, String complaintType, String targetType,
                           Long targetId, Boolean overdue, Integer limit);

    ComplaintStatVo stats(Long orgId);

    /**
     * 逾期扫描：把超过 24h 未办结的投诉打标、升级并通知。
     * 幂等：`overdue_flag` 已为 1 的不再重复升级，重复跑任务不会刷屏。
     *
     * @return 本次新升级的条数
     */
    int scanOverdue(String operator);
}
