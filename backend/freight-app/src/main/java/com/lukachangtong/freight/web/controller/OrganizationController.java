package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.auth.entity.SysOrg;
import com.lukachangtong.freight.auth.service.OrganizationService;
import com.lukachangtong.freight.auth.vo.SysOrgVo;
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
@RequestMapping("/api/orgs")
public class OrganizationController {

    @Resource
    private OrganizationService organizationService;

    @GetMapping("/tree")
    public Result<List<SysOrgVo>> tree() {
        return Result.ok(organizationService.tree());
    }

    @GetMapping
    public Result<List<SysOrg>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(organizationService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody SysOrg org) {
        return Result.ok(organizationService.create(org));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysOrg org) {
        org.setId(id);
        organizationService.update(org);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return Result.ok(null);
    }
}
