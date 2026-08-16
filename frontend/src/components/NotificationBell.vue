<template>
  <div class="notif-bell">
    <el-badge :value="unread" :hidden="!unread" :max="99">
      <el-popover placement="bottom-end" width="380" trigger="click" v-model:visible="show" @show="onShow">
        <template #reference>
          <button class="bell-btn" :class="{ active: show }">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          </button>
        </template>
        <div class="notif-panel">
          <div class="notif-head">
            <span class="notif-title">通知</span>
            <el-button v-if="unread" link type="primary" size="small" @click="readAll">全部已读</el-button>
          </div>
          <div v-if="list.length" class="notif-list">
            <div v-for="n in list" :key="n.id" class="notif-item" :class="{ read: n.isRead }" @click="onItemClick(n)">
              <div class="ni-title">{{ n.title }}</div>
              <div class="ni-content">{{ n.content }}</div>
              <div v-if="n.type === 'team_invite' && !handled.has(n.id)" class="ni-actions" @click.stop>
                <el-button size="small" type="primary" @click="handleInvite(n, true)">同意</el-button>
                <el-button size="small" plain @click="handleInvite(n, false)">拒绝</el-button>
              </div>
              <div v-else-if="n.type === 'team_invite'" class="ni-done">已处理</div>
              <div class="ni-time">{{ fmt(n.createTime) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无通知" :image-size="50" />
        </div>
      </el-popover>
    </el-badge>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { listNotifications, unreadCount, markNotificationRead, markAllNotificationsRead, acceptTeamInvite, rejectTeamInvite } from '../api/notification'

const show = ref(false)
const list = ref([])
const unread = ref(0)
const handled = ref(new Set())
let timer = null

async function refreshUnread() {
  try {
    unread.value = await unreadCount() || 0
  } catch (e) {}
}

async function loadList() {
  try {
    list.value = await listNotifications() || []
  } catch (e) {}
}

function onShow() {
  show.value = true
  loadList()
}

async function onItemClick(n) {
  if (!n.isRead) {
    try {
      await markNotificationRead(n.id)
      n.isRead = true
      refreshUnread()
    } catch (e) {}
  }
}

async function handleInvite(n, accept) {
  try {
    if (accept) {
      await acceptTeamInvite(n.data.inviteId)
      ElMessage.success('已加入团队')
    } else {
      await rejectTeamInvite(n.data.inviteId)
      ElMessage.success('已拒绝邀请')
    }
    handled.value.add(n.id)
    try { await markNotificationRead(n.id) } catch (e) {}
    refreshUnread()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
    loadList()
  }
}

async function readAll() {
  try {
    await markAllNotificationsRead()
    list.value.forEach(n => { n.isRead = true })
    refreshUnread()
  } catch (e) {}
}

function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, 20000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.notif-bell {
  display: inline-flex;
  align-items: center;
}
.bell-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--c-border, #e5e7eb);
  background: #fff;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text2, #6b7280);
  cursor: pointer;
  transition: all 0.2s;
}
.bell-btn:hover, .bell-btn.active {
  color: var(--c-primary, #3B6BFF);
  border-color: var(--c-primary, #3B6BFF);
}
.notif-panel {
  padding: 4px 0;
}
.notif-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 14px 10px;
  border-bottom: 1px solid #f0f0f0;
}
.notif-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
}
.notif-list {
  max-height: 380px;
  overflow-y: auto;
}
.notif-item {
  padding: 10px 14px;
  border-bottom: 1px solid #f6f6f6;
  cursor: pointer;
}
.notif-item:hover {
  background: #f8f9fc;
}
.notif-item.read {
  opacity: 0.65;
}
.ni-title {
  font-size: 13px;
  font-weight: 600;
  color: #2c3140;
}
.ni-content {
  font-size: 12.5px;
  color: #6b7280;
  margin-top: 3px;
  line-height: 1.5;
}
.ni-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.ni-done {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}
.ni-time {
  font-size: 11px;
  color: #b0b4bd;
  margin-top: 6px;
}
</style>
