import api from './index'

export function register(data) {
  return api.post('/user/register', data)
}

export function login(data) {
  return api.post('/user/login', data)
}

// 查询账号是否需要图形验证码(失败次数达阈值时前端才显示验证码)
export function needCaptcha(account) {
  return api.get('/user/need-captcha', { params: { account } })
}

export function getProfile() {
  return api.get('/user/profile')
}

export function getUserInfo() {
  return api.get('/user/info')
}

export function updateProfile(data) {
  return api.put('/user/profile', data)
}

export function logout() {
  return api.post('/user/logout')
}

export function deleteAccount() {
  return api.delete('/user/account')
}

export function sendEmailCode(data) {
  return api.post('/user/send-email-code', data)
}

export function resetPassword(data) {
  return api.post('/user/reset-password', data)
}
