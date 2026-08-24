<template>
  <div class="page">
    <NavBar />

    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">模板市场</h1>
        <p class="hero-desc">选择适合你的排版模板，一键应用到论文</p>
      </div>
    </section>

    <section class="section">
      <div class="section-inner">
        <!-- 官方模板市场 -->
        <h2 class="sec-title">官方模板市场</h2>
        <p class="sec-sub">管理员精选的排版模板，一键复制使用</p>
        <div class="filter-bar">
          <div class="filter-left">
            <span class="filter-label">分类</span>
            <button class="chip" :class="{ on: category === '' }" @click="changeCategory('')">全部</button>
            <button class="chip" v-for="c in categories" :key="c" :class="{ on: category === c }" @click="changeCategory(c)">{{ c }}</button>
          </div>
          <div class="filter-right">
            <span class="filter-label">排序</span>
            <button class="chip" :class="{ on: sort === 'recommended' }" @click="changeSort('recommended')">推荐</button>
            <button class="chip" :class="{ on: sort === 'usage' }" @click="changeSort('usage')">使用量</button>
            <button class="chip" :class="{ on: sort === 'rating' }" @click="changeSort('rating')">评分</button>
            <button class="chip" :class="{ on: sort === 'newest' }" @click="changeSort('newest')">最新</button>
          </div>
          <el-input v-model="keyword" placeholder="搜索模板名称" clearable :prefix-icon="Search" class="search-input" />
        </div>
        <div v-if="filteredMarket.length" class="grid">
          <div class="card" v-for="t in filteredMarket" :key="'p'+t.id" @click="openDetail(t)">
            <div class="card-top">
              <div class="card-type" :class="t.recommended ? '推荐' : '官方'">{{ t.recommended ? '推荐' : '官方' }}</div>
              <div class="card-icon">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/></svg>
              </div>
            </div>
            <h3 class="card-title">{{ t.name }}</h3>
            <p class="card-desc">共 {{ t.ruleCount || 0 }} 条排版规则 · {{ t.category || '未分类' }}</p>
            <div class="card-stats">
              <span class="stars">
                <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(t.ratingAvg || 0) }">★</span>
                <span class="score">{{ (t.ratingAvg || 0).toFixed(1) }}</span>
                <span class="count">({{ t.ratingCount || 0 }})</span>
              </span>
              <span class="usage">使用 {{ t.usageCount || 0 }}</span>
            </div>
            <div class="card-tags">
              <span class="tag" v-if="t.recommended">推荐</span>
              <span class="tag">一键复制</span>
            </div>
            <div class="card-actions">
              <button class="btn-use" @click.stop="useMarket(t)">使用此模板</button>
              <button v-if="isLoggedIn" class="btn-fav" :class="{ on: favSet.has(t.id) }" @click.stop="toggleFav(t)">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor"><path d="M12 21s-6.7-4.35-9.33-8.11C.85 10.16 1.94 6.5 5.16 5.5c1.94-.6 3.98.2 4.84 1.82C10.86 5.7 12.9 4.9 14.84 5.5c3.22 1 4.31 4.66 2.49 7.39C18.7 16.65 12 21 12 21z"/></svg>
              </button>
            </div>
          </div>
        </div>
        <p v-else class="empty">暂无符合条件的模板，你可以先使用内置预设或创建自己的模板</p>

        <!-- 我的收藏 -->
        <template v-if="isLoggedIn && favorites.length">
          <h2 class="sec-title mt">我的收藏</h2>
          <p class="sec-sub">收藏的市场模板，随时复制使用</p>
          <div class="grid">
            <div class="card" v-for="t in favorites" :key="'f'+t.id" @click="openDetail(t)">
              <div class="card-top">
                <div class="card-type 官方">{{ t.category || '收藏' }}</div>
                <div class="card-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/></svg>
                </div>
              </div>
              <h3 class="card-title">{{ t.name }}</h3>
              <p class="card-desc">共 {{ t.ruleCount || 0 }} 条排版规则 · {{ t.category || '未分类' }}</p>
              <div class="card-stats">
                <span class="stars">
                  <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(t.ratingAvg || 0) }">★</span>
                  <span class="score">{{ (t.ratingAvg || 0).toFixed(1) }}</span>
                </span>
                <span class="usage">使用 {{ t.usageCount || 0 }}</span>
              </div>
              <div class="card-actions">
                <button class="btn-use" @click.stop="useMarket(t)">使用此模板</button>
              </div>
            </div>
          </div>
        </template>

        <!-- 预设模板 -->
        <template v-if="!marketTemplates.length">
          <h2 class="sec-title mt">内置预设</h2>
          <p class="sec-sub">按需选择，快速开始</p>
          <div class="grid">
            <div class="card" v-for="(t, i) in presets" :key="'p'+i" @click="usePreset(t)">
              <div class="card-top">
                <div class="card-type" :class="t.tag">{{ t.tag }}</div>
                <div class="card-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/></svg>
                </div>
              </div>
              <h3 class="card-title">{{ t.name }}</h3>
              <p class="card-desc">{{ t.desc }}</p>
              <div class="card-tags">
                <span class="tag" v-for="(f, j) in t.features" :key="j">{{ f }}</span>
              </div>
              <button class="btn-use" @click.stop="usePreset(t)">使用此模板</button>
            </div>
          </div>
        </template>

        <!-- 我的模板 -->
        <template v-if="isLoggedIn">
          <h2 class="sec-title mt">我的模板</h2>
          <p class="sec-sub">你创建的排版模板</p>
          <div v-if="myTemplates.length" class="grid">
            <div class="card mine" v-for="t in myTemplates" :key="'m'+t.id">
              <h3 class="card-title">{{ t.name }}</h3>
              <div class="card-meta">创建于 {{ formatTime(t.createTime) }}</div>
              <div class="card-actions">
                <button class="btn-use" @click="goMy(t.id)">使用此模板</button>
              </div>
            </div>
          </div>
          <p v-else class="empty">还没有模板，去创建一个吧</p>
        </template>
      </div>
    </section>

    <!-- 模板详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail ? detail.name : '模板详情'" width="640px"
      align-center destroy-on-close>
      <div v-if="detail" class="d-body">
        <div class="d-meta">
          <span class="d-tag" v-if="detail.recommended">推荐</span>
          <span class="d-tag gray">{{ detail.category || '未分类' }}</span>
          <span class="d-stat">★ {{ (detail.ratingAvg || 0).toFixed(1) }}（{{ detail.ratingCount || 0 }} 人评分）</span>
          <span class="d-stat">已使用 {{ detail.usageCount || 0 }} 次</span>
        </div>
        <div class="d-grid2">
          <div class="d-item"><span class="d-k">排版规则</span><span class="d-v">{{ detail.ruleCount }} 条</span></div>
          <div class="d-item"><span class="d-k">自动生成目录</span><span class="d-v">{{ detail.generateToc ? '是' : '否' }}</span></div>
        </div>
        <h4 class="d-h">格式规则明细</h4>
        <el-table :data="detail.rules" size="small" border>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">{{ ruleTypeLabel(row.ruleType) }}</template>
          </el-table-column>
          <el-table-column label="字体" min-width="140">
            <template #default="{ row }">{{ row.font || '—' }}<span v-if="row.fontLatin" class="d-muted"> / {{ row.fontLatin }}</span></template>
          </el-table-column>
          <el-table-column label="字号" width="70" align="center">
            <template #default="{ row }">{{ row.fontSize ? row.fontSize + '号' : '—' }}</template>
          </el-table-column>
          <el-table-column label="对齐" width="90" align="center">
            <template #default="{ row }">{{ alignLabel(row.align) }}</template>
          </el-table-column>
          <el-table-column label="行距" width="90">
            <template #default="{ row }">{{ lineSpacingLabel(row) }}</template>
          </el-table-column>
        </el-table>
        <div class="d-actions">
          <el-button type="primary" @click="useMarket(detail)">使用此模板</el-button>
          <el-button v-if="isLoggedIn" :type="favSet.has(detail.id) ? 'warning' : 'default'"
            plain @click="toggleFav(detail)">
            {{ favSet.has(detail.id) ? '取消收藏' : '收藏' }}
          </el-button>
          <el-button v-if="isLoggedIn" plain @click="chooseRate(detail)">评分</el-button>
          <el-button v-if="isLoggedIn" :type="likeState.liked ? 'primary' : 'default'" plain @click="doLike">
            {{ likeState.liked ? '已赞' : '点赞' }}（{{ likeState.likeCount }}）
          </el-button>
        </div>

        <div class="d-comments">
          <h4 class="d-h">评论（{{ comments.length }}）</h4>
          <div v-if="comments.length" class="comment-list">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="c-avatar">{{ (c.nickname || c.username || '?').slice(0, 1) }}</div>
              <div class="c-main">
                <div class="c-head">
                  <span class="c-name">{{ c.nickname || c.username }}</span>
                  <span class="c-time">{{ fmtTime(c.createTime) }}</span>
                </div>
                <div class="c-content">{{ c.content }}</div>
              </div>
              <el-button v-if="isLoggedIn && (c.userId === meId || detail.userId === meId)" link type="danger" size="small" @click="removeComment(c)">删除</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无评论，来抢沙发" :image-size="50" />
          <div v-if="isLoggedIn" class="comment-input">
            <el-input v-model="commentText" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="写下你的评论..." resize="none" />
            <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发布</el-button>
          </div>
          <el-button v-else plain @click="router.push({ path: '/login', query: { redirect: '/template-market' } })">登录后评论</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 评分弹窗 -->
    <el-dialog v-model="rateVisible" title="模板评分" width="340px" align-center destroy-on-close>
      <div class="rate-body">
        <p class="rate-tip">为「{{ rateTarget ? rateTarget.name : '' }}」打分</p>
        <el-rate v-model="rateScore" :max="5" show-text :texts="['很差','较差','一般','较好','很好']" />
      </div>
      <template #footer>
        <el-button @click="rateVisible = false">取消</el-button>
        <el-button type="primary" :loading="rateSubmitting" @click="submitRate">提交评分</el-button>
      </template>
    </el-dialog>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'
import { listTemplates, createTemplate, listMarketTemplates, listMarketCategories, rateMarketTemplate, copyMarketTemplate, getMarketTemplateDetail, toggleFavoriteTemplate, listFavoriteTemplates, listMarketComments, addMarketComment, deleteMarketComment, toggleMarketLike } from '../api/template'

const router = useRouter()
const isLoggedIn = ref(false)
const myTemplates = ref([])
const marketTemplates = ref([])
const favorites = ref([])
const favSet = ref(new Set())
const categories = ref([])
const category = ref('')
const sort = ref('recommended')
const keyword = ref('')
const detailVisible = ref(false)
const detail = ref(null)
const comments = ref([])
const commentText = ref('')
const likeState = ref({ liked: false, likeCount: 0 })
const commentSubmitting = ref(false)
const meId = computed(() => Number(localStorage.getItem('userId')) || 0)
const filteredMarket = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return marketTemplates.value
  return marketTemplates.value.filter(t => (t.name || '').toLowerCase().includes(kw))
})

const presets = [
  {
    name: '本科毕业论文',
    tag: '本科',
    desc: '适合本科毕业论文的通用排版规范，标题层级清晰、正文规范。',
    features: ['黑体标题', '宋体正文', '三线表', '图表编号']
  },
  {
    name: '硕士毕业论文',
    tag: '硕士',
    desc: '面向硕士论文的排版模板，包含摘要、目录、参考文献规范。',
    features: ['独立摘要页', '目录生成', 'GB/T 7714', '页眉页脚']
  },
  {
    name: '期刊投稿模板',
    tag: '期刊',
    desc: '适合学术期刊投稿的排版格式，符合主流期刊要求。',
    features: ['双栏可选', '文献引用', '图表规范', '作者信息']
  },
  {
    name: '博士毕业论文',
    tag: '博士',
    desc: '博士论文全流程排版，覆盖封面、声明、答辩等完整结构。',
    features: ['封面/声明', '中英文摘要', '复杂图表', '全文排版']
  }
]

onMounted(async () => {
  try {
    categories.value = (await listMarketCategories()) || []
  } catch (e) {}
  await loadMarket()
  isLoggedIn.value = !!localStorage.getItem('token')
  if (isLoggedIn.value) {
    try {
      myTemplates.value = (await listTemplates()) || []
    } catch (e) {}
    await loadFavorites()
  }
})

async function loadFavorites() {
  try {
    favorites.value = (await listFavoriteTemplates()) || []
    favSet.value = new Set(favorites.value.map(f => f.id))
  } catch (e) {}
}

async function toggleFav(t) {
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/template-market' } })
    return
  }
  try {
    const res = await toggleFavoriteTemplate(t.id)
    const on = res.favorited
    const next = new Set(favSet.value)
    if (on) next.add(t.id); else next.delete(t.id)
    favSet.value = next
    await loadFavorites()
    ElMessage.success(on ? '已收藏' : '已取消收藏')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function openDetail(t) {
  try {
    detail.value = await getMarketTemplateDetail(t.id)
    detailVisible.value = true
    comments.value = []
    commentText.value = ''
    likeState.value = { liked: false, likeCount: 0 }
    loadComments(t.id)
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function loadComments(id) {
  try { comments.value = (await listMarketComments(id)) || [] } catch (e) { comments.value = [] }
}

function fmtTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

const ruleTypeLabel = t => ({
  heading1: '一级标题', heading2: '二级标题', heading3: '三级标题',
  body: '正文', figure: '图题注', table: '表题注'
}[t] || t || '—')

const alignLabel = a => ({
  left: '左对齐', center: '居中', right: '右对齐', justify: '两端对齐'
}[a] || a || '—')

const lineSpacingLabel = r => {
  if (r.lineSpacingType === 'exact') return r.lineSpacingExact ? r.lineSpacingExact + ' 磅' : '—'
  if (r.lineSpacing) return r.lineSpacing + ' 倍'
  return '—'
}

async function loadMarket() {
  try {
    marketTemplates.value = (await listMarketTemplates({ category: category.value || undefined, sort: sort.value })) || []
  } catch (e) {
    marketTemplates.value = []
  }
}

function changeCategory(c) {
  category.value = c
  loadMarket()
}

function changeSort(s) {
  sort.value = s
  loadMarket()
}

const rateVisible = ref(false)
const rateScore = ref(5)
const rateTarget = ref(null)
const rateSubmitting = ref(false)

/** 登录用户点击评分: 打开星级弹窗 */
function chooseRate(t) {
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/template-market' } })
    return
  }
  rateTarget.value = t
  rateScore.value = 5
  rateVisible.value = true
}

async function submitRate() {
  if (!rateTarget.value || !rateScore.value) {
    ElMessage.warning('请选择评分')
    return
  }
  rateSubmitting.value = true
  try {
    await rateMarketTemplate(rateTarget.value.id, rateScore.value)
    ElMessage.success('评分成功')
    rateVisible.value = false
    loadMarket()
  } catch (e) {
    ElMessage.error(e.message || '评分失败')
  } finally {
    rateSubmitting.value = false
  }
}

async function doLike() {
  if (!detail.value) return
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/template-market' } })
    return
  }
  try {
    likeState.value = (await toggleMarketLike(detail.value.id)) || likeState.value
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function submitComment() {
  if (!detail.value) return
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/template-market' } })
    return
  }
  const text = commentText.value.trim()
  if (!text) { ElMessage.warning('请输入评论内容'); return }
  commentSubmitting.value = true
  try {
    await addMarketComment(detail.value.id, text)
    commentText.value = ''
    await loadComments(detail.value.id)
    ElMessage.success('评论已发布')
  } catch (e) {
    ElMessage.error(e.message || '评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

async function removeComment(c) {
  try {
    await deleteMarketComment(c.id)
    await loadComments(detail.value.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function useMarket(t) {
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/template-market' } })
    return
  }
  try {
    const data = await copyMarketTemplate(t.id)
    ElMessage.success('模板已复制到我的模板')
    router.push(`/template/${data.id}`)
  } catch (e) {
    ElMessage.error(e.message || '复制模板失败')
  }
}

async function usePreset(preset) {
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: '/templates' } })
    return
  }
  try {
    const data = await createTemplate(preset.name)
    ElMessage.success('模板已创建，开始配置规则')
    router.push(`/template/${data.id}`)
  } catch (e) {
    ElMessage.error(e.message || '创建模板失败')
  }
}

function goMy(id) {
  router.push(`/template/${id}`)
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.page {
  --c-primary: #3B6BFF;
  --c-primary-dark: #2D52CC;
  --c-dark: #1a1a2e;
  --c-text: #374151;
  --c-text2: #6b7280;
  --c-text3: #9ca3af;
  --c-border: #e5e7eb;
  --c-bg2: #f8f9fc;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--c-text);
  background: #fff;
  min-height: 100vh;
}
.hero {
  background: linear-gradient(180deg, #f0f4ff 0%, #fff 100%);
  padding: 64px 32px;
  text-align: center;
}
.hero-title {
  font-size: 38px;
  font-weight: 800;
  color: var(--c-dark);
  margin: 0 0 10px;
}
.hero-desc {
  font-size: 16px;
  color: var(--c-text2);
  margin: 0;
}
.section {
  padding: 56px 32px;
}
.section-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.sec-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--c-dark);
  text-align: center;
  margin: 0 0 8px;
}
.sec-title.mt {
  margin-top: 56px;
}
.sec-sub {
  text-align: center;
  color: var(--c-text2);
  margin: 0 0 36px;
  font-size: 15px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}
.card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.card:hover {
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
  transform: translateY(-4px);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.card-type {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
}
.card-type.本科 { background: #EEF1FF; color: #3B6BFF; }
.card-type.硕士 { background: #F5F3FF; color: #7c3aed; }
.card-type.期刊 { background: #ECFDF5; color: #10b981; }
.card-type.博士 { background: #FFF7ED; color: #f59e0b; }
.card-type.官方 { background: #EEF1FF; color: #3B6BFF; }
.card-type.推荐 { background: linear-gradient(135deg, #FFF7ED, #FEF3C7); color: #b45309; }
.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #EEF1FF;
  color: #3B6BFF;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--c-dark);
  margin: 0 0 8px;
}
.card-desc {
  font-size: 13px;
  color: var(--c-text2);
  line-height: 1.6;
  margin: 0 0 14px;
  min-height: 42px;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.card-actions {
  display: flex;
  gap: 8px;
}
.btn-use {
  flex: 1;
  padding: 9px 0;
  border: none;
  border-radius: 8px;
  background: var(--c-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-use:hover {
  background: var(--c-primary-dark);
}
.btn-fav {
  width: 38px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: #fff;
  color: #c0c4cc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.btn-fav:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}
.btn-fav.on {
  color: #f56c6c;
  border-color: #f56c6c;
  background: #fef0f0;
}
.d-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.d-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.d-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #FFF7ED, #FEF3C7);
  color: #b45309;
}
.d-tag.gray {
  background: #f3f4f6;
  color: #6b7280;
}
.d-stat {
  font-size: 13px;
  color: var(--c-text2);
}
.d-grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 20px;
  background: #f8f9fc;
  border-radius: 10px;
  padding: 12px 16px;
}
.d-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
  align-items: baseline;
}
.d-k {
  color: var(--c-text3);
  flex-shrink: 0;
}
.d-v {
  color: var(--c-text);
}
.d-h {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--c-dark);
}
.d-muted {
  color: var(--c-text3);
}
.d-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}
.rate-body {
  text-align: center;
  padding: 8px 0 4px;
}
.rate-tip {
  color: var(--c-text2);
  font-size: 14px;
  margin: 0 0 16px;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 0 24px;
}
.filter-left, .filter-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.search-input {
  width: 220px;
}
.filter-label {
  font-size: 13px;
  color: var(--c-text2);
  margin-right: 2px;
}
.chip {
  border: 1px solid var(--c-border);
  background: #fff;
  border-radius: 999px;
  padding: 4px 14px;
  font-size: 13px;
  color: var(--c-text2);
  cursor: pointer;
  transition: all 0.2s;
}
.chip:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}
.chip.on {
  background: var(--c-primary);
  border-color: var(--c-primary);
  color: #fff;
}
.card-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
}
.star {
  color: #d1d5db;
  font-size: 15px;
}
.star.filled {
  color: #f59e0b;
}
.score {
  font-size: 13px;
  font-weight: 700;
  color: #b45309;
  margin-left: 4px;
}
.count {
  font-size: 12px;
  color: var(--c-text3);
}
.usage {
  font-size: 12px;
  color: var(--c-text3);
}
.card.mine .card-meta {
  font-size: 12px;
  color: var(--c-text3);
  margin-bottom: 14px;
}
.tag {
  font-size: 11px;
  color: var(--c-text2);
  background: var(--c-bg2);
  border: 1px solid var(--c-border);
  padding: 2px 8px;
  border-radius: 999px;
}
.empty {
  text-align: center;
  color: var(--c-text3);
  padding: 40px 0;
}
@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr 1fr; }
}
.d-comments {
  margin-top: 16px;
}
.d-comments .d-h {
  margin: 0 0 10px;
  font-size: 14px;
  color: #1a1a2e;
}
.comment-list {
  max-height: 280px;
  overflow: auto;
}
.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f1f5;
}
.c-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #EEF1FF;
  color: #3B6BFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}
.c-main {
  flex: 1;
  min-width: 0;
}
.c-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}
.c-name {
  font-size: 13px;
  font-weight: 600;
  color: #2c3140;
}
.c-time {
  font-size: 12px;
  color: #c0c4cc;
}
.c-content {
  font-size: 13px;
  color: #4a4f5e;
  line-height: 1.6;
  word-break: break-word;
}
.comment-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 12px;
}
.comment-input .el-button {
  flex-shrink: 0;
}
</style>
