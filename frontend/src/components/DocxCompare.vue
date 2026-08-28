<template>
  <div class="docx-compare">
    <div class="toolbar">
      <span class="mode-label">整篇滚动预览</span>
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
        <div ref="bodyRef" class="docx-body" :style="bodyStyle"></div>
        <div v-if="loading" class="loading">渲染中...</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { renderAsync } from 'docx-preview'

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
let currentDoc = null
let lastText = ref('')

// 响应式 bodyStyle: 用 transform scale 缩放, 不破坏布局宽度
const bodyStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transformOrigin: 'top center',
  transition: 'transform 0.15s ease'
}))

onMounted(async () => {
  loading.value = true
  try {
    const blob = new Blob([new Uint8Array(props.data)], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    currentDoc = await renderAsync(blob, bodyRef.value, null, {
      className: 'docx',
      inWrapper: true,
      ignoreWidth: true,
      ignoreHeight: true,
      breakPages: false,
      // 开启 docx-preview 实验性功能: 渲染 OMML 公式(OMML->MathML), 否则公式段落(普通文字段)整段空白
      experimental: true
    })
    // 兜底: 某些文档经 docx-preview 渲染会出现"文字在 DOM 中(可复制)但不可见"的情况
    // (如 字体缺字形、run 无 w:sz 导致字号被算成 0、样式色为白/透明、分页 section overflow:hidden 裁切等)
    // 渲染完成后做样式归一化; 连跑两次避免 docx-preview 后处理覆盖
    normalizeVisibility()
    setTimeout(() => normalizeVisibility(), 300)
    loading.value = false
  } catch (e) {
    window.__docxErr = String(e && e.message ? e.message : e)
    loading.value = false
  }
})

/** 修正隐形/异常文字:
 *  - 字号0 => 默认字号
 *  - 白/透明色 => 黑色
 *  - display:none => inline
 *  - 异常大字符间距/词间距 => normal
 *  - 字体: 直接整体替换为带通用字体族(sans-serif)收尾的栈 ——
 *    文档显式指定宋体/黑体, 若本机该字体存在但缺 CJK 字形, Chrome 会卡住直接显示空白(不再向后fallback);
 *    Windows 默认含 Microsoft YaHei, 且必须用 sans-serif 通用族收尾才能触发字形联查。
 *    这是用户实测有效的做法(控制台替换字体后文字出现)。 */
function normalizeVisibility() {
  if (!bodyRef.value) return
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
    // 统一字体栈(硬覆盖): 必须带 sans-serif 通用族收尾, 否则中文可能仍空白
    el.style.setProperty('font-family', 'Arial, "Microsoft YaHei", sans-serif', 'important')
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
  background: #fafbfc;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.toc-title {
  padding: 10px 14px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}
.toc-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.toc-item {
  padding: 5px 14px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background 0.15s, color 0.15s;
}
.toc-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.toc-l1 { font-weight: 600; color: #303133; }
.toc-l2 { padding-left: 26px; }
.toc-l3 { padding-left: 38px; font-size: 12px; color: #909399; }
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
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}
.mode-label {
  font-size: 13px;
  color: #606266;
}
.zoom {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.zoom-val {
  font-size: 13px;
  color: #606266;
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
  max-width: 900px;
  margin: 0 auto;
  /* 必须允许横向溢出: docx-preview 的 span 在两端对齐等场景会被撑到容器宽度之外,
     overflow-x:hidden 会把超出部分视觉裁掉(文字仍在DOM可复制), 表现为"缺字+散架" */
  overflow: visible !important;
}
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
/* 兜底: docx-preview 0.4.0 的分页容器是 flex+overflow:hidden, 内容超界会被视觉裁切(文字仍在DOM可复制)
   强制解除裁切与 flex 布局, 避免"有文字但显示空白" */
:deep(.docx section.docx) {
  overflow: visible !important;
  display: block !important;
  min-height: auto !important;
  height: auto !important;
}
:deep(.docx section.docx > article) {
  width: 100%;
}
/* 核心修复: docx-preview 的每个 run 是独立 span, Chrome 对 CJK 文本执行 text-align: justify 时
   会在 span 边界之间插入巨大间距(字与字之间出现大片空白), Word 用原生引擎不存在此问题。
   将段落对齐改为 start(左对齐), 消除散架间距。这是预览/对比工具, 不影响实际 docx 排版。 */
:deep(.docx p) {
  text-align: start !important;
}
/* 根因修复: docx-preview 按文档规则写死宋体/黑体, 若该字体在本机存在但缺 CJK 字形,
   Chrome 会卡住显示空白(不向后 fallback); 统一替换为带 sans-serif 通用族收尾的字体栈,
   确保任何环境下 CJK 都能渲染(Windows 默认含 Microsoft YaHei)。 */
:deep(.docx) {
  font-family: Arial, "Microsoft YaHei", sans-serif !important;
}
:deep(p.diff-target) {
  background: rgba(230, 57, 70, 0.18);
  box-shadow: 0 0 0 2px rgba(230, 57, 70, 0.6) inset;
  border-radius: 3px;
  transition: background 0.3s;
}
</style>
