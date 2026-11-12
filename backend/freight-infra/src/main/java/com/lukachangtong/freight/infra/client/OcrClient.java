package com.lukachangtong.freight.infra.client;

import com.lukachangtong.freight.infra.client.dto.OcrDto.OcrResult;

/**
 * 证照 OCR 识别抽象：身份证 / 驾驶证 / 行驶证。
 * 结果只用于前端表单预填，落库前必须人工核对（见 OcrDto 注释）。
 * dev/test 走 MockOcrClient（按图片内容哈希生成确定性字段，可复现、可断言）；
 * prod 未接真实厂商时抛明确异常，不静默返回假数据。
 */
public interface OcrClient {

    /**
     * @param imageBytes 证照图片原始字节
     * @param fileName   原始文件名（用于判定类型/日志）
     * @param type       {@link com.lukachangtong.freight.infra.client.dto.OcrDto} 的 TYPE_* 常量
     */
    OcrResult recognize(byte[] imageBytes, String fileName, String type);
}
