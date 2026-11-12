import request from '@/utils/request'

// 司机
export function listDrivers(orgId) {
  return request.get('/drivers', { params: { orgId } })
}
export function createDriver(data) {
  return request.post('/drivers', data)
}
export function updateDriver(id, data) {
  return request.put(`/drivers/${id}`, data)
}
export function deleteDriver(id) {
  return request.delete(`/drivers/${id}`)
}
export function auditDriver(id, params) {
  return request.post(`/drivers/${id}/audit`, null, { params })
}

// 车辆
export function listVehicles(orgId) {
  return request.get('/vehicles', { params: { orgId } })
}
export function createVehicle(data) {
  return request.post('/vehicles', data)
}
export function updateVehicle(id, data) {
  return request.put(`/vehicles/${id}`, data)
}
export function deleteVehicle(id) {
  return request.delete(`/vehicles/${id}`)
}
export function auditVehicle(id, params) {
  return request.post(`/vehicles/${id}/audit`, null, { params })
}

// 证照 OCR（结果仅用于表单预填，落库仍走新增/编辑接口人工确认）
export function ocrRecognize(file, type, orgId) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/carrier/ocr', fd, {
    params: { type, orgId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 证件到期预警
export function listExpiring(orgId, days) {
  return request.get('/carrier/expiring', { params: { orgId, days } })
}
