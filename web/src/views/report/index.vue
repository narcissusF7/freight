<template>
  <div class="page-container">
    <!-- 看板统计 -->
    <el-row :gutter="12">
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.total ?? 0 }}</div>
          <div class="stat-label">上报总数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-ok">
          <div class="stat-value">{{ stats.success ?? 0 }}</div>
          <div class="stat-label">上报成功</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-danger">
          <div class="stat-value">{{ stats.failed ?? 0 }}</div>
          <div class="stat-label">上报失败</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-warn">
          <div class="stat-value">{{ stats.waitingRetry ?? 0 }}</div>
          <div class="stat-label">待补偿重试</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-info">
          <div class="stat-value">{{ pct(stats.successRate) }}</div>
          <div class="stat-label">上报成功率</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ pendingTotal }}</div>
          <div class="stat-label">待上报单据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待上报概览 -->
    <el-card shadow="never" class="mt">
      <div class="toolbar">
        <div class="card-title">待上报概览</div>
        <div class="flex1" />
        <span class="hint">业务完成后必须上报，扫描任务兜底；失败进补偿队列退避重试</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="6" v-for="p in pendingItems" :key="p.label">
          <div class="pending-chip">
            <span class="pc-label">{{ p.label }}</span>
            <span class="pc-value" :class="p.value > 0 ? 'danger' : 'ok'">{{ p.value }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 上报记录列表 -->
    <el-card shadow="never" class="mt">
      <div class="toolbar">
        <el-select v-model="query.docType" placeholder="全部单据" clearable style="width: 140px" @change="load">
          <el-option label="运单" value="WAYBILL" />
          <el-option label="资金流水" value="FUND_FLOW" />
          <el-option label="车辆" value="VEHICLE" />
          <el-option label="驾驶员" value="DRIVER" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option label="待上报" value="PENDING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button type="primary" plain @click="load">查询</el-button>
        <div class="flex1" />
        <el-button :loading="scanning" @click="doScan">扫描上报</el-button>
        <el-button :loading="retrying" @click="doRetry">补偿重试</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="报文参考号" min-width="185" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.messageRefNo }}</span></template>
        </el-table-column>
        <el-table-column label="单据类型" width="95">
          <template #default="{ row }">
            <el-tag :type="docTypeTag(row.docType)" effect="plain" size="small">{{ DOC_TYPE_TEXT[row.docType] || row.docType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务单号" min-width="130" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.bizNo }}</span></template>
        </el-table-column>
        <el-table-column label="接口代码" width="150" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono small">{{ row.actionType }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="dark">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回执" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.receipt || '—' }}</template>
        </el-table-column>
        <el-table-column label="重试" width="55" align="center">
          <template #default="{ row }">{{ row.retryCount }}</template>
        </el-table-column>
        <el-table-column label="上报时间" width="125">
          <template #default="{ row }">{{ fmt(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" class-name="op-col">
          <template #default="{ row }">
            <el-button v-if="row.status === 'FAILED'" link type="warning" @click="doRetryRecord(row)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listReports, reportStats, retryRecord, retryReports, scanReports } from '@/api/report'

const STATUS_TEXT = { PENDING: '待上报', SUCCESS: '成功', FAILED: '失败' }
const DOC_TYPE_TEXT = { WAYBILL: '运单', FUND_FLOW: '资金流水', VEHICLE: '车辆', DRIVER: '驾驶员' }

const list = ref([])
const loading = ref(false)
const stats = ref({})
const scanning = ref(false)
const retrying = ref(false)
const query = reactive({ docType: '', status: '' })

const pendingItems = computed(() => [
  { label: '待上报运单', value: stats.value.pendingWaybill ?? 0 },
  { label: '待上报资金流水', value: stats.value.pendingFundFlow ?? 0 },
  { label: '待上报车辆', value: stats.value.pendingVehicle ?? 0 },
  { label: '待上报驾驶员', value: stats.value.pendingDriver ?? 0 }
])
const pendingTotal = computed(() =>
  pendingItems.value.reduce((a, p) => a + Number(p.value || 0), 0)
)

onMounted(async () => {
  await Promise.all([load(), loadStats()])
})

async function load() {
  loading.value = true
  try {
    list.value = await listReports({
      docType: query.docType || undefined,
      status: query.status || undefined,
      limit: 200
    })
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  stats.value = await reportStats()
}

function docTypeTag(t) {
  return { WAYBILL: 'primary', FUND_FLOW: 'success', VEHICLE: 'warning', DRIVER: 'info' }[t] || 'info'
}
function statusTag(s) {
  return { PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger' }[s] || 'info'
}
function pct(v) {
  if (v === null || v === undefined) return '—'
  return (Number(v) * 100).toFixed(1) + '%'
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

async function doScan() {
  scanning.value = true
  try {
    const n = await scanReports(100)
    ElMessage.success(n > 0 ? `本次上报 ${n} 条` : '没有新的待上报数据')
    await Promise.all([load(), loadStats()])
  } finally {
    scanning.value = false
  }
}

async function doRetry() {
  retrying.value = true
  try {
    const n = await retryReports(50)
    ElMessage.success(n > 0 ? `本次补偿重试 ${n} 条` : '没有到点的补偿任务')
    await Promise.all([load(), loadStats()])
  } finally {
    retrying.value = false
  }
}

async function doRetryRecord(row) {
  const r = await retryRecord(row.id)
  ElMessage.success(r.status === 'SUCCESS' ? '重试成功' : '已提交重试')
  await Promise.all([load(), loadStats()])
}
</script>

<style scoped>
.mt { margin-top: 14px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.sub { color: #a8abb2; font-size: 12px; }
.mono { font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 12px; }
.small { font-size: 11px; }
.ok { color: #0d9488; font-weight: 600; }
.danger { color: #d9363e; font-weight: 600; }
.card-title { font-size: 16px; font-weight: 600; }
:deep(.op-col .cell) { white-space: nowrap; overflow: visible; }

.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { margin-top: 4px; color: #909399; font-size: 12px; }
.tone-ok .stat-value { color: #0d9488; }
.tone-danger .stat-value { color: #d9363e; }
.tone-warn .stat-value { color: #c2751b; }
.tone-info .stat-value { color: #2f6bff; }

.pending-chip { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #f7f8fa; border-radius: 8px; }
.pc-label { color: #5c6470; font-size: 13px; }
.pc-value { font-size: 20px; font-weight: 700; }
</style>
