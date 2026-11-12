package com.lukachangtong.freight.carrier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.carrier.service.CarrierExpiryService;
import com.lukachangtong.freight.carrier.vo.ExpiryAlertVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CarrierExpiryServiceImpl implements CarrierExpiryService {

    private static final int URGENT_DAYS = 30;

    @Resource
    private CrDriverMapper driverMapper;
    @Resource
    private CrVehicleMapper vehicleMapper;

    @Override
    public List<ExpiryAlertVo> listExpiring(Long orgId, int days) {
        int window = days <= 0 ? 60 : days;
        LocalDate threshold = LocalDate.now().plusDays(window);
        List<ExpiryAlertVo> alerts = new ArrayList<>();

        // 司机：驾驶证 / 身份证
        QueryWrapper<CrDriver> dq = new QueryWrapper<>();
        if (orgId != null) {
            dq.eq("org_id", orgId);
        }
        dq.and(w -> w.isNotNull("license_expire_date").le("license_expire_date", threshold)
                .or().isNotNull("id_card_expire_date").le("id_card_expire_date", threshold));
        for (CrDriver d : driverMapper.selectList(dq)) {
            addIfExpiring(alerts, "DRIVER", d.getId(), d.getName(),
                    "驾驶证", d.getDriverLicenseNo(), d.getLicenseExpireDate(), window);
            addIfExpiring(alerts, "DRIVER", d.getId(), d.getName(),
                    "身份证", d.getIdCardNo(), d.getIdCardExpireDate(), window);
        }

        // 车辆：道路运输证 / 保险
        QueryWrapper<CrVehicle> vq = new QueryWrapper<>();
        if (orgId != null) {
            vq.eq("org_id", orgId);
        }
        vq.and(w -> w.isNotNull("vehicle_license_expire_date").le("vehicle_license_expire_date", threshold)
                .or().isNotNull("insurance_expire_date").le("insurance_expire_date", threshold));
        for (CrVehicle v : vehicleMapper.selectList(vq)) {
            addIfExpiring(alerts, "VEHICLE", v.getId(), v.getPlateNo(),
                    "道路运输证", v.getPlateNo(), v.getVehicleLicenseExpireDate(), window);
            addIfExpiring(alerts, "VEHICLE", v.getId(), v.getPlateNo(),
                    "保险", v.getPlateNo(), v.getInsuranceExpireDate(), window);
        }

        alerts.sort(Comparator.comparingLong(ExpiryAlertVo::getDaysLeft));
        return alerts;
    }

    private void addIfExpiring(List<ExpiryAlertVo> alerts, String bizType, Long bizId, String bizName,
                               String docType, String docNo, LocalDate expireDate, int window) {
        if (expireDate == null) {
            return;
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
        if (daysLeft > window) {
            return;
        }
        ExpiryAlertVo vo = new ExpiryAlertVo();
        vo.setBizType(bizType);
        vo.setBizId(bizId);
        vo.setBizName(bizName);
        vo.setDocType(docType);
        vo.setDocNo(docNo);
        vo.setExpireDate(expireDate);
        vo.setDaysLeft(daysLeft);
        vo.setLevel(daysLeft < 0 ? "EXPIRED" : (daysLeft <= URGENT_DAYS ? "URGENT" : "WARN"));
        alerts.add(vo);
    }
}
