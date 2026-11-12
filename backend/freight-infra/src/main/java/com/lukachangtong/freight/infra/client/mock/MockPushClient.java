package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.PushClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.PushRequest;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 本地模拟 APP 推送，行为约定同 {@link MockSmsClient}（FAIL 永久失败 / RETRY 首次失败）。 */
public class MockPushClient implements PushClient {

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<String, AtomicInteger>();

    @Override
    public MessageResult send(PushRequest request) {
        String target = request.getTarget() == null ? "" : request.getTarget();
        if (target.contains(MockSmsClient.FAIL_MARK)) {
            return MessageResult.fail("mock push rejected: device token invalid");
        }
        if (target.contains(MockSmsClient.RETRY_MARK)) {
            AtomicInteger c = attempts.computeIfAbsent(target, k -> new AtomicInteger());
            if (c.incrementAndGet() == 1) {
                return MessageResult.fail("mock push timeout (first attempt)");
            }
        }
        return MessageResult.ok("MOCK-PUSH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
}
