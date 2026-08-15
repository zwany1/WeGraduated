<template>
  <div class="mgmt dict-layout">
    <!-- 左侧: 字典类型 -->
    <div class="dict-pane">
      <div class="pane-head">
        <span class="pane-title">字典类型</span>
        <el-button v-perm="'system:dict:add'" size="small" type="primary" @click="openType(null)">新增</el-button>
      </div>
      <div class="type-list" v-loading="typeLoading">
        <div v-for="t in types" :key="t.id" class="type-item" :class="{ active: currentType === t.dictType }"
          @click="selectType(t)">
          <span class="ti-name">{{ t.dictName }}</span>
          <span class="ti-key">{{ t.dictType }}</span>
          <span class="ti-ops">
            <el-button v-perm="'system:dict:edit'" link type="primary" size="small" @click.stop="openType(t)">编辑</el-button>
            <el-button v-perm="'system:dict:delete'" link type="danger" size="small" @click.stop="removeType(t)">删除</el-button>
          </span>
        </div>
        <el-empty v-if="types.length === 0" description="暂无字典类型" :image-size="60" />
      </div>
    </div>

    <!-- 右侧: 字典数据 -->
    <div class="dict-pane data">
      <div class="pane-head">
        <span class="pane-title">{{ currentType || '选择字典类型' }}</span>
        <el-button v-if="currentType" v-perm="'system:dict:add'" size="small" type="primary" @click="openData(null)">新增数据</el-button>
      </div>
      <div class="table-card" v-if="currentType">
        <el-table :data="dataRows" v-loading="dataLoading" stripe>
          <el-table-column prop="dictLabel" label="标签" min-width="130" />
          <el-table-column prop="dictValue" label="键值" min-width="130" />
          <el-table-column prop="dictSort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <span class="status-tag" :class="row.status === false ? 'off' : 'on'">{{ row.status === false ? '停用' : '正常' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <span class="op-cell">
                <el-button v-perm="'system:dict:edit'" link type="primary" size="small" @click="openData(row)">编辑</el-button>
                <el-button v-perm="'system:dict:delete'" link type="danger" size="small" @click="removeData(row)">删除</el-button>
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeVisible" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" width="440px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="80px">
        <el-form-item label="类型名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：论文类型" />
        </el-form-item>
        <el-form-item label="类型标识" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：thesis_type" :disabled="!!typeForm.id" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="true">正常</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataVisible" :title="dataForm.id ? '编辑字典数据' : '新增字典数据'" width="440px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="80px">
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="如：毕业论文" />
        </el-form-item>
        <el-form-item label="键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="如：毕业论文" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="true">正常</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDictTypes, saveDictType, deleteDictType, listDictData, saveDictData, deleteDictData } from '../../api/admin'

const types = ref([])
const currentType = ref('')
const typeLoading = ref(false)
const dataRows = ref([])
const dataLoading = ref(false)
const saving = ref(false)

const typeVisible = ref(false)
const dataVisible = ref(false)
const typeFormRef = ref()
const dataFormRef = ref()
const typeForm = ref({})
const dataForm = ref({})
const typeRules = {
  dictName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入类型标识', trigger: 'blur' }]
}
const dataRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入键值', trigger: 'blur' }]
}

async function loadTypes() {
  typeLoading.value = true
  try {
    const data = await listDictTypes({ page: 1, size: 200 })
    types.value = data.records || []
    if (!currentType.value && types.value.length) {
      selectType(types.value[0])
    }
  } catch (e) {
  } finally {
    typeLoading.value = false
  }
}

function selectType(t) {
  currentType.value = t.dictType
  loadData()
}

async function loadData() {
  if (!currentType.value) return
  dataLoading.value = true
  try {
    dataRows.value = await listDictData(currentType.value)
  } catch (e) {
  } finally {
    dataLoading.value = false
  }
}

function openType(row) {
  typeForm.value = row ? { ...row } : { dictName: '', dictType: '', status: true, remark: '' }
  typeVisible.value = true
  nextTick(() => typeFormRef.value?.clearValidate())
}

async function saveType() {
  try { await typeFormRef.value.validate() } catch (e) { return }
  saving.value = true
  try {
    await saveDictType(typeForm.value)
    ElMessage.success('保存成功')
    typeVisible.value = false
    await loadTypes()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeType(row) {
  try {
    await ElMessageBox.confirm(`确定删除字典「${row.dictName}」及其数据吗？`, '删除字典', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteDictType(row.id)
    ElMessage.success('已删除')
    if (currentType.value === row.dictType) { currentType.value = ''; dataRows.value = [] }
    await loadTypes()
  } catch (e) {}
}

function openData(row) {
  dataForm.value = row ? { ...row } : { dictType: currentType.value, dictLabel: '', dictValue: '', dictSort: 0, status: true }
  dataVisible.value = true
  nextTick(() => dataFormRef.value?.clearValidate())
}

async function saveData() {
  try { await dataFormRef.value.validate() } catch (e) { return }
  saving.value = true
  try {
    await saveDictData(dataForm.value)
    ElMessage.success('保存成功')
    dataVisible.value = false
    await loadData()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeData(row) {
  try {
    await ElMessageBox.confirm(`确定删除字典数据「${row.dictLabel}」吗？`, '删除', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteDictData(row.id)
    ElMessage.success('已删除')
    await loadData()
  } catch (e) {}
}

onMounted(loadTypes)
</script>

<style scoped>
.mgmt { display: flex; flex-direction: column; gap: 18px; animation: mgmt-in 0.35s ease both; }
@keyframes mgmt-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.dict-layout { display: grid; grid-template-columns: 320px 1fr; gap: 18px; align-items: start; }
.dict-pane { background: #fffdf9; border: 1px solid #e6ded0; border-radius: 14px; padding: 16px; box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04); }
.pane-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.pane-title { font-size: 14px; font-weight: 600; color: #0d1b2e; }
.type-list { display: flex; flex-direction: column; gap: 6px; min-height: 200px; }
.type-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; border: 1px solid #efe8dc; cursor: pointer; transition: all 0.2s; }
.type-item:hover { border-color: #d6cdbb; }
.type-item.active { border-color: rgba(58, 110, 165, 0.5); background: rgba(58, 110, 165, 0.06); }
.ti-name {
  font-size: 13px;
  color: #2c3140;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ti-key {
  font-size: 11.5px;
  color: #b3a583;
  flex-shrink: 0;
  white-space: nowrap;
}
.ti-ops {
  margin-left: auto;
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}
/* 表格操作列: 编辑/删除强制同一行, 不因列宽不足而换行 */
.op-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.type-item:hover .ti-ops { opacity: 1; }
.table-card { border: none; padding: 0; box-shadow: none; }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #b23a2e; background: rgba(178, 58, 46, 0.08); border: 1px solid rgba(178, 58, 46, 0.3); }
</style>
