package com.lukachangtong.freight.waybill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.api.waybill.WaybillStatus;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.FileUploadResult;
import com.lukachangtong.freight.infra.mapper.OutboxEventMapper;
import com.lukachangtong.freight.infra.outbox.OutboxEvent;
import com.lukachangtong.freight.waybill.dto.ExceptionHandleRequest;
import com.lukachangtong.freight.waybill.dto.ExceptionReportRequest;
import com.lukachangtong.freight.waybill.dto.WaybillCreateRequest;
import com.lukachangtong.freight.waybill.dto.WaybillStatusChangeRequest;
import com.lukachangtong.freight.waybill.entity.WbException;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;
import com.lukachangtong.freight.waybill.entity.WbNode;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.mapper.ExceptionMapper;
import com.lukachangtong.freight.waybill.mapper.GoodsSourceMapper;
import com.lukachangtong.freight.waybill.mapper.NodeMapper;
import com.lukachangtong.freight.waybill.mapper.ReceiptMapper;
import com.lukachangtong.freight.waybill.mapper.WaybillMapper;
import com.lukachangtong.freight.waybill.service.WaybillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WaybillServiceImpl implements WaybillService {

    @Resource
    private WaybillMapper waybillMapper;
    @Resource
    private NodeMapper nodeMapper;
    @Resource
    private GoodsSourceMapper goodsSourceMapper;
    @Resource
    private OutboxEventMapper outboxEventMapper;
    @Resource
    private ExceptionMapper exceptionMapper;
    @Resource
    private ReceiptMapper receiptMapper;
    @Resource
    private FileClient fileClient;

    @Override
    public List<WbWaybill> list(Long orgId) {
        QueryWrapper<WbWaybill> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("id");
        return waybillMapper.selectList(qw);
    }

    @Override
    public WbWaybill detail(Long id) {
        return waybillMapper.selectById(id);
    }

    @Override
    public List<WbNode> nodes(Long waybillId) {
        QueryWrapper<WbNode> qw = new QueryWrapper<>();
        qw.eq("waybill_id", waybillId).orderByAsc("id");
        return nodeMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWaybill(WaybillCreateRequest request) {
        WbGoodsSource gs = goodsSourceMapper.selectById(request.getGoodsSourceId());
        if (gs == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "货源不存在");
        }
        if (gs.getStatus() != null && gs.getStatus() == 1) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "该货源已生成运单");
        }
        WbWaybill wb = new WbWaybill();
        wb.setOrgId(request.getOrgId());
        wb.setGoodsSourceId(gs.getId());
        wb.setShipperId(gs.getShipperId());
        wb.setDriverId(request.getDriverId());
        wb.setVehicleId(request.getVehicleId());
        wb.setCargoName(request.getCargoName() != null ? request.getCargoName() : gs.getGoodsName());
        wb.setWeight(request.getWeight() != null ? request.getWeight() : gs.getWeight());
        wb.setLoadAddr(request.getLoadAddr() != null ? request.getLoadAddr() : gs.getLoadAddr());
        wb.setLoadLat(request.getLoadLat() != null ? request.getLoadLat() : gs.getLoadLat());
        wb.setLoadLng(request.getLoadLng() != null ? request.getLoadLng() : gs.getLoadLng());
        wb.setUnloadAddr(request.getUnloadAddr() != null ? request.getUnloadAddr() : gs.getUnloadAddr());
        wb.setUnloadLat(request.getUnloadLat() != null ? request.getUnloadLat() : gs.getUnloadLat());
        wb.setUnloadLng(request.getUnloadLng() != null ? request.getUnloadLng() : gs.getUnloadLng());
        wb.setFreightAmount(gs.getFreightPrice());
        wb.setPlanLoadTime(request.getPlanLoadTime() != null ? request.getPlanLoadTime() : gs.getLoadTime());
        wb.setStatus(WaybillStatus.DRAFT.name());
        wb.setReportStatus(0);
        // 业务单号必须在 insert 前生成（waybill_no 为 NOT NULL 且无默认值）
        wb.setWaybillNo(generateWaybillNo());
        wb.setCreateBy(request.getOperateBy());
        wb.setCreateTime(LocalDateTime.now());
        wb.setUpdateTime(LocalDateTime.now());
        waybillMapper.insert(wb);
        // 标记货源已生成运单
        WbGoodsSource updGs = new WbGoodsSource();
        updGs.setId(gs.getId());
        updGs.setStatus(1);
        updGs.setUpdateBy(request.getOperateBy());
        updGs.setUpdateTime(LocalDateTime.now());
        goodsSourceMapper.updateById(updGs);
        // 初始节点：生成草稿
        appendNode(wb.getId(), null, WaybillStatus.DRAFT.name(), request.getOperateBy(), "生成运单草稿");
        return wb.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, WaybillStatusChangeRequest request) {
        WbWaybill wb = waybillMapper.selectById(id);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        WaybillStatus target;
        try {
            target = WaybillStatus.valueOf(request.getToStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知的目标状态: " + request.getToStatus());
        }
        transit(wb, target, request.getOperateBy(), request.getRemark());
    }

    // ---------- 异常件 ----------

    @Override
    public List<WbException> exceptions(Long waybillId) {
        QueryWrapper<WbException> qw = new QueryWrapper<>();
        qw.eq("waybill_id", waybillId).orderByDesc("id");
        return exceptionMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportException(Long waybillId, ExceptionReportRequest request) {
        WbWaybill wb = waybillMapper.selectById(waybillId);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        if (request.getExceptionType() == null || request.getExceptionType().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "异常类型不能为空");
        }
        WbException ex = new WbException();
        ex.setWaybillId(waybillId);
        ex.setExceptionType(request.getExceptionType());
        ex.setDescription(request.getDescription());
        ex.setImageUrls(request.getImageUrls());
        ex.setReportBy(request.getOperateBy());
        ex.setReportTime(LocalDateTime.now());
        ex.setStatus(0);
        ex.setCreateTime(LocalDateTime.now());
        ex.setUpdateTime(LocalDateTime.now());
        exceptionMapper.insert(ex);

        // 状态机允许时才推进状态；DRAFT/PUBLISHED 等未起运阶段只记录不流转
        WaybillStatus current = parseStatus(wb.getStatus());
        if (current.canTransferTo(WaybillStatus.EXCEPTION)) {
            transit(wb, WaybillStatus.EXCEPTION, request.getOperateBy(),
                    "异常上报：" + request.getExceptionType()
                            + (request.getDescription() == null ? "" : " - " + request.getDescription()));
        } else {
            appendNode(waybillId, current.name(), current.name(), request.getOperateBy(),
                    "异常登记（当前状态不允许转异常）：" + request.getExceptionType());
        }
        return ex.getId();
    }

    @Override
    public void handleException(Long exceptionId, ExceptionHandleRequest request) {
        WbException ex = exceptionMapper.selectById(exceptionId);
        if (ex == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "异常记录不存在");
        }
        WbException upd = new WbException();
        upd.setId(exceptionId);
        upd.setStatus(1);
        upd.setHandleRemark(request.getHandleRemark());
        upd.setHandleBy(request.getOperateBy());
        upd.setHandleTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        exceptionMapper.updateById(upd);
    }

    // ---------- 回单 ----------

    @Override
    public List<WbReceipt> receipts(Long waybillId) {
        QueryWrapper<WbReceipt> qw = new QueryWrapper<>();
        qw.eq("waybill_id", waybillId).orderByDesc("id");
        return receiptMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadReceipt(Long waybillId, InputStream data, String fileName, String contentType,
                              String operator, String remark) {
        WbWaybill wb = waybillMapper.selectById(waybillId);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        FileUploadResult uploaded = fileClient.upload(data, fileName, contentType);
        WbReceipt rec = new WbReceipt();
        rec.setWaybillId(waybillId);
        rec.setFileKey(uploaded.getFileKey());
        rec.setFileName(uploaded.getFileName());
        rec.setFileUrl(fileClient.getAccessUrl(uploaded.getFileKey()));
        rec.setUploadBy(operator);
        rec.setUploadTime(LocalDateTime.now());
        rec.setVerified(0);
        rec.setRemark(remark);
        rec.setCreateTime(LocalDateTime.now());
        rec.setUpdateTime(LocalDateTime.now());
        receiptMapper.insert(rec);

        // 已签收但缺回单的运单，回单到手即推进到 RECEIPT_UPLOADED（结算前置条件）
        WaybillStatus current = parseStatus(wb.getStatus());
        if (current.canTransferTo(WaybillStatus.RECEIPT_UPLOADED)) {
            transit(wb, WaybillStatus.RECEIPT_UPLOADED, operator, "回单上传，自动流转");
        }
        return rec.getId();
    }

    @Override
    public void verifyReceipt(Long receiptId, String operator) {
        WbReceipt rec = receiptMapper.selectById(receiptId);
        if (rec == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "回单不存在");
        }
        WbReceipt upd = new WbReceipt();
        upd.setId(receiptId);
        upd.setVerified(1);
        upd.setUpdateTime(LocalDateTime.now());
        receiptMapper.updateById(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSettled(Long waybillId, String settlementNo) {
        WbWaybill wb = waybillMapper.selectById(waybillId);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        WaybillStatus current = parseStatus(wb.getStatus());
        if (current == WaybillStatus.SETTLED || current == WaybillStatus.REPORTED
                || current == WaybillStatus.COMPLETED) {
            return false; // 幂等：已经结算过
        }
        if (!current.canTransferTo(WaybillStatus.SETTLED)) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "运单状态「" + current + "」不能结算，需先完成回单上传");
        }
        transit(wb, WaybillStatus.SETTLED, "SYSTEM", "运费结算完成：" + settlementNo);
        return true;
    }

    // ---------- 内部 ----------

    /** 状态流转统一入口：状态机校验 + 运单更新 + 节点留痕 + Outbox，同一事务。 */
    private void transit(WbWaybill wb, WaybillStatus target, String operator, String remark) {
        WaybillStatus current = parseStatus(wb.getStatus());
        try {
            current.assertTransferTo(target);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER,
                    "非法的运单状态流转: " + current + " -> " + target);
        }
        WbWaybill upd = new WbWaybill();
        upd.setId(wb.getId());
        upd.setStatus(target.name());
        if (target == WaybillStatus.REPORTED) {
            upd.setReportStatus(1);
        }
        // 业务时间戳：统计按「建单日 / 签收日」归集，只靠状态字符串还原不出历史某天的签收量。
        // 只写第一次，重复进入同一状态不覆盖（例如异常处理后回到原状态）。
        LocalDateTime now = LocalDateTime.now();
        if (target == WaybillStatus.ACCEPTED && wb.getAcceptTime() == null) {
            upd.setAcceptTime(now);
            wb.setAcceptTime(now);
        }
        if (target == WaybillStatus.SIGNED && wb.getSignTime() == null) {
            upd.setSignTime(now);
            wb.setSignTime(now);
        }
        upd.setUpdateBy(operator);
        upd.setUpdateTime(LocalDateTime.now());
        waybillMapper.updateById(upd);
        wb.setStatus(target.name());
        appendNode(wb.getId(), current.name(), target.name(), operator, remark);
        writeOutbox(wb, target);
    }

    private WaybillStatus parseStatus(String status) {
        try {
            return WaybillStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "运单当前状态异常: " + status);
        }
    }

    /**
     * 业务单号：WB + yyyyMMddHHmmssSSS + 3 位随机数（22 位，唯一索引 uk_wb_no）。
     * 不用自增 id 回填，避免 NOT NULL 字段在 insert 时缺值。
     */
    private String generateWaybillNo() {
        return com.lukachangtong.freight.waybill.support.BizNoGenerator.waybillNo();
    }

    private void appendNode(Long waybillId, String from, String to, String operator, String remark) {
        WbNode node = new WbNode();
        node.setWaybillId(waybillId);
        node.setFromStatus(from);
        node.setToStatus(to);
        node.setOperateBy(operator);
        node.setOperateTime(LocalDateTime.now());
        node.setRemark(remark);
        nodeMapper.insert(node);
    }

    /** Outbox：与业务数据同事务写入，由 XXL-Job 异步投递（监管上报/消息推送）。 */
    private void writeOutbox(WbWaybill wb, WaybillStatus target) {
        OutboxEvent event = new OutboxEvent();
        event.setEventKey(wb.getWaybillNo() + ":" + target.name() + ":" + System.currentTimeMillis());
        event.setEventType("WAYBILL_STATUS_CHANGED");
        event.setPayload("{\"waybillNo\":\"" + wb.getWaybillNo() + "\",\"status\":\"" + target.name() + "\"}");
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        outboxEventMapper.insert(event);
    }
}
