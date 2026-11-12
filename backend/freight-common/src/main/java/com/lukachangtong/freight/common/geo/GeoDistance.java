package com.lukachangtong.freight.common.geo;

/**
 * 坐标距离计算（Haversine 球面距离）。
 *
 * 放在 common 而不是某个业务模块：轨迹的偏航判定、围栏命中、司机端节点确认的
 * 「是否真的到厂」校验都要用它，任何单一业务模块持有都会逼出跨模块依赖。
 *
 * 约定：**全平台坐标统一 GCJ-02**（腾讯地图/AI 定位口径）。WGS-84 数据必须在接入层
 * 转换后再入库，否则 50~500 米的系统性偏移会让「3 公里内才能确认装卸」这类校验失真。
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371D;

    private GeoDistance() {
    }

    /** 球面距离（公里）。参数任一为 null 时返回 -1，调用方据此跳过校验。 */
    public static double km(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return -1D;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 球面距离（米），入参为 double 原始值。 */
    public static double meters(double lat1, double lng1, double lat2, double lng2) {
        return km(lat1, lng1, lat2, lng2) * 1000D;
    }
}
