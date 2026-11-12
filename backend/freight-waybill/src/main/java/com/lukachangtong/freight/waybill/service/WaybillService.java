package com.lukachangtong.freight.waybill.service;

import com.lukachangtong.freight.waybill.dto.ExceptionHandleRequest;
import com.lukachangtong.freight.waybill.dto.ExceptionReportRequest;
import com.lukachangtong.freight.waybill.dto.WaybillCreateRequest;
import com.lukachangtong.freight.waybill.dto.WaybillStatusChangeRequest;
import com.lukachangtong.freight.waybill.entity.WbException;
import com.lukachangtong.freight.waybill.entity.WbNode;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import com.lukachangtong.freight.waybill.entity.WbWaybill;

import java.io.InputStream;
import java.util.List;

public interface WaybillService {

    List<WbWaybill> list(Long orgId);

    /** 运单详情（含状态节点时间线）。 */
    WbWaybill detail(Long id);

    List<WbNode> nodes(Long waybillId);

    /** 由货源 + 指派司机/车辆生成运单，初始状态 DRAFT。 */
    Long createWaybill(WaybillCreateRequest request);

    /**
     * 状态流转：经 {@code WaybillStatus} 状态机校验，非法跃迁抛 B0002。
     * 同一事务内更新运单状态、写 wb_node、写 outbox_event（驱动上报/消息）。
     */
    void changeStatus(Long id, WaybillStatusChangeRequest request);

    // ---------- 异常件 ----------

    List<WbException> exceptions(Long waybillId);

    /**
     * 上报异常：异常记录始终落库；若当前状态允许转 EXCEPTION 则同步推进状态并留节点 + Outbox。
     *
     * @return 异常记录 id
     */
    Long reportException(Long waybillId, ExceptionReportRequest request);

    /** 处理异常：回写处理结果，状态置为已处理。 */
    void handleException(Long exceptionId, ExceptionHandleRequest request);

    // ---------- 回单 ----------

    List<WbReceipt> receipts(Long waybillId);

    /**
     * 上传回单：文件经 FileClient 落到对象存储，索引落 wb_receipt；
     * 若运单当前为 SIGNED，自动推进到 RECEIPT_UPLOADED（同一事务写节点 + Outbox）。
     *
     * @return 回单记录 id
     */
    Long uploadReceipt(Long waybillId, InputStream data, String fileName, String contentType,
                       String operator, String remark);

    /** 回单核验（0 待核验 → 1 已核验）。 */
    void verifyReceipt(Long receiptId, String operator);

    /**
     * 结算入账后推进运单：RECEIPT_UPLOADED → SETTLED。
     * 已处于 SETTLED 及之后返回 false（幂等）；未上传回单等不可结算状态抛 B0002。
     */
    boolean markSettled(Long waybillId, String settlementNo);
}
