package com.lukachangtong.freight.api.waybill;

/**
 * 客服域读取运单参与方的端口（依赖倒置）。
 *
 * 投诉要判断「被投诉人是不是这张运单的司机」，评价要判断「评价人是不是当事人、运单是否已签收」，
 * 这些都依赖运单事实，但客服域不能反向依赖运单模块（会造成 customer ↔ waybill 的环）。
 */
public interface WaybillPartyPort {

    /** 运单不存在返回 null。 */
    WaybillParty load(Long waybillId);
}
