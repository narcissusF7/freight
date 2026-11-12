package com.lukachangtong.freight.settlement.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额与税额计算。**所有金额运算都必须走这里**，不要在各处自己 setScale。
 *
 * 两条约定：
 * 1. 一律 `DECIMAL(18,2)` + `HALF_UP`；
 * 2. 含税/不含税互转时，**税额永远用差额得出**（tax = amount - net），
 *    不对两边各自四舍五入——否则发票税额与账上税额会差 1 分，日终对账永远不平。
 */
public final class Money {

    public static final BigDecimal ZERO = new BigDecimal("0.00");
    /** 交通运输服务增值税率 9% */
    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.09");

    private Money() {
    }

    public static BigDecimal scale(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    /** 不含税金额 = 含税 / (1 + 税率) */
    public static BigDecimal netOf(BigDecimal gross, BigDecimal taxRate) {
        BigDecimal rate = taxRate == null ? DEFAULT_TAX_RATE : taxRate;
        BigDecimal divisor = BigDecimal.ONE.add(rate);
        return scale(gross.divide(divisor, 6, RoundingMode.HALF_UP));
    }

    /** 税额 = 含税 - 不含税（差额法） */
    public static BigDecimal taxOf(BigDecimal gross, BigDecimal net) {
        return scale(nz(gross).subtract(nz(net)));
    }

    public static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }
}
