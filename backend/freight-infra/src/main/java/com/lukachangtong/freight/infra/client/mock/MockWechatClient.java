package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.WechatClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.WechatRequest;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 本地模拟微信模板消息，行为约定同 {@link MockSmsClient}（FAIL 永久失败 / RETRY 首次失败）。 */
public class MockWechatClient implements WechatClient {

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<String, AtomicInteger>();

    @Override
    public MessageResult send(WechatRequest request) {
        String openId = request.getOpenId() == null ? "" : request.getOpenId();
        if (openId.contains(MockSmsClient.FAIL_MARK)) {
            return MessageResult.fail("mock wechat api rejected: user not subscribed");
        }
        if (openId.contains(MockSmsClient.RETRY_MARK)) {
            AtomicInteger c = attempts.computeIfAbsent(openId, k -> new AtomicInteger());
            if (c.incrementAndGet() == 1) {
                return MessageResult.fail("mock wechat api timeout (first attempt)");
            }
        }
        return MessageResult.ok("MOCK-WX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
}
