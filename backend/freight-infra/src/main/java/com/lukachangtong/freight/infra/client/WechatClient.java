package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.MessageDto.WechatRequest;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;

/** 微信通道抽象（公众号模板消息 / 小程序订阅消息）。 */
public interface WechatClient {

    MessageResult send(WechatRequest request);
}
