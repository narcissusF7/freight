<template>
  <div class="page-container">
    <div class="toolbar">
      <el-input v-model="query.waybillId" placeholder="运单ID" clearable style="width: 130px" />
      <el-select v-model="query.alarmType" placeholder="全部类型" clearable style="width: 140px">
        <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="未处理" :value="0" />
        <el-option label="已处理" :value="1" />
        <el-option label="已忽略" :value="2" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button :disabled="!query.waybillId" @click="doAnalyze">补算该运单</el-button>
      <span class="muted">
        共 {{ list.length }} 条；未处理 {{ list.filter((x) => x.status === 0).length }} 条。
        上报轨迹后系统自动分析，补算用于历史时间窗回溯（幂等，不会重复）
      </span>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="alarmTime" label="触发时间" width="170" />
      <el-table-column prop="waybillId" label="运单ID" width="90" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.alarmType)">{{ TYPE_TEXT[row.alarmType] || row.alarmType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="90">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.alarmLevel)" effect="plain">{{ LEVEL_TEXT[row.alarmLevel] || row.alarmLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="预警内容" min-width="280" show-overflow-tooltip />
      <el-table-column label="位置" width="170">
        <template #default="{ row }">
          <span class="muted">{{ row.lat }}, {{ row.lng }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'danger' : row.status === 1 ? 'success' : 'info'">
            {{ STATUS_TEXT[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleRemark" label="处理备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status !== 0" @click="openHandle(row)">处理</el-button>
          <el-button link type="info" :disabled="row.status !== 0" @click="openHandle(row, true)">忽略</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">轨迹分区运维</el-divider>
    <div class="toolbar">
      <span class="muted">
        分区数 {{ partitions.length }} 个，最大边界 {{ maxBoundary }}（按月分区，冷数据先归档再 DROP PARTITION）
      </span>
      <el-button @click="loadPartitions">刷新分区</el-button>
      <el-button @click="doEnsure">预建下月分区</el-button>
      <el-date-picker
        v-model="archiveBefore"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="归档早于该日期"
        style="width: 180px"
      />
      <el-button @click="doArchive">归档演练（dryRun）</el-button>
    </div>
    <el-table v-if="archiveReport" :data="archiveReport.items" border size="small">
      <el-table-column prop="partitionName" label="分区" width="120" />
      <el-table-column prop="periodStart" label="起始" width="120" />
      <el-table-column prop="periodEnd" label="截止" width="120" />
      <el-table-column prop="rowCount" label="行数" width="110" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="message" label="说明" min-width="240" />
    </el-table>

    <el-dialog v-model="handleVisible" :title="handleTitle" width="460px">
      <el-form label-width="90px">
        <el-form-item label="预警内容">
          <span>{{ current ? current.content : '' }}</span>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleRemark" type="textarea" :rows="3" placeholder="如：已电话联系司机确认绕行" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listAlarms,
  handleAlarm,
  analyzeTrackAlarms,
  listTrackPartitions,
  ensureTrackPartition,
  archiveTrackPartitions
} from '@/api/track'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const query = reactive({ waybillId: '', alarmType: '', status: null })

const TYPE_TEXT = {
  OVERSPEED: '超速',
  DEVIATE: '偏航',
  LONG_STAY: '异常停留',
  FENCE_IN: '进入围栏',
  FENCE_OUT: '离开围栏'
}
const LEVEL_TEXT = { HIGH: '高', MEDIUM: '中', LOW: '低' }
const STATUS_TEXT = { 0: '未处理', 1: '已处理', 2: '已忽略' }

const typeTag = (t) =>
  t === 'OVERSPEED' ? 'danger' : t === 'DEVIATE' ? 'warning' : t === 'LONG_STAY' ? 'warning' : 'info'
const levelTag = (l) => (l === 'HIGH' ? 'danger' : l === 'MEDIUM' ? 'warning' : 'info')

const handleVisible = ref(false)
const handleTitle = ref('处理预警')
const handleRemark = ref('')
const current = ref(null)
const handleStatus = ref(1)
const saving = ref(false)

const partitions = ref([])
const archiveBefore = ref('')
const archiveReport = ref(null)
const maxBoundary = computed(() => {
  const list2 = partitions.value.filter((p) => p.partitionName !== 'pmax')
  if (!list2.length) return '-'
  return list2[list2.length - 1].partitionDescription
})

async function load() {
  list.value = await listAlarms({
    orgId: userStore.userInfo.orgId,
    waybillId: query.waybillId ? Number(query.waybillId) : undefined,
    alarmType: query.alarmType || undefined,
    status: query.status === null || query.status === '' ? undefined : query.status
  })
}

async function doAnalyze() {
  const n = await analyzeTrackAlarms(Number(query.waybillId))
  ElMessage.success(n > 0 ? `新增 ${n} 条预警` : '未发现新预警（已存在的不会重复生成）')
  await load()
}

function openHandle(row, ignore) {
  current.value = row
  handleStatus.value = ignore ? 2 : 1
  handleTitle.value = ignore ? '忽略预警' : '处理预警'
  handleRemark.value = ''
  handleVisible.value = true
}

async function submitHandle() {
  saving.value = true
  try {
    await handleAlarm(current.value.id, {
      handleBy: userStore.userInfo.username || 'admin',
      handleRemark: handleRemark.value,
      status: handleStatus.value
    })
    ElMessage.success('已提交')
    handleVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function loadPartitions() {
  partitions.value = await listTrackPartitions()
}

async function doEnsure() {
  const r = await ensureTrackPartition()
  ElMessage.success(`${r.partitionName}：${r.action}（${r.message}）`)
  await loadPartitions()
}

async function doArchive() {
  if (!archiveBefore.value) {
    ElMessage.warning('请先选择归档截止日期')
    return
  }
  archiveReport.value = await archiveTrackPartitions(archiveBefore.value, true)
  ElMessage.success(`演练完成：${archiveReport.value.items.length} 个分区，共 ${archiveReport.value.totalRows} 行（未删除）`)
}

onMounted(async () => {
  await load()
  await loadPartitions()
})
</script>
