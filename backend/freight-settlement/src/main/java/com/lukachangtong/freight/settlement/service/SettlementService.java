package com.lukachangtong.freight.settlement.service;

import com.lukachangtong.freight.settlement.dto.SettlementGenerateRequest;
import com.lukachangtong.freight.settlement.entity.StSettlement;
import com.lukachangtong.freight.settlement.vo.PayResultVo;

import java.util.List;

/**
 * 结算服务：计价 → 结算单 → 确认 → 支付 → 入账。
 *
 * 状态机：DRAFT → CONFIRMED → PAYING → PAID；PAID → INVOICED → CLOSED。
 * 只有 CONFIRMED 能发起支付，只有 PAID 能开票 —— 顺序不能反，
 * 否则会出现「钱没付票先开」，税务与资金两条线都对不上。
 */
public interface SettlementService {

    /**
     * 按运单生成结算单（DRAFT）。
     * 幂等：同一运单同一方向只会有一张，重复调用返回原单。
     * 只有回单已上传（RECEIPT_UPLOADED 及之后）的运单可结算。
     */
    StSettlement generate(SettlementGenerateRequest request);

    /** 财务复核：DRAFT → CONFIRMED，并落结算业务日期（对账按此归集）。 */
    StSettlement confirm(Long id, String operator);

    /**
     * 发起支付：CONFIRMED → PAYING，创建支付单后立即执行（`freight.payment.auto-execute` 控制）。
     * 执行失败会把结算单退回 CONFIRMED，允许重试。
     */
    PayResultVo pay(Long id, String operator);

    /** 作废：仅允许未付款的单子。 */
    StSettlement voidSettlement(Long id, String reason, String operator);

    StSettlement detail(Long id);

    List<StSettlement> list(Long orgId, String status, String bizType, Integer limit);

    /** 按运单查（司机端/后台详情页用），不存在返回 null。 */
    StSettlement byWaybill(Long waybillId, String bizType);
}
