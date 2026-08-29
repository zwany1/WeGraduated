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

export default api
