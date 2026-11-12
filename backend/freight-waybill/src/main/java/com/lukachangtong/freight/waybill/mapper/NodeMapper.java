package com.lukachangtong.freight.waybill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.waybill.entity.WbNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeMapper extends BaseMapper<WbNode> {
}
