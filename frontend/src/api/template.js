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

export function saveRule(rule) {
  return api.post('/template/rule/save', rule)
}

export function listRules(id) {
  return api.get(`/template/${id}/rules`)
}
