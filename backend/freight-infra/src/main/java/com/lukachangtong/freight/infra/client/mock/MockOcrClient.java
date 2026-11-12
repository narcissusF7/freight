package com.lukachangtong.freight.infra.client.mock;

import com.lukachangtong.freight.infra.client.OcrClient;
import com.lukachangtong.freight.infra.client.dto.OcrDto;
import com.lukachangtong.freight.infra.client.dto.OcrDto.OcrResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 证照 OCR 本地模拟。
 * 不是返回固定假串：按图片字节的 SHA-256 派生字段，**同样图片必然得到同样结果**，
 * 便于前端演示与集成测试断言（换一张图字段就变，不会出现"永远同一个人"的假象）。
 */
public class MockOcrClient implements OcrClient {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Override
    public OcrResult recognize(byte[] imageBytes, String fileName, String type) {
        if (imageBytes == null || imageBytes.length == 0) {
            return OcrResult.fail(type, "图片内容为空，无法识别");
        }
        String digest = sha256Hex(imageBytes);
        OcrResult r = new OcrResult();
        r.setSuccess(true);
        r.setType(type);
        r.setConfidence(0.95);
        // 有效期：默认自签发起两年，测试可通过文件名前缀 "-expiring" 拿到 20 天后到期的证件
        LocalDate validTo = fileName != null && fileName.toLowerCase().contains("expiring")
                ? LocalDate.now().plusDays(20)
                : LocalDate.now().plusYears(2);
        r.setValidFrom(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        r.setValidTo(validTo.format(DateTimeFormatter.ISO_LOCAL_DATE));

        if (OcrDto.TYPE_VEHICLE_LICENSE.equals(type)) {
            r.setPlateNo("闽D" + digits(digest, 0, 5));
            r.setVehicleType("重型半挂牵引车");
            r.setOwnerName("厦门鹭卡畅通运输有限公司");
            r.setLicenseNo("XZ" + digits(digest, 5, 10));
        } else if (OcrDto.TYPE_DRIVER_LICENSE.equals(type)) {
            r.setName("OCR司机" + digits(digest, 0, 4));
            r.setLicenseNo("DL" + digits(digest, 4, 10));
            r.setIdCardNo("3502" + digits(digest, 14, 10) + digits(digest, 24, 4));
        } else {
            r.setName("OCR司机" + digits(digest, 0, 4));
            r.setIdCardNo("3502" + digits(digest, 14, 10) + digits(digest, 24, 4));
        }
        r.setRawText("mock-ocr type=" + type + " file=" + fileName + " digest=" + digest.substring(0, 16));
        r.setMessage("mock ocr succeeded");
        return r;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(data);
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(HEX[(b >> 4) & 0x0f]).append(HEX[b & 0x0f]);
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 是 JDK 必备算法，不会发生；退化用长度保证不 NPE
            return Integer.toHexString(new String(data, StandardCharsets.UTF_8).length());
        }
    }

    /** 从哈希里取 [from, from+len) 区间的字符并映射成数字 */
    private static String digits(String digest, int from, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = digest.charAt((from + i) % digest.length());
            int v = Character.digit(c, 16);
            sb.append((char) ('0' + Math.floorMod(v, 10)));
        }
        return sb.toString();
    }
}
