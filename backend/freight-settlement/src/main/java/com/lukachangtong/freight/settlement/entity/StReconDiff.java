package com.lukachangtong.freight.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 对账差异明细。每条差异可被人工标记处理并留痕，
 * 这是财务月结时唯一需要人看的地方。
 */
@TableName("st_recon_diff")
public class StReconDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_SETTLE_VS_PAY = "SETTLE_VS_PAY";
    public static final String TYPE_PAY_VS_FLOW = "PAY_VS_FLOW";
    public static final String TYPE_WALLET_BALANCE = "WALLET_BALANCE";
    public static final String TYPE_BANK_FILE = "BANK_FILE";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("recon_id")
    private Long reconId;

    /** SETTLE_VS_PAY / PAY_VS_FLOW / WALLET_BALANCE / BANK_FILE */
    @TableField("diff_type")
    private String diffType;

    @TableField("biz_no")
    private String bizNo;

    @TableField("expect_amount")
    private BigDecimal expectAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("diff_amount")
    private BigDecimal diffAmount;

    @TableField("remark")
    private String remark;

    @TableField("handled")
    private Integer handled;

    @TableField("handle_by")
    private String handleBy;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReconId() { return reconId; }
    public void setReconId(Long reconId) { this.reconId = reconId; }
    public String getDiffType() { return diffType; }
    public void setDiffType(String diffType) { this.diffType = diffType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public BigDecimal getExpectAmount() { return expectAmount; }
    public void setExpectAmount(BigDecimal expectAmount) { this.expectAmount = expectAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getDiffAmount() { return diffAmount; }
    public void setDiffAmount(BigDecimal diffAmount) { this.diffAmount = diffAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getHandled() { return handled; }
    public void setHandled(Integer handled) { this.handled = handled; }
    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String handleBy) { this.handleBy = handleBy; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
