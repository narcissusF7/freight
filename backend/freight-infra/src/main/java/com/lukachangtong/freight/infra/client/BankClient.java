package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.PaymentResult;

/**
 * 银企直联客户端抽象。业务模块只依赖本接口，真实 SDK 接入时替换实现即可。
 * 所有方法必须以 requestId 做幂等；金额由服务端计算，不信前端传值。
 */
public interface BankClient {

    PaymentResult pay(PaymentRequest request);

    PaymentResult queryPayment(String requestId);

    /** 返回日终对账文件内容（真实环境为银行下发的对账文件） */
    String fetchReconciliationFile(String bizDate);
}
