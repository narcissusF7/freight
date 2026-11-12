import request from '@/utils/request'

// 货源发布
export function listGoodsSources(orgId) {
  return request.get('/goods-sources', { params: { orgId } })
}
export function createGoodsSource(data, operator) {
  return request.post('/goods-sources', data, { params: { operator } })
}
export function closeGoodsSource(id, operator) {
  return request.post(`/goods-sources/${id}/close`, null, { params: { operator } })
}

// 运单
export function listWaybills(orgId) {
  return request.get('/waybills', { params: { orgId } })
}
export function getWaybill(id) {
  return request.get(`/waybills/${id}`)
}
export function getWaybillNodes(id) {
  return request.get(`/waybills/${id}/nodes`)
}
export function createWaybill(data) {
  return request.post('/waybills', data)
}
export function changeWaybillStatus(id, data) {
  return request.post(`/waybills/${id}/status`, data)
}

// 车货匹配（返回候选司机+车辆及评分明细）
export function matchCarriers(goodsSourceId, limit) {
  return request.get('/waybills/match', { params: { goodsSourceId, limit } })
}

// 异常件
export function listExceptions(waybillId) {
  return request.get(`/waybills/${waybillId}/exceptions`)
}
export function reportException(waybillId, data) {
  return request.post(`/waybills/${waybillId}/exceptions`, data)
}
export function handleException(exceptionId, data) {
  return request.post(`/waybills/exceptions/${exceptionId}/handle`, data)
}

// 回单
export function listReceipts(waybillId) {
  return request.get(`/waybills/${waybillId}/receipts`)
}
export function uploadReceipt(waybillId, file, operator, remark) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post(`/waybills/${waybillId}/receipts`, fd, {
    params: { operator, remark },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export function verifyReceipt(receiptId, operator) {
  return request.post(`/waybills/receipts/${receiptId}/verify`, null, { params: { operator } })
}
