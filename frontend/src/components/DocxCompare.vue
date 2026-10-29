<template>
  <div class="docx-compare">
    <div class="toolbar">
      <div class="mode-switch">
        <button type="button" data-mode="page" :class="['mode-btn', { on: mode === 'page' }]" @click="setMode('page')">页面视图</button>
        <button type="button" data-mode="continuous" :class="['mode-btn', { on: mode === 'continuous' }]" @click="setMode('continuous')">连续滚动</button>
      </div>
      <span v-if="mode === 'page'" class="mode-hint" :title="pageTitle">分页按分页符近似 · {{ pageLabel }}</span>
      <span v-if="fontSwaps.length" class="font-note" :title="fontSwaps.join('\n')">字体已替换：{{ fontSwaps.join('、') }}</span>
      <span class="tip-note" :title="disclaimerTitle">
        <span class="tip-long">预览有偏差 · 以实际下载为准</span>
        <span class="tip-short">以实际下载为准</span>
      </span>
      <div class="zoom">
        <el-button size="small" @click="zoomOut">-</el-button>
        <span class="zoom-val">{{ Math.round(scale * 100) }}%</span>
        <el-button size="small" @click="zoomIn">+</el-button>
      </div>
      <el-button size="small" plain :disabled="!lastText" @click="refocus">聚焦上次</el-button>
      <el-button v-if="headings && headings.length" size="small" :type="showToc ? 'primary' : 'plain'" @click="showToc = !showToc">
        <span style="font-size:14px;line-height:1;">☰</span> 目录
      </el-button>
    </div>
    <div class="docx-main">
      <!-- 目录侧边栏 -->
      <transition name="toc-slide">
        <div v-if="showToc && headings && headings.length" class="toc-sidebar">
          <div class="toc-title">目录</div>
          <div class="toc-list">
            <div v-for="(h, i) in headings" :key="i" class="toc-item" :class="['toc-l' + h.level]" @click="jumpToHeading(h)">
              {{ h.text }}
            </div>
          </div>
        </div>
      </transition>
      <!-- 文档渲染区 -->
      <div class="docx-wrap" ref="wrapRef">
        <div ref="bodyRef" class="docx-body" :class="{ 'page-mode': mode === 'page' }" :style="bodyStyle"></div>
        <div v-if="loading" class="loading">渲染中...</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { renderAsync, parseAsync, renderDocument } from 'docx-preview'

const props = defineProps({
  data: { type: Array, required: true },
  headings: { type: Array, default: () => [] },
  onHeadingJump: { type: Function, default: null }
})

const wrapRef = ref(null)
const bodyRef = ref(null)
const scale = ref(1)
const loading = ref(true)
const showToc = ref(false)
/** 页面视图(默认, 真实纸张+分页) / 连续滚动(整篇快速浏览) */
const mode = ref(localStorage.getItem('docxPreviewMode') === 'continuous' ? 'continuous' : 'page')
const pageSize = ref({ w: 0, h: 0 })
/** 本机缺失、已被兜底字体替换的字体清单(展示给用户, 不静默替换) */
const fontSwaps = ref([])
let currentDoc = null
let lastText = ref('')

const pageLabel = computed(() => {
  const { w, h } = pageSize.value
  if (!w || !h) return ''
  const mm = px => Math.round(px / 96 * 25.4)
  return `${mm(w)}×${mm(h)}mm`
})
// 常态免责: 预览只是浏览器重排(不是 Word 排版引擎), 这几处已知的偏差写进 title, 避免用户拿预览当验收
const disclaimerTitle = computed(() =>
  '预览由浏览器渲染, 与下载的 .docx 可能存在偏差:\n' +
  '· 纸张高度内的内容不会回流到下一页, 超出一页的部分直接铺在当前纸面上(页数与 Word 可能不同)\n' +
  '· 本机缺失的字体按同族兜底显示\n' +
  '· 复杂公式 / 表格 / 图片绕排以浏览器渲染为准\n' +
  '验收请以实际下载的文档为准。')
const pageTitle = computed(() =>
  '页面视图按文档里的分页设置切页（含每章「段前分页」）；纸张高度内的空白照实显示，' +
  '超出一页的内容不会自动接到下一页（浏览器预览不做纵向排版回流）。')

/** docx-preview 只从「样式定义」里读段前分页(splitBySection 查的是 findStyle(styleName).paragraphProps)，
 *  段落直接格式的 <w:pageBreakBefore/> 会被丢掉 —— 而引擎写的正是段落级(摊平成 el.pageBreakBefore)。
 *
 *  做法: 把 page break 追加到**前一个段落最后一个 run 的后面**，splitBySection 就会在该段落结束后切页：
 *    - 不能把 break 塞进标题段落开头 —— split 会把标题段落截成两半，在上一页末尾留下一个
 *      仍带标题样式的空段落（会被当成第二个"一级标题"）；
 *    - 也不能在标题前插一个只含 break 的空段落 —— 若前一段本身以节分隔(sectPr)结尾，
 *      那个空段落会独占一整页（实测多出 11 个全白页）。
 *  追加成一个独立 run(而不是塞进最后一个 run 的 children)是为了让 pBreakIndex 落在这个新 run 上，
 *  这样 `pBreakIndex < children.length - 1` 为假，段落的其它 run 不会被搬到新段里去。
 *  break 的 break==='page' 不会被 renderBreak 渲染(只认 textWrapping)，所以视觉上不多出任何东西。
 *  只在页面视图调用。返回注入个数。 */
function injectPageBreaks(doc) {
  const body = doc && doc.documentPart && doc.documentPart.body
  if (!body || !Array.isArray(body.children)) return 0
  let n = 0
  const breakRun = () => ({ type: 'run', children: [{ type: 'break', break: 'page' }] })
  /**
   * 在同一层的元素里处理; 返回新数组。prev 为空说明该段是这层的第一段 —— 它本来就在新页顶部,
   * 无需分页(文档第一段、表格单元格第一段都属于这种情况)。
   */
  const visit = els => {
    const out = []
    for (const el of els) {
      if (el.type === 'paragraph' && el.pageBreakBefore) {
        const prev = out[out.length - 1]
        if (prev && prev.type === 'paragraph' && !prev.__br) {
          prev.children = prev.children || []
          prev.children.push(breakRun())
          prev.__br = true
          n++
        } else if (prev && prev.type !== 'paragraph') {
          // 前一块是表格等: 补一个只含分页符的空段落收尾(与表格同节, 不会独占一页)
          const tail = { type: 'paragraph', children: [breakRun()], cssStyle: {}, __br: true }
          out.push(tail)
          n++
        }
      } else if (el.type === 'table') {
        for (const row of el.children || []) for (const cell of row.children || []) cell.children = visit(cell.children || [])
      }
      out.push(el)
    }
    return out
  }
  body.children = visit(body.children)
  return n
}

// 缩放: 优先用 CSS zoom(重排布局, 放大不会把字切掉); 老浏览器回退到 transform
const zoomSupported = typeof CSS !== 'undefined' && !!CSS.supports && CSS.supports('zoom', '1')
const bodyStyle = computed(() => zoomSupported
  ? { zoom: String(scale.value) }
  : { transform: `scale(${scale.value})`, transformOrigin: 'top center', transition: 'transform 0.15s ease' })

/** 渲染入口: 页面视图走 parse -> 注入分页 -> render(两者都是 docx-preview 的公开导出,
 *  等价于 renderAsync 内部流程, 只是中间多一步); 任何一环不可用就退回 renderAsync。 */
async function renderDocx(blob, opts) {
  if (mode.value === 'page' && typeof parseAsync === 'function' && typeof renderDocument === 'function') {
    try {
      const doc = await parseAsync(blob, opts)
      window.__previewBreaks = injectPageBreaks(doc)
      const nodes = await renderDocument(doc, opts)
      bodyRef.value.innerHTML = ''
      for (const node of nodes) bodyRef.value.appendChild(node)
      return doc
    } catch (e) {
      delete window.__docxErr
      window.__docxErr = '分页增强失败, 已回退: ' + (e && e.message ? e.message : e)
    }
  }
  return await renderAsync(blob, bodyRef.value, null, opts)
}

/** 按当前模式渲染(切模式时重渲染) */
async function render() {
  if (!bodyRef.value) return
  loading.value = true
  try {
    if (currentDoc && typeof currentDoc.close === 'function') {
      try { currentDoc.close() } catch (e) {}
      currentDoc = null
    }
    bodyRef.value.innerHTML = ''
    const blob = new Blob([new Uint8Array(props.data)], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const base = {
      className: 'docx',
      inWrapper: true,
      // ignoreWidth 必须为 false: 纸张宽度取文档 pgSz, 否则 A4/B5 都渲染成容器宽
      ignoreWidth: false,
      // 开启 docx-preview 实验性功能: 渲染 OMML 公式(OMML->MathML)
      experimental: true
    }
    const opts = mode.value === 'page'
      ? { ...base, ignoreHeight: false, breakPages: true, renderHeaders: true, renderFooters: true }
      : { ...base, ignoreHeight: true, breakPages: false, renderHeaders: false, renderFooters: false }
    currentDoc = await renderDocx(blob, opts)
    // 渲染模型留给 e2e / 排障用(只读, 不参与渲染逻辑)
    window.__previewDoc = currentDoc
    // 兜底: 某些文档经 docx-preview 渲染会出现"文字在 DOM 中(可复制)但不可见"的情况
    normalizeVisibility()
    setTimeout(() => normalizeVisibility(), 300)
    markSheets()
    loading.value = false
  } catch (e) {
    window.__docxErr = String(e && e.message ? e.message : e)
    loading.value = false
  }
}

function setMode(next) {
  if (mode.value === next) return
  mode.value = next
  localStorage.setItem('docxPreviewMode', next)
  render()
}

/** 页面视图: 记录纸张尺寸, 并在每页下方标「第 N 页 / 共 M 页」 */
function markSheets() {
  const wrapper = bodyRef.value && bodyRef.value.querySelector('.docx-wrapper')
  if (!wrapper) return
  wrapper.querySelectorAll('.sheet-no').forEach(n => n.remove())
  if (mode.value !== 'page') {
    pageSize.value = { w: 0, h: 0 }
    return
  }
  const secs = [...wrapper.querySelectorAll('section')]
  if (!secs.length) return
  // 纸张尺寸取计算样式(width/min-height), 不能用 rect —— 内容超长时 section 会向下生长,
  // 而且 rect 会带上缩放倍数, computed 值才是纸张本身的尺寸
  const cs = getComputedStyle(secs[0])
  pageSize.value = { w: parseFloat(cs.width), h: parseFloat(cs.minHeight) }
  secs.forEach((s, i) => {
    const tag = document.createElement('div')
    tag.className = 'sheet-no'
    tag.textContent = `第 ${i + 1} 页 / 共 ${secs.length} 页`
    s.insertAdjacentElement('afterend', tag)
  })
}

onMounted(render)

/** 中文字体族 → 缺字时的同族兜底(保留衬线/无衬线观感差异) */
const FONT_FALLBACK = [
  { re: /宋体|SimSun|Songti|STSong/i, fb: '"Noto Serif SC", "Songti SC", serif' },
  { re: /仿宋|FangSong|STFangsong/i, fb: '"Noto Serif SC", "FangSong", serif' },
  { re: /楷体|KaiTi|STKaiti/i, fb: '"Noto Serif SC", "Kaiti SC", serif' },
  { re: /黑体|SimHei|Heiti|STHeiti/i, fb: '"Noto Sans SC", "Heiti SC", sans-serif' }
]

const fontCache = new Map()
let probeCtx = null

/** 字体是否真的可用: 与"必然缺失的族"比宽度, 宽度相同即视为缺失(浏览器已回退到默认字体) */
function fontAvailable(family) {
  const key = family.toLowerCase()
  if (fontCache.has(key)) return fontCache.get(key)
  if (!probeCtx) probeCtx = document.createElement('canvas').getContext('2d')
  const sample = '汉字测量样本 Wg'
  probeCtx.font = '72px "__docx_missing_font__", monospace'
  const base = probeCtx.measureText(sample).width
  probeCtx.font = `72px "${family}", "__docx_missing_font__", monospace`
  const wid = probeCtx.measureText(sample).width
  const ok = Math.abs(wid - base) > 0.5
  fontCache.set(key, ok)
  return ok
}

function firstFamily(stack) {
  const m = /^\s*"?([^",]+)"?/.exec(stack || '')
  return m ? m[1].trim() : ''
}

/** 修正隐形/异常文字:
 *  - 字号0 => 默认字号; 白/透明色 => 黑色; display:none => inline; 异常字间距 => normal
 *  - 字体: 只在本机缺字时补同族兜底, 不再无差别覆盖 —— 全覆盖会把宋体/仿宋/黑体的差异抹平,
 *    预览就失去了"验收字体"的意义。 */
function normalizeVisibility() {
  if (!bodyRef.value) return
  const swapped = new Set()
  bodyRef.value.querySelectorAll('p, span').forEach(el => {
    const text = (el.textContent || '').trim()
    if (!text) return
    const st = window.getComputedStyle(el)
    if (parseFloat(st.fontSize) <= 0) el.style.fontSize = '10.5pt'
    if (st.display === 'none') el.style.display = 'inline'
    const c = st.color || ''
    if (/transparent|rgba\(\s*0,\s*0,\s*0,\s*0\)|rgb\(\s*255,\s*255,\s*255\)/.test(c)) {
      el.style.color = '#000000'
    }
    const ls = parseFloat(st.letterSpacing)
    const ws = parseFloat(st.wordSpacing)
    if (ls > 3) el.style.letterSpacing = 'normal'
    if (ws > 6) el.style.wordSpacing = 'normal'

    const fam = firstFamily(st.fontFamily)
    if (!fam || fam.startsWith('__')) return
    const rule = FONT_FALLBACK.find(r => r.re.test(fam))
    if (!rule || fontAvailable(fam)) return
    el.style.setProperty('font-family', `"${fam}", ${rule.fb}`, 'important')
    swapped.add(`${fam} → ${rule.fb.split(',')[0].replace(/"/g, '')}`)
  })
  if (swapped.size) {
    const next = [...new Set([...fontSwaps.value, ...swapped])]
    if (next.length !== fontSwaps.value.length) fontSwaps.value = next
  }
  relaxJustify()
}

/** 两端对齐兜底: docx-preview 把每个 run 渲染成独立 span, Chrome 对 CJK 做 justify 时会在
 *  span 边界插入大片空白。只对"确实撑出空洞"的段落降级为左对齐, 其余保持原对齐。 */
function relaxJustify() {
  if (!bodyRef.value) return
  bodyRef.value.querySelectorAll('p').forEach(p => {
    if (p.dataset.justifyFallback === '1') return
    const st = window.getComputedStyle(p)
    if (st.textAlign !== 'justify') return
    const spans = [...p.querySelectorAll('span')].filter(s => (s.textContent || '').trim() && s.getBoundingClientRect().width > 0)
    if (spans.length < 2) return
    const em = parseFloat(st.fontSize) || 16
    const rects = spans.map(s => s.getBoundingClientRect())
    for (let i = 1; i < rects.length; i++) {
      // 同一行内的相邻 span 之间不应有大空洞; 换行时 prev.right 会明显小于 next.left(负值), 不算
      const gap = rects[i].left - rects[i - 1].right
      if (gap > em * 0.5 && Math.abs(rects[i].top - rects[i - 1].top) < em) {
        p.dataset.justifyFallback = '1'
        p.style.setProperty('text-align', 'start', 'important')
        break
      }
    }
  })
}

onBeforeUnmount(() => {
  if (currentDoc && typeof currentDoc.close === 'function') {
    try { currentDoc.close() } catch (e) {}
  }
  if (bodyRef.value) bodyRef.value.innerHTML = ''
})

function zoomIn() {
  scale.value = Math.min(2, Math.round((scale.value + 0.1) * 10) / 10)
}
function zoomOut() {
  scale.value = Math.max(0.5, Math.round((scale.value - 0.1) * 10) / 10)
}

/** 聚焦到上次定位的段落 */
function refocus() {
  if (lastText.value) scrollToText(lastText.value)
}

/** 目录跳转: 定位到指定标题段落(同时通知父组件同步两侧) */
function jumpToHeading(h) {
  scrollToText(h.text)
  if (props.onHeadingJump) props.onHeadingJump(h.text)
}

function scrollToText(text) {
  const needle = (text || '').replace(/\s+/g, '')
  if (!needle || !bodyRef.value) return
  lastText.value = text
  const paras = bodyRef.value.querySelectorAll('p')
  const jump = (p) => {
    p.scrollIntoView({ behavior: 'smooth', block: 'start' })
    p.classList.add('diff-target')
    setTimeout(() => p.classList.remove('diff-target'), 2200)
  }
  for (const p of paras) {
    const t = (p.textContent || '').replace(/\s+/g, '')
    if (t === needle) {
      jump(p)
      return
    }
  }
  const head = needle.slice(0, 10)
  if (!head) return
  for (const p of paras) {
    const t = (p.textContent || '').replace(/\s+/g, '')
    if (t && (t.includes(head) || head.includes(t.slice(0, 10)))) {
      jump(p)
      return
    }
  }
}

defineExpose({ scrollToText })
</script>

<style scoped>
.docx-compare {
  display: flex;
  flex-direction: column;
  height: 100%;
}
/* 文档主体区域: 目录侧边栏 + 文档渲染 */
.docx-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}
/* 目录侧边栏 */
.toc-sidebar {
  width: 200px;
  min-width: 200px;
  background: #F6F4EE;
  border-right: 1px solid #E3E0D5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.toc-title {
  padding: 10px 14px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #24312A;
  border-bottom: 1px solid #E3E0D5;
}
.toc-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.toc-item {
  padding: 5px 14px;
  font-size: 13px;
  color: #5C6B60;
  cursor: pointer;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background 0.15s, color 0.15s;
}
.toc-item:hover {
  background: #EAEFED;
  color: #2F5D46;
}
.toc-l1 { font-weight: 600; color: #24312A; }
.toc-l2 { padding-left: 26px; }
.toc-l3 { padding-left: 38px; font-size: 12px; color: #8B968D; }
/* 侧边栏进出动画 */
.toc-slide-enter-active,
.toc-slide-leave-active {
  transition: width 0.2s ease, opacity 0.2s ease;
}
.toc-slide-enter-from,
.toc-slide-leave-to {
  width: 0;
  min-width: 0;
  opacity: 0;
}
.toc-slide-enter-to,
.toc-slide-leave-from {
  width: 200px;
  min-width: 200px;
  opacity: 1;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-bottom: 1px solid #E3E0D5;
  background: #F6F4EE;
  /* 容器查询: 本组件在预览弹窗(宽)与对比视图栏(窄)里复用, 要按"所在容器"而不是视口来收放文案 */
  container-type: inline-size;
}
/* 功能控件不可被压缩: 说明类文案再挤也不能把模式/缩放/按钮压没 */
.mode-switch,
.zoom,
.toolbar > .el-button {
  flex-shrink: 0;
}
.mode-switch {
  display: flex;
  border: 1px solid #D6D1C5;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}
.mode-btn {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 4px 10px;
  font-size: 12px;
  color: #5C6B60;
  cursor: pointer;
  line-height: 1.6;
}
.mode-btn + .mode-btn { border-left: 1px solid #E3E0D5; }
.mode-btn:hover { color: #24312A; }
.mode-btn.on { background: #24312A; color: #fff; }
.mode-hint {
  font-size: 12px;
  color: #8B968D;
  flex-shrink: 2;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.font-note {
  font-size: 12px;
  color: #8A6D3B;
  background: #FCF8E3;
  border: 1px solid #F0E0B8;
  border-radius: 3px;
  padding: 1px 6px;
  flex-shrink: 2;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 常态免责: 用中性灰而不是告警黄 —— 它是"说明"不是"出错", 告警色留给字体缺失那类真异常。
   空间不够时先由 mode-hint / font-note 让位(shrink:2), 再轮到它(shrink:1); 窄容器里换成短文案。 */
.tip-note {
  font-size: 12px;
  color: #6B786F;
  background: #EFF2F0;
  border: 1px solid #D8DDD9;
  border-radius: 3px;
  padding: 1px 6px;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: help;
}
.tip-short { display: none; }
/* 窄容器(对比视图栏 ≈594px): 功能控件第一、免责提示第二, 其余说明让位 */
@container (max-width: 640px) {
  .mode-hint,
  .font-note { display: none; }
  .tip-long { display: none; }
  .tip-short { display: inline; }
}
.zoom {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.zoom-val {
  font-size: 13px;
  color: #5C6B60;
  width: 44px;
  text-align: center;
}
.docx-wrap {
  flex: 1;
  overflow: auto;
  background: #525659;
  padding: 20px;
  position: relative;
}
.docx-body {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  min-height: 200px;
  /* 纸宽由文档 pgSz 决定(ignoreWidth:false), 这里不能再写死 900px */
  max-width: none;
  width: fit-content;
  margin: 0 auto;
  /* 必须允许横向溢出: docx-preview 的 span 在两端对齐等场景会被撑到容器宽度之外,
     overflow-x:hidden 会把超出部分视觉裁掉(文字仍在DOM可复制), 表现为"缺字+散架" */
  overflow: visible !important;
}
/* 页面视图下白纸由每页 section 自己给, 容器保持透明 */
.docx-body.page-mode { background: transparent; box-shadow: none; }
.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 14px;
}
:deep(.docx-wrapper) {
  padding: 0;
  width: 100%;
}
/* 兜底(仅连续视图): docx-preview 0.4.0 的分页容器是 flex+overflow:hidden, 内容超界会被视觉裁切
   (文字仍在DOM可复制)。页面视图不能这么改, 否则纸张高度被抹掉就看不到分页了。 */
.docx-body:not(.page-mode) :deep(.docx section.docx) {
  overflow: visible !important;
  display: block !important;
  min-height: auto !important;
  height: auto !important;
}
:deep(.docx section.docx > article) {
  width: 100%;
}
/* 页面视图: 每页是一张纸, 高度不足按 min-height 撑开, 内容更长时向下生长(宁可露出超长页也不裁字) */
.docx-body.page-mode :deep(.docx-wrapper) {
  padding: 0;
  width: auto;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.docx-body.page-mode :deep(.docx section.docx) {
  overflow: visible !important;
  display: block !important;
  height: auto !important;
  margin: 0 0 10px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}
/* 页码角标: 由组件在每页后插入, 不写进 docx */
.sheet-no {
  font-size: 12px;
  line-height: 18px;
  color: #C7CDD1;
  text-align: center;
  letter-spacing: 0.5px;
  margin: 0 0 18px;
  user-select: none;
}
/* 对齐不再一刀切: 只有"被拆成多个 span 且 justify 撑出大空洞"的段落才降级为左对齐,
   其余保留文档原本的居中/两端对齐(题注、公式要能看出居中)。 */
:deep(.docx p[data-justify-fallback="1"]) {
  text-align: start !important;
}
/* 字体不再统一硬覆盖: 由 normalizeVisibility 只对"本机缺失的中文字体"补同族兜底 */
:deep(p.diff-target) {
  background: rgba(230, 57, 70, 0.18);
  box-shadow: 0 0 0 2px rgba(230, 57, 70, 0.6) inset;
  border-radius: 3px;
  transition: background 0.3s;
}
</style>
