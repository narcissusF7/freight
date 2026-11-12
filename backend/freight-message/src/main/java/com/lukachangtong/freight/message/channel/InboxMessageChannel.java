package com.lukachangtong.freight.message.channel;

import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.message.entity.MsgRecord;
import org.springframework.stereotype.Component;

/**
 * 站内信通道：没有外部网关，落库即可见，因此投递恒成功。
 * 仍然走统一调度链路，好处是四种通道的状态机、失败重试、统计口径完全一致。
 */
@Component
public class InboxMessageChannel implements MessageChannel {

    @Override
    public String channel() {
        return MsgRecord.CHANNEL_INBOX;
    }

    @Override
    public MessageResult send(MsgRecord record) {
        return MessageResult.ok("INBOX-" + record.getId());
    }
}
