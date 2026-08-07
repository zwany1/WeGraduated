// Chen 记法 ER 图 → Graphviz DOT 脚本转换
export function toDot(entities, relations, engine = 'neato') {
  const esc = (s) => (s || '').replace(/"/g, '\\"')
  // 各布局引擎的参数(减少连线交叉/重叠)
  const engineCfg = {
    neato: ['  layout=neato;', '  overlap=false;', '  splines=true;', '  graph [K=0.6, sep="+10"];'],
    fdp:   ['  layout=fdp;',   '  overlap=false;', '  splines=true;', '  graph [K=0.6, sep="+8", start=3];'],
    circo: ['  layout=circo;', '  overlap=false;', '  splines=true;'],
    dot:   ['  layout=dot;',   '  splines=spline;', '  ranksep=0.8;', '  nodesep=0.5;']
  }
  const cfg = engineCfg[engine] || engineCfg.neato
  const lines = []
  lines.push('graph ER_Diagram {')
  lines.push('  fontname="Microsoft YaHei";')
  lines.push('  node [fontname="Microsoft YaHei", fontsize=12];')
  lines.push('  edge [fontname="Microsoft YaHei", fontsize=10];')
  cfg.forEach(l => lines.push('  ' + l.trim()))
  lines.push('')

  // 1. 实体(矩形, 黑白无填充)
  lines.push('  node [shape=box];')
  const entityNames = entities.map(e => e.name)
  entityNames.forEach(n => lines.push(`  "${esc(n)}";`))
  lines.push('')

  // 2. 实体属性(椭圆, 黑白无填充), 主键下划线
  lines.push('  node [shape=ellipse];')
  entities.forEach(e => {
    e.attrs.forEach(a => {
      const name = esc(a.name)
      const id = `"${esc(e.name)}_${name}"`
      const label = a.key ? `<<u>${esc(a.name)}</u>>` : `"${esc(a.name)}"`
      lines.push(`  ${id} [label=${label}];`)
      lines.push(`  "${esc(e.name)}" -- ${id};`)
    })
  })
  lines.push('')

  // 3. 关系(菱形, 黑白无填充)
  lines.push('  node [shape=diamond];')
  relations.forEach(r => {
    const rid = `"rel_${esc(r.label)}"`
    lines.push(`  ${rid} [label="${esc(r.label)}"];`)
  })
  lines.push('')

  // 4. 关系属性(椭圆, 黑白无填充)
  lines.push('  node [shape=ellipse];')
  relations.forEach(r => {
    const rid = `"rel_${esc(r.label)}"`
    ;(r.attrs || []).forEach(a => {
      const name = esc(a.name)
      const aid = `"rel_${esc(r.label)}_${name}"`
      lines.push(`  ${aid} [label="${esc(a.name)}"];`)
      lines.push(`  ${rid} -- ${aid};`)
    })
  })
  lines.push('')

  // 5. 实体-关系连接: 基数用 edge label 嵌入连线中间(白底遮断线, 同一条边天然共线)
  // 用 HTML-like label 带白底, 文字处线断开, 两段线是同一段的左右半边
  const cardLabel = (t) =>
    `<table border="0" cellborder="0" cellpadding="2" bgcolor="white"><tr><td><font face="Microsoft YaHei" point-size="10">${t}</font></td></tr></table>`
  relations.forEach(r => {
    const rid = `"rel_${esc(r.label)}"`
    const card = (r.cardinality || '').trim()
    const parts = card.split(/[:：]/)
    const fromCard = parts[0] || ''
    const toCard = parts[1] || ''
    if (fromCard) lines.push(`  "${esc(r.from)}" -- ${rid} [label=<${cardLabel(fromCard)}>];`)
    else lines.push(`  "${esc(r.from)}" -- ${rid};`)
    if (toCard) lines.push(`  ${rid} -- "${esc(r.to)}" [label=<${cardLabel(toCard)}>];`)
    else lines.push(`  ${rid} -- "${esc(r.to)}";`)
  })

  lines.push('}')
  return lines.join('\n')
}
