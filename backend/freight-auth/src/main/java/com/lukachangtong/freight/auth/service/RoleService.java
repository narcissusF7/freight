package com.lukachangtong.freight.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.dto.RoleMenuAssignRequest;
import com.lukachangtong.freight.auth.entity.SysRole;
import com.lukachangtong.freight.auth.entity.SysRoleMenu;
import com.lukachangtong.freight.auth.mapper.SysRoleMapper;
import com.lukachangtong.freight.auth.mapper.SysRoleMenuMapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/** 角色管理（系统设置-角色信息）与菜单授权。 */
@Service
public class RoleService {

    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    public List<SysRole> list(Long orgId) {
        QueryWrapper<SysRole> qw = new QueryWrapper<SysRole>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        return roleMapper.selectList(qw);
    }

    public Long create(SysRole role) {
        if (roleMapper.selectCount(new QueryWrapper<SysRole>()
                .eq("org_id", role.getOrgId()).eq("role_code", role.getRoleCode())) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "同机构角色编码已存在");
        }
        roleMapper.insert(role);
        return role.getId();
    }

    public void update(SysRole role) {
        if (role.getId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "缺少主键");
        }
        roleMapper.updateById(role);
    }

    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, RoleMenuAssignRequest request) {
        roleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
        List<Long> menuIds = request.getMenuIds();
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
            }
        }
    }

    public List<Long> listMenuIds(Long roleId) {
        List<SysRoleMenu> list = roleMenuMapper.selectList(
                new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
        List<Long> ids = new ArrayList<Long>();
        for (SysRoleMenu rm : list) {
            ids.add(rm.getMenuId());
        }
        return ids;
    }
}
