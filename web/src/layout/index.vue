<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="app-aside">
      <div class="logo">网络货运平台</div>
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <Navbar />
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
</script>

<style scoped>
.app-layout {
  height: 100%;
}
.app-aside {
  background: #001529;
  overflow: hidden;
}
.logo {
  height: 56px;
  line-height: 56px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  background: #002140;
  letter-spacing: 1px;
}
.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-bottom: 1px solid #e8e8e8;
}
.app-main {
  background: #f0f2f5;
}
</style>
