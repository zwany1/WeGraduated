// 图表 → Mermaid 文本生成器
// 支持: 架构图/流程图(DSL)/泳道图/活动图/用例图/时序图/类图/ER图/自由绘画(drawio XML)

import { drawioXmlToGraph } from './drawioConvert'

const esc = s => String(s == null ? '' : s)
  .replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  .replace(/#/g, '&#35;').replace(/\(/g, '&#40;').replace(/\)/g, '&#41;')

const label = s => '["' + esc(s) + '"]'
const decision = s => '{"' + esc(s) + '"}'

// ===== 架构图 → flowchart(层=subgraph, 层间依赖连接) =====
export function archToMermaid(config) {
  const c = config || {}
  const lines = ['flowchart TB']
  const layers = (c.layers || []).filter(l => l.name)
  layers.forEach((layer, li) => {
    const comps = (layer.components || []).map(cp => cp.name).filter(Boolean)
    lines.push(`  subgraph L${li}["${esc(layer.name)}"]`)
    comps.forEach((name, ci) => lines.push(`    C${li}_${ci}${label(name)}`))
    lines.push('  end')
  })
  // 层间依赖: 每层组件指向下一层(分层架构的调用/依赖关系)
  for (let i = 0; i < layers.length - 1; i++) {
    const upper = (layers[i].components || []).map(cp => cp.name).filter(Boolean)
    const lower = (layers[i + 1].components || []).map(cp => cp.name).filter(Boolean)
    upper.forEach((_, u) => {
      lower.forEach((__, d) => lines.push(`  C${i}_${u} --> C${i + 1}_${d}`))
    })
  }
  return lines.join('\n')
}

// ===== 泳道图 → flowchart(泳道=subgraph) =====
export function swimToMermaid(swim) {
  const c = swim || {}
  const lines = ['flowchart LR']
  const idOf = nodeId => 'N_' + String(nodeId).replace(/[^A-Za-z0-9]/g, '_')
  ;(c.lanes || []).forEach((lane, li) => {
    const nodes = (lane.nodes || []).filter(n => n.name)
    if (!lane.name) return
    lines.push(`  subgraph L${li}["${esc(lane.name)}"]`)
    nodes.forEach(n => lines.push(`    ${idOf(n.id)}${label(n.name)}`))
    lines.push('  end')
  })
  ;(c.edges || []).forEach(e => {
    if (!e.source || !e.target) return
    const src = idOf(e.source), tgt = idOf(e.target)
    lines.push(`  ${src} -->${e.label ? '|' + esc(e.label) + '|' : ''} ${tgt}`)
  })
  return lines.join('\n')
}

// ===== 活动图 → flowchart =====
export function activityToMermaid(act) {
  const c = act || {}
  const lines = ['flowchart TD']
  const nodes = (c.nodes || []).filter(n => n.text)
  const byId = {}
  nodes.forEach((n, i) => { byId[n.id] = 'N' + i })
  nodes.forEach((n, i) => {
    if (n.type === 'DECISION') lines.push(`  N${i}${decision(n.text)}`)
    else if (n.type === 'START') lines.push(`  N${i}(["${esc(n.text)}"])`)
    else if (n.type === 'END') lines.push(`  N${i}(["${esc(n.text)}"])`)
    else lines.push(`  N${i}${label(n.text)}`)
  })
  ;(c.edges || []).forEach(e => {
    if (byId[e.source] == null || byId[e.target] == null) return
    lines.push(`  N${byId[e.source]} -->${e.label ? '|' + esc(e.label) + '|' : ''} N${byId[e.target]}`)
  })
  return lines.join('\n')
}

// ===== 用例图 → flowchart =====
export function usecaseToMermaid(uc) {
  const c = uc || {}
  const lines = ['flowchart LR']
  const labelOf = id => {
    const a = (c.actors || []).find(x => x.id === id)
    const u = (c.usecases || []).find(x => x.id === id)
    return a ? a.name : (u ? u.name : id)
  }
  lines.push(`  subgraph SYS["${esc(c.system || '系统')}"]`)
  ;(c.usecases || []).filter(u => u.name).forEach((u, i) => lines.push(`    U${i}${label(u.name)}`))
  lines.push('  end')
  ;(c.actors || []).filter(a => a.name).forEach((a, i) => lines.push(`  A${i}(["${esc(a.name)}"])`))
  const aId = {}, uId = {}
  ;(c.actors || []).forEach((a, i) => { aId[a.id] = 'A' + i })
  ;(c.usecases || []).forEach((u, i) => { uId[u.id] = 'U' + i })
  ;(c.relations || []).forEach(r => {
    const s = aId[r.source] || uId[r.source]
    const t = aId[r.target] || uId[r.target]
    if (!s || !t) return
    if (r.type === 'include') lines.push(`  ${s} -->|include| ${t}`)
    else if (r.type === 'extend') lines.push(`  ${s} -.->|extend| ${t}`)
    else lines.push(`  ${s} --- ${t}`)
  })
  return lines.join('\n')
}

// ===== 时序图 → sequenceDiagram =====
export function sequenceToMermaid(seq) {
  const c = seq || {}
  const lines = ['sequenceDiagram']
  const participants = (c.participants || []).filter(p => p.name)
  participants.forEach((p, i) => lines.push(`  participant ${p.id} as ${esc(p.name)}`))
  ;(c.messages || []).forEach(m => {
    if (!m.from || !m.to || !m.text) return
    const arrow = m.type === 'return' ? '-->>' : '->>'
    lines.push(`  ${m.from}${arrow}${m.to}: ${esc(m.text)}`)
  })
  return lines.join('\n')
}

// ===== 类图 → classDiagram =====
export function classToMermaid(cls) {
  const c = cls || {}
  const lines = ['classDiagram']
  ;(c.classes || []).filter(k => k.name).forEach(k => {
    lines.push(`  class ${esc(k.name)} {`)
    ;(k.attributes || []).filter(a => a.name).forEach(a => {
      const vis = a.visibility === '+' ? '+' : a.visibility === '#' ? '#' : '-'
      lines.push(`    ${vis}${esc(a.name)} : ${esc(a.type || '')}`)
    })
    ;(k.methods || []).filter(m => m.name).forEach(m => {
      const vis = m.visibility === '+' ? '+' : m.visibility === '#' ? '#' : '-'
      lines.push(`    ${vis}${esc(m.name)}() ${esc(m.returnType || '')}`)
    })
    lines.push('  }')
  })
  const idToName = {}
  ;(c.classes || []).forEach(k => { idToName[k.id] = k.name })
  ;(c.relations || []).forEach(r => {
    const a = idToName[r.source], b = idToName[r.target]
    if (!a || !b) return
    let rel = '--'
    if (r.type === 'composition') rel = '*--'
    else if (r.type === 'aggregation') rel = 'o--'
    else if (r.type === 'inheritance') rel = '<|--'
    else if (r.type === 'dependency') rel = '..>'
    const card = [esc(r.left || ''), esc(r.right || '')].filter(Boolean).join('..')
    lines.push(`  ${esc(a)} ${rel} ${esc(b)}${card ? ' : ' + card : ''}`)
  })
  return lines.join('\n')
}

// ===== ER 图 → erDiagram =====
export function erToMermaid(entities, relations) {
  const lines = ['erDiagram']
  const enId = {}
  ;(entities || []).forEach((e, i) => {
    if (!e.name) return
    const id = 'E' + (i + 1) // Mermaid 实体名须为标识符, 用序号避免中文
    enId[e.name] = id
    lines.push(`  ${id} {`)
    ;(e.attrs || []).filter(a => a.name).forEach(a => {
      // Mermaid erDiagram 属性行语法: <类型> <名称> [PK]
      lines.push(`    string ${esc(a.name)}${a.key ? ' PK' : ''}`)
    })
    lines.push('  }')
  })
  ;(relations || []).forEach(r => {
    const a = enId[r.from], b = enId[r.to]
    if (!a || !b) return
    const card = String(r.cardinality || '').replace(/\s+/g, '').toLowerCase()
    const left = card === '1:1' ? '||--||'
      : card === 'm:1' || card === 'n:1' ? '}o--||'
      : card === '1:m' || card === '1:n' ? '||--o{'
      : '}o--o{'
    lines.push(`  ${a} ${left} ${b} : ${esc(r.label || '')}`)
  })
  return lines.join('\n')
}

// ===== 流程图 DSL → flowchart =====
export function flowToMermaid(dsl) {
  const raw = String(dsl || '')
  const lines = raw.split('\n').map(l => l.replace(/\r/g, ''))
    .map(l => ({ text: l.trim(), indent: l.search(/\S/), trimmed: l.trim() }))
    .filter(x => x.trimmed)

  const out = ['flowchart TD']
  let seq = 0
  const nid = () => 'N' + (++seq)
  const isIf = t => /^if\s*\(/.test(t) || /^if\s*：/.test(t) || /^如果/.test(t) || /^if\s+/.test(t)
  const isElse = t => /^else\s*$/.test(t) || /^否则/.test(t)

  // 构建缩进树
  const root = { indent: -1, children: [] }
  const stack = [root]
  for (const l of lines) {
    while (stack.length > 1 && l.indent <= stack[stack.length - 1].indent) stack.pop()
    const node = { indent: l.indent, text: l.trimmed, children: [] }
    stack[stack.length - 1].children.push(node)
    stack.push(node)
  }

  // 顺序渲染一段节点: prev 为入口, firstLabel 用于第一个子节点从入口连出的分支标签
  // 结构: if 的缩进子块为"是"分支; if 之后的兄弟 else 及其缩进子块为"否"分支
  const renderSeq = (nodes, prev, firstLabel) => {
    let p = prev
    let isFirst = true
    let i = 0
    while (i < nodes.length) {
      const n = nodes[i]
      const arrow = (isFirst && firstLabel) ? ` -- ${firstLabel} --> ` : ' --> '
      if (isIf(n.text)) {
        const id = nid()
        const cond = n.text.replace(/^if\s*\(/, '').replace(/\)\s*$/, '').trim() || n.text
        out.push(`  ${id}${decision(cond)}`)
        if (p) out.push(`  ${p}${arrow}${id}`)
        const then = n.children || []
        const next = nodes[i + 1]
        const hasElse = next && isElse(next.text)
        const els = hasElse ? (next.children || []) : []
        renderSeq(then, id, '是')
        if (els.length) renderSeq(els, id, '否')
        p = null
        i += hasElse ? 2 : 1
      } else if (!isElse(n.text)) {
        const id = nid()
        out.push(`  ${id}${label(n.text)}`)
        if (p) out.push(`  ${p}${arrow}${id}`)
        p = id
        i++
      } else {
        i++ // 孤立 else(前面无 if): 其子块由外层已处理
      }
      isFirst = false
    }
    return p
  }

  renderSeq(root.children, null, null)
  return out.join('\n')
}

// ===== 自由绘画 drawio XML → flowchart(节点+连线) =====
export function drawioToMermaid(xml) {
  const { nodes, edges } = drawioXmlToGraph(xml)
  const lines = ['flowchart LR']
  const idOf = id => 'N_' + String(id).replace(/[^A-Za-z0-9]/g, '_')
  nodes.forEach(n => {
    const lbl = n.label || n.id
    const box = n.shape === 'ellipse' || n.shape === 'actor' ? '(([' + esc(lbl) + ']))'
      : n.shape === 'diamond' ? '{' + esc(lbl) + '}'
      : '[' + esc(lbl) + ']'
    lines.push(`  ${idOf(n.id)}${box}`)
  })
  edges.forEach(e => {
    if (!e.source || !e.target) return
    lines.push(`  ${idOf(e.source)} -->${e.label ? '|' + esc(e.label) + '|' : ''} ${idOf(e.target)}`)
  })
  return lines.join('\n')
}

// 顶层分发
export function toMermaid(type, data, extra) {
  switch (type) {
    case 'ARCH': return archToMermaid(data)
    case 'FLOW': return flowToMermaid(data)
    case 'SWIMLANE': return swimToMermaid(data)
    case 'ACTIVITY': return activityToMermaid(data)
    case 'USECASE': return usecaseToMermaid(data)
    case 'SEQUENCE': return sequenceToMermaid(data)
    case 'CLASS': return classToMermaid(data)
    case 'ER': return erToMermaid(data, extra)
    case 'FREEDRAW': return drawioToMermaid(data)
    case 'VO': return voToMermaid(data)
    default: return 'flowchart TD\n  A(["待支持"])'
  }
}

/**
 * 根据生成的图数据(节点+边)导出 flowchart Mermaid —— 保证导出的内容与画布上的图一致。
 */
export function voToMermaid(vo) {
  const lines = ['flowchart TD']
  const idMap = {}
  let seq = 0
  ;(vo.nodes || []).forEach(n => {
    const id = 'N' + (seq++)
    idMap[n.id] = id
    const label = (n.label || '').replace(/"/g, '')
    lines.push(`  ${id}["${label || id}"]`)
  })
  ;(vo.edges || []).forEach(e => {
    const s = idMap[e.source] || ('S' + e.source)
    const t = idMap[e.target] || ('T' + e.target)
    const lbl = (e.label || e.relationText || '').replace(/"/g, '')
    lines.push(`  ${s} -->${lbl ? '|' + lbl + '|' : ''} ${t}`)
  })
  return lines.join('\n')
}
