package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.customer.dto.ComplaintAssignRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCloseRequest;
import com.lukachangtong.freight.customer.dto.ComplaintCreateRequest;
import com.lukachangtong.freight.customer.dto.ComplaintFollowRequest;
import com.lukachangtong.freight.customer.dto.ComplaintRejectRequest;
import com.lukachangtong.freight.customer.dto.ComplaintResolveRequest;
import com.lukachangtong.freight.customer.entity.CsComplaint;
import com.lukachangtong.freight.customer.service.ComplaintService;
import com.lukachangtong.freight.customer.vo.ComplaintDetailVo;
import com.lukachangtong.freight.customer.vo.ComplaintStatVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 投诉受理：建单 → 分派 → 跟进 → 处理 → 办结/驳回。
 *
 * 不提供「修改投诉」和「删除投诉」：投诉是监管留痕数据，
 * 写错了只能追加处理记录或驳回，不能改内容也不能删。
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Resource
    private ComplaintService complaintService;

    @GetMapping
    public Result<List<CsComplaint>> list(@RequestParam(required = false) Long orgId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String complaintType,
                                          @RequestParam(required = false) String targetType,
                                          @RequestParam(required = false) Long targetId,
                                          @RequestParam(required = false) Boolean overdue,
                                          @RequestParam(required = false) Integer limit) {
        return Result.ok(complaintService.list(orgId, status, complaintType, targetType, targetId, overdue, limit));
    }

    @GetMapping("/stats")
    public Result<ComplaintStatVo> stats(@RequestParam(required = false) Long orgId) {
        return Result.ok(complaintService.stats(orgId));
    }

    @GetMapping("/{id}")
    public Result<ComplaintDetailVo> detail(@PathVariable Long id) {
        return Result.ok(complaintService.detail(id));
    }

    @PostMapping
    public Result<CsComplaint> create(@RequestBody ComplaintCreateRequest request) {
        return Result.ok(complaintService.create(request));
    }

    @PostMapping("/{id}/assign")
    public Result<ComplaintDetailVo> assign(@PathVariable Long id, @RequestBody ComplaintAssignRequest request) {
        return Result.ok(complaintService.assign(id, request));
    }

    @PostMapping("/{id}/follow")
    public Result<ComplaintDetailVo> follow(@PathVariable Long id, @RequestBody ComplaintFollowRequest request) {
        return Result.ok(complaintService.follow(id, request));
    }

    @PostMapping("/{id}/resolve")
    public Result<ComplaintDetailVo> resolve(@PathVariable Long id, @RequestBody ComplaintResolveRequest request) {
        return Result.ok(complaintService.resolve(id, request));
    }

    @PostMapping("/{id}/close")
    public Result<ComplaintDetailVo> close(@PathVariable Long id, @RequestBody ComplaintCloseRequest request) {
        return Result.ok(complaintService.close(id, request));
    }

    @PostMapping("/{id}/reject")
    public Result<ComplaintDetailVo> reject(@PathVariable Long id, @RequestBody ComplaintRejectRequest request) {
        return Result.ok(complaintService.reject(id, request));
    }

    /**
     * 逾期扫描（生产由调度器每分钟跑，这里留手动入口便于演示与排障）。
     * 幂等：已打标的不会重复升级。
     */
    @PostMapping("/scan-overdue")
    public Result<Integer> scanOverdue(@RequestParam(required = false) String operator) {
        return Result.ok(complaintService.scanOverdue(operator));
    }
}
