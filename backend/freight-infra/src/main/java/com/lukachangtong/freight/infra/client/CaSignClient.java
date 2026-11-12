package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignResult;

/**
 * CA 电子签章客户端抽象：托运框架协议、司机运输合同签章。
 * 签章前必须确认 JDK 8 的 JCE 无限强度加密可用（8u161+）。
 */
public interface CaSignClient {

    SignResult sign(SignRequest request);

    boolean verify(String contentHash, String signatureBase64);

    /** 返回证书到期日 yyyy-MM-dd，供证件到期预警使用 */
    String queryCertExpiry(String certSerialNo);
}
