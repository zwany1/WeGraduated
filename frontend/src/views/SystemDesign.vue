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
          <el-radio-button value="USECASE">用例图</el-radio-button>
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

        <!-- 流程图: 文本描述 -->
        <template v-else-if="type === 'FLOW'">
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

        <!-- 泳道图: 结构化配置(泳道 + 节点 + 连线) -->
        <template v-else-if="type === 'SWIMLANE'">
          <div class="input-title">泳道图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="业务名称">
              <el-input v-model="swimConfig.flowName" placeholder="如：订单流程" />
            </el-form-item>
          </el-form>
          <div class="layer-config">
            <div v-for="(lane, li) in swimConfig.lanes" :key="li" class="layer-config-card">
              <div class="layer-config-head">
                <el-input v-model="lane.name" size="small" placeholder="参与者，如：用户 / SpringBoot / MySQL" class="layer-name-input" />
                <el-button size="small" text type="danger" @click="removeLane(li)">删除泳道</el-button>
              </div>
              <div class="lane-act" v-for="(node, ni) in lane.nodes" :key="ni">
                <el-select v-model="node.type" size="small" class="act-type" placeholder="类型">
                  <el-option label="开始" value="start" />
                  <el-option label="任务" value="task" />
                  <el-option label="判断" value="gateway" />
                  <el-option label="结束" value="end" />
                </el-select>
                <el-input v-model="node.name" size="small" placeholder="节点，如：提交订单" />
                <el-button size="small" text type="danger" @click="removeNode(li, ni)">×</el-button>
              </div>
              <el-button size="small" class="add-comp-btn" @click="addNode(li)">+ 添加节点</el-button>
            </div>
            <el-button size="small" class="add-layer-btn" @click="addLane">+ 添加泳道</el-button>
          </div>
          <div class="edge-config">
            <div class="edge-title">流程连线</div>
            <div v-for="(eg, ei) in swimConfig.edges" :key="ei" class="edge-row">
              <el-select v-model="eg.source" size="small" class="edge-sel" placeholder="从节点">
                <el-option v-for="nd in allNodes" :key="nd.key" :label="nd.label" :value="nd.id" />
              </el-select>
              <span class="edge-arrow">→</span>
              <el-select v-model="eg.target" size="small" class="edge-sel" placeholder="到节点">
                <el-option v-for="nd in allNodes" :key="nd.key" :label="nd.label" :value="nd.id" />
              </el-select>
              <el-button size="small" text type="danger" @click="removeEdge(ei)">×</el-button>
            </div>
            <el-button size="small" class="add-comp-btn" @click="addEdge">+ 添加连线</el-button>
            <div class="tip" style="margin-top:6px">泳道横向排列，流程按连线顺序纵向流动</div>
          </div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成泳道图</el-button>
            <span class="tip">未配连线时按节点顺序自动连</span>
          </div>
        </template>

        <!-- 用例图: 结构化配置(参与者 + 用例 + 关系) -->
        <template v-else-if="type === 'USECASE'">
          <div class="input-title">用例图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="系统名称">
              <el-input v-model="ucConfig.system" placeholder="如：人事管理系统" />
            </el-form-item>
          </el-form>

          <div class="input-title" style="margin-top:12px">参与者 Actor</div>
          <div class="uc-act" v-for="(a, ai) in ucConfig.actors" :key="ai">
            <el-input v-model="a.name" size="small" placeholder="参与者，如：人事管理员" />
            <el-button size="small" text type="danger" @click="removeUcActor(ai)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcActor">+ 添加参与者</el-button>

          <div class="input-title" style="margin-top:12px">用例 UseCase</div>
          <div class="uc-act" v-for="(u, ui) in ucConfig.usecases" :key="ui">
            <el-input v-model="u.name" size="small" placeholder="用例，如：人员规划" />
            <el-input v-model="u.module" size="small" placeholder="模块(可选)" style="margin-left:6px" />
            <el-button size="small" text type="danger" @click="removeUcUseCase(ui)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcUseCase">+ 添加用例</el-button>

          <div class="input-title" style="margin-top:12px">关系</div>
          <div class="uc-act" v-for="(r, ri) in ucConfig.relations" :key="ri">
            <el-select v-model="r.type" size="small" class="act-type" placeholder="类型">
              <el-option label="关联" value="association" />
              <el-option label="包含" value="include" />
              <el-option label="扩展" value="extend" />
            </el-select>
            <el-select v-model="r.source" size="small" class="edge-sel" placeholder="来源">
              <el-option v-for="nd in ucAllNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="r.target" size="small" class="edge-sel" placeholder="目标">
              <el-option v-for="nd in ucAllNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeUcRelation(ri)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcRelation">+ 添加关系</el-button>
          <div class="tip" style="margin-top:6px">关联=实线；包含/扩展=虚线箭头«include»</div>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成用例图</el-button>
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
        <!-- FLOW/SWIMLANE/USECASE: X6 画布 -->
        <div v-show="graphReady && currentVO && currentVO.type !== 'ARCH'" ref="container" class="x6-container"></div>
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

// 注册 Actor 小人节点(用例图): 使用 xiaoren.svg 图片渲染
Graph.registerNode('actorNode', {
  inherit: 'rect',
  width: 120,
  height: 120,
  markup: [
    { tagName: 'image', selector: 'img' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    img: {
      'xlink:href': '/xiaoren.svg',
      width: 42,
      height: 42,
      x: 0,
      y: 0,
      refX: 0.5,
      refY: 0.25,
      xAlign: 'middle',
      yAlign: 'middle'
    },
    label: { text: '', fontSize: 12, fill: '#333', refX: 0.5, refY: 0.75, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  }
})

// 注册系统边界框节点(用例图)
Graph.registerNode('systemNode', {
  inherit: 'rect',
  markup: [
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'title' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    body: { refWidth: '100%', refHeight: '100%', fill: 'rgba(255,255,255,0.4)', stroke: '#333', strokeWidth: 2, rx: 4, ry: 4 },
    title: { text: '', fontSize: 13, fontWeight: 700, fill: '#333', refX: 0.5, refY: 0, refY2: 18, textAnchor: 'middle', textVerticalAnchor: 'middle' },
    label: { text: '' }
  }
})

let graph = null
const container = ref(null)
const archRef = ref(null)
const type = ref('SWIMLANE')
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

// 泳道图配置(BPMN 模型): 泳道(Lane) + 节点(Node) + 连线(Edge)
const swimConfig = ref({
  flowName: '订单流程',
  lanes: [
    { name: '用户', nodes: [
      { id: 'N1', name: '开始', type: 'start' },
      { id: 'N2', name: '提交订单', type: 'task' }
    ]},
    { name: 'Vue客户端', nodes: [
      { id: 'N3', name: '发送请求', type: 'task' },
      { id: 'N4', name: '展示结果', type: 'task' }
    ]},
    { name: 'SpringBoot服务', nodes: [
      { id: 'N5', name: '校验库存', type: 'gateway' },
      { id: 'N6', name: '创建订单', type: 'task' }
    ]},
    { name: 'MySQL数据库', nodes: [
      { id: 'N7', name: '保存订单', type: 'task' },
      { id: 'N8', name: '结束', type: 'end' }
    ]}
  ],
  edges: [
    { source: 'N1', target: 'N2' },
    { source: 'N2', target: 'N3' },
    { source: 'N3', target: 'N5' },
    { source: 'N5', target: 'N6', label: '通过' },
    { source: 'N6', target: 'N7' },
    { source: 'N7', target: 'N4' },
    { source: 'N4', target: 'N8' }
  ]
})
let nodeSeq = 9

// 用例图配置: 参与者 + 用例 + 关系
const ucConfig = ref({
  system: '人事管理系统',
  actors: [
    { id: 'A1', name: '人事管理员' },
    { id: 'A2', name: '普通员工' }
  ],
  usecases: [
    { id: 'U1', name: '人员规划', module: '人员管理' },
    { id: 'U2', name: '人员现状分析', module: '人员管理' },
    { id: 'U3', name: '人员年龄分析', module: '人员管理' },
    { id: 'U4', name: '登录系统', module: '' }
  ],
  relations: [
    { source: 'A1', target: 'U1', type: 'association' },
    { source: 'U1', target: 'U2', type: 'include' },
    { source: 'U1', target: 'U3', type: 'include' },
    { source: 'A2', target: 'U4', type: 'association' }
  ]
})
let ucSeq = 5

// 用例图: 参与者/用例/关系管理
function addUcActor() {
  ucConfig.value.actors.push({ id: 'A' + (ucSeq++), name: '' })
}
function removeUcActor(ai) {
  const id = ucConfig.value.actors[ai].id
  ucConfig.value.actors.splice(ai, 1)
  ucConfig.value.relations = ucConfig.value.relations.filter(r => r.source !== id && r.target !== id)
}
function addUcUseCase() {
  ucConfig.value.usecases.push({ id: 'U' + (ucSeq++), name: '', module: '' })
}
function removeUcUseCase(ui) {
  const id = ucConfig.value.usecases[ui].id
  ucConfig.value.usecases.splice(ui, 1)
  ucConfig.value.relations = ucConfig.value.relations.filter(r => r.source !== id && r.target !== id)
}
function addUcRelation() {
  ucConfig.value.relations.push({ source: '', target: '', type: 'association' })
}
function removeUcRelation(ri) {
  ucConfig.value.relations.splice(ri, 1)
}
// 全部参与者+用例(供关系下拉)
const ucAllNodes = computed(() => {
  const list = []
  ucConfig.value.actors.forEach(a => list.push({ id: a.id, label: a.name || ('参与者 ' + a.id) }))
  ucConfig.value.usecases.forEach(u => list.push({ id: u.id, label: u.name || ('用例 ' + u.id) }))
  return list
})
function buildUseCasePayload() {
  const actors = ucConfig.value.actors.filter(a => a.name && a.name.trim()).map(a => ({ id: a.id, name: a.name.trim() }))
  const usecases = ucConfig.value.usecases.filter(u => u.name && u.name.trim()).map(u => ({ id: u.id, name: u.name.trim(), module: u.module || '' }))
  const relations = (ucConfig.value.relations || [])
    .filter(r => r.source && r.target && r.source !== r.target)
    .map(r => ({ source: r.source, target: r.target, type: r.type || 'association' }))
  return { system: ucConfig.value.system, actors, usecases, relations }
}

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

// 泳道图: 泳道与节点管理
function addLane() {
  swimConfig.value.lanes.push({ name: '', nodes: [{ id: 'N' + (nodeSeq++), name: '', type: 'task' }] })
}
function removeLane(li) {
  if (swimConfig.value.lanes.length <= 1) {
    ElMessage.warning('至少保留一个泳道')
    return
  }
  swimConfig.value.lanes.splice(li, 1)
}
function addNode(li) {
  swimConfig.value.lanes[li].nodes.push({ id: 'N' + (nodeSeq++), name: '', type: 'task' })
}
function removeNode(li, ni) {
  if (swimConfig.value.lanes[li].nodes.length <= 1) {
    ElMessage.warning('至少保留一个节点')
    return
  }
  const id = swimConfig.value.lanes[li].nodes[ni].id
  swimConfig.value.lanes[li].nodes.splice(ni, 1)
  // 清理引用该节点的连线
  swimConfig.value.edges = swimConfig.value.edges.filter(e => e.source !== id && e.target !== id)
}
function addEdge() {
  swimConfig.value.edges.push({ source: '', target: '', label: '' })
}
function removeEdge(ei) {
  swimConfig.value.edges.splice(ei, 1)
}
// 全部节点(供连线下拉)
const allNodes = computed(() => {
  const list = []
  swimConfig.value.lanes.forEach((lane, li) => {
    ;(lane.nodes || []).forEach((nd, ni) => {
      list.push({ id: nd.id, key: li + '-' + ni, label: nd.name || ('节点 ' + nd.id) })
    })
  })
  return list
})
// 泳道配置 -> 后端 SwimlaneConfig (生成唯一 id + 构建 lanes/nodes/edges)
function buildSwimlanePayload() {
  const lanes = []
  const nodes = []
  swimConfig.value.lanes.forEach((lane, li) => {
    const laneId = 'L' + (li + 1)
    if (!lane.name || !lane.name.trim()) return
    lanes.push({ id: laneId, name: lane.name.trim() })
    ;(lane.nodes || []).forEach(nd => {
      if (!nd.name || !nd.name.trim()) return
      nodes.push({ id: nd.id, laneId, name: nd.name.trim(), type: nd.type || 'task' })
    })
  })
  const edges = (swimConfig.value.edges || [])
    .filter(e => e.source && e.target && e.source !== e.target)
    .map(e => ({ source: e.source, target: e.target, label: e.label || '' }))
  return { flowName: swimConfig.value.flowName, lanes, nodes, edges }
}

const examples = [
  { typeText: '流程', type: 'FLOW', text: '查询会员余额\nif(余额 >= 商品金额)\n    扣除余额\n    保存订单\nelse\n    返回余额不足' },
  { typeText: '流程', type: 'FLOW', text: '用户登录\nif(账号存在)\n    验证密码\n    if(密码正确)\n        登录成功\n    else\n        提示密码错误\nelse\n    提示账号不存在' },
  { typeText: '架构图', type: 'ARCH', text: '用户通过小程序访问会员服务，会员服务调用订单服务，订单服务查询MySQL数据库' }
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
  if (ex.type === 'SWIMLANE') {
    description.value = ''
  } else {
    description.value = ex.text
  }
}

function nodeShapeName(shape, type) {
  if (shape === 'start' || shape === 'end') return 'ellipse'
  if (shape === 'condition') return 'polygon'
  if (shape === 'database' || shape === 'cache' || shape === 'mq') return 'db'
  if (shape === 'actor' && type === 'ARCH') return 'rect'
  if (shape === 'actor') return 'actorNode'
  if (shape === 'usecase') return 'ellipse'
  if (shape === 'system') return 'systemNode'
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
  if (node.shape === 'usecase') {
    return {
      body: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  if (node.shape === 'system') {
    return {
      body: { fill: 'rgba(255,255,255,0.4)', stroke: color, strokeWidth: 2, rx: 4, ry: 4 },
      title: { text: node.label },
      label: { text: '' }
    }
  }
  if (node.shape === 'actor') {
    return {
      img: { 'xlink:href': '/xiaoren.svg' },
      label: { text: node.label }
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

  // SWIMLANE: 泳道容器为竖列背景, 节点用后端绝对坐标(已含泳道偏移)
  const nodes = []
  const laneIds = new Map() // lane.id -> 容器id
  if (vo.type === 'SWIMLANE' && vo.lanes && vo.lanes.length > 0) {
    vo.lanes.forEach(l => {
      const id = 'lane_' + l.id
      laneIds.set(l.id, id)
      nodes.push({
        id,
        shape: 'rect',
        x: l.x,
        y: l.y,
        width: l.width,
        height: l.height,
        zIndex: 0,
        attrs: {
          body: { fill: '#f0f2f7', stroke: '#c0c4cc', strokeWidth: 1.5, strokeDasharray: '6 3', rx: 8, ry: 8 },
          label: { text: l.name, fill: '#3B6BFF', fontSize: 14, fontWeight: 700, textAnchor: 'start',
                   textVerticalAnchor: 'top', refX: 12, refY: 8 }
        }
      })
    })
  }

  vo.nodes.forEach(n => {
    let w, h
    if (vo.type === 'USECASE') {
      w = n.width || (n.shape === 'actor' ? 120 : 170)
      h = n.height || (n.shape === 'actor' ? 120 : (n.shape === 'system' ? 300 : 56))
    } else {
      w = Math.max(120, n.label.length * 14 + 30)
      h = (n.shape === 'start' || n.shape === 'end') ? 56 : 48
    }
    nodes.push({
      id: n.id,
      shape: nodeShapeName(n.shape, vo.type),
      x: n.x,
      y: n.y,
      width: w,
      height: h,
      attrs: nodeAttrs(n),
      zIndex: n.shape === 'system' ? 0 : 10
    })
  })
  const edges = vo.edges.map(e => {
    const isInclude = e.style === 'include' || e.label === '«include»' || e.label === '«extend»'
    const edge = {
      id: e.id,
      source: e.source,
      target: e.target,
      router: vo.type === 'SWIMLANE' ? { name: 'manhattan', padding: 12 } : undefined,
      attrs: {
        line: isInclude
          ? { stroke: '#333333', strokeWidth: 1.2, strokeDasharray: '6 4', targetMarker: { name: 'block', size: 7 } }
          : { stroke: '#333333', strokeWidth: 1.5, targetMarker: 'block' }
      }
    }
    if (e.label) {
      edge.labels = [{ attrs: { label: { text: e.label, fill: '#666', fontSize: 11 } } }]
    }
    return edge
  })

  // SWIMLANE / USECASE: 泳道已定位, 节点用后端坐标, 不需 Dagre
  if (vo.type === 'SWIMLANE' || vo.type === 'USECASE') {
    graph.fromJSON({ nodes, edges })
    graph.centerContent()
    return
  }

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
  } else if (type.value === 'SWIMLANE') {
    const swim = buildSwimlanePayload()
    if (swim.lanes.length === 0 || swim.nodes.length === 0) {
      ElMessage.warning('请至少配置一个泳道和一个节点')
      return
    }
    payload = { type: 'SWIMLANE', swimlane: swim }
  } else if (type.value === 'USECASE') {
    const uc = buildUseCasePayload()
    if (uc.usecases.length === 0) {
      ElMessage.warning('请至少配置一个用例')
      return
    }
    payload = { type: 'USECASE', useCase: uc }
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

// 内联图片资源缓存: 导出时把相对路径图片(如 /xiaoren.svg)替换为 data URL
const svgImageCache = {}
async function inlineSvgImages(svgClone) {
  const images = svgClone.querySelectorAll('image')
  for (const img of images) {
    const href = img.getAttribute('href') || img.getAttribute('xlink:href')
    if (!href) continue
    if (href.startsWith('data:')) continue
    let dataUrl = svgImageCache[href]
    if (!dataUrl) {
      try {
        const resp = await fetch(href)
        const blob = await resp.blob()
        dataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result)
          reader.onerror = reject
          reader.readAsDataURL(blob)
        })
        svgImageCache[href] = dataUrl
      } catch (e) {
        continue
      }
    }
    img.setAttribute('href', dataUrl)
    img.removeAttribute('xlink:href')
  }
}

function getSvgString() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return ''
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  return '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
}

async function downloadSvg() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  await inlineSvgImages(clone)
  const svg = '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '系统图.svg'
  a.click()
  URL.revokeObjectURL(url)
}

async function downloadPng() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  await inlineSvgImages(clone)
  const svg = '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
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
.lane-act {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.act-type {
  width: 86px;
  flex-shrink: 0;
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
