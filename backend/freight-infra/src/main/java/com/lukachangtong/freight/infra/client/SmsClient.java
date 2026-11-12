package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.MessageDto.SmsRequest;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;

/** 短信通道抽象（阿里云/腾讯云短信）。业务只依赖本接口，dev/test 走 Mock。 */
public interface SmsClient {

    MessageResult send(SmsRequest request);
}
