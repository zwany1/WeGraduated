<template>
  <div class="page">
    <SiteNav>
      <el-button @click="triggerImport">导入方案</el-button>
      <el-button type="primary" @click="showCreate = true">新建格式方案</el-button>
    </SiteNav>
    <input ref="importFileRef" type="file" accept=".json" style="display: none" @change="onImportFile" />

    <main class="content">
      <h2>我的格式方案</h2>
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索方案名称" clearable :prefix-icon="Search" style="width: 240px" />
        <el-select v-model="teamFilter" placeholder="全部归属" clearable style="width: 180px">
          <el-option label="个人" :value="0" />
          <el-option v-for="tm in teams" :key="tm.id" :label="tm.name" :value="tm.id" />
        </el-select>
        <span class="count">共 {{ filteredTemplates.length }} 个方案</span>
      </div>
      <div v-if="filteredTemplates.length === 0" class="empty">
        <p>{{ templates.length === 0 ? '还没有格式方案，点击右上角「新建格式方案」创建' : '没有符合条件的方案' }}</p>
      </div>
      <div class="grid">
        <div v-for="t in pagedTemplates" :key="t.id" class="tpl-card">
          <h3>
            {{ t.name }}
            <span v-if="t.teamId && teamMap[t.teamId]" class="team-badge">{{ teamMap[t.teamId].name }}</span>
            <span v-if="missing[t.id] && missing[t.id].length" class="warn-badge" title="缺少关键规则：{{ missing[t.id].join('、') }}">缺规则</span>
          </h3>
          <p class="time">创建于 {{ formatTime(t.createTime) }}</p>
          <div class="ops">
            <el-button size="small" type="primary" @click="$router.push(`/template/${t.id}`)">配置格式</el-button>
            <el-button size="small" @click="$router.push('/tasks')">使用排版</el-button>
            <el-button size="small" @click="clone(t)">克隆</el-button>
            <el-button size="small" @click="exportConfig(t)" title="导出为 JSON 文件, 同学可直接导入">导出</el-button>
            <el-button size="small" :type="compareA && compareA.id === t.id ? 'warning' : ''" plain @click="markCompare(t)">
              {{ compareA && compareA.id === t.id ? '对比基准' : '对比' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="remove(t)">删除</el-button>
          </div>
        </div>
      </div>
      <el-pagination v-if="filteredTemplates.length > pageSize" v-model:current-page="page" :page-size="pageSize" :total="filteredTemplates.length" layout="prev, pager, next" class="pager" />
    </main>

    <el-dialog v-model="showCreate" title="新建格式方案" width="420px">
      <el-input v-model="newName" placeholder="请输入格式方案名称，如：我的毕业论文格式" />
      <el-select v-model="newTeamId" placeholder="归属（默认个人）" clearable style="width: 100%; margin-top: 12px">
        <el-option v-for="tm in teams" :key="tm.id" :label="`团队：${tm.name}`" :value="tm.id" />
      </el-select>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="diffVisible" title="模板配置对比" width="780px" top="6vh">
      <p class="diff-sub" v-if="compareA && compareB">{{ compareA.name }} ↔ {{ compareB.name }}（仅列出不同的参数）</p>
      <el-table v-if="diffRows.length" :data="diffRows" size="small" max-height="440">
        <el-table-column prop="group" label="配置组" width="110" />
        <el-table-column prop="key" label="参数" width="180" />
        <el-table-column prop="a" label="基准方案" min-width="160" show-overflow-tooltip />
        <el-table-column prop="b" label="对比方案" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="两个方案的配置完全一致" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import SiteNav from '../components/SiteNav.vue'
import { listTemplates, createTemplate, deleteTemplate, cloneTemplate, getMissingRules, getTemplateDetail, saveAllConfig } from '../api/template'
import { listTeams } from '../api/team'

const router = useRouter()
const templates = ref([])
const keyword = ref('')
const teamFilter = ref(null)
const page = ref(1)
const pageSize = 9

const filteredTemplates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return templates.value.filter(t => {
    if (kw && !(t.name || '').toLowerCase().includes(kw)) return false
    if (teamFilter.value === null || teamFilter.value === undefined) return true
    if (teamFilter.value === 0) return !t.teamId
    return t.teamId === teamFilter.value
  })
})
const pagedTemplates = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredTemplates.value.slice(start, start + pageSize)
})
watch([keyword, teamFilter], () => { page.value = 1 })
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
    teams.value = await listTeams() || []
    const m = {}
    teams.value.forEach(t => { m[t.id] = t })
    teamMap.value = m
  } catch (e) {}
})

async function load() {
  templates.value = await listTemplates()
  // 并行检查每个模板的规则完整性
  missing.value = {}
  for (const t of templates.value) {
    try {
      const m = await getMissingRules(t.id)
      if (m && m.length) missing.value[t.id] = m
    } catch (e) {}
  }
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
    await ElMessageBox.confirm(`确定删除「${t.name}」？其全部格式规则将一并删除。`, '删除格式方案', { type: 'warning', confirmButtonText: '删除' })
  } catch (e) { return }
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
  const pg = (() => { try { return JSON.parse(t.pageConfig || '{}') } catch (e) { return {} } })()
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
  ;(t.rules || []).forEach(r => { rulesMap[r.ruleType] = r })
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
  const rc = (() => { try { return JSON.parse(t.referenceConfig || '{}') } catch (e) { return {} } })()
  m['参考文献/启用'] = rc.enabled ? '是' : '否'
  m['参考文献/条目字体'] = `${rc.itemFont || '—'} ${rc.itemFontSize || ''}`
  const tc = (() => { try { return JSON.parse(t.tocConfig || '{}') } catch (e) { return {} } })()
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

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 40px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.user-chip:hover {
  border-color: #3B6BFF;
  background: #f5f7ff;
}
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.user-avatar-text {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #3B6BFF;
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.user-name {
  font-size: 13px;
  color: #303133;
}
.content {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
}
.content h2 {
  color: #303133;
  margin-bottom: 20px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.filter-bar .count {
  font-size: 13px;
  color: #909399;
}
.pager {
  margin-top: 24px;
  justify-content: center;
  display: flex;
}
.empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  background: #fff;
  border-radius: 10px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.tpl-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.tpl-card h3 {
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.team-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #3B6BFF;
  background: #EEF1FF;
  border-radius: 999px;
  padding: 2px 9px;
}
.warn-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #b45309;
  background: #FEF3C7;
  border-radius: 999px;
  padding: 2px 9px;
}
.tpl-card .time {
  color: #909399;
  font-size: 12px;
  margin-bottom: 16px;
}
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.diff-sub {
  color: #909399;
  font-size: 13px;
  margin: 0 0 10px;
}
</style>
