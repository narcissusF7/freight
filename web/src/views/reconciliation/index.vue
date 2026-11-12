<template>
  <div class="page-container">
    <div class="toolbar">
      <el-date-picker v-model="bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" style="width: 160px" />
      <el-button type="primary" :loading="running" @click="doRun">运行四方对账</el-button>
      <el-button @click="loadLatest">查看最近结果</el-button>
      <div class="flex1" />
      <span class="hint">四方：结算单 / 支付单 / 资金流水 / 钱包余额；另与银行文件核对单号</span>
    </div>

    <template v-if="recon">
      <div class="stat-row">
        <div class="stat">
          <div class="stat-label">结算单（已付）</div>
          <div class="stat-value">{{ money(recon.settlementAmount) }}</div>
          <div class="stat-sub">{{ recon.settlementCount }} 笔</div>
        </div>
        <div class="stat">
          <div class="stat-label">支付单（成功）</div>
          <div class="stat-value">{{ money(recon.paymentAmount) }}</div>
          <div class="stat-sub">{{ recon.paymentCount }} 笔</div>
        </div>
        <div class="stat">
          <div class="stat-label">资金流水（结算入账）</div>
          <div class="stat-value">{{ money(recon.flowAmount) }}</div>
          <div class="stat-sub">{{ recon.flowCount }} 笔</div>
        </div>
        <div class="stat">
          <div class="stat-label">钱包当日净变动</div>
          <div class="stat-value">{{ money(recon.walletDelta) }}</div>
          <div class="stat-sub">银行文件匹配 {{ recon.bankCount }} 笔</div>
        </div>
        <div class="stat" :class="recon.status === 'MATCH' ? 'ok' : 'bad'">
          <div class="stat-label">对账结果</div>
          <div class="stat-value">{{ recon.status === 'MATCH' ? '四方一致' : '有差异' }}</div>
          <div class="stat-sub">{{ recon.diffCount }} 项 / {{ money(recon.diffAmount) }}</div>
        </div>
      </div>

      <el-alert v-if="recon.status === 'MATCH'" type="success" :closable="false" show-icon
        style="margin: 14px 0"
        :title="`${recon.bizDate} 四方对账平衡：结算 ${money(recon.settlementAmount)} = 支付 ${money(recon.paymentAmount)} = 入账 ${money(recon.flowAmount)}`" />
      <el-alert v-else type="error" :closable="false" show-icon style="margin: 14px 0"
        :title="`${recon.bizDate} 检出 ${recon.diffCount} 项差异，合计 ${money(recon.diffAmount)}，请核实后人工处理`" />

      <el-table :data="diffs" border>
        <el-table-column label="差异类型" width="200">
          <template #default="{ row }">
            <el-tag :type="diffType(row.diffType)">{{ DIFF_TEXT[row.diffType] || row.diffType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" min-width="200" show-overflow-tooltip />
        <el-table-column label="应有" width="130" align="right">
          <template #default="{ row }">{{ money(row.expectAmount) }}</template>
        </el-table-column>
        <el-table-column label="实际" width="130" align="right">
          <template #default="{ row }">{{ money(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column label="差额" width="130" align="right">
          <template #default="{ row }">
            <span class="diff">{{ money(row.diffAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="处理" width="180" fixed="right">
          <template #default="{ row }">
            <el-tag v-if="row.handled === 1" type="success" effect="plain">已处理 / {{ row.handleBy }}</el-tag>
            <el-button v-else link type="primary" @click="doHandle(row)">标记已处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!diffs.length" description="没有差异，无需处理" />
    </template>
    <el-empty v-else description="当日还没有对账结果，点击「运行四方对账」" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { handleReconDiff, latestReconciliation, reconDiffs, runReconciliation } from '@/api/finance'

function todayStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const DIFF_TEXT = {
  SETTLE_VS_PAY: '结算 ≠ 支付',
  PAY_VS_FLOW: '支付 ≠ 入账',
  WALLET_BALANCE: '钱包余额 ≠ 流水净额',
  BANK_FILE: '银行文件缺单'
}

const bizDate = ref(todayStr())
const recon = ref(null)
const diffs = ref([])
const running = ref(false)

onMounted(loadLatest)

async function loadLatest() {
  recon.value = await latestReconciliation(bizDate.value)
  diffs.value = recon.value ? await reconDiffs(recon.value.id) : []
}

async function doRun() {
  running.value = true
  try {
    const result = await runReconciliation(bizDate.value)
    recon.value = result.reconciliation
    diffs.value = result.diffs || []
    ElMessage.success(result.reconciliation.status === 'MATCH'
      ? '四方一致，零差异'
      : `检出 ${result.reconciliation.diffCount} 项差异`)
  } finally {
    running.value = false
  }
}

async function doHandle(row) {
  const { value } = await ElMessageBox.prompt('处理说明（例如：已核实为手工调账，已补单）', '标记差异已处理',
    { inputPattern: /\S+/, inputErrorMessage: '请填写处理说明' })
  await handleReconDiff(row.id, value)
  ElMessage.success('已标记')
  await loadLatest()
}

function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function diffType(t) {
  return { SETTLE_VS_PAY: 'warning', PAY_VS_FLOW: 'danger', WALLET_BALANCE: 'danger', BANK_FILE: 'info' }[t] || 'info'
}
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.stat-row { display: flex; gap: 12px; }
.stat {
  flex: 1;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
}
.stat-label { color: #909399; font-size: 12px; }
.stat-value { font-size: 20px; font-weight: 600; margin: 8px 0 4px; }
.stat-sub { color: #909399; font-size: 12px; }
.stat.ok { border-color: #b3e19d; background: #f0f9eb; }
.stat.bad { border-color: #fab6b6; background: #fef0f0; }
.diff { color: #c45656; font-weight: 600; }
</style>
