import request from '@/utils/request'

// 机构树：返回 List<SysOrgVo>（含 children）
export function treeOrgs() {
  return request.get('/orgs/tree')
}

// 机构列表：可选 orgId 过滤
export function listOrgs(orgId) {
  return request.get('/orgs', { params: { orgId } })
}

export function createOrg(data) {
  return request.post('/orgs', data)
}

export function updateOrg(id, data) {
  return request.put(`/orgs/${id}`, data)
}

export function deleteOrg(id) {
  return request.delete(`/orgs/${id}`)
}
