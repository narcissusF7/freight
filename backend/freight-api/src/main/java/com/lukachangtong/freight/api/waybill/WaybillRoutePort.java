package com.lukachangtong.freight.api.waybill;

/**
 * 运单路线查询端口（依赖倒置）。
 *
 * 轨迹/预警需要知道运单的起终点与所属机构，但**不能让 track 依赖 waybill**：
 * waybill 后续会调用 track 做在途看板，双向依赖会形成循环。因此在 api 模块定义端口，
 * 由 waybill 模块提供实现，track 只注入接口。
 */
public interface WaybillRoutePort {

    /** 运单不存在返回 null。 */
    WaybillRouteInfo load(Long waybillId);
}
