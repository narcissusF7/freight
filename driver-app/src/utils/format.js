/** 与后端 DriverWaybillServiceImpl.statusText 保持一致；前端只做展示，不参与状态判断。 */
const STATUS_TEXT = {
  DRAFT: '草稿',
  PUBLISHED: '待接单',
  ACCEPTED: '待装货',
  LOADED: '待发车',
  IN_TRANSIT: '运输中',
  UNLOADED: '待签收',
  SIGNED: '待回单',
  RECEIPT_UPLOADED: '待结算',
  SETTLED: '已结算',
  REPORTED: '已上报',
  COMPLETED: '已完成',
  EXCEPTION: '异常处理中',
  WITHDRAWN: '已撤回',
  VOID: '已作废'
}

const STATUS_COLOR = {
  ACCEPTED: '#2f6bff',
  LOADED: '#7a5cff',
  IN_TRANSIT: '#0d9488',
  UNLOADED: '#c2751b',
  SIGNED: '#c2751b',
  EXCEPTION: '#d9363e'
}

export const statusText = (s) => STATUS_TEXT[s] || s || '-'
export const statusColor = (s) => STATUS_COLOR[s] || '#8a94a6'

export function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export function num(v, unit = '') {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v) + unit
}

/** "2026-11-10T03:08:00" → "11-10 03:08" */
export function shortTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(5, 16)
}

export function fullTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

/** 米 → 「320 米」/「12.3 公里」 */
export function distanceText(m) {
  if (m === null || m === undefined) return ''
  if (m < 1000) return m + ' 米'
  return (m / 1000).toFixed(1) + ' 公里'
}
