package com.lukachangtong.freight.waybill.service;

import com.lukachangtong.freight.waybill.dto.GoodsSourceSaveRequest;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;

import java.util.List;

public interface GoodsSourceService {

    List<WbGoodsSource> list(Long orgId);

    Long create(GoodsSourceSaveRequest request, String operator);

    /** 关闭货源（不再承运）。 */
    void close(Long id, String operator);
}
