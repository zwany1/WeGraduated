<template>
  <div class="docx-compare">
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
    <div class="docx-wrap" ref="wrapRef">
      <div ref="bodyRef" class="docx-body"></div>
      <div v-if="loading" class="loading">渲染中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { renderAsync } from 'docx-preview'

const props = defineProps({
  data: { type: Array, required: true }
})

const wrapRef = ref(null)
const bodyRef = ref(null)
const pageNum = ref(1)
const pageCount = ref(0)
const scale = ref(1)
const loading = ref(true)

let pages = [] // 分页后的页面容器数组
let currentDoc = null

onMounted(async () => {
  loading.value = true
  try {
    const blob = new Blob([new Uint8Array(props.data)], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    currentDoc = await renderAsync(blob, bodyRef.value, null, {
      className: 'docx',
      inWrapper: true,
      ignoreWidth: true,
      ignoreHeight: true,
      breakPages: true
    })
    await collectPages()
    loading.value = false
  } catch (e) {
    window.__docxErr = String(e && e.message ? e.message : e)
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (currentDoc && typeof currentDoc.close === 'function') {
    try { currentDoc.close() } catch (e) {}
  }
  if (bodyRef.value) bodyRef.value.innerHTML = ''
})

watch(scale, () => applyScale())

async function collectPages() {
  await nextTick()
  const container = bodyRef.value
  if (!container) return
  // docx-preview 渲染成多个 .docx-wrapper，每个 wrapper 是一页
  const wrappers = container.querySelectorAll('.docx-wrapper')
  pages = []
  wrappers.forEach((w) => {
    const clone = w.cloneNode(true)
    clone.style.display = 'none'
    pages.push(clone)
    w.style.display = 'none'
  })
  pageCount.value = pages.length || 1
  if (pages.length) {
    container.appendChild(pages[0])
    pages[0].style.display = ''
  }
  applyScale()
}

function applyScale() {
  pages.forEach((p) => {
    p.style.transform = `scale(${scale.value})`
    p.style.transformOrigin = 'top center'
  })
}

function showPage(n) {
  pages.forEach((p) => { p.style.display = 'none' })
  if (pages[n - 1]) {
    pages[n - 1].style.display = ''
  }
  if (wrapRef.value) wrapRef.value.scrollTop = 0
}

async function prev() {
  if (pageNum.value <= 1) return
  pageNum.value--
  showPage(pageNum.value)
}
async function next() {
  if (pageNum.value >= pageCount.value) return
  pageNum.value++
  showPage(pageNum.value)
}
function zoomIn() {
  scale.value = Math.min(2, Math.round((scale.value + 0.1) * 10) / 10)
}
function zoomOut() {
  scale.value = Math.max(0.5, Math.round((scale.value - 0.1) * 10) / 10)
}
</script>

<style scoped>
.docx-compare {
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
  margin: 0 auto;
  min-height: 200px;
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
}
:deep(.docx) {
  width: 100%;
}
</style>
