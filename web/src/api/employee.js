import request from '@/utils/request'

// 员工（账号）列表：可选 orgId 过滤，返回 List<SysUser>
export function listEmployees(orgId) {
  return request.get('/employees', { params: { orgId } })
}

// 创建/更新：EmployeeSaveRequest { id?, orgId, username, password?, realName, phoneEncrypted, status, roleIds }
export function createEmployee(data) {
  return request.post('/employees', data)
}

export function updateEmployee(id, data) {
  return request.put(`/employees/${id}`, data)
}

export function deleteEmployee(id) {
  return request.delete(`/employees/${id}`)
}

// 员工授权角色：body number[]
export function assignRoles(id, roleIds) {
  return request.post(`/employees/${id}/roles`, roleIds)
}

export function listEmployeeRoles(id) {
  return request.get(`/employees/${id}/roles`)
}
