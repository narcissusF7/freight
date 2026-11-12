<template>
  <div class="page-container">
    <el-card>
      <template #header>文件上传</template>
      <el-form :model="form" label-width="90px" style="max-width: 520px">
        <el-form-item label="机构ID">
          <el-input-number v-model="form.orgId" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="form.bizType" placeholder="如 contract / idcard / license" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="onChange"
            :on-exceed="onExceed"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">单文件，选好后点击「上传」</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="uploading" @click="handleUpload">上传</el-button>
          <el-button @click="resetUpload">重置</el-button>
        </el-form-item>
        <el-form-item v-if="lastId" label="上传结果">
          <el-tag type="success">文件ID：{{ lastId }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>文件访问地址查询</template>
      <el-form inline>
        <el-form-item label="文件ID">
          <el-input v-model="queryId" placeholder="输入文件ID" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQueryUrl">查询</el-button>
        </el-form-item>
      </el-form>
      <div v-if="fileUrl">
        访问地址：
        <el-link type="primary" :href="fileUrl" target="_blank">{{ fileUrl }}</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile, getFileUrl } from '@/api/file'

const uploadRef = ref()
const uploading = ref(false)
const selectedFile = ref(null)
const lastId = ref(null)
const queryId = ref('')
const fileUrl = ref('')

const form = reactive({ orgId: 0, bizType: 'contract' })

function onChange(file) {
  selectedFile.value = file.raw
}

function onExceed() {
  ElMessage.warning('只能选择一个文件，请先移除已选文件')
}

function resetUpload() {
  uploadRef.value?.clearFiles()
  selectedFile.value = null
  lastId.value = null
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!form.bizType) {
    ElMessage.warning('请填写业务类型')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('orgId', form.orgId)
    fd.append('bizType', form.bizType)
    fd.append('file', selectedFile.value)
    const id = await uploadFile(fd)
    lastId.value = id
    ElMessage.success(`上传成功，文件ID：${id}`)
  } finally {
    uploading.value = false
  }
}

async function handleQueryUrl() {
  if (!queryId.value) {
    ElMessage.warning('请输入文件ID')
    return
  }
  fileUrl.value = await getFileUrl(queryId.value)
}
</script>
