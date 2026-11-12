package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.auth.entity.SysMenu;
import com.lukachangtong.freight.auth.service.MenuService;
import com.lukachangtong.freight.auth.vo.SysMenuVo;
import com.lukachangtong.freight.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Resource
    private MenuService menuService;

    @GetMapping("/tree")
    public Result<List<SysMenuVo>> tree() {
        return Result.ok(menuService.tree());
    }

    @GetMapping
    public Result<List<SysMenu>> list() {
        return Result.ok(menuService.list());
    }

    @PostMapping
    public Result<Long> create(@RequestBody SysMenu menu) {
        return Result.ok(menuService.create(menu));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.update(menu);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok(null);
    }
}
