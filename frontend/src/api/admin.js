import api from './index'

// ============ 概览 ============

export function getOverview() {
  return api.get('/admin/stats/overview')
}

export function getSuccessRate() {
  return api.get('/admin/stats/success-rate')
}

export function getFailures() {
  return api.get('/admin/stats/failures')
}

export function getTopTemplates() {
  return api.get('/admin/stats/top-templates')
}

export function getTopUsers() {
  return api.get('/admin/stats/top-users')
}

// ============ 报表导出 ============

export function exportUsers() {
  return api.get('/admin/export/users', { responseType: 'blob' })
}

export function exportTemplates() {
  return api.get('/admin/export/templates', { responseType: 'blob' })
}

export function exportTasks() {
  return api.get('/admin/export/tasks', { responseType: 'blob' })
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

export function updateUserStatus(id, status) {
  return api.put(`/admin/users/${id}/status`, { status })
}

export function resetUserPassword(id, password) {
  return api.put(`/admin/users/${id}/password`, { password })
}

export function getUserDetail(id) {
  return api.get(`/admin/users/${id}/detail`)
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

// ============ 模板市场审核 ============

export function listMarketTemplates(params) {
  return api.get('/admin/market/templates', { params })
}

export function setMarketTemplate(id, data) {
  return api.put(`/admin/market/templates/${id}`, data)
}

// ============ 在线会话 ============

export function listOnlineSessions() {
  return api.get('/admin/session/online')
}

export function kickSession(id) {
  return api.delete(`/admin/session/${id}`)
}

export function getMarketTemplateDetail(id) {
  return api.get(`/admin/market/templates/${id}/detail`)
}

// ============ 排版任务 ============

export function listTasks(params) {
  return api.get('/admin/tasks', { params })
}

export function getTaskDetail(id) {
  return api.get(`/admin/tasks/${id}/detail`)
}

export function rerunTask(id) {
  return api.post(`/admin/tasks/${id}/rerun`)
}

export function cancelTask(id) {
  return api.post(`/admin/tasks/${id}/cancel`)
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

// ============ 日志审计 ============

export function listOperLogs(params) {
  return api.get('/admin/system/log/oper', { params })
}

export function deleteOperLogs(ids) {
  return api.delete('/admin/system/log/oper', { data: ids })
}

export function listLoginLogs(params) {
  return api.get('/admin/system/log/login', { params })
}

export function deleteLoginLogs(ids) {
  return api.delete('/admin/system/log/login', { data: ids })
}

// ============ 字典管理 ============

export function listDictTypes(params) {
  return api.get('/admin/system/dict/type/page', { params })
}

export function listAllDictTypes() {
  return api.get('/admin/system/dict/type/all')
}

export function saveDictType(data) {
  return data.id ? api.put('/admin/system/dict/type', data) : api.post('/admin/system/dict/type', data)
}

export function deleteDictType(id) {
  return api.delete(`/admin/system/dict/type/${id}`)
}

export function listDictData(dictType) {
  return api.get('/admin/system/dict/data', { params: { dictType } })
}

export function pageDictData(params) {
  return api.get('/admin/system/dict/data/page', { params })
}

export function saveDictData(data) {
  return data.id ? api.put('/admin/system/dict/data', data) : api.post('/admin/system/dict/data', data)
}

export function deleteDictData(id) {
  return api.delete(`/admin/system/dict/data/${id}`)
}

// ============ 参数设置 ============

export function listConfigs(params) {
  return api.get('/admin/system/config/page', { params })
}

export function saveConfig(data) {
  return data.id ? api.put('/admin/system/config', data) : api.post('/admin/system/config', data)
}

export function deleteConfig(id) {
  return api.delete(`/admin/system/config/${id}`)
}

// ============ 公告管理 ============

export function listNotices(params) {
  return api.get('/admin/system/notice/page', { params })
}

export function saveNotice(data) {
  return data.id ? api.put('/admin/system/notice', data) : api.post('/admin/system/notice', data)
}

export function deleteNotice(id) {
  return api.delete(`/admin/system/notice/${id}`)
}

export function listPublicNotices(limit = 5) {
  return api.get('/public/notice/list', { params: { limit } })
}

// ============ 数据备份 ============

export function backupNow() {
  return api.post('/admin/system/backup')
}

export function listBackups() {
  return api.get('/admin/system/backup/list')
}

export function downloadBackup(name) {
  return api.get(`/admin/system/backup/download/${name}`, { responseType: 'blob' })
}

export function deleteBackup(name) {
  return api.delete(`/admin/system/backup/${name}`)
}
