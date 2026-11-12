package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.waybill.dto.GoodsSourceSaveRequest;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;
import com.lukachangtong.freight.waybill.service.GoodsSourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/goods-sources")
public class GoodsSourceController {

    @Resource
    private GoodsSourceService goodsSourceService;

    @GetMapping
    public Result<List<WbGoodsSource>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(goodsSourceService.list(orgId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody GoodsSourceSaveRequest request,
                               @RequestParam(required = false) String operator) {
        return Result.ok(goodsSourceService.create(request, operator));
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id,
                              @RequestParam(required = false) String operator) {
        goodsSourceService.close(id, operator);
        return Result.ok(null);
    }
}
