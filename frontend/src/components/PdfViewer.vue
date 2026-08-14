<template>
  <div class="pdf-viewer">
    <div class="toolbar">
      <el-button size="small" :disabled="pageNum <= 1" @click="prev">上一页</el-button>
      <span class="page-info">{{ pageNum }} / {{ pageCount }}</span>
      <el-button size="small" :disabled="pageNum >= pageCount" @click="next">下一页</el-button>
      <div class="zoom">
        <el-button size="small" @click="zoomOut">-</el-button>
        <span class="zoom-val">{{ Math.round(scale * 100) }}%</span>
        <el-button size="small" @click="zoomIn">+</el-button>
      </div>
    </div>
    <div class="canvas-wrap" ref="wrapRef">
      <div v-if="!loading && !errMsg" class="page-layer" :style="layerStyle">
        <canvas ref="canvasRef"></canvas>
        <!-- 差异高亮层: 半透明红罩 + 红边框 -->
        <div v-for="(hl, i) in curHighlights" :key="i" class="diff-hl" :style="hlStyle(hl)"></div>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="errMsg" class="pdf-err">{{ errMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'

// pdfjs worker: 本地化(public/pdf.worker.min.mjs), 不依赖 CDN, 避免首次加载慢/失败
pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs'

const props = defineProps({
  url: { type: String, required: false, default: '' },
  data: { type: Array, required: false, default: () => [] },
  // 差异高亮: [{ page, y, h }]  y/h 为页面内归一化(0-1)
  highlights: { type: Array, required: false, default: () => [] }
})

const canvasRef = ref(null)
const wrapRef = ref(null)
const pageNum = ref(1)
const pageCount = ref(0)
const scale = ref(1.2)
const loading = ref(true)
const errMsg = ref('')
const canvasW = ref(0)
const canvasH = ref(0)

let pdfDoc = null
let renderTask = null
let renderSeq = 0

const layerStyle = computed(() => ({ width: canvasW.value + 'px', height: canvasH.value + 'px' }))
const curHighlights = computed(() => (props.highlights || []).filter(h => h.page === pageNum.value))
function hlStyle(hl) {
  return {
    top: (hl.y * 100) + '%',
    height: Math.max(0.04, (hl.h || 0.05) * 100) + '%'
  }
}

onMounted(async () => {
  try {
    let loadingTask
    if (props.data && props.data.length) {
      loadingTask = pdfjsLib.getDocument({ data: new Uint8Array(props.data) })
    } else {
      loadingTask = pdfjsLib.getDocument(props.url)
    }
    pdfDoc = await loadingTask.promise
    pageCount.value = pdfDoc.numPages
    loading.value = false
    await renderPage()
  } catch (e) {
    window.__pdfErr = String(e && e.message ? e.message : e)
    errMsg.value = 'PDF 加载失败：' + (e && e.message ? e.message : '未知错误')
    loading.value = false
  }
})

onBeforeUnmount(() => {
  renderSeq++
  if (renderTask) {
    renderTask.cancel()
    renderTask = null
  }
  if (pdfDoc) {
    pdfDoc.destroy()
    pdfDoc = null
  }
})

watch(scale, async () => {
  await renderPage()
})

async function renderPage() {
  if (!pdfDoc) return
  const mySeq = ++renderSeq
  if (renderTask) {
    try { renderTask.cancel() } catch (e) {}
    renderTask = null
  }
  await nextTick()
  const page = await pdfDoc.getPage(pageNum.value)
  if (mySeq !== renderSeq) return
  const viewport = page.getViewport({ scale: scale.value })
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(viewport.width * dpr)
  canvas.height = Math.floor(viewport.height * dpr)
  canvas.style.width = Math.floor(viewport.width) + 'px'
  canvas.style.height = Math.floor(viewport.height) + 'px'
  canvasW.value = Math.floor(viewport.width)
  canvasH.value = Math.floor(viewport.height)
  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  renderTask = page.render({ canvasContext: ctx, viewport })
  try {
    await renderTask.promise
  } catch (e) {
    if (mySeq !== renderSeq) return // 被新渲染取消
  }
  renderTask = null
}

async function prev() {
  if (pageNum.value <= 1) return
  pageNum.value--
  await renderPage()
  scrollTop()
}

async function next() {
  if (pageNum.value >= pageCount.value) return
  pageNum.value++
  await renderPage()
  scrollTop()
}

async function gotoPage(n) {
  if (n < 1 || n > pageCount.value || n === pageNum.value) {
    if (n === pageNum.value) scrollTop()
    return
  }
  pageNum.value = n
  await renderPage()
  scrollTop()
}

function zoomIn() {
  scale.value = Math.min(3, Math.round((scale.value + 0.2) * 10) / 10)
}
function zoomOut() {
  scale.value = Math.max(0.4, Math.round((scale.value - 0.2) * 10) / 10)
}
function scrollTop() {
  if (wrapRef.value) wrapRef.value.scrollTop = 0
}

defineExpose({ gotoPage, next, prev })
</script>

<style scoped>
.pdf-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}
.page-info {
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
.canvas-wrap {
  flex: 1;
  overflow: auto;
  background: #525659;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 20px;
  position: relative;
}
.page-layer {
  position: relative;
  margin: auto;
  flex-shrink: 0;
}
canvas {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  display: block;
}
.diff-hl {
  position: absolute;
  left: 0;
  right: 0;
  border: 2px solid rgba(230, 57, 70, 0.85);
  background: rgba(230, 57, 70, 0.16);
  border-radius: 4px;
  pointer-events: none;
}
.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 14px;
}
.pdf-err {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  max-width: 80%;
  color: #fde2e2;
  background: rgba(0, 0, 0, 0.55);
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}
</style>
