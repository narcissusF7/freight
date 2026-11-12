package com.lukachangtong.freight.track.support;

import java.util.List;

/**
 * 地理计算工具（纯静态，无第三方 GIS 依赖）。
 *
 * 精度取舍：
 * - 距离用 Haversine（球面），600km 量级误差 <0.5%，足够里程与偏航判定；
 * - 点到线段距离在局部平面投影（等距圆柱）上算，跨省线路的偏航阈值是公里级，不需要大地测量级精度；
 * - 多边形用射线法（ray casting），只判断内外，不含边界模糊处理——边界外 1m 内外的差异对预警无意义。
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371D;

    private GeoUtils() {
    }

    /** 球面距离，单位公里。 */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1D, Math.sqrt(a)));
    }

    /** 点到线段的距离（公里）。点与线段重合或线段退化为点时，退化为点距。 */
    public static double distanceToSegmentKm(double lat, double lng,
                                             double aLat, double aLng,
                                             double bLat, double bLng) {
        // 以当前纬度为基准做等距圆柱投影，公里为单位的局部平面
        double kx = Math.cos(Math.toRadians(lat)) * 111.320D;
        double ky = 110.574D;
        double px = lng * kx, py = lat * ky;
        double ax = aLng * kx, ay = aLat * ky;
        double bx = bLng * kx, by = bLat * ky;
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-12) {
            return haversineKm(lat, lng, aLat, aLng);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0D, Math.min(1D, t));
        double cx = ax + t * dx, cy = ay + t * dy;
        return Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
    }

    /** 圆形围栏判定：半径单位米。 */
    public static boolean inCircle(double lat, double lng,
                                   double centerLat, double centerLng, int radiusM) {
        return haversineKm(lat, lng, centerLat, centerLng) * 1000D <= radiusM;
    }

    /** 多边形围栏判定（射线法）。vertices 为 [lat, lng] 数组，至少 3 个点。 */
    public static boolean inPolygon(double lat, double lng, List<double[]> vertices) {
        if (vertices == null || vertices.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            double yi = vertices.get(i)[0], xi = vertices.get(i)[1];
            double yj = vertices.get(j)[0], xj = vertices.get(j)[1];
            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }
}
