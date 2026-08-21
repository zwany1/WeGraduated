<template>
  <el-dialog v-model="show" title="用户反馈" width="680px" top="6vh" :close-on-click-modal="false" append-to-body @open="onOpen">
    <!-- 提交表单 -->
    <div class="fb-form">
      <el-form label-position="top" @submit.prevent="submit">
        <div class="fb-form-row">
          <el-form-item label="分类" class="fb-form-cat">
            <el-select v-model="form.category">
              <el-option label="功能建议" value="suggestion" />
              <el-option label="Bug 反馈" value="bug" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系方式（选填）" class="fb-form-contact">
            <el-input v-model="form.contact" maxlength="128" placeholder="邮箱 / QQ / 微信，方便管理员联系你" />
          </el-form-item>
        </div>
        <el-form-item label="反馈内容">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="说说你遇到的问题或建议..." />
        </el-form-item>
        <el-form-item label="图片（选填，最多6张，单张≤2MB）">
          <div class="fb-images">
            <div v-for="(img, i) in form.images" :key="i" class="fb-img-item">
              <img :src="img" class="fb-img-thumb" />
              <span class="fb-img-del" @click="removeImage(i)">×</span>
            </div>
            <div v-if="form.images.length < maxImages" class="fb-img-add" @click="pickImages">+</div>
          </div>
          <input ref="imageInput" type="file" accept="image/*" multiple style="display:none" @change="onImageChange" />
        </el-form-item>
        <div class="fb-form-foot">
          <el-button type="primary" :loading="submitting" @click="submit">提交反馈</el-button>
        </div>
      </el-form>
    </div>

    <!-- 公开反馈墙 -->
    <div class="fb-wall">
      <div class="fb-wall-title">
        反馈墙 · 共 {{ total }} 条
        <span class="fb-wall-hint">单击查看详情</span>
      </div>
      <div v-loading="loading" class="fb-list">
        <div v-for="row in rows" :key="row.id" class="fb-item" @click="openDetail(row.id)">
          <div class="fb-item-head">
            <span class="fb-avatar">
              <img v-if="row.avatar" :src="row.avatar" class="fb-avatar-img" />
              <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
            </span>
            <span class="fb-username">{{ row.nickname || row.username || '匿名用户' }}</span>
            <el-tag size="small" :type="categoryTagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
            <el-tag size="small" :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
            <span class="fb-time">{{ formatTime(row.createTime) }}</span>
          </div>
          <div class="fb-item-content">{{ row.content }}</div>
          <div v-if="row.images && row.images.length" class="fb-item-images">
            <img v-for="(img, i) in row.images.slice(0, 4)" :key="i" :src="img" class="fb-item-img" />
            <span v-if="row.images.length > 4" class="fb-item-img-more">+{{ row.images.length - 4 }}</span>
          </div>
          <div v-if="row.reply" class="fb-item-reply">
            <span class="fb-reply-flag">管理员回复：</span>{{ row.reply }}
          </div>
        </div>
        <el-empty v-if="!loading && rows.length === 0" description="还没有反馈，快来抢沙发" />
      </div>
      <div class="fb-pager">
        <el-pagination background layout="prev, pager, next" :total="total" v-model:current-page="page"
          :page-size="size" @current-change="load" />
      </div>
    </div>
  </el-dialog>

  <!-- 详情抽屉 -->
  <el-drawer v-model="detailShow" title="反馈详情" size="440px" append-to-body>
    <div v-loading="detailLoading" class="fb-detail">
      <template v-if="detail">
        <div class="fb-detail-head">
          <span class="fb-avatar fb-avatar-lg">
            <img v-if="detail.avatar" :src="detail.avatar" class="fb-avatar-img" />
            <span v-else>{{ (detail.username || 'U').slice(0, 1).toUpperCase() }}</span>
          </span>
          <div class="fb-detail-meta">
            <div class="fb-detail-name">{{ detail.nickname || detail.username || '匿名用户' }}</div>
            <div class="fb-detail-sub">
              <el-tag size="small" :type="categoryTagType(detail.category)">{{ categoryLabel(detail.category) }}</el-tag>
              <el-tag size="small" :type="statusTagType(detail.status)" effect="plain">{{ statusLabel(detail.status) }}</el-tag>
              <span class="fb-time">{{ formatTime(detail.createTime) }}</span>
            </div>
          </div>
        </div>
        <div class="fb-detail-content">{{ detail.content }}</div>
        <div v-if="detail.images && detail.images.length" class="fb-detail-images">
          <el-image v-for="(img, i) in detail.images" :key="i" :src="img"
            :preview-src-list="detail.images" :initial-index="i" class="fb-detail-img" fit="cover" />
        </div>

        <div v-if="detail.reply" class="fb-detail-reply">
          <div class="fb-reply-flag">管理员回复 · {{ detail.replyUsername || '管理员' }} · {{ formatTime(detail.replyTime) }}</div>
          <div class="fb-reply-body">{{ detail.reply }}</div>
        </div>

        <div v-if="canReply" class="fb-reply-box">
          <div class="fb-reply-box-title">管理员回复</div>
          <el-input v-model="replyText" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="输入回复内容..." />
          <div class="fb-reply-box-foot">
            <el-button v-if="detail.status !== 'CLOSED'" size="small" @click="setStatus(detail.id, 'CLOSED')">关闭反馈</el-button>
            <el-button v-else size="small" @click="setStatus(detail.id, 'PENDING')">重新打开</el-button>
            <el-button type="primary" size="small" :loading="replying" @click="submitReply">提交回复</el-button>
          </div>
        </div>
        <div v-else class="fb-reply-tip">普通用户不可回复，如需跟进请联系管理员。</div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createFeedback, listFeedbacks, getFeedbackDetail } from '../api/feedback'
import { replyFeedback, updateFeedbackStatus } from '../api/admin'
import { isAdmin } from '../utils/perm'

const maxImages = 6

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible'])
const show = computed({
  get: () => props.visible,
  set: v => emit('update:visible', v)
})

const canReply = computed(() => isAdmin())

const form = ref({ category: 'suggestion', content: '', contact: '', images: [] })
const submitting = ref(false)
const imageInput = ref(null)

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const detail = ref(null)
const detailShow = ref(false)
const detailLoading = ref(false)
const replyText = ref('')
const replying = ref(false)

function onOpen() {
  // 每次打开都拉取最新列表, 避免后台删除/回复后前台仍显示旧数据
  load(1)
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await listFeedbacks({ page: page.value, size: size.value })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function pickImages() {
  imageInput.value && imageInput.value.click()
}

function onImageChange(e) {
  const files = e.target.files
  if (!files || !files.length) return
  const remain = maxImages - form.value.images.length
  if (remain <= 0) {
    ElMessage.warning(`最多上传${maxImages}张图片`)
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
      form.value.images.push(reader.result)
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

function removeImage(i) {
  form.value.images.splice(i, 1)
}

async function submit() {
  if (!form.value.content || !form.value.content.trim()) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  submitting.value = true
  try {
    await createFeedback({
      category: form.value.category,
      content: form.value.content,
      contact: form.value.contact,
      images: form.value.images
    })
    ElMessage.success('反馈已提交，感谢你的支持')
    form.value.content = ''
    form.value.contact = ''
    form.value.images = []
    await load(1)
  } finally {
    submitting.value = false
  }
}

async function openDetail(id) {
  detailShow.value = true
  detail.value = null
  detailLoading.value = true
  replyText.value = ''
  try {
    detail.value = await getFeedbackDetail(id)
    replyText.value = detail.value.reply || ''
  } finally {
    detailLoading.value = false
  }
}

async function submitReply() {
  if (!detail.value) return
  if (!replyText.value || !replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyFeedback(detail.value.id, replyText.value)
    ElMessage.success('已回复')
    await openDetail(detail.value.id)
    await load()
  } finally {
    replying.value = false
  }
}

async function setStatus(id, status) {
  await updateFeedbackStatus(id, status)
  ElMessage.success(status === 'CLOSED' ? '已关闭' : '已重新打开')
  await openDetail(id)
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
.fb-form { margin-bottom: 8px; }
.fb-form-row { display: flex; gap: 12px; }
.fb-form-cat { flex: 0 0 160px; }
.fb-form-contact { flex: 1; }
.fb-form-foot { text-align: right; }

.fb-images { display: flex; flex-wrap: wrap; gap: 8px; }
.fb-img-item { position: relative; width: 64px; height: 64px; border-radius: 6px; overflow: hidden; border: 1px solid #eee; }
.fb-img-thumb { width: 100%; height: 100%; object-fit: cover; }
.fb-img-del { position: absolute; top: 0; right: 0; width: 18px; height: 18px; background: rgba(0,0,0,0.5); color: #fff; font-size: 12px; line-height: 18px; text-align: center; cursor: pointer; }
.fb-img-add { width: 64px; height: 64px; border: 1px dashed #c0c4cc; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #909399; cursor: pointer; }
.fb-img-add:hover { border-color: #3b6bff; color: #3b6bff; }

.fb-wall { border-top: 1px solid #eee; padding-top: 12px; margin-top: 8px; }
.fb-wall-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.fb-wall-hint { font-size: 12px; font-weight: 400; color: #999; }

.fb-list { min-height: 120px; }
.fb-item { border: 1px solid #f0f0f0; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; cursor: pointer; transition: box-shadow 0.15s, border-color 0.15s; }
.fb-item:hover { border-color: #3b6bff; box-shadow: 0 4px 12px rgba(59, 107, 255, 0.08); }
.fb-item-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.fb-avatar { width: 28px; height: 28px; border-radius: 50%; background: #eef2ff; color: #3b6bff; display: flex; align-items: center; justify-content: center; font-size: 13px; overflow: hidden; flex-shrink: 0; }
.fb-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fb-avatar-lg { width: 40px; height: 40px; font-size: 16px; }
.fb-username { font-weight: 600; color: #333; font-size: 13px; }
.fb-time { margin-left: auto; color: #aaa; font-size: 12px; }
.fb-item-content { color: #444; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.fb-item-images { display: flex; gap: 6px; margin-top: 6px; align-items: center; }
.fb-item-img { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; border: 1px solid #eee; }
.fb-item-img-more { font-size: 12px; color: #999; }
.fb-item-reply { margin-top: 8px; padding: 8px 10px; background: #f6f9ff; border-left: 3px solid #3b6bff; border-radius: 4px; font-size: 12px; color: #555; line-height: 1.6; }
.fb-reply-flag { color: #3b6bff; font-weight: 600; }
.fb-pager { margin-top: 12px; text-align: center; }

.fb-detail { padding: 0 4px; min-height: 120px; }
.fb-detail-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fb-detail-meta { flex: 1; }
.fb-detail-name { font-weight: 600; color: #333; }
.fb-detail-sub { display: flex; align-items: center; gap: 6px; margin-top: 4px; flex-wrap: wrap; }
.fb-detail-content { color: #333; font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; padding: 12px; background: #fafafa; border-radius: 8px; margin-bottom: 12px; }
.fb-detail-images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.fb-detail-img { width: 88px; height: 88px; border-radius: 6px; border: 1px solid #eee; }
.fb-detail-reply { padding: 10px 12px; background: #f6f9ff; border-radius: 8px; margin-bottom: 12px; }
.fb-reply-body { color: #444; font-size: 13px; line-height: 1.6; margin-top: 4px; white-space: pre-wrap; word-break: break-word; }
.fb-reply-box { border-top: 1px solid #eee; padding-top: 12px; }
.fb-reply-box-title { font-weight: 600; color: #333; margin-bottom: 8px; }
.fb-reply-box-foot { margin-top: 8px; display: flex; justify-content: flex-end; gap: 8px; }
.fb-reply-tip { color: #999; font-size: 12px; text-align: center; padding: 12px; }
</style>
