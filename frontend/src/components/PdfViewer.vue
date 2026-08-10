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
      <canvas ref="canvasRef"></canvas>
      <div v-if="loading" class="loading">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'

// pdfjs worker: 通过 CDN 提供(避免 vite 打包 worker 的复杂配置)
const PDFJS_VERSION = '6.2.108'
pdfjsLib.GlobalWorkerOptions.workerSrc =
  `https://cdn.jsdelivr.net/npm/pdfjs-dist@${PDFJS_VERSION}/build/pdf.worker.min.mjs`

const props = defineProps({
  url: { type: String, required: false, default: '' },
  data: { type: Array, required: false, default: () => [] }
})

const canvasRef = ref(null)
const wrapRef = ref(null)
const pageNum = ref(1)
const pageCount = ref(0)
const scale = ref(1.2)
const loading = ref(true)

let pdfDoc = null
let renderTask = null

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
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (renderTask) renderTask.cancel()
  if (pdfDoc) pdfDoc.destroy()
})

watch(scale, async () => {
  await renderPage()
})

async function renderPage() {
  if (!pdfDoc) return
  if (renderTask) {
    renderTask.cancel()
    renderTask = null
  }
  await nextTick()
  const page = await pdfDoc.getPage(pageNum.value)
  const viewport = page.getViewport({ scale: scale.value })
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(viewport.width * dpr)
  canvas.height = Math.floor(viewport.height * dpr)
  canvas.style.width = Math.floor(viewport.width) + 'px'
  canvas.style.height = Math.floor(viewport.height) + 'px'
  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  renderTask = page.render({ canvasContext: ctx, viewport })
  await renderTask.promise
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

function zoomIn() {
  scale.value = Math.min(3, Math.round((scale.value + 0.2) * 10) / 10)
}
function zoomOut() {
  scale.value = Math.max(0.4, Math.round((scale.value - 0.2) * 10) / 10)
}
function scrollTop() {
  if (wrapRef.value) wrapRef.value.scrollTop = 0
}
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
  justify-content: center;
  padding: 20px;
  position: relative;
}
canvas {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}
.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 14px;
}
</style>
