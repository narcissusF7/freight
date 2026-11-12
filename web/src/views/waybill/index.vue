<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">生成运单</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="waybillNo" label="运单号" min-width="160" />
      <el-table-column label="司机" min-width="100">
        <template #default="{ row }">{{ driverName(row.driverId) }}</template>
      </el-table-column>
      <el-table-column label="车辆" min-width="110">
        <template #default="{ row }">{{ vehiclePlate(row.vehicleId) }}</template>
      </el-table-column>
      <el-table-column prop="cargoName" label="货物" min-width="120" />
      <el-table-column prop="weight" label="重量(吨)" min-width="100" />
      <el-table-column label="状态" min-width="130">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上报" width="90">
        <template #default="{ row }">
          <el-tag :type="row.reportStatus === 1 ? 'success' : row.reportStatus === 2 ? 'danger' : 'info'">
            {{ row.reportStatus === 1 ? '已报' : row.reportStatus === 2 ? '失败' : '未报' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情/流转</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 生成运单（含智能匹配） -->
    <el-dialog v-model="createVisible" title="生成运单" width="760px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="货源" prop="goodsSourceId">
          <el-select v-model="createForm.goodsSourceId" placeholder="选择待承运货源" style="width: 100%">
            <el-option
              v-for="g in pendingSources"
              :key="g.id"
              :label="`${g.goodsName} (${g.loadAddr}→${g.unloadAddr} / ${g.weight}吨 / ¥${g.freightPrice})`"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="智能匹配">
          <el-button :loading="matching" :disabled="!createForm.goodsSourceId" @click="doMatch">
            按载重/证照/线路/运价推荐车辆
          </el-button>
          <span class="muted" style="margin-left: 8px">匹配结果为调度建议，确认后才生成运单</span>
        </el-form-item>
      </el-form>

      <el-table v-if="matches.length" :data="matches" border size="small" style="margin-bottom: 12px">
        <el-table-column label="评分" width="80">
          <template #default="{ row }"><b>{{ row.score }}</b></template>
        </el-table-column>
        <el-table-column label="司机" min-width="100">
          <template #default="{ row }">{{ row.driverName }}</template>
        </el-table-column>
        <el-table-column label="车辆" min-width="110">
          <template #default="{ row }">{{ row.plateNo }}</template>
        </el-table-column>
        <el-table-column label="载重(吨)" width="90">
          <template #default="{ row }">{{ row.capacityTon }}</template>
        </el-table-column>
        <el-table-column label="评分明细" min-width="200">
          <template #default="{ row }">
            <span class="muted">载重{{ row.capacityScore }} / 证照{{ row.certScore }} / 线路{{ row.experienceScore }} / 运价{{ row.priceScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提示" min-width="220">
          <template #default="{ row }">
            <div v-for="(r, i) in row.reasons" :key="i" class="muted">{{ r }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.capacityFits" @click="applyMatch(row)">选用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form :model="createForm" label-width="90px">
        <el-form-item label="司机" prop="driverId">
          <el-select v-model="createForm.driverId" placeholder="选择司机" style="width: 100%">
            <el-option v-for="d in drivers" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车辆" prop="vehicleId">
          <el-select v-model="createForm.vehicleId" placeholder="选择车辆" style="width: 100%">
            <el-option v-for="v in vehicles" :key="v.id" :label="`${v.plateNo}（${v.capacityTon}吨）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="货物名称">
          <el-input v-model="createForm.cargoName" placeholder="留空则沿用货源名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">生成（草稿）</el-button>
      </template>
    </el-dialog>

    <!-- 详情：流转 / 异常件 / 回单 -->
    <el-dialog v-model="detailVisible" title="运单详情" width="920px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="运单号">{{ current.waybillNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)">{{ STATUS_TEXT[current.status] || current.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="司机">{{ driverName(current.driverId) }}</el-descriptions-item>
          <el-descriptions-item label="车辆">{{ vehiclePlate(current.vehicleId) }}</el-descriptions-item>
          <el-descriptions-item label="装货地">
            {{ current.loadAddr }}
            <span v-if="current.loadLat" class="muted">
              （{{ Number(current.loadLat).toFixed(5) }}, {{ Number(current.loadLng).toFixed(5) }}）
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="卸货地">
            {{ current.unloadAddr }}
            <span v-if="current.unloadLat" class="muted">
              （{{ Number(current.unloadLat).toFixed(5) }}, {{ Number(current.unloadLng).toFixed(5) }}）
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="重量(吨)">{{ current.weight }}</el-descriptions-item>
          <el-descriptions-item label="上报状态">
            {{ current.reportStatus === 1 ? '已上报' : current.reportStatus === 2 ? '失败' : '未上报' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab" style="margin-top: 12px">
          <el-tab-pane label="状态流转" name="flow">
            <div class="flow-buttons">
              <el-button
                v-for="t in nextStatuses(current.status)"
                :key="t"
                type="primary"
                plain
                @click="doChange(t)"
              >{{ STATUS_TEXT[t] }}</el-button>
              <span v-if="nextStatuses(current.status).length === 0" class="muted">当前为终态，无可流转状态</span>
            </div>
            <el-timeline style="margin-top: 16px">
              <el-timeline-item
                v-for="n in nodes"
                :key="n.id"
                :timestamp="n.operateTime"
                :type="n.toStatus === 'EXCEPTION' ? 'warning' : 'primary'"
              >
                {{ STATUS_TEXT[n.fromStatus] || '—' }} → <b>{{ STATUS_TEXT[n.toStatus] || n.toStatus }}</b>
                <span class="muted">（{{ n.operateBy }}）</span>
                <div v-if="n.remark" class="muted">{{ n.remark }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>

          <el-tab-pane :label="`异常件(${exceptions.length})`" name="exception">
            <div class="toolbar">
              <el-button type="warning" plain @click="openException">上报异常</el-button>
            </div>
            <el-table :data="exceptions" border size="small">
              <el-table-column prop="exceptionType" label="类型" width="140" />
              <el-table-column prop="description" label="描述" min-width="180" />
              <el-table-column prop="reportBy" label="上报人" width="100" />
              <el-table-column prop="reportTime" label="上报时间" min-width="150" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '已处理' : '待处理' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
                  <el-button v-if="row.status !== 1" link type="primary" @click="doHandle(row)">处理</el-button>
                  <span v-else class="muted">{{ row.handleRemark || '—' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`回单(${receipts.length})`" name="receipt">
            <div class="toolbar">
              <el-upload :show-file-list="false" :before-upload="doUploadReceipt">
                <el-button type="primary">上传回单</el-button>
              </el-upload>
              <span class="muted" style="margin-left: 8px">运单处于「已签收」时，上传回单会自动推进到「回单已上传」</span>
            </div>
            <el-table :data="receipts" border size="small">
              <el-table-column prop="fileName" label="文件名" min-width="180" />
              <el-table-column prop="uploadBy" label="上传人" width="100" />
              <el-table-column prop="uploadTime" label="上传时间" min-width="150" />
              <el-table-column label="核验" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.verified === 1 ? 'success' : 'info'">{{ row.verified === 1 ? '已核验' : '待核验' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openFile(row.fileUrl)">查看</el-button>
                  <el-button v-if="row.verified !== 1" link type="success" @click="doVerifyReceipt(row)">核验</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="轨迹回放" name="track">
            <TrackPlayback
              :active="activeTab === 'track'"
              :waybill-id="current.id"
              :load-lat="current.loadLat"
              :load-lng="current.loadLng"
              :unload-lat="current.unloadLat"
              :unload-lng="current.unloadLng"
            />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <!-- 异常上报 -->
    <el-dialog v-model="exceptionVisible" title="上报异常" width="480px">
      <el-form :model="exceptionForm" label-width="90px">
        <el-form-item label="异常类型">
          <el-select v-model="exceptionForm.exceptionType" style="width: 100%">
            <el-option v-for="(text, key) in EXCEPTION_TEXT" :key="key" :label="text" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="exceptionForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="exceptionForm.imageUrls" placeholder="多个用英文逗号分隔（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exceptionVisible = false">取消</el-button>
        <el-button type="primary" @click="submitException">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWaybills, getWaybill, getWaybillNodes, createWaybill, changeWaybillStatus, listGoodsSources,
  matchCarriers, listExceptions, reportException, handleException,
  listReceipts, uploadReceipt, verifyReceipt
} from '@/api/waybill'
import { listDrivers, listVehicles } from '@/api/carrier'
import TrackPlayback from '@/components/TrackPlayback.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const drivers = ref([])
const vehicles = ref([])
const sources = ref([])

const STATUS_TEXT = {
  DRAFT: '草稿', PUBLISHED: '已发布(待接单)', ACCEPTED: '已接单', LOADED: '已装货', IN_TRANSIT: '在途',
  UNLOADED: '已卸货', SIGNED: '已签收', RECEIPT_UPLOADED: '回单已上传', SETTLED: '已结算',
  REPORTED: '已上报', COMPLETED: '已完成', EXCEPTION: '异常', WITHDRAWN: '已撤回', VOID: '已作废'
}
const EXCEPTION_TEXT = {
  CARGO_DAMAGE: '货损', DELAY: '延误', ACCIDENT: '事故', ADDRESS: '地址异常', OTHER: '其他'
}
const TRANSITIONS = {
  DRAFT: ['PUBLISHED', 'WITHDRAWN', 'VOID'],
  PUBLISHED: ['ACCEPTED', 'WITHDRAWN', 'VOID'],
  ACCEPTED: ['LOADED', 'EXCEPTION', 'VOID'],
  LOADED: ['IN_TRANSIT', 'EXCEPTION'],
  IN_TRANSIT: ['UNLOADED', 'EXCEPTION'],
  UNLOADED: ['SIGNED', 'EXCEPTION'],
  SIGNED: ['RECEIPT_UPLOADED', 'EXCEPTION'],
  RECEIPT_UPLOADED: ['SETTLED'],
  SETTLED: ['REPORTED'],
  REPORTED: ['COMPLETED'],
  EXCEPTION: ['ACCEPTED', 'LOADED', 'IN_TRANSIT', 'UNLOADED', 'SIGNED', 'VOID'],
  WITHDRAWN: [], VOID: [], COMPLETED: []
}
const statusType = (s) => {
  if (s === 'COMPLETED') return 'success'
  if (s === 'EXCEPTION') return 'warning'
  if (s === 'VOID' || s === 'WITHDRAWN') return 'info'
  return 'primary'
}

const driverName = (id) => { const d = drivers.value.find((x) => x.id === id); return d ? d.name : '-' }
const vehiclePlate = (id) => { const v = vehicles.value.find((x) => x.id === id); return v ? v.plateNo : '-' }
const pendingSources = computed(() => sources.value.filter((s) => s.status === 0))
const nextStatuses = (s) => TRANSITIONS[s] || []

async function load() {
  list.value = await listWaybills()
}
async function loadMeta() {
  drivers.value = await listDrivers()
  vehicles.value = await listVehicles()
  sources.value = await listGoodsSources()
}

// ---------- 生成运单 + 智能匹配 ----------
const createVisible = ref(false)
const saving = ref(false)
const matching = ref(false)
const matches = ref([])
const createRef = ref()
const createForm = reactive({ goodsSourceId: null, driverId: null, vehicleId: null, cargoName: '' })
const createRules = {
  goodsSourceId: [{ required: true, message: '请选择货源', trigger: 'change' }],
  driverId: [{ required: true, message: '请选择司机', trigger: 'change' }],
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }]
}
function openCreate() {
  Object.assign(createForm, { goodsSourceId: null, driverId: null, vehicleId: null, cargoName: '' })
  matches.value = []
  createVisible.value = true
}
async function doMatch() {
  matching.value = true
  try {
    matches.value = await matchCarriers(createForm.goodsSourceId, 5)
    if (!matches.value.length) ElMessage.warning('暂无已审核通过且绑定司机的车辆可推荐')
  } finally {
    matching.value = false
  }
}
function applyMatch(row) {
  createForm.driverId = row.driverId
  createForm.vehicleId = row.vehicleId
  ElMessage.success(`已选用 ${row.driverName} / ${row.plateNo}（评分 ${row.score}）`)
}
async function handleCreate() {
  await createRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await createWaybill({
        orgId: userStore.userInfo.orgId,
        goodsSourceId: createForm.goodsSourceId,
        driverId: createForm.driverId,
        vehicleId: createForm.vehicleId,
        cargoName: createForm.cargoName || null,
        operateBy: userStore.userInfo.username
      })
      ElMessage.success('运单已生成（草稿）')
      createVisible.value = false
      await load(); await loadMeta()
    } finally {
      saving.value = false
    }
  })
}

// ---------- 详情 ----------
const detailVisible = ref(false)
const activeTab = ref('flow')
const current = ref(null)
const nodes = ref([])
const exceptions = ref([])
const receipts = ref([])

async function openDetail(id) {
  current.value = await getWaybill(id)
  await Promise.all([refreshNodes(id), refreshExceptions(id), refreshReceipts(id)])
  activeTab.value = 'flow'
  detailVisible.value = true
}
const refreshNodes = async (id) => { nodes.value = await getWaybillNodes(id) }
const refreshExceptions = async (id) => { exceptions.value = await listExceptions(id) }
const refreshReceipts = async (id) => { receipts.value = await listReceipts(id) }

async function doChange(toStatus) {
  const r = await ElMessageBox.prompt(`流转至「${STATUS_TEXT[toStatus]}」，可填备注（留空不填）`, '状态流转', {
    inputType: 'textarea', required: false
  }).catch(() => null)
  if (!r) return
  await changeWaybillStatus(current.value.id, {
    toStatus, operateBy: userStore.userInfo.username, remark: r.value || ''
  })
  ElMessage.success('流转成功')
  await openDetail(current.value.id)
  await load()
}

// ---------- 异常件 ----------
const exceptionVisible = ref(false)
const exceptionForm = reactive({ exceptionType: 'DELAY', description: '', imageUrls: '' })
function openException() {
  Object.assign(exceptionForm, { exceptionType: 'DELAY', description: '', imageUrls: '' })
  exceptionVisible.value = true
}
async function submitException() {
  await reportException(current.value.id, {
    ...exceptionForm, operateBy: userStore.userInfo.username
  })
  ElMessage.success('异常已上报')
  exceptionVisible.value = false
  current.value = await getWaybill(current.value.id)
  await Promise.all([refreshExceptions(current.value.id), refreshNodes(current.value.id)])
  await load()
}
async function doHandle(row) {
  const r = await ElMessageBox.prompt('请输入处理结果', '处理异常', { inputType: 'textarea' }).catch(() => null)
  if (!r) return
  await handleException(row.id, { handleRemark: r.value, operateBy: userStore.userInfo.username })
  ElMessage.success('已处理')
  await refreshExceptions(current.value.id)
}

// ---------- 回单 ----------
async function doUploadReceipt(file) {
  await uploadReceipt(current.value.id, file, userStore.userInfo.username, '现场签收回单')
  ElMessage.success('回单已上传')
  current.value = await getWaybill(current.value.id)
  await Promise.all([refreshReceipts(current.value.id), refreshNodes(current.value.id)])
  await load()
  return false
}
async function doVerifyReceipt(row) {
  await verifyReceipt(row.id, userStore.userInfo.username)
  ElMessage.success('回单已核验')
  await refreshReceipts(current.value.id)
}

const openFile = (url) => { if (url) window.open(url, '_blank') }

onMounted(() => { load(); loadMeta() })
</script>

<style scoped>
.block-title { font-weight: 600; margin: 16px 0 8px; color: #303133; }
.flow-buttons { display: flex; flex-wrap: wrap; gap: 8px; }
.muted { color: #909399; font-size: 12px; }
</style>
