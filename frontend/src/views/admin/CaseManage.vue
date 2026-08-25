<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索标题" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:case:add'" type="primary" @click="openCreate()">新增案例</el-button>
      <span class="toolbar-note">从真实排版任务精选上架, 或手写示范案例兜底</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">
            <span class="src-tag" :class="row.sourceType">{{ row.sourceType === 'real' ? '真实' : '示范' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="110">
          <template #default="{ row }"><span class="cell-tag">{{ row.tag || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }"><span class="cell-title">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="用户" width="110">
          <template #default="{ row }"><span class="cell-muted">{{ userName(row) }}</span></template>
        </el-table-column>
        <el-table-column label="模板" width="130">
          <template #default="{ row }"><span class="cell-muted">{{ tplName(row) }}</span></template>
        </el-table-column>
        <el-table-column label="指标" min-width="150">
          <template #default="{ row }"><span class="cell-num">{{ metricsText(row) }}</span></template>
        </el-table-column>
        <el-table-column label="排序" width="70" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.sortOrder }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.visible ? 'on' : 'off'">{{ row.visible ? '展示' : '隐藏' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:case:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:case:edit'" link size="small"
              :type="row.visible ? 'warning' : 'success'" @click="toggleVisible(row)">
              {{ row.visible ? '隐藏' : '展示' }}
            </el-button>
            <el-button v-perm="'system:case:delete'" link type="danger" size="small" @click="removeCase(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑案例' : '新增案例'" width="680px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="来源任务">
          <el-select v-model="form.taskId" clearable filterable placeholder="选择真实排版任务(留空=手写示范)" style="width:100%" @change="onTaskChange">
            <el-option v-for="t in candidates" :key="t.id" :value="t.id"
              :label="`${t.username || '匿名'} · ${t.templateName || '—'} · ${t.originalName || '—'}${t.minutes != null ? ' · ' + t.minutes + '分钟' : ''}`" />
          </el-select>
        </el-form-item>

        <div v-if="isReal" class="derived">
          <div><span class="d-label">用户</span>{{ selectedTask?.username || '—' }}</div>
          <div><span class="d-label">模板</span>{{ selectedTask?.templateName || '—' }}</div>
          <div><span class="d-label">文档</span>{{ selectedTask?.originalName || '—' }}</div>
          <div><span class="d-label">耗时</span>{{ selectedTask?.minutes != null ? selectedTask.minutes + '分钟' : '—' }}</div>
        </div>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="案例标题" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="标签">
            <el-input v-model="form.tag" placeholder="如：本科毕业论文 / 三线表 / ER图" />
          </el-form-item>
          <el-form-item label="预览色">
            <el-select v-model="form.color">
              <el-option label="蓝" value="blue" />
              <el-option label="绿" value="green" />
              <el-option label="紫" value="purple" />
              <el-option label="橙" value="orange" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="卡片简述" />
        </el-form-item>
        <div class="form-row form-row-3">
          <el-form-item label="页数"><el-input-number v-model="form.mPages" :min="0" controls-position="right" /></el-form-item>
          <el-form-item label="匹配率%"><el-input-number v-model="form.mMatchRate" :min="0" :max="100" controls-position="right" /></el-form-item>
          <el-form-item label="评分"><el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" :precision="1" /></el-form-item>
        </div>
        <el-form-item label="详情正文">
          <el-input v-model="form.detail" type="textarea" :rows="8" placeholder="案例详情正文，支持换行" />
        </el-form-item>

        <template v-if="!isReal">
          <el-form-item label="截图">
            <div class="img-up" @paste="onPaste" tabindex="0">
              <div class="img-list">
                <div v-for="(img, i) in form.imageList" :key="i" class="img-item">
                  <img :src="img" />
                  <span class="img-del" @click="form.imageList.splice(i, 1)">×</span>
                </div>
                <label class="img-add" v-if="(form.imageList?.length || 0) < 6">
                  <input type="file" accept="image/*" multiple @change="pickImages($event)" />
                  <span>+ 添加</span>
                </label>
              </div>
              <p class="img-tip">选择或粘贴图片, 单张 ≤ 2MB, 最多 6 张</p>
            </div>
          </el-form-item>
          <div class="form-row">
            <el-form-item label="署名">
              <el-input v-model="form.author" placeholder="案例署名(留空则匿名)" />
            </el-form-item>
            <el-form-item label="学校">
              <el-input v-model="form.school" placeholder="学校" />
            </el-form-item>
          </div>
          <el-form-item label="关联模板">
            <el-input-number v-model="form.templateId" :min="1" placeholder="选填, 公开模板可被试用" />
          </el-form-item>
        </template>

        <div class="form-row">
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
          <el-form-item label="展示"><el-switch v-model="form.visible" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCases, saveCase, deleteCase, listCandidateTasks } from '../../api/admin'

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
const candidates = ref([])

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const isReal = computed(() => form.value.taskId != null)
const selectedTask = computed(() => candidates.value.find(t => t.id === form.value.taskId) || null)

function parseJson(s, fb) {
  try { return s ? JSON.parse(s) : fb } catch (e) { return fb }
}

function metricsText(row) {
  const out = []
  if (row.minutes != null) out.push(row.minutes + '分钟')
  const m = parseJson(row.metrics, {})
  if (m.pages != null) out.push(m.pages + '页')
  if (m.matchRate != null) out.push(m.matchRate + '%匹配')
  return out.length ? out.join(' · ') : '—'
}

function userName(row) {
  return row.sourceType === 'real' ? (row.username || '—') : (row.author || '—')
}

function tplName(row) {
  return row.sourceType === 'real' ? (row.templateName || '—') : '—'
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listCases({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function loadCandidates() {
  try {
    candidates.value = await listCandidateTasks()
  } catch (e) {}
}

function openCreate() {
  form.value = {
    title: '', tag: '', description: '', color: 'blue', author: '', school: '',
    rating: 5, sortOrder: 0, visible: true,
    taskId: null, mPages: null, mMatchRate: null,
    detail: '', imageList: [], templateId: null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function onTaskChange() {
  const t = selectedTask.value
  if (t && !form.value.title) {
    form.value.title = t.originalName || ''
  }
}

function openEdit(row) {
  const m = parseJson(row.metrics, {})
  const imgs = parseJson(row.images, [])
  form.value = {
    ...row,
    mPages: m.pages ?? null,
    mMatchRate: m.matchRate ?? null,
    imageList: imgs
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function save() {
  try { await formRef.value.validate() } catch (e) { return }
  const f = { ...form.value }
  f.metrics = JSON.stringify({ pages: f.mPages ?? null, matchRate: f.mMatchRate ?? null })
  if (f.taskId != null) {
    f.images = null
    f.templateId = null
  } else {
    f.images = f.imageList && f.imageList.length ? JSON.stringify(f.imageList) : null
  }
  delete f.mPages
  delete f.mMatchRate
  delete f.imageList
  delete f.username
  delete f.templateName
  delete f.originalName
  delete f.minutes
  delete f.publicTemplateId
  delete f.hasDoc
  delete f.sourceType
  saving.value = true
  try {
    await saveCase(f)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

function pickImages(e) {
  const files = e.target.files
  if (!files || !files.length) return
  const remain = 6 - (form.value.imageList?.length || 0)
  if (remain <= 0) {
    ElMessage.warning('最多6张')
    e.target.value = ''
    return
  }
  const list = Array.from(files).slice(0, remain)
  let done = 0
  list.forEach(file => {
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过2MB，已跳过`)
      done++
      if (done >= list.length) e.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      form.value.imageList.push(reader.result)
      done++
      if (done >= list.length) e.target.value = ''
    }
    reader.onerror = () => {
      done++
      if (done >= list.length) e.target.value = ''
    }
    reader.readAsDataURL(file)
  })
}

function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (!file) continue
      if ((form.value.imageList?.length || 0) >= 6) {
        ElMessage.warning('最多6张')
        return
      }
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.warning('图片超过2MB，请压缩后粘贴')
        continue
      }
      const reader = new FileReader()
      reader.onload = () => form.value.imageList.push(reader.result)
      reader.readAsDataURL(file)
    }
  }
}

async function toggleVisible(row) {
  try {
    await saveCase({ id: row.id, visible: !row.visible })
    ElMessage.success(row.visible ? '已隐藏' : '已展示')
    await load()
  } catch (e) {}
}

async function removeCase(row) {
  try {
    await ElMessageBox.confirm(`确定删除案例「${row.title}」吗？`, '删除', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteCase(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {}
}

onMounted(() => {
  load()
  loadCandidates()
})
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
.cell-title { font-size: 13.5px; color: #2c3140; font-weight: 500; }
.cell-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; color: #3a6ea5; background: rgba(58, 110, 165, 0.1); border: 1px solid rgba(58, 110, 165, 0.35); }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.cell-num { font-variant-numeric: tabular-nums; color: #4a4f5e; font-size: 12.5px; }
.src-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.src-tag.real { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.src-tag.manual { color: #8a6a25; background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; }
.derived { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; background: #f4f0e6; border: 1px solid #e6ded0; border-radius: 10px; padding: 12px 16px; margin-bottom: 14px; font-size: 12.5px; color: #4a4f5e; }
.derived .d-label { color: #9a917d; margin-right: 8px; }
.form-row { display: flex; gap: 16px; }
.form-row-3 .el-form-item { flex: 1; min-width: 0; }
.form-row .el-form-item { flex: 1; }
.img-up { outline: none; }
.img-list { display: flex; gap: 8px; flex-wrap: wrap; }
.img-item { position: relative; width: 88px; height: 88px; border: 1px solid #e6ded0; border-radius: 8px; overflow: hidden; background: #f4f0e6; }
.img-item img { width: 100%; height: 100%; object-fit: cover; }
.img-del { position: absolute; top: 2px; right: 4px; cursor: pointer; color: #fff; background: rgba(0,0,0,0.55); width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; line-height: 1; }
.img-add { width: 88px; height: 88px; border: 1px dashed #c9a86a; border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #9a8240; background: #fffdf9; }
.img-add input { display: none; }
.img-tip { margin: 8px 0 0; font-size: 12px; color: #9a917d; }
</style>
