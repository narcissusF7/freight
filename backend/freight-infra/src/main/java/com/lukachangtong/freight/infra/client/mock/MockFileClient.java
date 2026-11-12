package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.FileUploadResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地模拟文件存储：内存暂存，返回 mock:// 前缀的访问地址。
 * 仅用于 dev/test，不落盘、不做真实对象存储。
 */
public class MockFileClient implements FileClient {

    private final ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<String, byte[]>();

    @Override
    public FileUploadResult upload(InputStream data, String fileName, String contentType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = data.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new RuntimeException("mock file read failed", e);
        }
        byte[] bytes = out.toByteArray();
        String key = "mock-" + UUID.randomUUID().toString().replace("-", "");
        store.put(key, bytes);
        return new FileUploadResult(key, fileName, contentType, bytes.length, "mock://file/" + key);
    }

    @Override
    public String getAccessUrl(String fileKey) {
        return "mock://file/" + fileKey;
    }
}
