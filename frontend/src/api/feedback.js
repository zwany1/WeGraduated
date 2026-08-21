import api from './index'

// ============ 用户反馈(前台, 登录即可) ============

export function createFeedback(data) {
  return api.post('/feedback', data)
}

export function listFeedbacks(params) {
  return api.get('/feedback/list', { params })
}

export function getFeedbackDetail(id) {
  return api.get(`/feedback/${id}`)
}
