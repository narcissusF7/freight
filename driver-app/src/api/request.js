/**
 * 统一请求封装（uni.request）。
 *
 * 三条约定：
 * 1. H5 走 vite 代理（/api → 8088），APP 打包时用 VITE_API_BASE 指向网关，业务代码不用改；
 * 2. 后端统一响应体 { code, message, data }，code !== '00000' 一律 reject，调用方只处理成功分支；
 * 3. 401 表示令牌失效：清掉本地令牌回登录页，避免每个页面各写一遍。
 */
const BASE = (import.meta.env && import.meta.env.VITE_API_BASE) || ''

export function getToken() {
  return uni.getStorageSync('driver_token') || ''
}

export function request({ url, method = 'GET', data, header = {}, silent = false }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      url: BASE + url,
      method,
      data,
      timeout: 20000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...header
      },
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('driver_token')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 600)
          reject({ code: 'A0002', message: '登录已过期' })
          return
        }
        if (res.statusCode === 403 && !silent) {
          uni.showToast({ title: '无权限访问', icon: 'none' })
        }
        const body = res.data || {}
        if (body.code === '00000') {
          resolve(body.data)
          return
        }
        if (!silent) {
          uni.showToast({ title: body.message || '请求失败', icon: 'none', duration: 2500 })
        }
        reject({ code: body.code, message: body.message })
      },
      fail: (err) => {
        if (!silent) {
          uni.showToast({ title: '网络异常，请检查网络', icon: 'none' })
        }
        reject({ code: 'NETWORK', message: (err && err.errMsg) || '网络异常' })
      }
    })
  })
}

/** 文件上传（回单/凭证）：uni.uploadFile 不支持 JSON 响应解析，这里补上 */
export function upload({ url, filePath, name = 'file', formData = {} }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: BASE + url,
      filePath,
      name,
      formData,
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        let body = {}
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          reject({ code: 'BAD_JSON', message: '上传响应解析失败' })
          return
        }
        if (body.code === '00000') {
          resolve(body.data)
        } else {
          uni.showToast({ title: body.message || '上传失败', icon: 'none' })
          reject({ code: body.code, message: body.message })
        }
      },
      fail: (err) => reject({ code: 'NETWORK', message: (err && err.errMsg) || '上传失败' })
    })
  })
}
