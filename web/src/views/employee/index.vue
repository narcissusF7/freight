<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增员工</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="username" label="账号" min-width="130" />
      <el-table-column prop="realName" label="姓名" min-width="110" />
      <el-table-column label="手机号" min-width="100">
        <template #default="{ row }">{{ row.phoneEncrypted ? '已加密' : '—' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openRoles(row)">分配角色</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑员工 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" placeholder="登录账号" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phoneEncrypted" placeholder="明文手机号，服务端加密存储" />
        </el-form-item>
        <el-form-item label="所属机构">
          <el-input-number v-model="form.orgId" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="可多选" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="420px">
      <el-select v-model="roleIds" multiple placeholder="可多选" style="width: 100%">
        <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleRoleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee,
  assignRoles,
  listEmployeeRoles
} from '@/api/employee'
import { listRoles } from '@/api/role'

const list = ref([])
const roleOptions = ref([])
const dialogVisible = ref(false)
const roleVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)
const roleEmpId = ref(null)
const roleIds = ref([])

const form = reactive({
  orgId: 0,
  username: '',
  password: '',
  realName: '',
  phoneEncrypted: '',
  status: 1,
  roleIds: []
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function load() {
  list.value = await listEmployees()
  if (!roleOptions.value.length) roleOptions.value = await listRoles()
}

function resetForm() {
  Object.assign(form, {
    orgId: 0,
    username: '',
    password: '',
    realName: '',
    phoneEncrypted: '',
    status: 1,
    roleIds: []
  })
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增员工'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    orgId: row.orgId,
    username: row.username,
    realName: row.realName,
    phoneEncrypted: row.phoneEncrypted,
    status: row.status,
    roleIds: row.roleIds || []
  })
  dialogTitle.value = '编辑员工'
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await updateEmployee(editingId.value, { ...form })
        ElMessage.success('更新成功')
      } else {
        await createEmployee({ ...form })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await load()
    } finally {
      saving.value = false
    }
  })
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除员工「${row.realName || row.username}」？`, '提示', {
    type: 'warning'
  })
  await deleteEmployee(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function openRoles(row) {
  roleEmpId.value = row.id
  if (!roleOptions.value.length) roleOptions.value = await listRoles()
  roleIds.value = await listEmployeeRoles(row.id)
  roleVisible.value = true
}

async function handleRoleSave() {
  saving.value = true
  try {
    await assignRoles(roleEmpId.value, roleIds.value)
    ElMessage.success('分配成功')
    roleVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
