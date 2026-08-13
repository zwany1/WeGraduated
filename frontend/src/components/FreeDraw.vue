<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>自由绘画</span>
        <span class="brand-sub">自由画板工具</span>
      </div>
    </header>

    <div class="free-draw">
      <!-- 顶部工具栏 -->
      <div class="toolbar">
        <button class="tb-btn" :class="{ active: tool === 'select' }" title="选择 (V)" @click="tool = 'select'">↖</button>
        <button class="tb-btn" :class="{ active: tool === 'edge' }" title="连线：从节点边缘锚点拖出" @click="tool = 'edge'">╱</button>
        <span class="tb-sep"></span>
        <button class="tb-btn" :disabled="!canUndo" title="撤销 (Ctrl+Z)" @click="undo">↩</button>
        <button class="tb-btn" :disabled="!canRedo" title="重做 (Ctrl+Y)" @click="redo">↪</button>
        <button class="tb-btn" title="复制 (Ctrl+C)" @click="copySel">⧉</button>
        <button class="tb-btn" title="粘贴 (Ctrl+V)" @click="paste">📋</button>
        <button class="tb-btn" title="删除 (Delete)" @click="removeCurrent">✕</button>
        <span class="tb-sep"></span>
        <button class="tb-btn" title="放大" @click="zoomIn">＋</button>
        <button class="tb-btn" title="缩小" @click="zoomOut">－</button>
        <button class="tb-btn" title="适应画布" @click="zoomToFit">⤢</button>
        <span class="tb-sep"></span>
        <button class="tb-btn" title="清空" @click="clearAll">🗑</button>
        <span class="tb-sep"></span>
        <button class="tb-btn tb-primary" @click="save">保存</button>
        <button class="tb-btn tb-outline" @click="downloadPng">导出 PNG</button>
      </div>

      <div class="editor-body">
        <!-- 左侧图形库 -->
        <aside class="palette">
          <div v-for="group in shapeGroups" :key="group.name" class="palette-group">
            <div class="palette-title">{{ group.name }}</div>
            <div class="palette-grid">
              <div class="palette-item" v-for="c in group.items" :key="c.type" draggable="true" @dragstart="onDragStart($event, c)" @click="addNodeAtCenter(c)" :title="c.label">
                <svg viewBox="0 0 60 40" width="56" height="36">
                  <g v-if="c.type === 'rect'"><rect x="4" y="6" width="52" height="28" rx="2" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'roundRect'"><rect x="4" y="6" width="52" height="28" rx="8" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'ellipse'"><ellipse cx="30" cy="20" rx="26" ry="14" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'circle'"><circle cx="30" cy="20" r="14" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'diamond'"><path d="M30 6 L52 20 L30 34 L8 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'hexagon'"><path d="M14 6 L46 6 L56 20 L46 34 L14 34 L4 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'triangle'"><path d="M30 6 L54 34 L6 34 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'text'"><text x="30" y="24" text-anchor="middle" font-size="12" fill="#333">Text</text></g>
                  <g v-else-if="c.type === 'document'"><path d="M8 6 h44 v22 c0 3 -5 6 -11 6 s-11 -3 -11 -6 s5 -6 11 -6 c4 0 8 1 11 3 V6 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'cloud'"><path d="M18 30 a12 12 0 0 1 0-24 h6 a10 10 0 0 1 19 4 a9 9 0 0 1 0 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'cylinder'"><ellipse cx="30" cy="10" rx="20" ry="6" fill="#fff" stroke="#333" stroke-width="1.5"/><path d="M10 10v18c0 3.3 9 6 20 6s20-2.7 20-6V10" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'box'"><rect x="6" y="6" width="48" height="28" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'arrow'"><line x1="6" y1="20" x2="50" y2="20" stroke="#333" stroke-width="1.5"/><path d="M46 15 l6 5 l-6 5 Z" fill="#333"/></g>
                  <g v-else-if="c.type === 'doubleArrow'"><line x1="10" y1="20" x2="50" y2="20" stroke="#333" stroke-width="1.5"/><path d="M8 17 l4 3 l-4 3 Z" fill="#333"/><path d="M52 17 l-4 3 l4 3 Z" fill="#333"/></g>
                  <g v-else-if="c.type === 'dashedArrow'"><line x1="6" y1="20" x2="50" y2="20" stroke="#333" stroke-width="1.5" stroke-dasharray="4,3"/><path d="M46 15 l6 5 l-6 5 Z" fill="#333"/></g>
                  <g v-else-if="c.type === 'curvedArrow'"><path d="M10 10 Q30 34 50 10" fill="none" stroke="#333" stroke-width="1.5"/><path d="M46 7 l6 2 l-4 5 Z" fill="#333"/></g>
                  <g v-else-if="c.type === 'blockArrow'"><path d="M6 14 h30 v-6 l18 12 l-18 12 v-6 h-30 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'start'"><circle cx="30" cy="20" r="12" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'process'"><rect x="4" y="6" width="52" height="28" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'decision'"><path d="M30 6 L52 20 L30 34 L8 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'preparation'"><path d="M20 6 L50 20 L20 34 L10 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'terminator'"><rect x="6" y="10" width="48" height="20" rx="10" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'loop'"><path d="M14 8 h32 c5 0 9 4 9 10 v4 c0 6 -4 10 -9 10 H20 c-5 0 -9 -4 -9 -10 V10 c0 -2 1 -2 3 -2 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'note'"><path d="M46 4 H14 a3 3 0 0 0 -3 3 v20 a3 3 0 0 0 3 3 h12 l8 6 v-6 h12 a3 3 0 0 0 3 -3 V7 a3 3 0 0 0 -3 -3 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'entity'"><rect x="8" y="10" width="44" height="20" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'attribute'"><ellipse cx="30" cy="20" rx="20" ry="11" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'relation'"><path d="M30 6 L52 20 L30 34 L8 20 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'class'"><rect x="4" y="4" width="52" height="12" rx="1" fill="#fff" stroke="#333" stroke-width="1.5"/><rect x="4" y="16" width="52" height="8" fill="#fff" stroke="#333" stroke-width="1.5"/><rect x="4" y="24" width="52" height="8" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'actor'"><circle cx="30" cy="12" r="6" fill="#fff" stroke="#333" stroke-width="1.5"/><line x1="30" y1="18" x2="30" y2="30" stroke="#333" stroke-width="1.5"/><line x1="22" y1="24" x2="38" y2="24" stroke="#333" stroke-width="1.5"/><line x1="30" y1="30" x2="24" y2="36" stroke="#333" stroke-width="1.5"/><line x1="30" y1="30" x2="36" y2="36" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'usecase'"><ellipse cx="30" cy="20" rx="24" ry="13" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'database'"><ellipse cx="30" cy="10" rx="22" ry="6" fill="#fff" stroke="#333" stroke-width="1.5"/><path d="M8 10v20c0 3.3 9.8 6 22 6s22-2.7 22-6V10" fill="#fff" stroke="#333" stroke-width="1.5"/><path d="M8 20c0 3.3 9.8 6 22 6s22-2.7 22-6" fill="none" stroke="#333" stroke-width="1.5"/></g>
                  <g v-else-if="c.type === 'folder'"><path d="M8 8 h18 l4 6 h22 a3 3 0 0 1 3 3 v13 a3 3 0 0 1 -3 3 H8 a3 3 0 0 1 -3 -3 V11 a3 3 0 0 1 3 -3 Z" fill="#fff" stroke="#333" stroke-width="1.5"/></g>
                </svg>
              </div>
            </div>
          </div>

          <div class="palette-tip">拖拽到画布或点击添加</div>
        </aside>

        <!-- 中间画布 -->
        <div class="canvas-area">
          <div ref="containerRef" class="x6-container"></div>
          <div v-if="!graph" class="canvas-loading">画布初始化中...</div>
          <div class="canvas-zoom-tip">Ctrl + 滚轮缩放 · 左键拖动平移</div>
        </div>

        <!-- 右侧格式面板 -->
        <aside class="props">
          <div class="props-title">格式</div>
          <template v-if="current">
            <div class="prop-field">
              <label>文本</label>
              <el-input v-model="currentLabel" size="small" @change="applyLabel" />
            </div>
            <div class="prop-field">
              <label>填充色</label>
              <el-color-picker v-if="!isEdgeSel" v-model="currentFill" size="small" @change="applyFill" />
              <el-tag v-else size="small" type="info">连线无填充</el-tag>
            </div>
            <div class="prop-field">
              <label>{{ isEdgeSel ? '线条色' : '边框色' }}</label>
              <el-color-picker v-model="currentStroke" size="small" @change="applyStroke" />
            </div>
            <div class="prop-field">
              <label>{{ isEdgeSel ? '线条粗细' : '边框粗细' }}</label>
              <el-input-number v-model="currentStrokeWidth" :min="1" :max="10" size="small" @change="applyStrokeWidth" />
            </div>
            <div class="prop-field" v-if="!isEdgeSel">
              <label>文字颜色</label>
              <el-color-picker v-model="currentFontColor" size="small" @change="applyFontColor" />
            </div>
            <div class="prop-field" v-if="!isEdgeSel">
              <label>字号</label>
              <el-input-number v-model="currentFontSize" :min="10" :max="48" size="small" @change="applyFont" />
            </div>
            <div class="prop-field" v-if="!isEdgeSel">
              <label>层叠</label>
              <div class="stack-row">
                <el-button size="small" @click="moveToFront">置顶</el-button>
                <el-button size="small" @click="moveToBack">置底</el-button>
              </div>
            </div>
            <div class="prop-field">
              <el-button size="small" type="danger" plain @click="removeCurrent">删除</el-button>
            </div>
          </template>
          <div v-else class="props-empty">选中节点后<br />可编辑格式</div>

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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const currentFill = ref('#ffffff')
const currentStroke = ref('#333333')
const currentStrokeWidth = ref(1.5)
const currentFontColor = ref('#333333')
const currentFontSize = ref(14)
const myDesigns = ref([])
const isEdgeSel = computed(() => current.value && current.value.isEdge && current.value.isEdge())
const canUndo = ref(false)
const canRedo = ref(false)

let clipboard = null

const shapeGroups = [
  {
    name: '基本形状',
    items: [
      { type: 'rect', label: '矩形' },
      { type: 'roundRect', label: '圆角矩形' },
      { type: 'ellipse', label: '椭圆' },
      { type: 'circle', label: '圆' },
      { type: 'diamond', label: '菱形' },
      { type: 'hexagon', label: '六边形' },
      { type: 'triangle', label: '三角形' },
      { type: 'text', label: '文本' },
      { type: 'document', label: '文档' },
      { type: 'cloud', label: '云' },
      { type: 'cylinder', label: '圆柱' },
      { type: 'box', label: '方框' }
    ]
  },
  {
    name: '流程图',
    items: [
      { type: 'start', label: '开始' },
      { type: 'process', label: '处理' },
      { type: 'decision', label: '判断' },
      { type: 'preparation', label: '准备' },
      { type: 'terminator', label: '终止' },
      { type: 'loop', label: '循环' },
      { type: 'note', label: '注释' }
    ]
  },
  {
    name: '实体关系',
    items: [
      { type: 'entity', label: '实体' },
      { type: 'attribute', label: '属性' },
      { type: 'relation', label: '关系' }
    ]
  },
  {
    name: 'UML',
    items: [
      { type: 'class', label: '类' },
      { type: 'actor', label: '角色' },
      { type: 'usecase', label: '用例' },
      { type: 'database', label: '数据库' }
    ]
  },
  {
    name: '高级',
    items: [
      { type: 'folder', label: '文件夹' }
    ]
  }
]

const base = '#333333'

function registerShapes() {
  // 基础几何形状
  const simple = {
    freerect: { inherit: 'rect', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, rx: 2, magnet: true }, label: { fontSize: 13, fill: base } } },
    freeroundrect: { inherit: 'rect', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, rx: 8, magnet: true }, label: { fontSize: 13, fill: base } } },
    freeellipse: { inherit: 'ellipse', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, magnet: true }, label: { fontSize: 13, fill: base } } },
    freecircle: { inherit: 'circle', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, magnet: true }, label: { fontSize: 13, fill: base } } },
    freediamond: { inherit: 'polygon', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, points: '50,0 100,50 50,100 0,50', magnet: true }, label: { fontSize: 12, fill: base } } },
    freehexagon: { inherit: 'polygon', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, points: '25,0 75,0 100,50 75,100 25,100 0,50', magnet: true }, label: { fontSize: 12, fill: base } } },
    freetriangle: { inherit: 'polygon', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, points: '50,0 100,100 0,100', magnet: true }, label: { fontSize: 12, fill: base } } },
    freepreparation: { inherit: 'polygon', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, points: '30,0 100,50 30,100 0,50', magnet: true }, label: { fontSize: 12, fill: base } } },
    freeterminator: { inherit: 'rect', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, rx: 25, magnet: true }, label: { fontSize: 13, fill: base } } },
    freeloop: { inherit: 'rect', attrs: { body: { fill: '#fff', stroke: base, strokeWidth: 1.5, rx: 8, magnet: true }, label: { fontSize: 12, fill: base } } }
  }
  Object.entries(simple).forEach(([name, cfg]) => Graph.registerNode(name, { ...cfg, width: 120, height: 60 }, true))

  // 文档
  Graph.registerNode('freedocument', {
    inherit: 'rect',
    width: 110,
    height: 80,
    markup: [
      { tagName: 'path', selector: 'body', attrs: { d: 'M8 6 h44 v22 c0 3 -5 6 -11 6 s-11 -3 -11 -6 s5 -6 11 -6 c4 0 8 1 11 3 V6 Z' } },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: { fill: '#fff', stroke: base, strokeWidth: 1.5, refWidth: '100%', refHeight: '100%', magnet: true },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.5, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 云
  Graph.registerNode('freecloud', {
    inherit: 'rect',
    width: 130,
    height: 80,
    markup: [
      { tagName: 'path', selector: 'body', attrs: { d: 'M20 70 a20 20 0 0 1 0 -40 h10 a18 18 0 0 1 34 6 a15 15 0 0 1 0 34 Z' } },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: { fill: '#fff', stroke: base, strokeWidth: 1.5, refWidth: '100%', refHeight: '100%', magnet: true },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 圆柱
  Graph.registerNode('freecylinder', {
    inherit: 'rect',
    width: 110,
    height: 80,
    markup: [
      { tagName: 'ellipse', selector: 'top' },
      { tagName: 'path', selector: 'side' },
      { tagName: 'path', selector: 'curve' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      top: { cx: 0, cy: 0, rx: 18, ry: 6, fill: '#fff', stroke: base, strokeWidth: 1.5, refX: 0.5, refY: 0 },
      side: { d: 'M 14 6 L 14 74 C 14 78 21 81 30 81 C 39 81 46 78 46 74 L 46 6', fill: '#fff', stroke: base, strokeWidth: 1.5 },
      curve: { d: 'M 14 40 C 14 44 21 47 30 47 C 39 47 46 44 46 40', fill: 'none', stroke: base, strokeWidth: 1.5 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 文件夹
  Graph.registerNode('freefolder', {
    inherit: 'rect',
    width: 130,
    height: 80,
    markup: [
      { tagName: 'path', selector: 'body', attrs: { d: 'M8 8 h18 l4 6 h22 a3 3 0 0 1 3 3 v13 a3 3 0 0 1 -3 3 H8 a3 3 0 0 1 -3 -3 V11 a3 3 0 0 1 3 -3 Z' } },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: { fill: '#fff', stroke: base, strokeWidth: 1.5, refWidth: '100%', refHeight: '100%', magnet: true },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 箭头类 (真实图形节点, 非边)
  Graph.registerNode('freearrow', {
    inherit: 'rect',
    width: 150,
    height: 40,
    markup: [
      { tagName: 'line', selector: 'line' },
      { tagName: 'path', selector: 'head' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      line: { x1: 5, y1: 20, x2: 130, y2: 20, stroke: base, strokeWidth: 1.5, refX: 0, refY: 0.5 },
      head: { d: 'M 128 14 L 140 20 L 128 26 Z', fill: base, stroke: base, strokeWidth: 1 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.4, refY: 0.6, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freedashedarrow', {
    inherit: 'rect',
    width: 150,
    height: 40,
    markup: [
      { tagName: 'line', selector: 'line' },
      { tagName: 'path', selector: 'head' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      line: { x1: 5, y1: 20, x2: 130, y2: 20, stroke: base, strokeWidth: 1.5, strokeDasharray: '5,4', refX: 0, refY: 0.5 },
      head: { d: 'M 128 14 L 140 20 L 128 26 Z', fill: base, stroke: base, strokeWidth: 1 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.4, refY: 0.6, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freedoublearrow', {
    inherit: 'rect',
    width: 150,
    height: 40,
    markup: [
      { tagName: 'line', selector: 'line' },
      { tagName: 'path', selector: 'lhead' },
      { tagName: 'path', selector: 'rhead' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      line: { x1: 12, y1: 20, x2: 138, y2: 20, stroke: base, strokeWidth: 1.5, refX: 0, refY: 0.5 },
      lhead: { d: 'M 12 14 L 0 20 L 12 26 Z', fill: base, stroke: base, strokeWidth: 1 },
      rhead: { d: 'M 138 14 L 150 20 L 138 26 Z', fill: base, stroke: base, strokeWidth: 1 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.6, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freecurvedarrow', {
    inherit: 'rect',
    width: 150,
    height: 50,
    markup: [
      { tagName: 'path', selector: 'path' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      path: { d: 'M 10 8 Q 75 42 140 8', fill: 'none', stroke: base, strokeWidth: 1.5 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.5, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)
  Graph.registerNode('freeblockarrow', {
    inherit: 'rect',
    width: 120,
    height: 60,
    markup: [
      { tagName: 'path', selector: 'body', attrs: { d: 'M 8 15 L 78 15 L 78 5 L 112 30 L 78 55 L 78 45 L 8 45 Z' } },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: { fill: '#fff', stroke: base, strokeWidth: 1.5, refWidth: '100%', refHeight: '100%', magnet: true },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 文本
  Graph.registerNode('freetext', {
    inherit: 'rect',
    width: 120,
    height: 36,
    attrs: {
      body: { fill: 'none', stroke: 'none' },
      label: { text: '', fontSize: 14, fill: base, textVerticalAnchor: 'middle' }
    }
  }, true)

  // 数据库
  Graph.registerNode('freedatabase', {
    inherit: 'rect',
    width: 130,
    height: 70,
    markup: [
      { tagName: 'ellipse', selector: 'top' },
      { tagName: 'rect', selector: 'body' },
      { tagName: 'path', selector: 'mid' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      top: { cx: 0, cy: 0, rx: 18, ry: 9, fill: '#fff', stroke: base, strokeWidth: 1.5, refX: 0.5, refY: 0 },
      body: { refWidth: '100%', refHeight: '100%', fill: '#fff', stroke: base, strokeWidth: 1.5, magnet: true },
      mid: { d: 'M 10 35 L 120 35', fill: 'none', stroke: base, strokeWidth: 1.5, opacity: 0 },
      label: { text: '', fontSize: 12, fill: base, refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // Actor
  Graph.registerNode('freeactor', {
    inherit: 'rect',
    width: 100,
    height: 100,
    markup: [
      { tagName: 'image', selector: 'img' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      img: { 'xlink:href': '/xiaoren.svg', width: 40, height: 40, x: 0, y: 0, refX: 0.5, refY: 0.3, xAlign: 'middle', yAlign: 'middle' },
      label: { text: '', fontSize: 13, fill: base, refX: 0.5, refY: 0.75, textAnchor: 'middle', textVerticalAnchor: 'middle' }
    }
  }, true)

  // 类
  Graph.registerNode('freeclass', {
    inherit: 'html',
    width: 180,
    height: 100,
    html(cell) {
      const data = cell.getData() || {}
      const esc = s => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      return `<div style="width:100%;height:100%;display:flex;flex-direction:column;box-sizing:border-box;background:#fff;border:1.5px solid #333;overflow:hidden">
        <div style="text-align:center;font-weight:bold;font-size:14px;padding:6px 8px;border-bottom:1.5px solid #333;background:#fafafa;color:#333">${esc(data.name || 'Class')}</div>
        <div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap;flex:1">${esc(data.attrs || '- 属性')}</div>
        <div style="border-top:1px solid #999"></div>
        <div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${esc(data.methods || '+ 方法')}</div>
      </div>`
    }
  }, true)

  // 边
  Graph.registerEdge('freeline', {
    inherit: 'edge',
    attrs: { line: { stroke: base, strokeWidth: 1.5, targetMarker: { name: 'block', size: 8 } } }
  }, true)
}

function nodeSpec(type) {
  const map = {
    rect: { shape: 'freerect', w: 120, h: 60 },
    roundRect: { shape: 'freeroundrect', w: 120, h: 60 },
    ellipse: { shape: 'freeellipse', w: 140, h: 70 },
    circle: { shape: 'freecircle', w: 80, h: 80 },
    diamond: { shape: 'freediamond', w: 100, h: 100 },
    hexagon: { shape: 'freehexagon', w: 110, h: 90 },
    triangle: { shape: 'freetriangle', w: 100, h: 80 },
    text: { shape: 'freetext', w: 120, h: 36 },
    document: { shape: 'freedocument', w: 110, h: 80 },
    cloud: { shape: 'freecloud', w: 130, h: 80 },
    cylinder: { shape: 'freecylinder', w: 110, h: 80 },
    box: { shape: 'freerect', w: 120, h: 60 },
    arrow: { shape: 'freearrow', w: 150, h: 40 },
    doubleArrow: { shape: 'freedoublearrow', w: 150, h: 40 },
    dashedArrow: { shape: 'freedashedarrow', w: 150, h: 40 },
    curvedArrow: { shape: 'freecurvedarrow', w: 150, h: 50 },
    blockArrow: { shape: 'freeblockarrow', w: 120, h: 60 },
    start: { shape: 'freecircle', w: 60, h: 60 },
    process: { shape: 'freerect', w: 120, h: 60 },
    decision: { shape: 'freediamond', w: 100, h: 100 },
    preparation: { shape: 'freepreparation', w: 120, h: 80 },
    terminator: { shape: 'freeterminator', w: 130, h: 60 },
    loop: { shape: 'freeloop', w: 130, h: 70 },
    note: { shape: 'freenote', w: 160, h: 90 },
    entity: { shape: 'freerect', w: 120, h: 60 },
    attribute: { shape: 'freeellipse', w: 130, h: 60 },
    relation: { shape: 'freediamond', w: 100, h: 100 },
    usecase: { shape: 'freeellipse', w: 150, h: 70 },
    database: { shape: 'freedatabase', w: 130, h: 70 },
    actor: { shape: 'freeactor', w: 100, h: 100 },
    class: { shape: 'freeclass', w: 180, h: 100 },
    folder: { shape: 'freefolder', w: 130, h: 80 }
  }
  return map[type] || { shape: 'freerect', w: 120, h: 60 }
}

const portDef = {
  groups: {
    top: { position: 'top' },
    bottom: { position: 'bottom' },
    left: { position: 'left' },
    right: { position: 'right' }
  },
  items: [
    { id: 't', group: 'top' },
    { id: 'b', group: 'bottom' },
    { id: 'l', group: 'left' },
    { id: 'r', group: 'right' }
  ]
}

function portAttrs() {
  return JSON.parse(JSON.stringify(portDef))
}

function buildNode(type, x, y) {
  const spec = nodeSpec(type)
  const id = 'n' + Date.now() + Math.random().toString(36).slice(2, 6)
  const nodeData = {
    id,
    shape: spec.shape,
    x: x - spec.w / 2,
    y: y - spec.h / 2,
    width: spec.w,
    height: spec.h,
    label: '',
    ports: portAttrs(),
    data: { type }
  }
  if (type === 'class') nodeData.data = { name: 'Class', attrs: '- 属性', methods: '+ 方法' }
  if (type === 'text') nodeData.label = '文本'
  return nodeData
}

function addNodeAtCenter(c) {
  if (!graph.value) return
  const pos = graph.value.getGraphAreaCenter ? graph.value.getGraphAreaCenter() : { x: 300, y: 200 }
  const count = graph.value.getNodes().length
  graph.value.addNode(buildNode(c.type, pos.x + (count % 5) * 30, pos.y + (count % 5) * 30))
}

function onDragStart(e, c) {
  e.dataTransfer.setData('application/x6-node', JSON.stringify({ type: c.type }))
}

function initGraph() {
  registerShapes()
  const g = new Graph({
    container: containerRef.value,
    width: containerRef.value.clientWidth || 900,
    height: containerRef.value.clientHeight || 600,
    background: { color: '#ffffff' },
    grid: { size: 10, visible: true, type: 'dot', args: { color: '#dcdcdc' } },
    panning: { enabled: true, eventTypes: ['leftMouseDown'] },
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    autoResize: true,
    selecting: { enabled: true, rubberband: true, multiple: true },
    snapline: true,
    portMarkup: [
      { tagName: 'circle', selector: 'portBody' }
    ],
    defaultPort: {
      attrs: {
        portBody: { r: 5, magnet: true, stroke: '#1a73e8', strokeWidth: 1, fill: '#fff', style: { visibility: 'hidden' } }
      }
    },
    connecting: {
      snap: true,
      allowBlank: false,
      allowLoop: true,
      allowNode: true,
      allowEdge: false,
      allowPort: true,
      allowMulti: true,
      highlight: true,
      router: 'orth',
      connector: 'rounded',
      createEdge() {
        return this.createEdge({ shape: 'freeline' })
      }
    },
    history: { enabled: true }
  })
  graph.value = g

  g.on('history:change', () => {
    canUndo.value = g.canUndo()
    canRedo.value = g.canRedo()
  })

  // 拖拽放置
  containerRef.value.addEventListener('dragover', e => e.preventDefault())
  containerRef.value.addEventListener('drop', e => {
    e.preventDefault()
    const raw = e.dataTransfer.getData('application/x6-node')
    if (!raw) return
    const { type } = JSON.parse(raw)
    const pos = g.clientToLocal(e.clientX, e.clientY)
    g.addNode(buildNode(type, pos.x, pos.y))
  })

  // 选择
  g.on('blank:click', ({ e }) => {
    if (tool.value === 'select') current.value = null
    if (tool.value === 'text') {
      const pos = g.clientToLocal(e.clientX, e.clientY)
      g.addNode(buildNode('text', pos.x, pos.y))
    }
  })
  g.on('node:click', ({ node }) => {
    if (tool.value === 'select') selectNode(node)
  })
  // 连接完成: 选中边以展示属性面板
  g.on('edge:selected', ({ edge }) => {
    current.value = edge
    currentStroke.value = edge.attr('line/stroke') || '#333333'
    currentStrokeWidth.value = edge.attr('line/strokeWidth') || 1.5
    currentLabel.value = edge.attr('label/text') || ''
    currentFill.value = ''
  })
  g.on('edge:unselected', () => {
    if (current.value && current.value.isEdge && current.value.isEdge()) current.value = null
  })

  // 双击编辑文本
  g.on('node:dblclick', ({ node }) => {
    selectNode(node)
    const label = node.attr('label/text')
    ElMessageBox.prompt('编辑文本', '文本', { inputValue: label || '', confirmButtonText: '确定', cancelButtonText: '取消' })
      .then(({ value }) => {
        if (node.shape === 'freeclass') {
          const d = node.getData() || {}
          d.name = value
          node.setData(d)
        } else {
          node.attr('label/text', value || '')
        }
      })
      .catch(() => {})
  })

  // 键盘快捷键
  const keyHandler = e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') { e.preventDefault(); undo() }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') { e.preventDefault(); redo() }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'c') { e.preventDefault(); copySel() }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'v') { e.preventDefault(); paste() }
    if (e.key === 'Delete' || e.key === 'Backspace') { removeCurrent() }
    if (e.key === 'v' || e.key === 'V') { tool.value = 'select' }
  }
  document.addEventListener('keydown', keyHandler)
  g.on('dispose', () => document.removeEventListener('keydown', keyHandler))
}

function selectNode(node) {
  current.value = node
  currentLabel.value = node.attr('label/text') || node.getData()?.name || ''
  currentFill.value = node.attr('body/fill') || '#ffffff'
  currentStroke.value = node.attr('body/stroke') || '#333333'
  currentStrokeWidth.value = node.attr('body/strokeWidth') || 1.5
  currentFontColor.value = node.attr('label/fill') || '#333333'
  currentFontSize.value = node.attr('label/fontSize') || 14
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
function applyFill() {
  if (!current.value) return
  if (current.value.isEdge && current.value.isEdge()) return
  current.value.attr('body/fill', currentFill.value)
}
function applyStroke() {
  if (!current.value) return
  const sel = current.value.isEdge && current.value.isEdge() ? 'line' : 'body'
  current.value.attr(sel + '/stroke', currentStroke.value)
}
function applyStrokeWidth() {
  if (!current.value) return
  const sel = current.value.isEdge && current.value.isEdge() ? 'line' : 'body'
  current.value.attr(sel + '/strokeWidth', currentStrokeWidth.value)
}
function applyFontColor() {
  if (!current.value) return
  if (current.value.isEdge && current.value.isEdge()) return
  current.value.attr('label/fill', currentFontColor.value)
}
function applyFont() {
  if (!current.value) return
  if (current.value.isEdge && current.value.isEdge()) return
  current.value.attr('label/fontSize', currentFontSize.value)
}
function moveToFront() {
  if (current.value) current.value.toFront()
}
function moveToBack() {
  if (current.value) current.value.toBack()
}
function removeCurrent() {
  if (current.value) {
    graph.value.removeCell(current.value)
    current.value = null
  }
}
function copySel() {
  const cells = graph.value.getSelectedCells()
  if (cells.length) {
    clipboard = graph.value.toJSON(cells)
    ElMessage.success('已复制')
  }
}
function paste() {
  if (!clipboard) return
  const nodes = graph.value.fromJSON(clipboard)
  nodes.forEach(c => {
    const pos = c.position()
    c.position(pos.x + 20, pos.y + 20)
  })
}
function clearAll() {
  graph.value.clearCells()
  current.value = null
}
function undo() {
  if (graph.value && graph.value.canUndo()) graph.value.undo()
}
function redo() {
  if (graph.value && graph.value.canRedo()) graph.value.redo()
}
function zoomIn() {
  const z = graph.value.zoom()
  graph.value.zoom(z * 1.2, { center: true })
}
function zoomOut() {
  const z = graph.value.zoom()
  graph.value.zoom(z * 0.8, { center: true })
}
function zoomToFit() {
  graph.value.zoomToFit({ padding: 40 })
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
      const nodeData = buildNode(n.shape, n.x + (n.width || 120) / 2, n.y + (n.height || 60) / 2)
      nodeData.id = n.id
      nodeData.width = n.width || nodeData.width
      nodeData.height = n.height || nodeData.height
      nodeData.label = n.label || ''
      nodeData.data = { type: n.shape, name: n.label, attrs: n.attrsText, methods: n.methodsText }
      graph.value.addNode(nodeData)
    })
    ;(vo.edges || []).forEach(e => {
      graph.value.addEdge({ source: e.source, target: e.target, shape: 'freeline' })
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
    const dataUrl = await graph.value.toPNG({ backgroundColor: '#fff', padding: 20 })
    const a = document.createElement('a')
    a.href = dataUrl
    a.download = '自由绘画.png'
    a.click()
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  requestAnimationFrame(() => requestAnimationFrame(initGraph))
  loadMyDesigns()
})

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
  background: #fff;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: #f7f8fa;
  border-bottom: 1px solid #e4e7ed;
}
.tb-btn {
  min-width: 32px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.15s;
}
.tb-btn:hover {
  background: #e8f0fe;
  color: #1a73e8;
}
.tb-btn.active {
  background: #e8f0fe;
  color: #1a73e8;
  border-color: #1a73e8;
}
.tb-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.tb-btn.tb-primary {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
}
.tb-btn.tb-primary:hover {
  background: #1765cc;
}
.tb-btn.tb-outline {
  border-color: #1a73e8;
  color: #1a73e8;
}
.tb-sep {
  width: 1px;
  height: 20px;
  background: #e4e7ed;
  margin: 0 6px;
}
.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.palette {
  width: 200px;
  background: #f7f8fa;
  border-right: 1px solid #e4e7ed;
  padding: 12px;
  overflow-y: auto;
}
.palette-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.palette-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}
.palette-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  background: #fff;
  transition: all 0.15s;
}
.palette-item:hover {
  border-color: #1a73e8;
  box-shadow: 0 2px 6px rgba(26, 115, 232, 0.2);
}
.palette-tip {
  margin-top: 10px;
  font-size: 11px;
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
/* 连接锚点: 节点悬停显示, 离开隐藏 (draw.io 交互) */
:deep(.x6-port-body) {
  visibility: hidden;
}
:deep(.x6-node:hover .x6-port-body) {
  visibility: visible;
}
.canvas-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 14px;
}
.canvas-zoom-tip {
  position: absolute;
  bottom: 10px;
  right: 12px;
  font-size: 11px;
  color: #c0c4cc;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 8px;
  border-radius: 4px;
  pointer-events: none;
}
.props {
  width: 240px;
  background: #f7f8fa;
  border-left: 1px solid #e4e7ed;
  padding: 12px;
  overflow-y: auto;
}
.props-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
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
.stack-row {
  display: flex;
  gap: 4px;
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
.save-name:hover {
  color: #1a73e8;
}
</style>
