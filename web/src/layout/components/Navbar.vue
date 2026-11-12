<template>
  <div class="navbar">
    <span class="navbar-title">网络货运平台 · 管理后台</span>
    <el-dropdown @command="handleCommand">
      <span class="navbar-user">
        <el-icon><UserFilled /></el-icon>
        {{ userStore.userInfo.realName || userStore.userInfo.username || '未登录' }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

// 刷新页面只恢复了 token，用户信息在内存里会丢；这里补一次 /auth/me，
// 否则顶部一直显示「未登录」，运营会以为掉线了。
onMounted(() => {
  if (getToken() && !userStore.userInfo.username) {
    userStore.fetchMe().catch(() => {})
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.navbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.navbar-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
</style>
