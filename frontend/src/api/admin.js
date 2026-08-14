import api from './index'

export function getOverview() {
  return api.get('/admin/stats/overview')
}

export function listUsers(params) {
  return api.get('/admin/users', { params })
}

export function setUserRole(id, role) {
  return api.put(`/admin/users/${id}/role`, { role })
}

export function deleteUser(id) {
  return api.delete(`/admin/users/${id}`)
}

export function listTemplates(params) {
  return api.get('/admin/templates', { params })
}

export function deleteTemplate(id) {
  return api.delete(`/admin/templates/${id}`)
}

export function listTasks(params) {
  return api.get('/admin/tasks', { params })
}
