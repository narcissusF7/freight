import request from '@/utils/request'

// ---------------- 监管上报 ----------------
export function listReports(params) {
  return request.get('/reports', { params })
}
export function reportStats(orgId) {
  return request.get('/reports/stats', { params: { orgId } })
}
export function scanReports(limit = 100) {
  return request.post('/reports/scan', null, { params: { limit } })
}
export function retryReports(limit = 50) {
  return request.post('/reports/retry', null, { params: { limit } })
}
export function reportOne(docType, bizNo) {
  return request.post('/reports/one', null, { params: { docType, bizNo, operator: 'web' } })
}
export function retryRecord(id) {
  return request.post(`/reports/records/${id}/retry`, null, { params: { operator: 'web' } })
}
