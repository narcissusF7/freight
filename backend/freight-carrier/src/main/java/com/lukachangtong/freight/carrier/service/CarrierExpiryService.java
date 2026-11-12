package com.lukachangtong.freight.carrier.service;

import com.lukachangtong.freight.carrier.vo.ExpiryAlertVo;

import java.util.List;

/** 证件到期预警：司机驾驶证/身份证、车辆道路运输证/保险。 */
public interface CarrierExpiryService {

    /**
     * @param orgId 可空，空则全平台
     * @param days  预警窗口天数（已过期的也会返回，level=EXPIRED）
     */
    List<ExpiryAlertVo> listExpiring(Long orgId, int days);
}
