import { request, upload } from './request'

/** 登录 / 账号 */
export const login = (phone, password) =>
  request({ url: '/api/driver/login', method: 'POST', data: { phone, password } })
export const initPassword = (phone, smsCode, newPassword) =>
  request({ url: '/api/driver/password/init', method: 'POST', data: { phone, smsCode, newPassword } })
export const changePassword = (oldPassword, newPassword) =>
  request({ url: '/api/driver/password', method: 'POST', data: { oldPassword, newPassword } })
export const profile = () => request({ url: '/api/driver/profile' })
export const setOnline = (online) =>
  request({ url: '/api/driver/online?online=' + online, method: 'POST' })
export const registerPush = (clientId) =>
  request({ url: '/api/driver/push/register', method: 'POST', data: { clientId }, silent: true })

/** 货源 / 接单 */
export const hall = (lat, lng, limit = 20, maxDistanceKm) => {
  const q = []
  if (lat !== null && lat !== undefined) q.push('lat=' + lat)
  if (lng !== null && lng !== undefined) q.push('lng=' + lng)
  q.push('limit=' + limit)
  if (maxDistanceKm) q.push('maxDistanceKm=' + maxDistanceKm)
  return request({ url: '/api/driver/goods-sources?' + q.join('&') })
}
export const accept = (goodsSourceId, vehicleId) =>
  request({
    url: '/api/driver/goods-sources/' + goodsSourceId + '/accept',
    method: 'POST',
    data: { vehicleId }
  })

/** 我的运单 */
export const myWaybills = (scope) =>
  request({ url: '/api/driver/waybills?scope=' + (scope || 'ALL') })
export const waybillDetail = (id, lat, lng) => {
  const q = []
  if (lat !== null && lat !== undefined) q.push('lat=' + lat)
  if (lng !== null && lng !== undefined) q.push('lng=' + lng)
  return request({ url: '/api/driver/waybills/' + id + (q.length ? '?' + q.join('&') : '') })
}
export const confirmNode = (id, payload) =>
  request({ url: '/api/driver/waybills/' + id + '/nodes', method: 'POST', data: payload })
export const uploadReceipt = (id, filePath, remark) =>
  upload({ url: '/api/driver/waybills/' + id + '/receipt', filePath, formData: { remark: remark || '' } })
export const reportException = (id, payload) =>
  request({ url: '/api/driver/waybills/' + id + '/exception', method: 'POST', data: payload })

/** 评价货主（方向与评价人由服务端按令牌确定） */
export const myRating = (waybillId) =>
  request({ url: '/api/driver/ratings?waybillId=' + waybillId, silent: true })
export const submitRating = (payload) =>
  request({ url: '/api/driver/ratings', method: 'POST', data: payload })

/** 待办 / 消息 / 轨迹 */
export const todo = () => request({ url: '/api/driver/todo' })
export const messages = (unread) =>
  request({ url: '/api/driver/messages' + (unread ? '?unread=true' : '') })
export const readMessages = (ids) =>
  request({ url: '/api/driver/messages/read', method: 'POST', data: ids })
export const reportTracks = (points) =>
  request({ url: '/api/driver/tracks/batch', method: 'POST', data: { points }, silent: true })
