package com.lukachangtong.freight.message.channel;

import com.lukachangtong.freight.infra.client.SmsClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.SmsRequest;
import com.lukachangtong.freight.message.entity.MsgRecord;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 短信通道：真实网关签名/模板报备由 SmsClient 实现内部处理。 */
@Component
public class SmsMessageChannel implements MessageChannel {

    @Resource
    private SmsClient smsClient;

    @Override
    public String channel() {
        return MsgRecord.CHANNEL_SMS;
    }

    @Override
    public MessageResult send(MsgRecord record) {
        SmsRequest req = new SmsRequest();
        req.setPhone(record.getReceiver());
        req.setContent(record.getContent());
        // 用记录 ID 做网关侧幂等键：网络重试不会给司机重复发一条
        req.setRequestId("msg-" + record.getId());
        return smsClient.send(req);
    }
}
