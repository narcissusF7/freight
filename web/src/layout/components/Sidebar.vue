<template>
  <el-menu
    router
    :default-active="activeMenu"
    background-color="#001529"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    class="app-menu"
  >
    <template v-for="item in menus" :key="item.path || item.title">
      <el-menu-item v-if="!item.children" :index="item.path">
        <el-icon><component :is="iconMap[item.icon]" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
      <el-sub-menu v-else :index="item.title">
        <template #title>
          <el-icon><component :is="iconMap[item.icon]" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
          <el-icon><component :is="iconMap[child.icon]" /></el-icon>
          <span>{{ child.title }}</span>
        </el-menu-item>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { navMenus } from '@/router'
import {
  Odometer,
  OfficeBuilding,
  UserFilled,
  Menu as MenuIcon,
  User,
  FolderOpened,
  Setting,
  Van,
  Ship,
  Box,
  Goods,
  Document,
  AlarmClock,
  EditPen,
  Aim,
  Location,
  Warning,
  Bell
} from '@element-plus/icons-vue'

const iconMap = {
  Odometer,
  OfficeBuilding,
  UserFilled,
  Menu: MenuIcon,
  User,
  FolderOpened,
  Setting,
  Van,
  Ship,
  Box,
  Goods,
  Document,
  AlarmClock,
  EditPen,
  Aim,
  Location,
  Warning,
  Bell
}

const route = useRoute()
const menus = navMenus
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.app-menu {
  border-right: none;
  height: calc(100% - 56px);
}
</style>
