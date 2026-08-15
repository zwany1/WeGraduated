/** 触发浏览器下载 Blob 文件 */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/** 下载文本文件 */
export function downloadText(text, filename) {
  downloadBlob(new Blob([text], { type: 'text/plain;charset=utf-8' }), filename)
}

/** 复制文本到剪贴板 */
export async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (e) {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      return true
    } catch (e2) {
      return false
    }
  }
}
