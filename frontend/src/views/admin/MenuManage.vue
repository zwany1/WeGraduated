<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="toolbar-note">管理系统侧边栏与按钮权限标识。按钮类型用于后端接口权限校验，格式如 <code>system:user:add</code>。</div>
      <el-button v-perm="'system:menu:add'" type="primary" @click="openCreate(null)">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        新增菜单
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
        :tree-props="{ children: 'children' }" border>
        <el-table-column label="菜单名称" min-width="220">
          <template #default="{ row }">
            <span class="menu-name" :class="'type-' + row.menuType">
              <span v-if="row.menuType === 'F'" class="btn-dot">⚙</span>
              <svg v-else-if="row.menuType === 'M'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#c9a45c" stroke-width="2" stroke-linecap="round"><path d="M3 6h18M3 12h18M3 18h18"/></svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#3a6ea5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              {{ row.menuName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <span class="type-tag" :class="'type-' + row.menuType">
              {{ { M: '目录', C: '菜单', F: '按钮' }[row.menuType] || row.menuType }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="路由地址" min-width="150">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.menuType === 'F' ? '—' : (row.path || '—') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="组件" min-width="170">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.component || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="190">
          <template #default="{ row }">
            <span class="cell-mono">{{ row.perms || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="70" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.orderNum }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === false ? 'off' : 'on'">{{ row.status === false ? '停用' : '正常' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 'F'" v-perm="'system:menu:add'" size="small" plain type="primary" @click="openCreate(row)">新增子项</el-button>
            <el-button v-perm="'system:menu:edit'" size="small" plain type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:menu:delete'" size="small" plain type="danger" @click="removeMenu(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="560px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="parentTreeWithRoot"
            :props="{ value: 'id', label: 'menuName', children: 'children' }"
            check-strictly :render-after-expand="false" default-expand-all placeholder="选择上级菜单(根为系统)" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio-button value="M">目录</el-radio-button>
            <el-radio-button value="C">菜单</el-radio-button>
            <el-radio-button value="F">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="如：用户管理" />
        </el-form-item>
        <template v-if="form.menuType !== 'F'">
          <el-form-item label="路由地址" prop="path">
            <el-input v-model="form.path" placeholder="如：users / system/role（相对 /admin）" />
          </el-form-item>
          <el-form-item label="组件路径">
            <el-input v-model="form.component" placeholder="如：admin/UserManage" />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="form.icon" placeholder="图标标识: chart/doc/document/task/setting/user/role/menu" />
          </el-form-item>
        </template>
        <el-form-item v-if="form.menuType === 'F'" label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如：system:user:add" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '../../api/admin'

const tree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由地址', trigger: 'blur' }],
  perms: [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
}

const parentTree = computed(() => {
  // 目录/菜单可作为父级, 按钮不可; 编辑时排除自身及其子树, 避免形成环
  const excludeId = form.value.id
  const filter = list => (list || [])
    .filter(n => n.menuType !== 'F' && n.id !== excludeId)
    .map(n => ({ ...n, children: filter(n.children) }))
  return filter(tree.value)
})

// 上级菜单数据源: 顶部追加"根目录"虚拟节点(id=0), 使根级菜单显示名称而非数字
const parentTreeWithRoot = computed(() => [
  { id: 0, menuName: '根目录（系统）', menuType: 'M', children: parentTree.value }
])

async function load() {
  loading.value = true
  try {
    tree.value = await getMenuTree()
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function defaultForm(parentId) {
  return {
    id: null,
    parentId: parentId || 0,
    menuName: '',
    menuType: 'C',
    path: '',
    component: '',
    perms: '',
    icon: '',
    orderNum: 0,
    visible: true,
    status: true
  }
}

function openCreate(parent) {
  form.value = defaultForm(parent ? parent.id : 0)
  if (parent) {
    // 父为按钮时不提供新增
    if (parent.menuType === 'F') {
      ElMessage.warning('按钮下不能再新增子项')
      return
    }
    form.value.menuType = parent.menuType === 'M' ? 'C' : 'F'
  }
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
      await updateMenu(form.value)
      ElMessage.success('菜单已更新')
    } else {
      await createMenu(form.value)
      ElMessage.success('菜单已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function removeMenu(row) {
  try {
    await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '删除菜单', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteMenu(row.id)
    ElMessage.success('菜单已删除')
    await load()
  } catch (e) {}
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
.toolbar-note {
  font-size: 12.5px;
  color: #8a8d99;
  flex: 1;
}
.toolbar-note code {
  background: #f3eee3;
  color: #3a6ea5;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.table-card {
  background: #fffdf9;
  border: 1px solid #e6ded0;
  border-radius: 14px;
  padding: 6px 16px 14px;
  box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04);
}
.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: #2c3140;
  font-weight: 500;
}
.menu-name.type-F {
  color: #7a7d8a;
  font-weight: 400;
}
.type-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.04em;
}
.type-tag.type-M {
  color: #8a6a25;
  background: rgba(201, 164, 92, 0.18);
  border: 1px solid rgba(201, 164, 92, 0.45);
}
.type-tag.type-C {
  color: #3a6ea5;
  background: rgba(58, 110, 165, 0.1);
  border: 1px solid rgba(58, 110, 165, 0.35);
}
.type-tag.type-F {
  color: #6b6f7d;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
}
.btn-dot {
  font-size: 12px;
}
.cell-muted {
  color: #8a8d99;
  font-size: 12.5px;
}
.cell-mono {
  color: #3a6ea5;
  font-size: 12px;
  font-family: Consolas, Monaco, monospace;
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
</style>
