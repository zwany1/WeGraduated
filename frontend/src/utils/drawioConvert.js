/**
 * 自由绘画格式转换:
 * - legacyToDrawioXml: 旧版自定义 JSON({nodes, edges}) → drawio XML(mxGraphModel)
 * - drawioXmlToGraph: drawio XML → 轻量图结构, 供 Mermaid 导出
 */

// 转义 XML 属性/文本中的保留字符
function escXml(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

// 旧版 shape 类型 → drawio 样式
const STYLE_MAP = {
  rect: 'rounded=0;whiteSpace=wrap;html=1;',
  process: 'rounded=0;whiteSpace=wrap;html=1;',
  entity: 'rounded=0;whiteSpace=wrap;html=1;',
  box: 'rounded=0;whiteSpace=wrap;html=1;',
  roundRect: 'rounded=1;whiteSpace=wrap;html=1;',
  terminator: 'rounded=1;arcSize=50;whiteSpace=wrap;html=1;',
  loop: 'rounded=1;whiteSpace=wrap;html=1;',
  ellipse: 'ellipse;whiteSpace=wrap;html=1;',
  circle: 'ellipse;whiteSpace=wrap;html=1;',
  attribute: 'ellipse;whiteSpace=wrap;html=1;',
  usecase: 'ellipse;whiteSpace=wrap;html=1;',
  start: 'ellipse;whiteSpace=wrap;html=1;',
  diamond: 'rhombus;whiteSpace=wrap;html=1;',
  decision: 'rhombus;whiteSpace=wrap;html=1;',
  relation: 'rhombus;whiteSpace=wrap;html=1;',
  hexagon: 'shape=hexagon;whiteSpace=wrap;html=1;',
  triangle: 'triangle;whiteSpace=wrap;html=1;',
  preparation: 'shape=parallelogram;whiteSpace=wrap;html=1;',
  document: 'shape=document;whiteSpace=wrap;html=1;',
  cylinder: 'shape=cylinder3;whiteSpace=wrap;html=1;',
  database: 'shape=cylinder3;whiteSpace=wrap;html=1;',
  cloud: 'ellipse;shape=cloud;whiteSpace=wrap;html=1;',
  note: 'shape=note;whiteSpace=wrap;html=1;',
  actor: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;',
  text: 'text;html=1;strokeColor=none;fillColor=none;',
  folder: 'rounded=0;whiteSpace=wrap;html=1;',
  arrow: 'shape=singleArrow;whiteSpace=wrap;html=1;',
  doubleArrow: 'shape=doubleArrow;whiteSpace=wrap;html=1;',
  dashedArrow: 'shape=singleArrow;whiteSpace=wrap;html=1;dashed=1;',
  curvedArrow: 'shape=singleArrow;whiteSpace=wrap;html=1;',
  blockArrow: 'shape=singleArrow;whiteSpace=wrap;html=1;'
}

const EDGE_STYLE = 'edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;'
const CLASS_ROW_STYLE = 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;'
const CLASS_HEAD_STYLE = 'swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize=26;horizontalStack=0;resizeParent=1;collapsible=0;html=1;'

// class 节点: 属性/方法多行文本折叠为 swimlane 行高估算
function classHeight(attrsText, methodsText) {
  const lines = s => Math.max((s || '').split('\n').filter(Boolean).length, 1)
  return 26 + lines(attrsText) * 20 + lines(methodsText) * 20
}

// 基础 mxGraphModel 骨架
function graphOpen(pageW, pageH) {
  return `<mxGraphModel dx="1000" dy="700" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageW}" pageHeight="${pageH}" math="0" shadow="0">`
}

// 旧版 VO → drawio XML。坐标 1:1 直传, 节点/边 id 原样复用以保持边的引用。
export function legacyToDrawioXml(vo) {
  const nodes = vo.nodes || []
  const edges = vo.edges || []
  const pageW = Math.max(1200, ...nodes.map(n => (n.x || 0) + (n.width || 120)))
  const pageH = Math.max(800, ...nodes.map(n => (n.y || 0) + (n.height || 60)))
  const cells = []

  nodes.forEach(n => {
    const label = n.label || ''
    if (n.shape === 'class') {
      const h = classHeight(n.attrsText, n.methodsText)
      cells.push(
        `<mxCell id="${escXml(n.id)}" value="${escXml(label)}" style="${CLASS_HEAD_STYLE}" vertex="1" parent="1">` +
        `<mxGeometry x="${n.x || 0}" y="${n.y || 0}" width="${n.width || 180}" height="${h}" as="geometry"/></mxCell>`
      )
      const rows = [['attrs', n.attrsText], ['methods', n.methodsText]]
      let y = 26
      rows.forEach(([key, text]) => {
        const rowH = Math.max((text || '').split('\n').filter(Boolean).length, 1) * 20
        cells.push(
          `<mxCell id="${escXml(n.id + '_' + key)}" value="${escXml(text || '')}" style="${CLASS_ROW_STYLE}" vertex="1" parent="${escXml(n.id)}">` +
          `<mxGeometry y="${y}" width="${n.width || 180}" height="${rowH}" as="geometry"/></mxCell>`
        )
        y += rowH
      })
      return
    }
    const style = STYLE_MAP[n.shape] || 'rounded=0;whiteSpace=wrap;html=1;'
    cells.push(
      `<mxCell id="${escXml(n.id)}" value="${escXml(label)}" style="${style}" vertex="1" parent="1">` +
      `<mxGeometry x="${n.x || 0}" y="${n.y || 0}" width="${n.width || 120}" height="${n.height || 60}" as="geometry"/></mxCell>`
    )
  })

  let edgeSeq = 0
  edges.forEach(e => {
    if (!e.source || !e.target) return
    cells.push(
      `<mxCell id="${escXml(e.id || ('e' + edgeSeq++))}" value="${escXml(e.label || '')}" style="${EDGE_STYLE}" edge="1" parent="1" source="${escXml(e.source)}" target="${escXml(e.target)}">` +
      `<mxGeometry relative="1" as="geometry"/></mxCell>`
    )
  })

  return (
    '<mxfile><diagram name="页面-1" id="page1">' + graphOpen(pageW, pageH) +
    '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + cells.join('') +
    '</root></mxGraphModel></diagram></mxfile>'
  )
}

// drawio XML → 轻量图结构(节点 id/label/形状 + 边引用), 供 Mermaid 导出。
// swimlane 子单元格(类行)跳过, 仅保留顶层节点与边。
export function drawioXmlToGraph(xml) {
  const doc = new DOMParser().parseFromString(xml, 'text/xml')
  if (doc.querySelector('parsererror')) return { nodes: [], edges: [] }
  const nodes = []
  const edges = []
  const all = Array.from(doc.querySelectorAll('mxCell'))
  all.forEach(c => {
    const parent = c.getAttribute('parent')
    // 父级为普通节点(非 0/1)的是 swimlane 类行, 跳过
    if (parent && parent !== '0' && parent !== '1') return
    const id = c.getAttribute('id')
    const value = (c.getAttribute('value') || '').replace(/<br\s*\/?>/gi, ' ').replace(/<[^>]+>/g, '').trim()
    if (c.getAttribute('edge') === '1') {
      const src = c.getAttribute('source')
      const tgt = c.getAttribute('target')
      if (src && tgt) edges.push({ source: src, target: tgt, label: value })
      return
    }
    if (c.getAttribute('vertex') !== '1') return
    const style = c.getAttribute('style') || ''
    let shape = 'rect'
    if (style.includes('swimlane')) shape = 'class'
    else if (style.includes('ellipse')) shape = 'ellipse'
    else if (style.includes('rhombus')) shape = 'diamond'
    else if (style.includes('shape=umlActor')) shape = 'actor'
    else if (style.includes('shape=cylinder3')) shape = 'database'
    else if (style.includes('shape=note')) shape = 'note'
    else if (style.includes('shape=hexagon')) shape = 'hexagon'
    nodes.push({ id, label: value, shape })
  })
  return { nodes, edges }
}
