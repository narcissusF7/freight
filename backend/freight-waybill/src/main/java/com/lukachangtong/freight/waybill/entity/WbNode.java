package com.lukachangtong.freight.waybill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运单状态变更节点（只追加）。
 *
 * 除状态跃迁外，还承载「打点类」事件（到达装货地 / 到达卸货地）——这类事件不改变运单状态，
 * 但必须留痕，否则无法证明司机确实到过厂区。定位、照片、IP 是事后核查的凭证。
 */
@TableName("wb_node")
public class WbNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("waybill_id")
    private Long waybillId;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operate_by")
    private String operateBy;

    @TableField("operate_time")
    private LocalDateTime operateTime;

    @TableField("remark")
    private String remark;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("address")
    private String address;

    /** 距装/卸货地参考点距离（米），用于校验打点真实性 */
    @TableField("distance_m")
    private Integer distanceM;

    /** 凭证图片，逗号分隔 */
    @TableField("evidence_urls")
    private String evidenceUrls;

    /** WEB / DRIVER_APP / SYSTEM */
    @TableField("source")
    private String source;

    @TableField("client_ip")
    private String clientIp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWaybillId() { return waybillId; }
    public void setWaybillId(Long waybillId) { this.waybillId = waybillId; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getOperateBy() { return operateBy; }
    public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
