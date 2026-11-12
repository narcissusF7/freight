import request from '@/utils/request'

// 文件上传：multipart/form-data，字段 orgId / bizType / file
export function uploadFile(formData) {
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 文件访问地址
export function getFileUrl(id) {
  return request.get(`/files/${id}/url`)
}
