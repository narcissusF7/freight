package com.lukachangtong.freight.infra.client.mock.support;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * TLS 本地模拟：不连真实银行，只探测当前 JRE 的 SSLContext 能力。
 * 目的：在接真实 SDK 之前，先证明 8u261+ 具备 TLS 1.2/1.3 能力，
 * 避免联调时才发现低版本 JDK 握手失败。
 */
public final class TlsSimulator {

    public static final class HandshakeResult {
        private final boolean tls12Supported;
        private final boolean tls13Supported;
        private final String preferredProtocol;
        private final String supportedProtocols;
        private final String detail;

        HandshakeResult(boolean tls12, boolean tls13, String preferred, String supported, String detail) {
            this.tls12Supported = tls12;
            this.tls13Supported = tls13;
            this.preferredProtocol = preferred;
            this.supportedProtocols = supported;
            this.detail = detail;
        }

        /** 银企直联最低要求 TLS 1.2；TLS 1.3 是加分项 */
        public boolean isPassed() { return tls12Supported; }
        public boolean isTls12Supported() { return tls12Supported; }
        public boolean isTls13Supported() { return tls13Supported; }
        public String getPreferredProtocol() { return preferredProtocol; }
        public String getSupportedProtocols() { return supportedProtocols; }
        public String getDetail() { return detail; }
    }

    private TlsSimulator() {
    }

    public static HandshakeResult probe() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            String[] protocols = context.getSupportedSSLParameters().getProtocols();
            Set<String> set = new HashSet<String>(Arrays.asList(protocols));
            boolean tls12 = set.contains("TLSv1.2");
            boolean tls13 = set.contains("TLSv1.3");
            String preferred = tls13 ? "TLSv1.3" : (tls12 ? "TLSv1.2" : "NONE");
            return new HandshakeResult(tls12, tls13, preferred,
                    Arrays.toString(protocols), "SSLContext TLS probe ok");
        } catch (Exception e) {
            return new HandshakeResult(false, false, "NONE", "[]",
                    "SSLContext 初始化失败: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
