package com.lukachangtong.freight.customer.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 投诉字典：类型/等级/状态的中文文案。
 *
 * 放在后端而不是各端各写一份：投诉内容会同时出现在后台列表、司机端消息、监管导出里，
 * 三处文案一旦不一致，同一个投诉看起来就像两件事。
 */
public final class ComplaintDict {

    private static final Map<String, String> TYPE;
    private static final Map<String, String> LEVEL;
    private static final Map<String, String> STATUS;

    static {
        Map<String, String> t = new HashMap<String, String>();
        t.put("DELAY", "运输延误");
        t.put("DAMAGE", "货物破损");
        t.put("ATTITUDE", "服务态度");
        t.put("FEE", "费用争议");
        t.put("LOST", "货物丢失");
        t.put("OTHER", "其他");
        TYPE = Collections.unmodifiableMap(t);

        Map<String, String> l = new HashMap<String, String>();
        l.put("NORMAL", "普通");
        l.put("URGENT", "紧急");
        LEVEL = Collections.unmodifiableMap(l);

        Map<String, String> s = new HashMap<String, String>();
        s.put("PENDING", "待受理");
        s.put("PROCESSING", "处理中");
        s.put("RESOLVED", "已处理待确认");
        s.put("REJECTED", "已驳回");
        s.put("CLOSED", "已办结");
        STATUS = Collections.unmodifiableMap(s);
    }

    private ComplaintDict() {
    }

    public static String typeLabel(String code) {
        return label(TYPE, code);
    }

    public static String levelLabel(String code) {
        return label(LEVEL, code);
    }

    public static String statusLabel(String code) {
        return label(STATUS, code);
    }

    public static boolean validType(String code) {
        return code != null && TYPE.containsKey(code.toUpperCase());
    }

    private static String label(Map<String, String> map, String code) {
        if (code == null) {
            return "-";
        }
        String v = map.get(code.toUpperCase());
        return v == null ? code : v;
    }
}
