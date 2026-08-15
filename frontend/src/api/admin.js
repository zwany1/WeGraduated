import api from './index'

// ============ 概览 ============

export function getOverview() {
  return api.get('/admin/stats/overview')
}

// ============ 用户管理 ============

export function listUsers(params) {
  return api.get('/admin/users', { params })
}

export function setUserRole(id, role) {
  return api.put(`/admin/users/${id}/role`, { role })
}

export function assignUserRoles(userId, roleIds) {
  return api.put(`/admin/users/${userId}/roles`, { roleIds })
}

export function deleteUser(id) {
  return api.delete(`/admin/users/${id}`)
}

// ============ 模板管理 ============

export function listTemplates(params) {
  return api.get('/admin/templates', { params })
}

export function deleteTemplate(id) {
  return api.delete(`/admin/templates/${id}`)
}

// ============ 排版任务 ============

export function listTasks(params) {
  return api.get('/admin/tasks', { params })
}

// ============ 菜单管理 ============

export function getMenuTree() {
  return api.get('/admin/system/menu/tree')
}

export function getMenus() {
  return api.get('/admin/system/menu/user-menus')
}

export function createMenu(data) {
  return api.post('/admin/system/menu', data)
}

export function updateMenu(data) {
  return api.put('/admin/system/menu', data)
}

export function deleteMenu(id) {
  return api.delete(`/admin/system/menu/${id}`)
}

// ============ 角色管理 ============

export function listRoles(params) {
  return api.get('/admin/system/role/page', { params })
}

export function listAllRoles() {
  return api.get('/admin/system/role/all')
}

export function createRole(data) {
  return api.post('/admin/system/role', data)
}

export function updateRole(data) {
  return api.put('/admin/system/role', data)
}

export function deleteRole(id) {
  return api.delete(`/admin/system/role/${id}`)
}

export function getRoleMenus(id) {
  return api.get(`/admin/system/role/${id}/menus`)
}

export function assignRoleMenus(roleId, menuIds) {
  return api.put('/admin/system/role/assign-menus', { roleId, menuIds })
}
