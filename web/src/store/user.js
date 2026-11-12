import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { getToken, setToken, clearToken } from '@/utils/request'

// 用户态：token 持久化到 localStorage，刷新页面不丢失。
export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref({ username: '', realName: '', orgId: null, roles: [] })

  async function login(username, password) {
    const data = await authApi.login(username, password)
    token.value = data.token
    setToken(data.token)
    await fetchMe()
    return data
  }

  async function fetchMe() {
    userInfo.value = await authApi.me()
    return userInfo.value
  }

  function logout() {
    token.value = ''
    userInfo.value = { username: '', realName: '', orgId: null, roles: [] }
    clearToken()
  }

  return { token, userInfo, login, fetchMe, logout }
})
