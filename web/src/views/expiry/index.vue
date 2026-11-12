<template>
  <div class="page-container">
    <div class="toolbar">
      <span class="muted">预警窗口</span>
      <el-radio-group v-model="days" size="small" @change="load">
        <el-radio-button :value="30">30 天</el-radio-button>
        <el-radio-button :value="60">60 天</el-radio-button>
        <el-radio-button :value="90">90 天</el-radio-button>
      </el-radio-group>
      <el-button @click="load">刷新</el-button>
      <span class="muted">
        共 {{ list.length }} 条：已过期 {{ countBy('EXPIRED') }}、30 天内到期 {{ countBy('URGENT') }}、预警 {{ countBy('WARN') }}
      </span>
    </div>

    <el-table :data="list" border>
      <el-table-column label="对象" width="110">
        <template #default="{ row }">
          <el-tag :type="row.bizType === 'DRIVER' ? 'primary' : 'success'">
            {{ row.bizType === 'DRIVER' ? '司机' : '车辆' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bizName" label="姓名/车牌" min-width="140" />
      <el-table-column prop="docType" label="证件" width="120" />
      <el-table-column prop="docNo" label="证件号" min-width="180" />
      <el-table-column prop="expireDate" label="到期日" width="130" />
      <el-table-column label="剩余" width="120">
        <template #default="{ row }">
          <span :class="row.daysLeft < 0 ? 'danger' : ''">
            {{ row.daysLeft < 0 ? `已过期 ${-row.daysLeft} 天` : `剩 ${row.daysLeft} 天` }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="levelType(row.level)">{{ LEVEL_TEXT[row.level] }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listExpiring } from '@/api/carrier'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const days = ref(60)
const list = ref([])
const LEVEL_TEXT = { EXPIRED: '已过期', URGENT: '紧急', WARN: '预警' }
const levelType = (l) => (l === 'EXPIRED' ? 'danger' : l === 'URGENT' ? 'warning' : 'info')
const countBy = (level) => list.value.filter((x) => x.level === level).length

async function load() {
  list.value = await listExpiring(userStore.userInfo.orgId, days.value)
}
onMounted(load)
</script>

<style scoped>
.muted { color: #909399; font-size: 13px; }
.danger { color: #f56c6c; font-weight: 600; }
</style>
