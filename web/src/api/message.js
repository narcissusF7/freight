import request from '@/utils/request'

// 消息记录（四通道统一表）：status/channel/bizType/bizKey 均可选
export function listMessages(params) {
  return request.get('/messages', { params })
}

// 站内信收件箱（INBOX 通道）
export function listInbox(params) {
  return request.get('/messages/inbox', { params })
}

export function markInboxRead(ids) {
  return request.post('/messages/inbox/read', { ids })
}

// 手动投递到期消息（等价于调度器跑一轮）
export function dispatchMessages(limit) {
  return request.post('/messages/dispatch', null, { params: { limit } })
}

// 重发：重置重试计数并立即投递一次
export function resendMessage(id) {
  return request.post(`/messages/${id}/resend`)
}

// 手工发消息（演示/补发）：按业务节点模板群发
export function notifyMessage(data) {
  return request.post('/messages/notify', data)
}

// ---------------------------------------------------------------- 模板（节点配置化）
export function listTemplates(params) {
  return request.get('/message-templates', { params })
}
export function createTemplate(data) {
  return request.post('/message-templates', data)
}
export function updateTemplate(id, data) {
  return request.put(`/message-templates/${id}`, data)
}
export function deleteTemplate(id) {
  return request.delete(`/message-templates/${id}`)
}
