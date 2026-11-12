package com.lukachangtong.freight.common;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * 单体进程内用 traceId 替代 SkyWalking。未来拆微服务时该值直接作为链路 ID 透传。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String incoming = null;
        if (request instanceof HttpServletRequest) {
            incoming = ((HttpServletRequest) request).getHeader(HEADER_TRACE_ID);
        }
        String traceId = (incoming == null || incoming.trim().isEmpty())
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming.trim();
        MDC.put(TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
