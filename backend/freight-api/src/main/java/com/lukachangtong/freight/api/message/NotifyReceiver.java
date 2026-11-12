package com.lukachangtong.freight.api.message;

import java.io.Serializable;

/**
 * 消息接收方。业务侧只说「发给谁」，具体地址（手机号/openId/设备号）由 message 模块
 * 通过 {@link ReceiverAddressPort} 解析——业务模块不需要知道渠道细节。
 */
public class NotifyReceiver implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_DRIVER = "DRIVER";
    public static final String TYPE_SHIPPER = "SHIPPER";
    public static final String TYPE_EMPLOYEE = "EMPLOYEE";
    /** 机构级收件箱（站内信），id 为 orgId */
    public static final String TYPE_ORG = "ORG";

    private String type;
    private Long id;
    /** 直接给地址时优先用它（测试/外部号码场景），留空则走解析端口 */
    private String address;
    private String name;

    public NotifyReceiver() {
    }

    public NotifyReceiver(String type, Long id) {
        this.type = type;
        this.id = id;
    }

    public NotifyReceiver(String type, Long id, String address) {
        this.type = type;
        this.id = id;
        this.address = address;
    }

    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
