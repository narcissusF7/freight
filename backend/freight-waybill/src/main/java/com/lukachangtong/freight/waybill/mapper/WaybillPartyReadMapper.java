package com.lukachangtong.freight.waybill.mapper;

import com.lukachangtong.freight.api.waybill.WaybillParty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 运单参与方只读查询。
 *
 * 这里直连 cr_driver / bd_shipper 取名字，是单库模块化单体下的有意妥协：
 * 为了两个展示名去新建两层端口与两个适配器，收益不抵成本；但**只读、只取 name**，
 * 不把司机与货主的业务规则搬进运单模块。
 */
@Mapper
public interface WaybillPartyReadMapper {

    @Select("SELECT w.id AS waybillId, w.waybill_no AS waybillNo, w.org_id AS orgId, "
            + "w.driver_id AS driverId, d.name AS driverName, "
            + "w.shipper_id AS shipperId, s.name AS shipperName, "
            + "w.status AS status, w.create_time AS createTime, w.sign_time AS signTime "
            + "FROM wb_waybill w "
            + "LEFT JOIN cr_driver d ON d.id = w.driver_id "
            + "LEFT JOIN bd_shipper s ON s.id = w.shipper_id "
            + "WHERE w.id = #{waybillId}")
    WaybillParty selectParty(@Param("waybillId") Long waybillId);
}
