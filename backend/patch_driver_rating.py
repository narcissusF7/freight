import io

# ---------------- 1) RatingService 增加按运单+方向查询 ----------------
p = 'freight-customer/src/main/java/com/lukachangtong/freight/customer/service/RatingService.java'
s = io.open(p, encoding='utf-8').read()
if 'byWaybill' not in s:
    s = s.replace("""    RatingSummaryVo summary(String targetType, Long targetId);""",
"""    RatingSummaryVo summary(String targetType, Long targetId);

    /**
     * 按运单 + 方向查已有评价，没有返回 null。
     * 司机端要区分「已评价 / 待评价」，不能靠异常来判断。
     */
    CsRating byWaybill(Long waybillId, String direction);""")
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('RatingService ok')
else:
    print('RatingService skip')

# ---------------- 2) 实现 ----------------
p2 = 'freight-customer/src/main/java/com/lukachangtong/freight/customer/service/impl/RatingServiceImpl.java'
s = io.open(p2, encoding='utf-8').read()
if 'public CsRating byWaybill' not in s:
    s = s.replace("""    // ------------------------------------------------------------------ 内部""",
"""    @Override
    public CsRating byWaybill(Long waybillId, String direction) {
        if (waybillId == null || direction == null) {
            return null;
        }
        return ratingMapper.selectOne(new QueryWrapper<CsRating>()
                .eq("waybill_id", waybillId)
                .eq("direction", direction.trim().toUpperCase())
                .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 内部""")
    io.open(p2, 'w', encoding='utf-8', newline='\n').write(s)
    print('RatingServiceImpl ok')
else:
    print('RatingServiceImpl skip')

# ---------------- 3) 司机端控制器 ----------------
p3 = 'freight-app/src/main/java/com/lukachangtong/freight/web/controller/DriverAppController.java'
s = io.open(p3, encoding='utf-8').read()
if '/ratings' not in s:
    s = s.replace("""import com.lukachangtong.freight.message.entity.MsgRecord;""",
"""import com.lukachangtong.freight.customer.dto.RatingSubmitRequest;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.service.RatingService;
import com.lukachangtong.freight.message.entity.MsgRecord;""")
    s = s.replace("""import com.lukachangtong.freight.web.dto.DriverPushRequest;""",
"""import com.lukachangtong.freight.web.dto.DriverPushRequest;
import com.lukachangtong.freight.web.dto.DriverRatingRequest;""")
    s = s.replace("""    @Resource
    private AlarmService alarmService;""",
"""    @Resource
    private AlarmService alarmService;
    @Resource
    private RatingService ratingService;""")
    s = s.replace("""    @PostMapping("/waybills/{id}/exception")""",
"""    // ------------------------------------------------------------------ 评价

    /**
     * 司机评价货主。
     *
     * 方向固定 `DRIVER_TO_SHIPPER`、评价人固定当前登录司机：这两个值只要能被前端传，
     * 就能给别人的运单刷差评。运单归属仍由 `requireOwned` 校验一次。
     */
    @PostMapping("/ratings")
    public Result<CsRating> rateShipper(@RequestHeader("Authorization") String token,
                                        @RequestBody DriverRatingRequest request) {
        Long driverId = driverAuthService.currentDriverId(token);
        if (request == null || request.getWaybillId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请指定要评价的运单");
        }
        requireOwned(request.getWaybillId(), driverId);

        RatingSubmitRequest submit = new RatingSubmitRequest();
        submit.setWaybillId(request.getWaybillId());
        submit.setDirection(CsRating.DIR_DRIVER_TO_SHIPPER);
        submit.setRaterId(driverId);
        submit.setScore(request.getScore());
        submit.setPunctuality(request.getPunctuality());
        submit.setService(request.getService());
        submit.setCargo(request.getCargo());
        submit.setAttitude(request.getAttitude());
        submit.setTags(request.getTags());
        submit.setContent(request.getContent());
        submit.setAnonymous(request.getAnonymous());
        submit.setOperator("DRIVER:" + driverId);
        return Result.ok(ratingService.submit(submit));
    }

    /** 查当前司机对这张运单的评价（没有则 data 为空），用于前端区分「待评价 / 已评价」。 */
    @GetMapping("/ratings")
    public Result<CsRating> myRating(@RequestHeader("Authorization") String token,
                                     @RequestParam Long waybillId) {
        Long driverId = driverAuthService.currentDriverId(token);
        requireOwned(waybillId, driverId);
        return Result.ok(ratingService.byWaybill(waybillId, CsRating.DIR_DRIVER_TO_SHIPPER));
    }

    @PostMapping("/waybills/{id}/exception")""")
    io.open(p3, 'w', encoding='utf-8', newline='\n').write(s)
    print('DriverAppController ok')
else:
    print('DriverAppController skip')
