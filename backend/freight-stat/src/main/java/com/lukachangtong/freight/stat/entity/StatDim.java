package com.lukachangtong.freight.stat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.io.Serializable;

/**
 * 统计维度聚合（地区 / 货物 / 司机）。
 *
 * 维度值单独成表而不是塞进宽表：维度是「可枚举但会增长」的（省份固定 34 个，
 * 货物名与司机却会一直加），主表列数不能跟着业务数据长。
 */
@TableName("stat_dim")
public class StatDim implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DIM_LOAD_PROVINCE = "LOAD_PROVINCE";
    public static final String DIM_UNLOAD_PROVINCE = "UNLOAD_PROVINCE";
    public static final String DIM_CARGO = "CARGO";
    public static final String DIM_DRIVER = "DRIVER";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_date")
    private LocalDate bizDate;

    @TableField("org_id")
    private Long orgId;

    /** LOAD_PROVINCE / UNLOAD_PROVINCE / CARGO / DRIVER */
    @TableField("dim_type")
    private String dimType;

    /** 维度值（省份名/货物名/司机ID） */
    @TableField("dim_key")
    private String dimKey;

    /** 展示名 */
    @TableField("dim_label")
    private String dimLabel;

    @TableField("waybill_count")
    private Integer waybillCount;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("complaint_count")
    private Integer complaintCount;

    @TableField("alarm_count")
    private Integer alarmCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getDimType() { return dimType; }
    public void setDimType(String dimType) { this.dimType = dimType; }
    public String getDimKey() { return dimKey; }
    public void setDimKey(String dimKey) { this.dimKey = dimKey; }
    public String getDimLabel() { return dimLabel; }
    public void setDimLabel(String dimLabel) { this.dimLabel = dimLabel; }
    public Integer getWaybillCount() { return waybillCount; }
    public void setWaybillCount(Integer waybillCount) { this.waybillCount = waybillCount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getComplaintCount() { return complaintCount; }
    public void setComplaintCount(Integer complaintCount) { this.complaintCount = complaintCount; }
    public Integer getAlarmCount() { return alarmCount; }
    public void setAlarmCount(Integer alarmCount) { this.alarmCount = alarmCount; }
}
