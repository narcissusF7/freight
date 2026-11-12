package com.lukachangtong.freight.waybill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReceiptMapper extends BaseMapper<WbReceipt> {
}
