package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.auth.dto.RoleMenuAssignRequest;
import com.lukachangtong.freight.auth.entity.SysRole;
import com.lukachangtong.freight.auth.service.RoleService;
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
@RequestMapping("/api/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping
    public Result<List<SysRole>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(roleService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody SysRole role) {
        return Result.ok(roleService.create(role));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody RoleMenuAssignRequest request) {
        roleService.assignMenus(roleId, request);
        return Result.ok(null);
    }

    @GetMapping("/{roleId}/menus")
    public Result<List<Long>> menus(@PathVariable Long roleId) {
        return Result.ok(roleService.listMenuIds(roleId));
    }
}
