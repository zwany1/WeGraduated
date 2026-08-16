import api from './index'

export function createTemplate(name) {
  return api.post('/template/create', { name })
}

export function listTemplates() {
  return api.get('/template/list')
}

export function getTemplateDetail(id) {
  return api.get(`/template/detail/${id}`)
}

export function deleteTemplate(id) {
  return api.delete(`/template/delete/${id}`)
}

export function savePageConfig(id, pageConfig) {
  return api.put(`/template/${id}/page-config`, { pageConfig })
}

export function saveHeadingPatterns(id, headingPatterns) {
  return api.put(`/template/${id}/heading-patterns`, headingPatterns)
}

export function saveCoverConfig(id, coverConfig) {
  return api.put(`/template/${id}/cover-config`, { pageConfig: JSON.stringify(coverConfig) })
}

export function saveGenerateToc(id, generateToc) {
  return api.put(`/template/${id}/generate-toc`, { generateToc })
}

export function saveReferenceConfig(id, referenceConfig) {
  return api.put(`/template/${id}/reference-config`, { referenceConfig: JSON.stringify(referenceConfig) })
}

export function saveRule(rule) {
  return api.post('/template/rule/save', rule)
}

export function listRules(id) {
  return api.get(`/template/${id}/rules`)
}

// ============ 模板市场 ============

export function listMarketTemplates(params) {
  return api.get('/template/market/list', { params })
}

export function listMarketCategories() {
  return api.get('/template/market/categories')
}

export function getMarketTemplateDetail(id) {
  return api.get(`/template/market/${id}/detail`)
}

export function toggleFavoriteTemplate(id) {
  return api.post(`/template/market/${id}/favorite`)
}

export function listFavoriteTemplates() {
  return api.get('/template/market/favorites')
}

export function rateMarketTemplate(id, score) {
  return api.post(`/template/market/${id}/rate`, { score })
}

export function copyMarketTemplate(id) {
  return api.post(`/template/market/${id}/copy`)
}

// ============ 模板导入 / 导出 ============

export function exportTemplate(id) {
  return api.get(`/template/${id}/export`)
}

export function importTemplate(data) {
  return api.post('/template/import', data)
}
