<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="status-tabs">
        <button v-for="s in statusTabs" :key="s.value" class="status-tab"
          :class="{ active: status === s.value }" @click="switchStatus(s.value)">
          {{ s.label }}
          <span v-if="s.count !== undefined" class="status-count" :style="{ background: s.tint, color: s.color }">{{ s.count }}</span>
        </button>
      </div>
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索用户 / 模板 / 文件名" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="论文文件" min-width="180">
          <template #default="{ row }">
            <div class="cell-file">
              <span class="file-mark">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              </span>
              <span class="file-name" :title="row.originalName">{{ row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交用户" min-width="120">
          <template #default="{ row }"><span class="cell-user-tag">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column label="使用模板" min-width="140">
          <template #default="{ row }"><span class="cell-muted">{{ row.templateName }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="task-tag" :style="tagStyle(row.status)">
              <span class="task-dot" :style="{ background: meta(row.status).color }"></span>
              {{ meta(row.status).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="110">
          <template #default="{ row }">
            <div class="progress-cell">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPct(row) + '%', background: meta(row.status).color }"></div>
              </div>
              <span class="progress-num">{{ progressPct(row) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="160">
          <template #default="{ row }">
            <span v-if="row.errorMsg" class="error-text" :title="row.errorMsg">! {{ truncate(row.errorMsg) }}</span>
            <span v-else class="cell-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="完成时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.finishTime) }}</span></template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview, listTasks } from '../../api/admin'

const STATUS_META = {
  PENDING: { label: '排队中', color: '#9aa3b2', tint: 'rgba(154,163,178,0.12)' },
  PROCESSING: { label: '排版中', color: '#3a6ea5', tint: 'rgba(58,110,165,0.12)' },
  SUCCESS: { label: '已完成', color: '#3f7d5a', tint: 'rgba(63,125,90,0.12)' },
  FAILED: { label: '失败', color: '#b23a2e', tint: 'rgba(178,58,46,0.12)' }
}

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const status = ref('')
const counts = ref({})
const loading = ref(false)

const statusTabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '排队中' },
  { value: 'PROCESSING', label: '排版中' },
  { value: 'SUCCESS', label: '已完成' },
  { value: 'FAILED', label: '失败' }
]

const meta = s => STATUS_META[s] || STATUS_META.PENDING
const tagStyle = s => ({ color: meta(s).color, background: meta(s).tint, borderColor: meta(s).color + '55' })

const progressPct = row => {
  if (row.status === 'SUCCESS') return 100
  if (row.status === 'FAILED') return row.progress || 0
  return row.progress || 0
}

const truncate = s => (s && s.length > 26 ? s.slice(0, 26) + '…' : s || '')
const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function switchStatus(v) {
  status.value = v
  load(1)
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listTasks({
      page: page.value, size: size.value,
      status: status.value || undefined,
      keyword: keyword.value || undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const s = await getOverview()
    counts.value = s.taskStatus || {}
  } catch (e) {}
  load()
})
</script>

<style scoped>
.mgmt {
  --serif: 'Songti SC', 'STSong', 'SimSun', serif;
  display: flex;
  flex-direction: column;
  gap: 18px;
  animation: mgmt-in 0.35s ease both;
}
@keyframes mgmt-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: none; }
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.status-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #f0eadd;
  border: 1px solid #e6ded0;
  border-radius: 10px;
}
.status-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: none;
  background: transparent;
  border-radius: 7px;
  font-size: 13px;
  color: #6b6f7d;
  cursor: pointer;
  transition: all 0.2s;
}
.status-tab:hover {
  color: #2c3140;
}
.status-tab.active {
  background: #fffdf9;
  color: #0d1b2e;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(13, 27, 46, 0.1);
}
.status-count {
  font-size: 10.5px;
  padding: 1px 7px;
  border-radius: 10px;
  font-variant-numeric: tabular-nums;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 240px;
  max-width: 340px;
  padding: 9px 14px;
  background: #fffdf9;
  border: 1px solid #e6ded0;
  border-radius: 9px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-box:focus-within {
  border-color: #3a6ea5;
  box-shadow: 0 0 0 3px rgba(58, 110, 165, 0.12);
}
.search-box input {
  border: none;
  outline: none;
  flex: 1;
  background: transparent;
  font-size: 13.5px;
  color: #2c3140;
}
.search-box input::placeholder {
  color: #b3a583;
}
.table-card {
  background: #fffdf9;
  border: 1px solid #e6ded0;
  border-radius: 14px;
  padding: 6px 16px 14px;
  box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04);
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}
.cell-file {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}
.file-mark {
  width: 27px;
  height: 27px;
  border-radius: 7px;
  background: rgba(58, 110, 165, 0.1);
  color: #3a6ea5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-name {
  font-size: 13px;
  color: #2c3140;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cell-user-tag {
  font-size: 12.5px;
  color: #4a4f5e;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
  border-radius: 5px;
  padding: 2px 9px;
}
.cell-muted {
  color: #8a8d99;
  font-size: 12.5px;
}
.task-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  border: 1px solid transparent;
}
.task-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: #f0eadd;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}
.progress-num {
  font-size: 11.5px;
  color: #6b6f7d;
  font-variant-numeric: tabular-nums;
  width: 32px;
  text-align: right;
}
.error-text {
  font-size: 12px;
  color: #b23a2e;
}
</style>
