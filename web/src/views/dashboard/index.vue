<template>
  <div class="page-container">
    <el-card>
      <div class="welcome">
        欢迎，{{ userStore.userInfo.realName || userStore.userInfo.username }}！
      </div>
      <div class="sub">当前机构 ID：{{ userStore.userInfo.orgId ?? '—' }}</div>
    </el-card>
    <el-row :gutter="16" class="cards">
      <el-col :span="8" v-for="c in cards" :key="c.title">
        <el-card shadow="hover">
          <div class="card-title">{{ c.title }}</div>
          <div class="card-desc">{{ c.desc }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const cards = [
  { title: '机构管理', desc: '维护组织树，支撑数据隔离与权责划分' },
  { title: '角色与权限', desc: '角色授权菜单，员工绑定角色实现 RBAC' },
  { title: '文件管理', desc: '合同、证件等附件上传，对接对象存储' }
]
</script>

<style scoped>
.welcome {
  font-size: 20px;
  font-weight: 600;
}
.sub {
  margin-top: 6px;
  color: #909399;
}
.cards {
  margin-top: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.card-desc {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
