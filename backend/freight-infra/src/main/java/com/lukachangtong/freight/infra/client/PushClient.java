package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.MessageDto.PushRequest;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;

/** APP 推送通道抽象（厂商通道 / 个推、极光等聚合推送）。 */
public interface PushClient {

    MessageResult send(PushRequest request);
}
