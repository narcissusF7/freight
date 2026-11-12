import request from '@/utils/request'

// ---------------- 结算单 ----------------
export function listSettlements(params) {
  return request.get('/settlements', { params })
}
export function getSettlement(id) {
  return request.get(`/settlements/${id}`)
}
export function settlementByWaybill(waybillId, bizType) {
  return request.get('/settlements/by-waybill', { params: { waybillId, bizType } })
}
// 金额由服务端按运单运价计算，这里只传附加费/扣款
export function generateSettlement(data) {
  return request.post('/settlements', data, { params: { operator: data.operator || 'web' } })
}
export function confirmSettlement(id, operator = 'web') {
  return request.post(`/settlements/${id}/confirm`, null, { params: { operator } })
}
export function paySettlement(id, operator = 'web') {
  return request.post(`/settlements/${id}/pay`, null, { params: { operator } })
}
export function voidSettlement(id, reason, operator = 'web') {
  return request.post(`/settlements/${id}/void`, null, { params: { reason, operator } })
}

// ---------------- 支付单 ----------------
export function listPayments(params) {
  return request.get('/payments', { params })
}
export function getPayment(payNo) {
  return request.get(`/payments/${payNo}`)
}
export function executePayment(payNo) {
  return request.post(`/payments/${payNo}/execute`)
}
export function queryPaymentBank(payNo) {
  return request.get(`/payments/${payNo}/bank`)
}
export function closePayment(payNo, reason) {
  return request.post(`/payments/${payNo}/close`, null, { params: { reason, operator: 'web' } })
}
export function bankFile(bizDate) {
  return request.get('/payments/bank-file', { params: { bizDate } })
}

// ---------------- 钱包 / 流水 / 提现 ----------------
export function listWallets(orgId) {
  return request.get('/wallets', { params: { orgId } })
}
export function walletDetail(ownerType, ownerId) {
  return request.get('/wallets/detail', { params: { ownerType, ownerId } })
}
export function walletFlows(ownerType, ownerId, limit = 100) {
  return request.get('/wallets/flows', { params: { ownerType, ownerId, limit } })
}
export function applyWithdraw(data) {
  return request.post('/wallets/withdrawals', data)
}
export function listWithdrawals(params) {
  return request.get('/wallets/withdrawals', { params })
}
export function auditWithdraw(id, pass, remark, operator = 'web') {
  return request.post(`/wallets/withdrawals/${id}/audit`, null, { params: { pass, remark, operator } })
}
export function retryWithdraw(id, operator = 'web') {
  return request.post(`/wallets/withdrawals/${id}/retry`, null, { params: { operator } })
}

// ---------------- 发票 ----------------
export function listInvoices(params) {
  return request.get('/invoices', { params })
}
export function issueInvoice(data) {
  return request.post('/invoices', data)
}
export function voidInvoice(id, reason, operator = 'web') {
  return request.post(`/invoices/${id}/void`, null, { params: { reason, operator } })
}

// ---------------- 对账 ----------------
export function runReconciliation(bizDate, operator = 'web') {
  return request.post('/reconciliation/run', null, { params: { bizDate, operator } })
}
export function latestReconciliation(bizDate) {
  return request.get('/reconciliation', { params: { bizDate } })
}
export function reconDiffs(reconId) {
  return request.get(`/reconciliation/${reconId}/diffs`)
}
export function handleReconDiff(diffId, remark, handleBy = 'web') {
  return request.post(`/reconciliation/diffs/${diffId}/handle`, null, { params: { handleBy, remark } })
}
