<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增角色</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column prop="roleCode" label="角色编码" min-width="140" />
      <el-table-column prop="orgId" label="所属机构ID" min-width="120" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openAuth(row)">授权菜单</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 角色新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="机构ID">
          <el-input-number v-model="form.orgId" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色授权菜单 -->
    <el-dialog v-model="authVisible" title="授权菜单" width="420px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
      />
      <template #footer>
        <el-button @click="authVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAuthSave">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoles, createRole, updateRole, deleteRole, assignRoleMenus, listRoleMenus } from '@/api/role'
import { treeMenus } from '@/api/menu'

const list = ref([])
const menuTree = ref([])
const dialogVisible = ref(false)
const authVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const menuTreeRef = ref()
const editingId = ref(null)
const authRoleId = ref(null)

const form = reactive({ roleName: '', roleCode: '', orgId: 0 })
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

async function load() {
  list.value = await listRoles()
}

function resetForm() {
  Object.assign(form, { roleName: '', roleCode: '', orgId: 0 })
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, { roleName: row.roleName, roleCode: row.roleCode, orgId: row.orgId })
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await updateRole(editingId.value, { ...form })
        ElMessage.success('更新成功')
      } else {
        await createRole({ ...form })
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
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function openAuth(row) {
  authRoleId.value = row.id
  if (!menuTree.value.length) menuTree.value = await treeMenus()
  const ids = await listRoleMenus(row.id)
  authVisible.value = true
  // 等待 el-tree 渲染后再设勾选
  setTimeout(() => menuTreeRef.value?.setCheckedKeys(ids || []), 0)
}

async function handleAuthSave() {
  const keys = menuTreeRef.value?.getCheckedKeys() || []
  saving.value = true
  try {
    await assignRoleMenus(authRoleId.value, keys)
    ElMessage.success('授权成功')
    authVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
