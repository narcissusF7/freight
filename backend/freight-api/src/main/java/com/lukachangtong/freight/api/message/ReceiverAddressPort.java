package com.lukachangtong.freight.api.message;

/**
 * 接收方地址解析端口（依赖倒置）。
 *
 * message 模块是通用基础设施，**不能依赖 carrier/auth** 去查司机手机号或员工 openId，
 * 否则消息域会反向依赖业务域。因此在 api 模块定义端口，由 carrier/auth 提供实现：
 * 返回 null 表示该接收方没有可用地址，message 侧会落一条「未找到接收地址」的终态记录，不进入重试。
 */
public interface ReceiverAddressPort {

    /**
     * @param receiverType {@link NotifyReceiver#TYPE_DRIVER} 等
     * @return 手机号 / openId / 设备号；无则 null
     */
    String resolve(String receiverType, Long receiverId);
}
