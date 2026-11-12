<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">发布货源</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="goodsName" label="货物名称" min-width="120" />
      <el-table-column prop="weight" label="重量(吨)" min-width="100" />
      <el-table-column prop="volume" label="体积(方)" min-width="100" />
      <el-table-column label="装货地" min-width="180">
        <template #default="{ row }">
          {{ row.loadAddr || '-' }}
          <el-tag v-if="row.loadLat" size="small" type="success" style="margin-left: 4px">已定位</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="卸货地" min-width="180">
        <template #default="{ row }">
          {{ row.unloadAddr || '-' }}
          <el-tag v-if="row.unloadLat" size="small" type="success" style="margin-left: 4px">已定位</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="freightPrice" label="运费(元)" min-width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="warning" @click="doClose(row)">关闭</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="货物名称" prop="goodsName">
          <el-input v-model="form.goodsName" />
        </el-form-item>
        <el-form-item label="重量(吨)">
          <el-input v-model.number="form.weight" type="number" />
        </el-form-item>
        <el-form-item label="体积(方)">
          <el-input v-model.number="form.volume" type="number" />
        </el-form-item>
        <el-form-item label="装货地">
          <el-input v-model="form.loadAddr" placeholder="输入详细地址，或用地图选点">
            <template #append>
              <el-button @click="openPicker('load')">地图选点</el-button>
            </template>
          </el-input>
          <div class="coord-hint">
            {{ coordText(form.loadLat, form.loadLng) }}
          </div>
        </el-form-item>
        <el-form-item label="卸货地">
          <el-input v-model="form.unloadAddr" placeholder="输入详细地址，或用地图选点">
            <template #append>
              <el-button @click="openPicker('unload')">地图选点</el-button>
            </template>
          </el-input>
          <div class="coord-hint">
            {{ coordText(form.unloadLat, form.unloadLng) }}
          </div>
        </el-form-item>
        <el-form-item label="装货时间">
          <el-date-picker v-model="form.loadTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" />
        </el-form-item>
        <el-form-item label="运费(元)">
          <el-input v-model.number="form.freightPrice" type="number" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 地图选点：腾讯地图 GL JS（GCJ-02），默认代理模式不暴露 key -->
    <MapPicker
      v-model="pickerVisible"
      :title="pickerTitle"
      :lat="pickerTarget === 'load' ? form.loadLat : form.unloadLat"
      :lng="pickerTarget === 'load' ? form.loadLng : form.unloadLng"
      :address="pickerTarget === 'load' ? form.loadAddr : form.unloadAddr"
      @confirm="onPickConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listGoodsSources, createGoodsSource, closeGoodsSource } from '@/api/waybill'
import MapPicker from '@/components/MapPicker.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  goodsName: '', weight: null, volume: null, loadAddr: '', unloadAddr: '',
  loadLat: null, loadLng: null, unloadLat: null, unloadLng: null,
  loadTime: '', freightPrice: 0, contactName: '', contactPhone: '', remark: ''
})
const rules = { goodsName: [{ required: true, message: '请输入货物名称', trigger: 'blur' }] }

const pickerVisible = ref(false)
const pickerTarget = ref('load')
const pickerTitle = ref('选择装货地')
function openPicker(target) {
  pickerTarget.value = target
  pickerTitle.value = target === 'load' ? '选择装货地' : '选择卸货地'
  pickerVisible.value = true
}
function onPickConfirm(p) {
  if (pickerTarget.value === 'load') {
    if (p.address) form.loadAddr = p.address
    form.loadLat = p.lat
    form.loadLng = p.lng
  } else {
    if (p.address) form.unloadAddr = p.address
    form.unloadLat = p.lat
    form.unloadLng = p.lng
  }
  ElMessage.success('已回填地点与经纬度（GCJ-02）')
}
const coordText = (lat, lng) =>
  lat === null || lat === undefined || lat === ''
    ? '未定位：可直接手填地址，或点「地图选点」精确落位'
    : `GCJ-02 ${Number(lat).toFixed(6)}, ${Number(lng).toFixed(6)}`

const statusTag = (s) => (s === 1 ? 'success' : s === 2 ? 'info' : 'warning')
const statusText = (s) => (s === 1 ? '已生成运单' : s === 2 ? '已关闭' : '待承运')

async function load() {
  list.value = await listGoodsSources()
}
function resetForm() {
  Object.assign(form, {
    goodsName: '', weight: null, volume: null, loadAddr: '', unloadAddr: '',
    loadLat: null, loadLng: null, unloadLat: null, unloadLng: null,
    loadTime: '', freightPrice: 0, contactName: '', contactPhone: '', remark: ''
  })
  editingId.value = null
}
function openCreate() {
  resetForm()
  dialogTitle.value = '发布货源'
  dialogVisible.value = true
}
function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    goodsName: row.goodsName, weight: row.weight, volume: row.volume,
    loadAddr: row.loadAddr, unloadAddr: row.unloadAddr, loadTime: row.loadTime,
    loadLat: row.loadLat ?? null, loadLng: row.loadLng ?? null,
    unloadLat: row.unloadLat ?? null, unloadLng: row.unloadLng ?? null,
    freightPrice: row.freightPrice, contactName: row.contactName, contactPhone: row.contactPhone, remark: row.remark
  })
  dialogTitle.value = '编辑货源'
  dialogVisible.value = true
}
async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        ElMessage.info('M2 货源编辑接口暂未开放，请删除后重新发布')
      } else {
        await createGoodsSource({ ...form }, userStore.userInfo.username)
        ElMessage.success('发布成功')
      }
      dialogVisible.value = false
      await load()
    } finally {
      saving.value = false
    }
  })
}
async function doClose(row) {
  await ElMessageBox.confirm(`确认关闭货源「${row.goodsName}」？`, '提示', { type: 'warning' })
  await closeGoodsSource(row.id, userStore.userInfo.username)
  ElMessage.success('已关闭')
  await load()
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除货源「${row.goodsName}」？`, '提示', { type: 'warning' })
  ElMessage.info('M2 货源删除接口暂未开放')
}
onMounted(load)
</script>

<style scoped>
.coord-hint { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 2px; }
</style>
