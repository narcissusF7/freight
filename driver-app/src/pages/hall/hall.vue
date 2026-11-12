<template>
  <view class="page">
    <view class="topbar">
      <view class="between">
        <view class="topbar-text">
          <text class="muted">按「载重 / 证照 / 距离 / 运价」四维打分排序</text>
        </view>
        <view class="locate" @click="reloadWithLocation">
          <text>{{ locating ? '定位中…' : locText }}</text>
        </view>
      </view>

      <view class="chips">
        <view
          v-for="c in filters"
          :key="c.key"
          class="chip"
          :class="{ on: filter === c.key }"
          @click="filter = c.key"
        >
          {{ c.label }}
        </view>
      </view>
    </view>

    <view v-if="loading" class="card tip">正在为你匹配货源…</view>

    <view v-else-if="!list.length" class="card empty">
      <text class="empty-title">暂时没有匹配的货源</text>
      <text class="muted">{{ locText === '未定位' ? '开启定位后才能按距离匹配' : '换个筛选条件或稍后再刷' }}</text>
      <view class="btn-ghost goto" @click="load">刷新</view>
    </view>

    <view v-for="g in shown" :key="g.goodsSourceId" class="card job">
      <view class="between">
        <view class="row">
          <text class="score">{{ g.score }}</text>
          <view class="score-box">
            <text class="job-name">{{ g.goodsName }}</text>
            <text class="muted">{{ g.weight }} 吨 / {{ g.volume || '-' }} 方</text>
          </view>
        </view>
        <view class="price">
          <text class="price-num">{{ money(g.freightPrice) }}</text>
          <text class="muted">{{ g.pricePerTon ? g.pricePerTon + ' 元/吨' : '未报价' }}</text>
        </view>
      </view>

      <view class="route">
        <view class="route-line">
          <text class="dot dot-load" />
          <text class="addr">{{ g.loadAddr }}</text>
        </view>
        <view class="route-line">
          <text class="dot dot-unload" />
          <text class="addr">{{ g.unloadAddr }}</text>
        </view>
      </view>

      <view class="meta">
        <text class="meta-item">装货 {{ shortTime(g.loadTime) }}</text>
        <text v-if="g.distanceToLoadKm !== null" class="meta-item">距装货地 {{ g.distanceToLoadKm }} km</text>
        <text class="meta-item">联系人 {{ g.contactName || '-' }} {{ g.contactPhone || '' }}</text>
      </view>

      <view v-if="g.reasons && g.reasons.length" class="reasons">
        <text v-for="(r, i) in g.reasons.slice(0, 3)" :key="i" class="reason">{{ r }}</text>
      </view>

      <view class="foot">
        <view v-if="!g.capacityFits" class="overload">核定载重不足，不可接单</view>
        <view
          v-if="g.capacityFits"
          class="btn-primary accept"
          :class="{ busy: accepting === g.goodsSourceId }"
          @click="doAccept(g)"
        >
          {{ accepting === g.goodsSourceId ? '接单中…' : '立即接单' }}
        </view>
      </view>
    </view>

    <tab-bar current="hall" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import * as api from '@/api/driver'
import { getLocation } from '@/utils/location'
import { state } from '@/store/auth'
import { money, shortTime } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const locating = ref(false)
const accepting = ref(0)
const filter = ref('all')
const loc = ref({ lat: null, lng: null })
const locText = ref('未定位')

const filters = [
  { key: 'all', label: '全部' },
  { key: 'near', label: '50km 内' },
  { key: 'price', label: '运价优先' }
]

const shown = computed(() => {
  let arr = list.value.slice()
  if (filter.value === 'near') {
    arr = arr.filter((g) => g.distanceToLoadKm !== null && g.distanceToLoadKm <= 50)
  }
  if (filter.value === 'price') {
    arr.sort((a, b) => (Number(b.pricePerTon) || 0) - (Number(a.pricePerTon) || 0))
  }
  return arr
})

async function load() {
  loading.value = true
  try {
    list.value = await api.hall(loc.value.lat, loc.value.lng, 30)
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function reloadWithLocation() {
  locating.value = true
  const l = await getLocation()
  locating.value = false
  if (l.ok) {
    loc.value = { lat: l.lat, lng: l.lng }
    locText.value = '已定位'
  } else {
    locText.value = '未定位'
    uni.showToast({ title: '未获取到定位，仅按打分排序', icon: 'none' })
  }
  await load()
}

onShow(async () => {
  if (loc.value.lat === null) {
    const l = await getLocation()
    if (l.ok) {
      loc.value = { lat: l.lat, lng: l.lng }
      locText.value = '已定位'
    }
  }
  await load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

function doAccept(g) {
  const plate = state.profile && state.profile.plateNo ? state.profile.plateNo : '默认车辆'
  uni.showModal({
    title: '确认接单',
    content: `${g.goodsName} ${g.weight} 吨\n${g.loadAddr} → ${g.unloadAddr}\n车辆：${plate}`,
    confirmText: '确认接单',
    success: async (res) => {
      if (!res.confirm) return
      accepting.value = g.goodsSourceId
      try {
        const detail = await api.accept(g.goodsSourceId, null)
        uni.showToast({ title: '接单成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/waybill/detail?id=' + detail.waybill.id })
        }, 500)
        await load()
      } catch (e) {
        // 被抢单等错误由 request 层提示
        await load()
      } finally {
        accepting.value = 0
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 20rpx;
}

.topbar {
  background: #ffffff;
  padding: 28rpx 24rpx 20rpx;
  border-bottom: 2rpx solid #eef2f8;
}

.topbar-text {
  flex: 1;
  min-width: 0;
  padding-right: 16rpx;
}

.locate {
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 24rpx;
  color: #2f6bff;
  border: 2rpx solid #d6e2ff;
  background: #f2f6ff;
  padding: 10rpx 20rpx;
  border-radius: 30rpx;
}

.chips {
  display: flex;
  margin-top: 22rpx;
}

.chip {
  font-size: 26rpx;
  color: #6b7787;
  padding: 10rpx 26rpx;
  border-radius: 30rpx;
  background: #f2f5fa;
  margin-right: 16rpx;
}

.chip.on {
  background: #2f6bff;
  color: #ffffff;
}

.tip {
  text-align: center;
  color: #8a94a6;
  font-size: 28rpx;
}

.empty {
  text-align: center;
  padding: 56rpx 28rpx;
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

.score {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #f2f6ff;
  color: #2f6bff;
  font-size: 30rpx;
  font-weight: 700;
  text-align: center;
  line-height: 76rpx;
  margin-right: 18rpx;
  flex-shrink: 0;
}

.score-box {
  display: flex;
  flex-direction: column;
}

.job-name {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.price-num {
  font-size: 34rpx;
  font-weight: 700;
  color: #d9363e;
}

.route {
  margin: 22rpx 0 16rpx;
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
  font-size: 28rpx;
  color: #1f2733;
  flex: 1;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  border-top: 2rpx solid #eef2f8;
  padding-top: 16rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #8a94a6;
  margin-right: 22rpx;
}

.reasons {
  margin-top: 14rpx;
  background: #f8faff;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
}

.reason {
  display: block;
  font-size: 24rpx;
  color: #5a6b85;
  line-height: 1.7;
}

.foot {
  margin-top: 22rpx;
}

.accept {
  height: 84rpx;
  line-height: 84rpx;
}

.accept.busy {
  opacity: 0.7;
}

.overload {
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  background: #fff1f0;
  color: #d9363e;
  border-radius: 16rpx;
  font-size: 28rpx;
}
</style>
