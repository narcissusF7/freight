<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新建合同</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="contractNo" label="合同号" min-width="170" />
      <el-table-column label="类型" width="130">
        <template #default="{ row }">{{ TYPE_TEXT[row.contractType] || row.contractType }}</template>
      </el-table-column>
      <el-table-column label="关联运单" width="100">
        <template #default="{ row }">{{ row.waybillId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="partyA" label="甲方" min-width="150" />
      <el-table-column prop="partyB" label="乙方" min-width="120" />
      <el-table-column label="签署状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.signStatus)">{{ STATUS_TEXT[row.signStatus] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情/验签</el-button>
          <el-button link type="success" :disabled="!!row.signatureA" @click="doSign(row, 'A')">甲方签署</el-button>
          <el-button link type="warning" :disabled="!!row.signatureB" @click="doSign(row, 'B')">乙方签署</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建电子合同" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="合同类型">
          <el-select v-model="form.contractType" style="width: 100%">
            <el-option label="司机运输合同" value="TRANSPORT" />
            <el-option label="托运框架协议" value="FRAMEWORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联运单">
          <el-select v-model="form.waybillId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="w in waybills" :key="w.id" :label="`${w.waybillNo}（${w.cargoName || '-'}）`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="甲方">
          <el-input v-model="form.partyA" placeholder="托运人 / 平台公司" />
        </el-form-item>
        <el-form-item label="乙方">
          <el-input v-model="form.partyB" placeholder="承运司机 / 承运公司" />
        </el-form-item>
        <el-form-item label="合同正文">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="留空则按类型、双方与运单要素自动生成" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 / 验签 -->
    <el-dialog v-model="detailVisible" title="合同详情 / 验签" width="720px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="合同号">{{ current.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.signStatus)">{{ STATUS_TEXT[current.signStatus] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="甲方">{{ current.partyA }}</el-descriptions-item>
          <el-descriptions-item label="乙方">{{ current.partyB }}</el-descriptions-item>
          <el-descriptions-item label="证书序列号" :span="2">{{ current.certSerialNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内容摘要(SHA-256)" :span="2">
            <span class="mono">{{ current.contentHash }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="甲方签名" :span="2">
            <span class="mono">{{ current.signatureA ? current.signatureA.slice(0, 48) + '…' : '未签署' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="乙方签名" :span="2">
            <span class="mono">{{ current.signatureB ? current.signatureB.slice(0, 48) + '…' : '未签署' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="block-title">合同正文</div>
        <pre class="content-box">{{ current.content }}</pre>

        <div class="block-title">验签</div>
        <div class="toolbar">
          <el-button type="primary" plain @click="doVerify(null)">按库内正文验签</el-button>
          <el-button type="warning" plain @click="doVerify('tampered')">模拟正文被篡改后验签</el-button>
        </div>
        <el-alert
          v-if="verifyResult !== null"
          :type="verifyResult ? 'success' : 'error'"
          :closable="false"
          show-icon
          :title="verifyResult ? '验签通过：签名与当前正文一致' : '验签失败：正文与签署时的摘要不一致（内容被改动）'"
          style="margin-top: 8px"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listContracts, createContract, signContract, verifyContract } from '@/api/contract'
import { listWaybills } from '@/api/waybill'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const waybills = ref([])
const TYPE_TEXT = { TRANSPORT: '司机运输合同', FRAMEWORK: '托运框架协议' }
const STATUS_TEXT = { 0: '待签署', 1: '甲方已签', 2: '双方已签', 3: '已作废' }
const statusType = (s) => (s === 2 ? 'success' : s === 1 ? 'warning' : s === 3 ? 'info' : 'primary')

async function load() {
  list.value = await listContracts()
}
async function loadWaybills() {
  waybills.value = await listWaybills()
}

const createVisible = ref(false)
const saving = ref(false)
const form = reactive({ contractType: 'TRANSPORT', waybillId: null, partyA: '', partyB: '', content: '' })
function openCreate() {
  Object.assign(form, {
    contractType: 'TRANSPORT', waybillId: null,
    partyA: '厦门市鹭卡畅通科技有限公司', partyB: '', content: ''
  })
  createVisible.value = true
}
async function handleCreate() {
  saving.value = true
  try {
    await createContract({ ...form, orgId: userStore.userInfo.orgId, operateBy: userStore.userInfo.username })
    ElMessage.success('合同已创建，请双方依次签署')
    createVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doSign(row, party) {
  await signContract(row.id, party, userStore.userInfo.username)
  ElMessage.success(`${party === 'A' ? '甲方' : '乙方'}签署成功`)
  await load()
}

const detailVisible = ref(false)
const current = ref(null)
const verifyResult = ref(null)
function openDetail(row) {
  current.value = row
  verifyResult.value = null
  detailVisible.value = true
}
async function doVerify(mode) {
  const content = mode === 'tampered' ? (current.value.content + '\n（此处被恶意追加了一行）') : null
  verifyResult.value = await verifyContract(current.value.id, content, 'A')
}

onMounted(() => { load(); loadWaybills() })
</script>

<style scoped>
.block-title { font-weight: 600; margin: 16px 0 8px; color: #303133; }
.mono { font-family: Consolas, Monaco, monospace; font-size: 12px; word-break: break-all; }
.content-box {
  background: #f5f7fa; border-radius: 4px; padding: 12px; font-size: 13px;
  white-space: pre-wrap; max-height: 220px; overflow: auto;
}
</style>
