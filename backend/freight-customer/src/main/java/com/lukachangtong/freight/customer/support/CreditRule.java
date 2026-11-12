package com.lukachangtong.freight.customer.support;

/**
 * 信用规则。全部集中在这里，避免「差评扣几分」散落在各个 Service 里各写一遍。
 *
 * 口径：
 * - 初始 100 分，区间 0~100；
 * - 等级：A ≥ 90，B ≥ 75，C ≥ 60，D < 60；
 * - 责任成立的投诉 -10，差评 -5，好评 +2，中评 0（但留一条 0 分记录，证明这条评价计过数）。
 *
 * 为什么好评只加 2 分、差评却扣 5 分：信用分的作用是筛风险，不是发奖状。
 * 上浮太容易会让「刷好评把分拉回 100」变成低成本操作，扣分权重必须大于加分。
 */
public final class CreditRule {

    public static final int BASE_SCORE = 100;
    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;

    public static final int DELTA_COMPLAINT_CONFIRMED = -10;
    public static final int DELTA_RATING_GOOD = 2;
    public static final int DELTA_RATING_NEUTRAL = 0;
    public static final int DELTA_RATING_BAD = -5;

    private CreditRule() {
    }

    public static int clamp(int score) {
        if (score < MIN_SCORE) {
            return MIN_SCORE;
        }
        return Math.min(score, MAX_SCORE);
    }

    public static String levelOf(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 75) {
            return "B";
        }
        if (score >= 60) {
            return "C";
        }
        return "D";
    }

    public static boolean isGood(int star) {
        return star >= 4;
    }

    public static boolean isBad(int star) {
        return star <= 2;
    }
}
