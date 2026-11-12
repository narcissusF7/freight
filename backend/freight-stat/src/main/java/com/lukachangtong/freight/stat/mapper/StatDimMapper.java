package com.lukachangtong.freight.stat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.stat.entity.StatDim;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StatDimMapper extends BaseMapper<StatDim> {

    /**
     * 维度重建前先清当天该口径的旧行。
     *
     * 维度行数不固定（货物名会新增、司机会离职），单靠唯一键 upsert 会残留昨日有今日无的维度值，
     * 造成「趋势里某省今天突然消失」。整日删掉重写最干净，且维度表体量小，代价可以接受。
     */
    @Delete("DELETE FROM stat_dim WHERE biz_date = #{bizDate} AND org_id = #{orgId}")
    int deleteByDay(@Param("bizDate") LocalDate bizDate, @Param("orgId") Long orgId);
}
