package com.lukachangtong.freight.common;

/**
 * 统一响应体。所有对外接口必须返回该结构，前端按 code 做统一拦截。
 */
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<T>();
        r.code = ErrorCode.OK.getCode();
        r.message = ErrorCode.OK.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String detail) {
        Result<T> r = new Result<T>();
        r.code = errorCode.getCode();
        r.message = detail == null || detail.length() == 0 ? errorCode.getMessage() : detail;
        return r;
    }

    public Result<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
