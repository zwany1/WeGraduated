// ER 图 SVG 后处理:
// 哑节点(基数)可能在 neato 布局中偏离连线, 这里把基数文本精确移动到
// 实体↔菱形连线的中点, 并把两段折线重画为一条共线直线(文字两侧留空隙=线断开)

export function postProcessSvg(svg) {
  const doc = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const ns = 'http://www.w3.org/2000/svg'

  // 收集节点: title -> { cx, cy, hw, hh, hasShape }
  const nodes = {}
  doc.querySelectorAll('g.node').forEach(g => {
    const t = g.querySelector('title')
    if (!t) return
    const title = t.textContent.trim()
    const text = g.querySelector('text')
    const poly = g.querySelector('polygon')
    const ellipse = g.querySelector('ellipse')
    let cx = 0, cy = 0, hw = 0, hh = 0, hasShape = true
    if (poly) {
      const pts = poly.getAttribute('points').split(/\s+/).filter(Boolean).map(p => p.split(',').map(Number))
      if (!pts.length) return
      const xs = pts.map(p => p[0])
      const ys = pts.map(p => p[1])
      cx = (Math.min(...xs) + Math.max(...xs)) / 2
      cy = (Math.min(...ys) + Math.max(...ys)) / 2
      hw = (Math.max(...xs) - Math.min(...xs)) / 2
      hh = (Math.max(...ys) - Math.min(...ys)) / 2
    } else if (ellipse) {
      cx = parseFloat(ellipse.getAttribute('cx'))
      cy = parseFloat(ellipse.getAttribute('cy'))
      hw = parseFloat(ellipse.getAttribute('rx'))
      hh = parseFloat(ellipse.getAttribute('ry'))
    } else if (text) {
      cx = parseFloat(text.getAttribute('x'))
      cy = parseFloat(text.getAttribute('y'))
      hasShape = false
    }
    nodes[title] = { cx, cy, hw, hh, hasShape, text, g }
  })

  // 收集边: title -> { path, g }
  const edges = []
  doc.querySelectorAll('g.edge').forEach(g => {
    const t = g.querySelector('title')
    const p = g.querySelector('path')
    if (t && p) edges.push({ title: t.textContent.trim(), path: p, g })
  })

  // 处理每个哑节点(无形状的 text 节点)
  Object.keys(nodes).forEach(title => {
    const n = nodes[title]
    if (n.hasShape || !n.text) return
    // 找与该哑节点相连的两条边
    const relEdges = edges.filter(e =>
      e.title.includes('--' + title) || e.title.includes(title + '--')
    )
    if (relEdges.length < 2) return
    // 提取两个端点(排除哑节点自身)
    const endpoints = []
    const seen = new Set()
    relEdges.forEach(e => {
      const parts = e.title.split('--').map(s => s.trim())
      parts.forEach(p => {
        if (p !== title && !seen.has(p) && nodes[p] && nodes[p].hasShape) {
          seen.add(p)
          endpoints.push(nodes[p])
        }
      })
    })
    if (endpoints.length < 2) return
    const A = endpoints[0]
    const B = endpoints[1]
    let dx = B.cx - A.cx
    let dy = B.cy - A.cy
    const len = Math.hypot(dx, dy)
    if (len < 1) return
    const ux = dx / len
    const uy = dy / len

    // 端点沿连线方向到边缘的距离
    const edgeDist = (node) => {
      if (node.hw <= 0 || node.hh <= 0) return 0
      const tH = Math.abs(ux) < 1e-9 ? Infinity : node.hw / Math.abs(ux)
      const tV = Math.abs(uy) < 1e-9 ? Infinity : node.hh / Math.abs(uy)
      return Math.min(tH, tV)
    }
    const rA = edgeDist(A)
    const rB = edgeDist(B)
    const E1x = A.cx + ux * rA
    const E1y = A.cy + uy * rA
    const E2x = B.cx - ux * rB
    const E2y = B.cy - uy * rB

    // 文本中点
    const Mx = (E1x + E2x) / 2
    const My = (E1y + E2y) / 2
    // 估算文本宽度
    const label = n.text.textContent || ''
    let textW = 0
    for (const ch of label) {
      textW += /[\u4e00-\u9fff]/.test(ch) ? 12 : 8
    }
    const gap = textW / 2 + 4

    // 移动文本到中点(保持垂直方向对齐原基线偏移)
    n.text.setAttribute('x', Mx.toFixed(2))
    n.text.setAttribute('y', My.toFixed(2))

    // 重画两段共线线段, 删除原两条折线 path
    relEdges.forEach(e => {
      if (e.path) e.path.remove()
    })
    // 线段1: E1 -> M-gap
    // 线段2: M+gap -> E2
    const segs = [
      [E1x, E1y, Mx - ux * gap, My - uy * gap],
      [Mx + ux * gap, My + uy * gap, E2x, E2y]
    ]
    segs.forEach(seg => {
      const line = doc.createElementNS(ns, 'line')
      line.setAttribute('x1', seg[0].toFixed(2))
      line.setAttribute('y1', seg[1].toFixed(2))
      line.setAttribute('x2', seg[2].toFixed(2))
      line.setAttribute('y2', seg[3].toFixed(2))
      line.setAttribute('stroke', 'black')
      line.setAttribute('stroke-width', '1')
      // 插到第一条边附近
      if (relEdges[0] && relEdges[0].g) {
        relEdges[0].g.parentNode.appendChild(line)
      }
    })
  })

  return new XMLSerializer().serializeToString(doc)
}
