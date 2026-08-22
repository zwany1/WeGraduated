<template>
  <div class="mgmt">
    <el-tabs v-model="activeTab" class="mgmt-tabs">
      <el-tab-pane label="用户列表" name="users">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索用户名 / 邮箱 / 昵称" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button v-perm="'system:user:export'" type="success" plain @click="doExport">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        导出
      </el-button>
      <el-button v-if="selectedIds.length" v-perm="'system:user:status'" type="danger" plain @click="batchStatus(true)">批量封禁({{ selectedIds.length }})</el-button>
      <el-button v-if="selectedIds.length" v-perm="'system:user:status'" plain @click="batchStatus(false)">批量启用</el-button>
      <span class="toolbar-note">共 {{ total }} 名用户 · 可管理角色、封禁与重置密码</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="cell-user">
              <span class="cell-avatar" @click="openAvatarUpload(row)">
                <img v-if="row.avatar" :src="row.avatar" class="cell-avatar-img" />
                <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
              </span>
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
        <el-table-column label="角色" width="170">
          <template #default="{ row }">
            <span v-if="isSelf(row)" class="role-tag self">当前账号</span>
            <span v-else>
              <span v-if="row.roleNames && row.roleNames.length" class="role-tags">
                <span v-for="(rn, i) in row.roleNames" :key="i" class="role-tag"
                  :class="row.role === 'ADMIN' ? 'admin' : 'user'">{{ rn }}</span>
              </span>
              <span v-else class="role-tag user">用户</span>
            </span>
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
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <template v-if="!isSelf(row)">
              <span class="op-cell">
                <el-button v-perm="'system:user:list'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                <el-button v-perm="'system:user:assign'" link type="primary" size="small" @click="openAssign(row)">角色</el-button>
                <el-button v-if="row.role === 'ADMIN'" v-perm="'system:user:edit'" link type="danger" size="small" @click="changeRole(row, 'USER')">取消管理员</el-button>
                <el-button v-else v-perm="'system:user:edit'" link type="primary" size="small" @click="changeRole(row, 'ADMIN')">设管理员</el-button>
                <el-button v-perm="'system:user:resetPwd'" link type="warning" size="small" @click="openResetPwd(row)">重置密码</el-button>
                <el-button v-perm="'system:user:status'" link :type="row.status === false ? 'success' : 'danger'" size="small" @click="toggleStatus(row)">
                  {{ row.status === false ? '启用' : '封禁' }}
                </el-button>
                <el-button v-perm="'system:user:delete'" link type="danger" size="small" @click="removeUser(row)">删除</el-button>
              </span>
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
      </el-tab-pane>

      <el-tab-pane label="在线会话" name="sessions">
        <div class="table-card">
          <div class="toolbar sess-toolbar">
            <span class="toolbar-note">在线会话 = 未过期且未撤销的登录 · 强制下线立即生效</span>
            <el-button type="primary" plain size="small" @click="loadSessions">刷新</el-button>
          </div>
          <el-table :data="sessions" v-loading="sessionLoading" stripe>
            <el-table-column prop="id" label="会话ID" width="80" />
            <el-table-column label="用户" min-width="140">
              <template #default="{ row }">
            <div class="cell-user">
              <span class="cell-avatar">
                <img v-if="row.avatar" :src="row.avatar" class="cell-avatar-img" />
                <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
              </span>
                  <div class="cell-user-meta">
                    <span class="cell-name">{{ row.username }}</span>
                    <span class="cell-uname">#{{ row.userId }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="登录 IP" width="160">
              <template #default="{ row }"><span class="cell-muted">{{ row.ip || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="登录时间" width="170">
              <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.loginTime) }}</span></template>
            </el-table-column>
            <el-table-column label="最近活跃" width="170">
              <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.lastActive) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'system:user:edit'" link type="danger" size="small" @click="kick(row)">强制下线</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 分配角色 -->
    <el-dialog v-model="assignVisible" :title="`分配角色 - ${assignUser ? (assignUser.nickname || assignUser.username) : ''}`" width="480px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <div class="assign-hint">一个用户可同时拥有多个角色，权限取并集。</div>
      <div v-loading="assignLoading" class="role-box">
        <el-checkbox-group v-model="assignRoleIds" class="role-checkbox">
          <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id" :disabled="r.roleKey === 'admin'">
            <span class="rc-name">{{ r.roleName }}</span>
            <span class="rc-key">{{ r.roleKey }}</span>
          </el-checkbox>
          <el-empty v-if="allRoles.length === 0 && !assignLoading" description="暂无可分配角色" :image-size="60" />
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" :title="`重置密码 - ${pwdUser ? (pwdUser.nickname || pwdUser.username) : ''}`" width="420px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <div class="assign-hint">重置后该用户将自动退出登录（所有 token 失效）。</div>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveResetPwd">确定重置</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情 -->
    <el-dialog v-model="detailVisible" :title="`用户详情 - ${detailUser ? (detailUser.nickname || detailUser.username) : ''}`" width="720px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <div class="dh-name">{{ detail.nickname || detail.username }}
                <span class="role-tag" :class="detail.role === 'ADMIN' ? 'admin' : 'user'">{{ detail.role === 'ADMIN' ? '管理员' : '用户' }}</span>
                <span class="role-tag" :class="detail.status === false ? 'off' : 'on'">{{ detail.status === false ? '已封禁' : '正常' }}</span>
              </div>
              <div class="dh-meta">@{{ detail.username }} · {{ detail.email || '无邮箱' }} · 注册于 {{ fmtTime(detail.createTime) }}</div>
              <div v-if="detail.roleNames && detail.roleNames.length" class="dh-roles">
                <span v-for="(rn, i) in detail.roleNames" :key="i" class="role-tag user">{{ rn }}</span>
              </div>
            </div>
            <div class="dh-stats">
              <div class="stat"><b>{{ detail.templateCount }}</b><span>模板</span></div>
              <div class="stat"><b>{{ detail.taskCount }}</b><span>任务</span></div>
              <div class="stat"><b>{{ detail.paperCount }}</b><span>文件</span></div>
            </div>
          </div>
          <el-tabs v-model="detailTab">
            <el-tab-pane :label="`模板 (${detail.templateCount})`" name="tpl">
              <el-table :data="detail.templates" size="small" max-height="260">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="name" label="模板名称" />
                <el-table-column label="更新时间" width="160">
                  <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.time) }}</span></template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.templates.length" description="无模板" :image-size="50" />
            </el-tab-pane>
            <el-tab-pane :label="`任务 (${detail.taskCount})`" name="task">
              <el-table :data="detail.tasks" size="small" max-height="260">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="name" label="论文文件" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <span class="task-tag" :class="row.status">{{ statusLabel(row.status) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="创建时间" width="160">
                  <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.time) }}</span></template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.tasks.length" description="无任务" :image-size="50" />
            </el-tab-pane>
            <el-tab-pane :label="`论文文件 (${detail.paperCount})`" name="file">
              <el-table :data="detail.papers" size="small" max-height="260">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="name" label="文件名" show-overflow-tooltip />
                <el-table-column label="上传时间" width="160">
                  <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.time) }}</span></template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.papers.length" description="无文件" :image-size="50" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>

    <!-- 换头像弹窗 -->
    <el-dialog v-model="avatarVisible" :title="`更换头像 - ${avatarUser ? (avatarUser.nickname || avatarUser.username) : ''}`" width="420px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <div class="avatar-upload-box">
        <div class="avatar-preview" @click="triggerAvatarPick">
          <img v-if="avatarData" :src="avatarData" class="avatar-preview-img" />
          <span v-else class="avatar-preview-placeholder">
            <img v-if="avatarUser && avatarUser.avatar" :src="avatarUser.avatar" class="avatar-preview-img" />
            <span v-else>{{ (avatarUser ? avatarUser.username : 'U').slice(0, 1).toUpperCase() }}</span>
          </span>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarChange" />
        <p class="avatar-tip">点击头像选择图片（JPG/PNG，≤2MB）</p>
      </div>
      <template #footer>
        <el-button @click="avatarVisible = false">取消</el-button>
        <el-button type="primary" :loading="avatarUploading" @click="saveAvatar">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, setUserRole, assignUserRoles, updateUserStatus, resetUserPassword, batchUserStatus,
  listOnlineSessions, kickSession, updateUserAvatar,
  getUserDetail, deleteUser, listAllRoles, exportUsers } from '../../api/admin'
import { downloadBlob } from '../../utils/download'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const activeTab = ref('users')
const sessions = ref([])
const sessionLoading = ref(false)
const selectedIds = ref([])

function onSelectionChange(sel) {
  selectedIds.value = sel.map(r => r.id)
}

async function batchStatus(disabled) {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定${disabled ? '封禁' : '启用'}选中的 ${selectedIds.value.length} 名用户？${disabled ? '封禁后其登录立即失效。' : ''}`, '批量操作', { type: 'warning' })
  } catch (e) { return }
  try {
    await batchUserStatus(selectedIds.value, disabled)
    ElMessage.success(disabled ? '已批量封禁' : '已批量启用')
    selectedIds.value = []
    load()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function loadSessions() {
  sessionLoading.value = true
  try {
    sessions.value = await listOnlineSessions() || []
  } catch (e) {
  } finally {
    sessionLoading.value = false
  }
}

async function kick(row) {
  try {
    await ElMessageBox.confirm(`确定强制下线「${row.username}」的当前会话？下线后其登录立即失效。`, '强制下线', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await kickSession(row.id)
    ElMessage.success('已强制下线')
    loadSessions()
  } catch (e) {
  }
}

// 头像更换
const avatarVisible = ref(false)
const avatarUser = ref(null)
const avatarData = ref('')
const avatarUploading = ref(false)
const avatarInput = ref(null)

function openAvatarUpload(row) {
  avatarUser.value = row
  avatarData.value = ''
  avatarVisible.value = true
}

function triggerAvatarPick() {
  avatarInput.value && avatarInput.value.click()
}

function onAvatarChange(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 2MB')
    e.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    avatarData.value = reader.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function saveAvatar() {
  if (!avatarUser.value) return
  if (!avatarData.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  avatarUploading.value = true
  try {
    await updateUserAvatar(avatarUser.value.id, avatarData.value)
    ElMessage.success('头像已更新')
    avatarVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    avatarUploading.value = false
  }
}

const assignVisible = ref(false)
const saving = ref(false)
const assignLoading = ref(false)
const assignUser = ref(null)
const assignRoleIds = ref([])
const allRoles = ref([])

const pwdVisible = ref(false)
const pwdUser = ref(null)
const newPassword = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailUser = ref(null)
const detail = ref(null)
const detailTab = ref('tpl')

const isSelf = row => String(row.id) === String(localStorage.getItem('userId'))

const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const statusLabel = s => ({ SUCCESS: '成功', FAILED: '失败', PENDING: '待处理', PROCESSING: '处理中' }[s] || s)

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

async function doExport() {
  try {
    const blob = await exportUsers()
    downloadBlob(blob, '用户报表.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {}
}

async function openAssign(row) {
  assignUser.value = row
  assignRoleIds.value = [...(row.roleIds || [])]
  assignVisible.value = true
  assignLoading.value = true
  try {
    if (allRoles.value.length === 0) {
      allRoles.value = await listAllRoles()
    }
  } catch (e) {
  } finally {
    assignLoading.value = false
  }
}

async function saveAssign() {
  saving.value = true
  try {
    await assignUserRoles(assignUser.value.id, assignRoleIds.value)
    ElMessage.success('角色已更新')
    assignVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

function openResetPwd(row) {
  pwdUser.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function saveResetPwd() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await resetUserPassword(pwdUser.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  detailUser.value = row
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getUserDetail(row.id)
  } catch (e) {
  } finally {
    detailLoading.value = false
  }
}

async function toggleStatus(row) {
  const action = row.status === false ? '启用' : '封禁'
  const msg = row.status === false
    ? `确定启用用户「${row.nickname || row.username}」吗？`
    : `确定封禁用户「${row.nickname || row.username}」吗？封禁后其将无法登录。`
  try {
    await ElMessageBox.confirm(msg, action, { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await updateUserStatus(row.id, row.status === false)
    ElMessage.success(action + '成功')
    await load()
  } catch (e) {}
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

onMounted(() => { load(); loadSessions() })
</script>

<style scoped>
.mgmt {
  --serif: 'Songti SC', 'STSong', 'SimSun', serif;
  display: flex;
  flex-direction: column;
  gap: 18px;
  animation: mgmt-in 0.35s ease both;
}
.mgmt-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 600;
}
.sess-toolbar {
  padding-bottom: 12px;
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
/* 操作列按钮组: 强制同一行, 不因列宽不足而换行 */
.op-cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
.op-cell .el-button + .el-button {
  margin-left: 0;
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
  cursor: pointer;
  transition: box-shadow 0.2s;
  overflow: hidden;
}
.cell-avatar:hover { box-shadow: 0 0 0 2px rgba(201,164,92,0.4); }
.cell-avatar-img { width: 100%; height: 100%; object-fit: cover; }
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
.role-tags {
  display: inline-flex;
  gap: 4px;
  flex-wrap: wrap;
}
.assign-hint {
  font-size: 12.5px;
  color: #8a8d99;
  margin-bottom: 12px;
}
.role-box {
  border: 1px solid #efe8dc;
  border-radius: 10px;
  background: #fffdf9;
  padding: 8px;
  max-height: 46vh;
  overflow-y: auto;
}
.role-box::-webkit-scrollbar {
  width: 8px;
}
.role-box::-webkit-scrollbar-thumb {
  background: #d6cdbb;
  border-radius: 4px;
}
.role-checkbox {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px;
}
.role-checkbox .el-checkbox {
  margin-right: 0;
  height: auto;
  padding: 8px 10px;
  border: 1px solid #efe8dc;
  border-radius: 8px;
  background: #fffdf9;
}
.role-checkbox .el-checkbox.is-checked {
  border-color: rgba(58, 110, 165, 0.5);
  background: rgba(58, 110, 165, 0.06);
}
.rc-name {
  font-size: 13px;
  color: #2c3140;
  font-weight: 500;
}
.rc-key {
  font-size: 11.5px;
  color: #b3a583;
  margin-left: 6px;
}
.detail-body {
  min-height: 200px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 2px 16px;
  border-bottom: 1px solid #efe8dc;
  margin-bottom: 8px;
}
.dh-name {
  font-size: 16px;
  font-weight: 600;
  color: #0d1b2e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dh-meta {
  font-size: 12.5px;
  color: #8a8d99;
  margin-top: 4px;
}
.dh-roles {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}
.dh-stats {
  display: flex;
  gap: 18px;
}
.stat {
  text-align: center;
}
.stat b {
  display: block;
  font-size: 20px;
  color: #0d1b2e;
  font-variant-numeric: tabular-nums;
}
.stat span {
  font-size: 11px;
  color: #8a8d99;
}
.role-tag.off {
  color: #b23a2e;
  background: rgba(178, 58, 46, 0.08);
  border: 1px solid rgba(178, 58, 46, 0.3);
}
.role-tag.on {
  color: #2e7d4f;
  background: rgba(46, 125, 79, 0.1);
  border: 1px solid rgba(46, 125, 79, 0.3);
}
.task-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  color: #6b6f7d;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
}
.task-tag.SUCCESS {
  color: #2e7d4f;
  background: rgba(46, 125, 79, 0.1);
  border: 1px solid rgba(46, 125, 79, 0.3);
}
.task-tag.FAILED {
  color: #b23a2e;
  background: rgba(178, 58, 46, 0.08);
  border: 1px solid rgba(178, 58, 46, 0.3);
}
.task-tag.PROCESSING {
  color: #3a6ea5;
  background: rgba(58, 110, 165, 0.1);
  border: 1px solid rgba(58, 110, 165, 0.35);
}

/* 头像上传弹窗 */
.avatar-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 0;
}
.avatar-preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #152a4d, #0d1b2e);
  color: #c9a45c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #e6ded0;
  transition: box-shadow 0.2s;
}
.avatar-preview:hover { box-shadow: 0 0 0 3px rgba(201,164,92,0.3); }
.avatar-preview-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-preview-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.avatar-tip { font-size: 13px; color: #9a917d; text-align: center; }
</style>
