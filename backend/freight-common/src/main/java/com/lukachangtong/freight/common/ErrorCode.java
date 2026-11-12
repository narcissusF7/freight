package com.lukachangtong.freight.common;

/**
 * 全局错误码。前两位区分域：A=接入/权限，B=业务，C=外部集成，Z=系统。
 */
public enum ErrorCode {

    OK("00000", "成功"),
    PARAM_INVALID("A0001", "参数校验失败"),
    UNAUTHORIZED("A0002", "未登录或令牌失效"),
    FORBIDDEN("A0003", "无权限访问"),
    IDEMPOTENT_CONFLICT("B0001", "重复请求"),
    ILLEGAL_STATE_TRANSFER("B0002", "非法的运单状态流转"),
    INTEGRATION_MOCK_ONLY("C0001", "外部 SDK 未对接，当前仅本地模拟"),
    INTEGRATION_FAILED("C0002", "外部集成调用失败"),
    INTERNAL_ERROR("Z9999", "系统内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
