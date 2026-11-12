package com.lukachangtong.freight.api.waybill;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 运单状态机。状态流转只能由服务端校验，禁止前端直接提交目标状态。
 * 放在 freight-api 是因为多个模块都要读状态，且它属于跨模块契约。
 */
public enum WaybillStatus {

    DRAFT,
    PUBLISHED,
    ACCEPTED,
    LOADED,
    IN_TRANSIT,
    UNLOADED,
    SIGNED,
    RECEIPT_UPLOADED,
    SETTLED,
    REPORTED,
    COMPLETED,
    EXCEPTION,
    WITHDRAWN,
    VOID;

    private static final Map<WaybillStatus, Set<WaybillStatus>> ALLOWED;

    static {
        EnumMap<WaybillStatus, Set<WaybillStatus>> m = new EnumMap<WaybillStatus, Set<WaybillStatus>>(WaybillStatus.class);
        m.put(DRAFT, EnumSet.of(PUBLISHED, WITHDRAWN, VOID));
        m.put(PUBLISHED, EnumSet.of(ACCEPTED, WITHDRAWN, VOID));
        m.put(ACCEPTED, EnumSet.of(LOADED, EXCEPTION, VOID));
        m.put(LOADED, EnumSet.of(IN_TRANSIT, EXCEPTION));
        m.put(IN_TRANSIT, EnumSet.of(UNLOADED, EXCEPTION));
        m.put(UNLOADED, EnumSet.of(SIGNED, EXCEPTION));
        m.put(SIGNED, EnumSet.of(RECEIPT_UPLOADED, EXCEPTION));
        m.put(RECEIPT_UPLOADED, EnumSet.of(SETTLED));
        m.put(SETTLED, EnumSet.of(REPORTED));
        m.put(REPORTED, EnumSet.of(COMPLETED));
        m.put(EXCEPTION, EnumSet.of(ACCEPTED, LOADED, IN_TRANSIT, UNLOADED, SIGNED, VOID));
        m.put(WITHDRAWN, Collections.<WaybillStatus>emptySet());
        m.put(VOID, Collections.<WaybillStatus>emptySet());
        m.put(COMPLETED, Collections.<WaybillStatus>emptySet());
        ALLOWED = Collections.unmodifiableMap(m);
    }

    public boolean canTransferTo(WaybillStatus target) {
        if (target == null) {
            return false;
        }
        Set<WaybillStatus> targets = ALLOWED.get(this);
        return targets != null && targets.contains(target);
    }

    /**
     * 非法跃迁直接抛 IllegalArgumentException，由调用方转成 B0002 错误码。
     */
    public void assertTransferTo(WaybillStatus target) {
        if (!canTransferTo(target)) {
            throw new IllegalArgumentException(
                    "illegal waybill status transfer: " + this + " -> " + target);
        }
    }
}
