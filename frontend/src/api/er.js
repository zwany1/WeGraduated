import api from './index'

export function renderErDiagram(data) {
  return api.post('/er/render', data, { responseType: 'blob' })
}

export function getErGraph(data) {
  return api.post('/er/graph', data)
}

export function saveErLayout(positions) {
  return api.post('/er/save-layout', positions)
}

export function loadErLayout() {
  return api.get('/er/load-layout')
}
