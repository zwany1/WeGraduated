<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索用户名 / IP" @keyup.enter="load(1)" />
      </div>
      <el-select v-model="status" placeholder="结果" clearable style="width: 110px" @change="load(1)">
        <el-option label="成功" :value="true" />
        <el-option label="失败" :value="false" />
      </el-select>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:log:login'" type="danger" plain @click="clearAll">清空</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="130">
          <template #default="{ row }"><span class="cell-name">{{ row.username || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" min-width="130" />
        <el-table-column label="客户端" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><span class="cell-muted">{{ row.userAgent || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === false ? 'off' : 'on'">{{ row.status === false ? '失败' : '成功' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="200" show-overflow-tooltip>
          <template #default="{ row }"><span class="cell-muted">{{ row.message || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listLoginLogs, deleteLoginLogs } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const status = ref(null)
const loading = ref(false)

const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listLoginLogs({ page: page.value, size: size.value, keyword: keyword.value || undefined, status: status.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空全部登录日志吗？', '清空日志', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const ids = rows.value.map(r => r.id)
    if (ids.length) await deleteLoginLogs(ids)
    ElMessage.success('已清空')
    await load(1)
  } catch (e) {}
}

onMounted(() => load())
</script>

<style scoped>
.mgmt { display: flex; flex-direction: column; gap: 18px; animation: mgmt-in 0.35s ease both; }
@keyframes mgmt-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.toolbar { display: flex; align-items: center; gap: 12px; }
.search-box { display: flex; align-items: center; gap: 8px; width: 300px; padding: 9px 14px; background: #fffdf9; border: 1px solid #e6ded0; border-radius: 9px; }
.search-box:focus-within { border-color: #3a6ea5; box-shadow: 0 0 0 3px rgba(58, 110, 165, 0.12); }
.search-box input { border: none; outline: none; flex: 1; background: transparent; font-size: 13.5px; color: #2c3140; }
.search-box input::placeholder { color: #b3a583; }
.table-card { background: #fffdf9; border: 1px solid #e6ded0; border-radius: 14px; padding: 6px 16px 14px; box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04); }
.pager { display: flex; justify-content: flex-end; padding-top: 14px; }
.cell-name { font-size: 13px; color: #2c3140; font-weight: 500; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #b23a2e; background: rgba(178, 58, 46, 0.08); border: 1px solid rgba(178, 58, 46, 0.3); }
</style>
