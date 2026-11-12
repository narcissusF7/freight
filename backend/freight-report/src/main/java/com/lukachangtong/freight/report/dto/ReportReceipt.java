package com.lukachangtong.freight.report.dto;

/**
 * 上报回执：监管平台对上传单据的校验结果。
 *
 * 回执是「上报成功与否」的唯一权威依据——不是「HTTP 200 就是成功」，
 * 平台可能返回 200 但报文字段校验失败，所以回执里必须有明确的成功/失败标志与原因。
 */
public class ReportReceipt {

    private boolean success;
    private String messageRefNo;
    /** 回执状态码：0 成功 / 非 0 失败（对齐规范） */
    private String code;
    /** 回执描述（成功确认 / 失败原因） */
    private String message;
    /** 平台返回的回执单号（若平台分配） */
    private String receiptNo;

    public static ReportReceipt ok(String messageRefNo, String receiptNo) {
        ReportReceipt r = new ReportReceipt();
        r.success = true;
        r.messageRefNo = messageRefNo;
        r.code = "0";
        r.message = "上报成功";
        r.receiptNo = receiptNo;
        return r;
    }

    public static ReportReceipt fail(String messageRefNo, String code, String message) {
        ReportReceipt r = new ReportReceipt();
        r.success = false;
        r.messageRefNo = messageRefNo;
        r.code = code;
        r.message = message;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
    public String getMessageRefNo() { return messageRefNo; }
    public void setMessageRefNo(String v) { this.messageRefNo = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String v) { this.receiptNo = v; }
}
