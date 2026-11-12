package com.lukachangtong.freight.waybill.service.impl;

import com.lukachangtong.freight.api.waybill.WaybillRouteInfo;
import com.lukachangtong.freight.api.waybill.WaybillRoutePort;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.mapper.WaybillMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * {@link WaybillRoutePort} 的运单侧实现：供轨迹域的偏航判定读取起终点坐标。
 * 只读、不做状态校验——轨迹上报时运单可能已经结束，预警仍需回溯分析。
 */
@Service
public class WaybillRoutePortImpl implements WaybillRoutePort {

    @Resource
    private WaybillMapper waybillMapper;

    @Override
    public WaybillRouteInfo load(Long waybillId) {
        if (waybillId == null) {
            return null;
        }
        WbWaybill wb = waybillMapper.selectById(waybillId);
        if (wb == null) {
            return null;
        }
        WaybillRouteInfo info = new WaybillRouteInfo();
        info.setWaybillId(wb.getId());
        info.setOrgId(wb.getOrgId());
        info.setDriverId(wb.getDriverId());
        info.setVehicleId(wb.getVehicleId());
        info.setLoadLat(wb.getLoadLat());
        info.setLoadLng(wb.getLoadLng());
        info.setUnloadLat(wb.getUnloadLat());
        info.setUnloadLng(wb.getUnloadLng());
        info.setStatus(wb.getStatus());
        return info;
    }
}
