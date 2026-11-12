package com.lukachangtong.freight.track.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.track.entity.TrTrackPoint;
import org.apache.ibatis.annotations.Mapper;

/** 轨迹表为分区表，只用于 insert 与按时间窗 selectList（不使用 updateById）。 */
@Mapper
public interface TrTrackPointMapper extends BaseMapper<TrTrackPoint> {
}
