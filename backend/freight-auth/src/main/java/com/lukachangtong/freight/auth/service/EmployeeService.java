package com.lukachangtong.freight.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.dto.EmployeeSaveRequest;
import com.lukachangtong.freight.auth.entity.SysUser;
import com.lukachangtong.freight.auth.entity.SysUserRole;
import com.lukachangtong.freight.auth.mapper.SysUserMapper;
import com.lukachangtong.freight.auth.mapper.SysUserRoleMapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/** 员工/账号管理（系统设置-员工管理）。 */
@Service
public class EmployeeService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    public List<SysUser> list(Long orgId) {
        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        return userMapper.selectList(qw);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeSaveRequest req) {
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", req.getUsername())) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "登录账号已存在");
        }
        SysUser user = new SysUser();
        user.setOrgId(req.getOrgId());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhoneEncrypted(req.getPhoneEncrypted());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(user);
        assignRoles(user.getId(), req.getRoleIds());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(EmployeeSaveRequest req) {
        if (req.getId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "缺少主键");
        }
        SysUser user = userMapper.selectById(req.getId());
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账号不存在");
        }
        user.setOrgId(req.getOrgId());
        user.setRealName(req.getRealName());
        user.setPhoneEncrypted(req.getPhoneEncrypted());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (req.getPassword() != null && req.getPassword().length() > 0) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.updateById(user);
        if (req.getRoleIds() != null) {
            assignRoles(user.getId(), req.getRoleIds());
        }
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                userRoleMapper.insert(new SysUserRole(userId, roleId));
            }
        }
    }

    public List<Long> listRoleIds(Long userId) {
        List<SysUserRole> list = userRoleMapper.selectList(
                new QueryWrapper<SysUserRole>().eq("user_id", userId));
        List<Long> ids = new java.util.ArrayList<Long>();
        for (SysUserRole ur : list) {
            ids.add(ur.getRoleId());
        }
        return ids;
    }
}
