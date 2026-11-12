package com.lukachangtong.freight.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CsComplaintMapper extends BaseMapper<CsComplaint> {

    /**
     * 已处理（RESOLVED/CLOSED）的单量。
     * 用 SQL 聚合而不是把明细捞进内存：投诉量随时间线性增长，看板接口不能被历史数据拖垮。
     */
    @Select("SELECT COUNT(*) FROM cs_complaint WHERE deleted = 0 AND resolved_time IS NOT NULL "
            + "AND status IN ('RESOLVED','CLOSED') AND (#{orgId} IS NULL OR org_id = #{orgId})")
    long countHandled(@Param("orgId") Long orgId);

    /** 24 小时内处理完的单量——监管考核的分子。 */
    @Select("SELECT COUNT(*) FROM cs_complaint WHERE deleted = 0 AND resolved_time IS NOT NULL "
            + "AND status IN ('RESOLVED','CLOSED') AND resolved_time <= deadline_time "
            + "AND (#{orgId} IS NULL OR org_id = #{orgId})")
    long countHandledInSla(@Param("orgId") Long orgId);

    /** 平均处理时长（分钟），用于看「平均响应」而不是只看达标率。 */
    @Select("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, accept_time, resolved_time)), 0) "
            + "FROM cs_complaint WHERE deleted = 0 AND resolved_time IS NOT NULL "
            + "AND accept_time IS NOT NULL AND (#{orgId} IS NULL OR org_id = #{orgId})")
    double avgHandleMinutes(@Param("orgId") Long orgId);
}
