<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索用户名 / 邮箱 / 昵称" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <span class="toolbar-note">共 {{ total }} 名用户 · 可提升或移除管理员权限</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="cell-user">
              <span class="cell-avatar">{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
              <div class="cell-user-meta">
                <span class="cell-name">{{ row.nickname || row.username }}</span>
                <span class="cell-uname">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.email || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <span v-if="isSelf(row)" class="role-tag self">当前账号</span>
            <span v-else-if="row.role === 'ADMIN'" class="role-tag admin">管理员</span>
            <span v-else class="role-tag user">用户</span>
          </template>
        </el-table-column>
        <el-table-column label="模板" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.templateCount }}</span></template>
        </el-table-column>
        <el-table-column label="任务" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.taskCount }}</span></template>
        </el-table-column>
        <el-table-column label="论文" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.paperCount }}</span></template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="!isSelf(row)">
              <el-button v-if="row.role === 'ADMIN'" size="small" plain type="danger" @click="changeRole(row, 'USER')">取消管理员</el-button>
              <el-button v-else size="small" plain type="primary" @click="changeRole(row, 'ADMIN')">设为管理员</el-button>
              <el-button size="small" plain type="danger" @click="removeUser(row)">删除</el-button>
            </template>
            <span v-else class="cell-muted">—</span>
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
import { listUsers, setUserRole, deleteUser } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const isSelf = row => String(row.id) === String(localStorage.getItem('userId'))

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
    const data = await listUsers({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function changeRole(row, role) {
  const label = role === 'ADMIN' ? '设为管理员' : '取消管理员权限'
  try {
    await ElMessageBox.confirm(`确定将用户「${row.nickname || row.username}」${label}吗？`, '权限变更', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await setUserRole(row.id, role)
    ElMessage.success(role === 'ADMIN' ? '已设为管理员' : '已取消管理员权限')
    load()
  } catch (e) {}
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `将永久删除用户「${row.nickname || row.username}」及其全部模板、任务与论文文件，该操作不可恢复！`,
      '删除用户',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
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
.cell-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cell-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #152a4d, #0d1b2e);
  color: #c9a45c;
  font-family: var(--serif);
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cell-user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.cell-name {
  font-size: 13.5px;
  color: #2c3140;
  font-weight: 500;
}
.cell-uname {
  font-size: 11.5px;
  color: #b3a583;
}
.cell-muted {
  color: #8a8d99;
  font-size: 12.5px;
}
.cell-num {
  font-variant-numeric: tabular-nums;
  color: #4a4f5e;
  font-weight: 600;
}
.role-tag {
  display: inline-block;
  font-size: 11.5px;
  padding: 2px 9px;
  border-radius: 4px;
  letter-spacing: 0.04em;
}
.role-tag.admin {
  color: #8a6a25;
  background: rgba(201, 164, 92, 0.18);
  border: 1px solid rgba(201, 164, 92, 0.45);
}
.role-tag.user {
  color: #6b6f7d;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
}
.role-tag.self {
  color: #3a6ea5;
  background: rgba(58, 110, 165, 0.1);
  border: 1px solid rgba(58, 110, 165, 0.35);
}
</style>
