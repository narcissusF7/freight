import request from '@/utils/request'

// ---------------- 投诉 ----------------
export function listComplaints(params) {
  return request.get('/complaints', { params })
}
export function complaintStats(orgId) {
  return request.get('/complaints/stats', { params: { orgId } })
}
export function getComplaint(id) {
  return request.get(`/complaints/${id}`)
}
export function createComplaint(data) {
  return request.post('/complaints', data)
}
export function assignComplaint(id, data) {
  return request.post(`/complaints/${id}/assign`, data)
}
export function followComplaint(id, data) {
  return request.post(`/complaints/${id}/follow`, data)
}
export function resolveComplaint(id, data) {
  return request.post(`/complaints/${id}/resolve`, data)
}
export function closeComplaint(id, data) {
  return request.post(`/complaints/${id}/close`, data)
}
export function rejectComplaint(id, data) {
  return request.post(`/complaints/${id}/reject`, data)
}
// 生产由调度器每分钟跑；这里留手动入口便于演示与排障
export function scanOverdue(operator = 'web') {
  return request.post('/complaints/scan-overdue', null, { params: { operator } })
}

// ---------------- 评价 ----------------
export function listRatings(params) {
  return request.get('/ratings', { params })
}
export function ratingSummary(targetType, targetId) {
  return request.get('/ratings/summary', { params: { targetType, targetId } })
}
export function submitRating(data) {
  return request.post('/ratings', data)
}
export function replyRating(id, content, operator = 'web') {
  return request.post(`/ratings/${id}/reply`, { content, operator })
}
export function hideRating(id, reason, operator = 'web') {
  return request.post(`/ratings/${id}/hide`, null, { params: { reason, operator } })
}

// ---------------- 信用 ----------------
export function listCredits(params) {
  return request.get('/credits', { params })
}
export function creditDetail(subjectType, subjectId) {
  return request.get('/credits/detail', { params: { subjectType, subjectId } })
}
export function adjustCredit(data) {
  return request.post('/credits/adjust', data)
}
