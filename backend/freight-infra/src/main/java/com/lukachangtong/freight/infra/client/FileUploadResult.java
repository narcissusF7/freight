package com.lukachangtong.freight.infra.client;

/** 文件上传结果；只返回对象存储 key 与访问地址，绝不返回公网直链长期暴露。 */
public class FileUploadResult {

    private String fileKey;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String url;

    public FileUploadResult() {}

    public FileUploadResult(String fileKey, String fileName, String contentType, long sizeBytes, String url) {
        this.fileKey = fileKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.url = url;
    }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
