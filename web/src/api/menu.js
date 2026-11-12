import request from '@/utils/request'

// 菜单树：返回 List<SysMenuVo>（含 children）
export function treeMenus() {
  return request.get('/menus/tree')
}

export function listMenus() {
  return request.get('/menus')
}

export function createMenu(data) {
  return request.post('/menus', data)
}

export function updateMenu(id, data) {
  return request.put(`/menus/${id}`, data)
}

export function deleteMenu(id) {
  return request.delete(`/menus/${id}`)
}
