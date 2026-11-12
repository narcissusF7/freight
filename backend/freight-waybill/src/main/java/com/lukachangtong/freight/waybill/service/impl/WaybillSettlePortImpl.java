package com.lukachangtong.freight.waybill.service.impl;

import com.lukachangtong.freight.api.finance.WaybillSettleInfo;
import com.lukachangtong.freight.api.finance.WaybillSettlePort;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.mapper.WaybillMapper;
import com.lukachangtong.freight.waybill.service.WaybillService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 结算端口实现（由运单域提供）。
 *
 * 放在 waybill 模块而不是 settlement：只有运单域知道「哪些状态可结算、推进状态要写节点和 Outbox」。
 * 结算域拿到的是一个窄视图，不需要理解运单状态机。
 */
@Service
public class WaybillSettlePortImpl implements WaybillSettlePort {

    @Resource
    private WaybillMapper waybillMapper;
    @Resource
    private CrDriverMapper driverMapper;
    @Resource
    private WaybillService waybillService;

    @Override
    public WaybillSettleInfo loadInfo(Long waybillId) {
        WbWaybill wb = waybillId == null ? null : waybillMapper.selectById(waybillId);
        if (wb == null) {
            return null;
        }
        WaybillSettleInfo info = new WaybillSettleInfo();
        info.setWaybillId(wb.getId());
        info.setWaybillNo(wb.getWaybillNo());
        info.setOrgId(wb.getOrgId());
        info.setDriverId(wb.getDriverId());
        info.setShipperId(wb.getShipperId());
        info.setVehicleId(wb.getVehicleId());
        info.setFreightAmount(wb.getFreightAmount());
        info.setWeight(wb.getWeight());
        info.setStatus(wb.getStatus());
        info.setLoadAddr(wb.getLoadAddr());
        info.setUnloadAddr(wb.getUnloadAddr());
        if (wb.getDriverId() != null) {
            CrDriver driver = driverMapper.selectById(wb.getDriverId());
            info.setDriverName(driver == null ? null : driver.getName());
        }
        return info;
    }

    @Override
    public boolean markSettled(Long waybillId, String settlementNo) {
        return waybillService.markSettled(waybillId, settlementNo);
    }
}
