package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.auth.dto.EmployeeSaveRequest;
import com.lukachangtong.freight.auth.entity.SysUser;
import com.lukachangtong.freight.auth.service.EmployeeService;
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
@RequestMapping("/api/employees")
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @GetMapping
    public Result<List<SysUser>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(employeeService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody EmployeeSaveRequest request) {
        return Result.ok(employeeService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EmployeeSaveRequest request) {
        request.setId(id);
        employeeService.update(request);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        employeeService.assignRoles(id, roleIds);
        return Result.ok(null);
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> roles(@PathVariable Long id) {
        return Result.ok(employeeService.listRoleIds(id));
    }
}
