package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.customer.dto.CreditAdjustRequest;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import com.lukachangtong.freight.customer.service.CreditService;
import com.lukachangtong.freight.customer.vo.CreditChangeVo;
import com.lukachangtong.freight.customer.vo.CreditDetailVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 信用分：排行 / 详情（含变动流水）/ 人工调账。
 *
 * 调账是唯一的「手改分」入口，必须写原因——否则司机的信用分成了黑箱，
 * 申诉时平台答不出为什么扣分。
 */
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    @Resource
    private CreditService creditService;

    @GetMapping
    public Result<List<CsCreditSubject>> ranking(@RequestParam(required = false) String subjectType,
                                                 @RequestParam(required = false) String level,
                                                 @RequestParam(required = false) Integer limit) {
        return Result.ok(creditService.ranking(subjectType, level, limit));
    }

    @GetMapping("/detail")
    public Result<CreditDetailVo> detail(@RequestParam String subjectType,
                                         @RequestParam Long subjectId) {
        return Result.ok(creditService.detail(subjectType, subjectId));
    }

    @PostMapping("/adjust")
    public Result<CreditChangeVo> adjust(@RequestBody CreditAdjustRequest request) {
        return Result.ok(creditService.adjust(request));
    }
}
