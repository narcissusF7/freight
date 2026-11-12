package com.lukachangtong.freight.carrier.service.impl;

import com.lukachangtong.freight.api.message.NotifyReceiver;
import com.lukachangtong.freight.api.message.ReceiverAddressPort;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 司机接收地址解析：message 模块需要给司机发短信/微信/推送时，通过本实现取手机号。
 *
 * 为什么不让 message 直接查 driver 表：消息域是通用能力，反向依赖业务域会让
 * 「运单→消息」「消息→承运」形成潜在环路，也会让消息模块无法独立复用。
 */
@Service
public class DriverAddressAdapter implements ReceiverAddressPort {

    @Resource
    private CrDriverMapper driverMapper;

    @Override
    public String resolve(String receiverType, Long receiverId) {
        if (!NotifyReceiver.TYPE_DRIVER.equalsIgnoreCase(receiverType) || receiverId == null) {
            return null;
        }
        CrDriver driver = driverMapper.selectById(receiverId);
        return driver == null ? null : driver.getPhone();
    }
}
