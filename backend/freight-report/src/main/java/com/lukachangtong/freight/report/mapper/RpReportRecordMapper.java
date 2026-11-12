package com.lukachangtong.freight.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.report.entity.RpReportRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RpReportRecordMapper extends BaseMapper<RpReportRecord> {

    /**
     * 上报成功后回写运单的 report_status。
     *
     * 这是上报域唯一的一处**跨模块写**：report 模块按「无成功上报记录」扫描待上报运单，
     * 上报成功后必须把运单标记为已上报，否则下次扫描又重复上报。
     * 为避免 report → waybill 的模块反向依赖，这里用一条受控 UPDATE 直接落库，
     * 只改 report_status 一个字段，不碰运单业务逻辑（状态机仍由 waybill 模块独占）。
     */
    @Update("UPDATE wb_waybill SET report_status = 1 WHERE id = #{waybillId}")
    int markWaybillReported(@Param("waybillId") Long waybillId);
}
