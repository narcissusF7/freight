package com.lukachangtong.freight.stat.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从自由文本地址解析省级行政区。
 *
 * 为什么不在 SQL 里做：地址是用户手填的（「上海浦东外高桥」「浙江省杭州市余杭区」），
 * 没有规范字段可 group by。解析只能放在应用层，或在建单时就落一个 province 字段。
 * 现在的量级（单日万级运单）在应用层聚合完全够用；量再上一个数量级，
 * 应当把解析结果落库而不是继续加大这里的内存开销。
 */
public final class RegionParser {

    public static final String UNKNOWN = "其他";

    /** 34 个省级行政区，按长度倒序匹配（先匹配「内蒙古自治区」再匹配「内蒙古」） */
    private static final List<String> PROVINCES = Arrays.asList(
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "香港特别行政区", "澳门特别行政区",
            "北京市", "天津市", "上海市", "重庆市",
            "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省", "江苏省", "浙江省", "安徽省",
            "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省", "广东省", "海南省",
            "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省");

    /** 简称 → 全称，覆盖「上海」「内蒙」「广西」这类写法 */
    private static final Map<String, String> ALIAS;

    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (String p : PROVINCES) {
            String shortName = p.replace("省", "").replace("市", "")
                    .replace("自治区", "").replace("特别行政区", "")
                    .replace("壮族", "").replace("回族", "").replace("维吾尔", "");
            m.put(shortName, p);
        }
        m.put("内蒙", "内蒙古自治区");
        m.put("黑龙江", "黑龙江省");
        ALIAS = Collections.unmodifiableMap(m);
    }

    /**
     * 主要城市 → 省。司机填地址常常只写城市（「厦门」「宁波北仑」），
     * 不带省名时按省前缀匹配会全部落到「其他」，地区分布图就废了。
     *
     * 这里只覆盖省会 + 计划单列市 + 主要物流城市（约 60 个），
     * 不做全量城市表：那需要一份会过期的行政区划数据源，收益也不成正比。
     * 匹配不到就老实归「其他」，不猜——统计猜错比统计缺失更糟。
     */
    private static final Map<String, String> CITY_ALIAS;

    static {
        Map<String, String> c = new LinkedHashMap<String, String>();
        c.put("厦门", "福建省");
        c.put("福州", "福建省");
        c.put("泉州", "福建省");
        c.put("莆田", "福建省");
        c.put("漳州", "福建省");
        c.put("广州", "广东省");
        c.put("深圳", "广东省");
        c.put("东莞", "广东省");
        c.put("佛山", "广东省");
        c.put("珠海", "广东省");
        c.put("中山", "广东省");
        c.put("惠州", "广东省");
        c.put("汕头", "广东省");
        c.put("杭州", "浙江省");
        c.put("宁波", "浙江省");
        c.put("温州", "浙江省");
        c.put("义乌", "浙江省");
        c.put("金华", "浙江省");
        c.put("嘉兴", "浙江省");
        c.put("绍兴", "浙江省");
        c.put("台州", "浙江省");
        c.put("南京", "江苏省");
        c.put("苏州", "江苏省");
        c.put("无锡", "江苏省");
        c.put("常州", "江苏省");
        c.put("南通", "江苏省");
        c.put("徐州", "江苏省");
        c.put("盐城", "江苏省");
        c.put("青岛", "山东省");
        c.put("济南", "山东省");
        c.put("烟台", "山东省");
        c.put("潍坊", "山东省");
        c.put("临沂", "山东省");
        c.put("淄博", "山东省");
        c.put("成都", "四川省");
        c.put("绵阳", "四川省");
        c.put("武汉", "湖北省");
        c.put("宜昌", "湖北省");
        c.put("襄阳", "湖北省");
        c.put("长沙", "湖南省");
        c.put("株洲", "湖南省");
        c.put("郑州", "河南省");
        c.put("洛阳", "河南省");
        c.put("西安", "陕西省");
        c.put("合肥", "安徽省");
        c.put("芜湖", "安徽省");
        c.put("南昌", "江西省");
        c.put("赣州", "江西省");
        c.put("石家庄", "河北省");
        c.put("唐山", "河北省");
        c.put("保定", "河北省");
        c.put("太原", "山西省");
        c.put("大同", "山西省");
        c.put("临汾", "山西省");
        c.put("沈阳", "辽宁省");
        c.put("大连", "辽宁省");
        c.put("长春", "吉林省");
        c.put("哈尔滨", "黑龙江省");
        c.put("昆明", "云南省");
        c.put("贵阳", "贵州省");
        c.put("南宁", "广西壮族自治区");
        c.put("柳州", "广西壮族自治区");
        c.put("海口", "海南省");
        c.put("兰州", "甘肃省");
        c.put("西宁", "青海省");
        c.put("银川", "宁夏回族自治区");
        c.put("乌鲁木齐", "新疆维吾尔自治区");
        c.put("呼和浩特", "内蒙古自治区");
        c.put("包头", "内蒙古自治区");
        c.put("拉萨", "西藏自治区");
        CITY_ALIAS = Collections.unmodifiableMap(c);
    }

    private RegionParser() {
    }

    public static String provinceOf(String address) {
        if (address == null) {
            return UNKNOWN;
        }
        String addr = address.trim();
        if (addr.isEmpty()) {
            return UNKNOWN;
        }
        for (String p : PROVINCES) {
            if (addr.startsWith(p)) {
                return p;
            }
        }
        // 简称匹配：取前 4/3/2 字去比对，避免「上海浦东」这类没有「市」的写法落空
        for (int len = Math.min(4, addr.length()); len >= 2; len--) {
            String candidate = ALIAS.get(addr.substring(0, len));
            if (candidate != null) {
                return candidate;
            }
        }
        // 只写了城市名的（「厦门北」「宁波北仑」）：按主要城市表归省
        for (int len = Math.min(3, addr.length()); len >= 2; len--) {
            String candidate = CITY_ALIAS.get(addr.substring(0, len));
            if (candidate != null) {
                return candidate;
            }
        }
        return UNKNOWN;
    }
}
