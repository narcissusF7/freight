package com.lukachangtong.freight.report.mapper;

import com.lukachangtong.freight.report.vo.WaybillReportRow;
import com.lukachangtong.freight.report.vo.FundFlowReportRow;
import com.lukachangtong.freight.report.vo.VehicleReportRow;
import com.lukachangtong.freight.report.vo.DriverReportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 上报域的**只读**数据访问（CQRS 读侧，与 freight-stat 的 StatReadMapper 同构）。
 *
 * 上报要跨运单/结算/支付/承运 4 个域取数，为每个指标定义端口 + 适配器会得到一堆
 * 「只有一条 SQL」的接口。这里直接只读查询业务表，用三条纪律约束风险：
 * 1. 只 SELECT，不写任何业务表——写路径仍走各域自己的 Service；
 * 2. 语句集中在这一个文件，schema 变更时改一处；
 * 3. 单表/单 join，不做复杂关联，避免上报扫描把业务库拖慢。
 *
 * 若将来上报独立部署，替换这个类的实现即可，Service 层不用改。
 */
@Mapper
public interface ReportReadMapper {

    /**
     * 待上报的运单：已签收（SIGNED 之后）且 report_status=0。
     * 上报触发点对齐规范——「运输业务确认、配载发车、货物签收、完成回单后上报」，
     * 这里取已签收作为最早可上报时点，结算后由资金流水单独上报。
     */
    @Select("SELECT w.id AS id, w.org_id AS orgId, w.waybill_no AS waybillNo, w.shipper_id AS shipperId, "
            + "w.driver_id AS driverId, w.vehicle_id AS vehicleId, w.cargo_name AS cargoName, "
            + "w.weight AS weight, w.freight_amount AS amount, "
            + "w.load_addr AS loadAddr, w.unload_addr AS unloadAddr, "
            + "w.create_time AS createTime, w.sign_time AS signTime, w.status AS status, "
            + "d.name AS driverName, v.plate_no AS plateNo, s.name AS shipperName "
            + "FROM wb_waybill w "
            + "LEFT JOIN cr_driver d ON d.id = w.driver_id "
            + "LEFT JOIN cr_vehicle v ON v.id = w.vehicle_id "
            + "LEFT JOIN bd_shipper s ON s.id = w.shipper_id "
            + "WHERE w.deleted = 0 AND w.report_status = 0 AND w.sign_time IS NOT NULL "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR w.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY w.sign_time ASC, w.id ASC LIMIT #{limit}")
    List<WaybillReportRow> pendingWaybills(@Param("orgId") Long orgId, @Param("limit") int limit);

    /**
     * 待上报的资金流水：已支付成功的付款单。
     * 规范要求「托运人及实际承运人资金流水」上传，这里取支付成功单作为上报对象。
     */
    @Select("SELECT p.id AS id, p.org_id AS orgId, p.pay_no AS flowNo, p.biz_no AS bizNo, "
            + "p.payee_name AS payeeName, p.amount AS amount, p.execute_time AS payTime, "
            + "p.payee_type AS payeeType, p.pay_type AS payType "
            + "FROM st_payment_order p "
            + "WHERE p.status = 'SUCCESS' "
            + "AND NOT EXISTS (SELECT 1 FROM rp_report_record r "
            + "  WHERE r.doc_type = 'FUND_FLOW' AND r.biz_no = p.pay_no AND r.status = 'SUCCESS') "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR p.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY p.id ASC LIMIT #{limit}")
    List<FundFlowReportRow> pendingFundFlows(@Param("orgId") Long orgId, @Param("limit") int limit);

    /**
     * 待上报的车辆：已审核通过。
     * 用「不存在成功上报记录」判重，比给车辆表加 report_status 字段更省（车辆量级大但上报低频）。
     */
    @Select("SELECT v.id AS id, v.org_id AS orgId, v.plate_no AS plateNo, v.vehicle_type AS vehicleType, "
            + "v.capacity_ton AS capacityTon, v.axle_count AS axleCount, v.owner_name AS ownerName, "
            + "v.create_time AS createTime, d.name AS driverName, d.id_card_no AS ownerIdCard "
            + "FROM cr_vehicle v LEFT JOIN cr_driver d ON d.id = v.driver_id "
            + "WHERE v.deleted = 0 AND v.audit_status = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM rp_report_record r "
            + "  WHERE r.doc_type = 'VEHICLE' AND r.biz_no = v.plate_no AND r.status = 'SUCCESS') "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR v.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY v.id ASC LIMIT #{limit}")
    List<VehicleReportRow> pendingVehicles(@Param("orgId") Long orgId, @Param("limit") int limit);

    /**
     * 待上报的驾驶员：已审核通过且身份证号非空。
     * 身份证号是驾驶员报文的必填主键（规范要求），缺失则不具备上报资格，
     * 必须过滤掉，否则 NOT EXISTS 判重时 NULL 比较恒 false，这些司机永远「待上报」。
     */
    @Select("SELECT d.id AS id, d.org_id AS orgId, d.name AS name, d.id_card_no AS idCardNo, "
            + "d.driver_license_no AS driverLicenseNo, d.phone AS phone, "
            + "d.license_expire_date AS licenseExpireDate, d.create_time AS createTime "
            + "FROM cr_driver d "
            + "WHERE d.deleted = 0 AND d.audit_status = 1 AND d.id_card_no IS NOT NULL "
            + "AND NOT EXISTS (SELECT 1 FROM rp_report_record r "
            + "  WHERE r.doc_type = 'DRIVER' AND r.biz_no = d.id_card_no AND r.status = 'SUCCESS') "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR d.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY d.id ASC LIMIT #{limit}")
    List<DriverReportRow> pendingDrivers(@Param("orgId") Long orgId, @Param("limit") int limit);
}
