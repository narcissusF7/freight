package com.lukachangtong.freight.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.entity.SysMenu;
import com.lukachangtong.freight.auth.mapper.SysMenuMapper;
import com.lukachangtong.freight.auth.vo.SysMenuVo;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 菜单/权限管理（系统设置-菜单权限）。 */
@Service
public class MenuService {

    @Resource
    private SysMenuMapper menuMapper;

    public List<SysMenuVo> tree() {
        List<SysMenu> all = menuMapper.selectList(
                new QueryWrapper<SysMenu>().eq("status", 1).orderByAsc("sort"));
        Map<Long, SysMenuVo> map = new HashMap<Long, SysMenuVo>();
        for (SysMenu m : all) {
            map.put(m.getId(), toVo(m));
        }
        List<SysMenuVo> roots = new ArrayList<SysMenuVo>();
        for (SysMenuVo vo : map.values()) {
            Long pid = vo.getParentId();
            if (pid == null || pid == 0L || !map.containsKey(pid)) {
                roots.add(vo);
            } else {
                map.get(pid).getChildren().add(vo);
            }
        }
        return roots;
    }

    public List<SysMenu> list() {
        return menuMapper.selectList(null);
    }

    public Long create(SysMenu menu) {
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(SysMenu menu) {
        if (menu.getId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "缺少主键");
        }
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        if (menuMapper.selectCount(new QueryWrapper<SysMenu>().eq("parent_id", id)) > 0) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    private SysMenuVo toVo(SysMenu m) {
        SysMenuVo vo = new SysMenuVo();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setMenuName(m.getMenuName());
        vo.setMenuType(m.getMenuType());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setPermission(m.getPermission());
        vo.setSort(m.getSort());
        vo.setStatus(m.getStatus());
        return vo;
    }
}
