package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.service.DriverService;
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
@RequestMapping("/api/drivers")
public class DriverController {

    @Resource
    private DriverService driverService;

    @GetMapping
    public Result<List<CrDriver>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(driverService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody CrDriver driver) {
        return Result.ok(driverService.create(driver));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CrDriver driver) {
        driver.setId(id);
        driverService.update(driver);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        driverService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam Integer status,
                              @RequestParam(required = false) String remark,
                              @RequestParam(required = false) String operator) {
        driverService.audit(id, status, remark, operator);
        return Result.ok(null);
    }
}
