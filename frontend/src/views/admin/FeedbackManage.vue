<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></svg>
        <input v-model="keyword" placeholder="搜索反馈内容 / 用户名" @keyup.enter="load(1)" />
      </div>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(1)">
        <el-option label="待处理" value="PENDING" />
        <el-option label="已回复" value="REPLIED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <span class="toolbar-note">共 {{ total }} 条反馈 · 可回复 / 关闭 / 删除</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="提交者" min-width="150">
          <template #default="{ row }">
            <div class="cell-user">
              <span class="cell-avatar">
                <img v-if="row.avatar" :src="row.avatar" class="cell-avatar-img" />
                <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
              </span>
              <div class="cell-user-meta">
                <span class="cell-name">{{ row.nickname || row.username || '匿名' }}</span>
                <span class="cell-uname">@{{ row.username || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="反馈内容" min-width="240">
          <template #default="{ row }"><span class="cell-content">{{ row.content }}</span></template>
        </el-table-column>
        <el-table-column label="联系方式" width="160">
          <template #default="{ row }"><span class="cell-muted">{{ row.contact || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }"><span class="cell-muted">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <span class="op-cell">
              <el-button v-perm="'system:feedback:list'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-perm="'system:feedback:reply'" link type="primary" size="small" @click="openReply(row)">回复</el-button>
              <el-button v-perm="'system:feedback:reply'" link :type="row.status === 'CLOSED' ? 'success' : 'warning'" size="small" @click="toggleStatus(row)">
                {{ row.status === 'CLOSED' ? '重开' : '关闭' }}
              </el-button>
              <el-button v-perm="'system:feedback:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 详情 / 回复弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'reply' ? '回复反馈' : '反馈详情'" width="560px" append-to-body>
      <div v-if="target" class="fb-detail">
        <div class="fb-detail-head">
          <span class="cell-avatar cell-avatar-lg">
            <img v-if="target.avatar" :src="target.avatar" class="cell-avatar-img" />
            <span v-else>{{ (target.username || 'U').slice(0, 1).toUpperCase() }}</span>
          </span>
          <div class="cell-user-meta">
            <span class="cell-name">{{ target.nickname || target.username || '匿名' }}</span>
            <span class="cell-uname">@{{ target.username || '-' }} · {{ formatTime(target.createTime) }}</span>
          </div>
        </div>
        <div class="fb-detail-tags">
          <el-tag size="small" :type="categoryTagType(target.category)">{{ categoryLabel(target.category) }}</el-tag>
          <el-tag size="small" :type="statusTagType(target.status)" effect="plain">{{ statusLabel(target.status) }}</el-tag>
          <span v-if="target.contact" class="cell-muted">联系方式：{{ target.contact }}</span>
        </div>
        <div class="fb-detail-content">{{ target.content }}</div>
        <div v-if="target.images && target.images.length" class="fb-detail-images">
          <el-image v-for="(img, i) in target.images" :key="i" :src="img"
            :preview-src-list="target.images" :initial-index="i" class="fb-detail-img" fit="cover" />
        </div>
        <div v-if="target.reply" class="fb-detail-reply">
          <div class="fb-reply-flag">管理员回复 · {{ target.replyUsername || '管理员' }} · {{ formatTime(target.replyTime) }}</div>
          <div class="fb-reply-body">{{ target.reply }}</div>
        </div>
        <div v-if="dialogMode === 'reply'" v-perm="'system:feedback:reply'" class="fb-reply-box">
          <el-input v-model="replyText" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="输入回复内容..." />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="dialogMode === 'reply'" v-perm="'system:feedback:reply'" type="primary" :loading="replying" @click="submitReply">
          提交回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFeedbacks, replyFeedback, updateFeedbackStatus, deleteFeedback } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const keyword = ref('')
const status = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('detail')
const target = ref(null)
const replyText = ref('')
const replying = ref(false)

onMounted(() => load(1))

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await listFeedbacks({
      page: page.value,
      size: size.value,
      status: status.value || undefined,
      keyword: keyword.value || undefined
    })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  status.value = ''
  load(1)
}

function openDetail(row) {
  dialogMode.value = 'detail'
  target.value = row
  dialogVisible.value = true
}

function openReply(row) {
  dialogMode.value = 'reply'
  target.value = row
  replyText.value = row.reply || ''
  dialogVisible.value = true
}

async function submitReply() {
  if (!target.value) return
  if (!replyText.value || !replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyFeedback(target.value.id, replyText.value)
    ElMessage.success('已回复')
    dialogVisible.value = false
    await load()
  } finally {
    replying.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 'CLOSED' ? 'PENDING' : 'CLOSED'
  await updateFeedbackStatus(row.id, next)
  ElMessage.success(next === 'CLOSED' ? '已关闭' : '已重新打开')
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该反馈？删除后不可恢复。', '提示', { type: 'warning' })
  await deleteFeedback(row.id)
  ElMessage.success('已删除')
  await load()
}

function categoryLabel(c) {
  return ({ suggestion: '功能建议', bug: 'Bug反馈', other: '其他' })[c] || '其他'
}
function categoryTagType(c) {
  return c === 'bug' ? 'danger' : (c === 'suggestion' ? 'success' : 'info')
}
function statusLabel(s) {
  return ({ PENDING: '待处理', REPLIED: '已回复', CLOSED: '已关闭' })[s] || s
}
function statusTagType(s) {
  return s === 'REPLIED' ? 'success' : (s === 'CLOSED' ? 'info' : 'warning')
}
function formatTime(t) {
  if (!t) return ''
  let d
  if (Array.isArray(t)) {
    d = new Date(t[0] || 1970, (t[1] || 1) - 1, t[2] || 1, t[3] || 0, t[4] || 0, t[5] || 0)
  } else {
    d = new Date(t)
  }
  if (isNaN(d.getTime())) return String(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.mgmt { padding: 4px 0; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.search-box { display: flex; align-items: center; gap: 6px; padding: 0 12px; height: 34px; background: #f7f7f4; border-radius: 8px; border: 1px solid #ece7da; min-width: 260px; }
.search-box input { border: none; outline: none; background: transparent; flex: 1; font-size: 13px; color: #4a4332; }
.toolbar-note { color: #9a917d; font-size: 12px; margin-left: auto; }

.table-card { background: #fff; border: 1px solid #ece7da; border-radius: 12px; padding: 12px; }
.cell-user { display: flex; align-items: center; gap: 8px; }
.cell-avatar { width: 30px; height: 30px; border-radius: 50%; background: #eef2ff; color: #3b6bff; display: flex; align-items: center; justify-content: center; font-size: 13px; overflow: hidden; flex-shrink: 0; }
.cell-avatar-lg { width: 40px; height: 40px; font-size: 16px; }
.cell-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.cell-user-meta { display: flex; flex-direction: column; }
.cell-name { font-weight: 600; color: #3a3320; font-size: 13px; }
.cell-uname { color: #9a917d; font-size: 12px; }
.cell-muted { color: #9a917d; font-size: 12px; }
.cell-content { color: #4a4332; font-size: 13px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.op-cell { display: inline-flex; gap: 4px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }

.fb-detail-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fb-detail-tags { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.fb-detail-content { color: #3a3320; font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; padding: 12px; background: #faf8f1; border-radius: 8px; margin-bottom: 12px; }
.fb-detail-images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.fb-detail-img { width: 80px; height: 80px; border-radius: 6px; border: 1px solid #eee; }
.fb-detail-reply { padding: 10px 12px; background: #f6f9ff; border-radius: 8px; margin-bottom: 12px; }
.fb-reply-flag { color: #3b6bff; font-weight: 600; font-size: 12px; }
.fb-reply-body { color: #444; font-size: 13px; line-height: 1.6; margin-top: 4px; white-space: pre-wrap; word-break: break-word; }
.fb-reply-box { border-top: 1px solid #eee; padding-top: 12px; }
</style>
