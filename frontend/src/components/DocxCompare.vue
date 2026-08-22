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
    </div>
    <div class="docx-wrap" ref="wrapRef">
      <div ref="bodyRef" class="docx-body" :style="bodyStyle"></div>
      <div v-if="loading" class="loading">渲染中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { renderAsync } from 'docx-preview'

const props = defineProps({
  data: { type: Array, required: true }
})

const wrapRef = ref(null)
const bodyRef = ref(null)
const scale = ref(1)
const loading = ref(true)
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
      breakPages: false
    })
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
  overflow-x: hidden;
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
:deep(p.diff-target) {
  background: rgba(230, 57, 70, 0.18);
  box-shadow: 0 0 0 2px rgba(230, 57, 70, 0.6) inset;
  border-radius: 3px;
  transition: background 0.3s;
}
</style>
