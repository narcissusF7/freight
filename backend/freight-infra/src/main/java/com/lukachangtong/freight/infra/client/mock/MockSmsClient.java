package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.SmsClient;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.SmsRequest;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地模拟短信网关。
 *
 * 为了让「失败重试」这一整条链路可以真实演示与测试，行为是**确定性**的，不随机会抖动：
 * - 号码含 FAIL：永远失败（演示最终放弃）；
 * - 号码含 RETRY：第 1 次失败、第 2 次成功（演示重试后成功）；
 * - 其他：直接成功，返回可追溯的 MOCK 消息号。
 */
public class MockSmsClient implements SmsClient {

    static final String FAIL_MARK = "FAIL";
    static final String RETRY_MARK = "RETRY";

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<String, AtomicInteger>();

    @Override
    public MessageResult send(SmsRequest request) {
        String phone = request.getPhone() == null ? "" : request.getPhone();
        if (phone.contains(FAIL_MARK)) {
            return MessageResult.fail("mock sms gateway rejected: invalid number");
        }
        if (phone.contains(RETRY_MARK)) {
            AtomicInteger c = attempts.computeIfAbsent(phone, k -> new AtomicInteger());
            if (c.incrementAndGet() == 1) {
                return MessageResult.fail("mock sms gateway timeout (first attempt)");
            }
        }
        return MessageResult.ok("MOCK-SMS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
}
