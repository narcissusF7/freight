package com.lukachangtong.freight.settlement.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/** 财务单号生成器。单号即幂等键的一部分，规则必须全局唯一。 */
public final class FinNo {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private FinNo() {
    }

    private static String gen(String prefix) {
        return prefix + LocalDateTime.now().format(TS)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** 结算单号 ST... */
    public static String settlement() {
        return gen("ST");
    }

    /** 资金流水号 FF... */
    public static String flow() {
        return gen("FF");
    }

    /** 提现单号 WD... */
    public static String withdraw() {
        return gen("WD");
    }
}
