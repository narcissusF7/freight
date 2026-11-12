<template>
  <div class="tp">
    <div class="tp-bar">
      <el-button size="small" :loading="loading" @click="reload">刷新轨迹</el-button>
      <el-button v-if="!points.length" size="small" type="primary" plain :loading="seeding" @click="seedDemo">
        生成演示轨迹
      </el-button>
      <span v-if="summary" class="tp-meta">
        点数 {{ summary.pointCount }}
        <span class="dot">·</span> 里程 {{ summary.distanceKm }} km
        <span class="dot">·</span> 均速 {{ summary.avgSpeed }} km/h
        <span class="dot">·</span> 最高 {{ summary.maxSpeed }} km/h
        <span v-if="summary.driftPoints" class="warn">（剔除漂移 {{ summary.driftPoints }} 点）</span>
      </span>
      <span v-else-if="!loading && !points.length" class="tp-meta muted">暂无轨迹点</span>
    </div>

    <el-alert
      v-if="error"
      type="error"
      :closable="false"
      show-icon
      :title="error"
      style="margin-bottom: 8px"
    />

    <div ref="mapEl" class="tp-map"></div>

    <div class="tp-ctrl">
      <el-button size="small" type="primary" :disabled="!points.length" @click="toggle">
        {{ playing ? '暂停' : '播放' }}
      </el-button>
      <el-button size="small" :disabled="!points.length" @click="reset">复位</el-button>
      <el-radio-group v-model="rate" size="small" :disabled="!points.length">
        <el-radio-button :value="1">1x</el-radio-button>
        <el-radio-button :value="2">2x</el-radio-button>
        <el-radio-button :value="4">4x</el-radio-button>
        <el-radio-button :value="8">8x</el-radio-button>
      </el-radio-group>
      <el-slider
        v-model="progress"
        :max="100"
        :step="0.1"
        :disabled="!points.length"
        :show-tooltip="false"
        class="tp-slider"
        @input="onSeek"
      />
      <span class="tp-clock mono">{{ clockText }}</span>
      <span class="tp-speed mono">{{ currentSpeed }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { loadTMap, DEFAULT_CENTER } from '@/utils/tmap'
import { listTrack, trackSummary, reportTrackBatch } from '@/api/track'

const props = defineProps({
  /** 只有标签页可见时才初始化地图，避免容器 display:none 导致地图尺寸为 0 */
  active: { type: Boolean, default: false },
  waybillId: { type: [Number, String], default: null },
  loadLat: { type: [Number, String], default: null },
  loadLng: { type: [Number, String], default: null },
  unloadLat: { type: [Number, String], default: null },
  unloadLng: { type: [Number, String], default: null }
})

const mapEl = ref(null)
const points = ref([])
const summary = ref(null)
const loading = ref(false)
const seeding = ref(false)
const error = ref('')
const playing = ref(false)
const rate = ref(1)
const progress = ref(0)
const virtualMs = ref(0)
const currentSpeed = ref('')

let TMapRef = null
let map = null
let polyline = null
let markers = null
let rafId = null
let lastFrameTs = 0

const totalMs = computed(() => {
  if (points.value.length < 2) return 0
  return new Date(points.value[points.value.length - 1].gpsTime).getTime()
    - new Date(points.value[0].gpsTime).getTime()
})

const clockText = computed(() => {
  if (!points.value.length) return '--:--:--'
  const t = new Date(new Date(points.value[0].gpsTime).getTime() + virtualMs.value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}`
})

function destroyMap() {
  stop()
  if (map) {
    map.destroy()
    map = null
    polyline = null
    markers = null
  }
}

async function initMap() {
  if (map || !mapEl.value) return
  error.value = ''
  try {
    TMapRef = await loadTMap()
    map = new TMapRef.Map(mapEl.value, {
      zoom: 8,
      center: new TMapRef.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng)
    })
    polyline = new TMapRef.MultiPolyline({
      map,
      styles: {
        trail: new TMapRef.PolylineStyle({
          color: '#409EFF', width: 6, borderWidth: 2, borderColor: '#ffffff', lineCap: 'round'
        })
      },
      geometries: []
    })
    markers = new TMapRef.MultiMarker({
      map,
      styles: {
        start: new TMapRef.MarkerStyle({ width: 26, height: 34, src: pinSvg('#67C23A') }),
        end: new TMapRef.MarkerStyle({ width: 26, height: 34, src: pinSvg('#F56C6C') }),
        car: new TMapRef.MarkerStyle({ width: 34, height: 34, src: carSvg() })
      },
      geometries: []
    })
    if (points.value.length) draw()
  } catch (e) {
    error.value = e.message || '地图加载失败'
  }
}

function pinSvg(color) {
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="26" height="34" viewBox="0 0 26 34">
      <path d="M13 0C5.8 0 0 5.8 0 13c0 9.1 13 21 13 21s13-11.9 13-21C26 5.8 20.2 0 13 0z" fill="${color}"/>
      <circle cx="13" cy="12" r="5" fill="#fff"/></svg>`)
}

function carSvg() {
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 34 34">
      <circle cx="17" cy="17" r="15" fill="#409EFF" fill-opacity="0.18"/>
      <circle cx="17" cy="17" r="9" fill="#409EFF"/>
      <circle cx="17" cy="17" r="4" fill="#fff"/></svg>`)
}

/** 画轨迹：折线 + 起终点 + 当前位置 */
function draw() {
  if (!map || !points.value.length) return
  const path = points.value.map((p) => new TMapRef.LatLng(Number(p.lat), Number(p.lng)))
  polyline.setGeometries([{ id: 'trail', styleId: 'trail', paths: path }])

  const geo = [
    { id: 'start', styleId: 'start', position: path[0] },
    { id: 'end', styleId: 'end', position: path[path.length - 1] }
  ]
  const current = positionAt(virtualMs.value)
  if (current) {
    geo.push({ id: 'car', styleId: 'car', position: new TMapRef.LatLng(current.lat, current.lng) })
  }
  markers.setGeometries(geo)

  const lats = path.map((p) => p.lat)
  const lngs = path.map((p) => p.lng)
  const bounds = new TMapRef.LatLngBounds(
    new TMapRef.LatLng(Math.min(...lats), Math.min(...lngs)),
    new TMapRef.LatLng(Math.max(...lats), Math.max(...lngs))
  )
  map.fitBounds(bounds, { padding: 60 })
}

/** 按虚拟时间在两点间线性插值（回放不追求拟合精度，平滑即可） */
function positionAt(ms) {
  const pts = points.value
  if (!pts.length) return null
  if (pts.length === 1 || ms <= 0) {
    return { lat: Number(pts[0].lat), lng: Number(pts[0].lng), speed: pts[0].speed }
  }
  const base = new Date(pts[0].gpsTime).getTime()
  const target = base + ms
  for (let i = 1; i < pts.length; i++) {
    const prevT = new Date(pts[i - 1].gpsTime).getTime()
    const curT = new Date(pts[i].gpsTime).getTime()
    if (target <= curT) {
      const span = curT - prevT || 1
      const r = Math.min(1, Math.max(0, (target - prevT) / span))
      const lat = Number(pts[i - 1].lat) + (Number(pts[i].lat) - Number(pts[i - 1].lat)) * r
      const lng = Number(pts[i - 1].lng) + (Number(pts[i].lng) - Number(pts[i - 1].lng)) * r
      return { lat, lng, speed: pts[i].speed }
    }
  }
  const last = pts[pts.length - 1]
  return { lat: Number(last.lat), lng: Number(last.lng), speed: last.speed }
}

function updateMoving() {
  if (!markers || !map) return
  const cur = positionAt(virtualMs.value)
  if (!cur) return
  markers.updateGeometries([{ id: 'car', styleId: 'car', position: new TMapRef.LatLng(cur.lat, cur.lng) }])
  currentSpeed.value = cur.speed !== null && cur.speed !== undefined ? `${Number(cur.speed).toFixed(0)} km/h` : ''
  progress.value = totalMs.value > 0 ? Math.min(100, (virtualMs.value / totalMs.value) * 100) : 0
}

function tick(ts) {
  if (!playing.value) return
  if (!lastFrameTs) lastFrameTs = ts
  const delta = ts - lastFrameTs
  lastFrameTs = ts
  // 演示用时间压缩：1 倍速 = 每秒推进 60 秒虚拟时间（否则 8 小时轨迹要看 8 小时）
  virtualMs.value = Math.min(totalMs.value, virtualMs.value + delta * rate.value * 60)
  updateMoving()
  if (virtualMs.value >= totalMs.value) {
    stop()
    return
  }
  rafId = requestAnimationFrame(tick)
}

function toggle() {
  if (playing.value) {
    stop()
    return
  }
  if (virtualMs.value >= totalMs.value) virtualMs.value = 0
  playing.value = true
  lastFrameTs = 0
  rafId = requestAnimationFrame(tick)
}

function stop() {
  playing.value = false
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
}

function reset() {
  stop()
  virtualMs.value = 0
  progress.value = 0
  updateMoving()
}

function onSeek(v) {
  virtualMs.value = (v / 100) * totalMs.value
  updateMoving()
}

async function reload() {
  if (!props.waybillId) return
  loading.value = true
  error.value = ''
  try {
    const [pts, sum] = await Promise.all([
      listTrack(props.waybillId, null, null, 2000),
      trackSummary(props.waybillId, null, null)
    ])
    points.value = pts || []
    summary.value = sum
    reset()
    if (map) draw()
    else await nextTick(initMap)
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '轨迹加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 生成演示轨迹：沿装货地→卸货地直线插值 + 轻微抖动，source 标记 DEMO 便于清理。
 * 真实环境由司机 APP 按 30s/200m 上报，这里只为让回放可见。
 */
async function seedDemo() {
  if (!props.waybillId) return
  seeding.value = true
  try {
    const from = {
      lat: props.loadLat ? Number(props.loadLat) : 24.4798,
      lng: props.loadLng ? Number(props.loadLng) : 118.0894
    }
    const to = {
      lat: props.unloadLat ? Number(props.unloadLat) : 24.8741,
      lng: props.unloadLng ? Number(props.unloadLng) : 118.6757
    }
    // 按目标车速反推总时长，再以 30s 节拍布点（与真实采集口径一致）；点数上限 800 便于演示
    const distKm = haversineKm(from.lat, from.lng, to.lat, to.lng)
    const durationSec = Math.max(600, (distKm / 70) * 3600)
    const total = Math.min(800, Math.max(60, Math.round(durationSec / 30)))
    const stepMs = (durationSec * 1000) / (total - 1)
    const startTs = Date.now() - durationSec * 1000
    const pts = []
    for (let i = 0; i < total; i++) {
      const r = i / (total - 1)
      const lat = from.lat + (to.lat - from.lat) * r + (Math.sin(i / 6) * 0.004)
      const lng = from.lng + (to.lng - from.lng) * r + (Math.cos(i / 7) * 0.004)
      pts.push({
        waybillId: Number(props.waybillId),
        lat: Number(lat.toFixed(6)),
        lng: Number(lng.toFixed(6)),
        speed: i === 0 ? 0 : 70,
        direction: 0,
        accuracy: 12,
        gpsTime: fmtLocal(startTs + i * stepMs),
        source: 'DEMO'
      })
    }
    await reportTrackBatch(pts)
    ElMessage.success(`已写入 ${pts.length} 个演示轨迹点（source=DEMO）`)
    await reload()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '生成演示轨迹失败')
  } finally {
    seeding.value = false
  }
}

function fmtLocal(ms) {
  const d = new Date(ms)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 球面距离（km），与服务端里程算法同源，用于反推演示采样间隔 */
function haversineKm(lat1, lng1, lat2, lng2) {
  const r = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)))
}

watch(
  () => props.active,
  async (v) => {
    if (v) {
      await nextTick()
      await initMap()
      await reload()
    } else {
      destroyMap()
    }
  },
  { immediate: true }
)

onBeforeUnmount(destroyMap)
</script>

<style scoped>
.tp-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.tp-meta { font-size: 12px; color: #606266; }
.tp-meta .dot { color: #c0c4cc; margin: 0 2px; }
.tp-map { width: 100%; height: 380px; border-radius: 4px; background: #f2f3f5; }
.tp-ctrl { display: flex; align-items: center; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.tp-slider { flex: 1; min-width: 160px; }
.tp-clock, .tp-speed { font-size: 12px; color: #606266; min-width: 62px; }
.mono { font-family: Consolas, Monaco, monospace; }
.muted { color: #909399; }
.warn { color: #e6a23c; }
</style>
