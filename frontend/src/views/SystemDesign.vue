<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/')">‹ 返回</el-button>
        <span>系统图设计</span>
      </div>
      <div class="type-tabs">
        <el-radio-group v-model="type" size="small" @change="onTypeChange">
          <el-radio-button value="FLOW">流程图</el-radio-button>
          <el-radio-button value="ARCH">架构图</el-radio-button>
          <el-radio-button value="SWIMLANE">泳道图</el-radio-button>
        </el-radio-group>
      </div>
      <div class="actions">
        <el-button size="small" :disabled="!graphReady" @click="save">保存</el-button>
        <el-button size="small" plain :disabled="!graphReady" @click="downloadSvg">SVG</el-button>
        <el-button size="small" type="primary" plain :disabled="!graphReady" @click="downloadPng">导出 PNG</el-button>
      </div>
    </header>

    <main class="body">
      <section class="input-panel">
        <!-- 架构图: 结构化配置表单 -->
        <template v-if="type === 'ARCH'">
          <div class="input-title">系统架构配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="系统名称">
              <el-input v-model="config.systemName" placeholder="如：会员卡系统" />
            </el-form-item>
          </el-form>
          <div class="layer-config">
            <div v-for="(layer, li) in config.layers" :key="li" class="layer-config-card">
              <div class="layer-config-head">
                <el-input v-model="layer.name" size="small" placeholder="层名称，如：客户端" class="layer-name-input" />
                <el-button size="small" text type="danger" @click="removeLayer(li)">删除层</el-button>
              </div>
              <div class="layer-comp" v-for="(comp, ci) in layer.components" :key="ci">
                <el-input v-model="comp.name" size="small" placeholder="组件名，如：Vue / SpringBoot / MySQL / Redis" />
                <el-button size="small" text type="danger" @click="removeComp(li, ci)">×</el-button>
              </div>
              <el-button size="small" class="add-comp-btn" @click="addComp(li)">+ 添加组件</el-button>
            </div>
            <el-button size="small" class="add-layer-btn" @click="addLayer">+ 添加层</el-button>
          </div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成架构图</el-button>
            <span class="tip">层名和每层组件可自由编辑</span>
          </div>
        </template>

        <!-- 流程图/泳道图: 文本描述 -->
        <template v-else>
          <div class="input-title">请输入流程脚本</div>
          <el-input
            v-model="description"
            type="textarea"
            :rows="9"
            placeholder="查询会员余额&#10;if(余额 >= 商品金额)&#10;    扣除余额&#10;    保存订单&#10;else&#10;    返回余额不足"
          />
          <div class="dsl-tip">普通文本 = 节点；if(条件) = 判断(菱形)；else = 否分支；缩进 = 归属</div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成设计图</el-button>
            <span class="tip">含 if( 的脚本走分支解析</span>
          </div>
          <div class="examples">
            <div class="ex-title">示例：</div>
            <div class="ex-item" v-for="(ex, i) in examples" :key="i" @click="useExample(ex)">
              <span class="ex-tag">{{ ex.typeText }}</span>{{ ex.text }}
            </div>
          </div>
        </template>
      </section>

      <section class="canvas-wrap">
        <div v-if="!graphReady && currentVO?.type !== 'ARCH'" class="canvas-empty">配置后点击「生成」预览</div>
        <div v-if="graphReady && currentVO && currentVO.type === 'ARCH'" class="arch-box" ref="archRef">
          <div class="arch-outer">
            <div class="arch-title">{{ currentVO.name || '系统架构图' }}</div>
            <template v-for="(layer, li) in archLayers" :key="layer.name">
              <div class="arch-divider"></div>
              <div class="arch-layer">
                <div class="arch-layer-name">{{ layer.name }}</div>
                <div class="arch-layer-body">
                  <div v-for="(node, i) in layer.nodes" :key="i" class="arch-comp">
                    {{ node.label }}
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div class="arch-export">
            <el-button size="small" plain @click="downloadArchSvg">SVG</el-button>
            <el-button size="small" type="primary" plain @click="downloadArchPng">PNG</el-button>
          </div>
        </div>
        <!-- FLOW/SWIMLANE: X6 画布 -->
        <div v-if="graphReady && currentVO && currentVO.type !== 'ARCH'" ref="container" class="x6-container"></div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Graph } from '@antv/x6'
import { DagreLayout } from '@antv/layout'
import html2canvas from 'html2canvas'
import { generateDiagram, saveDiagram } from '../api/diagram'

// 注册数据库圆柱形状
Graph.registerNode('db', {
  inherit: 'rect',
  markup: [
    { tagName: 'ellipse', selector: 'top' },
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    top: { cx: 0, cy: 0, rx: 16, ry: 8, fill: '#fff', stroke: '#333', strokeWidth: 1.5, refX: 0.5, refY: 0 },
    body: { refWidth: '100%', refHeight: '100%', fill: '#fff', stroke: '#333', strokeWidth: 1.5 },
    label: { text: '', fontSize: 12, fill: '#333', refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  }
})

let graph = null
const container = ref(null)
const archRef = ref(null)
const type = ref('ARCH')
const description = ref('')
const generating = ref(false)
const graphReady = ref(false)
const currentVO = ref(null)

// 架构图配置: 动态层结构(默认示例)
const config = ref({
  systemName: '会员卡系统',
  layers: [
    { name: '客户端', components: [{ name: 'Vue' }, { name: '后台管理系统' }] },
    { name: '业务层', components: [{ name: '用户管理API' }, { name: '订单API' }] },
    { name: '运行支持层', components: [{ name: 'Nginx' }, { name: 'Docker' }] },
    { name: '服务应用层', components: [{ name: 'SpringBoot1' }, { name: 'SpringBoot2' }] },
    { name: '数据层', components: [{ name: 'Redis' }, { name: 'RabbitMQ' }] },
    { name: '数据库', components: [{ name: 'MySQL 主' }, { name: 'MySQL 从' }] }
  ]
})

function addLayer() {
  config.value.layers.push({ name: '', components: [{ name: '' }] })
}
function removeLayer(li) {
  if (config.value.layers.length <= 1) {
    ElMessage.warning('至少保留一层')
    return
  }
  config.value.layers.splice(li, 1)
}
function addComp(li) {
  config.value.layers[li].components.push({ name: '' })
}
function removeComp(li, ci) {
  if (config.value.layers[li].components.length <= 1) {
    ElMessage.warning('至少保留一个组件')
    return
  }
  config.value.layers[li].components.splice(ci, 1)
}

const examples = [
  { typeText: '流程', type: 'FLOW', text: '查询会员余额\nif(余额 >= 商品金额)\n    扣除余额\n    保存订单\nelse\n    返回余额不足' },
  { typeText: '流程', type: 'FLOW', text: '用户登录\nif(账号存在)\n    验证密码\n    if(密码正确)\n        登录成功\n    else\n        提示密码错误\nelse\n    提示账号不存在' },
  { typeText: '架构图', type: 'ARCH', text: '用户通过小程序访问会员服务，会员服务调用订单服务，订单服务查询MySQL数据库' },
  { typeText: '泳道图', type: 'SWIMLANE', text: '用户提交充值申请，商户审核申请，系统更新余额，用户确认到账' }
]

const archLayers = computed(() => {
  if (!currentVO.value || currentVO.value.type !== 'ARCH') return []
  // 按用户配置的层顺序分组
  const groups = {}
  ;(currentVO.value.nodes || []).forEach(n => {
    if (n.lane) {
      if (!groups[n.lane]) groups[n.lane] = []
      groups[n.lane].push(n)
    }
  })
  // 按 config.layers 顺序输出, 未配置的层不显示
  const order = config.value.layers.map(l => l.name).filter(Boolean)
  const result = []
  order.forEach(name => {
    if (groups[name]) result.push({ name, nodes: groups[name] })
  })
  // 兼容后端返回但前端未列出的层
  Object.keys(groups).forEach(name => {
    if (!order.includes(name)) result.push({ name, nodes: groups[name] })
  })
  return result
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (graph) graph.dispose()
})

function handleResize() {
  if (graph && container.value) {
    graph.resize(container.value.clientWidth, container.value.clientHeight)
  }
}

function onTypeChange() {
  // 切类型后清空, 重新生成
  description.value = ''
  graphReady.value = false
  currentVO.value = null
}

function useExample(ex) {
  type.value = ex.type
  description.value = ex.text
}

function nodeShapeName(shape, type) {
  if (shape === 'start' || shape === 'end') return 'ellipse'
  if (shape === 'condition') return 'polygon'
  if (shape === 'database' || shape === 'cache' || shape === 'mq') return 'db'
  if (shape === 'actor' && type === 'ARCH') return 'rect'
  if (shape === 'actor') return 'ellipse'
  return 'rect'
}

function nodeAttrs(node) {
  const color = '#333333'
  const label = { text: node.label, fill: '#333', fontSize: 12, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  if (node.shape === 'start' || node.shape === 'end') {
    return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5 }, label }
  }
  if (node.shape === 'condition') {
    return {
      body: { refPoints: '0,10 10,0 20,10 10,20', fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  if (node.shape === 'service' || node.shape === 'web' || node.shape === 'gateway' || node.shape === 'third' ||
      node.shape === 'search' || node.shape === 'storage') {
    return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5, rx: 8, ry: 8 }, label }
  }
  if (node.shape === 'database' || node.shape === 'cache' || node.shape === 'mq') {
    return {
      body: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      top: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  // action
  return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5 }, label }
}

async function renderGraph(vo) {
  if (vo.type === 'ARCH') return // ARCH 用 HTML 渲染, 不用 X6
  graph.clearCells()
  const nodes = vo.nodes.map(n => {
    const w = Math.max(120, n.label.length * 14 + 30)
    const h = (n.shape === 'start' || n.shape === 'end') ? 56 : 48
    return {
      id: n.id,
      shape: nodeShapeName(n.shape, vo.type),
      x: n.x,
      y: n.y,
      width: w,
      height: h,
      lane: n.lane || '',
      attrs: nodeAttrs(n)
    }
  })
  const edges = vo.edges.map(e => ({
    id: e.id,
    source: e.source,
    target: e.target,
    attrs: {
      line: { stroke: '#333333', strokeWidth: 1.5, targetMarker: 'block' },
      label: { text: e.label || '', fill: '#666', fontSize: 11 }
    }
  }))

  // FLOW: 用 Dagre 布局(适合分支流程)
  if (vo.type === 'FLOW' && nodes.length > 0) {
    try {
      const layout = new DagreLayout({
        type: 'dagre',
        rankdir: 'TB',
        ranksep: 70,
        nodesep: 60
      })
      await layout.execute({
        nodes: nodes.map(n => ({ id: n.id, width: n.width, height: n.height })),
        edges: edges.map(e => ({ source: e.source, target: e.target }))
      })
      layout.forEachNode(node => {
        const n = nodes.find(x => x.id === node.id)
        if (n) {
          n.x = Math.round(node.x - n.width / 2)
          n.y = Math.round(node.y - n.height / 2)
        }
      })
      layout.destroy()
    } catch (e) {
      // Dagre 失败则用后端坐标
    }
  }

  graph.fromJSON({ nodes, edges })
  graph.centerContent()
}

async function generate() {
  let payload
  if (type.value === 'ARCH') {
    // 至少配置一个层且一个组件
    const hasAny = config.value.layers.some(l =>
      l.name && (l.components || []).some(c => c.name)
    )
    if (!hasAny) {
      ElMessage.warning('请至少配置一个层和一个组件')
      return
    }
    payload = { type: 'ARCH', config: config.value }
  } else {
    if (!description.value.trim()) {
      ElMessage.warning('请输入系统描述')
      return
    }
    payload = { type: type.value, description: description.value }
  }
  generating.value = true
  try {
    const vo = await generateDiagram(payload)
    currentVO.value = vo
    graphReady.value = true
    await nextTick()
    if (vo.type !== 'ARCH') {
      if (!graph) initGraph()
      renderGraph(vo)
    }
  } catch (e) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = false
  }
}

function initGraph() {
  graph = new Graph({
    container: container.value,
    width: container.value.clientWidth || 800,
    height: container.value.clientHeight || 600,
    background: { color: '#fafbfc' },
    grid: { size: 10, visible: true },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    autoResize: true
  })
}

async function save() {
  if (!currentVO.value) return
  try {
    const data = { ...currentVO.value, name: type.value }
    const saved = await saveDiagram(data)
    currentVO.value = saved
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

function getSvgString() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return ''
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  return '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
}

function downloadSvg() {
  const svg = getSvgString()
  if (!svg) return
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '系统图.svg'
  a.click()
  URL.revokeObjectURL(url)
}

function downloadPng() {
  const svg = getSvgString()
  if (!svg) return
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const img = new Image()
  img.onload = () => {
    const canvas = document.createElement('canvas')
    const box = svg.match(/viewBox="([^"]+)"/)
    let w = 800, h = 600
    if (box && box[1]) {
      const p = box[1].split(/\s+/).map(Number)
      if (p.length === 4) {
        w = Math.max(200, Math.round(p[2] - p[0]))
        h = Math.max(200, Math.round(p[3] - p[1]))
      }
    }
    const scale = 2
    canvas.width = w * scale
    canvas.height = h * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    URL.revokeObjectURL(url)
    canvas.toBlob(b => {
      if (!b) return
      const a = document.createElement('a')
      a.href = URL.createObjectURL(b)
      a.download = '系统图.png'
      a.click()
      URL.revokeObjectURL(a.href)
      ElMessage.success('PNG 已导出')
    }, 'image/png')
  }
  img.onerror = () => {
    URL.revokeObjectURL(url)
    ElMessage.error('PNG 导出失败')
  }
  img.src = url
}

// ARCH 导出: SVG
// ARCH 导出: 用 html2canvas 截图 DOM
async function archCanvas() {
  if (!archRef.value) return null
  const el = archRef.value.querySelector('.arch-outer')
  if (!el) return null
  const canvas = await html2canvas(el, {
    backgroundColor: '#ffffff',
    scale: 2,
    useCORS: true
  })
  return canvas
}

async function downloadArchPng() {
  const canvas = await archCanvas()
  if (!canvas) {
    ElMessage.error('导出失败')
    return
  }
  canvas.toBlob(b => {
    if (!b) return
    const a = document.createElement('a')
    a.href = URL.createObjectURL(b)
    a.download = (currentVO.value?.name || '系统架构图') + '.png'
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('PNG 已导出')
  }, 'image/png')
}

async function downloadArchSvg() {
  const canvas = await archCanvas()
  if (!canvas) {
    ElMessage.error('导出失败')
    return
  }
  const dataUrl = canvas.toDataURL('image/png')
  // 用 PNG 数据包一层 SVG, 保证可打开
  const svgStr = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${canvas.width}" height="${canvas.height}">
  <image href="${dataUrl}" x="0" y="0" width="${canvas.width}" height="${canvas.height}"/>
</svg>`
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = (currentVO.value?.name || '系统架构图') + '.svg'
  a.click()
  URL.revokeObjectURL(url)
}
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
  gap: 16px;
  padding: 12px 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #2c3e50;
}
.type-tabs {
  flex: 1;
  display: flex;
  justify-content: center;
}
.actions {
  display: flex;
  gap: 8px;
}
.body {
  flex: 1;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 0;
  min-height: 0;
}
.input-panel {
  background: #fff;
  border-right: 1px solid #ebeef5;
  padding: 20px 16px;
  overflow-y: auto;
}
.input-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.dsl-tip {
  font-size: 11px;
  color: #909399;
  line-height: 1.5;
  margin-top: 6px;
  background: #f8f9fb;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 6px 10px;
}
.layer-config {
  margin-top: 4px;
}
.layer-config-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.layer-config-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.layer-name-input {
  flex: 1;
}
.layer-comp {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.comp-type {
  width: 90px;
}
.add-comp-btn {
  width: 100%;
}
.add-layer-btn {
  width: 100%;
}
.input-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.tip {
  font-size: 11px;
  color: #909399;
  line-height: 1.5;
}
.examples {
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.ex-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.ex-item {
  font-size: 12px;
  color: #606266;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  line-height: 1.5;
  transition: all 0.15s;
}
.ex-item:hover {
  border-color: #3B6BFF;
  color: #3B6BFF;
}
.ex-tag {
  display: inline-block;
  font-size: 11px;
  color: #3B6BFF;
  background: #eef1ff;
  border-radius: 3px;
  padding: 0 5px;
  margin-right: 6px;
}
.canvas-wrap {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.arch-export {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.arch-box {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 20px;
  background: #fafbfc;
}
.arch-outer {
  border: 2px solid #333;
  padding: 0;
  background: #fff;
}
.arch-title {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #333;
  padding: 12px 0;
  border-bottom: 2px solid #333;
}
.arch-divider {
  height: 0;
  border-top: 2px dashed #333;
  margin: 0;
}
.arch-layer {
  display: flex;
  align-items: stretch;
  min-height: 60px;
}
.arch-layer-name {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  background: #f5f5f5;
  border-right: 1px solid #ddd;
  flex-shrink: 0;
  padding: 8px 4px;
}
.arch-layer-body {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}
.arch-comp {
  border: 1.5px solid #333;
  border-radius: 6px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  background: #fff;
}
.x6-container {
  width: 100%;
  height: 100%;
}
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background: #fafbfc;
}
</style>
