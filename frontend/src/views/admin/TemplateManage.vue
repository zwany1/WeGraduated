<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索模板名称" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <span class="toolbar-note">共 {{ total }} 个格式模板 · 删除将连同其格式规则一并移除</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="模板名称" min-width="190">
          <template #default="{ row }">
            <div class="cell-tpl">
              <span class="tpl-mark">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/></svg>
              </span>
              <span class="tpl-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属用户" min-width="140">
          <template #default="{ row }">
            <span class="cell-user-tag">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="规则数" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.ruleCount }}</span></template>
        </el-table-column>
        <el-table-column label="被引用任务" width="100" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.taskCount }}</span></template>
        </el-table-column>
        <el-table-column label="生成目录" width="100" align="center">
          <template #default="{ row }">
            <span class="toc" :class="{ on: row.generateToc }">{{ row.generateToc ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.updateTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain type="danger" @click="removeTpl(row)">删除</el-button>
          </template>
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
import { listTemplates, deleteTemplate } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listTemplates({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function removeTpl(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除模板「${row.name}」吗？其关联的 ${row.ruleCount} 条格式规则将一并删除，且不可恢复！`,
      '删除模板',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    load()
  } catch (e) {}
}

onMounted(() => load())
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
  gap: 12px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 300px;
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
.toolbar-note {
  margin-left: auto;
  font-size: 12px;
  color: #9a917d;
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
.cell-tpl {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tpl-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(201, 164, 92, 0.16);
  color: #b08a3e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tpl-name {
  font-size: 13.5px;
  color: #2c3140;
  font-weight: 500;
}
.cell-user-tag {
  font-size: 12.5px;
  color: #4a4f5e;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
  border-radius: 5px;
  padding: 2px 9px;
}
.cell-num {
  font-variant-numeric: tabular-nums;
  color: #4a4f5e;
  font-weight: 600;
}
.cell-muted {
  color: #8a8d99;
  font-size: 12.5px;
}
.toc {
  display: inline-block;
  font-size: 11.5px;
  padding: 2px 9px;
  border-radius: 4px;
  color: #8a8d99;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
}
.toc.on {
  color: #3f7d5a;
  background: rgba(63, 125, 90, 0.12);
  border: 1px solid rgba(63, 125, 90, 0.35);
}
</style>
