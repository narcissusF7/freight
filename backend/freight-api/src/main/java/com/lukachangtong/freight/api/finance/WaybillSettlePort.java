package com.lukachangtong.freight.api.finance;

/**
 * 结算域访问运单域的端口（依赖倒置）。
 *
 * 结算需要读运单财务视图、并在支付成功后把运单推进到 SETTLED。
 * 如果 settlement 直接依赖 freight-waybill，会形成「财务 → 运单」的硬依赖，
 * 以后运单拆服务就得改财务代码；这里定义端口，由 waybill 模块实现，方向反过来。
 */
public interface WaybillSettlePort {

    /** 运单不存在返回 null。 */
    WaybillSettleInfo loadInfo(Long waybillId);

    /**
     * 标记运单已结算：RECEIPT_UPLOADED → SETTLED。
     * 已处于 SETTLED 及之后的状态时直接返回（幂等），不抛异常。
     *
     * @return true 表示本次发生了状态推进
     */
    boolean markSettled(Long waybillId, String settlementNo);
}
