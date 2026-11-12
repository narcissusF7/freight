package com.lukachangtong.freight.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.customer.entity.CsRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CsRatingMapper extends BaseMapper<CsRating> {

    /**
     * 分数分布：`[{score:5, cnt:12}, …]`。
     * 聚合放到 SQL 里，避免为了算个分布把某人几年评价全捞进内存。
     */
    @Select("SELECT score AS score, COUNT(*) AS cnt FROM cs_rating "
            + "WHERE target_type = #{targetType} AND target_id = #{targetId} AND status = 'VALID' "
            + "GROUP BY score")
    List<Map<String, Object>> countByScore(@Param("targetType") String targetType,
                                           @Param("targetId") Long targetId);
}
