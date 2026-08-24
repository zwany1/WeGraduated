<template>
  <div class="page">
    <SiteNav />

    <main class="content">
      <section class="create-card">
        <h3>创建团队</h3>
        <p class="sub">团队成员可共享模板、查看彼此排版任务与结果</p>
        <div class="create-row">
          <el-input v-model="newName" placeholder="团队名称" maxlength="64" style="width: 220px" />
          <el-input v-model="newDesc" placeholder="团队简介（可选）" maxlength="120" style="width: 300px" @keyup.enter="create" />
          <el-button type="primary" :loading="creating" @click="create">创建团队</el-button>
        </div>
      </section>

      <section class="team-section">
        <h3>我的团队</h3>
        <div v-if="teams.length" class="team-grid">
          <div class="team-card" v-for="t in teams" :key="t.id" @click="openDetail(t)">
            <div class="team-top">
              <span class="team-icon">{{ t.name.slice(0, 1).toUpperCase() }}</span>
              <span class="role-tag" :class="t.role === 'owner' ? 'owner' : 'member'">{{ t.role === 'owner' ? '队长' : '成员' }}</span>
            </div>
            <h4 class="team-name">{{ t.name }}</h4>
            <p class="team-desc">{{ t.description || '暂无简介' }}</p>
            <span class="team-meta">{{ t.memberCount }} 名成员</span>
          </div>
        </div>
        <el-empty v-else description="还没有团队，创建一个开始协作吧" :image-size="80" />
      </section>
    </main>

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
          <el-table-column label="角色" width="80" align="center">
            <template #default="{ row }">
              <span class="role-tag" :class="row.role === 'owner' ? 'owner' : 'member'">{{ row.role === 'owner' ? '队长' : '成员' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button v-if="d.ownerId === meId && row.userId !== d.ownerId" link type="danger" size="small" @click="remove(row)">移除</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SiteNav from '../components/SiteNav.vue'
import { createTeam, listTeams, getTeamDetail, inviteMember, removeMember, leaveTeam, deleteTeam } from '../api/team'
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
    if (p) meId.value = p.userId
  } catch (e) {}
})
</script>

<style scoped>
.page {
  --c-primary: #3B6BFF;
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
  color: #1a1a2e;
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
  color: #1a1a2e;
}
.sub {
  margin: 0 0 14px;
  font-size: 13px;
  color: #909399;
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
  background: #EEF1FF;
  color: #3B6BFF;
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
.role-tag.member {
  background: #EEF1FF;
  color: #3B6BFF;
}
.team-name {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1a1a2e;
}
.team-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
  min-height: 18px;
}
.team-meta {
  font-size: 12px;
  color: #c0c4cc;
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
  color: #2c3140;
  font-weight: 500;
}
.pi-muted {
  color: #9ca3af;
  font-size: 12px;
}
.member-table {
  margin-top: 4px;
}
.muted {
  color: #c0c4cc;
  font-size: 12.5px;
}
@media (max-width: 800px) {
  .team-grid { grid-template-columns: 1fr 1fr; }
}
</style>
