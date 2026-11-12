package com.lukachangtong.freight.report.dto;

/**
 * 上报报文信封：对齐《网络货运单据报文格式》——
 * Root → Header（报文参考号/报文名称/发送方代码/发送时间/业务接口代码/令牌）+ Body（加密串）。
 *
 * 这是发给监管平台的**最终报文结构**，payload 是加密后的密文；
 * 明文内容在组装阶段由 ReportAssembler 生成，落 rp_report_record.payload 留痕。
 */
public class ReportEnvelope {

    /** 报文参考号（GUID，唯一） */
    private String messageRefNo;
    /** 报文名称：电子运单 / 资金流水单 / 车辆信息单 / 驾驶员信息单 */
    private String documentName;
    /** 报文版本号 */
    private String documentVersion = "1.0";
    /** 发送方代码（省监测系统交换代码） */
    private String senderCode;
    /** 接收方代码 */
    private String receiverCode;
    /** 发送日期时间 YYYYMMDDhhmmss */
    private String sendDateTime;
    /** 业务接口代码：WLHY_YD1001 / LOGINK_CN_FREIGHTCHARGES / ... */
    private String ipcType;
    /** 令牌 */
    private String token;
    /** 业务单据类型（内部用，不随报文传输） */
    private String docType;
    /** 业务单号（内部用） */
    private String bizNo;
    /** 对称密钥加密串（SM2 加密随机 KEY 后的密文） */
    private String encryptedKey;
    /** 单据内容加密串（SM4 加密后的密文） */
    private String encryptedContent;

    public String getMessageRefNo() { return messageRefNo; }
    public void setMessageRefNo(String v) { this.messageRefNo = v; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String v) { this.documentName = v; }
    public String getDocumentVersion() { return documentVersion; }
    public void setDocumentVersion(String v) { this.documentVersion = v; }
    public String getSenderCode() { return senderCode; }
    public void setSenderCode(String v) { this.senderCode = v; }
    public String getReceiverCode() { return receiverCode; }
    public void setReceiverCode(String v) { this.receiverCode = v; }
    public String getSendDateTime() { return sendDateTime; }
    public void setSendDateTime(String v) { this.sendDateTime = v; }
    public String getIpcType() { return ipcType; }
    public void setIpcType(String v) { this.ipcType = v; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public String getDocType() { return docType; }
    public void setDocType(String v) { this.docType = v; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String v) { this.bizNo = v; }
    public String getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(String v) { this.encryptedKey = v; }
    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String v) { this.encryptedContent = v; }
}
