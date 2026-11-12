package com.lukachangtong.freight.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。traceId 由 TraceIdFilter 写入 MDC，便于 ELK 串链路。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("business error: code={}, msg={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.<Void>fail(e.getErrorCode(), e.getMessage()).withTraceId(MDC.get("traceId"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        // 不把堆栈与内部细节返回给前端，只给 traceId 供排查
        log.error("unexpected error", e);
        return Result.<Void>fail(ErrorCode.INTERNAL_ERROR).withTraceId(MDC.get("traceId"));
    }
}
