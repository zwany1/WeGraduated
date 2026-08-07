import api from './index'

export function generateDiagram(data) {
  return api.post('/diagram/generate', data)
}

export function saveDiagram(data) {
  return api.post('/diagram/save', data)
}

export function listDiagrams() {
  return api.get('/diagram/list')
}

export function loadDiagram(id) {
  return api.get('/diagram/load', { params: { id } })
}

export function deleteDiagram(id) {
  return api.post('/diagram/delete', null, { params: { id } })
}
