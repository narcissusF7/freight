<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate()">新增机构</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" border default-expand-all>
      <el-table-column prop="orgName" label="机构名称" min-width="160" />
      <el-table-column prop="orgCode" label="机构编码" min-width="140" />
      <el-table-column prop="status" label="状态" width="90">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级机构">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'orgName', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="留空表示根机构"
          />
        </el-form-item>
        <el-form-item label="机构名称" prop="orgName">
          <el-input v-model="form.orgName" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="机构编码" prop="orgCode">
          <el-input v-model="form.orgCode" placeholder="请输入机构编码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { treeOrgs, createOrg, updateOrg, deleteOrg } from '@/api/org'

const tree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  parentId: null,
  orgName: '',
  orgCode: '',
  status: 1
})

const rules = {
  orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }]
}

// el-tree-select 需要树形数据；新增根机构时 parentId 为 null
const parentOptions = computed(() => tree.value)

async function load() {
  tree.value = await treeOrgs()
}

function resetForm() {
  Object.assign(form, { parentId: null, orgName: '', orgCode: '', status: 1 })
  editingId.value = null
}

function openCreate(parent) {
  resetForm()
  if (parent) form.parentId = parent.id
  dialogTitle.value = parent ? '新增下级机构' : '新增机构'
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    parentId: row.parentId,
    orgName: row.orgName,
    orgCode: row.orgCode,
    status: row.status
  })
  dialogTitle.value = '编辑机构'
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await updateOrg(editingId.value, { ...form })
        ElMessage.success('更新成功')
      } else {
        await createOrg({ ...form })
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
  await ElMessageBox.confirm(`确认删除机构「${row.orgName}」？`, '提示', { type: 'warning' })
  await deleteOrg(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
