package com.lukachangtong.freight.customer.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/** 客服单号：与其它域一致的时间戳 + 随机数，作为唯一索引与幂等键的一部分。 */
public final class CustomerNo {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private CustomerNo() {
    }

    private static String gen(String prefix) {
        return prefix + LocalDateTime.now().format(TS)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** 投诉单号 CP... */
    public static String complaint() {
        return gen("CP");
    }

    /** 评价单号 RT... */
    public static String rating() {
        return gen("RT");
    }
}
