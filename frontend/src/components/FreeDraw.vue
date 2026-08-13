<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>自由绘画</span>
        <span class="brand-sub">自由画板 · UML 设计工具</span>
      </div>
    </header>

    <div class="free-draw">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button size="small" :type="tool === 'select' ? 'primary' : ''" @click="tool = 'select'">选择</el-button>
        <el-button size="small" :type="tool === 'edge' ? 'primary' : ''" @click="tool = 'edge'">连线</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" :disabled="!graph" @click="clearAll">清空</el-button>
        <el-button size="small" :disabled="!graph" @click="save">保存</el-button>
        <el-button size="small" type="primary" plain :disabled="!graph" @click="downloadPng">导出 PNG</el-button>
      </div>

    <div class="editor-body">
      <!-- 左侧组件库 -->
      <aside class="palette">
        <div class="palette-title">组件库</div>
        <div class="palette-item" v-for="c in catalog" :key="c.type" draggable="true" @dragstart="onDragStart($event, c)" @click="addNodeAtCenter(c)">
          <div class="palette-icon" :style="{ color: c.color }">
            <svg v-if="c.type === 'rect'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="6" width="18" height="12" rx="2"/></svg>
            <svg v-else-if="c.type === 'ellipse'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="12" rx="9" ry="6"/></svg>
            <svg v-else-if="c.type === 'class'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="3" width="16" height="6"/><line x1="4" y1="12" x2="20" y2="12"/><rect x="4" y="12" width="16" height="4"/><line x1="4" y1="19" x2="20" y2="19"/></svg>
            <svg v-else-if="c.type === 'database'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="6" rx="8" ry="3"/><path d="M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6"/><path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/></svg>
            <svg v-else-if="c.type === 'actor'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="6" r="3"/><path d="M5 21c0-3.9 3.1-7 7-7s7 3.1 7 7"/></svg>
            <svg v-else-if="c.type === 'start'" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><circle cx="12" cy="12" r="7"/></svg>
            <svg v-else-if="c.type === 'end'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="4" fill="currentColor"/></svg>
            <svg v-else-if="c.type === 'decision'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l9 9-9 9-9-9z"/></svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="6" width="18" height="12" rx="2"/></svg>
          </div>
          <span>{{ c.label }}</span>
        </div>
        <div class="palette-tip">拖拽到画布或点击添加</div>
      </aside>

      <!-- 中间画布 -->
      <div class="canvas-area">
        <div ref="containerRef" class="x6-container"></div>
        <div v-if="!graph" class="canvas-loading">画布初始化中...</div>
      </div>

      <!-- 右侧属性面板 -->
      <aside class="props">
        <div class="props-title">属性</div>
        <template v-if="current">
          <div class="prop-field">
            <label>名称</label>
            <el-input v-model="currentLabel" size="small" @change="applyLabel" />
          </div>
          <div class="prop-field">
            <label>颜色</label>
            <el-color-picker v-model="currentColor" size="small" @change="applyColor" />
          </div>
          <div class="prop-field">
            <label>字号</label>
            <el-input-number v-model="currentFontSize" :min="10" :max="32" size="small" @change="applyFont" />
          </div>
          <div class="prop-field">
            <el-button size="small" type="danger" plain @click="removeCurrent">删除节点</el-button>
          </div>
        </template>
        <div v-else class="props-empty">
          选中节点后<br />可编辑属性
        </div>
        <div class="props-title" style="margin-top: 16px">我的设计</div>
        <div class="save-list">
          <div v-for="d in myDesigns" :key="d.id" class="save-item">
            <span class="save-name" @click="loadDesign(d)">{{ d.name }}</span>
            <el-button size="small" text type="danger" @click="delDesign(d)">×</el-button>
          </div>
          <div v-if="!myDesigns.length" class="props-empty">暂无保存</div>
        </div>
      </aside>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Graph } from '@antv/x6'
import { saveDiagram, listDiagrams, loadDiagram, deleteDiagram } from '../api/diagram'

const router = useRouter()
function goBack() {
  router.back()
}

const containerRef = ref(null)
const graph = ref(null)
const tool = ref('select')
const current = ref(null)
const currentLabel = ref('')
const currentColor = ref('')
const currentFontSize = ref(14)
const myDesigns = ref([])

const catalog = [
  { type: 'rect', label: '矩形', color: '#3B6BFF' },
  { type: 'ellipse', label: '椭圆', color: '#10b981' },
  { type: 'class', label: '类', color: '#7c3aed' },
  { type: 'database', label: '数据库', color: '#f59e0b' },
  { type: 'actor', label: '角色', color: '#0ea5e9' },
  { type: 'start', label: '开始', color: '#10b981' },
  { type: 'end', label: '结束', color: '#ef4444' },
  { type: 'decision', label: '判断', color: '#f59e0b' }
]

function registerShapes() {
  Graph.registerNode('freerect', {
    inherit: 'rect',
    width: 120,
    height: 60,
    attrs: {
      body: { fill: '#fff', stroke: '#3B6BFF', strokeWidth: 1.5, rx: 6 },
      label: { text: '', fontSize: 14, fill: '#333' }
    }
  }, true)
  Graph.registerNode('freeellipse', {
    inherit: 'ellipse',
    width: 140,
    height: 70,
    attrs: {
      body: { fill: '#fff', stroke: '#10b981', strokeWidth: 1.5 },
      label: { text: '', fontSize: 14, fill: '#333' }
    }
  }, true)
  Graph.registerNode('freedatabase', {
    inherit: 'rect',
    width: 130,
    height: 70,
    markup: [
      { tagName: 'ellipse', selector: 'top' },
      { tagName: 'rect', selector: 'body' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      top: { cx: 0, cy: 0, rx: 18, ry: 9, fill: '#fff', stroke: '#f59e0b', strokeWidth: 1.5, refX: 0.5, refY: 0 },
      body: { refWidth: '100%', refHeight: '100%', fill: '#fff', stroke: '#f59e0b', strokeWidth: 1.5 },
      label: { text: '', fontSize: 13, fill: '#333', refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freeactor', {
    inherit: 'rect',
    width: 100,
    height: 100,
    markup: [
      { tagName: 'image', selector: 'img' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      img: {
        'xlink:href': '/xiaoren.svg',
        width: 40,
        height: 40,
        x: 0,
        y: 0,
        refX: 0.5,
        refY: 0.3,
        xAlign: 'middle',
        yAlign: 'middle'
      },
      label: { text: '', fontSize: 13, fill: '#333', refX: 0.5, refY: 0.75, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freeclass', {
    inherit: 'html',
    width: 180,
    height: 100,
    html(cell) {
      const data = cell.getData() || {}
      const esc = s => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      const name = esc(data.name || 'Class')
      const attrs = esc(data.attrs || '-')
      const methods = esc(data.methods || '-')
      return `<div style="width:100%;height:100%;display:flex;flex-direction:column;box-sizing:border-box;background:#fff;border:1.5px solid #7c3aed;overflow:hidden">
        <div style="text-align:center;font-weight:bold;font-size:14px;padding:6px 8px;border-bottom:1.5px solid #7c3aed;background:#f5f3ff;color:#333">${name}</div>
        <div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap;flex:1">${attrs}</div>
        <div style="border-top:1px solid #999"></div>
        <div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${methods}</div>
      </div>`
    }
  }, true)
}

function nodeSpec(type) {
  switch (type) {
    case 'rect': return { shape: 'freerect', w: 120, h: 60 }
    case 'ellipse': return { shape: 'freeellipse', w: 140, h: 70 }
    case 'database': return { shape: 'freedatabase', w: 130, h: 70 }
    case 'actor': return { shape: 'freeactor', w: 100, h: 100 }
    case 'class': return { shape: 'freeclass', w: 180, h: 100 }
    case 'start': return { shape: 'circle', w: 40, h: 40, label: '' }
    case 'end': return { shape: 'circle', w: 40, h: 40, label: '' }
    case 'decision': return { shape: 'polygon', w: 100, h: 70, points: '50,0 100,35 50,70 0,35' }
    default: return { shape: 'rect', w: 120, h: 60 }
  }
}

function defaultLabel(type) {
  return { rect: '节点', ellipse: '椭圆', database: '数据库', actor: '角色', class: '类', start: '开始', end: '结束', decision: '判断' }[type] || '节点'
}

function addNodeAtCenter(c) {
  if (!graph.value) return
  const spec = nodeSpec(c.type)
  const id = 'n' + Date.now()
  const pos = graph.value.getGraphAreaCenter ? graph.value.getGraphAreaCenter() : { x: 300, y: 200 }
  const nodeData = {
    id,
    shape: spec.shape,
    x: pos.x - spec.w / 2,
    y: pos.y - spec.h / 2,
    width: spec.w,
    height: spec.h,
    label: c.label || defaultLabel(c.type),
    data: { type: c.type }
  }
  if (c.type === 'start') nodeData.attrs = { body: { fill: '#10b981', stroke: '#10b981' } }
  if (c.type === 'end') nodeData.attrs = { body: { fill: '#ef4444', stroke: '#ef4444' } }
  if (c.type === 'class') nodeData.data = { name: 'Class', attrs: '- 属性', methods: '+ 方法' }
  graph.value.addNode(nodeData)
}

function onDragStart(e, c) {
  e.dataTransfer.setData('application/x6-node', JSON.stringify({ catalogType: c.type }))
}

function initGraph() {
  registerShapes()
  const g = new Graph({
    container: containerRef.value,
    width: containerRef.value.clientWidth || 800,
    height: containerRef.value.clientHeight || 600,
    background: { color: '#fafbfc' },
    grid: { size: 10, visible: true },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    autoResize: true,
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
      snap: true,
      allowBlank: false,
      allowMulti: true
    },
    snapline: true,
    highlighting: true
  })
  graph.value = g

  // 画布点击放置(拖动组件到画布)
  containerRef.value.addEventListener('dragover', e => e.preventDefault())
  containerRef.value.addEventListener('drop', e => {
    e.preventDefault()
    const raw = e.dataTransfer.getData('application/x6-node')
    if (!raw) return
    const { catalogType } = JSON.parse(raw)
    const c = catalog.find(x => x.type === catalogType)
    if (!c) return
    const spec = nodeSpec(c.type)
    const pos = g.clientToLocal(e.clientX, e.clientY)
    const id = 'n' + Date.now()
    const nodeData = {
      id,
      shape: spec.shape,
      x: pos.x - spec.w / 2,
      y: pos.y - spec.h / 2,
      width: spec.w,
      height: spec.h,
      label: c.label || defaultLabel(c.type),
      data: { type: c.type }
    }
    if (c.type === 'start') nodeData.attrs = { body: { fill: '#10b981', stroke: '#10b981' } }
    if (c.type === 'end') nodeData.attrs = { body: { fill: '#ef4444', stroke: '#ef4444' } }
    if (c.type === 'class') nodeData.data = { name: 'Class', attrs: '- 属性', methods: '+ 方法' }
    g.addNode(nodeData)
  })

  // 连线工具
  g.on('blank:click', () => {
    if (tool.value === 'select') current.value = null
  })
  g.on('node:click', ({ node }) => {
    if (tool.value === 'select') {
      current.value = node
      currentLabel.value = node.attr('label/text') || node.getData()?.name || ''
      currentFontSize.value = node.attr('label/fontSize') || 14
      const bodyFill = node.attr('body/fill')
      currentColor.value = bodyFill && bodyFill !== '#fff' ? bodyFill : '#3B6BFF'
    }
  })
  g.on('node:change:position', () => {})
  // 连线模式: 点击两个节点连线
  let edgeSource = null
  g.on('node:click', ({ node }) => {
    if (tool.value === 'edge') {
      if (!edgeSource) {
        edgeSource = node
        ElMessage.info('再点击目标节点连线')
      } else {
        if (edgeSource !== node) {
          g.addEdge({ source: edgeSource.id, target: node.id, attrs: { line: { stroke: '#333', strokeWidth: 1.5 } } })
        }
        edgeSource = null
      }
    }
  })
}

function applyLabel() {
  if (!current.value) return
  if (current.value.shape === 'freeclass') {
    const d = current.value.getData() || {}
    d.name = currentLabel.value
    current.value.setData(d)
  } else {
    current.value.attr('label/text', currentLabel.value)
  }
}

function applyColor() {
  if (!current.value) return
  current.value.attr('body/fill', currentColor.value)
}

function applyFont() {
  if (!current.value) return
  current.value.attr('label/fontSize', currentFontSize.value)
}

function removeCurrent() {
  if (current.value) {
    graph.value.removeNode(current.value)
    current.value = null
  }
}

function clearAll() {
  graph.value.clearCells()
  current.value = null
}

function serialize() {
  const nodes = graph.value.getNodes().map(n => {
    const pos = n.position()
    const data = n.getData() || {}
    return {
      id: n.id,
      shape: data.type || 'rect',
      label: n.attr('label/text') || data.name || '',
      x: pos.x,
      y: pos.y,
      width: n.size().width,
      height: n.size().height,
      attrsText: data.attrs,
      methodsText: data.methods
    }
  })
  const edges = graph.value.getEdges().map(e => ({
    id: e.id,
    source: e.getSourceCellId(),
    target: e.getTargetCellId(),
    label: ''
  }))
  return { nodes, edges }
}

async function save() {
  const { nodes, edges } = serialize()
  const data = {
    name: '自由绘画_' + new Date().toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 5),
    type: 'FREE',
    nodes,
    edges,
    width: 1200,
    height: 800
  }
  try {
    const saved = await saveDiagram(data)
    ElMessage.success('已保存')
    loadMyDesigns()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function loadMyDesigns() {
  try {
    const list = await listDiagrams()
    myDesigns.value = (list || []).filter(d => d.type === 'FREE')
  } catch (e) {}
}

async function loadDesign(d) {
  try {
    const vo = await loadDiagram(d.id)
    graph.value.clearCells()
    ;(vo.nodes || []).forEach(n => {
      const c = catalog.find(x => x.type === n.shape)
      const spec = nodeSpec(n.shape)
      const nodeData = {
        id: n.id,
        shape: spec.shape,
        x: n.x,
        y: n.y,
        width: n.width || spec.w,
        height: n.height || spec.h,
        label: n.label,
        data: { type: n.shape, name: n.label, attrs: n.attrsText, methods: n.methodsText }
      }
      if (n.shape === 'start') nodeData.attrs = { body: { fill: '#10b981', stroke: '#10b981' } }
      if (n.shape === 'end') nodeData.attrs = { body: { fill: '#ef4444', stroke: '#ef4444' } }
      graph.value.addNode(nodeData)
    })
    ;(vo.edges || []).forEach(e => {
      graph.value.addEdge({ source: e.source, target: e.target, attrs: { line: { stroke: '#333', strokeWidth: 1.5 } } })
    })
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

async function delDesign(d) {
  try {
    await deleteDiagram(d.id)
    loadMyDesigns()
  } catch (e) {}
}

async function downloadPng() {
  if (!graph.value) return
  try {
    const dataUrl = await graph.value.toPNG({
      backgroundColor: '#fff',
      padding: 20
    })
    const a = document.createElement('a')
    a.href = dataUrl
    a.download = '自由绘画.png'
    a.click()
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  nextTickTick(() => {
    initGraph()
  })
  loadMyDesigns()
})

function nextTickTick(fn) {
  requestAnimationFrame(() => requestAnimationFrame(fn))
}

onBeforeUnmount(() => {
  if (graph.value) graph.value.dispose()
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
  padding: 14px 30px;
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
.free-draw {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #f5f6fa;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.palette {
  width: 180px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  padding: 12px;
  overflow-y: auto;
}
.palette-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: grab;
  font-size: 13px;
  color: #606266;
  background: #fafbfc;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.palette-item:hover {
  border-color: #3B6BFF;
  box-shadow: 0 2px 8px rgba(59, 107, 255, 0.15);
}
.palette-icon {
  display: flex;
  align-items: center;
}
.palette-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.canvas-area {
  flex: 1;
  position: relative;
  min-width: 0;
}
.x6-container {
  width: 100%;
  height: 100%;
}
.canvas-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 14px;
}
.props {
  width: 220px;
  background: #fff;
  border-left: 1px solid #ebeef5;
  padding: 12px;
  overflow-y: auto;
}
.props-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.prop-field {
  margin-bottom: 12px;
}
.prop-field label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
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
  border-radius: 6px;
  font-size: 13px;
}
.save-item:hover {
  background: #f5f7fa;
}
.save-name {
  cursor: pointer;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.save-name:hover {
  color: #3B6BFF;
}
</style>
