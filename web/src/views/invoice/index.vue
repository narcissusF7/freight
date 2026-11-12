<template>
  <div class="page-container">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="load">
        <el-option label="已开具" value="ISSUED" />
        <el-option label="已作废" value="VOID" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <div class="flex1" />
      <span class="hint">开票入口在「结算管理」中按结算单发起；金额与税额取自结算单，不可改</span>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="invoiceNo" label="发票号码" min-width="200" show-overflow-tooltip />
      <el-table-column label="结算单 / 运单" min-width="215">
        <template #default="{ row }">
          <div>{{ row.settlementNo }}</div>
          <div class="sub">{{ row.waybillNo }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="抬头" min-width="180" show-overflow-tooltip />
      <el-table-column label="含税金额" width="130" align="right">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="不含税 / 税额" width="180" align="right">
        <template #default="{ row }">
          <span class="muted">{{ money(row.netAmount) }}</span>
          <span class="tax"> + {{ money(row.taxAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ISSUED' ? 'success' : 'danger'">
            {{ row.status === 'ISSUED' ? '已开具' : '已作废' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="issuedTime" label="开具时间" width="170" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ISSUED'" link type="danger" @click="doVoid(row)">红冲</el-button>
          <span v-else class="muted">{{ row.voidReason || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInvoices, voidInvoice } from '@/api/finance'

const list = ref([])
const query = reactive({ status: '' })

onMounted(load)

async function load() {
  list.value = await listInvoices({ status: query.status || undefined, limit: 200 })
}

async function doVoid(row) {
  const { value } = await ElMessageBox.prompt(
    '红冲原因（真实环境会调用税控红字发票接口，当前为本地作废留痕）', '发票红冲',
    { inputPattern: /\S+/, inputErrorMessage: '请填写原因' }
  )
  await voidInvoice(row.id, value)
  ElMessage.success('已作废')
  await load()
}

function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.flex1 { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.muted { color: #909399; }
.sub { color: #a8abb2; font-size: 12px; }
.tax { color: #b88230; }
</style>
