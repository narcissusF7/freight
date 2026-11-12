package com.lukachangtong.freight.report.service;

import com.lukachangtong.freight.report.entity.RpReportRecord;
import com.lukachangtong.freight.report.vo.ReportStatVo;

import java.util.List;

/**
 * 监管上报服务。
 *
 * 三条硬约束（对齐大纲 1.2）：
 * 1. **上报完整性**：业务完成后必须上报，靠扫描任务兜底，不靠界面按钮；
 * 2. **幂等**：message_reference_number 唯一 + 成功记录判重，重复扫描不重复上报；
 * 3. **补偿**：失败进 rp_retry，指数退避重试，超过上限标记 ABANDONED 交人工。
 */
public interface ReportService {

    /** 扫描所有待上报数据（运单/资金流水/车辆/驾驶员）并上报，返回本次处理条数。 */
    int scanAndReport(int limit);

    /** 补偿重试：投递到点的失败记录。返回处理条数。 */
    int retryFailed(int limit);

    /** 手动触发某条业务数据的上报（运单/资金流水/车辆/驾驶员任一）。 */
    RpReportRecord reportOne(String docType, String bizNo, String operator);

    /** 手动重试某条失败的上报记录。 */
    RpReportRecord retryRecord(Long recordId, String operator);

    /** 上报记录列表（看板）。 */
    List<RpReportRecord> list(Long orgId, String docType, String status, Integer limit);

    /** 看板统计。 */
    ReportStatVo stats(Long orgId);
}
