<template>
  <WorkbenchShell title="我的格式方案">
    <template #meta>
      共 {{ templates.length }} 套 · 个人 {{ personalCount }} / 团队 {{ teamCount }}<br />
      按最近改动排序 · 红点是你上次打开的那套
    </template>

    <template #actions>
      <button class="wbtn ghost" type="button" @click="triggerImport">导入方案</button>
      <button class="wbtn solid" type="button" @click="showCreate = true">新建格式方案</button>
    </template>

    <template #toolbar>
      <div class="flt">
        <label class="flt-search">
          <span class="flt-k">搜索</span>
          <input v-model="keyword" type="search" placeholder="方案名称" />
        </label>
        <div class="flt-chips">
          <button :class="['chip', { on: teamFilter === null }]" type="button" @click="teamFilter = null">全部归属</button>
          <button :class="['chip', { on: teamFilter === 0 }]" type="button" @click="teamFilter = 0">个人</button>
          <button
            v-for="tm in teams"
            :key="tm.id"
            :class="['chip', { on: teamFilter === tm.id }]"
            type="button"
            @click="teamFilter = tm.id"
          >{{ tm.name }}</button>
        </div>
        <span class="flt-count">{{ filteredTemplates.length }} / {{ templates.length }}</span>
      </div>
    </template>

    <input ref="importFileRef" type="file" accept=".json" class="hidden-file" @change="onImportFile" />

    <!-- 空状态 -->
    <div v-if="filteredTemplates.length === 0" class="empty">
      <p class="empty-k">{{ templates.length ? '没有符合条件的方案' : '还没有格式方案' }}</p>
      <p class="empty-d">
        {{ templates.length ? '换个关键词，或把归属切回「全部」。' : '新建一套校规配置，用它跑第一次排版；也可以导入同学发来的 JSON。' }}
      </p>
      <div class="empty-a">
        <button class="wbtn solid" type="button" @click="templates.length ? resetFilter() : (showCreate = true)">
          {{ templates.length ? '清空筛选' : '新建格式方案' }}
        </button>
        <button v-if="!templates.length" class="wbtn ghost" type="button" @click="triggerImport">导入方案</button>
      </div>
    </div>

    <!-- 时间线 -->
    <div v-else class="tl">
      <span class="tl-axis" aria-hidden="true"></span>
      <article v-for="(t, i) in pagedTemplates" :key="t.id" class="tl-item">
        <time class="tl-date">{{ relTime(t.updateTime || t.createTime) }}</time>
        <span :class="['tl-node', { now: i === 0 && page === 1 }]" aria-hidden="true"></span>

        <div class="tl-body">
          <h3>
            {{ t.name }}
            <span v-if="t.teamId && teamMap[t.teamId]" class="tag team">{{ teamMap[t.teamId].name }}</span>
            <span v-else class="tag">个人</span>
          </h3>

          <p v-if="missing[t.id] && missing[t.id].length" class="tl-state bad">
            缺 {{ missing[t.id].length }} 项关键规则：{{ missing[t.id].join('、') }}。补齐后再用于正式排版。
          </p>
          <p v-else class="tl-state ok">规则完整，可直接用于排版。</p>

          <div class="tl-ops">
            <a class="op main" @click="$router.push(`/template/${t.id}`)">配置格式</a>
            <a class="op" @click="$router.push('/tasks')">用它排版</a>
            <a class="op" @click="clone(t)">克隆</a>
            <a class="op" @click="exportConfig(t)">导出 JSON</a>
            <a class="op" @click="markCompare(t)">{{ compareA && compareA.id === t.id ? '对比基准…' : '对比' }}</a>
            <a class="op danger" @click="remove(t)">删除</a>
          </div>
        </div>
      </article>
    </div>

    <template #footer>
      <span>方案可导出为 JSON 分享给同学；删除后 6 秒内可撤销</span>
      <span v-if="filteredTemplates.length > pageSize" class="pager">
        <button type="button" :disabled="page === 1" @click="page--">上一页</button>
        <b>{{ page }}</b>
        <span>/ {{ Math.ceil(filteredTemplates.length / pageSize) }}</span>
        <button type="button" :disabled="page >= Math.ceil(filteredTemplates.length / pageSize)" @click="page++">下一页</button>
      </span>
    </template>
  </WorkbenchShell>

  <!-- 新建 -->
  <el-dialog v-model="showCreate" title="新建格式方案" width="440px">
    <div class="dlg">
      <label>方案名称</label>
      <el-input v-model="newName" placeholder="如：本校本科毕业论文格式" />
      <label>归属</label>
      <el-select v-model="newTeamId" placeholder="默认个人" clearable style="width: 100%">
        <el-option v-for="tm in teams" :key="tm.id" :label="`团队：${tm.name}`" :value="tm.id" />
      </el-select>
    </div>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="create">创建并配置</el-button>
    </template>
  </el-dialog>

  <!-- 两两对比 -->
  <el-dialog v-model="diffVisible" title="模板配置对比" width="820px" top="6vh">
    <p class="diff-sub" v-if="compareB">{{ compareB.name }} · 仅列出不同的参数</p>
    <el-table v-if="diffRows.length" :data="diffRows" size="small" max-height="440">
      <el-table-column prop="group" label="配置组" width="110" />
      <el-table-column prop="key" label="参数" width="190" />
      <el-table-column prop="a" label="基准方案" min-width="170" show-overflow-tooltip />
      <el-table-column prop="b" label="对比方案" min-width="170" show-overflow-tooltip />
    </el-table>
    <el-empty v-else description="两个方案的配置完全一致" :image-size="60" />
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import WorkbenchShell from '../components/WorkbenchShell.vue'
import { listTemplates, createTemplate, deleteTemplate, cloneTemplate, getMissingRules, getTemplateDetail, saveAllConfig } from '../api/template'
import { listTeams } from '../api/team'

const router = useRouter()
const templates = ref([])
const keyword = ref('')
const teamFilter = ref(null)
const page = ref(1)
const pageSize = 8

const personalCount = computed(() => templates.value.filter(t => !t.teamId).length)
const teamCount = computed(() => templates.value.filter(t => !!t.teamId).length)

const filteredTemplates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const list = templates.value.filter(t => {
    if (kw && !(t.name || '').toLowerCase().includes(kw)) return false
    if (teamFilter.value === null || teamFilter.value === undefined) return true
    if (teamFilter.value === 0) return !t.teamId
    return t.teamId === teamFilter.value
  })
  // 最近改动优先：后端若给了 updateTime 用它，否则退回 createTime
  return list.slice().sort((a, b) => {
    const ta = new Date(a.updateTime || a.createTime || 0).getTime() || 0
    const tb = new Date(b.updateTime || b.createTime || 0).getTime() || 0
    return tb - ta
  })
})
const pagedTemplates = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredTemplates.value.slice(start, start + pageSize)
})
watch([keyword, teamFilter], () => {
  page.value = 1
})

function resetFilter() {
  keyword.value = ''
  teamFilter.value = null
}

const showCreate = ref(false)
const newName = ref('')
const newTeamId = ref(null)
const creating = ref(false)
const teams = ref([])
const teamMap = ref({})
const missing = ref({})

onMounted(async () => {
  load()
  try {
    teams.value = (await listTeams()) || []
    const m = {}
    teams.value.forEach(t => {
      m[t.id] = t
    })
    teamMap.value = m
  } catch (e) {}
})

async function load() {
  try {
    templates.value = (await listTemplates()) || []
  } catch (e) {
    templates.value = []
    return
  }
  // 并行检查每个模板的规则完整性（缺规则要在时间线上直接标出来）
  const next = {}
  await Promise.all(
    templates.value.map(async t => {
      try {
        const m = await getMissingRules(t.id)
        if (m && m.length) next[t.id] = m
      } catch (e) {}
    })
  )
  missing.value = next
}

/** 相对时间：今天 14:02 / 昨天 / 3 天前 / 2026-09-12 */
function relTime(t) {
  if (!t) return '—'
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(t).slice(0, 10)
  const now = new Date()
  const startOfDay = x => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const days = Math.round((startOfDay(now) - startOfDay(d)) / 86400000)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (days === 0) return `今天 ${hh}:${mm}`
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  if (days < 30) return `${Math.floor(days / 7)} 周前`
  if (days < 365) return `${Math.floor(days / 30)} 个月前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function clone(t) {
  try {
    await cloneTemplate(t.id)
    ElMessage.success('克隆成功')
    load()
  } catch (e) {
    ElMessage.error(e.message || '克隆失败')
  }
}

async function create() {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  creating.value = true
  try {
    const t = await createTemplate({ name: newName.value.trim(), teamId: newTeamId.value || undefined })
    ElMessage.success('创建成功')
    showCreate.value = false
    newTeamId.value = null
    router.push(`/template/${t.id}`)
  } finally {
    creating.value = false
  }
}

async function remove(t) {
  try {
    await ElMessageBox.confirm(`确定删除「${t.name}」？其全部格式规则将一并删除。`, '删除格式方案', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch (e) {
    return
  }
  // 记录配置快照, 删除后可 6 秒内点通知撤销(恢复为新的格式方案)
  let snapshot = null
  try {
    snapshot = detailToConfig((await getTemplateDetail(t.id)).template)
  } catch (e) {}
  await deleteTemplate(t.id)
  ElMessage.success('已删除')
  load()
  if (snapshot) {
    ElNotification({
      title: '已删除「' + t.name + '」',
      message: '点这里撤销（将恢复为一个新的格式方案）',
      type: 'warning',
      duration: 6000,
      onClick: async () => {
        try {
          const nt = await createTemplate({ name: snapshot.name, teamId: snapshot.teamId || undefined })
          await saveAllConfig(nt.id, {
            pageConfig: snapshot.pageConfig || '',
            heading1: parseHp(snapshot.headingPatterns).heading1,
            heading2: parseHp(snapshot.headingPatterns).heading2,
            heading3: parseHp(snapshot.headingPatterns).heading3,
            generateToc: !!snapshot.generateToc,
            generateAbstract: !!snapshot.generateAbstract,
            referenceConfig: snapshot.referenceConfig || '',
            tocConfig: snapshot.tocConfig || '',
            rules: (snapshot.rules || []).map(r => ({ ...r }))
          })
          ElMessage.success('已恢复为新的格式方案')
          load()
        } catch (e) {}
      }
    })
  }
}

// ===== 配置导出 / 导入 / 两两对比 =====
const importFileRef = ref(null)
const diffVisible = ref(false)
const diffRows = ref([])
const compareA = ref(null)
const compareB = ref(null)

function detailToConfig(t) {
  return {
    name: t.name,
    teamId: t.teamId || undefined,
    pageConfig: t.pageConfig || '',
    headingPatterns: t.headingPatterns || '',
    referenceConfig: t.referenceConfig || '',
    tocConfig: t.tocConfig || '',
    generateToc: !!t.generateToc,
    generateAbstract: !!t.generateAbstract,
    rules: (t.rules || []).map(r => ({ ...r }))
  }
}

function parseHp(s) {
  try {
    const o = JSON.parse(s || '{}')
    return { heading1: o.heading1 || '', heading2: o.heading2 || '', heading3: o.heading3 || '' }
  } catch (e) {
    return { heading1: '', heading2: '', heading3: '' }
  }
}

async function exportConfig(t) {
  try {
    const detail = await getTemplateDetail(t.id)
    const payload = { app: 'WeGraduated', type: 'template-config', version: 1, config: detailToConfig(detail.template) }
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `格式方案-${t.name}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出，发给同学即可直接导入')
  } catch (e) {}
}

function triggerImport() {
  if (importFileRef.value) importFileRef.value.click()
}

async function onImportFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  let payload = null
  try {
    payload = JSON.parse(await file.text())
  } catch (err) {
    ElMessage.error('文件不是合法的 JSON')
    return
  }
  const cfg = payload && payload.config ? payload.config : payload
  if (!cfg || !cfg.name) {
    ElMessage.error('文件格式不对（缺少方案名称）')
    return
  }
  try {
    const t = await createTemplate({ name: cfg.name + '（导入）', teamId: cfg.teamId || undefined })
    const hp = parseHp(cfg.headingPatterns)
    await saveAllConfig(t.id, {
      pageConfig: cfg.pageConfig || '',
      heading1: hp.heading1,
      heading2: hp.heading2,
      heading3: hp.heading3,
      generateToc: !!cfg.generateToc,
      generateAbstract: !!cfg.generateAbstract,
      referenceConfig: cfg.referenceConfig || '',
      tocConfig: cfg.tocConfig || '',
      rules: (cfg.rules || []).map(r => ({ ...r }))
    })
    ElMessage.success('导入成功')
    load()
  } catch (err) {}
}

async function markCompare(t) {
  if (!compareA.value) {
    compareA.value = { id: t.id, name: t.name, detail: null }
    ElMessage.info(`已选「${t.name}」为对比基准，再点另一个方案的「对比」完成比较`)
    return
  }
  if (compareA.value.id === t.id) {
    compareA.value = null
    return
  }
  try {
    if (!compareA.value.detail) {
      compareA.value.detail = (await getTemplateDetail(compareA.value.id)).template
    }
    const bDetail = (await getTemplateDetail(t.id)).template
    diffRows.value = diffConfig(compareA.value.detail, bDetail)
    compareB.value = { id: t.id, name: t.name }
    diffVisible.value = true
    compareA.value = null
  } catch (e) {}
}

/** 将模板详情摊平成 "组/参数 → 值" 便于逐项比较 */
function flattenConfig(t) {
  const m = {}
  const pg = (() => {
    try {
      return JSON.parse(t.pageConfig || '{}')
    } catch (e) {
      return {}
    }
  })()
  const mg = pg.margin || {}
  m['页面/纸张'] = pg.paper || 'A4'
  m['页面/边距 上·下·左·右(cm)'] = [mg.top, mg.bottom, mg.left, mg.right].map(v => (v == null ? '—' : v)).join(' / ')
  m['页面/页码位置'] = (pg.footer && pg.footer.pageNumber) || 'none'
  m['页面/页眉文字'] = (pg.header && pg.header.text) || ''
  const hp = parseHp(t.headingPatterns)
  m['标题/一级正则'] = hp.heading1
  m['标题/二级正则'] = hp.heading2
  m['标题/三级正则'] = hp.heading3
  const typeNames = { heading1: '一级标题', heading2: '二级标题', heading3: '三级标题', body: '正文', figure: '图题注', table: '表题注', tableText: '表格文字' }
  const rulesMap = {}
  ;(t.rules || []).forEach(r => {
    rulesMap[r.ruleType] = r
  })
  Object.keys(typeNames).forEach(type => {
    const r = rulesMap[type] || {}
    const g = typeNames[type]
    m[`${g}/字体(中文/西文)`] = `${r.font || '—'} / ${r.fontLatin || '—'}`
    m[`${g}/字号(pt)`] = r.fontSize == null ? '—' : r.fontSize
    m[`${g}/加粗`] = r.bold ? '是' : '否'
    m[`${g}/对齐`] = r.align || '—'
    if (type === 'body') {
      m['正文/行距'] = r.lineSpacingType === 'exact' ? `固定 ${r.lineSpacingExact} 磅` : `${r.lineSpacing || 1.5} 倍`
      m['正文/首行缩进(字符)'] = r.firstLineIndent == null ? '—' : r.firstLineIndent
    }
  })
  const rc = (() => {
    try {
      return JSON.parse(t.referenceConfig || '{}')
    } catch (e) {
      return {}
    }
  })()
  m['参考文献/启用'] = rc.enabled ? '是' : '否'
  m['参考文献/条目字体'] = `${rc.itemFont || '—'} ${rc.itemFontSize || ''}`
  const tc = (() => {
    try {
      return JSON.parse(t.tocConfig || '{}')
    } catch (e) {
      return {}
    }
  })()
  m['目录/行距'] = tc.lineSpacing || '—'
  m['目录/前导符'] = tc.leader || '—'
  return m
}

function diffConfig(a, b) {
  const ma = flattenConfig(a)
  const mb = flattenConfig(b)
  const rows = []
  Object.keys(ma).forEach(k => {
    if (String(ma[k]) !== String(mb[k])) {
      const [group, key] = k.split('/')
      rows.push({ group, key, a: String(ma[k]), b: String(mb[k]) })
    }
  })
  return rows
}
</script>

<style scoped>
.hidden-file {
  display: none;
}

/* ---------------- 工具栏：搜索 + 归属 chip ---------------- */
.flt {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  width: 100%;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--hair);
}
.flt-search {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--ink);
  min-width: 260px;
}
.flt-k {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
}
.flt-search input {
  flex: 1;
  border: 0;
  outline: none;
  background: none;
  font-family: inherit;
  font-size: 14px;
  color: var(--ink);
}
.flt-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  height: 38px;
  padding: 0 14px;
  background: none;
  border: 1px solid var(--hair);
  font-family: inherit;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
}
.chip:hover {
  border-color: var(--ink);
  color: var(--ink);
}
.chip.on {
  border-color: var(--ink);
  color: var(--ink);
  font-weight: 700;
}
.flt-count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

/* ---------------- 按钮 ---------------- */
.wbtn {
  height: 38px;
  padding: 0 18px;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  border: 0;
}
.wbtn.solid {
  background: var(--red);
  color: #fff;
  font-weight: 700;
}
.wbtn.solid:hover {
  background: #b91d1e;
}
.wbtn.ghost {
  background: none;
  border: 1px solid var(--ink);
  color: var(--ink);
}
.wbtn.ghost:hover {
  background: var(--ink);
  color: var(--paper);
}

/* ---------------- 时间线 ---------------- */
.tl {
  position: relative;
  margin-top: 8px;
}
.tl-axis {
  display: none;
}
.tl-item {
  position: relative;
  display: grid;
  grid-template-columns: 176px 48px 1fr;
  align-items: start;
  padding: 26px 0;
}
.tl-item + .tl-item {
  border-top: 1px solid var(--hair-soft);
}
/* 轴线只连接相邻两个节点：最后一项不再往下画 */
.tl-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 199px;
  top: 23px;
  bottom: -27px;
  width: 1px;
  background: var(--ink);
}
.tl-date {
  text-align: right;
  font-size: 13px;
  letter-spacing: 0.05em;
  color: var(--ink-3);
  padding-top: 9px;
}
.tl-node {
  position: relative;
}
.tl-node::before {
  content: '';
  position: absolute;
  left: 18px;
  top: 12px;
  width: 11px;
  height: 11px;
  background: var(--paper);
  border: 2px solid var(--ink);
  border-radius: 50%;
  box-sizing: border-box;
}
.tl-node.now::before {
  background: var(--red);
  border-color: var(--red);
}
.tl-body h3 {
  margin: 0 0 8px;
  font-size: 26px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.01em;
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.tag {
  font-size: 11.5px;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: var(--ink-2);
  border: 1px solid var(--hair);
  padding: 2px 8px;
}
.tag.team {
  border-color: var(--ink);
  color: var(--ink);
}
.tl-state {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 720px;
}
.tl-state.bad {
  color: var(--red);
  font-weight: 500;
}
.tl-ops {
  margin-top: 14px;
  display: flex;
  gap: 22px;
  flex-wrap: wrap;
}
.op {
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  border-bottom: 1px solid transparent;
  padding-bottom: 2px;
}
.op:hover {
  border-bottom-color: var(--hair);
  color: var(--ink);
}
.op.main {
  color: var(--ink);
  font-weight: 700;
  border-bottom: 2px solid var(--red);
}
.op.danger:hover {
  color: var(--red);
  border-bottom-color: var(--red);
}

/* ---------------- 空状态 ---------------- */
.empty {
  padding: 72px 0 96px;
  border-bottom: 1px solid var(--hair-soft);
}
.empty-k {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
}
.empty-d {
  margin: 0 0 22px;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 520px;
}
.empty-a {
  display: flex;
  gap: 12px;
}

/* ---------------- 分页 ---------------- */
.pager {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--ink-2);
}
.pager button {
  background: none;
  border: 1px solid var(--hair);
  height: 30px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
}
.pager button:disabled {
  color: var(--hair);
  border-color: var(--hair-soft);
  cursor: not-allowed;
}
.pager b {
  color: var(--ink);
}

/* ---------------- 弹窗内容 ---------------- */
.dlg {
  display: grid;
  gap: 10px;
}
.dlg label {
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--ink-3);
}
.dlg label:not(:first-child) {
  margin-top: 8px;
}
.diff-sub {
  margin: 0 0 14px;
  font-size: 13.5px;
  color: var(--ink-2);
}

/* ---------------- 窄屏 ---------------- */
@media (max-width: 900px) {
  .flt {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .flt-search {
    width: 100%;
    min-width: 0;
    flex: none;
  }
  .flt-chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    -webkit-overflow-scrolling: touch;
  }
  .chip {
    flex: none;
  }
  .flt-count {
    margin-left: 0;
  }
  .tl-item:not(:last-child)::after {
    left: 7px;
    top: 16px;
    bottom: -21px;
  }
  .tl-item {
    grid-template-columns: 24px 1fr;
    padding: 20px 0;
  }
  .tl-date {
    grid-column: 2;
    text-align: left;
    padding-top: 0;
    margin-bottom: 6px;
  }
  .tl-node {
    grid-row: 1 / span 2;
  }
  .tl-node::before {
    left: 2px;
    top: 4px;
  }
  .tl-body h3 {
    font-size: 19px;
  }
  .tl-state {
    font-size: 13px;
  }
  .tl-ops {
    gap: 16px;
  }
  .empty-k {
    font-size: 20px;
  }
}
</style>
