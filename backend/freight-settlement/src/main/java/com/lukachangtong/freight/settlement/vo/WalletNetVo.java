package com.lukachangtong.freight.settlement.vo;

import java.math.BigDecimal;

/** 按钱包汇总的流水净额（IN 为正，OUT 为负），对账时与钱包余额比对。 */
public class WalletNetVo {

    private Long walletId;
    private BigDecimal net;

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
}
