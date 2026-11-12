import request from '@/utils/request'

// 电子合同（托运框架协议 / 司机运输合同）
export function listContracts(orgId) {
  return request.get('/contracts', { params: { orgId } })
}
export function getContract(id) {
  return request.get(`/contracts/${id}`)
}
export function createContract(data) {
  return request.post('/contracts', data)
}
// party: A / B
export function signContract(id, party, operator) {
  return request.post(`/contracts/${id}/sign`, null, { params: { party, operator } })
}
// content 不传则按库内正文验签；传了则以该内容重算摘要（用于演示篡改失败）
export function verifyContract(id, content, party) {
  return request.post(`/contracts/${id}/verify`, null, { params: { content, party } })
}
