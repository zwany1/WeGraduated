/**
 * 从 docx ArrayBuffer 中提取标题层级结构(用于目录跳转)
 * 通过解析 word/document.xml 的 pStyle / outlineLvl 判断标题级别
 * @returns {Array<{level: number, text: string}>}
 */
export async function extractHeadings(arrayBuffer) {
  try {
    const JSZip = (await import('jszip')).default
    const zip = await JSZip.loadAsync(arrayBuffer)
    const xmlFile = zip.file('word/document.xml')
    if (!xmlFile) return []
    const xml = await xmlFile.async('string')
    const parser = new DOMParser()
    const doc = parser.parseFromString(xml, 'application/xml')

    const headings = []
    const paras = doc.querySelectorAll('p')
    for (const p of paras) {
      let level = 0
      // 方法1: outlineLvl (最可靠: 0=一级, 1=二级, 2=三级)
      const outlineEl = p.querySelector('outlineLvl')
      if (outlineEl) {
        level = parseInt(outlineEl.getAttribute('w:val'), 10) + 1
      }
      // 方法2: pStyle 样式名匹配
      if (!level) {
        const pStyle = p.querySelector('pStyle')
        if (pStyle) {
          const sid = (pStyle.getAttribute('w:val') || '').toLowerCase()
          if (sid.includes('heading1') || sid.includes('heading 1') || sid === '2') level = 1
          else if (sid.includes('heading2') || sid.includes('heading 2') || sid === '3') level = 2
          else if (sid.includes('heading3') || sid.includes('heading 3') || sid === '4') level = 3
        }
      }
      if (level >= 1 && level <= 3) {
        const text = (p.textContent || '').trim()
        if (text) headings.push({ level, text })
      }
    }
    return headings
  } catch {
    return []
  }
}
