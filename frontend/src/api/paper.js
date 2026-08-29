import api from './index'

export function uploadPaper(file) {
  const form = new FormData()
  form.append('file', file)
  return api.post('/paper/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function startFormat(fileId, templateId) {
  return api.post('/paper/format', { fileId, templateId })
}

/** 快速试排: 截取文档前若干段同步排版, 返回 .docx blob(配置页实时看真实效果) */
export function quickFormat(templateId, file, maxParagraphs = 150) {
  const form = new FormData()
  form.append('file', file)
  form.append('templateId', templateId)
  form.append('maxParagraphs', maxParagraphs)
  return api.post('/paper/format-quick', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
    timeout: 300000
  })
}

export function startFormatBatch(templateId, fileIds) {
  return api.post('/paper/format-batch', { templateId, fileIds })
}

export function getTask(id) {
  return api.get(`/paper/task/${id}`)
}

export function listTasks() {
  return api.get('/paper/tasks')
}

/** 排版体检报告(结构计数 + 疑似未匹配标题, 无报告返回 null) */
export function getTaskReport(taskId) {
  return api.get(`/paper/task/${taskId}/report`)
}

/** 引导修复重排: 对可疑标题行指定级别后生成新任务, overrides 形如 [{index, level}] */
export function refineTask(taskId, overrides) {
  return api.post(`/paper/task/${taskId}/refine`, overrides)
}

// 排版进度 SSE 一次性票据: 登录态签发, 60 秒内用于建立进度推送连接
export function progressTicket(taskId) {
  return api.post(`/paper/task/${taskId}/progress-ticket`)
}

export function downloadPaper(taskId) {
  return api.get(`/paper/download/${taskId}`, { responseType: 'blob' })
}

// 批量下载多个已排版任务, 打包成 zip
export function downloadPaperBatch(taskIds) {
  return api.post('/paper/download-batch', { taskIds }, { responseType: 'blob' })
}

// 排版差异分析: 返回差异段落列表(含 PDF 页码与坐标)
export function getDiff(taskId) {
  return api.get(`/paper/diff/${taskId}`)
}

export function previewPaperOriginal(taskId) {
  return api.get(`/paper/preview-original/${taskId}`, { responseType: 'blob' })
}

export function downloadPaperOriginal(taskId) {
  return api.get(`/paper/download-original/${taskId}`, { responseType: 'blob' })
}

export function deleteTask(id) {
  return api.delete(`/paper/task/${id}`)
}

// 批量删除任务: 级联清理结果文件
export function deleteTaskBatch(taskIds) {
  return api.delete('/paper/tasks', { data: { taskIds } })
}

export function listFiles() {
  return api.get('/paper/files')
}

export function deleteFile(id) {
  return api.delete(`/paper/file/${id}`)
}
