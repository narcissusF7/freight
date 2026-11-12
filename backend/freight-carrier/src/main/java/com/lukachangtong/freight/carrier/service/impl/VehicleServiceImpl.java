package com.lukachangtong.freight.carrier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.carrier.service.VehicleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private CrVehicleMapper vehicleMapper;

    @Override
    public List<CrVehicle> list(Long orgId) {
        QueryWrapper<CrVehicle> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("id");
        return vehicleMapper.selectList(qw);
    }

    @Override
    public Long create(CrVehicle vehicle) {
        if (vehicle.getCapacityTon() == null) {
            vehicle.setCapacityTon(BigDecimal.ZERO);
        }
        if (vehicle.getAuditStatus() == null) {
            vehicle.setAuditStatus(0);
        }
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());
        vehicleMapper.insert(vehicle);
        return vehicle.getId();
    }

    @Override
    public void update(CrVehicle vehicle) {
        vehicle.setUpdateTime(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
    }

    @Override
    public void delete(Long id) {
        vehicleMapper.deleteById(id);
    }

    @Override
    public void audit(Long id, Integer status, String remark, String operator) {
        CrVehicle upd = new CrVehicle();
        upd.setId(id);
        upd.setAuditStatus(status);
        upd.setAuditRemark(remark);
        upd.setUpdateBy(operator);
        upd.setUpdateTime(LocalDateTime.now());
        vehicleMapper.updateById(upd);
    }
}
