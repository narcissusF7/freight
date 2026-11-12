<template>
  <view class="page">
    <view class="header">
      <view class="between">
        <view class="head-left">
          <view class="hello">{{ greeting }}，{{ profile ? profile.name : '司机' }}</view>
          <view class="sub">
            {{ profile && profile.plateNo ? '当前车辆 ' + profile.plateNo : '暂无审核通过的车辆' }}
          </view>
        </view>
        <view class="online" :class="{ on: online }" @click="toggleOnline">
          <text class="online-dot" />
          <text>{{ online ? '接单中' : '已收车' }}</text>
        </view>
      </view>
    </view>

    <!-- 未审核：直接挡在最上面，避免司机接了单才发现不能承运 -->
    <view v-if="profile && profile.auditStatus !== 1" class="card warn">
      <text class="warn-title">{{ profile.auditStatus === 2 ? '准入审核未通过' : '准入审核中' }}</text>
      <text class="warn-text">
        {{ profile.auditRemark || '请等待调度完成资质审核，通过后即可接单' }}
      </text>
    </view>

    <view v-if="profile && profile.certAlerts > 0" class="card warn">
      <text class="warn-title">证照提醒（{{ profile.certAlerts }}）</text>
      <text v-for="(m, i) in profile.certAlertMessages" :key="i" class="warn-text">· {{ m }}</text>
    </view>

    <view class="stat-row">
      <view class="stat" @click="goList('PENDING_LOAD')">
        <text class="stat-num">{{ todo.pendingLoad || 0 }}</text>
        <text class="stat-label">待装货</text>
      </view>
      <view class="stat" @click="goList('IN_TRANSIT')">
        <text class="stat-num">{{ todo.inTransit || 0 }}</text>
        <text class="stat-label">运输中</text>
      </view>
      <view class="stat" @click="goList('PENDING_SIGN')">
        <text class="stat-num">{{ todo.pendingSign || 0 }}</text>
        <text class="stat-label">待签收</text>
      </view>
      <view class="stat" @click="goMessage">
        <text class="stat-num">{{ todo.unreadMessage || 0 }}</text>
        <text class="stat-label">未读消息</text>
      </view>
    </view>

    <view v-if="!ongoing.length" class="card empty">
      <text class="empty-title">当前没有进行中的运单</text>
      <text class="muted">去货源大厅看看有没有顺路的货</text>
      <view class="btn-ghost goto" @click="goHall">去接单</view>
    </view>

    <view v-for="wb in ongoing" :key="wb.id" class="card job" @click="openWaybill(wb.id)">
      <view class="between">
        <text class="job-no mono">{{ wb.waybillNo }}</text>
        <status-tag :status="wb.status" />
      </view>
      <view class="route">
        <view class="route-line">
          <text class="dot dot-load" />
          <text class="addr">{{ wb.loadAddr }}</text>
        </view>
        <view class="route-line">
          <text class="dot dot-unload" />
          <text class="addr">{{ wb.unloadAddr }}</text>
        </view>
      </view>
      <view class="job-foot between">
        <text class="muted">{{ wb.cargoName }} · {{ wb.weight }} 吨</text>
        <text class="muted">计划装货 {{ shortTime(wb.planLoadTime) }}</text>
      </view>
    </view>

    <tab-bar current="home" :badges="{ waybill: todo.pendingLoad + todo.inTransit + todo.pendingSign, home: todo.unreadMessage }" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import * as api from '@/api/driver'
import { state, refreshProfile } from '@/store/auth'
import { shortTime } from '@/utils/format'

const todo = ref({})
const ongoing = computed(() => todo.value.ongoing || [])
const profile = computed(() => state.profile)
const online = computed(() => state.online)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

async function load() {
  await refreshProfile()
  try {
    todo.value = await api.todo()
  } catch (e) {
    // 401 已在 request 层统一处理
  }
}

onShow(load)
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function toggleOnline() {
  const next = !state.online
  try {
    await api.setOnline(next)
    state.online = next
    uni.showToast({ title: next ? '已开始接单' : '已收车，不再接收新单', icon: 'none' })
  } catch (e) {
    // request 层已提示
  }
}

function goList(scope) {
  uni.reLaunch({ url: '/pages/waybill/list?scope=' + scope })
}
function goHall() {
  uni.reLaunch({ url: '/pages/hall/hall' })
}
function goMessage() {
  uni.navigateTo({ url: '/pages/message/list' })
}
function openWaybill(id) {
  uni.navigateTo({ url: '/pages/waybill/detail?id=' + id })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 20rpx;
}

.header {
  background: linear-gradient(135deg, #1a4fdc, #2f6bff);
  padding: 40rpx 36rpx 56rpx;
  color: #ffffff;
}

.head-left {
  flex: 1;
  min-width: 0;
}

.hello {
  font-size: 40rpx;
  font-weight: 700;
  max-width: 420rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sub {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.82);
  margin-top: 10rpx;
}

.online {
  flex-shrink: 0;
  white-space: nowrap;
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.18);
  font-size: 26rpx;
}

.online.on {
  background: #ffffff;
  color: #1a4fdc;
  font-weight: 600;
}

.online-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #9fb3d9;
  margin-right: 10rpx;
}

.online.on .online-dot {
  background: #12b76a;
}

.stat-row {
  display: flex;
  background: #ffffff;
  margin: -34rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 28rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(23, 43, 77, 0.08);
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 42rpx;
  font-weight: 700;
  color: #1f2733;
}

.stat-label {
  font-size: 24rpx;
  color: #8a94a6;
  margin-top: 8rpx;
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
  line-height: 1.6;
}

.empty {
  text-align: center;
  padding: 48rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.goto {
  margin: 28rpx 60rpx 0;
}

.job-no {
  font-size: 26rpx;
  color: #48546b;
}

.route {
  margin: 20rpx 0 16rpx;
}

.route-line {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-right: 14rpx;
  flex-shrink: 0;
}

.dot-load {
  background: #2f6bff;
}

.dot-unload {
  background: #12b76a;
}

.addr {
  font-size: 30rpx;
  color: #1f2733;
  flex: 1;
}

.job-foot {
  border-top: 2rpx solid #eef2f8;
  padding-top: 16rpx;
}
</style>
