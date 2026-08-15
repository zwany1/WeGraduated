<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="toolbar-note">数据库备份（mysqldump 导出 SQL）。每天凌晨 2 点自动备份，最多保留最近 10 份。</div>
      <el-button v-perm="'system:backup:create'" type="primary" :loading="backing" @click="doBackup">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
        立即备份
      </el-button>
      <el-button type="primary" plain @click="load">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="备份文件" min-width="220">
          <template #default="{ row }"><span class="cell-mono">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="大小" width="120" align="center">
          <template #default="{ row }"><span class="cell-num">{{ fmtSize(row.size) }}</span></template>
        </el-table-column>
        <el-table-column label="备份时间" width="170">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.time) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:backup:list'" link type="primary" size="small" @click="download(row)">下载</el-button>
            <el-button v-perm="'system:backup:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="rows.length === 0 && !loading" description="暂无备份，点击「立即备份」生成" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { backupNow, listBackups, downloadBackup, deleteBackup } from '../../api/admin'
import { downloadBlob } from '../../utils/download'

const rows = ref([])
const loading = ref(false)
const backing = ref(false)

const fmtSize = n => {
  if (!n) return '0 B'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(2) + ' MB'
}
const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function load() {
  loading.value = true
  try {
    rows.value = await listBackups() || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function doBackup() {
  backing.value = true
  try {
    const name = await backupNow()
    ElMessage.success('备份成功：' + name)
    await load()
  } catch (e) {
  } finally {
    backing.value = false
  }
}

async function download(row) {
  try {
    const blob = await downloadBackup(row.name)
    downloadBlob(blob, row.name)
  } catch (e) {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除备份「${row.name}」吗？`, '删除备份', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteBackup(row.name)
    ElMessage.success('已删除')
    await load()
  } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.mgmt { display: flex; flex-direction: column; gap: 18px; animation: mgmt-in 0.35s ease both; }
@keyframes mgmt-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.toolbar { display: flex; align-items: center; gap: 12px; }
.toolbar-note { font-size: 12.5px; color: #8a8d99; flex: 1; }
.table-card { background: #fffdf9; border: 1px solid #e6ded0; border-radius: 14px; padding: 6px 16px 14px; box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04); }
.cell-mono { color: #3a6ea5; font-size: 12.5px; font-family: Consolas, Monaco, monospace; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.cell-num { font-variant-numeric: tabular-nums; color: #4a4f5e; }
</style>
