<template>
  <view class="page">
    <view class="head">
      <view class="avatar">{{ initial }}</view>
      <view class="who">
        <text class="name">{{ profile ? profile.name : '-' }}</text>
        <text class="phone">{{ profile ? profile.phone : '' }}</text>
      </view>
      <view class="online" :class="{ on: online }" @click="toggleOnline">
        {{ online ? '接单中' : '已收车' }}
      </view>
    </view>

    <view class="card">
      <view class="kv"><text class="k">准入状态</text>
        <text class="v" :class="auditClass">{{ auditText }}</text>
      </view>
      <view class="kv"><text class="k">当前车辆</text><text class="v">{{ profile && profile.plateNo ? profile.plateNo : '未绑定' }}</text></view>
      <view class="kv"><text class="k">归属机构</text><text class="v">{{ profile ? profile.orgId : '-' }}</text></view>
      <view class="kv"><text class="k">证照提醒</text>
        <text class="v" :class="{ danger: profile && profile.certAlerts > 0 }">
          {{ profile && profile.certAlerts ? profile.certAlerts + ' 项待处理' : '正常' }}
        </text>
      </view>
    </view>

    <view v-if="profile && profile.certAlertMessages && profile.certAlertMessages.length" class="card warn">
      <text class="warn-title">证照到期明细</text>
      <text v-for="(m, i) in profile.certAlertMessages" :key="i" class="warn-text">· {{ m }}</text>
    </view>

    <view class="card menu">
      <view class="menu-item" @click="openMessage">
        <text>消息中心</text>
        <text class="muted">查看全部 ›</text>
      </view>
      <view class="menu-item" @click="showPassword">
        <text>修改密码</text>
        <text class="muted">›</text>
      </view>
      <view class="menu-item" @click="showPush">
        <text>推送登记</text>
        <text class="muted">APP 后台提醒 ›</text>
      </view>
      <view class="menu-item" @click="showAbout">
        <text>关于</text>
        <text class="muted">v0.1.0 ›</text>
      </view>
    </view>

    <view class="logout" @click="doLogout">退出登录</view>

    <tab-bar current="mine" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import * as api from '@/api/driver'
import { state, refreshProfile, logout } from '@/store/auth'

const profile = computed(() => state.profile)
const online = computed(() => state.online)
const initial = computed(() => (state.profile && state.profile.name ? state.profile.name.slice(0, 1) : '司'))

const auditText = computed(() => {
  const s = state.profile ? state.profile.auditStatus : null
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return '审核中'
})
const auditClass = computed(() => {
  const s = state.profile ? state.profile.auditStatus : null
  return s === 1 ? 'ok' : s === 2 ? 'danger' : 'warn'
})

onShow(() => {
  refreshProfile()
})

async function toggleOnline() {
  const next = !state.online
  try {
    await api.setOnline(next)
    state.online = next
  } catch (e) {
    // 已提示
  }
}

function openMessage() {
  uni.navigateTo({ url: '/pages/message/list' })
}

function showPassword() {
  uni.showModal({
    title: '修改密码',
    editable: true,
    placeholderText: '请输入原密码',
    success: async (res1) => {
      if (!res1.confirm || !res1.content) return
      uni.showModal({
        title: '设置新密码',
        editable: true,
        placeholderText: '至少 6 位',
        success: async (res2) => {
          if (!res2.confirm || !res2.content) return
          try {
            await api.changePassword(res1.content, res2.content)
            uni.showToast({ title: '密码已修改，请重新登录', icon: 'none' })
            setTimeout(() => logout(), 800)
          } catch (e) {
            // 已提示
          }
        }
      })
    }
  })
}

function showPush() {
  uni.showModal({
    title: '推送登记',
    content: 'APP 打包后在 manifest 中配置 uni-push 证书，启动时会自动把 clientId 上报到服务端；'
      + 'H5 调试环境不支持离线推送，请在「消息中心」查看。',
    showCancel: false
  })
}

function showAbout() {
  uni.showModal({
    title: '关于',
    content: '鹭卡畅通 · 司机端 v0.1.0\n网络货运平台（M3 司机端）\n找货接单 / 节点确认 / 轨迹上报 / 回单上传',
    showCancel: false
  })
}

function doLogout() {
  uni.showModal({
    title: '退出登录',
    content: '退出后将停止轨迹采集，在途运单请确认已上报',
    success: (res) => {
      if (res.confirm) logout()
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 20rpx;
}

.head {
  background: linear-gradient(135deg, #1a4fdc, #2f6bff);
  padding: 44rpx 36rpx 52rpx;
  display: flex;
  align-items: center;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 700;
  text-align: center;
  line-height: 108rpx;
}

.who {
  flex: 1;
  margin-left: 24rpx;
}

.name {
  display: block;
  color: #ffffff;
  font-size: 38rpx;
  font-weight: 700;
}

.phone {
  display: block;
  color: rgba(255, 255, 255, 0.8);
  font-size: 26rpx;
  margin-top: 8rpx;
}

.online {
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 26rpx;
}

.online.on {
  background: #ffffff;
  color: #1a4fdc;
  font-weight: 600;
}

.card {
  margin-top: 0;
}

.card:first-of-type {
  margin-top: -30rpx;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 2rpx solid #f2f5fa;
}

.kv:last-child {
  border-bottom: none;
}

.k {
  color: #8a94a6;
  font-size: 28rpx;
}

.v {
  font-size: 28rpx;
}

.ok {
  color: #12b76a;
}

.warn {
  color: #c2751b;
}

.danger {
  color: #d9363e;
}

.card.warn {
  background: #fff8ec;
  border-left: 8rpx solid #f0a020;
}

.warn-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #8a5a00;
  margin-bottom: 8rpx;
}

.warn-text {
  display: block;
  font-size: 26rpx;
  color: #8a5a00;
  line-height: 1.7;
}

.menu {
  padding: 0 28rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 2rpx solid #f2f5fa;
  font-size: 30rpx;
}

.menu-item:last-child {
  border-bottom: none;
}

.logout {
  margin: 40rpx 24rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background: #ffffff;
  color: #d9363e;
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
