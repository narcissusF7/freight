package com.lukachangtong.freight.waybill.dto;

import java.math.BigDecimal;
import java.util.List;

/** 司机端节点确认入参。动作由服务端映射状态，前端不能直接提交目标状态。 */
public class DriverNodeConfirmRequest {

    /** ARRIVE_LOAD / LOADED / DEPART / ARRIVE_UNLOAD / UNLOADED / SIGNED */
    private String action;

    /** 定位（GCJ-02）。到厂类动作必填，用于校验是否真的在装卸货地附近。 */
    private BigDecimal lat;
    private BigDecimal lng;

    /** 逆地址（前端可选传，便于人工核对） */
    private String address;

    /** 凭证图片地址（装卸货照片、签收单） */
    private List<String> photoUrls;

    private String remark;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
