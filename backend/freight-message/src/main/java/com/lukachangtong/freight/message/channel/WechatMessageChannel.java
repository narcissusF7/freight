package com.lukachangtong.freight.message.channel;

import com.lukachangtong.freight.infra.client.WechatClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.WechatRequest;
import com.lukachangtong.freight.message.entity.MsgRecord;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 微信通道：公众号模板消息 / 小程序订阅消息，openId 作为接收地址。 */
@Component
public class WechatMessageChannel implements MessageChannel {

    @Resource
    private WechatClient wechatClient;

    @Override
    public String channel() {
        return MsgRecord.CHANNEL_WECHAT;
    }

    @Override
    public MessageResult send(MsgRecord record) {
        WechatRequest req = new WechatRequest();
        req.setOpenId(record.getReceiver());
        req.setTemplateCode(record.getTemplateCode());
        req.setContent(record.getContent());
        req.setRequestId("msg-" + record.getId());
        return wechatClient.send(req);
    }
}
