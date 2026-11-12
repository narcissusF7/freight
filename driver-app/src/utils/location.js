/**
 * 定位封装。
 *
 * 坐标口径：**全平台统一 GCJ-02**（腾讯地图/AI 定位口径）。后端 wb_waybill / tr_track_point
 * 存的都是 GCJ-02，任何 WGS-84 数据都必须在这里转换后入库，否则 50~500 米的系统性偏移
 * 会让「到厂 5 公里内才能确认装卸」这类校验失真。
 *
 * 为什么 H5 要自己转：uni.getLocation 在 H5 上传 type:'gcj02' 需要配置地图服务商 key，
 * 调试环境不想为一个坐标转换绑定 key，因此取原生 WGS-84 后本地转换；
 * APP 端原生支持 gcj02，直接取用（少一次转换误差）。
 *
 * 定位失败不抛给页面：司机在隧道/地下车库没信号是常态，
 * 页面据此提示「未获取定位」，而不是把整个流程卡死。
 */
export function getLocation() {
  return new Promise((resolve) => {
    // 兜底超时：部分机型/H5 实现里定位回调可能永远不来，
    // 不设超时会让「确认节点」按钮一直转圈，司机以为死机。
    let settled = false
    const once = (v) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolve(v)
    }
    const timer = setTimeout(() => once({ lat: null, lng: null, ok: false }), 8000)
    const fail = () => once({ lat: null, lng: null, ok: false })

    // #ifdef H5
    uni.getLocation({
      type: 'wgs84',
      success: (res) => {
        const g = wgs84ToGcj02(res.latitude, res.longitude)
        once({ lat: g.lat, lng: g.lng, ok: true })
      },
      fail
    })
    // #endif

    // #ifndef H5
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (res) => once({ lat: res.latitude, lng: res.longitude, ok: true }),
      fail
    })
    // #endif
  })
}

const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

function outOfChina(lat, lng) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

/** WGS-84 → GCJ-02（国测局偏移算法）。境外坐标原样返回。 */
export function wgs84ToGcj02(lat, lng) {
  if (outOfChina(lat, lng)) return { lat, lng }
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI)
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI)
  return { lat: lat + dLat, lng: lng + dLng }
}
