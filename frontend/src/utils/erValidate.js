// ER 图一致性校验
// 返回 [{ type: 'error'|'warning', message }]
export function validateEr(entities, relations) {
  const problems = []
  const nameSet = new Map()
  const validNames = new Set()

  // 实体名收集 + 重复检测
  entities.forEach((e, i) => {
    const n = (e.name || '').trim()
    if (!n) {
      problems.push({ type: 'error', message: `实体 ${i + 1} 未填写名称` })
      return
    }
    if (nameSet.has(n)) {
      problems.push({ type: 'error', message: `实体名称重复: "${n}"` })
    } else {
      nameSet.set(n, true)
    }
    validNames.add(n)
  })

  // 实体无属性
  entities.forEach(e => {
    const n = (e.name || '').trim()
    if (!n) return
    const attrs = (e.attrs || []).map(a => (a.name || '').trim()).filter(Boolean)
    if (attrs.length === 0) {
      problems.push({ type: 'warning', message: `实体 "${n}" 没有任何属性` })
    }
  })

  // 关系检查
  const pairSeen = new Map()
  relations.forEach((r, i) => {
    const from = (r.from || '').trim()
    const to = (r.to || '').trim()
    if (!from || !to) {
      problems.push({ type: 'error', message: `关系 ${i + 1} 未选择完整的两个实体` })
      return
    }
    if (from === to) {
      problems.push({ type: 'warning', message: `关系 "${r.label || '未命名'}" 为自环(起点=终点)` })
    }
    if (!validNames.has(from)) {
      problems.push({ type: 'error', message: `关系 "${r.label || '未命名'}" 引用了不存在的实体: "${from}"` })
    }
    if (!validNames.has(to)) {
      problems.push({ type: 'error', message: `关系 "${r.label || '未命名'}" 引用了不存在的实体: "${to}"` })
    }
    // 重复关系检测(同一对实体)
    const key = [from, to].sort().join('##')
    const prev = pairSeen.get(key)
    if (prev) {
      problems.push({ type: 'warning', message: `实体 "${from}" 与 "${to}" 之间定义了多个关系` })
    } else {
      pairSeen.set(key, true)
    }
    // 基数格式
    const card = (r.cardinality || '').trim()
    if (card) {
      const parts = card.split(/[:：]/)
      if (parts.length !== 2 || parts.some(p => !p.trim())) {
        problems.push({ type: 'warning', message: `关系 "${r.label || '未命名'}" 基数格式应为 1:1 / 1:n / n:m, 当前: "${card}"` })
      }
    }
    // 关系无名称
    if (!(r.label || '').trim()) {
      problems.push({ type: 'warning', message: `关系 ${from} ↔ ${to} 未命名` })
    }
  })

  // 孤立实体(没有任何关系连接)
  entities.forEach(e => {
    const n = (e.name || '').trim()
    if (!n) return
    const connected = relations.some(r =>
      (r.from || '').trim() === n || (r.to || '').trim() === n
    )
    if (!connected) {
      problems.push({ type: 'warning', message: `实体 "${n}" 没有任何关系(孤立节点)` })
    }
  })

  return problems
}
