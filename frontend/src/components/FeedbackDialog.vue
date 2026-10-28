<template>
  <!-- 右侧抽屉(不遮挡页面内容) -->
  <el-drawer v-model="show" direction="rtl" size="720px" :with-header="false" :close-on-click-modal="true" append-to-body @open="onOpen">
    <div class="fb-drawer">
      <!-- 头部 -->
      <div class="fb-header">
        <div class="fb-header-left">
          <span class="fb-sq" aria-hidden="true"></span>
          <div class="fb-header-text">
            <span class="fb-kicker">FEEDBACK</span>
            <span class="fb-header-title">用户反馈</span>
          </div>
        </div>
        <span class="fb-header-count" v-if="activeTab === 'wall'">{{ total }} 条</span>
      </div>
      <!-- Tab 切换(编号式) -->
      <div class="fb-tabs">
        <button class="fb-tab" :class="{ active: activeTab === 'form' }" @click="activeTab = 'form'">
          <span class="fb-tab-no">01</span>提交反馈
        </button>
        <button class="fb-tab" :class="{ active: activeTab === 'wall' }" @click="activeTab = 'wall'; load()">
          <span class="fb-tab-no">02</span>反馈墙
        </button>
      </div>

      <div class="fb-content">
        <!-- 提交表单 -->
        <transition name="fb-fade" mode="out-in">
        <div v-if="activeTab === 'form'" key="form" class="fb-form">
          <div class="fb-field-label">分类</div>
          <div class="fb-chips">
            <button v-for="c in categories" :key="c.value" class="fb-chip" :class="[c.value, { active: form.category === c.value }]" @click="form.category = c.value">
              <span class="fb-chip-dot"></span>{{ c.label }}
            </button>
          </div>
          <div class="fb-field-label">反馈内容</div>
          <div class="fb-editor" @paste="onPaste">
            <textarea v-model="form.content" class="fb-textarea" rows="5" maxlength="2000" placeholder="说说你遇到的问题或建议... (支持粘贴图片)"></textarea>
            <span class="fb-editor-count">{{ form.content.length }}/2000</span>
          </div>
          <div class="fb-field-label">联系方式（选填）</div>
          <input v-model="form.contact" class="fb-input" maxlength="128" placeholder="邮箱 / QQ / 微信" />
          <div v-if="form.images.length" class="fb-field-label">已添加 {{ form.images.length }} 张图片</div>
          <div class="fb-images">
            <div v-for="(img, i) in form.images" :key="i" class="fb-img-item">
              <img :src="img" class="fb-img-thumb" />
              <span class="fb-img-del" @click="removeImage(i)">×</span>
            </div>
            <div v-if="form.images.length < maxImages" class="fb-img-add" @click="pickImages">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/></svg>
              <span>上传</span>
            </div>
          </div>
          <input ref="imageInput" type="file" accept="image/*" multiple style="display:none" @change="onImageChange" />
          <div class="fb-form-foot">
            <el-button type="primary" :loading="submitting" @click="submit">提交反馈</el-button>
          </div>
        </div>
        </transition>

        <!-- 反馈墙 -->
        <transition name="fb-fade" mode="out-in">
        <div v-if="activeTab === 'wall'" key="wall" class="fb-wall">
          <div v-loading="loading" class="fb-grid">
            <div v-for="row in rows" :key="row.id" class="fb-card" @click="openDetail(row.id)">
              <div class="fb-card-top">
                <span class="fb-avatar">
                  <img v-if="row.avatar" :src="row.avatar" class="fb-avatar-img" />
                  <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
                </span>
                <span class="fb-card-name">{{ row.nickname || row.username || '匿名用户' }}</span>
                <span class="fb-status" :class="row.status" :title="statusLabel(row.status)"></span>
              </div>
              <div class="fb-card-meta">
                <span class="fb-cat" :class="row.category">{{ categoryLabel(row.category) }}</span>
                <span class="fb-time">{{ formatTime(row.createTime) }}</span>
              </div>
              <p class="fb-card-text">{{ row.content }}</p>
              <div class="fb-card-foot">
                <span v-if="row.images && row.images.length" class="fb-card-imgs">{{ row.images.length }} 张图片</span>
                <span v-if="row.reply" class="fb-card-replied">管理员已回复</span>
                <button type="button" class="fb-detail-btn" @click.stop="openDetail(row.id)">详情</button>
              </div>
            </div>
            <div v-if="!loading && rows.length === 0" class="fb-empty">
              <div class="fb-empty-title">还没有反馈</div>
              <div class="fb-empty-sub">来抢第一个沙发吧</div>
            </div>
          </div>
          <div class="fb-pager">
            <el-pagination background layout="prev, pager, next" :total="total" v-model:current-page="page"
              :page-size="size" @current-change="load" />
          </div>
        </div>
        </transition>
      </div>
    </div>
  </el-drawer>

  <!-- 详情抽屉 -->
  <el-drawer v-model="detailShow" direction="rtl" size="440px" :with-header="false" append-to-body>
    <div class="fb-detail">
      <div v-if="detailLoading" class="fb-detail-loading">加载中...</div>
      <template v-if="detail">
        <!-- 头部 -->
        <div class="fd-hero">
          <div class="fd-hero-row">
            <span class="fd-avatar">
              <img v-if="detail.avatar" :src="detail.avatar" class="fd-avatar-img" />
              <span v-else>{{ (detail.username || 'U').slice(0, 1).toUpperCase() }}</span>
            </span>
            <div class="fd-hero-info">
              <div class="fd-hero-name">{{ detail.nickname || detail.username || '匿名用户' }}</div>
              <div class="fd-hero-tags">
                <span class="fd-tag" :class="detail.category">{{ categoryLabel(detail.category) }}</span>
                <span class="fd-status-pill" :class="detail.status">
                  <span class="fd-status-dot2"></span>{{ statusLabel(detail.status) }}
                </span>
              </div>
            </div>
          </div>
          <div class="fd-hero-time">{{ formatTime(detail.createTime) }}</div>
        </div>

        <!-- 反馈内容 -->
        <div class="fd-section">
          <div class="fd-section-label">反馈内容</div>
          <div class="fd-content-box">{{ detail.content }}</div>
        </div>

        <!-- 图片 -->
        <div v-if="detail.images && detail.images.length" class="fd-section">
          <div class="fd-section-label">附件 ({{ detail.images.length }})</div>
          <div class="fd-images">
            <img v-for="(img, i) in detail.images" :key="i" :src="img" class="fd-img" @click="previewImages(detail.images, i)" />
          </div>
        </div>

        <!-- 管理员回复 -->
        <div v-if="detail.reply" class="fd-section">
          <div class="fd-section-label">管理员回复</div>
          <div class="fd-reply-box">
            <div class="fd-reply-head">
              <span class="fd-reply-avatar">
                <img v-if="detail.replyAvatar" :src="detail.replyAvatar" class="fd-reply-avatar-img" />
                <span v-else>{{ (detail.replyUsername || '管').slice(0, 1).toUpperCase() }}</span>
              </span>
              <span class="fd-reply-name">{{ detail.replyUsername || '管理员' }}</span>
              <span class="fd-reply-time">{{ formatTime(detail.replyTime) }}</span>
            </div>
            <div class="fd-reply-text">{{ detail.reply }}</div>
          </div>
        </div>

        <!-- 管理员操作区 -->
        <div v-if="canReply" class="fd-section fd-admin-box">
          <div class="fd-section-label">管理员操作</div>
          <textarea v-model="replyText" class="fd-reply-input" rows="3" placeholder="输入回复内容..."></textarea>
          <div class="fd-admin-actions">
            <el-button v-if="detail.status !== 'CLOSED'" size="small" @click="setStatus(detail.id, 'CLOSED')">关闭反馈</el-button>
            <el-button v-else size="small" @click="setStatus(detail.id, 'PENDING')">重新打开</el-button>
            <el-button type="primary" size="small" :loading="replying" @click="submitReply">提交回复</el-button>
          </div>
        </div>
        <div v-else class="fd-no-reply">如需跟进，请联系管理员。</div>
      </template>
    </div>
  </el-drawer>
  <!-- 图片预览 -->
  <el-image-viewer v-if="imgViewerShow" :url-list="imgViewerList" :initial-index="imgViewerStart" @close="imgViewerShow = false" />
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
const activeTab = ref('form')
const categories = [
  { value: 'suggestion', label: '功能建议' },
  { value: 'bug', label: 'Bug 反馈' },
  { value: 'other', label: '其他' }
]

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
  activeTab.value = 'form'
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

/** 粘贴图片: 监听 paste 事件, 提取剪贴板中的图片 */
function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (!file) continue
      if (form.value.images.length >= maxImages) {
        ElMessage.warning(`最多上传${maxImages}张图片`)
        return
      }
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.warning('图片超过2MB，请压缩后粘贴')
        continue
      }
      const reader = new FileReader()
      reader.onload = () => {
        form.value.images.push(reader.result)
      }
      reader.readAsDataURL(file)
    }
  }
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
    activeTab.value = 'wall'
    await load(1)
  } finally {
    submitting.value = false
  }
}

// 图片预览
const imgViewerShow = ref(false)
const imgViewerList = ref([])
const imgViewerStart = ref(0)
function previewImages(list, start) {
  imgViewerList.value = list
  imgViewerStart.value = start
  imgViewerShow.value = true
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
function statusLabel(s) {
  return ({ PENDING: '待处理', REPLIED: '已回复', CLOSED: '已关闭' })[s] || s
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
/* ===== 抽屉容器(瑞士海报世界) ===== */
.fb-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f7f6f6;
  --ink: #0a0909;
  --paper: #f7f6f6;
  --red: #dd2627;
  --hair: #c1c1c3;
  --hair-2: #e4e4e4;
  --gray: #6b6b6e;
}

/* ===== 头部 ===== */
.fb-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 24px 28px 14px;
  border-bottom: 4px solid var(--ink);
  margin: 0 28px;
  padding-left: 0;
  padding-right: 0;
}
.fb-header-left { display: flex; align-items: flex-start; gap: 10px; }
.fb-sq { width: 12px; height: 12px; background: var(--red); margin-top: 5px; flex-shrink: 0; }
.fb-header-text { display: flex; flex-direction: column; gap: 2px; }
.fb-kicker { font-size: 10px; letter-spacing: 0.28em; color: var(--gray); font-weight: 700; }
.fb-header-title { font-size: 24px; font-weight: 900; color: var(--ink); line-height: 1.1; }
.fb-header-count { font-size: 13px; color: var(--gray); font-variant-numeric: tabular-nums; }

/* ===== Tab(编号 + 红下划线) ===== */
.fb-tabs {
  display: flex;
  gap: 22px;
  padding: 0 28px;
  border-bottom: 1px solid var(--hair);
}
.fb-tab {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 12px 2px 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--gray);
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.fb-tab-no { font-size: 11px; font-weight: 700; color: var(--gray); font-variant-numeric: tabular-nums; }
.fb-tab:hover { color: var(--ink); }
.fb-tab.active { color: var(--ink); font-weight: 700; border-bottom-color: var(--red); }
.fb-tab.active .fb-tab-no { color: var(--red); }

/* ===== 内容区 ===== */
.fb-content { flex: 1; overflow-y: auto; padding: 20px 28px 28px; }

/* ===== 表单 ===== */
.fb-form { display: flex; flex-direction: column; }
.fb-field-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--gray);
  letter-spacing: 0.08em;
  margin: 18px 0 8px;
}
.fb-field-label:first-child { margin-top: 0; }

/* 分类 chip(方形 hairline) */
.fb-chips { display: flex; gap: 8px; }
.fb-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px;
  border: 1px solid var(--hair);
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--gray);
  cursor: pointer;
  border-radius: 0;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.fb-chip-dot { width: 8px; height: 8px; border-radius: 0; }
.fb-chip:hover { border-color: var(--ink); color: var(--ink); }
.fb-chip.active { background: var(--ink); border-color: var(--ink); color: #fff; font-weight: 700; }
.fb-chip.active .fb-chip-dot { background: #fff; }
.fb-chip.suggestion .fb-chip-dot { background: var(--ink); }
.fb-chip.bug .fb-chip-dot { background: var(--red); }
.fb-chip.other .fb-chip-dot { background: #8a8a8e; }
.fb-chip.active.bug { background: var(--red); border-color: var(--red); }

/* 编辑器 */
.fb-editor { position: relative; }
.fb-textarea {
  width: 100%;
  border: 1px solid var(--hair);
  border-radius: 0;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink);
  background: #fff;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.fb-textarea:focus { border-color: var(--ink); }
.fb-editor-count {
  position: absolute; right: 10px; bottom: 6px; font-size: 11px; color: #a8a8ab;
  pointer-events: none; font-variant-numeric: tabular-nums;
}
.fb-input {
  width: 100%;
  border: 1px solid var(--hair);
  border-radius: 0;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--ink);
  background: #fff;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.fb-input:focus { border-color: var(--ink); }

/* 图片区 */
.fb-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
.fb-img-item { position: relative; width: 68px; height: 68px; overflow: hidden; border: 1px solid var(--hair); border-radius: 0; }
.fb-img-thumb { width: 100%; height: 100%; object-fit: cover; display: block; }
.fb-img-del {
  position: absolute; top: 0; right: 0; width: 20px; height: 20px;
  background: var(--ink); color: #fff; font-size: 13px; line-height: 20px;
  text-align: center; cursor: pointer; opacity: 0; transition: opacity 0.2s;
}
.fb-img-item:hover .fb-img-del { opacity: 1; }
.fb-img-add {
  width: 68px; height: 68px;
  border: 1px dashed var(--hair);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  font-size: 11px; color: var(--gray); cursor: pointer; background: transparent;
  transition: border-color 0.2s, color 0.2s;
}
.fb-img-add:hover { border-color: var(--red); color: var(--red); }
.fb-form-foot { text-align: right; margin-top: 22px; }

/* ===== 反馈墙 ===== */
.fb-wall { min-height: 200px; }
.fb-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  padding: 4px 0 16px;
  align-items: start;
}
@media (max-width: 640px) { .fb-grid { grid-template-columns: 1fr; } }

.fb-card {
  background: #fff;
  border: 1px solid var(--hair);
  border-radius: 0;
  padding: 16px 16px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color 0.2s;
}
.fb-card:hover { border-color: var(--ink); }

.fb-card-top { display: flex; align-items: center; gap: 10px; }
.fb-avatar {
  width: 30px; height: 30px;
  background: var(--ink); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; overflow: hidden; flex-shrink: 0;
}
.fb-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fb-card-name { font-weight: 700; font-size: 14px; color: var(--ink); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fb-status { width: 9px; height: 9px; flex-shrink: 0; }
.fb-status.PENDING { background: #f59e0b; }
.fb-status.REPLIED { background: #10b981; }
.fb-status.CLOSED { background: var(--hair); }

.fb-card-meta { display: flex; align-items: center; gap: 10px; }
.fb-cat { font-size: 11px; font-weight: 700; color: var(--gray); letter-spacing: 0.04em; }
.fb-cat.bug { color: var(--red); }
.fb-time { font-size: 11px; color: #a8a8ab; font-variant-numeric: tabular-nums; }

.fb-card-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: #3d3d40;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fb-card-foot { display: flex; align-items: center; gap: 10px; margin-top: 2px; }
.fb-card-imgs { font-size: 11px; color: #a8a8ab; }
.fb-card-replied {
  font-size: 11px;
  font-weight: 700;
  color: var(--red);
  border: 1px solid var(--red);
  padding: 1px 6px;
}
.fb-detail-btn {
  margin-left: auto;
  background: transparent;
  border: 1px solid var(--hair);
  border-radius: 0;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--ink);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
}
.fb-detail-btn:hover { background: var(--ink); border-color: var(--ink); color: #fff; }

/* 空状态 */
.fb-empty { grid-column: 1 / -1; text-align: center; padding: 70px 0 50px; }
.fb-empty-title { font-size: 22px; font-weight: 900; color: var(--ink); }
.fb-empty-sub { font-size: 13px; color: var(--gray); margin-top: 6px; }

.fb-pager { display: flex; justify-content: center; padding-top: 8px; }

/* ===== 详情 ===== */
.fb-detail {
  padding: 0;
  height: 100%;
  overflow-y: auto;
  background: var(--paper);
  --ink: #0a0909;
  --paper: #f7f6f6;
  --red: #dd2627;
  --hair: #c1c1c3;
  --gray: #6b6b6e;
}
.fb-detail-loading { padding: 40px; text-align: center; color: var(--gray); }

/* 头部 */
.fd-hero {
  padding: 24px 20px 18px;
  border-top: 4px solid var(--red);
  border-bottom: 1px solid var(--hair);
}
.fd-hero-row { display: flex; align-items: center; gap: 12px; }
.fd-avatar {
  width: 46px; height: 46px;
  background: var(--ink); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; overflow: hidden; flex-shrink: 0;
}
.fd-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fd-hero-info { flex: 1; min-width: 0; }
.fd-hero-name { font-weight: 900; font-size: 18px; color: var(--ink); }
.fd-hero-tags { display: flex; align-items: center; gap: 8px; margin-top: 5px; flex-wrap: wrap; }
.fd-tag {
  font-size: 11px; font-weight: 700; padding: 2px 8px;
  border: 1px solid var(--hair); color: var(--gray); background: #fff;
}
.fd-tag.suggestion { border-color: var(--ink); color: var(--ink); }
.fd-tag.bug { border-color: var(--red); color: var(--red); }
.fd-status-pill { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; padding: 2px 8px; border: 1px solid var(--hair); background: #fff; color: var(--gray); }
.fd-status-dot2 { width: 6px; height: 6px; border-radius: 0; }
.fd-status-pill.PENDING .fd-status-dot2 { background: #f59e0b; }
.fd-status-pill.REPLIED .fd-status-dot2 { background: #10b981; }
.fd-status-pill.CLOSED .fd-status-dot2 { background: var(--hair); }
.fd-hero-time { font-size: 11px; color: #a8a8ab; margin-top: 10px; font-variant-numeric: tabular-nums; }

/* 分区 */
.fd-section { padding: 16px 20px; }
.fd-section-label {
  font-size: 11px; font-weight: 700; color: var(--gray);
  letter-spacing: 0.14em; margin-bottom: 8px;
}
.fd-content-box {
  padding: 14px 16px;
  background: #fff;
  border: 1px solid var(--hair);
  font-size: 14px; line-height: 1.7; color: var(--ink);
  white-space: pre-wrap; word-break: break-word;
}
.fd-images { display: flex; flex-wrap: wrap; gap: 8px; }
.fd-img { width: 80px; height: 80px; border: 1px solid var(--hair); cursor: pointer; object-fit: cover; display: block; }
.fd-img:hover { border-color: var(--ink); }

/* 回复区 */
.fd-reply-box {
  background: #fff;
  border: 1px solid var(--hair);
  border-left: 3px solid var(--ink);
  padding: 12px 14px;
}
.fd-reply-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.fd-reply-avatar {
  width: 26px; height: 26px;
  background: var(--ink); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; overflow: hidden; flex-shrink: 0;
}
.fd-reply-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fd-reply-name { font-weight: 700; font-size: 13px; color: var(--ink); }
.fd-reply-time { font-size: 11px; color: #a8a8ab; margin-left: auto; font-variant-numeric: tabular-nums; }
.fd-reply-text { font-size: 13px; line-height: 1.7; color: #3d3d40; white-space: pre-wrap; word-break: break-word; }

/* 管理员操作区 */
.fd-admin-box { border-top: 1px solid var(--hair); margin-top: 8px; }
.fd-reply-input {
  width: 100%;
  border: 1px solid var(--hair);
  border-radius: 0;
  padding: 10px 12px;
  font-size: 14px; line-height: 1.6; color: var(--ink); background: #fff;
  outline: none; font-family: inherit; resize: vertical;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.fd-reply-input:focus { border-color: var(--ink); }
.fd-admin-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.fd-no-reply { padding: 20px; text-align: center; font-size: 13px; color: #a8a8ab; }

/* ===== Tab 过渡 ===== */
.fb-fade-enter-active, .fb-fade-leave-active { transition: opacity 0.18s ease; }
.fb-fade-enter-from, .fb-fade-leave-to { opacity: 0; }

/* ===== 滚动条 ===== */
.fb-content::-webkit-scrollbar { width: 5px; }
.fb-content::-webkit-scrollbar-track { background: transparent; }
.fb-content::-webkit-scrollbar-thumb { background: var(--hair); }
.fb-content::-webkit-scrollbar-thumb:hover { background: var(--gray); }
</style>
