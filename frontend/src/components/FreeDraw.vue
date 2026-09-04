<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>自由绘画</span>
        <span class="brand-sub">drawio 画板</span>
      </div>
      <div class="bar-actions">
        <el-input v-model="name" class="name-input" size="default" maxlength="60" placeholder="未命名设计" />
        <span v-if="dirty" class="dirty-dot" title="有未保存的修改"></span>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="saveAs">另存为</el-button>
        <el-button @click="downloadPng">导出 PNG</el-button>
        <el-button @click="exportMermaid">Mermaid</el-button>
      </div>
    </header>

    <MermaidExportDialog ref="mmdDlg" />

    <div class="editor-body">
      <div class="canvas-area">
        <iframe
          :key="frameKey"
          ref="frameRef"
          class="drawio-frame"
          :src="EMBED_URL"
          frameborder="0"
        ></iframe>
        <div v-if="loadFailed" class="canvas-loading">
          <div>diagrams.net 加载缓慢或无法连接，请检查网络后重试</div>
          <el-button class="retry-btn" type="primary" @click="retry">重试</el-button>
        </div>
      </div>

      <aside class="props">
        <div class="props-head">
          <div class="props-title">我的设计</div>
          <el-button size="small" @click="newDesign">新建</el-button>
        </div>
        <div class="save-list">
          <div v-for="d in myDesigns" :key="d.id" class="save-item">
            <span class="save-name" :class="{ current: d.id === currentId }" @click="openDesign(d)">{{ d.name }}</span>
            <el-button size="small" text type="danger" @click="delDesign(d)">×</el-button>
          </div>
          <div v-if="!myDesigns.length" class="props-empty">暂无保存</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveDiagram, listDiagrams, loadDiagram, deleteDiagram } from '../api/diagram'
import { toMermaid } from '../utils/mermaid'
import { legacyToDrawioXml } from '../utils/drawioConvert'
import MermaidExportDialog from './MermaidExportDialog.vue'

const EMBED_URL = 'https://embed.diagrams.net/?embed=1&proto=json&ui=min&spin=1&libraries=1'

const router = useRouter()
function goBack() {
  router.back()
}

const frameRef = ref(null)
const mmdDlg = ref(null)
const frameKey = ref(0)
const editorReady = ref(false)
const loadFailed = ref(false)
const name = ref('未命名设计')
const dirty = ref(false)
const saving = ref(false)
const myDesigns = ref([])
const currentId = ref(null)

let currentXml = null
let pendingLoadXml = null
let readyTimer = null
let autosaveTimer = null
let savingXml = false
const pendingQueue = []
const exportWaiters = []

function postToEditor(msg) {
  if (editorReady.value && frameRef.value) {
    frameRef.value.contentWindow.postMessage(JSON.stringify(msg), '*')
  } else {
    pendingQueue.push(msg)
  }
}

function requestExport(format) {
  if (!editorReady.value) return Promise.reject(new Error('编辑器尚未就绪'))
  return new Promise((resolve, reject) => {
    exportWaiters.push({ format, resolve, reject })
    postToEditor({ action: 'export', format })
  })
}

function resolveExport(msg) {
  if (msg.error) {
    while (exportWaiters.length) exportWaiters.shift().reject(new Error('导出失败'))
    return
  }
  const w = exportWaiters.shift()
  if (w) w.resolve(msg.data)
}

function buildPayload(xml) {
  return {
    id: currentId.value,
    name: name.value || '未命名设计',
    type: 'FREE',
    description: '',
    nodes: [],
    edges: [],
    lanes: [],
    width: 0,
    height: 0,
    content: xml
  }
}

function doSave(xml, silent) {
  savingXml = true
  currentXml = null
  return saveDiagram(buildPayload(xml))
    .then(res => {
      if (currentId.value == null && res && res.id != null) currentId.value = res.id
      if (!silent) ElMessage.success('已保存')
      dirty.value = false
      loadMyDesigns()
    })
    .catch(e => {
      if (!silent) ElMessage.error(e.message || '保存失败')
    })
    .finally(() => {
      savingXml = false
    })
}

async function save() {
  if (savingXml) return
  let xml = currentXml
  if (!xml) {
    try {
      xml = await requestExport('xml')
    } catch (e) {
      ElMessage.error(e.message)
      return
    }
  }
  await doSave(xml, false)
}

async function saveAs() {
  if (!editorReady.value) {
    ElMessage.warning('编辑器尚未就绪')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入新设计名称', '另存为', {
      inputValue: name.value === '未命名设计' ? '' : name.value + ' 副本',
      inputPlaceholder: '设计名称',
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
    if (value) name.value = value
    currentId.value = null
    const xml = await requestExport('xml')
    await doSave(xml, false)
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

// 自动回传的 xml 静默落盘; 防抖合并连续编辑
function scheduleAutosave() {
  if (currentId.value == null) return
  clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(async () => {
    if (savingXml || currentId.value == null || !currentXml) return
    await doSave(currentXml, true)
  }, 2000)
}

function confirmIfDirty() {
  if (!dirty.value) return Promise.resolve()
  return ElMessageBox.confirm('当前图纸有未保存的修改，是否继续？', '提示', {
    confirmButtonText: '继续',
    cancelButtonText: '取消',
    type: 'warning'
  })
}

// 重挂载 iframe 后由 init→load 握手载入图纸
function mountWithXml(xml) {
  pendingLoadXml = xml
  editorReady.value = false
  loadFailed.value = false
  frameKey.value++
  armReadyTimer()
}

async function openDesign(d) {
  try {
    await confirmIfDirty()
  } catch (e) {
    return
  }
  try {
    const vo = await loadDiagram(d.id)
    if (!vo) return
    const xml = vo.content ? vo.content : legacyToDrawioXml(vo)
    autosaveTimer && clearTimeout(autosaveTimer)
    currentId.value = vo.id
    name.value = vo.name || '未命名设计'
    mountWithXml(xml)
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

function newDesign() {
  confirmIfDirty()
    .then(() => {
      autosaveTimer && clearTimeout(autosaveTimer)
      currentId.value = null
      currentXml = null
      name.value = '未命名设计'
      mountWithXml(emptyXml())
    })
    .catch(() => {})
}

function delDesign(d) {
  deleteDiagram(d.id)
    .then(() => {
      if (d.id === currentId.value) {
        currentId.value = null
        currentXml = null
        dirty.value = false
      }
      loadMyDesigns()
    })
    .catch(() => {})
}

async function downloadPng() {
  try {
    const dataUrl = await requestExport('png')
    const a = document.createElement('a')
    a.href = dataUrl
    a.download = (name.value || '自由绘画') + '.png'
    a.click()
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function exportMermaid() {
  try {
    const xml = await requestExport('xml')
    const mmd = toMermaid('FREEDRAW', xml)
    mmdDlg.value.open(mmd, 'free-draw.mmd')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function loadMyDesigns() {
  try {
    const list = await listDiagrams()
    myDesigns.value = (list || []).filter(d => d.type === 'FREE')
  } catch (e) {}
}

function emptyXml() {
  return (
    '<mxfile><diagram name="页面-1" id="page1"><mxGraphModel dx="1000" dy="700" grid="1" ' +
    'pageWidth="1200" pageHeight="800"><root><mxCell id="0"/><mxCell id="1" parent="0"/>' +
    '</root></mxGraphModel></diagram></mxfile>'
  )
}

function armReadyTimer() {
  clearTimeout(readyTimer)
  readyTimer = setTimeout(() => {
    if (!editorReady.value) loadFailed.value = true
  }, 20000)
}

function retry() {
  loadFailed.value = false
  editorReady.value = false
  frameKey.value++
  armReadyTimer()
}

function onMessage(e) {
  if (!frameRef.value || e.source !== frameRef.value.contentWindow) return
  let msg
  try {
    msg = JSON.parse(e.data)
  } catch (err) {
    return
  }
  switch (msg.event) {
    case 'init':
      editorReady.value = true
      loadFailed.value = false
      clearTimeout(readyTimer)
      while (pendingQueue.length) postToEditor(pendingQueue.shift())
      postToEditor({ action: 'load', xml: pendingLoadXml || emptyXml(), autosave: 1 })
      pendingLoadXml = null
      break
    case 'load':
      dirty.value = false
      currentXml = null
      break
    case 'autosave':
    case 'save':
      currentXml = msg.xml
      dirty.value = true
      scheduleAutosave()
      break
    case 'export':
      resolveExport(msg)
      break
    case 'exit':
      dirty.value = true
      break
  }
}

function onBeforeUnload(e) {
  if (!dirty.value) return
  e.preventDefault()
  e.returnValue = ''
}

onMounted(() => {
  window.addEventListener('message', onMessage)
  window.addEventListener('beforeunload', onBeforeUnload)
  armReadyTimer()
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onMessage)
  window.removeEventListener('beforeunload', onBeforeUnload)
  clearTimeout(readyTimer)
  clearTimeout(autosaveTimer)
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: #2c3e50;
}
.brand-sub {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}
.bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name-input {
  width: 200px;
}
.dirty-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e6a23c;
}
.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.canvas-area {
  flex: 1;
  position: relative;
  min-width: 0;
  background: #fff;
}
.drawio-frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
.canvas-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #909399;
  font-size: 14px;
  line-height: 2;
}
.retry-btn {
  margin-top: 10px;
}
.props {
  width: 240px;
  background: #f7f8fa;
  border-left: 1px solid #e4e7ed;
  padding: 12px;
  overflow-y: auto;
}
.props-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.props-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.props-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 20px 0;
  line-height: 1.8;
}
.save-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.save-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
}
.save-item:hover {
  background: #eef1f5;
}
.save-name {
  cursor: pointer;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.save-name:hover,
.save-name.current {
  color: #1a73e8;
}
</style>
