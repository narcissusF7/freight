<template>
  <div class="page-container">
    <div class="toolbar">
      <el-radio-group v-model="tab">
        <el-radio-button label="wallet">司机 / 货主钱包</el-radio-button>
        <el-radio-button label="withdraw">提现审核</el-radio-button>
      </el-radio-group>
      <el-button @click="load">刷新</el-button>
      <div class="flex1" />
      <el-button v-if="tab === 'withdraw'" type="primary" @click="openApply">发起提现</el-button>
    </div>

    <!-- 钱包 -->
    <el-table v-if="tab === 'wallet'" :data="wallets" border>
      <el-table-column prop="ownerName" label="账户名" min-width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ OWNER_TEXT[row.ownerType] || row.ownerType }}</template>
      </el-table-column>
      <el-table-column label="总余额" width="140" align="right">
        <template #default="{ row }"><b class="amount">{{ money(row.balance) }}</b></template>
      </el-table-column>
      <el-table-column label="冻结" width="120" align="right">
        <template #default="{ row }">{{ money(row.frozen) }}</template>
      </el-table-column>
      <el-table-column label="可用" width="140" align="right">
        <template #default="{ row }">
          {{ money(Number(row.balance) - Number(row.frozen)) }}
        </template>
      </el-table-column>
      <el-table-column label="累计入/出" width="220" align="right">
        <template #default="{ row }">
          <span class="in">+{{ money(row.totalIn) }}</span>
          <span class="out" style="margin-left: 8px">-{{ money(row.totalOut) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openFlows(row)">资金流水</el-button>
          <el-button link type="warning" @click="openApply(row)">提现</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 提现 -->
    <el-table v-else :data="withdrawals" border>
      <el-table-column prop="withdrawNo" label="提现单号" min-width="200" />
      <el-table-column prop="ownerName" label="申请人" min-width="120" />
      <el-table-column label="申请金额" width="130" align="right">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="手续费" width="100" align="right">
        <template #default="{ row }">{{ money(row.fee) }}</template>
      </el-table-column>
      <el-table-column prop="bankAccount" label="收款账号" min-width="160" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="wdType(row.status)">{{ WD_TEXT[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button v-if="row.status === 'APPLIED'" link type="success" @click="doAudit(row, true)">通过并付款</el-button>
          <el-button v-if="row.status === 'APPLIED'" link type="danger" @click="doAudit(row, false)">驳回</el-button>
          <el-button v-if="row.status === 'APPROVED'" link type="warning" @click="doRetry(row)">重试付款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 流水 -->
    <el-drawer v-model="flowVisible" :title="`资金流水 · ${currentWallet ? currentWallet.ownerName : ''}`" size="760px">
      <el-table :data="flows" border size="small">
        <el-table-column prop="flowNo" label="流水号" min-width="190" />
        <el-table-column label="业务类型" width="130">
          <template #default="{ row }">{{ FLOW_TEXT[row.bizType] || row.bizType }}</template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" min-width="185" />
        <el-table-column label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'warning'" effect="plain">
              {{ row.direction === 'IN' ? '入账' : '出账' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.direction === 'IN' ? 'in' : 'out'">
              {{ row.direction === 'IN' ? '+' : '-' }}{{ money(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="记账后余额" width="130" align="right">
          <template #default="{ row }">{{ money(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="!flows.length" description="暂无流水" />
    </el-drawer>

    <!-- 提现申请 -->
    <el-dialog v-model="applyVisible" title="发起提现" width="480px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="账户">
          <span>{{ applyForm.ownerName }}（可用 {{ money(available) }}）</span>
        </el-form-item>
        <el-form-item label="提现金额">
          <el-input-number v-model="applyForm.amount" :min="1" :max="200000" :precision="2" style="width: 180px" />
        </el-form-item>
        <el-form-item label="收款账号">
          <el-input v-model="applyForm.bankAccount" placeholder="银行卡号（演示可填 FAIL 触发银行失败）" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="applyForm.bankName" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyWithdraw, auditWithdraw, listWallets, listWithdrawals, retryWithdraw, walletFlows
} from '@/api/finance'

const OWNER_TEXT = { DRIVER: '司机', SHIPPER: '货主', CARRIER: '承运商' }
const WD_TEXT = { APPLIED: '待审核', APPROVED: '已通过待付款', REJECTED: '已驳回', PAID: '已到账' }
const FLOW_TEXT = { SETTLE_IN: '结算入账', WITHDRAW_OUT: '提现出金', ADJUST: '人工调账' }

const tab = ref('wallet')
const wallets = ref([])
const withdrawals = ref([])
const flows = ref([])
const currentWallet = ref(null)
const flowVisible = ref(false)
const applyVisible = ref(false)
const saving = ref(false)

const applyForm = reactive({ ownerType: 'DRIVER', ownerId: null, ownerName: '', amount: 100, bankAccount: '', bankName: '', operator: 'web' })
const available = computed(() => {
  const w = wallets.value.find((x) => x.ownerType === applyForm.ownerType && x.ownerId === applyForm.ownerId)
  return w ? Number(w.balance) - Number(w.frozen) : 0
})

onMounted(load)

async function load() {
  if (tab.value === 'wallet') {
    wallets.value = await listWallets()
  } else {
    withdrawals.value = await listWithdrawals({ limit: 200 })
  }
}

async function openFlows(row) {
  currentWallet.value = row
  flows.value = await walletFlows(row.ownerType, row.ownerId, 200)
  flowVisible.value = true
}

function openApply(row) {
  const w = row || wallets.value[0]
  if (!w) {
    ElMessage.warning('没有可提现的账户')
    return
  }
  applyForm.ownerType = w.ownerType
  applyForm.ownerId = w.ownerId
  applyForm.ownerName = w.ownerName
  applyForm.amount = 100
  applyForm.bankAccount = ''
  applyForm.bankName = ''
  applyVisible.value = true
}

async function doApply() {
  if (!applyForm.bankAccount) {
    ElMessage.warning('请填写收款账号')
    return
  }
  saving.value = true
  try {
    await applyWithdraw({ ...applyForm })
    ElMessage.success('已提交，等待审核（金额已冻结）')
    applyVisible.value = false
    tab.value = 'withdraw'
    await load()
  } finally {
    saving.value = false
  }
}

async function doAudit(row, pass) {
  if (pass) {
    await ElMessageBox.confirm(`通过后将立即向银行发起 ${money(row.amount)} 付款，确认？`, '审核通过', { type: 'warning' })
  } else {
    const { value } = await ElMessageBox.prompt('驳回原因', '驳回提现', { inputPattern: /\S+/, inputErrorMessage: '请填写原因' })
    await auditWithdraw(row.id, false, value)
    ElMessage.success('已驳回，冻结金额已释放')
    await load()
    return
  }
  try {
    await auditWithdraw(row.id, true, '审核通过')
    ElMessage.success('已付款到账')
  } catch (e) {
    // 银行失败：钱不会少，单据停在“已通过待付款”，可重试
    ElMessage.warning('银行付款失败，单据已保留，可稍后重试')
  }
  await load()
}

async function doRetry(row) {
  try {
    await retryWithdraw(row.id)
    ElMessage.success('已到账')
  } catch (e) {
    // 已提示
  }
  await load()
}

function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function wdType(s) {
  return { APPLIED: 'warning', APPROVED: 'warning', REJECTED: 'danger', PAID: 'success' }[s] || 'info'
}
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.amount { color: #c45656; font-size: 16px; }
.in { color: #2f9e44; }
.out { color: #c45656; }
</style>
