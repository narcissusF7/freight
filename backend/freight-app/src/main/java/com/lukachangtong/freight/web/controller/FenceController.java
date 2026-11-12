package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.track.dto.FenceSaveRequest;
import com.lukachangtong.freight.track.entity.TrFence;
import com.lukachangtong.freight.track.service.FenceService;
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

/** 电子围栏维护：圆形（装卸点/园区）与多边形（禁行区）。 */
@RestController
@RequestMapping("/api/fences")
public class FenceController {

    @Resource
    private FenceService fenceService;

    @GetMapping
    public Result<List<TrFence>> list(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String fenceType,
            @RequestParam(required = false) Integer enabled) {
        return Result.ok(fenceService.list(orgId, fenceType, enabled));
    }

    @GetMapping("/{id}")
    public Result<TrFence> get(@PathVariable Long id) {
        return Result.ok(fenceService.get(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody FenceSaveRequest request) {
        return Result.ok(fenceService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FenceSaveRequest request) {
        fenceService.update(id, request);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fenceService.delete(id);
        return Result.ok(null);
    }
}
