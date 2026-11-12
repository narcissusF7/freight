package com.lukachangtong.freight.carrier.vo;

import java.time.LocalDate;

/**
 * 证件到期预警行。
 * 监管与保险要求「人车证照持续有效」，过期车辆/司机继续承运属于重大合规风险，
 * 因此预警按剩余天数分级：EXPIRED 已过期 / URGENT 30 天内 / WARN 预警窗口内。
 */
public class ExpiryAlertVo {

    /** DRIVER / VEHICLE */
    private String bizType;
    private Long bizId;
    /** 司机姓名或车牌号 */
    private String bizName;
    /** 证件类型：驾驶证 / 身份证 / 道路运输证 / 保险 */
    private String docType;
    private String docNo;
    private LocalDate expireDate;
    private long daysLeft;
    /** EXPIRED / URGENT / WARN */
    private String level;

    public String getBizType() { return bizType; }
    public void setBizType(String v) { this.bizType = v; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long v) { this.bizId = v; }
    public String getBizName() { return bizName; }
    public void setBizName(String v) { this.bizName = v; }
    public String getDocType() { return docType; }
    public void setDocType(String v) { this.docType = v; }
    public String getDocNo() { return docNo; }
    public void setDocNo(String v) { this.docNo = v; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate v) { this.expireDate = v; }
    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long v) { this.daysLeft = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
}
