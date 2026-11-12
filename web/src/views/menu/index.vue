<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增菜单</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" border default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" min-width="160" />
      <el-table-column prop="menuType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.menuType)">{{ typeLabel(row.menuType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" min-width="140" />
      <el-table-column prop="permission" label="权限标识" min-width="140" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreate(row)">加下级</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="tree"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="留空表示顶级菜单"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-select v-model="form.menuType" placeholder="请选择">
            <el-option label="目录" value="CATALOG" />
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="如 /org" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" placeholder="如 views/org/index" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="如 org:add" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { treeMenus, createMenu, updateMenu, deleteMenu } from '@/api/menu'

const tree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  parentId: null,
  menuName: '',
  menuType: 'MENU',
  path: '',
  component: '',
  permission: '',
  sort: 0,
  status: 1
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

function typeLabel(t) {
  return t === 'CATALOG' ? '目录' : t === 'BUTTON' ? '按钮' : '菜单'
}
function typeTag(t) {
  return t === 'CATALOG' ? 'warning' : t === 'BUTTON' ? 'info' : 'success'
}

async function load() {
  tree.value = await treeMenus()
}

function resetForm() {
  Object.assign(form, {
    parentId: null,
    menuName: '',
    menuType: 'MENU',
    path: '',
    component: '',
    permission: '',
    sort: 0,
    status: 1
  })
  editingId.value = null
}

function openCreate(parent) {
  resetForm()
  if (parent) form.parentId = parent.id
  dialogTitle.value = parent ? '新增下级菜单' : '新增菜单'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path,
    component: row.component,
    permission: row.permission,
    sort: row.sort,
    status: row.status
  })
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await updateMenu(editingId.value, { ...form })
        ElMessage.success('更新成功')
      } else {
        await createMenu({ ...form })
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
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
