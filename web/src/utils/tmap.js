/**
 * 腾讯地图 GL JS 加载器（合规约束，勿随意改动）
 *
 * 合规要点：
 * 1. 只使用官方 CDN + 官方 SDK，不引入 Google/Apple/Bing 海外版/OSM/Mapbox 等未获批地图源；
 * 2. **不在前端出现任何可用 key**：默认走 WorkBuddy 的 key 代理模式（_TMapSecurityConfig），
 *    SDK URL 不带 key 参数；__WB_HTTP_PORT__ / __WB_TMAP_SECRET__ 是运行时占位符，由宿主替换；
 * 3. 若需在独立环境（非 WorkBuddy 预览）运行，请自行申请腾讯位置服务 key，写入 .env.local 的
 *    VITE_TMAP_KEY（不要提交到仓库，也不要把 key 硬编码进代码）；
 * 4. 坐标系为 GCJ-02（火星坐标），后端存的就是该坐标，不要再做 WGS84 转换。
 */
const SDK_URL = 'https://map.qq.com/api/gljs?v=1.exp&libraries=service'

/** 默认场景的代理地址：key 由宿主后端持有，前端只拿到代理入口 */
const PROXY_SERVICE_HOST = 'http://127.0.0.1:__WB_HTTP_PORT__/_TMapService/_wbt/__WB_TMAP_SECRET__'

/** 默认地图中心：厦门（平台所在地） */
export const DEFAULT_CENTER = { lat: 24.4798, lng: 118.0894 }

let loading = null

export function loadTMap() {
  if (window.TMap) return Promise.resolve(window.TMap)
  if (loading) return loading

  loading = new Promise((resolve, reject) => {
    const userKey = import.meta.env.VITE_TMAP_KEY
    // 代理配置必须在 SDK <script> 之前写入；两者不能同时用（会白屏）
    if (!window._TMapSecurityConfig && !userKey) {
      window._TMapSecurityConfig = { serviceHost: PROXY_SERVICE_HOST }
    }
    const script = document.createElement('script')
    script.src = userKey ? `${SDK_URL}&key=${userKey}` : SDK_URL
    script.async = true
    script.onload = () => {
      if (window.TMap) resolve(window.TMap)
      else reject(new Error('地图 SDK 加载完成但未初始化，可能是代理地址未就绪'))
    }
    script.onerror = () => reject(new Error('地图 SDK 加载失败，请检查网络或地图代理配置'))
    document.head.appendChild(script)
  })
  return loading
}
