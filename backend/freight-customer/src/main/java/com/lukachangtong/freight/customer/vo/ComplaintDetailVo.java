package com.lukachangtong.freight.customer.vo;

import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.entity.CsHandle;
import java.util.List;

/**
 * 投诉详情：单据 + 处理过程 + SLA 结论。
 *
 * `remainHours` 为负数表示已逾期——前端不该自己算时区，逾期与否由服务端给结论。
 */
public class ComplaintDetailVo {

    private CsComplaint complaint;
    /** 处理过程 */
    private List<CsHandle> handles = new java.util.ArrayList<CsHandle>();
    /** 距截止还剩小时数（负 = 已逾期） */
    private Long remainHours;
    /** 是否超时 */
    private boolean slaBreached = false;

    public CsComplaint getComplaint() { return complaint; }
    public void setComplaint(CsComplaint complaint) { this.complaint = complaint; }
    public List<CsHandle> getHandles() { return handles; }
    public void setHandles(List<CsHandle> handles) { this.handles = handles; }
    public Long getRemainHours() { return remainHours; }
    public void setRemainHours(Long remainHours) { this.remainHours = remainHours; }
    public boolean getSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
}
