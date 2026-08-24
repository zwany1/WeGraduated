import api from './index'

export function createTeam(data) {
  return api.post('/team/create', data)
}

export function listTeams() {
  return api.get('/team/list')
}

export function getTeamDetail(id) {
  return api.get(`/team/${id}`)
}

export function inviteMember(id, keyword) {
  return api.post(`/team/${id}/invite`, { keyword })
}

export function removeMember(id, userId) {
  return api.delete(`/team/${id}/member/${userId}`)
}

// 设置成员角色(admin/editor/viewer/member)
export function setMemberRole(id, userId, role) {
  return api.put(`/team/${id}/member/${userId}/role`, { role })
}

// 转让队长所有权(原队长降为管理员)
export function transferOwnership(id, newOwnerId) {
  return api.post(`/team/${id}/transfer`, { newOwnerId })
}

export function leaveTeam(id) {
  return api.post(`/team/${id}/leave`)
}

export function deleteTeam(id) {
  return api.delete(`/team/${id}`)
}
