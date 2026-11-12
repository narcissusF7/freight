<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增司机</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="phone" label="手机" min-width="120" />
      <el-table-column prop="idCardNo" label="身份证号" min-width="170" />
      <el-table-column prop="driverLicenseNo" label="驾驶证号" min-width="150" />
      <el-table-column label="驾驶证有效期" min-width="130">
        <template #default="{ row }">
          <el-tag v-if="expireLevel(row.licenseExpireDate)" :type="expireLevel(row.licenseExpireDate).type">
            {{ row.licenseExpireDate }}（{{ expireLevel(row.licenseExpireDate).text }}）
          </el-tag>
          <span v-else>{{ row.licenseExpireDate || '-' }}</span>
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
        title="可上传驾驶证/身份证照片自动识别并填充，识别结果须人工核对后再保存"
        style="margin-bottom: 12px"
      />
      <el-upload :show-file-list="false" :before-upload="handleOcr" accept="image/*">
        <el-button :loading="ocrLoading">上传证件照片识别</el-button>
      </el-upload>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="margin-top: 12px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCardNo" placeholder="生产须加密存储" />
        </el-form-item>
        <el-form-item label="驾驶证号">
          <el-input v-model="form.driverLicenseNo" />
        </el-form-item>
        <el-form-item label="驾驶证有效期">
          <el-date-picker
            v-model="form.licenseExpireDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择到期日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="身份证有效期">
          <el-date-picker
            v-model="form.idCardExpireDate"
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
import { listDrivers, createDriver, updateDriver, deleteDriver, auditDriver, ocrRecognize } from '@/api/carrier'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const ocrLoading = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  name: '', phone: '', idCardNo: '', driverLicenseNo: '', licenseExpireDate: null, idCardExpireDate: null
})
const rules = { name: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }

const auditTag = (s) => (s === 1 ? 'success' : s === 2 ? 'danger' : 'info')
const auditText = (s) => (s === 1 ? '通过' : s === 2 ? '驳回' : '待审核')

// 剩余天数分级：已过期 danger / 30 天内 warning / 60 天内 info
function expireLevel(dateStr) {
  if (!dateStr) return null
  const days = Math.ceil((new Date(dateStr + 'T00:00:00') - new Date(new Date().toDateString())) / 86400000)
  if (days < 0) return { type: 'danger', text: `已过期${-days}天` }
  if (days <= 30) return { type: 'warning', text: `剩${days}天` }
  if (days <= 60) return { type: 'info', text: `剩${days}天` }
  return null
}

// 证照识别：只做表单预填，不直接入库
async function handleOcr(file) {
  ocrLoading.value = true
  try {
    const r = await ocrRecognize(file, 'DRIVER_LICENSE', userStore.userInfo.orgId)
    if (!r || !r.success) {
      ElMessage.error((r && r.message) || '识别失败')
      return false
    }
    if (r.name) form.name = r.name
    if (r.idCardNo) form.idCardNo = r.idCardNo
    if (r.licenseNo) form.driverLicenseNo = r.licenseNo
    if (r.validTo) form.licenseExpireDate = r.validTo
    ElMessage.success(`识别完成（置信度 ${Math.round((r.confidence || 0) * 100)}%），请核对后保存`)
  } finally {
    ocrLoading.value = false
  }
  return false
}

async function load() {
  list.value = await listDrivers()
}
function resetForm() {
  Object.assign(form, {
    name: '', phone: '', idCardNo: '', driverLicenseNo: '', licenseExpireDate: null, idCardExpireDate: null
  })
  editingId.value = null
}
function openCreate() {
  resetForm()
  dialogTitle.value = '新增司机'
  dialogVisible.value = true
}
function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    phone: row.phone,
    idCardNo: row.idCardNo,
    driverLicenseNo: row.driverLicenseNo,
    licenseExpireDate: row.licenseExpireDate || null,
    idCardExpireDate: row.idCardExpireDate || null
  })
  dialogTitle.value = '编辑司机'
  dialogVisible.value = true
}
async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) await updateDriver(editingId.value, { ...form })
      else await createDriver({ ...form })
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
  await auditDriver(row.id, { status, remark, operator: userStore.userInfo.username })
  ElMessage.success('审核完成')
  await load()
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除司机「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDriver(row.id)
  ElMessage.success('删除成功')
  await load()
}
onMounted(load)
</script>
