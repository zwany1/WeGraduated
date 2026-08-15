<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索模板名称" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <span class="toolbar-note">将优质模板上架到前台模板市场，供所有用户一键复制使用</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="模板名称" min-width="180">
          <template #default="{ row }"><span class="cell-name">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="所属用户" min-width="130">
          <template #default="{ row }"><span class="cell-user-tag">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column label="规则数" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.ruleCount }}</span></template>
        </el-table-column>
        <el-table-column label="被引用" width="90" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.taskCount }}</span></template>
        </el-table-column>
        <el-table-column label="推荐" width="80" align="center">
          <template #default="{ row }">
            <span class="tag-rec" :class="{ on: row.recommended }">{{ row.recommended ? '推荐' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.isPublic ? 'on' : 'off'">{{ row.isPublic ? '已上架' : '未上架' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.publicTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:market:edit'" size="small" plain
              :type="row.isPublic ? 'warning' : 'primary'" @click="togglePublic(row)">
              {{ row.isPublic ? '下架' : '上架' }}
            </el-button>
            <el-button v-if="row.isPublic" v-perm="'system:market:edit'" size="small" plain
              :type="row.recommended ? 'danger' : 'success'" @click="toggleRecommended(row)">
              {{ row.recommended ? '取消推荐' : '推荐' }}
            </el-button>
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
import { ElMessage } from 'element-plus'
import { listMarketTemplates, setMarketTemplate } from '../../api/admin'

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
    const data = await listMarketTemplates({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function togglePublic(row) {
  try {
    await setMarketTemplate(row.id, { isPublic: !row.isPublic })
    ElMessage.success(row.isPublic ? '已下架' : '已上架到模板市场')
    await load()
  } catch (e) {}
}

async function toggleRecommended(row) {
  try {
    await setMarketTemplate(row.id, { recommended: !row.recommended })
    ElMessage.success(row.recommended ? '已取消推荐' : '已设为推荐')
    await load()
  } catch (e) {}
}

onMounted(() => load())
</script>

<style scoped>
.mgmt { display: flex; flex-direction: column; gap: 18px; animation: mgmt-in 0.35s ease both; }
@keyframes mgmt-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.toolbar { display: flex; align-items: center; gap: 12px; }
.search-box { display: flex; align-items: center; gap: 8px; width: 280px; padding: 9px 14px; background: #fffdf9; border: 1px solid #e6ded0; border-radius: 9px; }
.search-box:focus-within { border-color: #3a6ea5; box-shadow: 0 0 0 3px rgba(58, 110, 165, 0.12); }
.search-box input { border: none; outline: none; flex: 1; background: transparent; font-size: 13.5px; color: #2c3140; }
.search-box input::placeholder { color: #b3a583; }
.toolbar-note { margin-left: auto; font-size: 12px; color: #9a917d; }
.table-card { background: #fffdf9; border: 1px solid #e6ded0; border-radius: 14px; padding: 6px 16px 14px; box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04); }
.pager { display: flex; justify-content: flex-end; padding-top: 14px; }
.cell-name { font-size: 13.5px; color: #2c3140; font-weight: 500; }
.cell-user-tag { display: inline-block; font-size: 12px; color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; padding: 2px 8px; border-radius: 4px; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.cell-num { font-variant-numeric: tabular-nums; color: #4a4f5e; }
.tag-rec { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; color: #8a6a25; background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.tag-rec.on { color: #b23a2e; background: rgba(178, 58, 46, 0.08); border: 1px solid rgba(178, 58, 46, 0.3); }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; }
</style>
