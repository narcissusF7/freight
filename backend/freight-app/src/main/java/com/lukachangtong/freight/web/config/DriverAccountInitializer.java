package com.lukachangtong.freight.web.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.carrier.entity.CrDriver;
import com.lukachangtong.freight.carrier.mapper.CrDriverMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 开发环境：给已审核通过的司机补默认登录密码（123456），方便本地/演示环境直接登 APP。
 *
 * 放在应用层而不是 auth 模块：它同时依赖 auth 的密码编码器与 carrier 的司机表，
 * 让 auth 依赖 carrier 会把「认证域」变成业务域的下游，ArchUnit 的循环检查迟早会报。
 *
 * **只在 dev 生效**：生产环境的司机密码必须由司机本人通过「手机号 + 短信验证码」自助设置，
 * 系统批量发初始密码是典型的弱口令来源（同号同密、从不修改）。
 * 仅补 password_hash 为空的行，重复启动不会覆盖已改过的密码。
 */
@Component
@Profile("dev")
public class DriverAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DriverAccountInitializer.class);
    private static final String DEMO_PASSWORD = "123456";

    @Resource
    private CrDriverMapper driverMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        QueryWrapper<CrDriver> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1).and(w -> w.isNull("password_hash").or().eq("password_hash", ""));
        List<CrDriver> drivers = driverMapper.selectList(qw);
        if (drivers.isEmpty()) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        int n = 0;
        for (CrDriver d : drivers) {
            CrDriver upd = new CrDriver();
            upd.setId(d.getId());
            upd.setPasswordHash(hash);
            driverMapper.updateById(upd);
            n++;
        }
        log.info("开发环境已为 {} 位审核通过的司机初始化司机端密码（默认 {}），示例账号：{}",
                n, DEMO_PASSWORD, drivers.get(0).getPhone());
    }
}
