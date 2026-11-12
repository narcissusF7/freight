package com.lukachangtong.freight.waybill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 货源发布（运输管理-发布货源）。 */
@TableName("wb_goods_source")
public class WbGoodsSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("shipper_id")
    private Long shipperId;

    @TableField("goods_name")
    private String goodsName;

    /** 重量（吨） */
    @TableField("weight")
    private BigDecimal weight;

    /** 体积（方） */
    @TableField("volume")
    private BigDecimal volume;

    @TableField("load_addr")
    private String loadAddr;

    /** 装货地纬度（GCJ-02） */
    @TableField("load_lat")
    private BigDecimal loadLat;

    /** 装货地经度（GCJ-02） */
    @TableField("load_lng")
    private BigDecimal loadLng;

    @TableField("unload_addr")
    private String unloadAddr;

    /** 卸货地纬度（GCJ-02） */
    @TableField("unload_lat")
    private BigDecimal unloadLat;

    /** 卸货地经度（GCJ-02） */
    @TableField("unload_lng")
    private BigDecimal unloadLng;

    @TableField("load_time")
    private LocalDateTime loadTime;

    /** 运费报价（元） */
    @TableField("freight_price")
    private BigDecimal freightPrice;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("remark")
    private String remark;

    /** 0 待承运 1 已生成运单 2 关闭 */
    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public String getLoadAddr() { return loadAddr; }
    public void setLoadAddr(String loadAddr) { this.loadAddr = loadAddr; }
    public BigDecimal getLoadLat() { return loadLat; }
    public void setLoadLat(BigDecimal loadLat) { this.loadLat = loadLat; }
    public BigDecimal getLoadLng() { return loadLng; }
    public void setLoadLng(BigDecimal loadLng) { this.loadLng = loadLng; }
    public String getUnloadAddr() { return unloadAddr; }
    public void setUnloadAddr(String unloadAddr) { this.unloadAddr = unloadAddr; }
    public BigDecimal getUnloadLat() { return unloadLat; }
    public void setUnloadLat(BigDecimal unloadLat) { this.unloadLat = unloadLat; }
    public BigDecimal getUnloadLng() { return unloadLng; }
    public void setUnloadLng(BigDecimal unloadLng) { this.unloadLng = unloadLng; }
    public LocalDateTime getLoadTime() { return loadTime; }
    public void setLoadTime(LocalDateTime loadTime) { this.loadTime = loadTime; }
    public BigDecimal getFreightPrice() { return freightPrice; }
    public void setFreightPrice(BigDecimal freightPrice) { this.freightPrice = freightPrice; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
