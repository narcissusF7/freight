import request from '@/utils/request'

// 批量上报轨迹点（APP 攒批，服务端按 waybillId+gpsTime 幂等去重）
export function reportTrackBatch(points) {
  return request.post('/tracks/batch', { points })
}

// 时间窗查询（升序），maxPoints 控制抽稀上限
export function listTrack(waybillId, from, to, maxPoints) {
  return request.get('/tracks', { params: { waybillId, from, to, maxPoints } })
}

// 轨迹摘要：点数/里程/均速/最高速/漂移剔除数
export function trackSummary(waybillId, from, to) {
  return request.get('/tracks/summary', { params: { waybillId, from, to } })
}

// ---------------------------------------------------------------- 电子围栏
export function listFences(params) {
  return request.get('/fences', { params })
}
export function createFence(data) {
  return request.post('/fences', data)
}
export function updateFence(id, data) {
  return request.put(`/fences/${id}`, data)
}
export function deleteFence(id) {
  return request.delete(`/fences/${id}`)
}

// ---------------------------------------------------------------- 在途预警
export function listAlarms(params) {
  return request.get('/tracks/alarms', { params })
}
export function handleAlarm(id, data) {
  return request.post(`/tracks/alarms/${id}/handle`, data)
}
// 人工补算：对历史时间窗重跑规则（服务端按 dedup_key 幂等）
export function analyzeTrackAlarms(waybillId, from, to) {
  return request.post('/tracks/analyze', null, { params: { waybillId, from, to } })
}

// ---------------------------------------------------------------- 分区运维
export function listTrackPartitions() {
  return request.get('/tracks/maintenance/partitions')
}
export function ensureTrackPartition(month) {
  return request.post('/tracks/maintenance/partition', null, { params: { month } })
}
export function archiveTrackPartitions(before, dryRun) {
  return request.post('/tracks/maintenance/archive', null, { params: { before, dryRun } })
}
