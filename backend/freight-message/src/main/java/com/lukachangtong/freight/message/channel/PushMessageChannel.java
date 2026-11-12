package com.lukachangtong.freight.message.channel;

import com.lukachangtong.freight.infra.client.PushClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.PushRequest;
import com.lukachangtong.freight.message.entity.MsgRecord;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** APP 推送通道：司机端设备 token 作为接收地址。 */
@Component
public class PushMessageChannel implements MessageChannel {

    @Resource
    private PushClient pushClient;

    @Override
    public String channel() {
        return MsgRecord.CHANNEL_PUSH;
    }

    @Override
    public MessageResult send(MsgRecord record) {
        PushRequest req = new PushRequest();
        req.setTarget(record.getReceiver());
        req.setTitle(record.getTitle());
        req.setContent(record.getContent());
        req.setRequestId("msg-" + record.getId());
        return pushClient.send(req);
    }
}
