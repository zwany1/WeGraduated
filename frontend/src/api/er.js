import api from './index'

/** 解析建表 SQL 为实体/关系(多表+外键); 类图 SQL 导入也复用此接口 */
export function parseErSql(sql) {
  return api.post('/er/parse-sql', { sql })
}

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
