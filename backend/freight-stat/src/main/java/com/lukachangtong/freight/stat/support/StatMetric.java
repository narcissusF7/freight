package com.lukachangtong.freight.stat.support;

/**
 * 可查询的指标清单。
 *
 * 指标名同时是「钻取接口的参数」与「前端默认展示的维度」，
 * 集中枚举可以避免前端传了一个后端没有的字符串却静默返回空列表。
 */
public enum StatMetric {

    WAYBILL_CREATED("新建运单", "单", "wb_waybill.create_time"),
    WAYBILL_SIGNED("签收完成", "单", "wb_waybill.sign_time"),
    SETTLEMENT("应付结算", "元", "st_settlement.settled_date"),
    PAYMENT("支付成功", "元", "st_payment_order.execute_time"),
    COMPLAINT("新增投诉", "单", "cs_complaint.create_time"),
    RATING("评价", "条", "cs_rating.create_time"),
    ALARM("轨迹预警", "条", "tr_track_alarm.alarm_time");

    private final String label;
    private final String unit;
    private final String source;

    StatMetric(String label, String unit, String source) {
        this.label = label;
        this.unit = unit;
        this.source = source;
    }

    public String getLabel() { return label; }
    public String getUnit() { return unit; }
    public String getSource() { return source; }

    public static StatMetric of(String code) {
        if (code == null) {
            throw new IllegalArgumentException("指标不能为空");
        }
        try {
            return StatMetric.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的统计指标：" + code);
        }
    }
}
