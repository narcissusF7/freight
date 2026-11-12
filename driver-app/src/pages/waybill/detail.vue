<template>
  <view class="page" v-if="detail">
    <!-- 状态头 -->
    <view class="head">
      <view class="between">
        <text class="wb-no mono">{{ wb.waybillNo }}</text>
        <status-tag :status="wb.status" />
      </view>
      <view class="head-line">
        <text class="head-label">{{ detail.statusText }}</text>
        <text v-if="detail.plateNo" class="head-plate">{{ detail.plateNo }}</text>
      </view>
      <view v-if="detail.tip" class="head-tip">{{ detail.tip }}</view>
    </view>

    <!-- 路线：APP 用原生地图，H5 用示意图（H5 仅调试用，正式载体是 APP） -->
    <view class="card map-card">
      <view class="between">
        <text class="card-title">运输路线</text>
        <text class="link" @click="navigateTo('unload')">导航到卸货地</text>
      </view>

      <!-- #ifdef APP-PLUS -->
      <map
        class="map"
        :latitude="mapCenter.lat"
        :longitude="mapCenter.lng"
        :markers="mapMarkers"
        :polyline="mapLine"
        :scale="8"
        show-location
      />
      <!-- #endif -->

      <!-- #ifndef APP-PLUS -->
      <view class="strip">
        <view class="strip-track">
          <view class="strip-node node-load" />
          <view class="strip-line" />
          <view class="strip-node node-unload" />
        </view>
        <view class="strip-labels">
          <view class="strip-side">
            <text class="strip-title">装货地</text>
            <text class="strip-addr">{{ wb.loadAddr }}</text>
            <text v-if="detail.distanceToLoadM !== null" class="strip-dist">
              距我 {{ distanceText(detail.distanceToLoadM) }}
            </text>
          </view>
          <view class="strip-side right">
            <text class="strip-title">卸货地</text>
            <text class="strip-addr">{{ wb.unloadAddr }}</text>
            <text v-if="detail.distanceToUnloadM !== null" class="strip-dist">
              距我 {{ distanceText(detail.distanceToUnloadM) }}
            </text>
          </view>
        </view>
      </view>
      <!-- #endif -->

      <view class="map-actions">
        <view class="btn-plain mini" @click="navigateTo('load')">导航到装货地</view>
        <view class="btn-plain mini" @click="navigateTo('unload')">导航到卸货地</view>
      </view>
    </view>

    <!-- 货物与联系信息 -->
    <view class="card">
      <text class="card-title">货物信息</text>
      <view class="kv"><text class="k">货物</text><text class="v">{{ wb.cargoName }}</text></view>
      <view class="kv"><text class="k">重量</text><text class="v">{{ wb.weight }} 吨</text></view>
      <view class="kv">
        <text class="k">计划装货</text><text class="v">{{ fullTime(wb.planLoadTime) }}</text>
      </view>
      <view class="kv">
        <text class="k">现场联系人</text>
        <view class="v row">
          <text>{{ detail.contactName || '-' }}</text>
          <text v-if="detail.contactPhone" class="phone" @click="call(detail.contactPhone)">
            {{ detail.contactPhone }}
          </text>
        </view>
      </view>
    </view>

    <!-- 节点时间轴 -->
    <view class="card">
      <text class="card-title">节点记录（{{ detail.nodes.length }}）</text>
      <view v-for="(n, i) in detail.nodes" :key="n.id" class="node">
        <view class="node-axis">
          <view class="node-dot" :class="{ first: i === 0 }" />
          <view v-if="i < detail.nodes.length - 1" class="node-line" />
        </view>
        <view class="node-body">
          <view class="between">
            <text class="node-title">{{ n.remark || n.toStatus }}</text>
            <text class="muted">{{ shortTime(n.operateTime) }}</text>
          </view>
          <text v-if="n.address" class="node-sub">{{ n.address }}</text>
          <view v-if="n.distanceM !== null && n.distanceM !== undefined" class="node-sub">
            距参考点 {{ distanceText(n.distanceM) }}
          </view>
          <view v-if="n.evidenceUrls" class="photos">
            <image
              v-for="(u, k) in n.evidenceUrls.split(',')"
              :key="k"
              class="photo"
              :src="u"
              mode="aspectFill"
              @click="preview(n.evidenceUrls.split(','), k)"
            />
          </view>
          <text v-if="n.source === 'DRIVER_APP'" class="src">司机端确认</text>
        </view>
      </view>
    </view>

    <!-- 回单 -->
    <view v-if="detail.receipts && detail.receipts.length" class="card">
      <text class="card-title">回单（{{ detail.receipts.length }}）</text>
      <view v-for="r in detail.receipts" :key="r.id" class="row receipt">
        <image class="photo big" :src="r.fileUrl" mode="aspectFill" @click="preview([r.fileUrl], 0)" />
        <view class="receipt-info">
          <text class="strong">已上传</text>
          <text class="muted">{{ shortTime(r.uploadTime) }}</text>
          <text class="muted">{{ r.verified === 1 ? '已核验' : '待核验' }}</text>
        </view>
      </view>
    </view>

    <!-- 评价货主：签收后才能评，一张运单只能评一次 -->
    <view v-if="canRate" class="card">
      <view class="between">
        <text class="card-title">评价货主</text>
        <text v-if="rating" class="muted">{{ rating.score }} 星</text>
      </view>

      <template v-if="rating">
        <view class="star-row">
          <text v-for="n in 5" :key="n" class="star" :class="{ on: n <= rating.score }">★</text>
        </view>
        <text class="muted">{{ rating.content || '未填写文字评价' }}</text>
      </template>

      <template v-else>
        <view class="star-row">
          <text
            v-for="n in 5"
            :key="n"
            class="star"
            :class="{ on: n <= ratingForm.score }"
            @click="ratingForm.score = n"
          >★</text>
          <text class="muted rate-tip">{{ scoreText }}</text>
        </view>
        <textarea
          v-model="ratingForm.content"
          class="rate-input"
          placeholder="装卸效率、结算及时性、沟通配合（选填）"
          maxlength="200"
        />
        <view class="btn-primary" @click="doRate">提交评价</view>
        <text class="muted rate-hint">评价提交后不可修改；差评会进入平台客服复核</text>
      </template>
    </view>

    <!-- 底部操作 -->
    <view class="action-bar">
      <view class="action-row" v-if="showReceiptBtn">
        <view class="btn-ghost flex1" @click="pickReceipt">上传回单</view>
      </view>

      <view v-if="detail.nextAction" class="action-main">
        <view class="hint" v-if="actionHint">{{ actionHint }}</view>
        <view class="btn-primary" :class="{ busy: submitting }" @click="startAction(nextAction)">
          {{ submitting ? '提交中…' : '确认「' + nextAction.label + '」' }}
        </view>
      </view>
      <view v-else class="action-main">
        <view class="btn-plain">当前状态无可确认节点</view>
      </view>

      <view class="action-sub">
        <text class="sub-link" @click="pickPhotos('EXCEPTION')">上报异常</text>
        <text class="sub-link" @click="openTrackInfo">轨迹状态</text>
      </view>
    </view>

    <view class="bottom-holder" />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import StatusTag from '@/components/StatusTag.vue'
import * as api from '@/api/driver'
import { getLocation } from '@/utils/location'
import { startTracking, stopTracking, tickAt, pendingCount } from '@/utils/tracker'
import { distanceText, fullTime, shortTime } from '@/utils/format'

const id = ref(0)
const detail = ref(null)
const submitting = ref(false)
const wb = computed(() => (detail.value ? detail.value.waybill : {}))
const nextAction = computed(() => {
  if (!detail.value || !detail.value.nextAction) return null
  const a = detail.value.availableActions.find((x) => x.action === detail.value.nextAction)
  return a || { action: detail.value.nextAction, label: detail.value.nextActionLabel, locationRequired: true }
})
const showReceiptBtn = computed(() => wb.value.status === 'SIGNED')

/** 可评价状态：签收及之后；已评价则显示结果而不是评分控件 */
const RATABLE = ['SIGNED', 'RECEIPT_UPLOADED', 'SETTLED', 'REPORTED', 'COMPLETED']
const rating = ref(null)
const ratingForm = reactive({ score: 5, content: '' })
const canRate = computed(() => RATABLE.includes(wb.value.status))
const scoreText = computed(() => ['', '很差', '较差', '一般', '满意', '很满意'][ratingForm.score] || '')

async function loadRating() {
  if (!canRate.value) return
  try {
    rating.value = await api.myRating(id.value)
  } catch (e) {
    rating.value = null
  }
}

async function doRate() {
  if (submitting.value) return
  submitting.value = true
  try {
    rating.value = await api.submitRating({
      waybillId: id.value,
      score: ratingForm.score,
      content: ratingForm.content
    })
    uni.showToast({ title: '评价已提交', icon: 'success' })
  } catch (e) {
    // 请求层已提示（例如重复评价 B0001），这里不再弹第二次
  } finally {
    submitting.value = false
  }
}
const actionHint = computed(() => {
  if (!nextAction.value) return ''
  if (needPhotos(nextAction.value.action)) return '需要定位 + 现场照片，照片作为凭证留存'
  if (nextAction.value.locationRequired) return '需要定位，请确认人已到现场'
  return ''
})

const mapCenter = reactive({ lat: 31.2, lng: 120.9 })
const mapMarkers = ref([])
const mapLine = ref([])

onLoad(async (options) => {
  id.value = Number(options.id || 0)
  await load()
})

onShow(() => {
  if (id.value) load()
})

onUnload(() => {
  stopTracking()
})

async function load() {
  const loc = await getLocation()
  try {
    detail.value = await api.waybillDetail(id.value, loc.lat, loc.lng)
    syncMap()
    // 在途状态自动开始采集轨迹，签收后停止（签收后不再产生运输轨迹）
    if (['ACCEPTED', 'LOADED', 'IN_TRANSIT'].includes(wb.value.status)) {
      startTracking(id.value)
    } else {
      stopTracking()
    }
    // 评价是次要信息：失败不影响运单详情展示，静默降级为「未评价」
    await loadRating()
  } catch (e) {
    // 401/403 由 request 层处理
  }
}

function syncMap() {
  const m = []
  if (wb.value.loadLat) {
    m.push({
      id: 1,
      latitude: Number(wb.value.loadLat),
      longitude: Number(wb.value.loadLng),
      title: '装货地',
      callout: { content: '装货：' + (wb.value.loadAddr || ''), display: 'ALWAYS', padding: 6 }
    })
  }
  if (wb.value.unloadLat) {
    m.push({
      id: 2,
      latitude: Number(wb.value.unloadLat),
      longitude: Number(wb.value.unloadLng),
      title: '卸货地',
      callout: { content: '卸货：' + (wb.value.unloadAddr || ''), display: 'ALWAYS', padding: 6 }
    })
  }
  mapMarkers.value = m
  mapLine.value = m.length === 2
    ? [{ points: m.map((x) => ({ latitude: x.latitude, longitude: x.longitude })), color: '#2f6bff', width: 6 }]
    : []
  if (m.length) {
    mapCenter.lat = (m[0].latitude + (m[1] ? m[1].latitude : m[0].latitude)) / 2
    mapCenter.lng = (m[0].longitude + (m[1] ? m[1].longitude : m[0].longitude)) / 2
  }
}

/** 需要现场照片的节点：装货完成 / 卸货完成 / 签收 */
function needPhotos(action) {
  return ['LOADED', 'UNLOADED', 'SIGNED'].includes(action)
}

async function startAction(action) {
  const photos = []
  if (needPhotos(action.action)) {
    uni.showModal({
      title: action.label,
      content: '需要拍摄现场照片作为凭证，是否现在拍摄？',
      confirmText: '拍照',
      cancelText: '取消',
      success: async (res) => {
        if (!res.confirm) return
        const urls = await chooseImages(3)
        if (!urls.length) {
          uni.showToast({ title: '未选择照片，无法确认', icon: 'none' })
          return
        }
        photos.push(...urls)
        await submitAction(action, photos)
      }
    })
    return
  }
  await submitAction(action, photos)
}

async function submitAction(action, photoUrls) {
  submitting.value = true
  try {
    const loc = await getLocation()
    if (!loc.ok && action.locationRequired) {
      uni.showToast({ title: '未获取到定位，请开启定位权限后重试', icon: 'none', duration: 2500 })
      return
    }
    if (loc.ok) await tickAt(id.value, loc.lat, loc.lng)
    const res = await api.confirmNode(id.value, {
      action: action.action,
      lat: loc.lat,
      lng: loc.lng,
      photoUrls,
      remark: ''
    })
    detail.value = res
    syncMap()
    uni.showToast({ title: action.label + '成功', icon: 'success' })
    if (!['ACCEPTED', 'LOADED', 'IN_TRANSIT'].includes(wb.value.status)) stopTracking()
  } catch (e) {
    // request 层已提示（距离超限、状态已变更等）
  } finally {
    submitting.value = false
  }
}

function chooseImages(count) {
  return new Promise((resolve) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => resolve(res.tempFilePaths || []),
      fail: () => resolve([])
    })
  })
}

function pickPhotos() {
  uni.showActionSheet({
    itemList: ['车辆故障', '货损/货差', '装卸延误', '交通事故', '其他异常'],
    success: async (res) => {
      const types = ['BREAKDOWN', 'CARGO_DAMAGE', 'LOAD_DELAY', 'ACCIDENT', 'OTHER']
      const exceptionType = types[res.tapIndex]
      const urls = await chooseImages(3)
      try {
        await api.reportException(id.value, {
          exceptionType,
          description: '司机端上报：' + exceptionType,
          imageUrls: urls.join(',')
        })
        uni.showToast({ title: '已上报，调度会联系你', icon: 'none' })
        await load()
      } catch (e) {
        // 已提示
      }
    }
  })
}

async function pickReceipt() {
  const urls = await chooseImages(1)
  if (!urls.length) return
  uni.showLoading({ title: '上传中…' })
  try {
    await api.uploadReceipt(id.value, urls[0], '司机端上传')
    uni.hideLoading()
    uni.showToast({ title: '回单已上传', icon: 'success' })
    await load()
  } catch (e) {
    uni.hideLoading()
  }
}

function navigateTo(side) {
  const lat = side === 'load' ? wb.value.loadLat : wb.value.unloadLat
  const lng = side === 'load' ? wb.value.loadLng : wb.value.unloadLng
  const addr = side === 'load' ? wb.value.loadAddr : wb.value.unloadAddr
  if (!lat || !lng) {
    uni.showToast({ title: '该地点未设置坐标，无法导航', icon: 'none' })
    return
  }
  uni.openLocation({
    latitude: Number(lat),
    longitude: Number(lng),
    name: side === 'load' ? '装货地' : '卸货地',
    address: addr || '',
    scale: 15
  })
}

function call(phone) {
  uni.makePhoneCall({ phoneNumber: phone })
}

function preview(urls, index) {
  uni.previewImage({ urls, current: index })
}

function openTrackInfo() {
  uni.showModal({
    title: '轨迹采集',
    content: `每 30 秒采集一次位置，位移小于 100 米不重复上报。\n待上传点数：${pendingCount()}`,
    showCancel: false
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.head {
  background: linear-gradient(135deg, #1a4fdc, #2f6bff);
  color: #ffffff;
  padding: 32rpx 36rpx 48rpx;
}

.wb-no {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.86);
}

.head-line {
  display: flex;
  align-items: baseline;
  margin-top: 14rpx;
}

.head-label {
  font-size: 44rpx;
  font-weight: 700;
}

.head-plate {
  margin-left: 18rpx;
  font-size: 26rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.head-tip {
  margin-top: 18rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.map-card {
  margin-top: -30rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
}

.link {
  font-size: 26rpx;
  color: #2f6bff;
}

.map {
  width: 100%;
  height: 360rpx;
  border-radius: 16rpx;
  margin-top: 20rpx;
}

.strip {
  margin: 24rpx 0 8rpx;
  background: #f7f9fe;
  border-radius: 16rpx;
  padding: 28rpx 24rpx 20rpx;
}

.strip-track {
  display: flex;
  align-items: center;
}

.strip-node {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  border: 4rpx solid #ffffff;
  box-shadow: 0 0 0 2rpx #c9d6ee;
}

.node-load {
  background: #2f6bff;
}

.node-unload {
  background: #12b76a;
}

.strip-line {
  flex: 1;
  height: 4rpx;
  background: repeating-linear-gradient(90deg, #2f6bff 0 16rpx, transparent 16rpx 28rpx);
  margin: 0 8rpx;
}

.strip-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 18rpx;
}

.strip-side {
  display: flex;
  flex-direction: column;
  max-width: 46%;
}

.strip-side.right {
  align-items: flex-end;
  text-align: right;
}

.strip-title {
  font-size: 24rpx;
  color: #8a94a6;
}

.strip-addr {
  font-size: 28rpx;
  margin-top: 6rpx;
  line-height: 1.5;
}

.strip-dist {
  font-size: 24rpx;
  color: #2f6bff;
  margin-top: 6rpx;
}

.map-actions {
  display: flex;
  margin-top: 18rpx;
}

.mini {
  flex: 1;
  height: 76rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  margin-right: 16rpx;
}

.mini:last-child {
  margin-right: 0;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
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
  text-align: right;
  max-width: 68%;
}

.phone {
  color: #2f6bff;
  margin-left: 14rpx;
}

.node {
  display: flex;
  margin-top: 22rpx;
}

.node-axis {
  width: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.node-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #c8d2e4;
  margin-top: 12rpx;
}

.node-dot.first {
  background: #2f6bff;
}

.node-line {
  flex: 1;
  width: 2rpx;
  background: #e6ecf5;
  margin-top: 6rpx;
}

.node-body {
  flex: 1;
  padding-bottom: 18rpx;
}

.node-title {
  font-size: 28rpx;
  font-weight: 500;
}

.node-sub {
  display: block;
  font-size: 24rpx;
  color: #8a94a6;
  margin-top: 6rpx;
}

.src {
  display: inline-block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #2f6bff;
  background: #f2f6ff;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.photos {
  display: flex;
  margin-top: 12rpx;
}

.photo {
  width: 120rpx;
  height: 120rpx;
  border-radius: 10rpx;
  margin-right: 12rpx;
  background: #eef2f8;
}

.photo.big {
  width: 160rpx;
  height: 160rpx;
}

.receipt {
  margin-top: 16rpx;
}

.receipt-info {
  display: flex;
  flex-direction: column;
  margin-left: 20rpx;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  border-top: 2rpx solid #eef2f8;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -8rpx 24rpx rgba(23, 43, 77, 0.06);
}

.action-row {
  display: flex;
  margin-bottom: 16rpx;
}

.flex1 {
  flex: 1;
}

.star-row {
  display: flex;
  align-items: center;
  margin: 12rpx 0;
}

.star {
  font-size: 46rpx;
  color: #dfe4ee;
  margin-right: 10rpx;
}

.star.on {
  color: #f5a623;
}

.rate-tip {
  margin-left: 10rpx;
}

.rate-input {
  width: 100%;
  min-height: 120rpx;
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  margin-bottom: 16rpx;
}

.rate-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
}

.action-main .hint {
  font-size: 22rpx;
  color: #8a94a6;
  margin-bottom: 10rpx;
  text-align: center;
}

.busy {
  opacity: 0.7;
}

.action-sub {
  display: flex;
  justify-content: center;
  margin-top: 14rpx;
}

.sub-link {
  font-size: 24rpx;
  color: #6b7787;
  margin: 0 24rpx;
}

.bottom-holder {
  height: 300rpx;
}
</style>
