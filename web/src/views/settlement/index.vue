<template>
  <div class="page-container">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="load">
        <el-option v-for="(t, k) in STATUS_TEXT" :key="k" :label="t" :value="k" />
      </el-select>
      <el-select v-model="query.bizType" placeholder="全部类型" clearable style="width: 140px" @change="load">
        <el-option label="应付司机" value="PAYABLE" />
        <el-option label="应收货主" value="RECEIVABLE" />
      </el-select>
      <el-button type="primary" @click="openGenerate">按运单生成结算单</el-button>
      <el-button @click="load">刷新</el-button>
      <div class="flex1" />
      <span class="hint">金额与税额由服务端按运单运价与税率计算，前端不可改</span>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="settlementNo" label="结算单号" min-width="195" show-overflow-tooltip />
      <el-table-column label="结算对象 / 运单" min-width="215">
        <template #default="{ row }">
          <div>{{ row.partnerName || '-' }}</div>
          <div class="sub">{{ row.waybillNo }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.bizType === 'PAYABLE' ? 'warning' : 'success'" effect="plain">
            {{ row.bizType === 'PAYABLE' ? '应付' : '应收' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="含税金额" width="115" align="right">
        <template #default="{ row }"><b>{{ money(row.amount) }}</b></template>
      </el-table-column>
      <el-table-column label="不含税 / 税额" width="170" align="right">
        <template #default="{ row }">
          <span class="muted">{{ money(row.netAmount) }}</span>
          <span class="tax"> + {{ money(row.taxAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="settledDate" label="结算日" width="105" />
      <el-table-column label="操作" width="205">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="doConfirm(row)">确认</el-button>
          <el-button v-if="row.status === 'CONFIRMED'" link type="warning" @click="doPay(row)">付款</el-button>
          <el-button v-if="canInvoice(row)" link type="primary" @click="openInvoice(row)">开票</el-button>
          <el-button v-if="canVoid(row)" link type="danger" @click="doVoid(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 生成 -->
    <el-dialog v-model="genVisible" title="按运单生成结算单" width="560px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px"
        title="基础运费取自运单约定运价；只有附加费与扣款可调整。运单需已完成签收并上传回单。" />
      <el-form :model="genForm" label-width="110px">
        <el-form-item label="运单">
          <el-select v-model="genForm.waybillId" filterable placeholder="选择运单（可搜索卡号）" style="width: 100%">
            <el-option v-for="w in waybills" :key="w.id" :value="w.id"
              :label="`${w.waybillNo}｜${w.status}｜${money(w.freightAmount)}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="结算类型">
          <el-radio-group v-model="genForm.bizType">
            <el-radio label="PAYABLE">应付司机</el-radio>
            <el-radio label="RECEIVABLE">应收货主</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="装货费">
          <el-input-number v-model="genForm.loadFee" :min="0" :precision="2" :step="50" style="width: 180px" />
        </el-form-item>
        <el-form-item label="卸货费">
          <el-input-number v-model="genForm.unloadFee" :min="0" :precision="2" :step="50" style="width: 180px" />
        </el-form-item>
        <el-form-item label="等待费">
          <el-input-number v-model="genForm.waitFee" :min="0" :precision="2" :step="50" style="width: 180px" />
        </el-form-item>
        <el-form-item label="油补">
          <el-input-number v-model="genForm.oilSubsidy" :min="0" :precision="2" :step="50" style="width: 180px" />
        </el-form-item>
        <el-form-item label="扣款">
          <el-input-number v-model="genForm.deductAmount" :min="0" :precision="2" :step="50" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="genForm.remark" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 明细 -->
    <el-dialog v-model="detailVisible" title="结算单明细" width="640px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算单号">{{ current.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ current.waybillNo }}</el-descriptions-item>
          <el-descriptions-item label="结算对象">{{ current.partnerType }} / {{ current.partnerName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)">{{ STATUS_TEXT[current.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="基础运费">{{ money(current.baseAmount) }}</el-descriptions-item>
          <el-descriptions-item label="装/卸/等待/油补">
            {{ money(current.loadFee) }} / {{ money(current.unloadFee) }} /
            {{ money(current.waitFee) }} / {{ money(current.oilSubsidy) }}
          </el-descriptions-item>
          <el-descriptions-item label="扣款">{{ money(current.deductAmount) }}</el-descriptions-item>
          <el-descriptions-item label="税率">{{ (Number(current.taxRate) * 100).toFixed(2) }}%</el-descriptions-item>
          <el-descriptions-item label="含税金额">
            <b class="amount">{{ money(current.amount) }}</b>
          </el-descriptions-item>
          <el-descriptions-item label="不含税 / 税额">
            {{ money(current.netAmount) }} / {{ money(current.taxAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建">{{ current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ current.paidTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 开票 -->
    <el-dialog v-model="invoiceVisible" title="开具电子发票" width="480px">
      <el-form :model="invoiceForm" label-width="100px">
        <el-form-item label="发票抬头">
          <el-input v-model="invoiceForm.title" placeholder="不填则按结算对象生成" />
        </el-form-item>
        <el-form-item label="纳税人识别号">
          <el-input v-model="invoiceForm.taxNo" placeholder="选填" />
        </el-form-item>
        <el-form-item label="金额">
          <span class="amount">{{ current ? money(current.amount) : '-' }}</span>
          <span class="muted" style="margin-left: 10px">（含税，取结算单，不可修改）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doInvoice">开具</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  confirmSettlement, generateSettlement, issueInvoice, listSettlements, paySettlement, voidSettlement
} from '@/api/finance'
import { listWaybills } from '@/api/waybill'

const STATUS_TEXT = {
  DRAFT: '待确认',
  CONFIRMED: '已确认',
  PAYING: '支付中',
  PAID: '已支付',
  INVOICED: '已开票',
  CLOSED: '已关闭',
  VOID: '已作废'
}

const list = ref([])
const waybills = ref([])
const current = ref(null)
const saving = ref(false)
const query = reactive({ status: '', bizType: '' })

const genVisible = ref(false)
const detailVisible = ref(false)
const invoiceVisible = ref(false)
const genForm = reactive({
  waybillId: null, bizType: 'PAYABLE', loadFee: 0, unloadFee: 0,
  waitFee: 0, oilSubsidy: 0, deductAmount: 0, remark: '', operator: 'web'
})
const invoiceForm = reactive({ title: '', taxNo: '' })

onMounted(load)

async function load() {
  list.value = await listSettlements({ status: query.status || undefined, bizType: query.bizType || undefined, limit: 200 })
}

async function openGenerate() {
  genVisible.value = true
  waybills.value = await listWaybills()
}

async function doGenerate() {
  if (!genForm.waybillId) {
    ElMessage.warning('请选择运单')
    return
  }
  saving.value = true
  try {
    const st = await generateSettlement({ ...genForm })
    ElMessage.success(`已生成结算单 ${st.settlementNo}，金额 ${money(st.amount)}`)
    genVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doConfirm(row) {
  await ElMessageBox.confirm(`确认结算单 ${row.settlementNo}（${money(row.amount)}）？确认后才能付款。`, '确认结算')
  await confirmSettlement(row.id)
  ElMessage.success('已确认')
  await load()
}

async function doPay(row) {
  await ElMessageBox.confirm(
    `向 ${row.partnerName || '承运司机'} 支付 ${money(row.amount)}？\n金额将直接入司机钱包，请核对无误。`,
    '发起付款', { type: 'warning' }
  )
  try {
    const r = await paySettlement(row.id)
    ElMessage.success(r.message || '支付成功')
  } catch (e) {
    // 失败提示由请求层统一弹出，这里刷新即可看到状态回退
  }
  await load()
}

async function doVoid(row) {
  const { value } = await ElMessageBox.prompt('作废原因', '作废结算单', { inputPattern: /\S+/, inputErrorMessage: '请填写作废原因' })
  await voidSettlement(row.id, value)
  ElMessage.success('已作废')
  await load()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function openInvoice(row) {
  current.value = row
  invoiceForm.title = ''
  invoiceForm.taxNo = ''
  invoiceVisible.value = true
}

async function doInvoice() {
  saving.value = true
  try {
    const inv = await issueInvoice({
      settlementId: current.value.id,
      title: invoiceForm.title || undefined,
      taxNo: invoiceForm.taxNo || undefined,
      operator: 'web'
    })
    ElMessage.success(`开票成功：${inv.invoiceNo}`)
    invoiceVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function canVoid(row) {
  return ['DRAFT', 'CONFIRMED'].includes(row.status)
}
function canInvoice(row) {
  return ['CONFIRMED', 'PAID'].includes(row.status)
}
function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function statusType(s) {
  return { DRAFT: 'info', CONFIRMED: 'warning', PAYING: 'warning', PAID: 'success', INVOICED: 'success', VOID: 'danger', CLOSED: 'info' }[s] || 'info'
}
</script>

<style scoped>
:deep(.el-table .cell) { white-space: nowrap; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.amount { color: #c45656; font-size: 16px; }
.muted { color: #909399; }
.sub { color: #a8abb2; font-size: 12px; }
.tax { color: #b88230; }
</style>
