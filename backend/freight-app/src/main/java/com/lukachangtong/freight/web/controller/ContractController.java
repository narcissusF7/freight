package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.contract.dto.ContractSaveRequest;
import com.lukachangtong.freight.contract.entity.CtContract;
import com.lukachangtong.freight.contract.service.ContractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/** 电子合同：创建 → 双方签署（CA）→ 验签。 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Resource
    private ContractService contractService;

    @GetMapping
    public Result<List<CtContract>> list(@RequestParam(required = false) Long orgId) {
        return Result.ok(contractService.list(orgId));
    }

    @GetMapping("/{id}")
    public Result<CtContract> detail(@PathVariable Long id) {
        return Result.ok(contractService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ContractSaveRequest request) {
        return Result.ok(contractService.create(request));
    }

    @PostMapping("/{id}/sign")
    public Result<CtContract> sign(@PathVariable Long id,
                                   @RequestParam(defaultValue = "A") String party,
                                   @RequestParam(required = false) String operator) {
        return Result.ok(contractService.sign(id, party, operator));
    }

    /**
     * 验签。传 content 时以该内容重算摘要——内容被改动则验签失败，
     * 用于向业务方证明"签章绑定的是当时的合同正文"。
     */
    @PostMapping("/{id}/verify")
    public Result<Boolean> verify(@PathVariable Long id,
                                  @RequestParam(required = false) String content,
                                  @RequestParam(required = false) String party) {
        return Result.ok(contractService.verify(id, content, party));
    }
}
