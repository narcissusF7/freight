package com.lukachangtong.freight.web.service;

import com.lukachangtong.freight.auth.security.JwtTokenProvider;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.carrier.service.CarrierExpiryService;
import com.lukachangtong.freight.carrier.service.DriverService;
import com.lukachangtong.freight.carrier.vo.ExpiryAlertVo;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.web.dto.DriverLoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 司机端账号服务。
 *
 * 放在应用层（freight-app）而不是 carrier 模块，因为它是**跨模块编排**：
 * 读承运模块的司机档案 + 用 auth 模块的 JWT 签发 + 拉证件到期预警。
 * 让 carrier 依赖 auth 会造成「承运域反向依赖认证域」，ArchUnit 的循环依赖检查会直接失败。
 *
 * 登录失败锁定用「数据库计数」而不是 Redis：
 * 司机端登录量级（几千人/天）远达不到需要分布式计数的程度，多一个 Redis 依赖
 * 就多一个「Redis 挂了司机登不进 APP」的故障面。真要上多实例时把
 * {@link #recordFailure} 换成 INCR + EXPIRE 即可，接口不用改。
 */
@Service
public class DriverAuthService {

    private static final Logger log = LoggerFactory.getLogger(DriverAuthService.class);

    /** 连续失败 5 次锁 10 分钟 */
    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 10;
    private static final String ROLE_DRIVER = "DRIVER";
    private static final String BAD_CREDENTIALS = "手机号或密码错误";

    @Resource
    private DriverService driverService;
    @Resource
    private CrVehicleMapper vehicleMapper;
    @Resource
    private CarrierExpiryService expiryService;
    @Resource
    private JwtTokenProvider jwtTokenProvider;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${freight.jwt.expiration-seconds:7200}")
    private long expirationSeconds;
    /** 司机端会话更长：司机在途连续作业，2 小时就过期会反复要求重登 */
    @Value("${freight.driver.token-seconds:604800}")
    private long driverTokenSeconds;

    @Transactional(rollbackFor = Exception.class)
    public DriverLoginResponse login(String phone, String password) {
        if (phone == null || phone.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "手机号和密码不能为空");
        }
        CrDriver driver = driverService.findByPhone(phone.trim());
        if (driver == null) {
            // 不区分「用户不存在」和「密码错误」，避免手机号被枚举
            throw new BizException(ErrorCode.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (driver.getLockUntil() != null && driver.getLockUntil().isAfter(LocalDateTime.now())) {
            long min = Math.max(1, Duration.between(LocalDateTime.now(), driver.getLockUntil()).toMinutes());
            throw new BizException(ErrorCode.UNAUTHORIZED,
                    "登录失败次数过多，请 " + min + " 分钟后再试");
        }
        if (driver.getPasswordHash() == null || driver.getPasswordHash().isEmpty()) {
            throw new BizException(ErrorCode.UNAUTHORIZED,
                    "该手机号尚未开通司机端账号，请联系调度在「司机管理」中维护");
        }
        if (!passwordEncoder.matches(password, driver.getPasswordHash())) {
            recordFailure(driver);
            throw new BizException(ErrorCode.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (driver.getAuditStatus() == null || driver.getAuditStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    driver.getAuditStatus() != null && driver.getAuditStatus() == 2
                            ? "司机准入审核未通过：" + (driver.getAuditRemark() == null ? "请联系调度" : driver.getAuditRemark())
                            : "司机准入审核中，请等待调度审核");
        }
        driverService.recordLoginSuccess(driver.getId());
        log.info("司机端登录成功 driverId={} phone={}", driver.getId(), mask(phone));
        return buildResponse(driver);
    }

    /** 刷新：用旧令牌换新令牌（未过期才允许）。 */
    public DriverLoginResponse refresh(String bearerToken) {
        Long driverId = currentDriverId(bearerToken);
        CrDriver driver = driverService.findById(driverId);
        if (driver == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "司机不存在");
        }
        return buildResponse(driver);
    }

    public DriverLoginResponse profile(String bearerToken) {
        Long driverId = currentDriverId(bearerToken);
        CrDriver driver = driverService.findById(driverId);
        if (driver == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "司机不存在");
        }
        DriverLoginResponse resp = buildResponse(driver);
        resp.setToken(null); // profile 不重复下发令牌
        resp.setExpiresIn(0);
        return resp;
    }

    public void changePassword(String bearerToken, String oldPassword, String newPassword) {
        Long driverId = currentDriverId(bearerToken);
        CrDriver driver = driverService.findById(driverId);
        if (driver == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "司机不存在");
        }
        if (driver.getPasswordHash() == null
                || !passwordEncoder.matches(oldPassword == null ? "" : oldPassword, driver.getPasswordHash())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "原密码不正确");
        }
        validateNewPassword(newPassword);
        driverService.savePasswordHash(driverId, passwordEncoder.encode(newPassword));
    }

    /** 首次设置密码：仅当账号未开通（passwordHash 为空）时可用。 */
    public void initPassword(String phone, String smsCode, String newPassword) {
        CrDriver driver = driverService.findByPhone(phone);
        if (driver == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "手机号未登记为司机");
        }
        if (driver.getPasswordHash() != null && !driver.getPasswordHash().isEmpty()) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "账号已开通，请直接登录或用原密码修改");
        }
        // 短信校验码接入由 freight-infra 的 SmsClient 提供；当前 mock 模式下固定 6 位任意码可用，
        // 生产必须校验真实下发的验证码（见大纲 14 节外部服务商待确认项）
        if (smsCode == null || smsCode.trim().length() != 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "验证码不正确");
        }
        validateNewPassword(newPassword);
        driverService.savePasswordHash(driver.getId(), passwordEncoder.encode(newPassword));
    }

    public void setOnline(String bearerToken, boolean online) {
        driverService.setOnline(currentDriverId(bearerToken), online);
    }

    public void registerPush(String bearerToken, String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "clientId 不能为空");
        }
        driverService.bindPushClient(currentDriverId(bearerToken), clientId.trim());
    }

    /** 从令牌解析 driverId（token 由本服务签发，含 driverId 声明）。 */
    public Long currentDriverId(String bearerToken) {
        String token = strip(bearerToken);
        if (!jwtTokenProvider.validate(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        Object id = jwtTokenProvider.parse(token).get("driverId");
        if (id == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前令牌不是司机端令牌");
        }
        return Long.valueOf(String.valueOf(id));
    }

    // ------------------------------------------------------------------ 内部

    private DriverLoginResponse buildResponse(CrDriver driver) {
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("driverId", driver.getId());
        String token = jwtTokenProvider.generate(driver.getPhone(), driver.getOrgId(),
                Arrays.asList(ROLE_DRIVER), extra, driverTokenSeconds);

        DriverLoginResponse resp = new DriverLoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(driverTokenSeconds);
        resp.setDriverId(driver.getId());
        resp.setName(driver.getName());
        resp.setPhone(driver.getPhone());
        resp.setOrgId(driver.getOrgId());
        resp.setAuditStatus(driver.getAuditStatus());
        resp.setAuditRemark(driver.getAuditRemark());
        resp.setOnlineStatus(driver.getOnlineStatus() == null ? 0 : driver.getOnlineStatus());

        List<CrVehicle> vehicles = vehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrVehicle>()
                        .eq("driver_id", driver.getId()).eq("audit_status", 1).orderByDesc("id"));
        List<Long> ids = new ArrayList<Long>();
        for (CrVehicle v : vehicles) {
            ids.add(v.getId());
        }
        resp.setVehicleIds(ids);
        resp.setPlateNo(vehicles.isEmpty() ? null : vehicles.get(0).getPlateNo());

        List<String> alerts = certAlertMessages(driver, ids);
        resp.setCertAlerts(alerts.size());
        resp.setCertAlertMessages(alerts);
        return resp;
    }

    /** 证件到期提醒条数（首页红点用）。查不到时返回 0，不能因为预警服务异常挡住司机开工。 */
    public int certAlerts(Long driverId) {
        CrDriver driver = driverService.findById(driverId);
        if (driver == null) {
            return 0;
        }
        return certAlertMessages(driver, driverService.vehicleIdsOf(driverId)).size();
    }

    private List<String> certAlertMessages(CrDriver driver, List<Long> vehicleIds) {
        List<String> alerts = new ArrayList<String>();
        try {
            List<ExpiryAlertVo> list = expiryService.listExpiring(driver.getOrgId(), 30);
            for (ExpiryAlertVo a : list) {
                if (!belongsToDriver(a, driver.getId(), vehicleIds)) {
                    continue;
                }
                alerts.add(a.getDocType() + " " + a.getExpireDate()
                        + ("EXPIRED".equals(a.getLevel()) ? " 已过期" : " 还剩 " + a.getDaysLeft() + " 天"));
            }
        } catch (Exception e) {
            log.warn("证件到期预警查询失败（不影响登录）driverId={} err={}", driver.getId(), e.getMessage());
        }
        return alerts;
    }

    private boolean belongsToDriver(ExpiryAlertVo a, Long driverId, List<Long> vehicleIds) {
        if ("DRIVER".equals(a.getBizType())) {
            return driverId.equals(a.getBizId());
        }
        if ("VEHICLE".equals(a.getBizType())) {
            return vehicleIds.contains(a.getBizId());
        }
        return false;
    }

    private void recordFailure(CrDriver driver) {
        int fails = driverService.recordLoginFailure(driver.getId());
        if (fails >= MAX_FAIL) {
            driverService.lock(driver.getId(), LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            log.warn("司机端连续登录失败 {} 次，锁定 {} 分钟 driverId={}", fails, LOCK_MINUTES, driver.getId());
        }
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新密码至少 6 位");
        }
    }

    private String strip(String bearerToken) {
        if (bearerToken == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少令牌");
        }
        return bearerToken.startsWith("Bearer ")
                ? bearerToken.substring("Bearer ".length()).trim()
                : bearerToken.trim();
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
