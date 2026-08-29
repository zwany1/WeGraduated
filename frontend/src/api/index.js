import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  res => {
    if (res.config.responseType === 'blob' || res.data instanceof Blob) {
      return res.data
    }
    if (res.data.code === 200) {
      return res.data.data
    }
    ElMessage.error(res.data.message || '请求失败')
    return Promise.reject(new Error(res.data.message || '请求失败'))
  },
  err => {
    if (err.response && err.response.status === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
      localStorage.removeItem('role')
      localStorage.removeItem('roles')
      localStorage.removeItem('perms')
      localStorage.removeItem('menus')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if ([502, 503, 504].includes(err.response?.status)) {
      ElMessage.error('后端服务未启动或正在重启，请稍后重试')
    } else {
      const msg = err.response?.data?.message || err.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

// 登录过期预警: token 剩余有效期不足 2 小时时提醒一次, 避免用户编辑内容因过期丢失
let expiryWarned = false
function scheduleExpiryWarning() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    const msLeft = payload.exp * 1000 - Date.now()
    if (msLeft <= 0) return
    if (msLeft < 2 * 3600 * 1000 && !expiryWarned) {
      expiryWarned = true
      ElMessage.warning('登录即将过期，请尽快保存配置并重新登录')
    }
    // 剩余时间很长则半小时后复查, 临近则按剩余时间到期时复查
    setTimeout(scheduleExpiryWarning, Math.min(msLeft, 30 * 60 * 1000))
  } catch (e) {
    // token 格式异常, 忽略
  }
}
scheduleExpiryWarning()

export default api
