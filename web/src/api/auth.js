import request from '@/utils/request'

// 登录：返回 LoginResponse { token, tokenType, expiresIn, username, realName, orgId, roles }
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

// 当前用户信息：返回 UserInfo { username, realName, orgId, roles }
export function me() {
  return request.get('/auth/me')
}

// 刷新令牌（M1 未强制自动刷新，保留接口）
export function refresh() {
  return request.post('/auth/refresh')
}

export function logout() {
  return request.post('/auth/logout')
}
