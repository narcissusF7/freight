package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.customer.dto.RatingSubmitRequest;
import com.lukachangtong.freight.customer.entity.CsRating;
import com.lukachangtong.freight.customer.service.RatingService;
import com.lukachangtong.freight.message.entity.MsgRecord;
import com.lukachangtong.freight.message.service.MessageService;
import com.lukachangtong.freight.track.dto.TrackBatchRequest;
import com.lukachangtong.freight.track.dto.TrackPointDto;
import com.lukachangtong.freight.track.service.AlarmService;
import com.lukachangtong.freight.track.service.TrackService;
import com.lukachangtong.freight.waybill.dto.DriverNodeConfirmRequest;
import com.lukachangtong.freight.waybill.dto.ExceptionReportRequest;
import com.lukachangtong.freight.waybill.entity.WbWaybill;
import com.lukachangtong.freight.waybill.service.DriverWaybillService;
import com.lukachangtong.freight.waybill.service.WaybillService;
import com.lukachangtong.freight.waybill.vo.DriverHallVo;
import com.lukachangtong.freight.waybill.vo.DriverTodoVo;
import com.lukachangtong.freight.waybill.vo.DriverWaybillDetailVo;
import com.lukachangtong.freight.web.dto.DriverAcceptRequest;
import com.lukachangtong.freight.web.dto.DriverLoginRequest;
import com.lukachangtong.freight.web.dto.DriverLoginResponse;
import com.lukachangtong.freight.web.dto.DriverPasswordChangeRequest;
import com.lukachangtong.freight.web.dto.DriverPasswordInitRequest;
import com.lukachangtong.freight.web.dto.DriverPushRequest;
import com.lukachangtong.freight.web.dto.DriverRatingRequest;
import com.lukachangtong.freight.web.service.DriverAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 司机端 APP 接口（/api/driver/**）。
 *
 * 与后台接口的隔离靠两层：
 * 1. URL 前缀 + `hasRole('DRIVER')`：后台令牌进不来；
 * 2. `DriverScopeFilter`：司机令牌只能访问 /api/driver/** 与文件下载，防止司机端令牌被拿去调后台接口。
 *
 * 所有接口的 driverId 都从令牌里取，**不接受前端传 driverId**——传了也能改，等于越权。
 */
@RestController
@RequestMapping("/api/driver")
public class DriverAppController {

    @Resource
    private DriverAuthService driverAuthService;
    @Resource
    private DriverWaybillService driverWaybillService;
    @Resource
    private WaybillService waybillService;
    @Resource
    private MessageService messageService;
    @Resource
    private TrackService trackService;
    @Resource
    private AlarmService alarmService;
    @Resource
    private RatingService ratingService;

    // ------------------------------------------------------------------ 账号

    @PostMapping("/login")
    public Result<DriverLoginResponse> login(@RequestBody DriverLoginRequest request) {
        return Result.ok(driverAuthService.login(request.getPhone(), request.getPassword()));
    }

    /** 首次开通（司机未被后台初始化密码时自助设置）。 */
    @PostMapping("/password/init")
    public Result<Void> initPassword(@RequestBody DriverPasswordInitRequest request) {
        driverAuthService.initPassword(request.getPhone(), request.getSmsCode(), request.getNewPassword());
        return Result.ok(null);
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@RequestHeader("Authorization") String token,
                                       @RequestBody DriverPasswordChangeRequest request) {
        driverAuthService.changePassword(token, request.getOldPassword(), request.getNewPassword());
        return Result.ok(null);
    }

    @PostMapping("/refresh")
    public Result<DriverLoginResponse> refresh(@RequestHeader("Authorization") String token) {
        return Result.ok(driverAuthService.refresh(token));
    }

    @GetMapping("/profile")
    public Result<DriverLoginResponse> profile(@RequestHeader("Authorization") String token) {
        return Result.ok(driverAuthService.profile(token));
    }

    /** 接单开关。关掉后不再进推送名单，但仍可完成手上的运单。 */
    @PostMapping("/online")
    public Result<Void> online(@RequestHeader("Authorization") String token,
                               @RequestParam boolean online) {
        driverAuthService.setOnline(token, online);
        return Result.ok(null);
    }

    @PostMapping("/push/register")
    public Result<Void> registerPush(@RequestHeader("Authorization") String token,
                                     @RequestBody DriverPushRequest request) {
        driverAuthService.registerPush(token, request.getClientId());
        return Result.ok(null);
    }

    // ------------------------------------------------------------------ 找货 / 接单

    @GetMapping("/goods-sources")
    public Result<List<DriverHallVo>> hall(@RequestHeader("Authorization") String token,
                                           @RequestParam(required = false) BigDecimal lat,
                                           @RequestParam(required = false) BigDecimal lng,
                                           @RequestParam(required = false) Integer limit,
                                           @RequestParam(required = false) Integer maxDistanceKm) {
        Long driverId = driverAuthService.currentDriverId(token);
        return Result.ok(driverWaybillService.hall(driverId, lat, lng, limit, maxDistanceKm));
    }

    @PostMapping("/goods-sources/{id}/accept")
    public Result<DriverWaybillDetailVo> accept(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @RequestBody(required = false) DriverAcceptRequest request,
                                                HttpServletRequest http) {
        Long driverId = driverAuthService.currentDriverId(token);
        Long waybillId = driverWaybillService.accept(id, driverId,
                request == null ? null : request.getVehicleId(), clientIp(http));
        return Result.ok(driverWaybillService.detail(waybillId, driverId, null, null));
    }

    // ------------------------------------------------------------------ 我的运单

    @GetMapping("/waybills")
    public Result<List<WbWaybill>> myWaybills(@RequestHeader("Authorization") String token,
                                              @RequestParam(required = false) String scope) {
        return Result.ok(driverWaybillService.myWaybills(driverAuthService.currentDriverId(token), scope));
    }

    @GetMapping("/waybills/{id}")
    public Result<DriverWaybillDetailVo> waybillDetail(@RequestHeader("Authorization") String token,
                                                       @PathVariable Long id,
                                                       @RequestParam(required = false) BigDecimal lat,
                                                       @RequestParam(required = false) BigDecimal lng) {
        return Result.ok(driverWaybillService.detail(id, driverAuthService.currentDriverId(token), lat, lng));
    }

    /** 节点确认：动作由服务端映射状态，前端只提交 action + 定位 + 照片。 */
    @PostMapping("/waybills/{id}/nodes")
    public Result<DriverWaybillDetailVo> confirmNode(@RequestHeader("Authorization") String token,
                                                     @PathVariable Long id,
                                                     @RequestBody DriverNodeConfirmRequest request,
                                                     HttpServletRequest http) {
        Long driverId = driverAuthService.currentDriverId(token);
        return Result.ok(driverWaybillService.confirm(id, driverId, request, clientIp(http)));
    }

    /** 回单上传：司机签收后拍照上传，自动推进 SIGNED → RECEIPT_UPLOADED。 */
    @PostMapping("/waybills/{id}/receipt")
    public Result<Long> uploadReceipt(@RequestHeader("Authorization") String token,
                                      @PathVariable Long id,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String remark) throws IOException {
        Long driverId = driverAuthService.currentDriverId(token);
        requireOwned(id, driverId);
        return Result.ok(waybillService.uploadReceipt(id, file.getInputStream(),
                file.getOriginalFilename(), file.getContentType(), "DRIVER:" + driverId, remark));
    }

    // ------------------------------------------------------------------ 评价

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

    @PostMapping("/waybills/{id}/exception")
    public Result<Long> reportException(@RequestHeader("Authorization") String token,
                                        @PathVariable Long id,
                                        @RequestBody ExceptionReportRequest request) {
        Long driverId = driverAuthService.currentDriverId(token);
        requireOwned(id, driverId);
        request.setOperateBy("DRIVER:" + driverId);
        return Result.ok(waybillService.reportException(id, request));
    }

    // ------------------------------------------------------------------ 待办 / 消息

    @GetMapping("/todo")
    public Result<DriverTodoVo> todo(@RequestHeader("Authorization") String token) {
        Long driverId = driverAuthService.currentDriverId(token);
        DriverTodoVo todo = driverWaybillService.todo(driverId);
        todo.setUnreadMessage(messageService.unreadCount("DRIVER", driverId));
        todo.setCertAlerts(driverAuthService.certAlerts(driverId));
        return Result.ok(todo);
    }

    @GetMapping("/messages")
    public Result<List<MsgRecord>> messages(@RequestHeader("Authorization") String token,
                                            @RequestParam(required = false) Boolean unread,
                                            @RequestParam(required = false) Integer limit) {
        Long driverId = driverAuthService.currentDriverId(token);
        return Result.ok(messageService.inboxByReceiver("DRIVER", driverId, unread, limit));
    }

    @PostMapping("/messages/read")
    public Result<Integer> readMessages(@RequestHeader("Authorization") String token,
                                        @RequestBody List<Long> ids) {
        Long driverId = driverAuthService.currentDriverId(token);
        // 只允许把自己名下的站内信标记为已读
        List<Long> owned = new ArrayList<Long>();
        for (MsgRecord r : messageService.inboxByReceiver("DRIVER", driverId, null, 200)) {
            if (ids != null && ids.contains(r.getId())) {
                owned.add(r.getId());
            }
        }
        return Result.ok(messageService.markInboxRead(owned));
    }

    // ------------------------------------------------------------------ 轨迹上报

    /**
     * APP 轨迹批量上报。与后台 /api/tracks/batch 的区别：
     * 逐点校验 waybillId 是否属于当前司机，司机不能给别人的运单刷轨迹。
     */
    @PostMapping("/tracks/batch")
    public Result<Integer> reportTracks(@RequestHeader("Authorization") String token,
                                        @RequestBody TrackBatchRequest request) {
        Long driverId = driverAuthService.currentDriverId(token);
        List<TrackPointDto> points = request.getPoints();
        if (points == null || points.isEmpty()) {
            return Result.ok(0);
        }
        List<Long> waybillIds = new ArrayList<Long>();
        for (TrackPointDto p : points) {
            if (p.getWaybillId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "轨迹点缺少运单号");
            }
            requireOwned(p.getWaybillId(), driverId);
            p.setDriverId(driverId);
            if (!waybillIds.contains(p.getWaybillId())) {
                waybillIds.add(p.getWaybillId());
            }
        }
        int inserted = trackService.report(points);
        if (inserted > 0) {
            alarmService.analyzeQuietly(waybillIds);
        }
        return Result.ok(inserted);
    }

    // ------------------------------------------------------------------ 内部

    private void requireOwned(Long waybillId, Long driverId) {
        WbWaybill wb = waybillService.detail(waybillId);
        if (wb == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "运单不存在");
        }
        if (!driverId.equals(wb.getDriverId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该运单");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }
}
