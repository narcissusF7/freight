package com.lukachangtong.freight.waybill.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成。运单号 22 位：WB + yyyyMMddHHmmssSSS + 3 位随机，命中 uk_wb_no 的概率可忽略。
 *
 * 单独抽出来是因为调度建单（草稿）与司机接单（直接 ACCEPTED）是两个入口，
 * 单号规则必须只有一份，否则监管上报的对账口径会分裂。
 */
public final class BizNoGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private BizNoGenerator() {
    }

    public static String waybillNo() {
        return "WB" + LocalDateTime.now().format(TS) + String.format("%03d",
                ThreadLocalRandom.current().nextInt(1000));
    }

    /** 手机号脱敏：138****1234。接单前只暴露脱敏号码。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
