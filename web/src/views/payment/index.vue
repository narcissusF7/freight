<template>
  <div class="page-container">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="load">
        <el-option label="待执行" value="PENDING" />
        <el-option label="支付中" value="PAYING" />
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-select v-model="query.payType" placeholder="全部类型" clearable style="width: 140px" @change="load">
        <el-option label="结算付款" value="SETTLE" />
        <el-option label="提现出金" value="WITHDRAW" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <div class="flex1" />
      <el-date-picker v-model="bankDate" type="date" value-format="YYYY-MM-DD" placeholder="银行文件日期"
        style="width: 160px" />
      <el-button @click="showBankFile">查看银行对账文件</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="payNo" label="支付单号" min-width="195" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.payType === 'SETTLE' ? 'primary' : 'warning'" effect="plain">
            {{ row.payType === 'SETTLE' ? '结算' : '提现' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bizNo" label="业务单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="payeeName" label="收款方" min-width="110" />
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="银行流水号" min-width="195" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ row.bankSerialNo || '-' }}</div>
          <div class="sub">TLS {{ row.tlsProtocol || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="185">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'SUCCESS' && row.status !== 'CLOSED'" link type="warning"
            @click="doExecute(row)">执行</el-button>
          <el-button link type="primary" @click="doQueryBank(row)">反查银行</el-button>
          <el-button v-if="row.status !== 'SUCCESS' && row.status !== 'CLOSED'" link type="danger"
            @click="doClose(row)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="fileVisible" title="银行对账文件（原文）" width="680px">
      <pre class="bank-file">{{ fileContent || '（空）' }}</pre>
      <div class="muted" style="margin-top: 8px">
        每行：支付单号,银行流水号,状态。日终对账会逐单核对这些单号。
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bankFile, closePayment, executePayment, listPayments, queryPaymentBank } from '@/api/finance'

function todayStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const STATUS_TEXT = { PENDING: '待执行', PAYING: '支付中', SUCCESS: '成功', FAILED: '失败', CLOSED: '已关闭' }

const list = ref([])
const query = reactive({ status: '', payType: '' })
const fileVisible = ref(false)
const fileContent = ref('')
const bankDate = ref(todayStr())

onMounted(load)

async function load() {
  list.value = await listPayments({ status: query.status || undefined, payType: query.payType || undefined, limit: 200 })
}

async function doExecute(row) {
  await ElMessageBox.confirm(`确认向银行发起 ${money(row.amount)} 的付款？`, '执行支付', { type: 'warning' })
  try {
    await executePayment(row.payNo)
    ElMessage.success('支付成功')
  } catch (e) {
    // 失败原因由请求层提示
  }
  await load()
}

async function doQueryBank(row) {
  const order = await queryPaymentBank(row.payNo)
  ElMessage.info(`银行侧状态：${order.status}，流水号：${order.bankSerialNo || '-'}`)
  await load()
}

async function doClose(row) {
  const { value } = await ElMessageBox.prompt('关闭原因', '关闭支付单', { inputPattern: /\S+/, inputErrorMessage: '请填写原因' })
  await closePayment(row.payNo, value)
  ElMessage.success('已关闭')
  await load()
}

async function showBankFile() {
  fileContent.value = await bankFile(bankDate.value)
  fileVisible.value = true
}

function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function statusType(s) {
  return { PENDING: 'info', PAYING: 'warning', SUCCESS: 'success', FAILED: 'danger', CLOSED: 'info' }[s] || 'info'
}
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.muted { color: #909399; }
.sub { color: #a8abb2; font-size: 12px; }
.bank-file {
  background: #0f1720;
  color: #d6e2f0;
  padding: 14px;
  border-radius: 8px;
  max-height: 420px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.7;
  margin: 0;
}
</style>
