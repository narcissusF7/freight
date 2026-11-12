package com.lukachangtong.freight.report.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.report.dto.ReportEnvelope;
import com.lukachangtong.freight.report.entity.RpConfig;
import com.lukachangtong.freight.report.entity.RpReportRecord;
import com.lukachangtong.freight.report.vo.DriverReportRow;
import com.lukachangtong.freight.report.vo.FundFlowReportRow;
import com.lukachangtong.freight.report.vo.VehicleReportRow;
import com.lukachangtong.freight.report.vo.WaybillReportRow;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 报文组装器：把业务数据视图组装成「报文头 + 报文明文」。
 *
 * 职责边界：这里只**组装报文结构**，不负责加密（加密由 client 侧模拟，真实国密在联调前替换）、
 * 不负责上报（上报由 ReportService 调 client）。这样三件事各自可测、可替换。
 *
 * 字段对齐《网络货运单据报文格式》表 2.2 / 2.3，时间戳统一 YYYYMMDDhhmmss（规范 n14 格式）。
 */
@Component
public class ReportAssembler {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 组装运单报文。 */
    public ReportEnvelope waybill(WaybillReportRow w, RpConfig config) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("OriginalDocumentNumber", w.getWaybillNo());
        body.put("ShippingNoteNumber", w.getWaybillNo());
        body.put("SendToProDateTime", w.getSignTime() == null ? "" : w.getSignTime().format(TS));
        body.put("Carrier", config.getSenderCode());
        body.put("ShipperName", nz(w.getShipperName()));
        body.put("DriverName", nz(w.getDriverName()));
        body.put("VehiclePlateNo", nz(w.getPlateNo()));
        body.put("CargoName", nz(w.getCargoName()));
        body.put("Weight", w.getWeight() == null ? "0" : w.getWeight().toPlainString());
        body.put("FreightAmount", w.getAmount() == null ? "0" : w.getAmount().toPlainString());
        body.put("LoadAddress", nz(w.getLoadAddr()));
        body.put("UnloadAddress", nz(w.getUnloadAddr()));
        return envelope(config, RpReportRecord.DOC_WAYBILL, w.getWaybillNo(),
                ReportDict.IPC_WAYBILL, ReportDict.DOC_NAME_WAYBILL, body);
    }

    /** 组装资金流水报文。 */
    public ReportEnvelope fundFlow(FundFlowReportRow f, RpConfig config) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("FlowNo", f.getFlowNo());
        body.put("BizNo", nz(f.getBizNo()));
        body.put("PayeeName", nz(f.getPayeeName()));
        body.put("Amount", f.getAmount() == null ? "0" : f.getAmount().toPlainString());
        body.put("PayTime", f.getPayTime() == null ? "" : f.getPayTime().format(TS));
        body.put("PayeeType", nz(f.getPayeeType()));
        return envelope(config, RpReportRecord.DOC_FUND_FLOW, f.getFlowNo(),
                ReportDict.IPC_FUND_FLOW, ReportDict.DOC_NAME_FUND_FLOW, body);
    }

    /** 组装车辆报文。 */
    public ReportEnvelope vehicle(VehicleReportRow v, RpConfig config) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("PlateNo", nz(v.getPlateNo()));
        body.put("VehicleType", nz(v.getVehicleType()));
        body.put("CapacityTon", v.getCapacityTon() == null ? "0" : v.getCapacityTon().toPlainString());
        body.put("AxleCount", v.getAxleCount() == null ? 0 : v.getAxleCount());
        body.put("OwnerName", nz(v.getOwnerName()));
        body.put("OwnerIdCard", nz(v.getOwnerIdCard()));
        return envelope(config, RpReportRecord.DOC_VEHICLE, v.getPlateNo(),
                ReportDict.IPC_VEHICLE, ReportDict.DOC_NAME_VEHICLE, body);
    }

    /** 组装驾驶员报文。 */
    public ReportEnvelope driver(DriverReportRow d, RpConfig config) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("Name", nz(d.getName()));
        body.put("IdCardNo", nz(d.getIdCardNo()));
        body.put("DriverLicenseNo", nz(d.getDriverLicenseNo()));
        body.put("Phone", nz(d.getPhone()));
        body.put("LicenseExpireDate", d.getLicenseExpireDate() == null ? "" : d.getLicenseExpireDate().toString());
        return envelope(config, RpReportRecord.DOC_DRIVER, d.getIdCardNo(),
                ReportDict.IPC_DRIVER, ReportDict.DOC_NAME_DRIVER, body);
    }

    // ------------------------------------------------------------------ 内部

    /** 生成报文信封：header + 明文 body（序列化成 JSON 字符串）。 */
    private ReportEnvelope envelope(RpConfig config, String docType, String bizNo,
                                    String ipcType, String docName, Map<String, Object> body) {
        ReportEnvelope env = new ReportEnvelope();
        env.setMessageRefNo(UUID.randomUUID().toString());
        env.setDocumentName(docName);
        env.setSenderCode(config.getSenderCode());
        env.setReceiverCode(config.getReceiverCode());
        env.setSendDateTime(LocalDateTime.now().format(TS));
        env.setIpcType(ipcType);
        env.setDocType(docType);
        env.setBizNo(bizNo);
        try {
            env.setEncryptedContent(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            env.setEncryptedContent("{}");
        }
        return env;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
