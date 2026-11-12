import request from '@/utils/request'

export function statOverview(params) {
  return request.get('/stat/overview', { params })
}
export function statTrend(params) {
  return request.get('/stat/trend', { params })
}
export function statDims(params) {
  return request.get('/stat/dims', { params })
}
export function statDrill(params) {
  return request.get('/stat/drill', { params })
}
export function statRebuild(params) {
  return request.post('/stat/rebuild', null, { params })
}
export function statMetrics() {
  return request.get('/stat/metrics')
}
