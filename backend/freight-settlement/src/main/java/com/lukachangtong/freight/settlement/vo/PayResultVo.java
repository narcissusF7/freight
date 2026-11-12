package com.lukachangtong.freight.settlement.vo;

import com.lukachangtong.freight.payment.entity.StPaymentOrder;
import com.lukachangtong.freight.settlement.entity.StSettlement;

/** 发起支付的结果：结算单最新状态 + 支付单。前端据此提示「已到账 / 银行拒绝」。 */
public class PayResultVo {

    private StSettlement settlement;
    private StPaymentOrder payment;
    private String message;

    public PayResultVo() {
    }

    public PayResultVo(StSettlement settlement, StPaymentOrder payment, String message) {
        this.settlement = settlement;
        this.payment = payment;
        this.message = message;
    }

    public StSettlement getSettlement() { return settlement; }
    public void setSettlement(StSettlement settlement) { this.settlement = settlement; }
    public StPaymentOrder getPayment() { return payment; }
    public void setPayment(StPaymentOrder payment) { this.payment = payment; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
