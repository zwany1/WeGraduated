<template>
  <!-- 右侧抽屉(CC Switch 风格: 不遮挡页面内容) -->
  <el-drawer v-model="show" direction="rtl" size="720px" :with-header="false" :close-on-click-modal="true" append-to-body @open="onOpen">
    <div class="fb-drawer">
      <!-- 头部 -->
      <div class="fb-header">
        <div class="fb-header-left">
          <span class="fb-header-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" /></svg>
          </span>
          <span class="fb-header-title">用户反馈</span>
        </div>
        <span class="fb-header-count" v-if="activeTab === 'wall'">{{ total }} 条</span>
      </div>
      <!-- Tab 切换(胶囊式) -->
      <div class="fb-tabs">
        <div class="fb-tabs-track">
          <span class="fb-tabs-indicator" :class="{ right: activeTab === 'wall' }"></span>
          <button class="fb-tab" :class="{ active: activeTab === 'form' }" @click="activeTab = 'form'">
            <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 4v9a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1h6"/><path d="M8 11h.01M8 8h.01"/></svg>
            提交反馈
          </button>
          <button class="fb-tab" :class="{ active: activeTab === 'wall' }" @click="activeTab = 'wall'; load()">
            <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M3 4h10M3 8h10M3 12h6"/></svg>
            反馈墙
          </button>
        </div>
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
            <textarea v-model="form.content" class="fb-textarea" rows="4" maxlength="2000" placeholder="说说你遇到的问题或建议... (支持粘贴图片)"></textarea>
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
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/></svg>
              <span>上传</span>
            </div>
          </div>
          <input ref="imageInput" type="file" accept="image/*" multiple style="display:none" @change="onImageChange" />
          <div class="fb-form-foot">
            <el-button type="primary" :loading="submitting" @click="submit">提交反馈</el-button>
          </div>
        </div>
        </transition>

        <!-- 反馈墙(DepthFold 3D 卡片) -->
        <transition name="fb-fade" mode="out-in">
        <div v-if="activeTab === 'wall'" key="wall" class="fb-wall">
          <div v-loading="loading" class="df-grid">
            <div v-for="(row, i) in rows" :key="row.id" class="df-parent" :class="row.category" :style="{ '--stagger': i * 0.06 + 's', '--df-grad': gradFor(i), '--df-glow': glowFor(i) }">
              <div class="df-card" @click="openDetail(row.id)">
                <div class="df-logo">
                  <span class="df-circle"></span><span class="df-circle"></span><span class="df-circle"></span><span class="df-circle"></span>
                  <span class="df-circle df-circle-icon">
                    <span class="df-avatar-mini">
                      <img v-if="row.avatar" :src="row.avatar" class="df-avatar-img-mini" />
                      <span v-else>{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</span>
                    </span>
                  </span>
                </div>
                <div class="df-glass"></div>
                <div class="df-content">
                  <span class="df-title">{{ row.nickname || row.username || '匿名用户' }}</span>
                  <span class="df-cat" :class="row.category">{{ categoryLabel(row.category) }} · {{ formatTime(row.createTime) }}</span>
                  <span class="df-text">{{ row.content }}</span>
                  <span v-if="row.images && row.images.length" class="df-imgs">{{ row.images.length }} 张图片</span>
                  <span v-if="row.reply" class="df-reply">管理员已回复</span>
                  <span class="df-status" :class="row.status"></span>
                </div>
                <div class="df-bottom">
                  <div class="df-social">
                    <button type="button" class="df-social-btn" @click.stop="openDetail(row.id)">详情</button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="!loading && rows.length === 0" class="fb-empty">
              <div class="fb-empty-icon">
                <svg viewBox="0 0 48 48" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M24 6l4 12 12 4-12 4-4 12-4-12-12-4 12-4 4-12z" opacity="0.3"/><circle cx="24" cy="24" r="16" stroke-dasharray="4 4" opacity="0.2"/><path d="M18 22c2 2 4 2 6 0M30 22c-2 2-4 2-6 0" opacity="0.4"/></svg>
              </div>
              <div class="fb-empty-text">还没有反馈</div>
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

  <!-- 详情抽屉(重构) -->
  <el-drawer v-model="detailShow" direction="rtl" size="440px" :with-header="false" append-to-body>
    <div class="fb-detail">
      <div v-if="detailLoading" class="fb-detail-loading">加载中...</div>
      <template v-if="detail">
        <!-- 头部(3D 渐变卡片) -->
        <div class="fd-hero" :style="{ background: detail._grad || GRADIENTS[0] }">
          <div class="fd-hero-glass"></div>
          <div class="fd-hero-content">
            <span class="fd-hero-avatar">
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
                <span class="fd-hero-time">{{ formatTime(detail.createTime) }}</span>
              </div>
            </div>
          </div>
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
            <img v-for="(img, i) in detail.images" :key="i" :src="img" class="fd-img" fit="cover" @click="previewImages(detail.images, i)" />
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

// 随机渐变色池(每张卡片不同颜色)
const GRADIENTS = [
  'linear-gradient(135deg, #00ffd6, #08e260)',
  'linear-gradient(145deg, #a855f7, #6366f1, #ec4899)',
  'linear-gradient(135deg, #fbbf24, #f97316)',
  'linear-gradient(155deg, #22d3ee, #0284c7)',
  'linear-gradient(135deg, #f472b6, #ef4444)',
  'linear-gradient(160deg, #0f172a, #1e1b4b, #312e81)',
  'linear-gradient(145deg, #34d399, #3b82f6)',
  'linear-gradient(135deg, #fbbf24, #ec4899)',
  'linear-gradient(155deg, #8b5cf6, #06b6d4)',
  'linear-gradient(135deg, #10b981, #f59e0b)',
]
function gradFor(i) { return GRADIENTS[i % GRADIENTS.length] }
function glowFor(i) {
  const g = [
    'rgba(0,255,214,0.15)', 'rgba(192,132,252,0.15)', 'rgba(251,191,36,0.15)',
    'rgba(34,211,238,0.15)', 'rgba(244,63,94,0.15)', 'rgba(167,139,250,0.15)',
    'rgba(52,211,153,0.15)', 'rgba(251,113,133,0.15)', 'rgba(139,92,246,0.15)',
    'rgba(16,185,129,0.15)',
  ]
  return g[i % g.length]
}

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
/* ===== 抽屉容器(后台暖色系) ===== */
.fb-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f7f7f4;
}

/* ===== 头部 ===== */
.fb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 16px;
}
.fb-header-left { display: flex; align-items: center; gap: 10px; }
.fb-header-icon {
  width: 38px; height: 38px; border-radius: 10px;
  background: linear-gradient(135deg, #3b6bff, #7c3aed);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(59,107,255,0.3);
}
.fb-header-title { font-size: 19px; font-weight: 700; color: #3a3320; letter-spacing: -0.3px; }
.fb-header-count { font-size: 13px; color: #9a917d; background: #fffdf9; border: 1px solid #ece7da; padding: 3px 12px; border-radius: 999px; }

/* ===== Tab(胶囊式) ===== */
.fb-tabs { padding: 0 28px 16px; }
.fb-tabs-track {
  display: flex; position: relative; background: #fffdf9; border: 1px solid #ece7da;
  border-radius: 10px; padding: 3px;
}
.fb-tabs-indicator {
  position: absolute; top: 3px; bottom: 3px; left: 3px; width: calc(50% - 3px);
  border-radius: 8px; background: #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fb-tabs-indicator.right { transform: translateX(100%); }
.fb-tab {
  flex: 1; padding: 8px 0; font-size: 14px; font-weight: 600; color: #9a917d;
  background: none; border: none; cursor: pointer; position: relative; z-index: 1;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.fb-tab:hover { color: #3b6bff; }
.fb-tab.active { color: #3b6bff; }

/* ===== 内容区 ===== */
.fb-content { flex: 1; overflow-y: auto; padding: 20px 28px; }

/* ===== 表单(白卡片) ===== */
.fb-form {
  background: #fff; border-radius: 14px; padding: 24px;
  border: 1px solid #ece7da; box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}
.fb-field-label {
  font-size: 13px; font-weight: 600; color: #4a4332; margin-bottom: 8px; margin-top: 18px;
}
.fb-field-label:first-child { margin-top: 0; }

/* 彩色 chip */
.fb-chips { display: flex; gap: 8px; margin-bottom: 4px; }
.fb-chip {
  padding: 8px 16px; border-radius: 999px; border: 1.5px solid #ece7da;
  background: #fff; font-size: 13px; font-weight: 500; color: #9a917d;
  cursor: pointer; display: flex; align-items: center; gap: 6px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.fb-chip-dot { width: 8px; height: 8px; border-radius: 50%; transition: all 0.25s; }
.fb-chip:hover { border-color: #c7d2fe; transform: translateY(-1px); }
.fb-chip.active { border-color: transparent; color: #fff; transform: scale(1.02); }
.fb-chip.active.suggestion { background: #3b82f6; box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
.fb-chip.active.bug { background: #f43f5e; box-shadow: 0 4px 12px rgba(244,63,94,0.3); }
.fb-chip.active.other { background: #8b5cf6; box-shadow: 0 4px 12px rgba(139,92,246,0.3); }
.fb-chip.active .fb-chip-dot { background: #fff; }
.fb-chip.suggestion .fb-chip-dot { background: #3b82f6; }
.fb-chip.bug .fb-chip-dot { background: #f43f5e; }
.fb-chip.other .fb-chip-dot { background: #8b5cf6; }

/* 编辑器(内容+字数) */
.fb-editor { position: relative; }
.fb-textarea {
  width: 100%; border: 1.5px solid #ece7da; border-radius: 10px; padding: 12px 14px;
  font-size: 14px; line-height: 1.6; color: #3a3320; background: #fffdf9;
  resize: vertical; outline: none; font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.fb-textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); background: #fff; }
.fb-textarea:hover { border-color: #c7d2fe; }
.fb-editor-count {
  position: absolute; right: 10px; bottom: 6px; font-size: 11px; color: #bbb;
  pointer-events: none;
}
.fb-input {
  width: 100%; border: 1.5px solid #ece7da; border-radius: 10px; padding: 10px 14px;
  font-size: 14px; color: #3a3320; background: #fffdf9; outline: none;
  font-family: inherit; transition: border-color 0.2s, box-shadow 0.2s;
}
.fb-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); background: #fff; }
.fb-input:hover { border-color: #c7d2fe; }

/* 图片区 */
.fb-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
.fb-img-item { position: relative; width: 68px; height: 68px; border-radius: 10px; overflow: hidden; border: 1px solid #ece7da; animation: fbImgIn 0.35s cubic-bezier(0.34,1.56,0.64,1); }
@keyframes fbImgIn { from { transform: scale(0.6); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.fb-img-thumb { width: 100%; height: 100%; object-fit: cover; transition: transform 0.2s; }
.fb-img-item:hover .fb-img-thumb { transform: scale(1.1); }
.fb-img-del { position: absolute; top: 0; right: 0; width: 20px; height: 20px; background: rgba(0,0,0,0.6); color: #fff; font-size: 13px; line-height: 20px; text-align: center; cursor: pointer; border-radius: 0 10px 0 6px; opacity: 0; transition: opacity 0.2s; }
.fb-img-item:hover .fb-img-del { opacity: 1; }
.fb-img-add {
  width: 68px; height: 68px; border: 1.5px dashed #c0c4cc; border-radius: 10px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  font-size: 11px; color: #9a917d; cursor: pointer; background: #fffdf9;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.fb-img-add:hover { border-color: #3b6bff; color: #3b6bff; transform: scale(1.05); background: #eff6ff; }
.fb-form-foot { text-align: right; margin-top: 20px; }
.fb-form-foot :deep(.el-button--primary) {
  background: linear-gradient(135deg, #3b6bff, #7c3aed); border: none;
  box-shadow: 0 4px 14px rgba(59,107,255,0.3);
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
}
.fb-form-foot :deep(.el-button--primary:hover) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(59,107,255,0.4); }

/* ===== 反馈墙(DepthFold 3D 卡片) ===== */
.fb-wall { min-height: 200px; }
.df-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
  gap: 16px;
  padding: 8px 0 16px;
}
@media (min-width: 600px) { .df-grid { grid-template-columns: repeat(2, 1fr); } }

/* 3D 父容器(perspective) */
.df-parent {
  width: 100%;
  height: 260px;
  perspective: 1000px;
  perspective-origin: 50% 50%;
  filter: drop-shadow(0 20px 40px rgba(0,0,0,0.15));
  animation: dfIn 0.5s cubic-bezier(0.34,1.2,0.64,1) both;
  animation-delay: var(--stagger, 0s);
}
@keyframes dfIn { from { opacity: 0; transform: translateY(20px) scale(0.95); } to { opacity: 1; transform: none; } }

/* 卡片主体 */
.df-card {
  position: relative;
  height: 100%;
  border-radius: 36px;
  background: var(--df-grad);
  transform-style: preserve-3d;
  transition: transform 0.55s cubic-bezier(0.22,1,0.36,1), box-shadow 0.55s ease;
  box-shadow: rgba(0,0,0,0.12) 0 20px 25px -8px;
  cursor: pointer;
  overflow: hidden;
}
.df-parent:hover .df-card {
  transform: rotate3d(1, 1, 0, 28deg);
  box-shadow: rgba(0,0,0,0.2) 20px 40px 25px -18px, var(--df-glow) 0 0 60px -10px;
}

/* 分类不再覆盖渐变(每张卡片随机色, 分类只影响标签) */
.df-parent.suggestion { --df-title: #0f172a; --df-body: rgba(15,23,42,0.85); --df-cta: #3b82f6; }
.df-parent.bug { --df-title: #0f172a; --df-body: rgba(15,23,42,0.85); --df-cta: #ef4444; }
.df-parent.other { --df-title: #0f172a; --df-body: rgba(15,23,42,0.85); --df-cta: #8b5cf6; }

/* 玻璃层(3D translateZ) */
.df-glass {
  transform-style: preserve-3d;
  position: absolute; inset: 8px;
  border-radius: 40px;
  border-top-right-radius: 100%;
  background: linear-gradient(0deg, rgba(255,255,255,0.38), rgba(255,255,255,0.82));
  transform: translate3d(0, 0, 25px);
  border-left: 1px solid rgba(255,255,255,0.85);
  border-bottom: 1px solid rgba(255,255,255,0.75);
  transition: all 0.5s ease-in-out;
  pointer-events: none;
}

/* Logo 轨道圆(3D 层叠) */
.df-logo { position: absolute; right: 0; top: 0; transform-style: preserve-3d; pointer-events: none; z-index: 2; }
.df-circle {
  display: block; position: absolute; aspect-ratio: 1; border-radius: 50%;
  top: 0; right: 0; background: rgba(255,255,255,0.3);
  box-shadow: rgba(100,100,111,0.25) -10px 10px 24px 0;
  backdrop-filter: blur(6px); transition: all 0.5s ease-in-out;
}
.df-circle:nth-child(1) { width: 140px; transform: translate3d(0,0,20px); top: 6px; right: 6px; }
.df-circle:nth-child(2) { width: 110px; transform: translate3d(0,0,40px); top: 10px; right: 10px; transition-delay: 0.05s; }
.df-circle:nth-child(3) { width: 80px; transform: translate3d(0,0,60px); top: 16px; right: 16px; transition-delay: 0.1s; }
.df-circle:nth-child(4) { width: 56px; transform: translate3d(0,0,80px); top: 22px; right: 22px; transition-delay: 0.15s; }
.df-circle-icon {
  width: 44px !important; transform: translate3d(0,0,100px) !important;
  top: 28px !important; right: 28px !important;
  display: grid !important; place-content: center;
  transition-delay: 0.2s;
}
.df-parent:hover .df-circle:nth-child(2) { transform: translate3d(0,0,60px); }
.df-parent:hover .df-circle:nth-child(3) { transform: translate3d(0,0,80px); }
.df-parent:hover .df-circle:nth-child(4) { transform: translate3d(0,0,100px); }
.df-parent:hover .df-circle-icon { transform: translate3d(0,0,120px) !important; }

/* 头像迷你 */
.df-avatar-mini { width: 28px; height: 28px; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: #3b6bff; background: #eef2ff; }
.df-avatar-img-mini { width: 100%; height: 100%; object-fit: cover; }

/* 内容(3D 层) */
.df-content {
  padding: 60px 1.5rem 0 1.25rem;
  transform: translate3d(0, 0, 26px);
  position: relative; z-index: 3;
  display: flex; flex-direction: column; gap: 6px;
}
.df-title { font-weight: 800; font-size: 1.1rem; letter-spacing: -0.02em; color: var(--df-title, #0f172a); }
.df-cat { font-size: 0.75rem; font-weight: 700; opacity: 0.8; color: var(--df-title, #0f172a); }
.df-text {
  font-size: 0.85rem; line-height: 1.5; font-weight: 600; color: var(--df-body, rgba(15,23,42,0.85));
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.df-imgs { font-size: 0.75rem; opacity: 0.7; color: var(--df-title, #0f172a); }
.df-reply { font-size: 0.75rem; font-weight: 700; color: var(--df-title, #0f172a); opacity: 0.8; }
.df-status { width: 8px; height: 8px; border-radius: 50%; position: absolute; top: 14px; right: 14px; transform: translate3d(0,0,30px); }
.df-status.PENDING { background: #f59e0b; animation: fbPulse 2s infinite; }
.df-status.REPLIED { background: #10b981; box-shadow: 0 0 0 2px rgba(16,185,129,0.15); }
.df-status.CLOSED { background: #d1d5db; }

/* 底部操作栏 */
.df-bottom {
  position: absolute; bottom: 16px; left: 16px; right: 16px;
  display: flex; align-items: center; justify-content: space-between;
  transform: translate3d(0, 0, 26px); z-index: 4;
}
.df-social-btn {
  background: #fff; border-radius: 50%; border: none; padding: 8px 14px;
  font-weight: 700; font-size: 0.75rem; cursor: pointer;
  box-shadow: rgba(0,0,0,0.2) 0 6px 6px -4px; color: var(--df-cta, #3b6bff);
  transition: transform 0.2s, box-shadow 0.2s;
}
.df-parent:hover .df-social-btn { transform: translate3d(0,0,50px); box-shadow: rgba(0,0,0,0.15) -4px 16px 10px 0; }
.df-social-btn:hover { background: #0f172a; color: #fff; }

/* ===== 空状态 ===== */
.fb-empty { display: flex; flex-direction: column; align-items: center; padding: 60px 0 40px; }
.fb-empty-icon { color: #c0c4cc; margin-bottom: 12px; animation: fbEmptyFloat 3s ease-in-out infinite; }
@keyframes fbEmptyFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
.fb-empty-text { font-size: 15px; font-weight: 600; color: #9a917d; }
.fb-empty-sub { font-size: 13px; color: #c0c4cc; margin-top: 4px; }

/* ===== 详情(重构) ===== */
.fb-detail {
  padding: 0;
  height: 100%;
  overflow-y: auto;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.fb-detail-loading { padding: 40px; text-align: center; color: #909399; }

/* 头部渐变 3D */
.fd-hero {
  position: relative;
  padding: 24px 20px 20px;
  overflow: hidden;
  border-radius: 0 0 24px 24px;
}
.fd-hero-glass {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.25), rgba(255,255,255,0.05));
  backdrop-filter: blur(8px);
  pointer-events: none;
}
.fd-hero-content { position: relative; display: flex; align-items: center; gap: 12px; }
.fd-hero-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 800; color: #3b6bff; overflow: hidden; flex-shrink: 0;
  border: 2px solid rgba(255,255,255,0.8); box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.fd-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fd-hero-info { flex: 1; min-width: 0; }
.fd-hero-name { font-weight: 800; font-size: 17px; color: #fff; text-shadow: 0 1px 4px rgba(0,0,0,0.3); }
.fd-hero-tags { display: flex; align-items: center; gap: 6px; margin-top: 4px; flex-wrap: wrap; }
.fd-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 999px; background: rgba(255,255,255,0.9); color: #0f172a; }
.fd-tag.suggestion { color: #3b82f6; } .fd-tag.bug { color: #ef4444; } .fd-tag.other { color: #8b5cf6; }
.fd-status-pill { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; padding: 2px 8px; border-radius: 999px; background: rgba(255,255,255,0.85); color: #0f172a; }
.fd-status-dot2 { width: 6px; height: 6px; border-radius: 50%; }
.fd-status-pill.PENDING .fd-status-dot2 { background: #f59e0b; animation: fbPulse 2s infinite; }
.fd-status-pill.REPLIED .fd-status-dot2 { background: #10b981; }
.fd-status-pill.CLOSED .fd-status-dot2 { background: #d1d5db; }
.fd-hero-time { font-size: 11px; color: rgba(255,255,255,0.8); margin-left: auto; }

/* 分区 */
.fd-section { padding: 16px 20px; }
.fd-section-label { font-size: 12px; font-weight: 700; color: #909399; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px; }
.fd-content-box {
  padding: 14px 16px; background: #f7f7f4; border: 1px solid #ece7da;
  border-radius: 12px; font-size: 14px; line-height: 1.7; color: #3a3320;
  white-space: pre-wrap; word-break: break-word;
}
.fd-images { display: flex; flex-wrap: wrap; gap: 8px; }
.fd-img { width: 80px; height: 80px; border-radius: 10px; border: 1px solid #ece7da; cursor: pointer; transition: transform 0.2s; }
.fd-img:hover { transform: scale(1.05); }

/* 回复区 */
.fd-reply-box {
  background: #f6f9ff; border: 1px solid #dbeafe; border-left: 3px solid #3b82f6;
  border-radius: 0 12px 12px 0; padding: 12px 14px;
}
.fd-reply-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.fd-reply-avatar {
  width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #3b6bff, #7c3aed);
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700;
  overflow: hidden; flex-shrink: 0;
}
.fd-reply-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.fd-reply-name { font-weight: 700; font-size: 13px; color: #3b6bff; }
.fd-reply-time { font-size: 11px; color: #bbb; margin-left: auto; }
.fd-reply-text { font-size: 13px; line-height: 1.7; color: #4a4332; white-space: pre-wrap; word-break: break-word; }

/* 管理员操作区 */
.fd-admin-box { background: #faf8f1; border-top: 1px solid #ece7da; margin-top: 8px; }
.fd-reply-input {
  width: 100%; border: 1.5px solid #ece7da; border-radius: 10px; padding: 10px 12px;
  font-size: 14px; line-height: 1.6; color: #3a3320; background: #fffdf9;
  outline: none; font-family: inherit; resize: vertical; transition: border-color 0.2s, box-shadow 0.2s;
}
.fd-reply-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.fd-reply-input:hover { border-color: #c7d2fe; }
.fd-admin-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.fd-no-reply { padding: 20px; text-align: center; font-size: 13px; color: #bbb; }

/* ===== Tab 过渡 ===== */
.fb-fade-enter-active, .fb-fade-leave-active { transition: opacity 0.2s cubic-bezier(0.4,0,0.2,1), transform 0.2s cubic-bezier(0.4,0,0.2,1); }
.fb-fade-enter-from { opacity: 0; transform: translateX(12px); }
.fb-fade-leave-to { opacity: 0; transform: translateX(-12px); }

/* ===== 滚动条 ===== */
.fb-content::-webkit-scrollbar { width: 5px; }
.fb-content::-webkit-scrollbar-track { background: transparent; }
.fb-content::-webkit-scrollbar-thumb { background: #ece7da; border-radius: 3px; }
.fb-content::-webkit-scrollbar-thumb:hover { background: #d4ccbe; }
</style>
