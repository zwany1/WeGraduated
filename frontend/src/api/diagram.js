import api from './index'

export function generateDiagram(data) {
  return api.post('/diagram/generate', data)
}

export function saveDiagram(data) {
  return api.post('/diagram/save', data)
}

/**
 * 结构化配置 -> 文本 DSL。
 * 画布上改名/改消息文案后调用，把改动换回文本，保证文本视图不被旧文本覆盖。
 */
export function renderDiagramDsl(type, config) {
  return api.post('/diagram/dsl/render', { type, config })
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
