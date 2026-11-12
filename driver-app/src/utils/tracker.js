import { getLocation } from './location'
import { reportTracks } from '@/api/driver'

/**
 * 在途轨迹采集（APP 端）。
 *
 * 采集策略与后端 M3 的入库规则对齐：
 * - 30 秒一个点：满足监管「两客一危/普货 30s~60s」的常规要求，也刚好在 200km/h 跳点判定之下；
 * - 精度 > 100 米的点直接丢弃（地下车库/基站定位噪声，会污染里程）；
 * - 静止时（位移 < 100 米且速度 ≈ 0）不重复上报，省流量与分区存储；
 * - 一批 5 个点或 60 秒兜底上报一次，弱网时缓存最多 200 个点。
 *
 * 只在 APP 前台运行时采集；后台持续定位需要原生插件或厂商保活，
 * 上线前需按实际机型验证（大纲 14 节「司机端形态」待确认项）。
 */
const POINTS = []
let timer = null
let last = null
let currentWaybillId = null

export function startTracking(waybillId) {
  if (currentWaybillId === waybillId && timer) return
  currentWaybillId = waybillId
  stopTracking()
  last = null
  timer = setInterval(tick, 30000)
  tick()
}

export function stopTracking() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  currentWaybillId = null
}

async function tick() {
  if (!currentWaybillId) return
  const loc = await getLocation()
  if (!loc.ok) return
  push(currentWaybillId, loc.lat, loc.lng, null, 20)
}

/**
 * 手动打点：节点确认时立刻补一个点，保证轨迹与节点时间吻合。
 *
 * 传入页面已经取到的坐标，而不是再取一次——重复调用系统定位在部分 H5 实现里会挂起，
 * 而且多一次 GPS 读取就多一次耗电，司机端一天要确认十几次节点。
 */
export async function tickAt(waybillId, lat, lng, accuracy = 15) {
  if (lat === null || lng === null || lat === undefined) return
  push(waybillId, lat, lng, null, accuracy)
  await flush()
}

/** 单独取一次定位并打点（无可用坐标时用） */
export async function tickNow(waybillId, accuracy = 15) {
  const loc = await getLocation()
  if (!loc.ok) return null
  await tickAt(waybillId, loc.lat, loc.lng, accuracy)
  return loc
}

function push(waybillId, lat, lng, speed, accuracy) {
  if (lat === null || lng === null) return
  if (accuracy && accuracy > 100) return
  if (last) {
    const d = haversineM(last.lat, last.lng, lat, lng)
    if (d < 100 && !speed) return
  }
  last = { lat, lng }
  POINTS.push({
    waybillId,
    lat,
    lng,
    speed,
    accuracy,
    gpsTime: nowIso(),
    source: 'APP'
  })
  if (POINTS.length >= 5) flush()
}

let flushTimer = null
function scheduleFlush() {
  if (flushTimer) return
  flushTimer = setTimeout(() => {
    flushTimer = null
    flush()
  }, 60000)
}

export async function flush() {
  if (!POINTS.length) return
  const batch = POINTS.splice(0, Math.min(POINTS.length, 20))
  try {
    await reportTracks(batch)
  } catch (e) {
    // 失败回填到队首，下次再传；超出上限丢弃最旧的（避免离线久了内存爆掉）
    POINTS.unshift(...batch)
    while (POINTS.length > 200) POINTS.shift()
    scheduleFlush()
  }
}

export function pendingCount() {
  return POINTS.length
}

function nowIso() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function haversineM(lat1, lng1, lat2, lng2) {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
