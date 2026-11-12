import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一响应结构：{ code, message, data, traceId, timestamp }
// 后端成功约定 code === '00000'；非 2xx HTTP（如 401）走 error 分支。
const TOKEN_KEY = 'freight_token'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== '00000') {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Business error'))
    }
    // 解包：直接返回业务 data
    return res.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem(TOKEN_KEY)
        ElMessage.error('登录已过期，请重新登录')
        // 整页跳转避免与路由模块形成循环依赖
        window.location.assign('/login')
      } else {
        const msg = error.response.data && error.response.data.message
        ElMessage.error(msg || `请求错误 (${status})`)
      }
    } else {
      ElMessage.error('网络异常，无法连接后端服务')
    }
    return Promise.reject(error)
  }
)

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export default service
