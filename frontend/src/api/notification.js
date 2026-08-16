import api from './index'

export function listNotifications() {
  return api.get('/notification/list')
}

export function unreadCount() {
  return api.get('/notification/unread-count')
}

export function markNotificationRead(id) {
  return api.post(`/notification/${id}/read`)
}

export function markAllNotificationsRead() {
  return api.post('/notification/read-all')
}

export function acceptTeamInvite(inviteId) {
  return api.post(`/team/invite/${inviteId}/accept`)
}

export function rejectTeamInvite(inviteId) {
  return api.post(`/team/invite/${inviteId}/reject`)
}
