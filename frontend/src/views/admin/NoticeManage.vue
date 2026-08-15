<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索标题" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:notice:add'" type="primary" @click="openCreate()">新增公告</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }"><span class="cell-title">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.noticeType === '2' ? '公告' : '通知' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === false ? 'off' : 'on'">{{ row.status === false ? '停用' : '发布' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip>
          <template #default="{ row }"><span class="cell-muted">{{ row.content || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:notice:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:notice:delete'" link type="danger" size="small" @click="removeNotice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="560px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.noticeType">
            <el-radio value="1">通知</el-radio>
            <el-radio value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="true">发布</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNotices, saveNotice, deleteNotice } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

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
    const data = await listNotices({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { title: '', content: '', noticeType: '1', status: true }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function save() {
  try { await formRef.value.validate() } catch (e) { return }
  saving.value = true
  try {
    await saveNotice(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeNotice(row) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteNotice(row.id)
    ElMessage.success('已删除')
    await load()
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
.cell-title { font-size: 13.5px; color: #2c3140; font-weight: 500; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.type-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; color: #3a6ea5; background: rgba(58, 110, 165, 0.1); border: 1px solid rgba(58, 110, 165, 0.35); }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #b23a2e; background: rgba(178, 58, 46, 0.08); border: 1px solid rgba(178, 58, 46, 0.3); }
</style>
