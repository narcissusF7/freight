package com.lukachangtong.freight.message.job;

import com.lukachangtong.freight.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息投递调度器。
 *
 * 默认关闭（`freight.message.job.enabled=false`）：单测会加载应用上下文，不能让后台线程抢跑；
 * 生产用 XXL-Job 或打开该开关，接口 `/api/messages/dispatch` 也可手动触发（便于演示与排障）。
 */
@Component
@ConditionalOnProperty(name = "freight.message.job.enabled", havingValue = "true")
public class MessageDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatchJob.class);

    @Resource
    private MessageService messageService;

    @Scheduled(fixedDelayString = "${freight.message.job.dispatch-interval-ms:30000}")
    public void dispatch() {
        try {
            int n = messageService.dispatchDue(200);
            if (n > 0) {
                log.info("消息投递完成：处理 {} 条", n);
            }
        } catch (Exception e) {
            log.error("消息投递任务异常（下轮重试）", e);
        }
    }
}
