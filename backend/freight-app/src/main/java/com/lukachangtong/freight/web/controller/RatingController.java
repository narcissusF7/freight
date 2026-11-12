package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.customer.dto.RatingReplyRequest;
import com.lukachangtong.freight.customer.dto.RatingSubmitRequest;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.service.RatingService;
import com.lukachangtong.freight.customer.vo.RatingSummaryVo;
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
 * 双向星级评价：查询 / 提交 / 回复 / 隐藏。
 *
 * 「隐藏」不是删除：违规评价隐藏后仍可被管理员与监管查到，
 * 删掉就等于把纠纷证据销毁了。
 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Resource
    private RatingService ratingService;

    @GetMapping
    public Result<List<CsRating>> list(@RequestParam(required = false) Long orgId,
                                       @RequestParam(required = false) String direction,
                                       @RequestParam(required = false) String targetType,
                                       @RequestParam(required = false) Long targetId,
                                       @RequestParam(required = false) Integer scoreBelow,
                                       @RequestParam(required = false) Integer limit) {
        return Result.ok(ratingService.list(orgId, direction, targetType, targetId, scoreBelow, limit));
    }

    @GetMapping("/summary")
    public Result<RatingSummaryVo> summary(@RequestParam String targetType,
                                           @RequestParam Long targetId) {
        return Result.ok(ratingService.summary(targetType, targetId));
    }

    @PostMapping
    public Result<CsRating> submit(@RequestBody RatingSubmitRequest request) {
        return Result.ok(ratingService.submit(request));
    }

    @PostMapping("/{id}/reply")
    public Result<CsRating> reply(@PathVariable Long id, @RequestBody RatingReplyRequest request) {
        return Result.ok(ratingService.reply(id, request));
    }

    @PostMapping("/{id}/hide")
    public Result<CsRating> hide(@PathVariable Long id,
                                 @RequestParam(required = false) String reason,
                                 @RequestParam(required = false) String operator) {
        return Result.ok(ratingService.hide(id, reason, operator));
    }
}
