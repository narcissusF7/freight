package com.lukachangtong.freight.waybill.service.impl;

import com.lukachangtong.freight.api.waybill.WaybillParty;
import com.lukachangtong.freight.api.waybill.WaybillPartyPort;
import com.lukachangtong.freight.waybill.mapper.WaybillPartyReadMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** {@link WaybillPartyPort} 的运单侧实现，供客服域判断投诉被诉对象与评价资格。 */
@Service
public class WaybillPartyPortImpl implements WaybillPartyPort {

    @Resource
    private WaybillPartyReadMapper readMapper;

    @Override
    public WaybillParty load(Long waybillId) {
        if (waybillId == null) {
            return null;
        }
        return readMapper.selectParty(waybillId);
    }
}
