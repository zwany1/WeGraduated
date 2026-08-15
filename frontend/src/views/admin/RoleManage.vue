<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索角色名称 / 权限字符" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:role:add'" type="primary" @click="openCreate()">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        新增角色
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="角色名称" min-width="140">
          <template #default="{ row }">
            <span class="cell-name">{{ row.roleName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限字符" min-width="140">
          <template #default="{ row }">
            <span class="cell-mono">{{ row.roleKey }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.userCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === false ? 'off' : 'on'">{{ row.status === false ? '停用' : '正常' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:role:assign'" size="small" plain type="primary" @click="openAssign(row)">分配菜单</el-button>
            <el-button v-perm="'system:role:edit'" size="small" plain type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:role:delete'" size="small" plain type="danger" @click="removeRole(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：运营管理员" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey" :disabled="!!form.id">
          <el-input v-model="form.roleKey" placeholder="如：operator" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="true">正常</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="assignVisible" title="分配菜单权限" width="480px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <div class="assign-hint">勾选角色可访问的菜单与可操作的按钮权限。超管角色默认拥有全部权限。</div>
      <el-tree ref="menuTreeRef" v-loading="assignLoading" :data="menuTree" :props="{ label: 'menuName', children: 'children' }"
        show-checkbox node-key="id" default-expand-all :check-strictly="false" class="assign-tree" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoles, createRole, updateRole, deleteRole, getRoleMenus, assignRoleMenus, getMenuTree } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const assignVisible = ref(false)
const saving = ref(false)
const assignLoading = ref(false)
const formRef = ref()
const menuTreeRef = ref()
const menuTree = ref([])
const currentRole = ref(null)
const form = ref({})
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
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
    const data = await listRoles({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { id: null, roleName: '', roleKey: '', remark: '', status: true }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function save() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateRole(form.value)
      ElMessage.success('角色已更新')
    } else {
      await createRole(form.value)
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeRole(row) {
  if (row.roleKey === 'admin' || row.roleKey === 'user') {
    ElMessage.warning('内置角色不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除角色', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    await load()
  } catch (e) {}
}

async function openAssign(row) {
  currentRole.value = row
  if (row.roleKey === 'admin') {
    ElMessage.info('超管角色拥有全部权限，无需分配')
    return
  }
  assignVisible.value = true
  assignLoading.value = true
  menuTree.value = []
  try {
    const [menus, checked] = await Promise.all([getMenuTree(), getRoleMenus(row.id)])
    menuTree.value = menus || []
    await nextTick()
    // 勾选父级时默认连带子级; 使用 setCheckedKeys 前先把半选父级也纳入
    const keys = new Set(checked || [])
    ;(function collectParents(list) {
      for (const m of list || []) {
        if (m.children && m.children.length) {
          collectParents(m.children)
          // 若所有子级都被勾选, 父级也勾选
          const childrenKeys = collectIds(m.children)
          if (childrenKeys.length && childrenKeys.every(k => keys.has(k))) {
            keys.add(m.id)
          }
        }
      }
    })(menuTree.value)
    menuTreeRef.value.setCheckedKeys([...keys])
  } catch (e) {
    menuTree.value = []
  } finally {
    assignLoading.value = false
  }
}

function collectIds(list) {
  const ids = []
  const walk = arr => (arr || []).forEach(n => { ids.push(n.id); walk(n.children) })
  walk(list)
  return ids
}

async function saveAssign() {
  saving.value = true
  try {
    const checked = menuTreeRef.value.getCheckedKeys()
    const half = menuTreeRef.value.getHalfCheckedKeys()
    await assignRoleMenus(currentRole.value.id, [...checked, ...half])
    ElMessage.success('菜单权限已保存')
    assignVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mgmt {
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
  width: 280px;
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
.cell-name {
  font-size: 13.5px;
  color: #2c3140;
  font-weight: 500;
}
.cell-mono {
  color: #3a6ea5;
  font-size: 12px;
  font-family: Consolas, Monaco, monospace;
}
.cell-muted {
  color: #8a8d99;
  font-size: 12.5px;
}
.cell-num {
  font-variant-numeric: tabular-nums;
  color: #4a4f5e;
}
.status-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}
.status-tag.on {
  color: #2e7d4f;
  background: rgba(46, 125, 79, 0.1);
  border: 1px solid rgba(46, 125, 79, 0.3);
}
.status-tag.off {
  color: #b23a2e;
  background: rgba(178, 58, 46, 0.08);
  border: 1px solid rgba(178, 58, 46, 0.3);
}
.assign-hint {
  font-size: 12.5px;
  color: #8a8d99;
  margin-bottom: 12px;
}
.assign-tree {
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #efe8dc;
  border-radius: 8px;
  padding: 8px;
}
</style>
