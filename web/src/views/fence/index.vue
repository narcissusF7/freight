<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增围栏</el-button>
      <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 140px" @change="load">
        <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="k" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <span class="muted">
        共 {{ list.length }} 个围栏，启用 {{ list.filter((x) => x.enabled === 1).length }} 个；
        轨迹上报后按围栏做进出判定，命中即生成「轨迹预警」
      </span>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="fenceCode" label="编码" width="130" />
      <el-table-column prop="fenceName" label="名称" min-width="150" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.fenceType)">{{ TYPE_TEXT[row.fenceType] || row.fenceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="形状" width="90">
        <template #default="{ row }">
          {{ row.shape === 'POLYGON' ? '多边形' : '圆形' }}
        </template>
      </el-table-column>
      <el-table-column label="范围" min-width="220">
        <template #default="{ row }">
          <template v-if="row.shape === 'POLYGON'">
            {{ vertexCount(row.vertices) }} 个顶点
          </template>
          <template v-else>
            {{ row.centerLat }}, {{ row.centerLng }} · 半径 {{ row.radiusM }} m
          </template>
        </template>
      </el-table-column>
      <el-table-column label="触发" width="100">
        <template #default="{ row }">{{ TRIGGER_TEXT[row.triggerType] || row.triggerType }}</template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === 1" @change="(v) => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="围栏名称" prop="fenceName">
          <el-input v-model="form.fenceName" placeholder="如 厦门北站装货区" />
        </el-form-item>
        <el-form-item label="围栏编码" prop="fenceCode">
          <el-input v-model="form.fenceCode" :disabled="!!editingId" placeholder="机构内唯一，如 FENCE-XMB" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.fenceType" style="width: 100%">
            <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="形状">
          <el-radio-group v-model="form.shape">
            <el-radio-button value="CIRCLE">圆形</el-radio-button>
            <el-radio-button value="POLYGON">多边形</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.shape === 'CIRCLE'">
          <el-form-item label="中心点">
            <el-input v-model="centerText" readonly placeholder="点右侧按钮在地图上选点">
              <template #append>
                <el-button @click="pickerVisible = true">地图选点</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="半径(米)" prop="radiusM">
            <el-input v-model.number="form.radiusM" type="number" placeholder="如 3000" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="顶点">
            <el-input
              v-model="vertexText"
              type="textarea"
              :rows="5"
              placeholder="每行一个顶点：纬度,经度（至少 3 行）&#10;24.60,118.10&#10;24.45,117.99&#10;24.45,118.21"
            />
            <div class="coord-hint">闭合由服务端按首尾自动处理；坐标系 GCJ-02（与地图选点一致）</div>
          </el-form-item>
        </template>

        <el-form-item label="触发方式">
          <el-radio-group v-model="form.triggerType">
            <el-radio-button value="BOTH">进出都报</el-radio-button>
            <el-radio-button value="IN">仅进入</el-radio-button>
            <el-radio-button value="OUT">仅离开</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <MapPicker
      v-model="pickerVisible"
      title="选择围栏中心点"
      :lat="form.centerLat"
      :lng="form.centerLng"
      :address="''"
      @confirm="onPickCenter"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFences, createFence, updateFence, deleteFence } from '@/api/track'
import { useUserStore } from '@/store/user'
import MapPicker from '@/components/MapPicker.vue'

const userStore = useUserStore()
const list = ref([])
const typeFilter = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)
const pickerVisible = ref(false)
const vertexText = ref('')

const TYPE_TEXT = { LOAD: '装货区', UNLOAD: '卸货区', FORBIDDEN: '禁行区', CUSTOM: '自定义' }
const TRIGGER_TEXT = { IN: '仅进入', OUT: '仅离开', BOTH: '进出都报' }

const form = reactive({
  fenceName: '',
  fenceCode: '',
  fenceType: 'LOAD',
  shape: 'CIRCLE',
  centerLat: null,
  centerLng: null,
  radiusM: 3000,
  triggerType: 'BOTH',
  enabled: 1,
  remark: ''
})

const rules = {
  fenceName: [{ required: true, message: '请输入围栏名称', trigger: 'blur' }],
  fenceCode: [{ required: true, message: '请输入围栏编码', trigger: 'blur' }],
  radiusM: [{ required: true, message: '圆形围栏必须填半径', trigger: 'blur' }]
}

const centerText = computed(() =>
  form.centerLat === null || form.centerLat === undefined || form.centerLat === ''
    ? ''
    : `${form.centerLat}, ${form.centerLng}`
)

const typeTag = (t) => (t === 'FORBIDDEN' ? 'danger' : t === 'LOAD' ? 'success' : t === 'UNLOAD' ? 'warning' : 'info')

function vertexCount(vertices) {
  if (!vertices) return 0
  try {
    return JSON.parse(vertices).length
  } catch (e) {
    return 0
  }
}

async function load() {
  list.value = await listFences({ orgId: userStore.userInfo.orgId, fenceType: typeFilter.value || undefined })
}

function resetForm() {
  Object.assign(form, {
    fenceName: '',
    fenceCode: '',
    fenceType: 'LOAD',
    shape: 'CIRCLE',
    centerLat: null,
    centerLng: null,
    radiusM: 3000,
    triggerType: 'BOTH',
    enabled: 1,
    remark: ''
  })
  vertexText.value = ''
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增围栏'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    fenceName: row.fenceName,
    fenceCode: row.fenceCode,
    fenceType: row.fenceType,
    shape: row.shape,
    centerLat: row.centerLat === null ? null : Number(row.centerLat),
    centerLng: row.centerLng === null ? null : Number(row.centerLng),
    radiusM: row.radiusM,
    triggerType: row.triggerType,
    enabled: row.enabled,
    remark: row.remark
  })
  if (row.shape === 'POLYGON' && row.vertices) {
    try {
      vertexText.value = JSON.parse(row.vertices)
        .map((v) => `${v[0]},${v[1]}`)
        .join('\n')
    } catch (e) {
      vertexText.value = ''
    }
  }
  dialogTitle.value = '编辑围栏'
  dialogVisible.value = true
}

function onPickCenter(p) {
  form.centerLat = p.lat
  form.centerLng = p.lng
}

function parseVertices() {
  const lines = vertexText.value
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  const out = []
  for (const line of lines) {
    const parts = line.split(',').map((s) => Number(s.trim()))
    if (parts.length < 2 || Number.isNaN(parts[0]) || Number.isNaN(parts[1])) {
      throw new Error(`顶点格式错误：${line}`)
    }
    out.push([parts[0], parts[1]])
  }
  if (out.length < 3) {
    throw new Error('多边形至少需要 3 个顶点')
  }
  return out
}

async function handleSave() {
  await formRef.value.validate()
  const payload = {
    orgId: userStore.userInfo.orgId,
    fenceName: form.fenceName,
    fenceCode: form.fenceCode,
    fenceType: form.fenceType,
    shape: form.shape,
    triggerType: form.triggerType,
    enabled: form.enabled,
    remark: form.remark,
    operateBy: userStore.userInfo.username || 'admin'
  }
  if (form.shape === 'CIRCLE') {
    if (form.centerLat === null || form.centerLng === null) {
      ElMessage.warning('请先用地图选点确定围栏中心')
      return
    }
    payload.centerLat = form.centerLat
    payload.centerLng = form.centerLng
    payload.radiusM = form.radiusM
  } else {
    try {
      payload.vertices = parseVertices()
    } catch (e) {
      ElMessage.warning(e.message || '顶点格式错误')
      return
    }
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateFence(editingId.value, payload)
    } else {
      await createFence(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, on) {
  try {
    await updateFence(row.id, {
      orgId: row.orgId,
      fenceName: row.fenceName,
      fenceCode: row.fenceCode,
      fenceType: row.fenceType,
      shape: row.shape,
      centerLat: row.centerLat,
      centerLng: row.centerLng,
      radiusM: row.radiusM,
      vertices: row.vertices ? JSON.parse(row.vertices) : null,
      triggerType: row.triggerType,
      enabled: on ? 1 : 0,
      remark: row.remark,
      operateBy: userStore.userInfo.username || 'admin'
    })
    ElMessage.success(on ? '已启用' : '已停用')
    await load()
  } catch (e) {
    await load()
  }
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除围栏「${row.fenceName}」？历史预警不会删除。`, '提示', { type: 'warning' })
  await deleteFence(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.coord-hint {
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
</style>
