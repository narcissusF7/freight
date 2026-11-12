package com.lukachangtong.freight;

import com.lukachangtong.freight.infra.client.BankClient;
import com.lukachangtong.freight.infra.client.CaSignClient;
import com.lukachangtong.freight.infra.client.InvoiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自包含上下文加载冒烟测试。
 * 通过 application-unittest.yml 的 spring.autoconfigure.exclude 关闭 Redis / Redisson，
 * 用 H2 内存库 + 关闭 Flyway，仅验证 Web + MyBatis + 外部集成 mock 装配链路。
 */
@SpringBootTest
@ActiveProfiles("unittest")
class FreightApplicationTests {

    @Resource
    private BankClient bankClient;
    @Resource
    private InvoiceClient invoiceClient;
    @Resource
    private CaSignClient caSignClient;

    @Test
    void contextLoads() {
        assertThat(bankClient).isNotNull();
        assertThat(invoiceClient).isNotNull();
        assertThat(caSignClient).isNotNull();
    }
}
