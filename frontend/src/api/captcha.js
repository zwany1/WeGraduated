import api from './index'

export function generateCaptcha() {
  return api.get('/captcha/generate')
}
