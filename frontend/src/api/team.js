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

export function leaveTeam(id) {
  return api.post(`/team/${id}/leave`)
}

export function deleteTeam(id) {
  return api.delete(`/team/${id}`)
}
