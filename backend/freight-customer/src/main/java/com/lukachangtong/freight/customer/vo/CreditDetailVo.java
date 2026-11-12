package com.lukachangtong.freight.customer.vo;

import com.lukachangtong.freight.customer.entity.CsCredit;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import java.util.List;

/**
 * 信用详情：当前分值 + 变动流水（可逐条追溯到投诉单/评价单）。
 */
public class CreditDetailVo {

    private CsCreditSubject subject;
    private List<CsCredit> flows = new java.util.ArrayList<CsCredit>();

    public CsCreditSubject getSubject() { return subject; }
    public void setSubject(CsCreditSubject subject) { this.subject = subject; }
    public List<CsCredit> getFlows() { return flows; }
    public void setFlows(List<CsCredit> flows) { this.flows = flows; }
}
