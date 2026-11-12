package com.lukachangtong.freight.web.dto;

/** APP 推送 clientId 登记（uni-push / 厂商通道）。 */
public class DriverPushRequest {

    private String clientId;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}
