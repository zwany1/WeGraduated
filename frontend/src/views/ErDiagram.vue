<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/home')">‹ 返回</el-button>
        <span>ER 图生成</span>
      </div>
      <div class="actions">
        <el-button @click="resetExample">重置示例</el-button>
        <el-button :disabled="!graphReady" @click="saveLayout">保存布局</el-button>
        <el-button type="success" plain @click="exportMermaid">Mermaid</el-button>
        <el-button type="primary" :loading="rendering" @click="render">生成 ER 图</el-button>
      </div>
    </header>

    <MermaidExportDialog ref="mmdDlg" />

    <main class="content">
      <section class="panel left">
        <h3>实体定义</h3>
        <div class="entity-card" v-for="(e, ei) in entities" :key="ei">
          <div class="entity-head">
            <el-input v-model="e.name" placeholder="实体名，如：学生" size="small" />
            <el-button size="small" text type="danger" @click="removeEntity(ei)">删除</el-button>
          </div>
          <div class="attr-row" v-for="(a, ai) in e.attrs" :key="ai">
            <el-input v-model="a.name" placeholder="属性名" size="small" />
            <el-checkbox v-model="a.key" size="small">主键</el-checkbox>
            <el-button size="small" text type="danger" @click="removeAttr(ei, ai)">×</el-button>
          </div>
          <el-button size="small" class="add-btn" @click="addAttr(ei)">+ 添加属性</el-button>
        </div>
        <el-button class="add-entity" @click="addEntity">+ 添加实体</el-button>

        <h3>关系定义</h3>
        <div class="rel-tip">Chen 记法: 实体为矩形, 属性为椭圆, 关系为菱形; N:M 关系的属性(如购买数量)可直接填在关系下方</div>
        <div class="rel-card" v-for="(r, ri) in relations" :key="ri">
          <div class="rel-head">
            <el-select v-model="r.from" size="small" placeholder="实体1">
              <el-option v-for="n in entityNames" :key="n" :label="n" :value="n" />
            </el-select>
            <span class="rel-diamond">◇</span>
            <el-input v-model="r.label" placeholder="关系名" size="small" class="rel-label-input" />
            <span class="rel-diamond">◇</span>
            <el-select v-model="r.to" size="small" placeholder="实体2">
              <el-option v-for="n in entityNames" :key="n" :label="n" :value="n" />
            </el-select>
            <el-input v-model="r.cardinality" placeholder="基数 m:n" size="small" class="card-input" />
            <el-button size="small" text type="danger" @click="removeRelation(ri)">删除</el-button>
          </div>
          <div class="rel-attrs-header">关系属性</div>
          <div class="attr-row" v-for="(a, ai) in r.attrs" :key="'ra' + ri + '-' + ai">
            <el-input v-model="a.name" placeholder="属性名" size="small" />
            <el-button size="small" text type="danger" @click="removeRelAttr(ri, ai)">×</el-button>
          </div>
          <el-button size="small" class="add-btn" @click="addRelAttr(ri)">+ 添加关系属性</el-button>
        </div>
        <el-button class="add-entity" @click="addRelation">+ 添加关系</el-button>

        <h3>绘制设置</h3>
        <el-form label-width="70px" style="max-width: 280px">
          <el-form-item label="字号">
            <el-select v-model="fontSize" style="width: 120px">
              <el-option label="小五 (9pt)" :value="9" />
              <el-option label="五号 (10pt)" :value="10" />
              <el-option label="小四 (12pt)" :value="12" />
              <el-option label="四号 (14pt)" :value="14" />
            </el-select>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel right">
        <div class="right-head">
          <h3>预览</h3>
          <div v-if="graphReady" class="download-group">
            <el-button size="small" plain @click="downloadSvg">下载 SVG</el-button>
            <el-button size="small" type="primary" plain @click="downloadPng">下载 PNG</el-button>
          </div>
        </div>
        <div v-if="problems.length" class="validate-panel" :class="{ 'has-error': hasErrors }">
          <div class="validate-title">
            校验结果
            <el-tag size="small" :type="hasErrors ? 'danger' : 'warning'" class="validate-count">
              {{ problems.filter(p => p.type === 'error').length }} 错误 / {{ problems.filter(p => p.type === 'warning').length }} 警告
            </el-tag>
          </div>
          <ul class="validate-list">
            <li v-for="(p, i) in problems" :key="i" :class="p.type">
              <span class="v-badge">{{ p.type === 'error' ? '错误' : '警告' }}</span>
              {{ p.message }}
            </li>
          </ul>
        </div>
        <div class="preview-box">
          <div ref="container" class="x6-container" v-show="graphReady"></div>
          <div v-if="!graphReady" class="preview-empty">
            编辑左侧实体与关系后，点击「生成 ER 图」预览
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Graph } from '@antv/x6'
import { getErGraph, saveErLayout, loadErLayout } from '../api/er'
import { validateEr } from '../utils/erValidate'
import { toMermaid } from '../utils/mermaid'
import MermaidExportDialog from '../components/MermaidExportDialog.vue'

let graph = null
const mmdDlg = ref(null)

const entities = ref([
  {
    name: '学生',
    attrs: [
      { name: 'id', key: true },
      { name: '用户名', key: false },
      { name: '密码', key: false },
      { name: '出生年月', key: false }
    ]
  },
  {
    name: '班级',
    attrs: [
      { name: 'id', key: true },
      { name: '名称', key: false }
    ]
  },
  {
    name: '饭卡',
    attrs: [
      { name: 'id', key: true },
      { name: '余额', key: false }
    ]
  },
  {
    name: '菜品',
    attrs: [
      { name: 'id', key: true },
      { name: '名称', key: false },
      { name: '价格', key: false }
    ]
  }
])
const relations = ref([
  { from: '学生', to: '菜品', label: '关注', cardinality: 'm:n', attrs: [] },
  { from: '学生', to: '班级', label: '属于', cardinality: 'm:1', attrs: [] },
  { from: '学生', to: '饭卡', label: '拥有', cardinality: '1:1', attrs: [] },
  { from: '饭卡', to: '菜品', label: '购买', cardinality: 'm:n', attrs: [{ name: '购买数量', key: false }, { name: '购买时间', key: false }] }
])
const fontSize = ref(12)
const rendering = ref(false)
const graphReady = ref(false)
const container = ref(null)
const problems = ref([])
const hasErrors = ref(false)

const entityNames = computed(() =>
  entities.value.map(e => e.name.trim()).filter(n => n)
)

function addEntity() {
  entities.value.push({ name: '', attrs: [{ name: '', key: false }] })
}
function removeEntity(i) {
  if (entities.value.length <= 1) {
    ElMessage.warning('至少保留一个实体')
    return
  }
  const removed = entities.value[i]
  relations.value = relations.value.filter(r => r.from !== removed.name && r.to !== removed.name)
  entities.value.splice(i, 1)
  graphReady.value = false
}
function addAttr(ei) {
  entities.value[ei].attrs.push({ name: '', key: false })
}
function removeAttr(ei, ai) {
  if (entities.value[ei].attrs.length <= 1) {
    ElMessage.warning('至少保留一个属性')
    return
  }
  entities.value[ei].attrs.splice(ai, 1)
}
function addRelation() {
  relations.value.push({ from: entityNames.value[0] || '', to: entityNames.value[0] || '', label: '', cardinality: '', attrs: [] })
}
function removeRelation(i) {
  relations.value.splice(i, 1)
  graphReady.value = false
}
function addRelAttr(ri) {
  if (!relations.value[ri].attrs) relations.value[ri].attrs = []
  relations.value[ri].attrs.push({ name: '', key: false })
}
function removeRelAttr(ri, ai) {
  relations.value[ri].attrs.splice(ai, 1)
}

function resetExample() {
  entities.value = [
    { name: '学生', attrs: [{ name: 'id', key: true }, { name: '用户名', key: false }, { name: '密码', key: false }, { name: '出生年月', key: false }] },
    { name: '班级', attrs: [{ name: 'id', key: true }, { name: '名称', key: false }] },
    { name: '饭卡', attrs: [{ name: 'id', key: true }, { name: '余额', key: false }] },
    { name: '菜品', attrs: [{ name: 'id', key: true }, { name: '名称', key: false }, { name: '价格', key: false }] }
  ]
  relations.value = [
    { from: '学生', to: '菜品', label: '关注', cardinality: 'm:n', attrs: [] },
    { from: '学生', to: '班级', label: '属于', cardinality: 'm:1', attrs: [] },
    { from: '学生', to: '饭卡', label: '拥有', cardinality: '1:1', attrs: [] },
    { from: '饭卡', to: '菜品', label: '购买', cardinality: 'm:n', attrs: [{ name: '购买数量', key: false }, { name: '购买时间', key: false }] }
  ]
  fontSize.value = 12
  graphReady.value = false
}

/** 导出 ER 图为 Mermaid(打开预览弹窗) */
function exportMermaid() {
  const mmd = toMermaid('ER', entities.value, relations.value)
  mmdDlg.value.open(mmd, 'er-diagram.mmd')
}

async function render() {
  const valid = entityNames.value
  if (valid.length === 0) {
    ElMessage.warning('请至少填写一个实体名称')
    return
  }
  const cleanEntities = entities.value
    .map(e => ({
      name: (e.name || '').trim(),
      attrs: (e.attrs || []).map(a => ({ name: (a.name || '').trim(), key: !!a.key })).filter(a => a.name)
    }))
    .filter(e => e.name)
  const cleanRelations = relations.value
    .filter(r => r.from && r.to && r.from !== r.to)
    .map(r => ({
      from: r.from.trim(),
      to: r.to.trim(),
      label: (r.label || '').trim() || '关联',
      cardinality: (r.cardinality || '').trim(),
      attrs: (r.attrs || []).map(a => ({ name: (a.name || '').trim(), key: !!a.key })).filter(a => a.name)
    }))
  // 校验
  problems.value = validateEr(entities.value, relations.value)
  hasErrors.value = problems.value.some(p => p.type === 'error')
  if (hasErrors.value) {
    ElMessage.warning(`存在 ${problems.value.filter(p => p.type === 'error').length} 个错误，请检查后生成`)
  }
  rendering.value = true
  try {
    const vo = await getErGraph({
      fontSize: fontSize.value,
      entities: cleanEntities,
      relations: cleanRelations
    })
    // 先显示容器, 再初始化画布
    graphReady.value = true
    await nextTick()
    // 销毁旧画布, 确保不叠加残留
    if (graph) {
      graph.dispose()
      graph = null
    }
    // 直接用后端布局好的坐标渲染
    initGraph(vo.width, vo.height)
    renderGraph(vo)
  } catch (e) {
    graphReady.value = false
    ElMessage.error(e.message || 'ER 图生成失败')
  } finally {
    rendering.value = false
  }
}


function initGraph(width, height) {
  const cw = container.value ? container.value.clientWidth || width : width
  const ch = Math.max(container.value ? container.value.clientHeight || height : height, 480)
  graph = new Graph({
    container: container.value,
    width: Math.max(cw, 400),
    height: Math.max(ch, 400),
    background: { color: '#ffffff' },
    grid: false,
    interacting: { nodeMovable: true, edgeMovable: false },
    panning: true,
    mousewheel: { enabled: true, modifiers: [] },
    autoResize: true
  })
}

function renderGraph(vo) {
  const nodes = vo.nodes.map(n => {
    const base = {
      id: n.id,
      shape: n.shape === 'rect' ? 'rect' : (n.shape === 'rhombus' ? 'polygon' : 'ellipse'),
      x: n.x,
      y: n.y,
      width: n.width,
      height: n.height
    }
    if (n.shape === 'rect') {
      base.attrs = {
        body: { fill: '#ffffff', stroke: '#000000', strokeWidth: 1.5 },
        label: { text: n.label, fill: '#000000', fontSize: 12 }
      }
    } else if (n.shape === 'rhombus') {
      base.attrs = {
        body: { refPoints: '0,10 10,0 20,10 10,20', fill: '#ffffff', stroke: '#000000', strokeWidth: 1.5 },
        label: { text: n.label, fill: '#000000', fontSize: 12, textAnchor: 'middle', textVerticalAnchor: 'middle' }
      }
    } else {
      base.attrs = {
        body: { fill: '#ffffff', stroke: '#000000', strokeWidth: 1.2 },
        label: { text: n.label, fill: '#000000', fontSize: 10, textAnchor: 'middle', textVerticalAnchor: 'middle' }
      }
    }
    return base
  })
  const edges = vo.edges.map(e => {
    const edge = {
      id: e.id,
      source: e.source,
      target: e.target,
      connector: { name: 'normal' },
      attrs: { line: { stroke: '#000000', strokeWidth: 1.2, targetMarker: null } }
    }
    if (e.relationText) {
      edge.labels = [{
        attrs: {
          label: {
            text: e.relationText,
            fill: '#000000',
            fontSize: 12,
            textAnchor: 'middle',
            textVerticalAnchor: 'middle'
          }
        },
        position: { distance: e.textPosition }
      }]
    }
    return edge
  })
  graph.fromJSON({ nodes, edges })
}

function downloadPng() {
  const svgStr = getSvgString()
  if (!svgStr) return
  const svgBlob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  const img = new Image()
  img.onload = () => {
    const scale = 2
    const canvas = document.createElement('canvas')
    const box = svgStr.match(/viewBox="([^"]+)"/)
    let w = 800, h = 600
    if (box && box[1]) {
      const p = box[1].split(/\s+/).map(Number)
      if (p.length === 4) {
        w = Math.max(100, Math.round(p[2] - p[0]))
        h = Math.max(100, Math.round(p[3] - p[1]))
      }
    }
    canvas.width = w * scale
    canvas.height = h * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    URL.revokeObjectURL(url)
    canvas.toBlob(blob => {
      if (!blob) {
        ElMessage.error('PNG 导出失败')
        return
      }
      const down = document.createElement('a')
      down.href = URL.createObjectURL(blob)
      down.download = 'ER图.png'
      down.click()
      URL.revokeObjectURL(down.href)
      ElMessage.success('ER 图已下载')
    }, 'image/png')
  }
  img.onerror = () => {
    URL.revokeObjectURL(url)
    ElMessage.error('PNG 导出失败')
  }
  img.src = url
}

function downloadSvg() {
  const svgStr = getSvgString()
  if (!svgStr) return
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ER图.svg'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('SVG 已下载')
}

/** 从容器内提取 X6 渲染的 SVG 字符串 */
function getSvgString() {
  if (!graph || !container.value) return ''
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) {
    ElMessage.error('未找到画布 SVG')
    return ''
  }
  // 确保背景为白
  svgNode.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  const clone = svgNode.cloneNode(true)
  const xml = new XMLSerializer().serializeToString(clone)
  return '<?xml version="1.0" encoding="UTF-8"?>\n' + xml
}

/**
 * 保存当前拖拽后的布局坐标到后端
 */
async function saveLayout() {
  if (!graph) return
  const positions = graph.getNodes().map(node => ({
    id: node.id,
    x: node.getPosition().x,
    y: node.getPosition().y
  }))
  try {
    await saveErLayout(positions)
    ElMessage.success('布局已保存')
  } catch (e) {
    ElMessage.error(e.message || '布局保存失败')
  }
}

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
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
}
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 40px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 20px;
  align-items: start;
}
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.panel h3 {
  color: #303133;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.panel h3:not(:first-child) {
  margin-top: 22px;
}
.entity-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.entity-head {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.attr-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.add-btn {
  width: 100%;
}
.add-entity {
  width: 100%;
  margin-top: 4px;
}
.rel-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.rel-head {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.rel-diamond {
  color: #7c3aed;
  font-size: 14px;
  flex-shrink: 0;
}
.rel-label-input {
  width: 90px;
}
.rel-attrs-header {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
  padding-left: 2px;
}
.rel-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  background: #f8f9fb;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 10px;
}
.card-input {
  width: 80px;
}
.right-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.right-head h3 {
  margin: 0;
  padding: 0;
  border: none;
}
.download-group {
  display: flex;
  gap: 8px;
}
.validate-panel {
  margin-top: 16px;
  border: 1px solid #f0c36d;
  background: #fffbe8;
  border-radius: 8px;
  padding: 10px 14px;
}
.validate-panel.has-error {
  border-color: #f56c6c;
  background: #fef0f0;
}
.validate-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}
.validate-count {
  margin-left: auto;
}
.validate-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 180px;
  overflow-y: auto;
}
.validate-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #606266;
  padding: 3px 0;
  line-height: 1.5;
}
.validate-list li.error {
  color: #f56c6c;
}
.validate-list li.warning {
  color: #e6a23c;
}
.v-badge {
  flex-shrink: 0;
  font-size: 11px;
  padding: 0 6px;
  border-radius: 3px;
  line-height: 18px;
  margin-top: 1px;
}
li.error .v-badge {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}
li.warning .v-badge {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f3d19e;
}
.preview-box {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  overflow: auto;
}
.x6-container {
  width: 100%;
  height: 560px;
}
.x6-container :deep(.x6-graph) {
  overflow: hidden;
}
.preview-empty {
  margin-top: 16px;
  padding: 80px 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
@media (max-width: 900px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>
