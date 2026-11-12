package com.lukachangtong.freight.customer.vo;

import java.util.List;

/**
 * 评价汇总：单个被评价方的分数分布。
 *
 * `distribution` 固定长度 5，下标 0 对应 1 星，前端直接画柱状条，不用再做聚合。
 */
public class RatingSummaryVo {

    private String targetType;
    private Long targetId;
    private String targetName;
    private long count = 0;
    private double avgScore = 0;
    /** 好评率（≥4 星） */
    private double goodRate = 0;
    private long badCount = 0;
    /** 1~5 星各自数量 */
    private long[] distribution = new long[5];

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public double getAvgScore() { return avgScore; }
    public void setAvgScore(double avgScore) { this.avgScore = avgScore; }
    public double getGoodRate() { return goodRate; }
    public void setGoodRate(double goodRate) { this.goodRate = goodRate; }
    public long getBadCount() { return badCount; }
    public void setBadCount(long badCount) { this.badCount = badCount; }
    public long[] getDistribution() { return distribution; }
    public void setDistribution(long[] distribution) { this.distribution = distribution; }
}
