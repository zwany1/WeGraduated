<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索参数名称 / 键" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:config:add'" type="primary" @click="openCreate()">新增参数</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configName" label="参数名称" min-width="160" />
        <el-table-column prop="configKey" label="参数键名" min-width="180">
          <template #default="{ row }"><span class="cell-mono">{{ row.configKey }}</span></template>
        </el-table-column>
        <el-table-column prop="configValue" label="参数值" min-width="160">
          <template #default="{ row }"><span class="cell-value">{{ row.configValue }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.configType ? '内置' : '自定义' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="cell-muted">{{ row.remark || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:config:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:config:delete'" link type="danger" size="small" @click="removeConfig(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑参数' : '新增参数'" width="460px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如：上传文件大小上限" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如：upload.max.size" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="form.configValue" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { listConfigs, saveConfig, deleteConfig } from '../../api/admin'

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
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }]
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listConfigs({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { configName: '', configKey: '', configValue: '', configType: false, remark: '' }
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
    await saveConfig(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeConfig(row) {
  if (row.configType) {
    ElMessage.warning('内置参数不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除参数「${row.configName}」吗？`, '删除', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteConfig(row.id)
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
.cell-mono { color: #3a6ea5; font-size: 12px; font-family: Consolas, Monaco, monospace; }
.cell-value { color: #2c3140; font-size: 13px; font-weight: 500; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.type-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; }
</style>
