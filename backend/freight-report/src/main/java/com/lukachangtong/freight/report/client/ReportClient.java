package com.lukachangtong.freight.report.client;

import com.lukachangtong.freight.report.dto.ReportEnvelope;
import com.lukachangtong.freight.report.dto.ReportReceipt;

/**
 * 监管上报客户端抽象：向部/省网络货运监测平台上传 4 类单据。
 *
 * 业务模块只依赖本接口，真实平台接入时替换实现即可（与 Bank/Invoice/CaSign 同一套约定）。
 * 关键约束：
 * 1. **令牌**：先身份验证换取令牌（规范有效期 2 小时），令牌由调用方缓存并传入；
 * 2. **幂等**：messageReferenceNumber 是报文唯一标识，同一报文号重复上传必须返回首次结果；
 * 3. **国密加密**：报文头/正文按规范用 SM2/SM4 加密，本地 mock 用 CryptoSimulator 模拟闭环。
 */
public interface ReportClient {

    /** 身份验证：换取令牌。规范要求令牌有效期 2 小时，过期需重取。 */
    String authenticate(String senderCode, String authEndpoint);

    /** 上传单据报文，返回回执。envelope 含完整报文（header + 加密 body）。 */
    ReportReceipt upload(ReportEnvelope envelope, String token);
}
