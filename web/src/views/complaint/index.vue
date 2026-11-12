<template>
  <div class="page-container">
    <el-row :gutter="12">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <el-card shadow="never" class="stat-card" :class="card.tone">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="load">
          <el-option v-for="(v, k) in STATUS_TEXT" :key="k" :label="v" :value="k" />
        </el-select>
        <el-select v-model="query.complaintType" placeholder="全部类型" clearable style="width: 150px" @change="load">
          <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="k" />
        </el-select>
        <el-checkbox v-model="query.overdue" @change="load">仅看逾期</el-checkbox>
        <el-button type="primary" plain @click="load">查询</el-button>
        <div class="flex1" />
        <span class="hint">时限口径：受理后 24 小时内处理并反馈</span>
        <el-button @click="doScan">扫描逾期</el-button>
        <el-button type="primary" @click="openCreate">登记投诉</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border @row-click="openDetail">
        <el-table-column label="投诉单号 / 运单" min-width="205">
          <template #default="{ row }">
            <div class="mono">{{ row.complaintNo }}</div>
            <div class="sub">{{ row.waybillNo || '未关联运单' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型 / 等级" width="105">
          <template #default="{ row }">
            <div>{{ TYPE_TEXT[row.complaintType] || row.complaintType }}</div>
            <el-tag v-if="row.level === 'URGENT'" type="danger" size="small" effect="plain">紧急</el-tag>
            <span v-else class="sub">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="投诉人 → 被投诉" min-width="165" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="truncate">{{ row.complainantName || '匿名' }}</div>
            <div class="truncate">→ {{ row.targetName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时限" width="100">
          <template #default="{ row }">
            <span :class="slaClass(row)">{{ slaText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="135" class-name="op-col">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" @click.stop="openAction('assign', row)">受理</el-button>
            <el-button v-if="row.status === 'PROCESSING'" link type="warning" @click.stop="openAction('resolve', row)">处理</el-button>
            <el-button v-if="row.status === 'RESOLVED'" link type="success" @click.stop="openAction('close', row)">办结</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情抽屉：单据 + 处理过程 + 可执行动作 -->
    <el-drawer v-model="drawer" title="投诉详情" size="640px">
      <template v-if="detail.complaint">
        <div class="drawer-head">
          <div>
            <div class="mono big">{{ detail.complaint.complaintNo }}</div>
            <div class="sub">
              {{ TYPE_TEXT[detail.complaint.complaintType] }} ·
              {{ detail.complaint.level === 'URGENT' ? '紧急' : '普通' }} ·
              {{ SOURCE_TEXT[detail.complaint.source] || detail.complaint.source }}
            </div>
          </div>
          <el-tag :type="statusType(detail.complaint.status)" size="large">
            {{ STATUS_TEXT[detail.complaint.status] }}
          </el-tag>
        </div>

        <el-alert
          v-if="detail.slaBreached"
          type="error" :closable="false" show-icon
          :title="`已超过 24 小时时限（截止 ${fmt(detail.complaint.deadlineTime)}）`"
          class="mb"
        />
        <el-alert
          v-else-if="detail.complaint.status === 'PENDING' || detail.complaint.status === 'PROCESSING'"
          type="warning" :closable="false" show-icon
          :title="`剩余 ${detail.remainHours} 小时（截止 ${fmt(detail.complaint.deadlineTime)}）`"
          class="mb"
        />

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="投诉人">
            {{ detail.complaint.complainantName || '-' }}
            <span class="sub">{{ detail.complaint.complainantPhone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="被投诉">
            {{ detail.complaint.targetName }}
          </el-descriptions-item>
          <el-descriptions-item label="关联运单">{{ detail.complaint.waybillNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ detail.complaint.assignTo || '未分派' }}</el-descriptions-item>
          <el-descriptions-item label="投诉内容" :span="2">
            {{ detail.complaint.content }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.complaint.resolveResult" label="处理结论" :span="2">
            {{ detail.complaint.resolveResult }}
            <el-tag v-if="detail.complaint.liable === 1" type="danger" size="small" effect="plain">责任成立</el-tag>
            <el-tag v-else-if="detail.complaint.status === 'RESOLVED' || detail.complaint.status === 'CLOSED'"
                    type="info" size="small" effect="plain">责任不成立</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.complaint.satisfaction" label="满意度">
            <el-rate :model-value="detail.complaint.satisfaction" disabled />
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">处理过程</div>
        <el-timeline>
          <el-timeline-item
            v-for="h in detail.handles" :key="h.id"
            :timestamp="fmt(h.handleTime)" placement="top"
            :type="h.handleType === 'ESCALATE' ? 'danger' : h.handleType === 'RESOLVE' ? 'success' : 'primary'"
          >
            <div class="handle-title">{{ HANDLE_TEXT[h.handleType] || h.handleType }}
              <span class="sub">{{ h.handleBy }}</span>
            </div>
            <div class="handle-body">{{ h.content }}</div>
          </el-timeline-item>
        </el-timeline>

        <div class="drawer-actions">
          <el-button v-if="detail.complaint.status === 'PENDING'" type="success" @click="openAction('assign')">受理分派</el-button>
          <el-button v-if="detail.complaint.status === 'PROCESSING'" @click="openAction('follow')">跟进记录</el-button>
          <el-button v-if="detail.complaint.status === 'PROCESSING'" type="warning" @click="openAction('resolve')">处理完成</el-button>
          <el-button v-if="detail.complaint.status === 'RESOLVED'" type="success" @click="openAction('close')">办结</el-button>
          <el-button
            v-if="detail.complaint.status === 'PENDING' || detail.complaint.status === 'PROCESSING'"
            type="danger" plain @click="openAction('reject')"
          >驳回</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 登记投诉 -->
    <el-dialog v-model="createVisible" title="登记投诉" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="关联运单">
          <el-input v-model="form.waybillId" placeholder="运单 ID（填了可自动识别被诉司机/货主）" />
        </el-form-item>
        <el-form-item label="投诉类型">
          <el-select v-model="form.complaintType" style="width: 100%">
            <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="form.level">
            <el-radio label="NORMAL">普通</el-radio>
            <el-radio label="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="投诉人">
          <el-input v-model="form.complainantName" placeholder="姓名 / 单位" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.complainantPhone" />
        </el-form-item>
        <el-form-item label="被投诉对象">
          <el-select v-model="form.targetType" style="width: 120px; margin-right: 8px">
            <el-option label="司机" value="DRIVER" />
            <el-option label="货主" value="SHIPPER" />
            <el-option label="平台" value="PLATFORM" />
          </el-select>
          <el-input v-model="form.targetId" placeholder="对象 ID（关联运单可留空）" style="width: 200px" />
        </el-form-item>
        <el-form-item label="投诉内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="时间、地点、经过、诉求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 动作弹窗：受理 / 跟进 / 处理 / 办结 / 驳回 -->
    <el-dialog v-model="actionVisible" :title="actionTitle" width="520px">
      <el-form label-width="92px">
        <template v-if="action === 'assign'">
          <el-form-item label="处理人"><el-input v-model="actionForm.assignTo" placeholder="客服姓名" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="actionForm.remark" /></el-form-item>
        </template>
        <template v-else-if="action === 'follow'">
          <el-form-item label="跟进内容">
            <el-input v-model="actionForm.content" type="textarea" :rows="3" placeholder="联系了谁、对方怎么说" />
          </el-form-item>
        </template>
        <template v-else-if="action === 'resolve'">
          <el-form-item label="处理结论">
            <el-input v-model="actionForm.result" type="textarea" :rows="4" placeholder="会作为反馈内容发给投诉人" />
          </el-form-item>
          <el-form-item label="责任认定">
            <el-radio-group v-model="actionForm.liable">
              <el-radio :label="true">成立（扣信用分 10）</el-radio>
              <el-radio :label="false">不成立</el-radio>
            </el-radio-group>
            <div class="hint">只有责任成立才扣信用分，默认不冤枉人</div>
          </el-form-item>
        </template>
        <template v-else-if="action === 'close'">
          <el-form-item label="满意度">
            <el-rate v-model="actionForm.satisfaction" />
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="actionForm.remark" /></el-form-item>
        </template>
        <template v-else-if="action === 'reject'">
          <el-form-item label="驳回原因">
            <el-input v-model="actionForm.reason" type="textarea" :rows="3" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignComplaint, closeComplaint, complaintStats, createComplaint, followComplaint,
  getComplaint, listComplaints, rejectComplaint, resolveComplaint, scanOverdue
} from '@/api/customer'

const STATUS_TEXT = {
  PENDING: '待受理',
  PROCESSING: '处理中',
  RESOLVED: '已处理',
  REJECTED: '已驳回',
  CLOSED: '已办结'
}
const TYPE_TEXT = {
  DELAY: '运输延误',
  DAMAGE: '货物破损',
  ATTITUDE: '服务态度',
  FEE: '费用争议',
  LOST: '货物丢失',
  OTHER: '其他'
}
const HANDLE_TEXT = {
  CREATE: '建单',
  ASSIGN: '受理分派',
  FOLLOW: '跟进',
  RESOLVE: '处理完成',
  REJECT: '驳回',
  CLOSE: '办结',
  ESCALATE: '逾期升级'
}

const list = ref([])
const loading = ref(false)
const stats = ref({})
const query = reactive({ status: '', complaintType: '', overdue: false })

const drawer = ref(false)
const detail = ref({ complaint: null, handles: [] })

const createVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  waybillId: '', complaintType: 'DELAY', level: 'NORMAL',
  complainantName: '', complainantPhone: '', targetType: 'DRIVER', targetId: '', content: ''
})

const actionVisible = ref(false)
const action = ref('assign')
const actionForm = reactive({ assignTo: '', remark: '', content: '', result: '', liable: false, satisfaction: 4, reason: '' })
const actionTitle = computed(() => ({
  assign: '受理分派', follow: '跟进记录', resolve: '处理完成', close: '办结确认', reject: '驳回投诉'
}[action.value]))

// 平均处理时长：不足 1 小时显示分钟，否则「0 h」会让人以为没在干活
const handleTime = computed(() => {
  const handled = (stats.value.closed ?? 0) + (stats.value.resolved ?? 0)
  if (!handled) return '—'
  const h = stats.value.avgHandleHours ?? 0
  if (!h) return '1 分钟内'
  return h < 1 ? Math.round(h * 60) + ' 分钟' : h.toFixed(1) + ' 小时'
})

const cards = computed(() => [
  { label: '累计投诉', value: stats.value.total ?? 0, tone: '' },
  { label: '待受理', value: stats.value.pending ?? 0, tone: 'tone-warn' },
  { label: '处理中', value: stats.value.processing ?? 0, tone: 'tone-info' },
  { label: '当前逾期', value: stats.value.overdue ?? 0, tone: stats.value.overdue ? 'tone-danger' : '' },
  { label: '24h 达标率', value: pct(stats.value.onTimeRate), tone: 'tone-ok' },
  { label: '平均处理时长', value: handleTime, tone: '' }
])

onMounted(async () => {
  await load()
  await loadStats()
})

async function load() {
  loading.value = true
  try {
    list.value = await listComplaints({
      status: query.status || undefined,
      complaintType: query.complaintType || undefined,
      overdue: query.overdue || undefined,
      limit: 200
    })
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  stats.value = await complaintStats()
}

function statusType(s) {
  return { PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'info', REJECTED: 'info', CLOSED: 'success' }[s] || 'info'
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

function pct(v) {
  if (v === null || v === undefined) return '—'
  return (Number(v) * 100).toFixed(0) + '%'
}

/** 剩余/逾期小时：提交后由服务端判定，这里只做展示，避免前后端两套口径 */
function remainHours(row) {
  const deadline = new Date(String(row.deadlineTime).replace(' ', 'T'))
  const base = row.resolvedTime ? new Date(String(row.resolvedTime).replace(' ', 'T')) : new Date()
  return Math.round((deadline - base) / 3600000)
}

function slaText(row) {
  // 已结束的单子看「是否按时办结」，而不是显示剩余时间：剩余时间对已办结的投诉没有意义
  if (['CLOSED', 'REJECTED'].includes(row.status)) {
    if (!row.resolvedTime) return '已结束'
    return new Date(String(row.resolvedTime)) <= new Date(String(row.deadlineTime)) ? '按时办结' : '超时办结'
  }
  const h = remainHours(row)
  if (h < 0) return `已超时 ${Math.abs(h)}h`
  return `剩 ${h}h`
}

function slaClass(row) {
  if (['CLOSED', 'REJECTED'].includes(row.status)) {
    if (!row.resolvedTime) return 'sub'
    return new Date(String(row.resolvedTime)) <= new Date(String(row.deadlineTime)) ? 'ok' : 'danger'
  }
  return remainHours(row) < 0 ? 'danger' : remainHours(row) <= 4 ? 'warn' : 'sub'
}

const SOURCE_TEXT = { DRIVER_APP: '司机端', SHIPPER_APP: '货主端', WEB: '后台录入', PHONE: '电话受理' }

async function openDetail(row) {
  detail.value = await getComplaint(row.id)
  drawer.value = true
}

function openCreate() {
  Object.assign(form, {
    waybillId: '', complaintType: 'DELAY', level: 'NORMAL',
    complainantName: '', complainantPhone: '', targetType: 'DRIVER', targetId: '', content: ''
  })
  createVisible.value = true
}

async function doCreate() {
  if (!form.content || form.content.trim().length < 5) {
    ElMessage.warning('请填写投诉内容（至少 5 个字）')
    return
  }
  submitting.value = true
  try {
    const payload = {
      orgId: 1,
      source: 'WEB',
      complainantType: 'SHIPPER',
      complainantName: form.complainantName || undefined,
      complainantPhone: form.complainantPhone || undefined,
      targetType: form.targetType,
      targetId: form.targetId ? Number(form.targetId) : undefined,
      waybillId: form.waybillId ? Number(form.waybillId) : undefined,
      complaintType: form.complaintType,
      level: form.level,
      content: form.content,
      operator: 'web'
    }
    const created = await createComplaint(payload)
    ElMessage.success(`投诉已登记：${created.complaintNo}，24 小时内需处理`)
    createVisible.value = false
    await load()
    await loadStats()
  } finally {
    submitting.value = false
  }
}

function openAction(kind, row) {
  action.value = kind
  if (row) {
    detail.value = { complaint: row, handles: [] }
  }
  Object.assign(actionForm, {
    assignTo: detail.value.complaint?.assignTo || '',
    remark: '', content: '', result: '', liable: false, satisfaction: 4, reason: ''
  })
  actionVisible.value = true
}

async function doAction() {
  const id = detail.value.complaint?.id
  if (!id) return
  submitting.value = true
  try {
    let res
    if (action.value === 'assign') {
      res = await assignComplaint(id, { assignTo: actionForm.assignTo, remark: actionForm.remark, operator: 'web' })
    } else if (action.value === 'follow') {
      res = await followComplaint(id, { content: actionForm.content, operator: 'web' })
    } else if (action.value === 'resolve') {
      res = await resolveComplaint(id, {
        result: actionForm.result, liable: actionForm.liable, operator: 'web'
      })
    } else if (action.value === 'close') {
      res = await closeComplaint(id, {
        satisfaction: actionForm.satisfaction, remark: actionForm.remark, operator: 'web'
      })
    } else {
      res = await rejectComplaint(id, { reason: actionForm.reason, operator: 'web' })
    }
    detail.value = res
    drawer.value = true
    actionVisible.value = false
    ElMessage.success('已提交')
    await load()
    await loadStats()
  } finally {
    submitting.value = false
  }
}

async function doScan() {
  const n = await scanOverdue()
  ElMessage.success(n > 0 ? `本次升级 ${n} 条逾期投诉` : '没有新的逾期投诉')
  await load()
  await loadStats()
}
</script>

<style scoped>
.mt { margin-top: 14px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.sub { color: #a8abb2; font-size: 12px; }
.danger { color: #d9363e; font-weight: 600; }
.ok { color: #0d9488; font-weight: 600; }
:deep(.op-col .cell) { white-space: nowrap; overflow: visible; }
.warn { color: #c2751b; font-weight: 600; }
.mono { font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 12px; }
.truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.big { font-size: 16px; font-weight: 600; }

.stat-card { text-align: center; }
.stat-card.tone-warn .stat-value { color: #c2751b; }
.stat-card.tone-info .stat-value { color: #2f6bff; }
.stat-card.tone-danger .stat-value { color: #d9363e; }
.stat-card.tone-ok .stat-value { color: #0d9488; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { margin-top: 4px; color: #909399; font-size: 12px; }

.drawer-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.section-title { margin: 20px 0 12px; font-weight: 600; }
.handle-title { font-weight: 600; }
.handle-body { color: #5c6470; margin-top: 4px; }
.mb { margin-bottom: 12px; }
.drawer-actions { margin-top: 18px; display: flex; gap: 8px; }
</style>
