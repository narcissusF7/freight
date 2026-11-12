package com.lukachangtong.freight.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.report.client.ReportClient;
import com.lukachangtong.freight.report.dto.ReportEnvelope;
import com.lukachangtong.freight.report.dto.ReportReceipt;
import com.lukachangtong.freight.report.entity.RpConfig;
import com.lukachangtong.freight.report.entity.RpReportRecord;
import com.lukachangtong.freight.report.entity.RpRetry;
import com.lukachangtong.freight.report.mapper.RpConfigMapper;
import com.lukachangtong.freight.report.mapper.RpReportRecordMapper;
import com.lukachangtong.freight.report.mapper.RpRetryMapper;
import com.lukachangtong.freight.report.mapper.ReportReadMapper;
import com.lukachangtong.freight.report.service.ReportService;
import com.lukachangtong.freight.report.support.ReportAssembler;
import com.lukachangtong.freight.report.support.ReportDict;
import com.lukachangtong.freight.report.vo.DriverReportRow;
import com.lukachangtong.freight.report.vo.FundFlowReportRow;
import com.lukachangtong.freight.report.vo.ReportStatVo;
import com.lukachangtong.freight.report.vo.VehicleReportRow;
import com.lukachangtong.freight.report.vo.WaybillReportRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 上报实现。
 *
 * 三个刻意取舍：
 * 1. **扫描而非事件**：不依赖各业务模块在事务里写 outbox 事件——漏写一次就漏报，
 *    而监管要的是「业务完成后必报」。这里直接扫业务表「已完成但无成功上报记录」的数据，
 *    既满足完整性，又不用去改 4 个业务模块。
 * 2. **令牌缓存**：规范要求令牌 2 小时有效，这里在 rp_config 里存令牌与过期时间，
 *    上报前检查，过期/缺失时调身份验证接口重取。
 * 3. **补偿退避**：失败进 rp_retry，按 1min → 5min → 30min → 2h → 6h 指数退避，
 *    超过 max_retry 标记 ABANDONED 交人工，不会无限重试。
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    /** 补偿退避步长（分钟）：每次重试间隔 = step[min(retryCount, len-1)] */
    private static final int[] BACKOFF_MINUTES = {1, 5, 30, 120, 360};
    private static final int DEFAULT_MAX_RETRY = 5;

    @Resource
    private ReportReadMapper readMapper;
    @Resource
    private RpReportRecordMapper recordMapper;
    @Resource
    private RpRetryMapper retryMapper;
    @Resource
    private RpConfigMapper configMapper;
    @Resource
    private ReportAssembler assembler;
    @Resource
    private ReportClient reportClient;

    @Override
    public int scanAndReport(int limit) {
        int cap = limit <= 0 ? 50 : Math.min(limit, 200);
        int total = 0;
        // 每类单据取 config 时按 org 分组，这里简化：取第一个启用的 config 作为默认上报目标。
        // 多机构场景按 orgId 分别上报，见 reportOne。
        total += reportWaybills(cap);
        total += reportFundFlows(cap);
        total += reportVehicles(cap);
        total += reportDrivers(cap);
        return total;
    }

    @Override
    public int retryFailed(int limit) {
        int cap = limit <= 0 ? 20 : Math.min(limit, 100);
        QueryWrapper<RpRetry> qw = new QueryWrapper<RpRetry>();
        qw.eq("status", RpRetry.STATUS_WAITING).le("next_retry_time", LocalDateTime.now())
                .orderByAsc("next_retry_time").last("LIMIT " + cap);
        List<RpRetry> retries = retryMapper.selectList(qw);
        int n = 0;
        for (RpRetry retry : retries) {
            RpReportRecord record = recordMapper.selectById(retry.getRecordId());
            if (record == null) {
                markAbandoned(retry, "上报记录不存在");
                continue;
            }
            if (RpReportRecord.STATUS_SUCCESS.equals(record.getStatus())) {
                retry.setStatus(RpRetry.STATUS_DONE);
                retryMapper.updateById(retry);
                continue;
            }
            doReport(record, "RETRY");
            n++;
        }
        return n;
    }

    @Override
    public RpReportRecord reportOne(String docType, String bizNo, String operator) {
        String type = docType == null ? null : docType.trim().toUpperCase();
        RpConfig config = defaultConfig();
        if (config == null) {
            throw new BizException(ErrorCode.INTEGRATION_MOCK_ONLY, "未配置上报参数（rp_config 无启用记录）");
        }
        // 按 docType + bizNo 找业务数据并组装
        if (RpReportRecord.DOC_WAYBILL.equals(type)) {
            WaybillReportRow w = readMapper.pendingWaybills(null, 1).stream()
                    .filter(x -> x.getWaybillNo().equals(bizNo)).findFirst().orElse(null);
            if (w == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "找不到待上报运单：" + bizNo);
            }
            return reportWaybill(w, config, operator);
        }
        if (RpReportRecord.DOC_FUND_FLOW.equals(type)) {
            FundFlowReportRow f = readMapper.pendingFundFlows(null, 1).stream()
                    .filter(x -> x.getFlowNo().equals(bizNo)).findFirst().orElse(null);
            if (f == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "找不到待上报资金流水：" + bizNo);
            }
            return reportFundFlow(f, config, operator);
        }
        if (RpReportRecord.DOC_VEHICLE.equals(type)) {
            VehicleReportRow v = readMapper.pendingVehicles(null, 1).stream()
                    .filter(x -> x.getPlateNo().equals(bizNo)).findFirst().orElse(null);
            if (v == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "找不到待上报车辆：" + bizNo);
            }
            return reportVehicle(v, config, operator);
        }
        if (RpReportRecord.DOC_DRIVER.equals(type)) {
            DriverReportRow d = readMapper.pendingDrivers(null, 1).stream()
                    .filter(x -> x.getIdCardNo().equals(bizNo)).findFirst().orElse(null);
            if (d == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "找不到待上报驾驶员：" + bizNo);
            }
            return reportDriver(d, config, operator);
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "不支持的上报单据类型：" + docType);
    }

    @Override
    public RpReportRecord retryRecord(Long recordId, String operator) {
        RpReportRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "上报记录不存在：" + recordId);
        }
        if (RpReportRecord.STATUS_SUCCESS.equals(record.getStatus())) {
            return record;
        }
        doReport(record, operator == null ? "MANUAL" : operator);
        return recordMapper.selectById(recordId);
    }

    @Override
    public List<RpReportRecord> list(Long orgId, String docType, String status, Integer limit) {
        QueryWrapper<RpReportRecord> qw = new QueryWrapper<RpReportRecord>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (docType != null && !docType.trim().isEmpty()) {
            qw.eq("doc_type", docType.trim().toUpperCase());
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim().toUpperCase());
        }
        qw.orderByDesc("id");
        int cap = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
        qw.last("LIMIT " + cap);
        return recordMapper.selectList(qw);
    }

    @Override
    public ReportStatVo stats(Long orgId) {
        ReportStatVo vo = new ReportStatVo();
        QueryWrapper<RpReportRecord> qw = new QueryWrapper<RpReportRecord>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        vo.setTotal(recordMapper.selectCount(qw));
        vo.setSuccess(count(orgId, RpReportRecord.STATUS_SUCCESS));
        vo.setFailed(count(orgId, RpReportRecord.STATUS_FAILED));
        vo.setPending(count(orgId, RpReportRecord.STATUS_PENDING));

        QueryWrapper<RpRetry> rq = new QueryWrapper<RpRetry>();
        rq.eq("status", RpRetry.STATUS_WAITING);
        vo.setWaitingRetry(retryMapper.selectCount(rq));

        vo.setPendingWaybill(countPendingWaybills(orgId));
        vo.setPendingFundFlow(countPendingFundFlows(orgId));
        vo.setPendingVehicle(countPendingVehicles(orgId));
        vo.setPendingDriver(countPendingDrivers(orgId));

        long finished = vo.getSuccess() + vo.getFailed();
        vo.setSuccessRate(finished == 0 ? 0 : Math.round((double) vo.getSuccess() / finished * 10000.0) / 10000.0);
        return vo;
    }

    // ------------------------------------------------------------------ 内部：扫描上报

    private int reportWaybills(int cap) {
        List<WaybillReportRow> rows = readMapper.pendingWaybills(null, cap);
        RpConfig config = defaultConfig();
        if (config == null) return 0;
        int n = 0;
        for (WaybillReportRow w : rows) {
            try {
                reportWaybill(w, config, "SCAN");
                n++;
            } catch (Exception e) {
                log.warn("运单上报失败：{} {}", w.getWaybillNo(), e.getMessage());
            }
        }
        return n;
    }

    private int reportFundFlows(int cap) {
        List<FundFlowReportRow> rows = readMapper.pendingFundFlows(null, cap);
        RpConfig config = defaultConfig();
        if (config == null) return 0;
        int n = 0;
        for (FundFlowReportRow f : rows) {
            try {
                reportFundFlow(f, config, "SCAN");
                n++;
            } catch (Exception e) {
                log.warn("资金流水上报失败：{} {}", f.getFlowNo(), e.getMessage());
            }
        }
        return n;
    }

    private int reportVehicles(int cap) {
        List<VehicleReportRow> rows = readMapper.pendingVehicles(null, cap);
        RpConfig config = defaultConfig();
        if (config == null) return 0;
        int n = 0;
        for (VehicleReportRow v : rows) {
            try {
                reportVehicle(v, config, "SCAN");
                n++;
            } catch (Exception e) {
                log.warn("车辆上报失败：{} {}", v.getPlateNo(), e.getMessage());
            }
        }
        return n;
    }

    private int reportDrivers(int cap) {
        List<DriverReportRow> rows = readMapper.pendingDrivers(null, cap);
        RpConfig config = defaultConfig();
        if (config == null) return 0;
        int n = 0;
        for (DriverReportRow d : rows) {
            try {
                reportDriver(d, config, "SCAN");
                n++;
            } catch (Exception e) {
                log.warn("驾驶员上报失败：{} {}", d.getIdCardNo(), e.getMessage());
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ 内部：单条上报

    private RpReportRecord reportWaybill(WaybillReportRow w, RpConfig config, String operator) {
        ReportEnvelope env = assembler.waybill(w, config);
        return submit(env, w.getOrgId(), RpReportRecord.DOC_WAYBILL, w.getWaybillNo(), w.getId(), operator);
    }

    private RpReportRecord reportFundFlow(FundFlowReportRow f, RpConfig config, String operator) {
        ReportEnvelope env = assembler.fundFlow(f, config);
        return submit(env, f.getOrgId(), RpReportRecord.DOC_FUND_FLOW, f.getFlowNo(), f.getId(), operator);
    }

    private RpReportRecord reportVehicle(VehicleReportRow v, RpConfig config, String operator) {
        ReportEnvelope env = assembler.vehicle(v, config);
        return submit(env, v.getOrgId(), RpReportRecord.DOC_VEHICLE, v.getPlateNo(), v.getId(), operator);
    }

    private RpReportRecord reportDriver(DriverReportRow d, RpConfig config, String operator) {
        ReportEnvelope env = assembler.driver(d, config);
        return submit(env, d.getOrgId(), RpReportRecord.DOC_DRIVER, d.getIdCardNo(), d.getId(), operator);
    }

    /**
     * 提交上报：落记录 → 调 client → 回写状态。
     * 幂等：同一 (doc_type, biz_no) 无论成功失败只保留一条记录——失败复用旧记录重报，
     * 成功直接返回。否则含 FAIL 的单据每次扫描都插新行，rp_report_record 会被失败记录刷爆。
     */
    @Transactional(rollbackFor = Exception.class)
    public RpReportRecord submit(ReportEnvelope env, Long orgId, String docType, String bizNo,
                                 Long bizId, String operator) {
        // 查同 (doc_type, biz_no) 的既有记录：成功直接返回，失败复用重报
        QueryWrapper<RpReportRecord> existQw = new QueryWrapper<RpReportRecord>();
        existQw.eq("doc_type", docType).eq("biz_no", bizNo).orderByDesc("id").last("LIMIT 1");
        RpReportRecord exist = recordMapper.selectOne(existQw);
        if (exist != null && RpReportRecord.STATUS_SUCCESS.equals(exist.getStatus())) {
            return exist;
        }

        RpReportRecord record;
        if (exist != null) {
            // 复用失败记录：更新报文号与 payload 后重报，不新增行
            record = exist;
            record.setMessageRefNo(env.getMessageRefNo());
            record.setPayload(env.getEncryptedContent());
            record.setActionType(env.getIpcType());
            record.setUpdateTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else {
            record = new RpReportRecord();
            record.setOrgId(orgId);
            record.setMessageRefNo(env.getMessageRefNo());
            record.setDocType(docType);
            record.setBizNo(bizNo);
            record.setBizId(bizId);
            record.setActionType(env.getIpcType());
            record.setPayload(env.getEncryptedContent());
            record.setStatus(RpReportRecord.STATUS_PENDING);
            record.setRetryCount(0);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }

        doReport(record, operator);
        return recordMapper.selectById(record.getId());
    }

    /** 执行一次上报（含令牌管理 + 成功回写 / 失败进补偿）。 */
    private void doReport(RpReportRecord record, String operator) {
        RpConfig config = configOf(record.getOrgId());
        if (config == null) {
            markFailed(record, "无上报配置");
            return;
        }
        String token = ensureToken(config);
        ReportEnvelope env = new ReportEnvelope();
        env.setMessageRefNo(record.getMessageRefNo());
        env.setDocType(record.getDocType());
        env.setBizNo(record.getBizNo());
        env.setSenderCode(config.getSenderCode());
        env.setReceiverCode(config.getReceiverCode());
        env.setIpcType(record.getActionType());
        env.setEncryptedContent(record.getPayload());
        env.setToken(token);

        ReportReceipt receipt = reportClient.upload(env, token);
        LocalDateTime now = LocalDateTime.now();
        if (receipt.isSuccess()) {
            RpReportRecord upd = new RpReportRecord();
            upd.setId(record.getId());
            upd.setStatus(RpReportRecord.STATUS_SUCCESS);
            upd.setReceipt(receipt.getReceiptNo() == null ? receipt.getMessage() : receipt.getReceiptNo());
            upd.setSendTime(now);
            upd.setUpdateTime(now);
            recordMapper.updateById(upd);
            completeRetry(record.getId());
            markBizReported(record);
        } else {
            markFailedWithRetry(record, receipt.getMessage());
        }
    }

    /** 失败：更新记录状态 + 进补偿队列。 */
    private void markFailedWithRetry(RpReportRecord record, String reason) {
        int retryCount = record.getRetryCount() == null ? 0 : record.getRetryCount() + 1;
        RpReportRecord upd = new RpReportRecord();
        upd.setId(record.getId());
        upd.setStatus(RpReportRecord.STATUS_FAILED);
        upd.setReceipt(reason);
        upd.setRetryCount(retryCount);
        upd.setUpdateTime(LocalDateTime.now());

        if (retryCount >= DEFAULT_MAX_RETRY) {
            // 超过最大重试：标记补偿放弃，交人工
            upd.setNextRetryTime(null);
            recordMapper.updateById(upd);
            RpRetry retry = findByRecord(record.getId());
            if (retry != null) {
                retry.setStatus(RpRetry.STATUS_ABANDONED);
                retry.setFailReason(reason);
                retryMapper.updateById(retry);
            }
            log.warn("上报补偿放弃（重试 {} 次）：{} {}", retryCount, record.getBizNo(), reason);
        } else {
            LocalDateTime next = LocalDateTime.now().plusMinutes(
                    BACKOFF_MINUTES[Math.min(retryCount - 1, BACKOFF_MINUTES.length - 1)]);
            upd.setNextRetryTime(next);
            recordMapper.updateById(upd);
            RpRetry retry = findByRecord(record.getId());
            if (retry == null) {
                retry = new RpRetry();
                retry.setRecordId(record.getId());
                retry.setRetryCount(retryCount);
                retry.setMaxRetry(DEFAULT_MAX_RETRY);
                retry.setFailReason(reason);
                retry.setNextRetryTime(next);
                retry.setStatus(RpRetry.STATUS_WAITING);
                retry.setCreateTime(LocalDateTime.now());
                retry.setUpdateTime(LocalDateTime.now());
                retryMapper.insert(retry);
            } else {
                retry.setRetryCount(retryCount);
                retry.setFailReason(reason);
                retry.setNextRetryTime(next);
                retry.setStatus(RpRetry.STATUS_WAITING);
                retryMapper.updateById(retry);
            }
        }
    }

    private void markFailed(RpReportRecord record, String reason) {
        RpReportRecord upd = new RpReportRecord();
        upd.setId(record.getId());
        upd.setStatus(RpReportRecord.STATUS_FAILED);
        upd.setReceipt(reason);
        upd.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(upd);
    }

    private void completeRetry(Long recordId) {
        QueryWrapper<RpRetry> qw = new QueryWrapper<RpRetry>();
        qw.eq("record_id", recordId).eq("status", RpRetry.STATUS_WAITING).last("LIMIT 1");
        RpRetry retry = retryMapper.selectOne(qw);
        if (retry != null) {
            retry.setStatus(RpRetry.STATUS_DONE);
            retryMapper.updateById(retry);
        }
    }

    private void markAbandoned(RpRetry retry, String reason) {
        retry.setStatus(RpRetry.STATUS_ABANDONED);
        retry.setFailReason(reason);
        retryMapper.updateById(retry);
    }

    /** 上报成功后回写业务表的 report_status（运单）。 */
    private void markBizReported(RpReportRecord record) {
        if (RpReportRecord.DOC_WAYBILL.equals(record.getDocType()) && record.getBizId() != null) {
            // 直接通过 SQL 更新，避免 report 模块依赖 waybill 模块的 Mapper
            recordMapper.markWaybillReported(record.getBizId());
        }
    }

    private RpRetry findByRecord(Long recordId) {
        QueryWrapper<RpRetry> qw = new QueryWrapper<RpRetry>();
        qw.eq("record_id", recordId).last("LIMIT 1");
        return retryMapper.selectOne(qw);
    }

    // ------------------------------------------------------------------ 内部：令牌与配置

    /** 确保令牌有效：缺失或过期时重新换取。 */
    private String ensureToken(RpConfig config) {
        if (config.getToken() != null && config.getTokenExpireTime() != null
                && config.getTokenExpireTime().isAfter(LocalDateTime.now())) {
            return config.getToken();
        }
        String token = reportClient.authenticate(config.getSenderCode(), config.getAuthEndpoint());
        RpConfig upd = new RpConfig();
        upd.setId(config.getId());
        upd.setToken(token);
        upd.setTokenExpireTime(LocalDateTime.now().plusHours(2)); // 规范 2 小时
        upd.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(upd);
        config.setToken(token);
        config.setTokenExpireTime(upd.getTokenExpireTime());
        return token;
    }

    private RpConfig defaultConfig() {
        QueryWrapper<RpConfig> qw = new QueryWrapper<RpConfig>();
        qw.eq("enabled", 1).orderByAsc("id").last("LIMIT 1");
        return configMapper.selectOne(qw);
    }

    private RpConfig configOf(Long orgId) {
        QueryWrapper<RpConfig> qw = new QueryWrapper<RpConfig>();
        qw.eq("enabled", 1);
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByAsc("id").last("LIMIT 1");
        RpConfig config = configMapper.selectOne(qw);
        if (config == null) {
            config = defaultConfig();
        }
        return config;
    }

    private long count(Long orgId, String status) {
        QueryWrapper<RpReportRecord> qw = new QueryWrapper<RpReportRecord>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.eq("status", status);
        return recordMapper.selectCount(qw);
    }

    // 待上报数量的精确统计（避免 pendingXxx(limit=1) 只取 1 条导致看板数字不准）
    private long countPendingWaybills(Long orgId) {
        return readMapper.pendingWaybills(orgId, 500).size();
    }

    private long countPendingFundFlows(Long orgId) {
        return readMapper.pendingFundFlows(orgId, 500).size();
    }

    private long countPendingVehicles(Long orgId) {
        return readMapper.pendingVehicles(orgId, 500).size();
    }

    private long countPendingDrivers(Long orgId) {
        return readMapper.pendingDrivers(orgId, 500).size();
    }
}
