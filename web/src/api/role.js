import request from '@/utils/request'

// 角色列表：可选 orgId 过滤，返回 List<SysRole>
export function listRoles(orgId) {
  return request.get('/roles', { params: { orgId } })
}

export function createRole(data) {
  return request.post('/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/roles/${id}`)
}

// 角色授权菜单：body { menuIds: number[] }
export function assignRoleMenus(roleId, menuIds) {
  return request.post(`/roles/${roleId}/menus`, { menuIds })
}

// 角色已授权菜单 id 列表
export function listRoleMenus(roleId) {
  return request.get(`/roles/${roleId}/menus`)
}
