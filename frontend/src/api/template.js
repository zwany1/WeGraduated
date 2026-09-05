import api from './index'

export function createTemplate(data) {
  return api.post('/template/create', typeof data === 'string' ? { name: data } : data)
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

/** 一次性保存模板全部配置(单事务): 替代逐项发多个请求 */
export function saveAllConfig(id, data) {
  return api.put(`/template/${id}/config`, data)
}

/** 校规文档启发式抽取: 上传《格式规范》.docx, 返回模板配置初稿 + 原文摘录 */
export function extractSpec(file) {
  const form = new FormData()
  form.append('file', file)
  return api.post('/template/extract-spec', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function saveHeadingPatterns(id, headingPatterns) {
  return api.put(`/template/${id}/heading-patterns`, headingPatterns)
}

export function saveGenerateToc(id, generateToc) {
  return api.put(`/template/${id}/generate-toc`, { generateToc })
}

export function saveGenerateAbstract(id, generateAbstract) {
  return api.put(`/template/${id}/generate-abstract`, { generateAbstract })
}

export function saveReferenceConfig(id, referenceConfig) {
  return api.put(`/template/${id}/reference-config`, { referenceConfig: JSON.stringify(referenceConfig) })
}

export function saveTocConfig(id, tocConfig) {
  return api.put(`/template/${id}/toc-config`, { tocConfig: JSON.stringify(tocConfig) })
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

export function listMarketComments(id) {
  return api.get(`/template/market/${id}/comments`)
}

export function addMarketComment(id, content, parentId) {
  return api.post(`/template/market/${id}/comment`, { content, parentId })
}

export function deleteMarketComment(commentId) {
  return api.delete(`/template/comment/${commentId}`)
}

export function toggleMarketLike(id) {
  return api.post(`/template/market/${id}/like`)
}

export function copyMarketTemplate(id) {
  return api.post(`/template/market/${id}/copy`)
}

export function cloneTemplate(id) {
  return api.post(`/template/${id}/clone`)
}

export function getMissingRules(id) {
  return api.get(`/template/${id}/missing-rules`)
}
