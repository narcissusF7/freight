package com.lukachangtong.freight.carrier.service;

import com.lukachangtong.freight.carrier.entity.CrDriver;

import java.time.LocalDateTime;
import java.util.List;

public interface DriverService {

    List<CrDriver> list(Long orgId);

    Long create(CrDriver driver);

    void update(CrDriver driver);

    void delete(Long id);

    /** 审核：status 0 待审 / 1 通过 / 2 驳回；operator 为操作人（合规留痕）。 */
    void audit(Long id, Integer status, String remark, String operator);

    // ---------------------------------------------------------------- 司机端账号

    CrDriver findById(Long id);

    /** 按手机号查司机（司机端登录凭据）；同号多条时抛业务异常，避免登录到错误档案。 */
    CrDriver findByPhone(String phone);

    /** 写入密码哈希（BCrypt）。由应用层编码后传入，承运模块不引入安全框架依赖。 */
    void savePasswordHash(Long id, String passwordHash);

    /** 登录成功：刷新最后登录时间、清零失败计数、解除锁定。 */
    void recordLoginSuccess(Long id);

    /**
     * 登录失败：失败计数 +1。
     * @return 累计失败次数（达到阈值由调用方决定是否锁定）
     */
    int recordLoginFailure(Long id);

    void lock(Long id, LocalDateTime lockUntil);

    void setOnline(Long id, boolean online);

    void bindPushClient(Long id, String clientId);

    /** 司机名下车辆（已审核通过），司机端发车前选车用。 */
    List<Long> vehicleIdsOf(Long driverId);
}
