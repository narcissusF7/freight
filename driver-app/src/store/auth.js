import { reactive } from 'vue'
import * as api from '@/api/driver'

/**
 * 司机会话状态。
 *
 * 没引入 Pinia：司机端只有「当前司机 + 在线状态」两个共享状态，
 * 一个 reactive 对象就够；pinia 在这里只会增加一层需要维护的心智负担。
 */
export const state = reactive({
  token: uni.getStorageSync('driver_token') || '',
  profile: uni.getStorageSync('driver_profile') || null,
  online: false
})

export function setSession(loginData) {
  state.token = loginData.token || ''
  state.profile = loginData
  state.online = loginData.onlineStatus === 1
  uni.setStorageSync('driver_token', state.token)
  uni.setStorageSync('driver_profile', loginData)
}

export async function refreshProfile() {
  try {
    const p = await api.profile()
    state.profile = p
    state.online = p.onlineStatus === 1
    uni.setStorageSync('driver_profile', p)
    return p
  } catch (e) {
    return state.profile
  }
}

export function logout() {
  state.token = ''
  state.profile = null
  state.online = false
  uni.removeStorageSync('driver_token')
  uni.removeStorageSync('driver_profile')
  uni.reLaunch({ url: '/pages/login/login' })
}
