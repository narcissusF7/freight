package com.lukachangtong.freight.infra.config;

import com.lukachangtong.freight.infra.client.BankClient;
import com.lukachangtong.freight.infra.client.CaSignClient;
import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.InvoiceClient;
import com.lukachangtong.freight.infra.client.OcrClient;
import com.lukachangtong.freight.infra.client.PushClient;
import com.lukachangtong.freight.infra.client.SmsClient;
import com.lukachangtong.freight.infra.client.WechatClient;
import com.lukachangtong.freight.infra.client.mock.MockBankClient;
import com.lukachangtong.freight.infra.client.mock.MockCaSignClient;
import com.lukachangtong.freight.infra.client.mock.MockFileClient;
import com.lukachangtong.freight.infra.client.mock.MockInvoiceClient;
import com.lukachangtong.freight.infra.client.mock.MockOcrClient;
import com.lukachangtong.freight.infra.client.mock.MockPushClient;
import com.lukachangtong.freight.infra.client.mock.MockSmsClient;
import com.lukachangtong.freight.infra.client.mock.MockWechatClient;
import com.lukachangtong.freight.infra.client.real.RealClientStub;
import com.lukachangtong.freight.infra.client.real.RealFileClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 外部集成装配开关：
 * - dev / test：freight.integration.mode=mock，使用本地模拟实现（默认，未配置时也走 mock）
 * - prod：必须显式 real；真实 SDK 未接入时调用会抛明确异常，不会静默降级为假数据
 *
 * 业务模块只依赖 BankClient/InvoiceClient/CaSignClient 接口，切换实现无需改业务代码。
 */
@Configuration
public class IntegrationClientConfig {

    private static final String MODE = "freight.integration.mode";

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public BankClient mockBankClient() {
        return new MockBankClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public InvoiceClient mockInvoiceClient() {
        return new MockInvoiceClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public CaSignClient mockCaSignClient() {
        return new MockCaSignClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public BankClient realBankClient() {
        return new RealClientStub.Bank();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public InvoiceClient realInvoiceClient() {
        return new RealClientStub.Invoice();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public CaSignClient realCaSignClient() {
        return new RealClientStub.CaSign();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public FileClient mockFileClient() {
        return new MockFileClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public FileClient realFileClient() {
        return new RealFileClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public OcrClient mockOcrClient() {
        return new MockOcrClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public OcrClient realOcrClient() {
        return new RealClientStub.Ocr();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public SmsClient mockSmsClient() {
        return new MockSmsClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public SmsClient realSmsClient() {
        return new RealClientStub.Sms();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public WechatClient mockWechatClient() {
        return new MockWechatClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public WechatClient realWechatClient() {
        return new RealClientStub.Wechat();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "mock", matchIfMissing = true)
    public PushClient mockPushClient() {
        return new MockPushClient();
    }

    @Bean
    @ConditionalOnProperty(name = MODE, havingValue = "real")
    public PushClient realPushClient() {
        return new RealClientStub.Push();
    }
}
