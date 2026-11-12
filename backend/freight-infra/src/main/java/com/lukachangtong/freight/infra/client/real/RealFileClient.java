package com.lukachangtong.freight.infra.client.real;

import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.FileUploadResult;

import java.io.InputStream;

/**
 * 真实文件存储桩：待接入 OBS / MinIO SDK 后实现。
 * 未接入时调用直接抛异常，避免静默降级为模拟数据上线。
 */
public class RealFileClient implements FileClient {

    @Override
    public FileUploadResult upload(InputStream data, String fileName, String contentType) {
        throw new UnsupportedOperationException(
                "FileClient real SDK not integrated; please implement OBS/MinIO adapter in freight-infra");
    }

    @Override
    public String getAccessUrl(String fileKey) {
        throw new UnsupportedOperationException(
                "FileClient real SDK not integrated; please implement OBS/MinIO adapter in freight-infra");
    }
}
