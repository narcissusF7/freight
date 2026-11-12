package com.lukachangtong.freight.carrier.service;

import com.lukachangtong.freight.carrier.entity.CrVehicle;

import java.util.List;

public interface VehicleService {

    List<CrVehicle> list(Long orgId);

    Long create(CrVehicle vehicle);

    void update(CrVehicle vehicle);

    void delete(Long id);

    void audit(Long id, Integer status, String remark, String operator);
}
