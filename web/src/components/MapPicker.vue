<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="820px"
    top="6vh"
    append-to-body
    @update:model-value="(v) => emit('update:modelValue', v)"
    @opened="initMap"
    @closed="destroyMap"
  >
    <div class="mp-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索地点，如：厦门北站 / 湖里区保税区"
        clearable
        @keyup.enter="doSearch"
      />
      <el-button type="primary" :loading="searching" @click="doSearch">搜索</el-button>
    </div>

    <el-alert
      v-if="error"
      type="error"
      :closable="false"
      show-icon
      :title="error"
      style="margin-bottom: 8px"
    />

    <div ref="mapEl" class="mp-map"></div>

    <div v-if="results.length" class="mp-results">
      <div v-for="(r, i) in results" :key="i" class="mp-item" @click="chooseResult(r)">
        <div class="mp-item-title">{{ r.title }}</div>
        <div class="mp-item-addr">{{ r.address }}</div>
      </div>
    </div>

    <div class="mp-picked">
      <span class="mp-picked-label">已选地点</span>
      <span :class="picked.address ? '' : 'muted'">
        {{ picked.address || '点击地图落点，或搜索后选择结果' }}
      </span>
      <span v-if="picked.lat !== null" class="muted mono">
        GCJ-02 {{ Number(picked.lat).toFixed(6) }}, {{ Number(picked.lng).toFixed(6) }}
      </span>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :disabled="picked.lat === null" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { loadTMap, DEFAULT_CENTER } from '@/utils/tmap'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '地图选点' },
  /** 初始坐标（编辑时回显） */
  lat: { type: [Number, String], default: null },
  lng: { type: [Number, String], default: null },
  address: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const mapEl = ref(null)
const keyword = ref('')
const results = ref([])
const searching = ref(false)
const error = ref('')
const picked = reactive({ address: '', lat: null, lng: null })

let TMapRef = null
let map = null
let markers = null
let geocoder = null
let suggestion = null

// 打开时用传入坐标复位（编辑回显）
watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    results.value = []
    error.value = ''
    keyword.value = ''
    picked.address = props.address || ''
    picked.lat = props.lat === null || props.lat === '' ? null : Number(props.lat)
    picked.lng = props.lng === null || props.lng === '' ? null : Number(props.lng)
  }
)

async function initMap() {
  error.value = ''
  try {
    TMapRef = await loadTMap()
    if (!map) {
      map = new TMapRef.Map(mapEl.value, {
        zoom: 12,
        center: new TMapRef.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng)
      })
      markers = new TMapRef.MultiMarker({
        map,
        styles: { default: new TMapRef.MarkerStyle({ width: 26, height: 34 }) },
        geometries: []
      })
      geocoder = new TMapRef.service.Geocoder()
      suggestion = new TMapRef.service.Suggestion({ pageSize: 10 })
      map.on('click', (evt) => setPoint(evt.latLng.lat, evt.latLng.lng, ''))
    }
    if (picked.lat !== null && picked.lng !== null) {
      map.setCenter(new TMapRef.LatLng(picked.lat, picked.lng))
      map.setZoom(15)
      dropMarker(picked.lat, picked.lng)
    }
  } catch (e) {
    error.value = e.message || '地图加载失败'
  }
}

function dropMarker(lat, lng) {
  markers.setGeometries([{ id: 'picked', styleId: 'default', position: new TMapRef.LatLng(lat, lng) }])
}

/** 落点：选完坐标后反查地址，地址栏可直接编辑 */
function setPoint(lat, lng, address) {
  if (!TMapRef) return
  picked.lat = Number(lat)
  picked.lng = Number(lng)
  picked.address = address || ''
  dropMarker(picked.lat, picked.lng)
  if (address) return
  geocoder
    .getAddress({ location: new TMapRef.LatLng(Number(lat), Number(lng)) })
    .then((res) => {
      const r = res && res.result
      if (!r) return
      const recommend = r.formatted_addresses && r.formatted_addresses.recommend
      picked.address = recommend || r.address || ''
    })
    .catch(() => {
      // 反查失败不阻断选点，允许用户手填地址
      if (!picked.address) picked.address = ''
    })
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  if (!TMapRef) {
    error.value = '地图尚未加载完成，请稍候重试'
    return
  }
  searching.value = true
  try {
    const res = await suggestion.getSuggestions({ keyword: kw })
    results.value = (res.data || []).map((it) => ({
      title: it.title || it.address || '未命名地点',
      address: it.address || '',
      lat: it.location && it.location.lat,
      lng: it.location && it.location.lng
    }))
    if (!results.value.length) ElMessage.info('未找到匹配地点，可直接在地图上点选')
  } catch (e) {
    error.value = '搜索失败：' + (e.message || '未知错误')
  } finally {
    searching.value = false
  }
}

function chooseResult(r) {
  if (r.lat === undefined || r.lng === undefined) return
  setPoint(r.lat, r.lng, r.address || r.title)
  results.value = []
  if (map) {
    map.setCenter(new TMapRef.LatLng(Number(r.lat), Number(r.lng)))
    map.setZoom(16)
  }
}

function confirm() {
  if (picked.lat === null) return
  emit('confirm', {
    address: (picked.address || '').slice(0, 200),
    lat: Number(picked.lat),
    lng: Number(picked.lng)
  })
  emit('update:modelValue', false)
}

function destroyMap() {
  if (map) {
    map.destroy()
    map = null
    markers = null
  }
}
</script>

<style scoped>
.mp-bar { display: flex; gap: 8px; margin-bottom: 8px; }
.mp-map { width: 100%; height: 380px; border-radius: 4px; background: #f2f3f5; }
.mp-results {
  max-height: 132px; overflow: auto; border: 1px solid #ebeef5; border-radius: 4px;
  margin-top: 8px;
}
.mp-item { padding: 6px 10px; cursor: pointer; border-bottom: 1px solid #f5f7fa; }
.mp-item:hover { background: #f5f7fa; }
.mp-item-title { font-size: 13px; color: #303133; }
.mp-item-addr { font-size: 12px; color: #909399; }
.mp-picked {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  margin-top: 10px; font-size: 13px;
}
.mp-picked-label { color: #606266; font-weight: 600; }
.muted { color: #909399; }
.mono { font-family: Consolas, Monaco, monospace; font-size: 12px; }
</style>
