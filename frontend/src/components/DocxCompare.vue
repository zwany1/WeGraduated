<template>
  <div class="docx-compare">
    <div class="toolbar">
      <span class="mode-label">整篇滚动预览</span>
      <div class="zoom">
        <el-button size="small" @click="zoomOut">-</el-button>
        <span class="zoom-val">{{ Math.round(scale * 100) }}%</span>
        <el-button size="small" @click="zoomIn">+</el-button>
      </div>
    </div>
    <div class="docx-wrap" ref="wrapRef">
      <div ref="bodyRef" class="docx-body" :style="bodyStyle"></div>
      <div v-if="loading" class="loading">渲染中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { renderAsync } from 'docx-preview'

const props = defineProps({
  data: { type: Array, required: true }
})

const wrapRef = ref(null)
const bodyRef = ref(null)
const scale = ref(1)
const loading = ref(true)

let currentDoc = null

const bodyStyle = {
  width: `${Math.round(100 * scale.value)}%`,
  maxWidth: '1000px',
  margin: '0 auto',
  transition: 'width 0.1s'
}

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
  updateBodyStyle()
}
function zoomOut() {
  scale.value = Math.max(0.5, Math.round((scale.value - 0.1) * 10) / 10)
  updateBodyStyle()
}
function updateBodyStyle() {
  bodyStyle.width = `${Math.round(100 * scale.value)}%`
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
</style>
