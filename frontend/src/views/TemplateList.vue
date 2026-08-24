<template>
  <div class="page">
    <SiteNav>
      <el-button type="primary" @click="showCreate = true">新建格式方案</el-button>
    </SiteNav>

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
            <el-button size="small" type="danger" plain @click="remove(t.id)">删除</el-button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import SiteNav from '../components/SiteNav.vue'
import { listTemplates, createTemplate, deleteTemplate, cloneTemplate, getMissingRules } from '../api/template'
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

async function remove(id) {
  await ElMessageBox.confirm('确定删除该格式方案？', '提示', { type: 'warning' })
  await deleteTemplate(id)
  ElMessage.success('已删除')
  load()
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
</style>
