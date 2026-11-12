package com.lukachangtong.freight.infra.client.real;

import com.lukachangtong.freight.infra.client.BankClient;
import com.lukachangtong.freight.infra.client.CaSignClient;
import com.lukachangtong.freight.infra.client.InvoiceClient;
import com.lukachangtong.freight.infra.client.OcrClient;
import com.lukachangtong.freight.infra.client.PushClient;
import com.lukachangtong.freight.infra.client.SmsClient;
import com.lukachangtong.freight.infra.client.WechatClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.InvoiceResult;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentResult;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.MessageResult;
import com.lukachangtong.freight.infra.client.dto.MessageDto.PushRequest;
import com.lukachangtong.freight.infra.client.dto.MessageDto.SmsRequest;
import com.lukachangtong.freight.infra.client.dto.MessageDto.WechatRequest;
import com.lukachangtong.freight.infra.client.dto.OcrDto.OcrResult;

/**
 * 真实 SDK 占位实现（prod 模式）。
 * 任何调用都会立刻抛出明确异常，绝不静默返回假数据 —— 防止把 mock 误带上生产。
 * 接入厂商 SDK 时：在 freight-infra 内新增 Real*Client 实现同样接口，并替换本类的 Bean 装配。
 */
public final class RealClientStub {

    private RealClientStub() {
    }

    private static UnsupportedOperationException notReady(String vendor) {
        return new UnsupportedOperationException(
                vendor + " SDK 尚未对接，禁止在 prod 模式下以模拟数据运行；"
                        + "请先在 freight-infra 内接入真实 SDK 并完成 TLS/签章联调。");
    }

    public static final class Bank implements BankClient {
        @Override public PaymentResult pay(PaymentRequest request) { throw notReady("银企直联"); }
        @Override public PaymentResult queryPayment(String requestId) { throw notReady("银企直联"); }
        @Override public String fetchReconciliationFile(String bizDate) { throw notReady("银企直联"); }
    }

    public static final class Invoice implements InvoiceClient {
        @Override public InvoiceResult issueInvoice(InvoiceRequest request) { throw notReady("电子发票"); }
        @Override public InvoiceResult queryInvoice(String invoiceNo) { throw notReady("电子发票"); }
    }

    public static final class CaSign implements CaSignClient {
        @Override public SignResult sign(SignRequest request) { throw notReady("CA 电子签章"); }
        @Override public boolean verify(String contentHash, String signatureBase64) { throw notReady("CA 电子签章"); }
        @Override public String queryCertExpiry(String certSerialNo) { throw notReady("CA 电子签章"); }
    }

    public static final class Ocr implements OcrClient {
        @Override
        public OcrResult recognize(byte[] imageBytes, String fileName, String type) { throw notReady("证照 OCR"); }
    }

    public static final class Sms implements SmsClient {
        @Override public MessageResult send(SmsRequest request) { throw notReady("短信网关"); }
    }

    public static final class Wechat implements WechatClient {
        @Override public MessageResult send(WechatRequest request) { throw notReady("微信模板消息"); }
    }

    public static final class Push implements PushClient {
        @Override public MessageResult send(PushRequest request) { throw notReady("APP 推送"); }
    }
}
