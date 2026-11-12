package com.lukachangtong.freight.report.support;

import com.lukachangtong.freight.report.entity.RpReportRecord;

/**
 * 上报字典：业务接口代码、单据类型与报文名称的映射。
 *
 * 业务接口代码对齐交通运输部《网络平台道路货物运输经营服务指南》：
 * - 运单：WLHY_YD1001
 * - 资金流水单：LOGINK_CN_FREIGHTCHARGES
 * - 车辆信息单：LOGINK_CN_CREDIT_VEHICLE
 * - 驾驶员信息单：LOGINK_CN_CREDIT_PERSON
 *
 * 集中枚举，避免「前端传了个后端没有的单据类型却静默上报错误」。
 */
public final class ReportDict {

    public static final String IPC_WAYBILL = "WLHY_YD1001";
    public static final String IPC_FUND_FLOW = "LOGINK_CN_FREIGHTCHARGES";
    public static final String IPC_VEHICLE = "LOGINK_CN_CREDIT_VEHICLE";
    public static final String IPC_DRIVER = "LOGINK_CN_CREDIT_PERSON";

    /** 报文名称（规范要求的 DocumentName 中文名） */
    public static final String DOC_NAME_WAYBILL = "电子运单";
    public static final String DOC_NAME_FUND_FLOW = "资金流水单";
    public static final String DOC_NAME_VEHICLE = "车辆信息单";
    public static final String DOC_NAME_DRIVER = "驾驶员信息单";

    private ReportDict() {
    }

    public static String ipcTypeOf(String docType) {
        if (RpReportRecord.DOC_WAYBILL.equals(docType)) return IPC_WAYBILL;
        if (RpReportRecord.DOC_FUND_FLOW.equals(docType)) return IPC_FUND_FLOW;
        if (RpReportRecord.DOC_VEHICLE.equals(docType)) return IPC_VEHICLE;
        if (RpReportRecord.DOC_DRIVER.equals(docType)) return IPC_DRIVER;
        throw new IllegalArgumentException("不支持的上报单据类型：" + docType);
    }

    public static String docNameOf(String docType) {
        if (RpReportRecord.DOC_WAYBILL.equals(docType)) return DOC_NAME_WAYBILL;
        if (RpReportRecord.DOC_FUND_FLOW.equals(docType)) return DOC_NAME_FUND_FLOW;
        if (RpReportRecord.DOC_VEHICLE.equals(docType)) return DOC_NAME_VEHICLE;
        if (RpReportRecord.DOC_DRIVER.equals(docType)) return DOC_NAME_DRIVER;
        throw new IllegalArgumentException("不支持的上报单据类型：" + docType);
    }

    /** 单据类型中文标签（看板展示用） */
    public static String docTypeLabel(String docType) {
        if (RpReportRecord.DOC_WAYBILL.equals(docType)) return "运单";
        if (RpReportRecord.DOC_FUND_FLOW.equals(docType)) return "资金流水";
        if (RpReportRecord.DOC_VEHICLE.equals(docType)) return "车辆";
        if (RpReportRecord.DOC_DRIVER.equals(docType)) return "驾驶员";
        return docType;
    }
}
