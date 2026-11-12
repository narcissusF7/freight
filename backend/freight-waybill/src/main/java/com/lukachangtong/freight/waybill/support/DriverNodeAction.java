package com.lukachangtong.freight.waybill.support;

import com.lukachangtong.freight.api.waybill.WaybillStatus;

/**
 * 司机端节点动作。
 *
 * 关键点：**「到达」不是状态**。
 * 装货地/卸货地的到达只是事实打点（记录时间、位置、照片），运单状态不变；
 * 只有「装货完成 / 发车 / 卸货完成 / 签收」才推进状态机。
 *
 * 这么做的好处：
 * 1. 状态机保持 14 个业务态，不被「到达」类事件污染；
 * 2. 到厂时间、离厂时间这类 KPI（装卸等待时长）从打点记录即可算出；
 * 3. 打点记录能带定位与照片，是可核查的证据，而状态字段只能存一个当前值。
 */
public enum DriverNodeAction {

    ARRIVE_LOAD("到达装货地", WaybillStatus.ACCEPTED, null, true),
    LOADED("装货完成", WaybillStatus.ACCEPTED, WaybillStatus.LOADED, true),
    DEPART("发车启运", WaybillStatus.LOADED, WaybillStatus.IN_TRANSIT, false),
    ARRIVE_UNLOAD("到达卸货地", WaybillStatus.IN_TRANSIT, null, true),
    UNLOADED("卸货完成", WaybillStatus.IN_TRANSIT, WaybillStatus.UNLOADED, true),
    SIGNED("收货签收", WaybillStatus.UNLOADED, WaybillStatus.SIGNED, true);

    private final String label;
    /** 允许执行该动作的当前状态（前置条件） */
    private final WaybillStatus requiredFrom;
    /** 动作完成后推进到的状态；null 表示只打点不改状态 */
    private final WaybillStatus target;
    /** 是否必须带定位（到厂/装卸类必须，发车不强制） */
    private final boolean locationRequired;

    DriverNodeAction(String label, WaybillStatus requiredFrom, WaybillStatus target, boolean locationRequired) {
        this.label = label;
        this.requiredFrom = requiredFrom;
        this.target = target;
        this.locationRequired = locationRequired;
    }

    public String getLabel() { return label; }
    public WaybillStatus getRequiredFrom() { return requiredFrom; }
    public WaybillStatus getTarget() { return target; }
    public boolean isLocationRequired() { return locationRequired; }

    /** 只打点不流转。 */
    public boolean isCheckpointOnly() { return target == null; }

    /** 解析动作名，非法值抛 IllegalArgumentException 由调用方转 A0001。 */
    public static DriverNodeAction of(String name) {
        if (name == null) {
            throw new IllegalArgumentException("节点动作不能为空");
        }
        return valueOf(name.trim().toUpperCase());
    }
}
