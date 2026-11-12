package com.lukachangtong.freight.message.channel;

import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.message.entity.MsgRecord;

/**
 * 通道适配器。四种通道（短信/微信/APP 推送/站内信）实现同一接口，
 * 调度器不关心差异；新增通道（如语音外呼）只需增加实现类。
 */
public interface MessageChannel {

    /** 与 msg_record.channel 取值一致 */
    String channel();

    MessageResult send(MsgRecord record);
}
