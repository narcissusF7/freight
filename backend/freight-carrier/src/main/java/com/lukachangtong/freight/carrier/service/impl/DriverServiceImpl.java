package com.lukachangtong.freight.carrier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.entity.CrVehicle;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import com.lukachangtong.freight.carrier.mapper.CrVehicleMapper;
import com.lukachangtong.freight.carrier.service.DriverService;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private CrDriverMapper driverMapper;
    @Resource
    private CrVehicleMapper vehicleMapper;

    @Override
    public List<CrDriver> list(Long orgId) {
        QueryWrapper<CrDriver> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("id");
        return driverMapper.selectList(qw);
    }

    @Override
    public Long create(CrDriver driver) {
        if (driver.getAuditStatus() == null) {
            driver.setAuditStatus(0);
        }
        driver.setCreateTime(LocalDateTime.now());
        driver.setUpdateTime(LocalDateTime.now());
        driverMapper.insert(driver);
        return driver.getId();
    }

    @Override
    public void update(CrDriver driver) {
        driver.setUpdateTime(LocalDateTime.now());
        driverMapper.updateById(driver);
    }

    @Override
    public void delete(Long id) {
        driverMapper.deleteById(id);
    }

    @Override
    public void audit(Long id, Integer status, String remark, String operator) {
        CrDriver upd = new CrDriver();
        upd.setId(id);
        upd.setAuditStatus(status);
        upd.setAuditRemark(remark);
        upd.setUpdateBy(operator);
        upd.setUpdateTime(LocalDateTime.now());
        driverMapper.updateById(upd);
    }

    // ---------------------------------------------------------------- 司机端账号

    @Override
    public CrDriver findById(Long id) {
        return id == null ? null : driverMapper.selectById(id);
    }

    @Override
    public CrDriver findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        List<CrDriver> list = driverMapper.selectList(
                new QueryWrapper<CrDriver>().eq("phone", phone.trim()));
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            // 历史数据存在同号多条；不猜哪条是他的，交给运营处理，避免登录到已离职司机的档案
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "该手机号对应多条司机档案，请联系运营处理后再登录");
        }
        return list.get(0);
    }

    @Override
    public void savePasswordHash(Long id, String passwordHash) {
        CrDriver upd = new CrDriver();
        upd.setId(id);
        upd.setPasswordHash(passwordHash);
        upd.setUpdateTime(LocalDateTime.now());
        driverMapper.updateById(upd);
    }

    @Override
    public void recordLoginSuccess(Long id) {
        // 失败计数要用 set 覆盖为 0，updateById 会跳过 null 但不会跳过 0
        CrDriver upd = new CrDriver();
        upd.setId(id);
        upd.setLastLoginTime(LocalDateTime.now());
        upd.setLoginFailCount(0);
        upd.setUpdateTime(LocalDateTime.now());
        driverMapper.updateById(upd);
        // lock_until 需要显式置 NULL，updateById 做不到
        driverMapper.update(null, new UpdateWrapper<CrDriver>()
                .set("lock_until", null).eq("id", id));
    }

    @Override
    public int recordLoginFailure(Long id) {
        CrDriver d = driverMapper.selectById(id);
        int next = (d == null || d.getLoginFailCount() == null) ? 1 : d.getLoginFailCount() + 1;
        driverMapper.update(null, new UpdateWrapper<CrDriver>()
                .set("login_fail_count", next)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
        return next;
    }

    @Override
    public void lock(Long id, LocalDateTime lockUntil) {
        driverMapper.update(null, new UpdateWrapper<CrDriver>()
                .set("lock_until", lockUntil)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
    }

    @Override
    public void setOnline(Long id, boolean online) {
        driverMapper.update(null, new UpdateWrapper<CrDriver>()
                .set("online_status", online ? 1 : 0)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
    }

    @Override
    public void bindPushClient(Long id, String clientId) {
        driverMapper.update(null, new UpdateWrapper<CrDriver>()
                .set("push_client_id", clientId)
                .set("update_time", LocalDateTime.now())
                .eq("id", id));
    }

    @Override
    public List<Long> vehicleIdsOf(Long driverId) {
        List<CrVehicle> list = vehicleMapper.selectList(new QueryWrapper<CrVehicle>()
                .eq("driver_id", driverId).eq("audit_status", 1).orderByDesc("id"));
        List<Long> ids = new ArrayList<>();
        for (CrVehicle v : list) {
            ids.add(v.getId());
        }
        return ids;
    }
}
