package com.lukachangtong.freight.infra.client;

import java.io.InputStream;

/**
 * 文件存储抽象。业务只依赖此接口，真实实现由 OBS / MinIO SDK 提供；
 * dev/test 走 MockFileClient，prod 接真实 SDK。切换不改动业务代码。
 */
public interface FileClient {

    FileUploadResult upload(InputStream data, String fileName, String contentType);

    String getAccessUrl(String fileKey);
}
