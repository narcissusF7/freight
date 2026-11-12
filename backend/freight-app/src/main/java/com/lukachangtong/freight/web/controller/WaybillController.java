package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.waybill.dto.ExceptionHandleRequest;
import com.lukachangtong.freight.waybill.dto.ExceptionReportRequest;
import com.lukachangtong.freight.waybill.dto.WaybillCreateRequest;
import com.lukachangtong.freight.waybill.dto.WaybillStatusChangeRequest;
import com.lukachangtong.freight.waybill.entity.WbException;
import com.lukachangtong.freight.waybill.entity.WbNode;
import com.lukachangtong.freight.waybill.entity.WbReceipt;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.service.MatchingService;
import com.lukachangtong.freight.waybill.service.WaybillService;
import com.lukachangtong.freight.waybill.vo.MatchVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/waybills")
public class WaybillController {

    @Resource
    private WaybillService waybillService;
    @Resource
    private MatchingService matchingService;

    @GetMapping
    public Result<List<WbWaybill>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(waybillService.list(orgId));
    }

    @GetMapping("/{id}")
    public Result<WbWaybill> detail(@PathVariable Long id) {
        return Result.ok(waybillService.detail(id));
    }

    @GetMapping("/{id}/nodes")
    public Result<List<WbNode>> nodes(@PathVariable Long id) {
        return Result.ok(waybillService.nodes(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody WaybillCreateRequest request) {
        return Result.ok(waybillService.createWaybill(request));
    }

    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody WaybillStatusChangeRequest request) {
        waybillService.changeStatus(id, request);
        return Result.ok(null);
    }

    /** 车货匹配：给货源推荐已审核通过的司机+车辆（含评分明细与理由）。 */
    @GetMapping("/match")
    public Result<List<MatchVo>> match(@RequestParam Long goodsSourceId,
                                       @RequestParam(required = false) Integer limit) {
        return Result.ok(matchingService.match(goodsSourceId, limit));
    }

    // ---------- 异常件 ----------

    @GetMapping("/{id}/exceptions")
    public Result<List<WbException>> exceptions(@PathVariable Long id) {
        return Result.ok(waybillService.exceptions(id));
    }

    @PostMapping("/{id}/exceptions")
    public Result<Long> reportException(@PathVariable Long id,
                                        @RequestBody ExceptionReportRequest request) {
        return Result.ok(waybillService.reportException(id, request));
    }

    @PostMapping("/exceptions/{exceptionId}/handle")
    public Result<Void> handleException(@PathVariable Long exceptionId,
                                        @RequestBody ExceptionHandleRequest request) {
        waybillService.handleException(exceptionId, request);
        return Result.ok(null);
    }

    // ---------- 回单 ----------

    @GetMapping("/{id}/receipts")
    public Result<List<WbReceipt>> receipts(@PathVariable Long id) {
        return Result.ok(waybillService.receipts(id));
    }

    @PostMapping("/{id}/receipts")
    public Result<Long> uploadReceipt(@PathVariable Long id,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String operator,
                                      @RequestParam(required = false) String remark) throws IOException {
        return Result.ok(waybillService.uploadReceipt(id, file.getInputStream(),
                file.getOriginalFilename(), file.getContentType(), operator, remark));
    }

    @PostMapping("/receipts/{receiptId}/verify")
    public Result<Void> verifyReceipt(@PathVariable Long receiptId,
                                      @RequestParam(required = false) String operator) {
        waybillService.verifyReceipt(receiptId, operator);
        return Result.ok(null);
    }
}
