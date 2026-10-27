<template>
  <div class="market">
    <PublicNav />

    <!-- 海报式页头 -->
    <section class="m-hero">
      <div class="m-hero-main">
        <h1>模板市场</h1>
        <p>别人调好的校规配置，直接拿来用。也可以把自己那套上架，让同校的人少走一遍。</p>
      </div>
      <div class="m-hero-side">
        <span class="m-stat"><b>{{ marketTemplates.length }}</b> 套官方模板</span>
        <span class="m-stat"><b>{{ categories.length }}</b> 个分类</span>
        <span class="m-stat" v-if="isLoggedIn"><b>{{ favorites.length }}</b> 个收藏</span>
      </div>
    </section>

    <!-- 筛选 -->
    <section class="m-filter">
      <div class="flt">
        <div class="flt-chips">
          <button :class="['chip', { on: category === '' }]" type="button" @click="changeCategory('')">全部分类</button>
          <button v-for="c in categories" :key="c" :class="['chip', { on: category === c }]" type="button" @click="changeCategory(c)">{{ c }}</button>
        </div>
        <label class="flt-search">
          <span class="flt-k">搜索</span>
          <input v-model="keyword" type="search" placeholder="模板名称" />
        </label>
        <div class="flt-chips sort">
          <span class="flt-k">排序</span>
          <button :class="['chip', { on: sort === 'recommended' }]" type="button" @click="changeSort('recommended')">推荐</button>
          <button :class="['chip', { on: sort === 'usage' }]" type="button" @click="changeSort('usage')">使用量</button>
          <button :class="['chip', { on: sort === 'rating' }]" type="button" @click="changeSort('rating')">评分</button>
          <button :class="['chip', { on: sort === 'newest' }]" type="button" @click="changeSort('newest')">最新</button>
        </div>
        <span class="flt-count">{{ filteredMarket.length }} / {{ marketTemplates.length }}</span>
      </div>
    </section>

    <!-- 官方模板 -->
    <section class="m-grid-wrap">
      <ul v-if="filteredMarket.length" class="m-grid">
        <li v-for="t in filteredMarket" :key="'p' + t.id" class="m-card" @click="openDetail(t)">
          <div class="m-card-top">
            <span :class="['m-flag', { rec: t.recommended }]">{{ t.recommended ? '推荐' : '官方' }}</span>
            <span class="m-cat">{{ t.category || '未分类' }}</span>
          </div>

          <h3 class="m-name">{{ t.name }}</h3>

          <div class="m-spec">
            <span class="m-num">{{ t.ruleCount || 0 }}</span>
            <span class="m-unit">条排版规则</span>
          </div>

          <div class="m-meta">
            <span>评分 {{ (t.ratingAvg || 0).toFixed(1) }}<span class="m-faint">（{{ t.ratingCount || 0 }} 人）</span></span>
            <span>使用 {{ t.usageCount || 0 }} 次</span>
          </div>

          <div class="m-actions">
            <button class="m-use" type="button" @click.stop="useMarket(t)">使用此模板</button>
            <button
              v-if="isLoggedIn"
              :class="['m-fav', { on: favSet.has(t.id) }]"
              type="button"
              :title="favSet.has(t.id) ? '取消收藏' : '收藏'"
              @click.stop="toggleFav(t)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20.5l-1.45-1.32C5.4 14.36 2 11.28 2 7.5 2 4.42 4.42 2 7.5 2c1.74 0 3.41.81 4.5 2.09C13.09 2.81 14.76 2 16.5 2 19.58 2 22 4.42 22 7.5c0 3.78-3.4 6.86-8.55 11.68z" /></svg>
              <span>{{ favSet.has(t.id) ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
        </li>
      </ul>

      <div v-else class="t-empty">
        <p class="t-empty-k">没有符合条件的模板</p>
        <p class="t-empty-d">换个分类或清空搜索；也可以直接用自己的格式方案，或去「我的格式方案」新建一套。</p>
        <div class="t-empty-a">
          <button class="wbtn ghost" type="button" @click="resetFilter">清空筛选</button>
          <button class="wbtn solid" type="button" @click="$router.push('/templates')">新建格式方案</button>
        </div>
      </div>
    </section>

    <!-- 我的收藏 -->
    <section v-if="isLoggedIn && favorites.length" class="m-band">
      <div class="m-band-head">
        <h2>我的收藏</h2>
        <p>收藏过的模板，随时复制到自己名下。</p>
      </div>
      <ul class="m-grid">
        <li v-for="t in favorites" :key="'f' + t.id" class="m-card" @click="openDetail(t)">
          <div class="m-card-top">
            <span class="m-flag">{{ t.category || '收藏' }}</span>
          </div>
          <h3 class="m-name">{{ t.name }}</h3>
          <div class="m-spec">
            <span class="m-num">{{ t.ruleCount || 0 }}</span>
            <span class="m-unit">条排版规则</span>
          </div>
          <div class="m-actions">
            <button class="m-use" type="button" @click.stop="useMarket(t)">使用此模板</button>
            <button class="m-fav on" type="button" @click.stop="toggleFav(t)">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20.5l-1.45-1.32C5.4 14.36 2 11.28 2 7.5 2 4.42 4.42 2 7.5 2c1.74 0 3.41.81 4.5 2.09C13.09 2.81 14.76 2 16.5 2 19.58 2 22 4.42 22 7.5c0 3.78-3.4 6.86-8.55 11.68z" /></svg>
              <span>已收藏</span>
            </button>
          </div>
        </li>
      </ul>
    </section>

    <!-- 我的模板 -->
    <section v-if="isLoggedIn" class="m-band">
      <div class="m-band-head">
        <h2>我的模板</h2>
        <p>你自己创建的配置，上架后会出现在市场里。</p>
      </div>
      <ul v-if="myTemplates.length" class="m-rows">
        <li v-for="t in myTemplates" :key="'m' + t.id">
          <span class="m-row-name">{{ t.name }}</span>
          <span class="m-row-time">创建于 {{ formatTime(t.createTime) }}</span>
          <button class="m-row-go" type="button" @click="goMy(t.id)">使用此模板 →</button>
        </li>
      </ul>
      <div v-else class="t-empty">
        <p class="t-empty-k">还没有自己的模板</p>
        <p class="t-empty-d">在「我的格式方案」里配置好一套，就能在这里使用与分享。</p>
        <div class="t-empty-a">
          <button class="wbtn solid" type="button" @click="$router.push('/templates')">去配置</button>
        </div>
      </div>
    </section>

    <!-- 模板详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail ? detail.name : '模板详情'" width="640px"
      align-center destroy-on-close>
      <div v-if="detail" class="d-body">
        <div class="d-meta">
          <span class="d-tag" v-if="detail.recommended">推荐</span>
          <span class="d-tag gray">{{ detail.category || '未分类' }}</span>
          <span class="d-stat">评分 {{ (detail.ratingAvg || 0).toFixed(1) }}（{{ detail.ratingCount || 0 }} 人）</span>
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

    <PublicFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'
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
function resetFilter() {
  keyword.value = ''
  category.value = ''
  sort.value = 'recommended'
}
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
/* ============================================================
   模板市场：海报页头 + 样张式卡片（1px 硬边，靠数字与排印立住）
   ============================================================ */
.market {
  min-height: 100vh;
  background: var(--paper, #f7f6f6);
  color: var(--ink, #0a0909);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}
.m-hero {
  max-width: 1536px;
  margin: 0 auto;
  padding: 64px 48px 40px;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 40px;
  border-bottom: 2px solid var(--ink, #0a0909);
  box-sizing: border-box;
}
.m-hero-main h1 {
  margin: 0 0 14px;
  font-size: 56px;
  line-height: 1.04;
  font-weight: 900;
  letter-spacing: -0.02em;
}
.m-hero-main p {
  margin: 0;
  max-width: 620px;
  font-size: 16px;
  line-height: 1.7;
  color: var(--ink-2, #3f3f3f);
}
.m-hero-side {
  display: grid;
  gap: 6px;
  text-align: right;
  font-size: 12.5px;
  letter-spacing: 0.05em;
  color: var(--ink-3, #6b6b6b);
}
.m-stat b {
  font-size: 22px;
  font-weight: 900;
  color: var(--ink, #0a0909);
  margin-right: 6px;
  letter-spacing: -0.01em;
}
.m-filter {
  max-width: 1536px;
  margin: 0 auto;
  padding: 20px 48px 0;
  box-sizing: border-box;
}
.m-filter .sort {
  align-items: center;
  gap: 8px;
}
.m-grid-wrap {
  max-width: 1536px;
  margin: 0 auto;
  padding: 28px 48px 56px;
  box-sizing: border-box;
}
.m-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}
.m-card {
  border: 1px solid var(--ink, #0a0909);
  margin-right: -1px;
  margin-bottom: -1px;
  padding: 20px 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  background: var(--paper, #f7f6f6);
  transition: background 0.16s ease;
}
.m-card:hover {
  background: #fff;
}
.m-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.m-flag {
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--ink-2, #3f3f3f);
  border: 1px solid var(--hair, #c1c1c3);
  padding: 2px 7px;
}
.m-flag.rec {
  border-color: var(--red, #dd2627);
  color: var(--red, #dd2627);
  font-weight: 700;
}
.m-cat {
  font-size: 12px;
  color: var(--ink-3, #6b6b6b);
  letter-spacing: 0.04em;
}
.m-name {
  margin: 0;
  font-size: 21px;
  line-height: 1.35;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.m-spec {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-top: 2px;
}
.m-num {
  font-size: 40px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.03em;
}
.m-unit {
  font-size: 12.5px;
  color: var(--ink-3, #6b6b6b);
}
.m-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--ink-2, #3f3f3f);
}
.m-faint {
  color: var(--ink-3, #6b6b6b);
}
.m-actions {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #e4e4e4;
  display: flex;
  align-items: center;
  gap: 16px;
}
.m-use {
  background: none;
  border: 0;
  padding: 0 0 2px;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--ink, #0a0909);
  border-bottom: 2px solid var(--red, #dd2627);
  cursor: pointer;
}
.m-fav {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px solid var(--hair, #c1c1c3);
  padding: 4px 9px;
  font-family: inherit;
  font-size: 12px;
  color: var(--ink-3, #6b6b6b);
  cursor: pointer;
}
.m-fav svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
}
.m-fav:hover {
  border-color: var(--ink, #0a0909);
  color: var(--ink, #0a0909);
}
.m-fav.on {
  border-color: var(--red, #dd2627);
  color: var(--red, #dd2627);
}
.m-fav.on svg {
  fill: currentColor;
}

/* 收藏 / 我的模板 区 */
.m-band {
  max-width: 1536px;
  margin: 0 auto;
  padding: 48px;
  border-top: 2px solid var(--ink, #0a0909);
  box-sizing: border-box;
}
.m-band-head {
  margin-bottom: 22px;
}
.m-band-head h2 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.m-band-head p {
  margin: 0;
  font-size: 14px;
  color: var(--ink-2, #3f3f3f);
}
.m-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--ink, #0a0909);
}
.m-rows li {
  display: grid;
  grid-template-columns: 1fr 240px auto;
  align-items: center;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid var(--hair, #c1c1c3);
}
.m-row-name {
  font-size: 18px;
  font-weight: 700;
}
.m-row-time {
  font-size: 13px;
  color: var(--ink-3, #6b6b6b);
}
.m-row-go {
  background: none;
  border: 0;
  padding: 0;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--ink, #0a0909);
  cursor: pointer;
  border-bottom: 2px solid var(--red, #dd2627);
  padding-bottom: 2px;
}

/* 详情弹窗内的表与评论 */
:deep(.el-dialog) {
  border-radius: 0;
}
:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--ink, #0a0909);
}
:deep(.el-table th.el-table__cell) {
  background: transparent;
  font-weight: 400;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3, #6b6b6b);
  border-bottom: 1px solid var(--ink, #0a0909);
}
:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #e4e4e4;
}
:deep(.el-tag),
:deep(.el-button) {
  border-radius: 0;
}

@media (max-width: 900px) {
  .m-hero {
    grid-template-columns: 1fr;
    padding: 32px 18px 24px;
    gap: 18px;
  }
  .m-hero-main h1 {
    font-size: 32px;
  }
  .m-hero-main p {
    font-size: 14.5px;
  }
  .m-hero-side {
    text-align: left;
    grid-auto-flow: column;
    justify-content: start;
    gap: 18px;
  }
  .m-filter {
    padding: 14px 18px 0;
  }
  .m-grid-wrap {
    padding: 18px 18px 36px;
  }
  .m-grid {
    grid-template-columns: 1fr;
  }
  .m-band {
    padding: 32px 18px;
  }
  .m-band-head h2 {
    font-size: 22px;
  }
  .m-rows li {
    grid-template-columns: 1fr auto;
    row-gap: 6px;
  }
  .m-row-time {
    grid-column: 1;
  }
}


/* ---------- 详情弹窗 / 评论 / 评分 ---------- */
.d-body {
  display: grid;
  gap: 14px;
}
.d-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--ink-3, #6b6b6b);
}
.d-tag {
  border: 1px solid var(--red, #dd2627);
  color: var(--red, #dd2627);
  padding: 2px 8px;
  font-size: 11.5px;
  letter-spacing: 0.08em;
}
.d-tag.gray {
  border-color: var(--hair, #c1c1c3);
  color: var(--ink-2, #3f3f3f);
}
.d-stat {
  letter-spacing: 0.04em;
}
.d-grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-top: 1px solid var(--ink, #0a0909);
  border-bottom: 1px solid #e4e4e4;
}
.d-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  font-size: 14px;
}
.d-k {
  color: var(--ink-3, #6b6b6b);
}
.d-v {
  color: var(--ink, #0a0909);
  font-weight: 700;
}
.d-h {
  margin: 6px 0 8px;
  font-size: 15px;
  font-weight: 700;
}
.d-muted {
  color: var(--ink-3, #6b6b6b);
}
.d-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.d-comments {
  border-top: 1px solid #e4e4e4;
  padding-top: 12px;
}
.comment-list {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
}
.comment-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.c-avatar {
  width: 28px;
  height: 28px;
  flex: none;
  display: grid;
  place-items: center;
  border: 1px solid var(--ink, #0a0909);
  font-size: 13px;
  font-weight: 700;
}
.c-main {
  min-width: 0;
  flex: 1;
}
.c-head {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.c-name {
  font-size: 13.5px;
  font-weight: 700;
}
.c-time {
  font-size: 12px;
  color: var(--ink-3, #6b6b6b);
}
.c-content {
  margin-top: 3px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-2, #3f3f3f);
}
.comment-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.comment-input :deep(.el-textarea) {
  flex: 1;
}
.rate-body {
  padding: 6px 0 4px;
}
.rate-tip {
  margin: 0 0 10px;
  font-size: 13.5px;
  color: var(--ink-2, #3f3f3f);
}

</style>
