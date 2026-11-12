<template>
  <div class="page-container">
    <el-tabs v-model="tab">
      <!-- ------------------------------------------------ 消息记录 -->
      <el-tab-pane label="消息记录" name="records">
        <div class="toolbar">
          <el-select v-model="filter.channel" placeholder="全部通道" clearable style="width: 130px" @change="loadRecords">
            <el-option v-for="(v, k) in CHANNEL_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="filter.status" placeholder="全部状态" clearable style="width: 140px" @change="loadRecords">
            <el-option v-for="(v, k) in STATUS_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
          <el-input v-model="filter.bizKey" placeholder="业务键（如 alarm:1）" clearable style="width: 190px" @keyup.enter="loadRecords" />
          <el-button type="primary" @click="loadRecords">查询</el-button>
          <el-button @click="doDispatch">投递待发</el-button>
          <el-button @click="openNotify">模拟发消息</el-button>
          <span class="muted">
            共 {{ records.length }} 条；待发 {{ countStatus('PENDING') }}、已发 {{ countStatus('SENT') }}、
            失败 {{ countStatus('FAILED') }}、已放弃 {{ countStatus('GIVEN_UP') }}
          </span>
        </div>

        <el-table :data="records" border>
          <el-table-column prop="createTime" label="创建时间" width="165" />
          <el-table-column label="通道" width="100">
            <template #default="{ row }">
              <el-tag :type="channelTag(row.channel)">{{ CHANNEL_TEXT[row.channel] || row.channel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizType" label="业务节点" width="150" />
          <el-table-column label="收件人" width="170">
            <template #default="{ row }">
              {{ row.receiverType }}<span class="muted"> / {{ row.receiver || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ STATUS_TEXT[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="重试" width="70">
            <template #default="{ row }">{{ row.retryCount }}</template>
          </el-table-column>
          <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="providerMsgId" label="服务商消息号" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="row.status === 'PENDING'" @click="doResend(row)">重发</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ------------------------------------------------ 站内信 -->
      <el-tab-pane label="站内信" name="inbox">
        <div class="toolbar">
          <el-switch v-model="inboxUnread" active-text="只看未读" @change="loadInbox" />
          <el-button @click="loadInbox">刷新</el-button>
          <el-button :disabled="!selected.length" @click="doMarkRead">标记已读（{{ selected.length }}）</el-button>
          <span class="muted">站内信即 INBOX 通道，调度台可见；与短信/微信/推送共用同一套模板与重试</span>
        </div>
        <el-table :data="inbox" border @selection-change="(rows) => (selected = rows)">
          <el-table-column type="selection" width="46" />
          <el-table-column prop="createTime" label="时间" width="165" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
          <el-table-column prop="bizKey" label="业务键" min-width="150" />
          <el-table-column label="已读" width="80">
            <template #default="{ row }">
              <el-tag :type="row.readFlag === 1 ? 'info' : 'danger'" effect="plain">
                {{ row.readFlag === 1 ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ------------------------------------------------ 模板配置 -->
      <el-tab-pane label="消息模板" name="templates">
        <div class="toolbar">
          <el-button type="primary" @click="openTplCreate">新增模板</el-button>
          <el-button @click="loadTemplates">刷新</el-button>
          <span class="muted">
            一行 = 一个「业务节点 + 通道」；机构模板（orgId={{ userStore.userInfo.orgId }}）覆盖平台默认（orgId=0），停用即该节点不再发此通道
          </span>
        </div>
        <el-table :data="templates" border>
          <el-table-column prop="templateCode" label="模板编码" min-width="180" />
          <el-table-column prop="templateName" label="名称" min-width="140" />
          <el-table-column prop="bizType" label="业务节点" width="150" />
          <el-table-column label="通道" width="100">
            <template #default="{ row }">
              <el-tag :type="channelTag(row.channel)">{{ CHANNEL_TEXT[row.channel] || row.channel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiverType" label="收件方" width="100" />
          <el-table-column prop="titleTpl" label="标题模板" min-width="160" show-overflow-tooltip />
          <el-table-column prop="contentTpl" label="内容模板" min-width="240" show-overflow-tooltip />
          <el-table-column label="范围" width="100">
            <template #default="{ row }">
              <el-tag :type="row.orgId === 0 ? 'info' : 'success'" effect="plain">
                {{ row.orgId === 0 ? '平台默认' : '机构' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="85">
            <template #default="{ row }">
              <el-switch :model-value="row.enabled === 1" @change="(v) => toggleTemplate(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTplEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="doTplDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 模板编辑 -->
    <el-dialog v-model="tplVisible" :title="tplTitle" width="560px">
      <el-form ref="tplFormRef" :model="tplForm" :rules="tplRules" label-width="96px">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="tplForm.templateCode" :disabled="!!tplEditingId" placeholder="如 TRACK_ALARM_SMS" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="tplForm.templateName" />
        </el-form-item>
        <el-form-item label="业务节点" prop="bizType">
          <el-input v-model="tplForm.bizType" placeholder="TRACK_ALARM / WAYBILL_STATUS / CERT_EXPIRY" />
        </el-form-item>
        <el-form-item label="通道">
          <el-select v-model="tplForm.channel" style="width: 100%">
            <el-option v-for="(v, k) in CHANNEL_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件方">
          <el-select v-model="tplForm.receiverType" style="width: 100%">
            <el-option label="司机" value="DRIVER" />
            <el-option label="货主" value="SHIPPER" />
            <el-option label="员工" value="EMPLOYEE" />
            <el-option label="机构（站内信）" value="ORG" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板">
          <el-input v-model="tplForm.titleTpl" placeholder="支持 ${var} 占位符，短信通道可留空" />
        </el-form-item>
        <el-form-item label="内容模板" prop="contentTpl">
          <el-input v-model="tplForm.contentTpl" type="textarea" :rows="3" placeholder="如 ${content}（运单 ${waybillId}）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="tplForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="tplForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模拟发消息 -->
    <el-dialog v-model="notifyVisible" title="模拟发消息（按模板群发四通道）" width="520px">
      <el-form label-width="90px">
        <el-form-item label="业务节点">
          <el-select v-model="notifyForm.bizType" style="width: 100%">
            <el-option label="在途预警 TRACK_ALARM" value="TRACK_ALARM" />
            <el-option label="运单状态 WAYBILL_STATUS" value="WAYBILL_STATUS" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务键">
          <el-input v-model="notifyForm.bizKey" placeholder="同一业务键只发一次（幂等）" />
        </el-form-item>
        <el-form-item label="预警类型">
          <el-input v-model="notifyForm.alarmType" placeholder="OVERSPEED / DEVIATE …" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="notifyForm.content" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="司机手机">
          <el-input v-model="notifyForm.driverPhone" placeholder="留空则按 driverId 从承运档案取号" />
        </el-form-item>
        <el-form-item label="司机ID">
          <el-input v-model.number="notifyForm.driverId" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doNotify">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMessages,
  listInbox,
  markInboxRead,
  dispatchMessages,
  resendMessage,
  notifyMessage,
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate
} from '@/api/message'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const tab = ref('records')
const saving = ref(false)

const CHANNEL_TEXT = { SMS: '短信', WECHAT: '微信', PUSH: 'APP推送', INBOX: '站内信' }
const STATUS_TEXT = { PENDING: '待发', SENT: '已发送', FAILED: '失败待重试', GIVEN_UP: '已放弃' }

const records = ref([])
const inbox = ref([])
const templates = ref([])
const selected = ref([])
const inboxUnread = ref(true)
const filter = reactive({ channel: '', status: '', bizKey: '' })

const channelTag = (c) => (c === 'SMS' ? 'primary' : c === 'WECHAT' ? 'success' : c === 'PUSH' ? 'warning' : 'info')
const statusTag = (s) => (s === 'SENT' ? 'success' : s === 'PENDING' ? 'info' : s === 'FAILED' ? 'warning' : 'danger')
const countStatus = (s) => records.value.filter((x) => x.status === s).length

async function loadRecords() {
  records.value = await listMessages({
    channel: filter.channel || undefined,
    status: filter.status || undefined,
    bizKey: filter.bizKey || undefined,
    limit: 200
  })
}

async function loadInbox() {
  inbox.value = await listInbox({
    orgId: userStore.userInfo.orgId,
    unread: inboxUnread.value ? true : undefined,
    limit: 100
  })
}

async function loadTemplates() {
  templates.value = await listTemplates({ orgId: userStore.userInfo.orgId })
}

async function doDispatch() {
  const n = await dispatchMessages(200)
  ElMessage.success(`已投递 ${n} 条`)
  await Promise.all([loadRecords(), loadInbox()])
}

async function doResend(row) {
  const r = await resendMessage(row.id)
  ElMessage.success(`重发结果：${STATUS_TEXT[r.status] || r.status}${r.failReason ? '（' + r.failReason + '）' : ''}`)
  await loadRecords()
}

async function doMarkRead() {
  const n = await markInboxRead(selected.value.map((x) => x.id))
  ElMessage.success(`已标记 ${n} 条为已读`)
  await loadInbox()
}

// ---------------------------------------------------------------- 模板
const tplVisible = ref(false)
const tplTitle = ref('')
const tplEditingId = ref(null)
const tplFormRef = ref()
const tplForm = reactive({
  templateCode: '',
  templateName: '',
  bizType: '',
  channel: 'INBOX',
  receiverType: 'ORG',
  titleTpl: '',
  contentTpl: '',
  enabled: 1,
  remark: ''
})
const tplRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  bizType: [{ required: true, message: '请输入业务节点', trigger: 'blur' }],
  contentTpl: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
}

function resetTpl() {
  Object.assign(tplForm, {
    templateCode: '',
    templateName: '',
    bizType: '',
    channel: 'INBOX',
    receiverType: 'ORG',
    titleTpl: '',
    contentTpl: '',
    enabled: 1,
    remark: ''
  })
  tplEditingId.value = null
}

function openTplCreate() {
  resetTpl()
  tplTitle.value = '新增模板'
  tplVisible.value = true
}

function openTplEdit(row) {
  resetTpl()
  tplEditingId.value = row.id
  Object.assign(tplForm, {
    templateCode: row.templateCode,
    templateName: row.templateName,
    bizType: row.bizType,
    channel: row.channel,
    receiverType: row.receiverType,
    titleTpl: row.titleTpl,
    contentTpl: row.contentTpl,
    enabled: row.enabled,
    remark: row.remark
  })
  tplTitle.value = '编辑模板'
  tplVisible.value = true
}

function tplPayload() {
  return {
    orgId: userStore.userInfo.orgId,
    templateCode: tplForm.templateCode,
    templateName: tplForm.templateName,
    bizType: tplForm.bizType,
    channel: tplForm.channel,
    receiverType: tplForm.receiverType,
    titleTpl: tplForm.titleTpl,
    contentTpl: tplForm.contentTpl,
    enabled: tplForm.enabled,
    remark: tplForm.remark,
    operateBy: userStore.userInfo.username || 'admin'
  }
}

async function saveTemplate() {
  await tplFormRef.value.validate()
  saving.value = true
  try {
    if (tplEditingId.value) {
      await updateTemplate(tplEditingId.value, tplPayload())
    } else {
      await createTemplate(tplPayload())
    }
    ElMessage.success('已保存')
    tplVisible.value = false
    await loadTemplates()
  } finally {
    saving.value = false
  }
}

async function toggleTemplate(row, on) {
  try {
    await updateTemplate(row.id, { ...tplPayloadOf(row), enabled: on ? 1 : 0 })
    ElMessage.success(on ? '已启用' : '已停用')
  } finally {
    await loadTemplates()
  }
}

function tplPayloadOf(row) {
  return {
    orgId: row.orgId,
    templateCode: row.templateCode,
    templateName: row.templateName,
    bizType: row.bizType,
    channel: row.channel,
    receiverType: row.receiverType,
    titleTpl: row.titleTpl,
    contentTpl: row.contentTpl,
    enabled: row.enabled,
    remark: row.remark,
    operateBy: userStore.userInfo.username || 'admin'
  }
}

async function doTplDelete(row) {
  await ElMessageBox.confirm(`确认删除模板「${row.templateCode}」？已发送记录不受影响。`, '提示', { type: 'warning' })
  await deleteTemplate(row.id)
  ElMessage.success('已删除')
  await loadTemplates()
}

// ---------------------------------------------------------------- 模拟发送
const notifyVisible = ref(false)
const notifyForm = reactive({
  bizType: 'TRACK_ALARM',
  bizKey: '',
  alarmType: 'OVERSPEED',
  content: '超速：连续 3 个点速度 98km/h，超过限值 90km/h',
  driverId: null,
  driverPhone: ''
})

function openNotify() {
  notifyForm.bizKey = 'demo-' + Date.now()
  notifyVisible.value = true
}

async function doNotify() {
  const receivers = []
  if (notifyForm.driverId) {
    receivers.push({ type: 'DRIVER', id: notifyForm.driverId, address: notifyForm.driverPhone || null })
  }
  receivers.push({ type: 'ORG', id: userStore.userInfo.orgId })
  saving.value = true
  try {
    const n = await notifyMessage({
      orgId: userStore.userInfo.orgId,
      bizType: notifyForm.bizType,
      bizKey: notifyForm.bizKey,
      vars: {
        alarmType: notifyForm.alarmType,
        alarmLevel: 'HIGH',
        content: notifyForm.content,
        waybillId: 'DEMO',
        alarmTime: new Date().toLocaleString(),
        waybillNo: 'DEMO',
        status: 'PUBLISHED'
      },
      receivers
    })
    ElMessage.success(n > 0 ? `已生成 ${n} 条待发消息，点「投递待发」即可发送` : '无匹配模板或已发送过（幂等）')
    notifyVisible.value = false
    tab.value = 'records'
    await loadRecords()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRecords()
  await loadInbox()
  await loadTemplates()
})
</script>
