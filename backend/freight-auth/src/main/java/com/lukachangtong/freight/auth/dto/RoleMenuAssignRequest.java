package com.lukachangtong.freight.auth.dto;

import java.util.List;

/** 角色授权菜单请求。 */
public class RoleMenuAssignRequest {

    private List<Long> menuIds;

    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
}
