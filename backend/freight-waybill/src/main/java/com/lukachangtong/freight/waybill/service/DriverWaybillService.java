package com.lukachangtong.freight.waybill.service;

import com.lukachangtong.freight.waybill.dto.DriverNodeConfirmRequest;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.vo.DriverHallVo;
import com.lukachangtong.freight.waybill.vo.DriverTodoVo;
import com.lukachangtong.freight.waybill.vo.DriverWaybillDetailVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 司机端（APP）业务能力：找货、接单、节点确认、我的运单。
 *
 * 与调度端（{@link WaybillService}）的关系：
 * - 调度端可以任意创建运单、指派司机、纠正状态（人工作业兜底）；
 * - 司机端**只能操作自己名下的运单**，且动作是受限的枚举（{@code DriverNodeAction}），
 *   不允许提交目标状态。这条边界是防止「司机端接口被拿来当后台接口用」的关键。
 */
public interface DriverWaybillService {

    /**
     * 货源大厅：待承运货源按适配度打分排序。
     *
     * @param lat/lng 司机当前位置，用于算「距装货地多远」；可为空（不给定位则线路维度取中性分）
     */
    List<DriverHallVo> hall(Long driverId, BigDecimal lat, BigDecimal lng, Integer limit, Integer maxDistanceKm);

    /**
     * 接单：货源 → 运单（ACCEPTED），一车一单。
     * 并发抢单靠 `UPDATE ... WHERE status=0` 的受影响行数兜底，不做分布式锁。
     *
     * @param vehicleId 为空时自动取司机名下第一辆审核通过的车
     */
    Long accept(Long goodsSourceId, Long driverId, Long vehicleId, String clientIp);

    /**
     * @param scope PENDING_LOAD / IN_TRANSIT / PENDING_SIGN / DONE / ALL
     */
    List<WbWaybill> myWaybills(Long driverId, String scope);

    /** 首页待办（不含消息未读数与证件提醒——跨模块数据由应用层补）。 */
    DriverTodoVo todo(Long driverId);

    /** 详情；传当前定位时顺带回算距装/卸货地距离，供前端提示。 */
    DriverWaybillDetailVo detail(Long waybillId, Long driverId, BigDecimal lat, BigDecimal lng);

    /** 节点确认：校验归属 → 校验定位（到厂类）→ 状态机 → 留痕 → 通知。 */
    DriverWaybillDetailVo confirm(Long waybillId, Long driverId, DriverNodeConfirmRequest request, String clientIp);
}
