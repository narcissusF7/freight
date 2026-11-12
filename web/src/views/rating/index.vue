<template>
  <div class="page-container">
    <el-row :gutter="12">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">筛选结果条数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card tone-warn">
          <div class="stat-value">{{ avgScore }}</div>
          <div class="stat-label">平均星级</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card tone-ok">
          <div class="stat-value">{{ goodRate }}</div>
          <div class="stat-label">好评率（≥4 星）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card tone-danger">
          <div class="stat-value">{{ badCount }}</div>
          <div class="stat-label">差评数（≤2 星）</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt">
      <div class="toolbar">
        <el-select v-model="query.direction" placeholder="全部方向" clearable style="width: 190px" @change="load">
          <el-option label="货主评司机" value="SHIPPER_TO_DRIVER" />
          <el-option label="司机评货主" value="DRIVER_TO_SHIPPER" />
        </el-select>
        <el-select v-model="query.scoreBelow" placeholder="全部星级" clearable style="width: 140px" @change="load">
          <el-option label="≤2 星（差评）" :value="2" />
          <el-option label="≤3 星" :value="3" />
          <el-option label="4 星及以下" :value="4" />
        </el-select>
        <el-button type="primary" plain @click="load">查询</el-button>
        <div class="flex1" />
        <span class="hint">评价资格：运单已签收；同一运单每个方向只能评一次（唯一索引兜底）</span>
      </div>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="评价单号 / 运单 / 时间" min-width="215">
          <template #default="{ row }">
            <div class="mono small">{{ row.ratingNo }}</div>
            <div class="sub">{{ row.waybillNo }}</div>
            <div class="sub">{{ fmt(row.createTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="100">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'SHIPPER_TO_DRIVER' ? 'primary' : 'success'" effect="plain" size="small">
              {{ row.direction === 'SHIPPER_TO_DRIVER' ? '货主→司机' : '司机→货主' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价人 → 被评价" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="truncate">{{ row.anonymous ? '匿名货主' : row.raterName || '-' }}</div>
            <div class="truncate">→ {{ row.targetName }}</div>
            <div class="sub">{{ row.tags || '无标签' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="星级 / 评价内容" min-width="230">
          <template #default="{ row }">
            <div class="row-rate">
              <el-rate :model-value="row.score" disabled size="small" />
              <span class="sub">{{ row.score }} 分</span>
              <el-tag v-if="row.status === 'HIDDEN'" type="info" size="small">已隐藏</el-tag>
            </div>
            <el-tooltip :content="row.content || '未填写文字'" placement="top">
              <div class="content-line">{{ row.content || '—' }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="125" class-name="op-col">
          <template #default="{ row }">
            <el-button v-if="row.status === 'VALID' && !row.replyContent" link type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.status === 'VALID'" link type="danger" @click="doHide(row)">隐藏</el-button>
            <span v-if="row.replyContent" class="sub">已回复</span>
            <span v-if="row.status === 'HIDDEN'" class="sub">已隐藏</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="replyVisible" title="回复评价" width="480px">
      <el-form label-width="80px">
        <el-form-item label="评价内容">
          <div class="quote">{{ current.content || '（未填写文字）' }}</div>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="被评价方的解释或说明，不改变评分" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hideRating, listRatings, replyRating } from '@/api/customer'

const list = ref([])
const loading = ref(false)
const query = reactive({ direction: '', scoreBelow: '' })

const replyVisible = ref(false)
const replyContent = ref('')
const current = ref({})
const submitting = ref(false)

const avgScore = computed(() => {
  if (!list.value.length) return '—'
  const sum = list.value.reduce((a, r) => a + Number(r.score || 0), 0)
  return (sum / list.value.length).toFixed(1)
})
const goodRate = computed(() => {
  if (!list.value.length) return '—'
  const good = list.value.filter((r) => r.score >= 4).length
  return ((good / list.value.length) * 100).toFixed(0) + '%'
})
const badCount = computed(() => list.value.filter((r) => r.score <= 2).length)

onMounted(load)

async function load() {
  loading.value = true
  try {
    list.value = await listRatings({
      direction: query.direction || undefined,
      scoreBelow: query.scoreBelow || undefined,
      limit: 200
    })
  } finally {
    loading.value = false
  }
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

function openReply(row) {
  current.value = row
  replyContent.value = ''
  replyVisible.value = true
}

async function doReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  submitting.value = true
  try {
    await replyRating(current.value.id, replyContent.value)
    ElMessage.success('已回复')
    replyVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function doHide(row) {
  const { value } = await ElMessageBox.prompt(
    '隐藏后前台不再展示，但记录仍保留（评价是纠纷证据，不做物理删除）',
    '隐藏评价',
    { inputPlaceholder: '隐藏原因', inputPattern: /\S+/, inputErrorMessage: '请填写原因' }
  )
  await hideRating(row.id, value)
  ElMessage.success('已隐藏')
  await load()
}
</script>

<style scoped>
.mt { margin-top: 14px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.sub { color: #a8abb2; font-size: 12px; }
.mono { font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 12px; }
.small { font-size: 12px; }
.truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-rate { display: flex; align-items: center; gap: 8px; }
.content-line { margin-top: 4px; color: #5c6470; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
:deep(.op-col .cell) { white-space: nowrap; overflow: visible; }
.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { margin-top: 4px; color: #909399; font-size: 12px; }
.tone-warn .stat-value { color: #c2751b; }
.tone-ok .stat-value { color: #0d9488; }
.tone-danger .stat-value { color: #d9363e; }
.quote { color: #5c6470; background: #f7f8fa; border-radius: 6px; padding: 8px 10px; width: 100%; }
</style>
