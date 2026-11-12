<template>
  <div class="page-container">
    <el-row :gutter="12">
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ allSubjects.length }}</div>
          <div class="stat-label">信用主体</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-ok">
          <div class="stat-value">{{ levelCount.A }}</div>
          <div class="stat-label">A 级（≥90）</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-info">
          <div class="stat-value">{{ levelCount.B }}</div>
          <div class="stat-label">B 级（75~89）</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-warn">
          <div class="stat-value">{{ levelCount.C }}</div>
          <div class="stat-label">C 级（60~74）</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card tone-danger">
          <div class="stat-value">{{ levelCount.D }}</div>
          <div class="stat-label">D 级（&lt;60）</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ avgScore }}</div>
          <div class="stat-label">平均信用分</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt">
      <div class="toolbar">
        <el-select v-model="query.subjectType" placeholder="全部主体" clearable style="width: 150px" @change="load">
          <el-option label="司机" value="DRIVER" />
          <el-option label="货主" value="SHIPPER" />
        </el-select>
        <el-select v-model="query.level" placeholder="全部等级" clearable style="width: 140px" @change="load">
          <el-option v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l + ' 级'" :value="l" />
        </el-select>
        <el-button type="primary" plain @click="load">查询</el-button>
        <div class="flex1" />
        <span class="hint">初始 100 分：投诉成立 −10、差评 −5、好评 +2；低分优先展示（风控视角）</span>
      </div>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="信用主体" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.subjectName || '—' }}</div>
            <div class="sub">{{ row.subjectType === 'DRIVER' ? '司机' : '货主' }} · ID {{ row.subjectId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="信用分" width="120">
          <template #default="{ row }">
            <span class="score" :class="'lv-' + row.level">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" effect="dark">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="好评 / 中评 / 差评" width="170" align="center">
          <template #default="{ row }">
            <span class="ok">{{ row.goodCount }}</span> /
            <span class="sub">{{ row.neutralCount }}</span> /
            <span class="danger">{{ row.badCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="投诉成立" width="100" align="center">
          <template #default="{ row }">{{ row.complaintCount }}</template>
        </el-table-column>
        <el-table-column label="累计评价" width="100" align="center">
          <template #default="{ row }">{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="最近变动" width="150">
          <template #default="{ row }">{{ fmt(row.lastChangeTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">变动明细</el-button>
            <el-button link type="warning" @click="openAdjust(row)">调账</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty">
            暂无信用记录。产生评价或责任成立的投诉后，这里会自动生成分值。
          </div>
        </template>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="信用变动明细" size="620px">
      <template v-if="detail.subject">
        <div class="drawer-head">
          <div>
            <div class="big">{{ detail.subject.subjectName || detail.subject.subjectId }}</div>
            <div class="sub">
              {{ detail.subject.subjectType === 'DRIVER' ? '司机' : '货主' }} ·
              好评 {{ detail.subject.goodCount }} / 中评 {{ detail.subject.neutralCount }} /
              差评 {{ detail.subject.badCount }} · 投诉成立 {{ detail.subject.complaintCount }}
            </div>
          </div>
          <div class="score-big" :class="'lv-' + detail.subject.level">
            {{ detail.subject.score }}<span class="unit">分</span>
          </div>
        </div>

        <el-table :data="detail.flows" border size="small">
          <el-table-column label="时间" width="110">
            <template #default="{ row }">{{ fmt(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="事件" width="96">
            <template #default="{ row }">{{ BIZ_TEXT[row.bizType] || row.bizType }}</template>
          </el-table-column>
          <el-table-column label="变动" width="68" align="right">
            <template #default="{ row }">
              <span :class="row.delta > 0 ? 'ok' : row.delta < 0 ? 'danger' : 'sub'">
                {{ row.delta > 0 ? '+' + row.delta : row.delta }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变化" width="105" align="center">
            <template #default="{ row }">{{ row.scoreBefore }} → {{ row.scoreAfter }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        </el-table>
        <div class="hint mt8">
          分值 = 100 + 所有变动之和；有疑问按单号回到投诉/评价页面核查，人工纠错走「调账」并留原因。
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="adjustVisible" title="信用人工调账" width="480px">
      <el-form label-width="86px">
        <el-form-item label="主体">
          <div>{{ adjustForm.subjectName }}（当前 {{ adjustForm.currentScore }} 分）</div>
        </el-form-item>
        <el-form-item label="调整分值">
          <el-input-number v-model="adjustForm.delta" :min="-100" :max="100" :step="1" />
          <span class="hint" style="margin-left: 8px">负数扣分，正数恢复</span>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="必须写清核实结论，司机申诉时可追溯" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adjustCredit, creditDetail, listCredits } from '@/api/customer'

const BIZ_TEXT = {
  COMPLAINT_CONFIRMED: '投诉成立',
  RATING_GOOD: '好评',
  RATING_NEUTRAL: '中评',
  RATING_BAD: '差评',
  ADJUST: '人工调账'
}

const list = ref([])
const loading = ref(false)
const query = reactive({ subjectType: '', level: '' })

// 指标卡基于「全量主体」计算，不受列表筛选影响：平均信用分是全局口径，
// 切换筛选后列表变了但平均分不该跟着漂，否则数字无法解释。
const allSubjects = ref([])

const drawer = ref(false)
const detail = ref({ subject: null, flows: [] })

const adjustVisible = ref(false)
const submitting = ref(false)
const adjustForm = reactive({ subjectType: '', subjectId: null, subjectName: '', currentScore: 0, delta: 0, reason: '' })

const levelCount = computed(() => {
  const c = { A: 0, B: 0, C: 0, D: 0 }
  allSubjects.value.forEach((s) => { if (c[s.level] !== undefined) c[s.level]++ })
  return c
})
const avgScore = computed(() => {
  if (!allSubjects.value.length) return '—'
  const sum = allSubjects.value.reduce((a, s) => a + Number(s.score || 0), 0)
  return (sum / allSubjects.value.length).toFixed(0)
})

onMounted(async () => {
  await Promise.all([load(), loadAllStats()])
})

async function load() {
  loading.value = true
  try {
    list.value = await listCredits({
      subjectType: query.subjectType || undefined,
      level: query.level || undefined,
      limit: 300
    })
  } finally {
    loading.value = false
  }
}

async function loadAllStats() {
  allSubjects.value = await listCredits({ limit: 500 })
}

function levelType(l) {
  return { A: 'success', B: 'primary', C: 'warning', D: 'danger' }[l] || 'info'
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(5, 16) : '-'
}

async function openDetail(row) {
  detail.value = await creditDetail(row.subjectType, row.subjectId)
  drawer.value = true
}

function openAdjust(row) {
  Object.assign(adjustForm, {
    subjectType: row.subjectType,
    subjectId: row.subjectId,
    subjectName: row.subjectName || row.subjectId,
    currentScore: row.score,
    delta: 0,
    reason: ''
  })
  adjustVisible.value = true
}

async function doAdjust() {
  if (!adjustForm.delta) {
    ElMessage.warning('调整分值不能为 0')
    return
  }
  if (!adjustForm.reason.trim()) {
    ElMessage.warning('必须填写调账原因')
    return
  }
  submitting.value = true
  try {
    const r = await adjustCredit({
      subjectType: adjustForm.subjectType,
      subjectId: adjustForm.subjectId,
      subjectName: adjustForm.subjectName,
      delta: adjustForm.delta,
      reason: adjustForm.reason,
      operator: 'web'
    })
    ElMessage.success(`已调整：${r.scoreBefore} → ${r.scoreAfter}（${r.levelAfter} 级）`)
    adjustVisible.value = false
    await Promise.all([load(), loadAllStats()])
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mt { margin-top: 14px; }
.mt8 { margin-top: 8px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.sub { color: #a8abb2; font-size: 12px; }
.ok { color: #0d9488; font-weight: 600; }
.danger { color: #d9363e; font-weight: 600; }
.empty { color: #909399; padding: 24px 0; }

.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { margin-top: 4px; color: #909399; font-size: 12px; }
.tone-ok .stat-value { color: #0d9488; }
.tone-info .stat-value { color: #2f6bff; }
.tone-warn .stat-value { color: #c2751b; }
.tone-danger .stat-value { color: #d9363e; }

.score { font-size: 18px; font-weight: 700; }
.lv-A { color: #0d9488; }
.lv-B { color: #2f6bff; }
.lv-C { color: #c2751b; }
.lv-D { color: #d9363e; }

.drawer-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.big { font-size: 17px; font-weight: 600; }
.score-big { font-size: 34px; font-weight: 700; }
.unit { font-size: 14px; font-weight: 400; margin-left: 2px; }
</style>
