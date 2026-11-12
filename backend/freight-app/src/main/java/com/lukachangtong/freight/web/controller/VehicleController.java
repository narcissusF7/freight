package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.service.VehicleService;
import com.lukachangtong.freight.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Resource
    private VehicleService vehicleService;

    @GetMapping
    public Result<List<CrVehicle>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(vehicleService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody CrVehicle vehicle) {
        return Result.ok(vehicleService.create(vehicle));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CrVehicle vehicle) {
        vehicle.setId(id);
        vehicleService.update(vehicle);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam Integer status,
                              @RequestParam(required = false) String remark,
                              @RequestParam(required = false) String operator) {
        vehicleService.audit(id, status, remark, operator);
        return Result.ok(null);
    }
}
