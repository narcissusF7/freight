<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增车辆</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="plateNo" label="车牌号" min-width="120" />
      <el-table-column prop="vehicleType" label="车型" min-width="100" />
      <el-table-column prop="ownerName" label="车主" min-width="100" />
      <el-table-column prop="capacityTon" label="载重(吨)" min-width="100" />
      <el-table-column prop="axleCount" label="轴数" min-width="80" />
      <el-table-column label="关联司机" min-width="100">
        <template #default="{ row }">{{ driverName(row.driverId) }}</template>
      </el-table-column>
      <el-table-column label="运输证有效期" min-width="130">
        <template #default="{ row }">
          <el-tag v-if="expireLevel(row.vehicleLicenseExpireDate)" :type="expireLevel(row.vehicleLicenseExpireDate).type">
            {{ row.vehicleLicenseExpireDate }}（{{ expireLevel(row.vehicleLicenseExpireDate).text }}）
          </el-tag>
          <span v-else>{{ row.vehicleLicenseExpireDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="保险有效期" min-width="130">
        <template #default="{ row }">
          <el-tag v-if="expireLevel(row.insuranceExpireDate)" :type="expireLevel(row.insuranceExpireDate).type">
            {{ row.insuranceExpireDate }}（{{ expireLevel(row.insuranceExpireDate).text }}）
          </el-tag>
          <span v-else>{{ row.insuranceExpireDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag :type="auditTag(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="doAudit(row, 1)">通过</el-button>
          <el-button link type="warning" @click="doAudit(row, 2)">驳回</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="可上传行驶证照片自动识别并填充，识别结果须人工核对后再保存"
        style="margin-bottom: 12px"
      />
      <el-upload :show-file-list="false" :before-upload="handleOcr" accept="image/*">
        <el-button :loading="ocrLoading">上传行驶证识别</el-button>
      </el-upload>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="margin-top: 12px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="form.plateNo" placeholder="如 闽D12345" />
        </el-form-item>
        <el-form-item label="车型">
          <el-input v-model="form.vehicleType" placeholder="如 高栏/平板" />
        </el-form-item>
        <el-form-item label="车主">
          <el-input v-model="form.ownerName" />
        </el-form-item>
        <el-form-item label="载重(吨)">
          <el-input v-model.number="form.capacityTon" type="number" />
        </el-form-item>
        <el-form-item label="轴数">
          <el-input v-model.number="form.axleCount" type="number" />
        </el-form-item>
        <el-form-item label="关联司机">
          <el-select v-model="form.driverId" clearable placeholder="请选择司机" style="width: 100%">
            <el-option v-for="d in drivers" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运输证有效期">
          <el-date-picker
            v-model="form.vehicleLicenseExpireDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择到期日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保险有效期">
          <el-date-picker
            v-model="form.insuranceExpireDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择到期日"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVehicles, createVehicle, updateVehicle, deleteVehicle, auditVehicle, ocrRecognize } from '@/api/carrier'
import { listDrivers } from '@/api/carrier'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const drivers = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const ocrLoading = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  plateNo: '', vehicleType: '', ownerName: '', capacityTon: 0, axleCount: null, driverId: null,
  vehicleLicenseExpireDate: null, insuranceExpireDate: null
})
const rules = { plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }] }

const auditTag = (s) => (s === 1 ? 'success' : s === 2 ? 'danger' : 'info')
const auditText = (s) => (s === 1 ? '通过' : s === 2 ? '驳回' : '待审核')
const driverName = (id) => {
  const d = drivers.value.find((x) => x.id === id)
  return d ? d.name : '-'
}

// 剩余天数分级：已过期 danger / 30 天内 warning / 60 天内 info
function expireLevel(dateStr) {
  if (!dateStr) return null
  const days = Math.ceil((new Date(dateStr + 'T00:00:00') - new Date(new Date().toDateString())) / 86400000)
  if (days < 0) return { type: 'danger', text: `已过期${-days}天` }
  if (days <= 30) return { type: 'warning', text: `剩${days}天` }
  if (days <= 60) return { type: 'info', text: `剩${days}天` }
  return null
}

// 行驶证识别：只做表单预填
async function handleOcr(file) {
  ocrLoading.value = true
  try {
    const r = await ocrRecognize(file, 'VEHICLE_LICENSE', userStore.userInfo.orgId)
    if (!r || !r.success) {
      ElMessage.error((r && r.message) || '识别失败')
      return false
    }
    if (r.plateNo) form.plateNo = r.plateNo
    if (r.vehicleType) form.vehicleType = r.vehicleType
    if (r.ownerName) form.ownerName = r.ownerName
    if (r.validTo) form.vehicleLicenseExpireDate = r.validTo
    ElMessage.success(`识别完成（置信度 ${Math.round((r.confidence || 0) * 100)}%），请核对后保存`)
  } finally {
    ocrLoading.value = false
  }
  return false
}

async function load() {
  list.value = await listVehicles()
}
async function loadDrivers() {
  drivers.value = await listDrivers()
}
function resetForm() {
  Object.assign(form, {
    plateNo: '', vehicleType: '', ownerName: '', capacityTon: 0, axleCount: null, driverId: null,
    vehicleLicenseExpireDate: null, insuranceExpireDate: null
  })
  editingId.value = null
}
function openCreate() {
  resetForm()
  dialogTitle.value = '新增车辆'
  dialogVisible.value = true
}
function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    plateNo: row.plateNo, vehicleType: row.vehicleType, ownerName: row.ownerName,
    capacityTon: row.capacityTon, axleCount: row.axleCount, driverId: row.driverId,
    vehicleLicenseExpireDate: row.vehicleLicenseExpireDate || null,
    insuranceExpireDate: row.insuranceExpireDate || null
  })
  dialogTitle.value = '编辑车辆'
  dialogVisible.value = true
}
async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) await updateVehicle(editingId.value, { ...form })
      else await createVehicle({ ...form })
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await load()
    } finally {
      saving.value = false
    }
  })
}
async function doAudit(row, status) {
  let remark = ''
  if (status === 2) {
    const r = await ElMessageBox.prompt('请输入驳回原因', '审核', { inputType: 'textarea' }).catch(() => null)
    if (!r) return
    remark = r.value
  }
  await auditVehicle(row.id, { status, remark, operator: userStore.userInfo.username })
  ElMessage.success('审核完成')
  await load()
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除车辆「${row.plateNo}」？`, '提示', { type: 'warning' })
  await deleteVehicle(row.id)
  ElMessage.success('删除成功')
  await load()
}
onMounted(() => { load(); loadDrivers() })
</script>
