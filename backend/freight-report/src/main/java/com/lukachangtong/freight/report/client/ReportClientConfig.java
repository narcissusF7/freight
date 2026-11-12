package com.lukachangtong.freight.report.client;

import com.lukachangtong.freight.report.client.ReportClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 上报客户端装配：mock（默认）用本地模拟，real 待真实国密 SDK 联调前替换。
 *
 * 与 infra 的 IntegrationClientConfig 同一套约定：业务只依赖 ReportClient 接口，
 * 切换实现无需改业务代码；prod 必须显式 real，未接入时不会静默降级为假上报。
 */
@Configuration
public class ReportClientConfig {

    @Bean
    @ConditionalOnProperty(name = "freight.report.client.mode", havingValue = "mock", matchIfMissing = true)
    public ReportClient mockReportClient() {
        return new MockReportClient();
    }
}
