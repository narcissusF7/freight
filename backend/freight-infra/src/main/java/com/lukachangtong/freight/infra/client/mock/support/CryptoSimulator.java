package com.lukachangtong.freight.infra.client.mock.support;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

/**
 * 签章/加密能力本地模拟：用 JDK 自带 JCE 完成 AES-256 与 SHA256withRSA 签名验签闭环。
 * 8u161 之前 Cipher.getMaxAllowedKeyLength("AES") 只有 128，CA 签章会报 Illegal key size；
 * 这里先探测，避免真实 CA SDK 联调时才暴露问题。
 */
public final class CryptoSimulator {

    public static final String SIGN_ALGORITHM = "SHA256withRSA";
    public static final String MOCK_CERT_SERIAL_NO = "MOCK-CA-8U261-0001";

    public static final class Capability {
        private final int maxAesKeyLength;
        private final boolean aes256Available;
        private final boolean signVerifyPassed;
        private final String detail;

        Capability(int maxAesKeyLength, boolean aes256Available, boolean signVerifyPassed, String detail) {
            this.maxAesKeyLength = maxAesKeyLength;
            this.aes256Available = aes256Available;
            this.signVerifyPassed = signVerifyPassed;
            this.detail = detail;
        }

        public boolean isPassed() { return aes256Available && signVerifyPassed; }
        public int getMaxAesKeyLength() { return maxAesKeyLength; }
        public boolean isAes256Available() { return aes256Available; }
        public boolean isSignVerifyPassed() { return signVerifyPassed; }
        public String getDetail() { return detail; }
    }

    private static volatile KeyPair keyPair;

    private CryptoSimulator() {
    }

    public static Capability probe() {
        try {
            int maxLen = Cipher.getMaxAllowedKeyLength("AES");
            boolean aes256 = maxLen >= 256;

            boolean aesRoundTrip = false;
            if (aes256) {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                SecretKey key = kg.generateKey();
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encrypted = cipher.doFinal("freight-probe".getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.DECRYPT_MODE, key);
                String roundTrip = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
                aesRoundTrip = "freight-probe".equals(roundTrip);
            }

            String sig = sign("capability-probe");
            boolean verifyPassed = verify("capability-probe", sig);
            boolean passed = aes256 && aesRoundTrip && verifyPassed;
            return new Capability(maxLen, aes256 && aesRoundTrip, verifyPassed,
                    passed ? "AES-256 and SHA256withRSA round-trip ok" : "crypto capability incomplete");
        } catch (Exception e) {
            return new Capability(-1, false, false,
                    "crypto probe failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static String sign(String content) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(getKeyPair().getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verify(String content, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(getKeyPair().getPublic());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    private static KeyPair getKeyPair() throws Exception {
        KeyPair local = keyPair;
        if (local == null) {
            synchronized (CryptoSimulator.class) {
                local = keyPair;
                if (local == null) {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(2048);
                    local = generator.generateKeyPair();
                    keyPair = local;
                }
            }
        }
        return local;
    }
}
