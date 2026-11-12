<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ on: scope === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="!list.length" class="card empty">
      <text class="empty-title">{{ emptyText }}</text>
      <view v-if="scope !== 'DONE'" class="btn-ghost goto" @click="goHall">去货源大厅</view>
    </view>

    <view v-for="wb in list" :key="wb.id" class="card job" @click="open(wb.id)">
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

      <view class="between foot">
        <text class="muted">{{ wb.cargoName }} · {{ wb.weight }} 吨</text>
        <text class="muted">{{ shortTime(wb.planLoadTime) }} 装货</text>
      </view>
    </view>

    <tab-bar current="waybill" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import * as api from '@/api/driver'
import { shortTime } from '@/utils/format'

const tabs = [
  { key: 'PENDING_LOAD', label: '待装货' },
  { key: 'IN_TRANSIT', label: '运输中' },
  { key: 'PENDING_SIGN', label: '待签收' },
  { key: 'DONE', label: '已完成' }
]

const scope = ref('PENDING_LOAD')
const list = ref([])

const emptyText = computed(() =>
  scope.value === 'DONE' ? '还没有已完成的运单' : '当前没有这类运单'
)

onLoad((options) => {
  if (options && options.scope) scope.value = options.scope
})

onShow(load)

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  try {
    list.value = await api.myWaybills(scope.value)
  } catch (e) {
    list.value = []
  }
}

function switchTab(key) {
  scope.value = key
  load()
}

function open(id) {
  uni.navigateTo({ url: '/pages/waybill/detail?id=' + id })
}

function goHall() {
  uni.reLaunch({ url: '/pages/hall/hall' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 20rpx;
}

.tabs {
  display: flex;
  background: #ffffff;
  border-bottom: 2rpx solid #eef2f8;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  font-size: 28rpx;
  color: #6b7787;
  position: relative;
}

.tab.on {
  color: #2f6bff;
  font-weight: 600;
}

.tab.on::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 56rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: #2f6bff;
}

.empty {
  text-align: center;
  padding: 60rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.goto {
  margin: 30rpx 60rpx 0;
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
  flex: 1;
}

.foot {
  border-top: 2rpx solid #eef2f8;
  padding-top: 16rpx;
}
</style>
