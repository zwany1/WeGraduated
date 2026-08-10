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

export function getTask(id) {
  return api.get(`/paper/task/${id}`)
}

export function listTasks() {
  return api.get('/paper/tasks')
}

export function downloadPaper(taskId) {
  return api.get(`/paper/download/${taskId}`, { responseType: 'blob' })
}

export function previewPaper(taskId) {
  return api.get(`/paper/preview/${taskId}`, { responseType: 'blob' })
}

export function downloadPaperPdf(taskId) {
  return api.get(`/paper/download-pdf/${taskId}`, { responseType: 'blob' })
}
