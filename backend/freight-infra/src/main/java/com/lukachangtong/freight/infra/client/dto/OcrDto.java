package com.lukachangtong.freight.infra.client.dto;

/**
 * OCR 识别结果（本地模拟阶段）。
 * 真实 OCR 厂商接入后字段保持不变，只替换 OcrClient 实现。
 * 识别结果仅作**表单预填**，不得直接入库：必须经人工核对后由准入审核接口落库，
 * 否则会把误识别数据写进合规档案（大纲 1.2 硬性约束）。
 */
public final class OcrDto {

    private OcrDto() {
    }

    /** 识别类型：身份证明细/驾驶证/行驶证 */
    public static final String TYPE_ID_CARD = "ID_CARD";
    public static final String TYPE_DRIVER_LICENSE = "DRIVER_LICENSE";
    public static final String TYPE_VEHICLE_LICENSE = "VEHICLE_LICENSE";

    public static class OcrResult {
        private boolean success;
        private String type;
        private String name;
        private String idCardNo;
        private String licenseNo;      // 驾驶证号 / 行驶证号
        private String plateNo;        // 车牌号（行驶证）
        private String vehicleType;    // 车辆类型（行驶证）
        private String ownerName;      // 所有人（行驶证）
        private String validFrom;      // yyyy-MM-dd
        private String validTo;        // yyyy-MM-dd，用于证件到期预警
        private double confidence;
        private String rawText;
        /** 证照原件留档（对象存储 key / 访问地址），用于审核与监管核查 */
        private String fileKey;
        private String fileUrl;
        private String message;

        public static OcrResult fail(String type, String message) {
            OcrResult r = new OcrResult();
            r.success = false;
            r.type = type;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getIdCardNo() { return idCardNo; }
        public void setIdCardNo(String v) { this.idCardNo = v; }
        public String getLicenseNo() { return licenseNo; }
        public void setLicenseNo(String v) { this.licenseNo = v; }
        public String getPlateNo() { return plateNo; }
        public void setPlateNo(String v) { this.plateNo = v; }
        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String v) { this.vehicleType = v; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String v) { this.ownerName = v; }
        public String getValidFrom() { return validFrom; }
        public void setValidFrom(String v) { this.validFrom = v; }
        public String getValidTo() { return validTo; }
        public void setValidTo(String v) { this.validTo = v; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double v) { this.confidence = v; }
        public String getRawText() { return rawText; }
        public void setRawText(String v) { this.rawText = v; }
        public String getFileKey() { return fileKey; }
        public void setFileKey(String v) { this.fileKey = v; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String v) { this.fileUrl = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}
