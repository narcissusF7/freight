import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/request'
import Layout from '@/layout/index.vue'

// 内置导航：后续可替换为后端 /api/menus/tree 动态路由。
export const navMenus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  {
    title: '系统设置',
    icon: 'Setting',
    children: [
      { path: '/org', title: '机构管理', icon: 'OfficeBuilding' },
      { path: '/role', title: '角色管理', icon: 'UserFilled' },
      { path: '/menu', title: '菜单权限', icon: 'Menu' },
      { path: '/employee', title: '员工管理', icon: 'User' },
      { path: '/file', title: '文件管理', icon: 'FolderOpened' },
      { path: '/message', title: '消息中心', icon: 'Bell' }
    ]
  },
  {
    title: '承运管理',
    icon: 'Van',
    children: [
      { path: '/driver', title: '司机管理', icon: 'User' },
      { path: '/vehicle', title: '车辆管理', icon: 'Ship' },
      { path: '/expiry', title: '证件到期预警', icon: 'AlarmClock' }
    ]
  },
  {
    title: '运输管理',
    icon: 'Box',
    children: [
      { path: '/goods-source', title: '货源发布', icon: 'Goods' },
      { path: '/waybill', title: '运单管理', icon: 'Document' },
      { path: '/contract', title: '电子合同', icon: 'EditPen' }
    ]
  },
  {
    title: '监控预警',
    icon: 'Aim',
    children: [
      { path: '/fence', title: '电子围栏', icon: 'Location' },
      { path: '/alarm', title: '轨迹预警', icon: 'Warning' }
    ]
  },
  {
    title: '财务管理',
    icon: 'Money',
    children: [
      { path: '/settlement', title: '结算管理', icon: 'Tickets' },
      { path: '/payment', title: '支付管理', icon: 'CreditCard' },
      { path: '/wallet', title: '钱包与提现', icon: 'Wallet' },
      { path: '/invoice', title: '发票管理', icon: 'Document' },
      { path: '/reconciliation', title: '对账中心', icon: 'Checked' }
    ]
  },
  {
    title: '客服中心',
    icon: 'Service',
    children: [
      { path: '/complaint', title: '投诉管理', icon: 'ChatDotRound' },
      { path: '/rating', title: '星级评价', icon: 'Star' },
      { path: '/credit', title: '信用管理', icon: 'Medal' }
    ]
  },
  { path: '/stat', title: '统计中心', icon: 'DataAnalysis' },
  { path: '/report', title: '监管上报', icon: 'Upload' }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'org', name: 'Org', component: () => import('@/views/org/index.vue'), meta: { title: '机构管理' } },
      { path: 'role', name: 'Role', component: () => import('@/views/role/index.vue'), meta: { title: '角色管理' } },
      { path: 'menu', name: 'Menu', component: () => import('@/views/menu/index.vue'), meta: { title: '菜单权限' } },
      { path: 'employee', name: 'Employee', component: () => import('@/views/employee/index.vue'), meta: { title: '员工管理' } },
      { path: 'file', name: 'File', component: () => import('@/views/file/index.vue'), meta: { title: '文件管理' } },
      { path: 'message', name: 'Message', component: () => import('@/views/message/index.vue'), meta: { title: '消息中心' } },
      { path: 'driver', name: 'Driver', component: () => import('@/views/driver/index.vue'), meta: { title: '司机管理' } },
      { path: 'vehicle', name: 'Vehicle', component: () => import('@/views/vehicle/index.vue'), meta: { title: '车辆管理' } },
      { path: 'expiry', name: 'Expiry', component: () => import('@/views/expiry/index.vue'), meta: { title: '证件到期预警' } },
      { path: 'goods-source', name: 'GoodsSource', component: () => import('@/views/goods-source/index.vue'), meta: { title: '货源发布' } },
      { path: 'waybill', name: 'Waybill', component: () => import('@/views/waybill/index.vue'), meta: { title: '运单管理' } },
      { path: 'contract', name: 'Contract', component: () => import('@/views/contract/index.vue'), meta: { title: '电子合同' } },
      { path: 'fence', name: 'Fence', component: () => import('@/views/fence/index.vue'), meta: { title: '电子围栏' } },
      { path: 'alarm', name: 'Alarm', component: () => import('@/views/alarm/index.vue'), meta: { title: '轨迹预警' } },
      { path: 'settlement', name: 'Settlement', component: () => import('@/views/settlement/index.vue'), meta: { title: '结算管理' } },
      { path: 'payment', name: 'Payment', component: () => import('@/views/payment/index.vue'), meta: { title: '支付管理' } },
      { path: 'wallet', name: 'Wallet', component: () => import('@/views/wallet/index.vue'), meta: { title: '钱包与提现' } },
      { path: 'invoice', name: 'Invoice', component: () => import('@/views/invoice/index.vue'), meta: { title: '发票管理' } },
      { path: 'reconciliation', name: 'Reconciliation', component: () => import('@/views/reconciliation/index.vue'), meta: { title: '对账中心' } },
      { path: 'complaint', name: 'Complaint', component: () => import('@/views/complaint/index.vue'), meta: { title: '投诉管理' } },
      { path: 'rating', name: 'Rating', component: () => import('@/views/rating/index.vue'), meta: { title: '星级评价' } },
      { path: 'credit', name: 'Credit', component: () => import('@/views/credit/index.vue'), meta: { title: '信用管理' } },
      { path: 'stat', name: 'Stat', component: () => import('@/views/stat/index.vue'), meta: { title: '统计中心' } },
      { path: 'report', name: 'Report', component: () => import('@/views/report/index.vue'), meta: { title: '监管上报' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = getToken()
  // 未登录访问受保护页 -> 跳转登录（携带 redirect）
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录再访问登录页 -> 进首页
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
  return true
})

export default router
