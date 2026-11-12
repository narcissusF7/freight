package com.lukachangtong.freight.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.dto.EmployeeSaveRequest;
import com.lukachangtong.freight.auth.entity.SysOrg;
import com.lukachangtong.freight.auth.entity.SysRole;
import com.lukachangtong.freight.auth.mapper.SysOrgMapper;
import com.lukachangtong.freight.auth.mapper.SysRoleMapper;
import com.lukachangtong.freight.auth.mapper.SysUserMapper;
import com.lukachangtong.freight.auth.service.EmployeeService;
import com.lukachangtong.freight.auth.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * dev 环境默认数据初始化：平台根机构 + 超级管理员角色 + admin 账号。
 * 仅 dev 激活；生产由部署脚本/初始化 SQL 注入，绝不在此硬编码任何凭据到代码。
 */
@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource
    private SysOrgMapper orgMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private EmployeeService employeeService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            init();
        } catch (Exception e) {
            // 初始化失败不应阻断启动；记录日志由运维排查（如库表尚未 Flyway 执行）
            log.warn("dev data init skipped: {}", e.getMessage());
        }
    }

    private void init() {
        if (userMapper.selectCount(new QueryWrapper<>()) > 0) {
            return;
        }
        SysOrg root = new SysOrg();
        root.setParentId(0L);
        root.setOrgName("鹭卡畅通平台");
        root.setOrgCode("ROOT");
        root.setStatus(1);
        orgMapper.insert(root);

        SysRole adminRole = new SysRole();
        adminRole.setOrgId(root.getId());
        adminRole.setRoleCode("SUPER_ADMIN");
        adminRole.setRoleName("超级管理员");
        roleService.create(adminRole);

        EmployeeSaveRequest admin = new EmployeeSaveRequest();
        admin.setOrgId(root.getId());
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setRealName("超级管理员");
        admin.setStatus(1);
        admin.setRoleIds(java.util.Collections.singletonList(adminRole.getId()));
        employeeService.create(admin);
        log.info("dev default admin created: username=admin / password=admin123 (please change)");
    }
}
