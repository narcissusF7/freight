<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <div class="toolbar">
        <el-date-picker
          v-model="range" type="daterange" value-format="YYYY-MM-DD"
          start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false"
          style="width: 260px" @change="reloadAll"
        />
        <el-radio-group v-model="granularity" @change="loadTrend">
          <el-radio-button label="DAY">按日</el-radio-button>
          <el-radio-button label="WEEK">按周</el-radio-button>
          <el-radio-button label="MONTH">按月</el-radio-button>
        </el-radio-group>
        <div class="flex1" />
        <span class="hint">{{ overview.calcTime }}</span>
        <el-button :loading="rebuilding" @click="doRebuild">重算区间</el-button>
        <el-button type="primary" plain :loading="loading" @click="reloadAll">刷新</el-button>
      </div>
    </el-card>

    <!-- 指标卡：点击直接穿透到明细 -->
    <el-row :gutter="12" class="mt">
      <el-col :span="6" v-for="c in cards" :key="c.metric">
        <el-card shadow="never" class="stat-card clickable" @click="openDrill(c.metric)">
          <div class="stat-value">{{ c.value }}</div>
          <div class="stat-label">
            {{ c.label }}<span class="drill-hint">查看明细 →</span>
            <div v-if="c.sub" class="stat-sub">{{ c.sub }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mt">
      <el-col :span="16">
        <el-card shadow="never">
          <div class="card-head">
            <div>
              <div class="card-title">趋势分析</div>
              <div class="sub">按{{ granularityText }}聚合，数据来自预聚合宽表（stat_daily）</div>
            </div>
            <el-select v-model="metricKey" style="width: 170px">
              <el-option v-for="m in metrics" :key="m.code" :label="m.label" :value="m.code" />
            </el-select>
          </div>

          <div v-if="chart.coords.length" class="chart-wrap">
            <svg :viewBox="`0 0 ${W} ${H}`" class="chart" preserveAspectRatio="xMidYMid meet">
              <defs>
                <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#2f6bff" stop-opacity="0.22" />
                  <stop offset="100%" stop-color="#2f6bff" stop-opacity="0.01" />
                </linearGradient>
              </defs>
              <g v-for="g in grid" :key="g.y">
                <line :x1="PADL" :y1="g.y" :x2="W - PADR" :y2="g.y" stroke="#eef2f8" stroke-width="1" />
                <text :x="PADL - 8" :y="g.y + 4" text-anchor="end" class="axis">{{ g.label }}</text>
              </g>
              <path :d="chart.area" fill="url(#areaFill)" />
              <path :d="chart.line" fill="none" stroke="#2f6bff" stroke-width="2.2"
                    stroke-linejoin="round" stroke-linecap="round" />
              <g v-for="(p, i) in chart.coords" :key="i">
                <circle :cx="p.x" :cy="p.y" r="3.2" fill="#ffffff" stroke="#2f6bff" stroke-width="2" />
                <title>{{ p.label }}：{{ formatValue(p.v) }}</title>
              </g>
              <text v-for="l in xLabels" :key="l.x" :x="l.x" :y="H - 8" text-anchor="middle" class="axis">
                {{ l.label }}
              </text>
            </svg>
          </div>
          <el-empty v-else description="该区间暂无数据，点右上角「重算区间」生成预聚合" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <div class="card-head">
            <div>
              <div class="card-title">区域 / 维度分布</div>
              <div class="sub">点击任意一行穿透到该维度的明细</div>
            </div>
            <div class="dim-controls">
              <el-select v-model="dimType" style="width: 150px" @change="loadDims">
                <el-option label="装货地省份" value="LOAD_PROVINCE" />
                <el-option label="卸货地省份" value="UNLOAD_PROVINCE" />
                <el-option label="货物名称" value="CARGO" />
                <el-option label="司机" value="DRIVER" />
              </el-select>
              <el-radio-group v-model="dimByAmount" size="small" @change="loadDims">
                <el-radio-button :label="false">按单数</el-radio-button>
                <el-radio-button :label="true">按金额</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div v-if="dims.length" class="bars">
            <div v-for="d in dims" :key="d.dimKey" class="bar-row" @click="openDrill(metricKey, dimType, d.dimKey)">
              <div class="bar-head">
                <span class="bar-name" :title="d.dimLabel">{{ d.dimLabel }}</span>
                <span class="bar-val">{{ dimByAmount ? money(d.amount) : d.waybillCount + ' 单' }} · {{ dimByAmount ? percent(d.amountShare) : percent(d.share) }}</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: Math.max(2, (dimByAmount ? d.amountShare : d.share) * 100) + '%' }" />
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无维度数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 指标口径：统计页最容易吵的就是「这个数怎么算的」，把口径摊开写在页面上 -->
    <el-card shadow="never" class="mt">
      <div class="card-title">指标口径与数据来源</div>
      <div class="sub" style="margin: 6px 0 10px">
        所有数字均可逐级穿透到底层单据；预聚合按 `(日期, 机构)` 覆盖写入，重算即校正。
      </div>
      <el-table :data="metrics" size="small" border>
        <el-table-column prop="label" label="指标" width="140" />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column prop="source" label="取数来源（只读）" min-width="260" />
        <el-table-column label="说明" min-width="300">
          <template #default="{ row }">{{ metricNote(row.code) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drillVisible" :title="drillTitle" size="760px">
      <div class="drill-head">
        <div>
          <span class="sub">{{ range[0] }} ~ {{ range[1] }} · 共 {{ drillRows.length }} 条（最多展示 200 条）</span>
        </div>
        <el-tag v-if="drillFilter" type="warning" effect="plain">{{ drillFilter }}</el-tag>
      </div>
      <el-table :data="drillRows" border size="small" v-loading="drillLoading">
        <el-table-column label="单号" min-width="195" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.bizNo }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ BIZ_TEXT[row.bizType] || row.bizType }}</template>
        </el-table-column>
        <el-table-column label="相关方" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.partner || '—' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ row.amount === null ? '—' : money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="130">
          <template #default="{ row }">{{ fmt(row.bizTime) }}</template>
        </el-table-column>
        <el-table-column prop="extra" label="说明" min-width="220" show-overflow-tooltip />
        <template #empty>
          <div class="drill-empty">该区间 / 维度下暂无明细数据</div>
        </template>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { statDims, statDrill, statMetrics, statOverview, statRebuild, statTrend } from '@/api/stat'

const BIZ_TEXT = {
  WAYBILL: '运单',
  SETTLEMENT: '结算',
  PAYMENT: '支付',
  COMPLAINT: '投诉',
  RATING: '评价',
  ALARM: '预警'
}

/** 指标 → 趋势字段。金额与星级要按不同格式渲染，所以单独维护一张映射表。 */
const METRIC_FIELD = {
  WAYBILL_CREATED: { field: 'waybillCreated', type: 'count' },
  WAYBILL_SIGNED: { field: 'waybillSigned', type: 'count' },
  SETTLEMENT: { field: 'settleAmount', type: 'money' },
  PAYMENT: { field: 'payAmount', type: 'money' },
  COMPLAINT: { field: 'complaintCount', type: 'count' },
  RATING: { field: 'avgRating', type: 'score' },
  ALARM: { field: 'alarmCount', type: 'count' }
}

const W = 720
const H = 220
const PADL = 52
const PADR = 16
const PADT = 16
const PADB = 30

const today = new Date()
const start = new Date(today.getTime() - 29 * 86400000)
const range = ref([fmtDate(start), fmtDate(today)])
const granularity = ref('DAY')
const dimType = ref('LOAD_PROVINCE')
const dimByAmount = ref(false)
const metricKey = ref('WAYBILL_CREATED')

const loading = ref(false)
const rebuilding = ref(false)
const overview = ref({})
const metrics = ref([])
const trend = ref([])
const dims = ref([])

const drillVisible = ref(false)
const drillRows = ref([])
const drillLoading = ref(false)
const drillFilter = ref('')
const drillTitle = ref('数据穿透')

const granularityText = computed(() => ({ DAY: '日', WEEK: '周', MONTH: '月' }[granularity.value]))

const cards = computed(() => [
  { metric: 'WAYBILL_CREATED', label: '新建运单', value: overview.value.waybillCreated ?? 0 },
  {
    metric: 'WAYBILL_SIGNED',
    label: `签收完成（完成率 ${percent(overview.value.completionRate)}）`,
    value: overview.value.waybillSigned ?? 0,
    sub: `签收金额 ${money(overview.value.signedAmount)}`
  },
  { metric: 'SETTLEMENT', label: '应付结算金额', value: money(overview.value.settleAmount) },
  { metric: 'PAYMENT', label: '支付成功金额', value: money(overview.value.payAmount) },
  {
    metric: 'COMPLAINT',
    label: `投诉（逾期 ${overview.value.complaintOverdue ?? 0}）`,
    value: overview.value.complaintCount ?? 0
  },
  { metric: 'RATING', label: '平均星级', value: overview.value.avgRating ? Number(overview.value.avgRating).toFixed(2) : '—' },
  { metric: 'ALARM', label: '轨迹预警', value: overview.value.alarmCount ?? 0 }
])

const chart = computed(() => {
  const conf = METRIC_FIELD[metricKey.value] || METRIC_FIELD.WAYBILL_CREATED
  const values = trend.value.map((p) => Number(p[conf.field] || 0))
  // 先按 5 等分算出「整齐的刻度步长」，再反推坐标轴上限，刻度才是 0/25/50/75/100 这样的数
  const tickStep = niceStep(Math.max(...values))
  const max = tickStep * 5
  const innerW = W - PADL - PADR
  const innerH = H - PADT - PADB
  const step = values.length > 1 ? innerW / (values.length - 1) : 0
  const coords = values.map((v, i) => ({
    x: PADL + i * step,
    y: PADT + innerH - (v / max) * innerH,
    v,
    label: trend.value[i].period
  }))
  const line = coords.map((c, i) => `${i ? 'L' : 'M'}${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ')
  const area = coords.length
    ? `${line} L${coords[coords.length - 1].x.toFixed(1)},${PADT + innerH} L${coords[0].x.toFixed(1)},${PADT + innerH} Z`
    : ''
  return { coords, line, area, max }
})

const grid = computed(() => {
  const innerH = H - PADT - PADB
  const labels = [1, 0.8, 0.6, 0.4, 0.2, 0]
  return labels.map((r) => ({
    y: PADT + innerH * (1 - r),
    label: formatValue(chart.value.max * r)
  }))
})

const xLabels = computed(() => {
  const c = chart.value.coords
  if (!c.length) return []
  if (c.length <= 7) return c.map((p) => ({ x: p.x, label: shortPeriod(p.label) }))
  const idx = [0, Math.floor(c.length / 3), Math.floor((c.length * 2) / 3), c.length - 1]
  return idx.map((i) => ({ x: c[i].x, label: shortPeriod(c[i].label) }))
})

onMounted(async () => {
  metrics.value = await statMetrics()
  await reloadAll()
})

async function reloadAll() {
  loading.value = true
  try {
    const params = { from: range.value[0], to: range.value[1] }
    overview.value = await statOverview(params)
    await Promise.all([loadTrend(), loadDims()])
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  trend.value = await statTrend({ from: range.value[0], to: range.value[1], granularity: granularity.value })
}

async function loadDims() {
  dims.value = await statDims({ from: range.value[0], to: range.value[1], dimType: dimType.value, limit: 10 })
}

async function doRebuild() {
  rebuilding.value = true
  try {
    const days = await statRebuild({ from: range.value[0], to: range.value[1], operator: 'web' })
    ElMessage.success(`已重算 ${days} 天`)
    await reloadAll()
  } finally {
    rebuilding.value = false
  }
}

async function openDrill(metric, dim, key) {
  drillTitle.value = `数据穿透 · ${labelOf(metric)}`
  drillFilter.value = dim && key ? `${dimLabelOf(dim)}：${key}` : ''
  drillVisible.value = true
  drillLoading.value = true
  try {
    drillRows.value = await statDrill({
      from: range.value[0],
      to: range.value[1],
      metric,
      dimType: dim || undefined,
      dimKey: key || undefined,
      limit: 200
    })
  } finally {
    drillLoading.value = false
  }
}

// ---------------------------------------------------------------- 展示工具

function fmtDate(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function shortPeriod(p) {
  if (!p) return ''
  return p.length === 10 ? p.slice(5) : p
}

function percent(v) {
  if (v === null || v === undefined) return '0%'
  return (Number(v) * 100).toFixed(1).replace(/\.0$/, '') + '%'
}

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (n >= 10000) return '¥' + (n / 10000).toFixed(1) + ' 万'
  return '¥' + n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

/** 刻度步长：把「最大值/5」向上取到 1/2/2.5/5/10 × 10^n */
function niceStep(v) {
  const raw = (v || 0) / 5
  if (raw <= 0) return 1
  const mag = Math.pow(10, Math.floor(Math.log10(raw)))
  const n = raw / mag
  const niceN = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10
  return niceN * mag
}

function formatValue(v) {
  const conf = METRIC_FIELD[metricKey.value] || { type: 'count' }
  if (conf.type === 'money') return money(v)
  if (conf.type === 'score') return Number(v).toFixed(1)
  return Number(v).toFixed(0)
}

function labelOf(code) {
  const m = metrics.value.find((x) => x.code === code)
  return m ? `${m.label}（${m.unit}）` : code
}

function dimLabelOf(dim) {
  return {
    LOAD_PROVINCE: '装货省',
    UNLOAD_PROVINCE: '卸货省',
    CARGO: '货物',
    DRIVER: '司机'
  }[dim] || dim
}

/** 明细里的时间是「哪天发生的」，MM-DD HH:mm 足够，还能省一列宽度 */
const METRIC_NOTE = {
  WAYBILL_CREATED: '按建单时间归集，重复建单不合并（一张运单一次）',
  WAYBILL_SIGNED: '按签收时间归集；签收时间在状态跃迁时落库，不靠状态字符串反推',
  SETTLEMENT: '按结算业务日期归集，只统计应付（司机侧）',
  PAYMENT: '按支付成功时间归集，含结算付款与提现出款',
  COMPLAINT: '按建单时间归集，逾期指超过 24 小时时限',
  RATING: '只统计有效评价，隐藏评价不计入均分',
  ALARM: '按预警触发时间归集，四类预警合并计数'
}
function metricNote(code) {
  return METRIC_NOTE[code] || '—'
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(5, 16) : '-'
}

const STATUS_TEXT = {
  DRAFT: '草稿', PUBLISHED: '待接单', ACCEPTED: '待装货', LOADED: '待发车', IN_TRANSIT: '运输中',
  UNLOADED: '待签收', SIGNED: '已签收', RECEIPT_UPLOADED: '已回单', SETTLED: '已结算',
  REPORTED: '已上报', COMPLETED: '已完成', EXCEPTION: '异常', WITHDRAWN: '已撤回', VOID: '作废',
  PENDING: '待受理', PROCESSING: '处理中', RESOLVED: '已处理', REJECTED: '已驳回', CLOSED: '已办结',
  CONFIRMED: '已确认', PAYING: '支付中', PAID: '已支付', INVOICED: '已开票',
  SUCCESS: '成功', FAILED: '失败', VALID: '正常', HIDDEN: '已隐藏', 0: '待处理', 1: '已处理'
}
function statusLabel(s) {
  if (s === null || s === undefined || s === '') return '—'
  return STATUS_TEXT[s] || s
}
</script>

<style scoped>
.mt { margin-top: 12px; }
.filter-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; gap: 12px; align-items: center; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.sub { color: #a8abb2; font-size: 12px; }
.mono { font-family: 'SF Mono', Consolas, Monaco, monospace; }

.stat-card { text-align: center; }
.stat-card.clickable { cursor: pointer; transition: box-shadow 0.15s ease; }
.stat-card.clickable:hover { box-shadow: 0 4px 14px rgba(23, 43, 77, 0.1); }
.stat-value { font-size: 24px; font-weight: 700; color: #1f2733; }
.stat-label { margin-top: 4px; color: #909399; font-size: 12px; }
.stat-sub { margin-top: 3px; color: #c0c4cc; font-size: 12px; font-weight: 400; }
.drill-hint { margin-left: 6px; color: #2f6bff; opacity: 0; transition: opacity 0.15s ease; }
.clickable:hover .drill-hint { opacity: 1; }

.card-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.card-title { font-size: 16px; font-weight: 600; }
.dim-controls { display: flex; flex-direction: column; gap: 8px; align-items: flex-end; }

.chart-wrap { width: 100%; }
.chart { width: 100%; height: auto; display: block; }
.axis { font-size: 11px; fill: #a8abb2; }

.bars { margin-top: 6px; }
.bar-row { padding: 8px 0; cursor: pointer; border-bottom: 1px dashed #f0f2f5; }
.bar-row:last-child { border-bottom: none; }
.bar-row:hover .bar-name { color: #2f6bff; }
.bar-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px; }
.bar-name { font-size: 13px; color: #1f2733; max-width: 55%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-val { font-size: 12px; color: #8a94a6; }
.bar-track { height: 8px; background: #f2f5fa; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, #5b8cff, #2f6bff); }

.drill-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.drill-empty { color: #909399; padding: 24px 0; }
</style>
