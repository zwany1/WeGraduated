import api from './index'

export function register(data) {
  return api.post('/user/register', data)
}

export function login(data) {
  return api.post('/user/login', data)
}

export function getProfile() {
  return api.get('/user/profile')
}

export function updateProfile(data) {
  return api.put('/user/profile', data)
}

export function logout() {
  return api.post('/user/logout')
}
