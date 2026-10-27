<template>
  <WorkbenchShell title="团队">
    <template #meta>
      共 {{ teams.length }} 个团队 · 队长 {{ ownerCount }} 人<br />
      团队内共享格式方案、排版任务与结果
    </template>

    <template #toolbar>
      <div class="flt">
        <label class="flt-search">
          <span class="flt-k">团队名称</span>
          <input v-model="newName" type="text" maxlength="64" placeholder="如：张老师课题组" />
        </label>
        <label class="flt-search wide">
          <span class="flt-k">简介</span>
          <input v-model="newDesc" type="text" maxlength="120" placeholder="可选，一句话说明这个团队在做什么" @keyup.enter="create" />
        </label>
        <button class="wbtn solid" type="button" :disabled="creating" @click="create">
          {{ creating ? '创建中…' : '创建团队' }}
        </button>
        <span class="flt-count">{{ teams.length }} 个团队</span>
      </div>
    </template>

    <section class="teams" ref="createRef">
      <h2 class="sec-h">我的团队</h2>

      <ul v-if="teams.length" class="team-rows">
        <li v-for="t in teams" :key="t.id" class="team-row" @click="openDetail(t)">
          <span class="t-mark">{{ t.name.slice(0, 1).toUpperCase() }}</span>
          <span class="t-main">
            <span class="t-name">{{ t.name }}</span>
            <span class="t-desc">{{ t.description || '暂无简介' }}</span>
          </span>
          <span class="t-role" :class="roleClass(t.role)">{{ roleLabel(t.role) }}</span>
          <span class="t-count">{{ t.memberCount }} 名成员</span>
          <span class="t-go" aria-hidden="true">→</span>
        </li>
      </ul>

      <div v-else class="t-empty">
        <p class="t-empty-k">还没有团队</p>
        <p class="t-empty-d">团队里可以共享格式方案、看到彼此的排版任务与结果；创建后把同学或导师拉进来即可。</p>
        <div class="t-empty-a">
          <button class="wbtn solid" type="button" @click="focusCreate">创建团队</button>
        </div>
      </div>
    </section>

    <el-dialog v-model="dlg" :title="d ? d.name : '团队详情'" width="640px" destroy-on-close>
      <template v-if="d">
        <div class="invite-row" v-if="d.ownerId === meId">
          <el-input v-model="inviteKw" placeholder="输入用户名或邮箱邀请成员" style="flex: 1" @keyup.enter="invite" />
          <el-button type="primary" :loading="inviting" @click="invite">邀请</el-button>
        </div>
        <div v-if="d.pendingInvites && d.pendingInvites.length" class="pending-box">
          <div class="pending-title">待确认邀请（等待对方同意）</div>
          <div v-for="pi in d.pendingInvites" :key="pi.id" class="pending-item">
            <span class="pi-name">{{ pi.nickname || pi.username }}</span>
            <span class="pi-muted">@{{ pi.username }} · {{ fmtTime(pi.inviteTime) }}</span>
          </div>
        </div>
        <el-table :data="d.members" size="small" border class="member-table">
          <el-table-column prop="nickname" label="昵称" min-width="110" />
          <el-table-column prop="username" label="用户名" min-width="110" />
          <el-table-column prop="email" label="邮箱" min-width="170">
            <template #default="{ row }"><span class="muted">{{ row.email || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="角色" width="130" align="center">
            <template #default="{ row }">
              <span v-if="row.role === 'owner'" class="role-tag owner">队长</span>
              <el-select v-else-if="d.ownerId === meId" v-model="row.role" size="small" style="width:100px" @change="setRole(row)">
                <el-option label="管理员" value="admin" />
                <el-option label="编辑" value="editor" />
                <el-option label="查看" value="viewer" />
                <el-option label="成员" value="member" />
              </el-select>
              <span v-else class="role-tag" :class="roleClass(row.role)">{{ roleLabel(row.role) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <template v-if="d.ownerId === meId && row.userId !== d.ownerId">
                <el-button link type="primary" size="small" @click="transfer(row)">转让</el-button>
                <el-button link type="danger" size="small" @click="remove(row)">移除</el-button>
              </template>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button v-if="d && d.ownerId === meId" type="danger" plain @click="del">解散团队</el-button>
        <el-button v-else-if="d" plain @click="leave">退出团队</el-button>
        <el-button @click="dlg = false">关闭</el-button>
      </template>
    </el-dialog>
  </WorkbenchShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkbenchShell from '../components/WorkbenchShell.vue'
import { createTeam, listTeams, getTeamDetail, inviteMember, removeMember, leaveTeam, deleteTeam, setMemberRole, transferOwnership } from '../api/team'
import { getProfile } from '../api/user'

const teams = ref([])
const newName = ref('')
const newDesc = ref('')
const creating = ref(false)
const dlg = ref(false)
const d = ref(null)
const inviteKw = ref('')
const inviting = ref(false)
const meId = ref(null)
const createRef = ref(null)
const ownerCount = computed(() => teams.value.filter(t => t.role === 'owner').length)

function focusCreate() {
  if (createRef.value) createRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function load() {
  try {
    teams.value = await listTeams() || []
  } catch (e) {}
}

async function create() {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入团队名称')
    return
  }
  creating.value = true
  try {
    await createTeam({ name: newName.value.trim(), description: newDesc.value.trim() })
    ElMessage.success('团队创建成功')
    newName.value = ''
    newDesc.value = ''
    load()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function openDetail(t) {
  d.value = null
  dlg.value = true
  try {
    d.value = await getTeamDetail(t.id)
  } catch (e) {
    dlg.value = false
    ElMessage.error(e.message || '加载失败')
  }
}

async function invite() {
  if (!inviteKw.value.trim()) {
    ElMessage.warning('请输入用户名或邮箱')
    return
  }
  inviting.value = true
  try {
    await inviteMember(d.value.id, inviteKw.value.trim())
    ElMessage.success('邀请成功')
    inviteKw.value = ''
    d.value = await getTeamDetail(d.value.id)
    load()
  } catch (e) {
    ElMessage.error(e.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定将「${row.nickname || row.username}」移出团队？`, '移除成员', { type: 'warning' })
  } catch (e) { return }
  try {
    await removeMember(d.value.id, row.userId)
    ElMessage.success('已移除')
    d.value = await getTeamDetail(d.value.id)
    load()
  } catch (e) {}
}

function roleLabel(r) {
  return { owner: '队长', admin: '管理员', editor: '编辑', viewer: '查看', member: '成员' }[r] || '成员'
}
function roleClass(r) {
  return { owner: 'owner', admin: 'admin', editor: 'editor', viewer: 'viewer' }[r] || 'member'
}
async function setRole(row) {
  try {
    await setMemberRole(d.value.id, row.userId, row.role)
    ElMessage.success('角色已更新')
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
    d.value = await getTeamDetail(d.value.id)
  }
}
async function transfer(row) {
  try {
    await ElMessageBox.confirm(`确定将队长转让给「${row.nickname || row.username}」？转让后你将变为管理员。`, '转让所有权', { type: 'warning', confirmButtonText: '转让' })
  } catch (e) { return }
  try {
    await transferOwnership(d.value.id, row.userId)
    ElMessage.success('队长已转让')
    d.value = await getTeamDetail(d.value.id)
    load()
  } catch (e) {}
}

async function leave() {
  try {
    await ElMessageBox.confirm('确定退出该团队？', '退出团队', { type: 'warning' })
  } catch (e) { return }
  try {
    await leaveTeam(d.value.id)
    ElMessage.success('已退出团队')
    dlg.value = false
    load()
  } catch (e) {}
}

async function del() {
  try {
    await ElMessageBox.confirm('解散后团队成员将失去访问权限，团队内模板会归还各创建者。确定解散？', '解散团队', { type: 'warning', confirmButtonText: '解散' })
  } catch (e) { return }
  try {
    await deleteTeam(d.value.id)
    ElMessage.success('团队已解散')
    dlg.value = false
    load()
  } catch (e) {}
}

function fmtTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  load()
  try {
    const p = await getProfile()
    if (p) meId.value = p.userId ?? p.id
  } catch (e) {}
})
</script>

<style scoped>
.page {
  --c-primary: #2F5D46;
  min-height: 100vh;
  background: #f7f8fb;
  font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
.bar {
  display: flex;
  align-items: center;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #1F2E26;
}
.content {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 20px;
}
.create-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 14px;
  padding: 22px 24px;
}
.create-card h3, .team-section h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1F2E26;
}
.sub {
  margin: 0 0 14px;
  font-size: 13px;
  color: #8B968D;
}
.create-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.team-section {
  margin-top: 22px;
}
.team-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 14px;
}
.team-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.team-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.team-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.team-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #E8EFE6;
  color: #2F5D46;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 17px;
}
.role-tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 999px;
}
.role-tag.owner {
  background: #FFF7ED;
  color: #b45309;
}
.role-tag.admin {
  background: #F3F0E6;
  color: #8B6F47;
}
.role-tag.editor {
  background: #ECFDF5;
  color: #059669;
}
.role-tag.viewer {
  background: #F1F5F9;
  color: #64748b;
}
.role-tag.member {
  background: #E8EFE6;
  color: #2F5D46;
}
.team-name {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1F2E26;
}
.team-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #8B968D;
  min-height: 18px;
}
.team-meta {
  font-size: 12px;
  color: #B3BCB2;
}
.invite-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pending-box {
  background: #FFFDF7;
  border: 1px solid #F2E8CE;
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 14px;
}
.pending-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #8a6a25;
  margin-bottom: 8px;
}
.pending-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
}
.pi-name {
  color: #24312A;
  font-weight: 500;
}
.pi-muted {
  color: #8B968D;
  font-size: 12px;
}
.member-table {
  margin-top: 4px;
}
.muted {
  color: #B3BCB2;
  font-size: 12.5px;
}
@media (max-width: 800px) {
  .team-grid { grid-template-columns: 1fr 1fr; }
}

/* ============================================================
   团队页：行式团队清单 + 成员表（沿用工作台通用语汇）
   ============================================================ */
.teams {
  margin-top: 8px;
}
.team-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--ink);
}
.team-row {
  display: grid;
  grid-template-columns: 48px 1fr 96px 120px 24px;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
  border-bottom: 1px solid var(--hair);
  cursor: pointer;
  transition: background 0.14s ease;
}
.team-row:hover {
  background: #fff;
}
.t-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--ink);
  font-size: 16px;
  font-weight: 700;
}
.t-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}
.t-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.t-desc {
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.t-role {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-2);
  border: 1px solid var(--hair);
  padding: 3px 8px;
  text-align: center;
}
.t-role.owner {
  border-color: var(--ink);
  color: var(--ink);
  font-weight: 700;
}
.t-role.admin {
  border-color: var(--ink);
  color: var(--ink);
}
.t-count {
  font-size: 13px;
  color: var(--ink-3);
  text-align: right;
}
.t-go {
  font-size: 18px;
  color: var(--red);
  transition: transform 0.16s ease;
}
.team-row:hover .t-go {
  transform: translateX(4px);
}

/* 成员弹窗 */
.invite-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pending-box {
  border: 1px solid var(--hair);
  padding: 12px 14px;
  margin-bottom: 14px;
}
.pending-title {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
  margin-bottom: 8px;
}
.pending-item {
  display: flex;
  gap: 10px;
  align-items: baseline;
  padding: 6px 0;
  border-top: 1px solid var(--hair-soft);
  font-size: 13.5px;
}
.pi-name {
  color: var(--ink);
  font-weight: 500;
}
.pi-muted,
.muted {
  color: var(--ink-3);
  font-size: 12.5px;
}
:deep(.member-table) {
  border: 0;
}
:deep(.member-table .el-table__cell) {
  border-color: var(--hair-soft);
}
:deep(.member-table th.el-table__cell) {
  border-bottom: 1px solid var(--ink);
  background: transparent;
  font-weight: 400;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
}
:deep(.member-table .el-button) {
  height: auto;
  padding: 0 0 2px;
  border: 0;
  background: none !important;
}
:deep(.member-table .el-button--primary) {
  color: var(--ink) !important;
  font-weight: 700;
  border-bottom: 2px solid var(--red);
}
:deep(.member-table .el-button--danger) {
  color: var(--ink-3) !important;
}
:deep(.member-table .el-button--danger:hover) {
  color: var(--red) !important;
}
:deep(.member-table .el-tag) {
  border-radius: 0;
  background: none;
}

@media (max-width: 900px) {
  .team-row {
    grid-template-columns: 40px 1fr 24px;
    row-gap: 8px;
    padding: 16px 0;
  }
  .t-role,
  .t-count {
    grid-column: 2;
    justify-self: start;
    text-align: left;
  }
  .t-go {
    grid-column: 3;
    grid-row: 1;
  }
  .t-name {
    font-size: 17px;
  }
}


/* 角色标签：旧的彩色胶囊 → 硬线方块 */
:deep(.role-tag) {
  border-radius: 0;
  padding: 3px 8px;
  font-size: 12px;
  letter-spacing: 0.06em;
  background: none !important;
  border: 1px solid var(--hair);
  color: var(--ink-2) !important;
  font-weight: 400;
}
:deep(.role-tag.owner),
:deep(.role-tag.admin) {
  border-color: var(--ink);
  color: var(--ink) !important;
  font-weight: 700;
}


/* 弹窗底部按钮：硬边、方角、红为危险动作 */
:deep(.el-dialog__footer .el-button) {
  border-radius: 0;
  height: 34px;
  padding: 0 16px;
  font-family: inherit;
  border: 1px solid var(--ink);
  background: none;
  color: var(--ink);
}
:deep(.el-dialog__footer .el-button:hover) {
  background: var(--ink);
  color: #fff;
}
:deep(.el-dialog__footer .el-button--danger) {
  border-color: var(--red);
  color: var(--red);
}
:deep(.el-dialog__footer .el-button--danger:hover) {
  background: var(--red);
  color: #fff;
}
:deep(.member-table .el-button + .el-button) {
  margin-left: 12px;
}
:deep(.el-dialog__body) {
  padding-top: 6px;
}

</style>
