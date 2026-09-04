<template>
  <div class="page">
    <NavBar />

    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">成功案例</h1>
        <p class="hero-desc">看看他们如何使用我们的排版工具</p>
      </div>
    </section>

    <!-- 案例 -->
    <section class="section">
      <div class="section-inner">
        <div class="filters">
          <div class="chips">
            <button class="chip" :class="{ on: filterTag === '' }" @click="filterTag = ''">全部</button>
            <button v-for="t in tags" :key="t" class="chip" :class="{ on: filterTag === t }" @click="filterTag = t">{{ t }}</button>
          </div>
          <div class="chips">
            <button class="chip" :class="{ on: sortKey === 'rating' }" @click="sortKey = 'rating'">评分</button>
            <button class="chip" :class="{ on: sortKey === 'newest' }" @click="sortKey = 'newest'">最新</button>
          </div>
        </div>
        <div class="grid">
          <div class="card" v-for="c in shownCases" :key="c.id" @click="goDetail(c)">
            <CasePreview :tag="c.tag" :color="c.color" />
            <div class="case-tag">{{ c.tag }}</div>
            <h3 class="case-title">{{ c.title }}</h3>
            <p class="case-desc">{{ c.description }}</p>
            <div class="case-metrics" v-if="metricsOf(c).length">
              <span v-for="m in metricsOf(c)" :key="m.label" class="metric">{{ m.text }}</span>
            </div>
            <div class="case-meta">
              <div class="case-user">
                <div class="case-avatar" :class="c.color">{{ displayName(c).slice(0,1) }}</div>
                <div>
                  <div class="case-name">{{ displayName(c) }}</div>
                  <div class="case-school">{{ displaySub(c) }}</div>
                </div>
              </div>
              <div class="case-rating">
                <span>★</span> {{ c.rating }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!shownCases.length" description="暂无案例" :image-size="80" />
      </div>
    </section>

    <!-- 数据 -->
    <section class="section alt">
      <div class="section-inner">
        <div class="stats">
          <div class="stat" v-for="(s, i) in stats" :key="i">
            <div class="stat-num">{{ s.num }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'
import CasePreview from '../components/CasePreview.vue'
import { listPublicCases } from '../api/admin'

const router = useRouter()
const cases = ref([])
const filterTag = ref('')
const sortKey = ref('rating')
const stats = [
  { num: '5,000+', label: '注册用户' },
  { num: '10万+', label: '排版文档' },
  { num: '98%', label: '格式匹配率' },
  { num: '4.9', label: '用户评分' }
]

const tags = computed(() => [...new Set(cases.value.map(c => c.tag).filter(Boolean))])

const shownCases = computed(() => {
  let list = cases.value
  if (filterTag.value) list = list.filter(c => c.tag === filterTag.value)
  return [...list].sort((a, b) => {
    if (sortKey.value === 'newest') {
      return new Date(b.createTime || 0) - new Date(a.createTime || 0)
    }
    return Number(b.rating || 0) - Number(a.rating || 0)
  })
})

function metricsOf(c) {
  const out = []
  if (c.minutes != null) out.push({ label: 'minutes', text: `${c.minutes}分钟` })
  let m = {}
  try { m = JSON.parse(c.metrics || '{}') } catch (e) { m = {} }
  if (m.pages != null && m.pages !== '') out.push({ label: 'pages', text: `${m.pages}页` })
  if (m.matchRate != null && m.matchRate !== '') out.push({ label: 'matchRate', text: `${m.matchRate}%匹配` })
  return out
}

function displayName(c) {
  return c.sourceType === 'real' ? (c.username || '匿名用户') : (c.author || '匿名用户')
}

function displaySub(c) {
  if (c.sourceType === 'real') return c.templateName ? ('模板：' + c.templateName) : ''
  return c.school || ''
}

function goDetail(c) {
  router.push('/cases/' + c.id)
}

onMounted(async () => {
  try {
    cases.value = await listPublicCases(50)
  } catch (e) {}
})
</script>

<style scoped>
.page {
  --c-primary: #2F5D46;
  --c-dark: #1F2E26;
  --c-text: #33413A;
  --c-text2: #5C6B60;
  --c-text3: #8B968D;
  --c-border: #E3E0D5;
  --c-bg2: #F6F4EE;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--c-text);
  background: #fff;
  min-height: 100vh;
}
.hero {
  background: linear-gradient(180deg, #F1EEE5 0%, #fff 100%);
  padding: 64px 32px;
  text-align: center;
}
.hero-title { font-size: 38px; font-weight: 800; color: var(--c-dark); margin: 0 0 10px; }
.hero-desc { font-size: 16px; color: var(--c-text2); margin: 0; }
.section { padding: 56px 32px; }
.section.alt { background: var(--c-bg2); }
.section-inner { max-width: 1200px; margin: 0 auto; }
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  padding: 20px;
  transition: box-shadow 0.2s, transform 0.2s;
}
.card:hover {
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
  transform: translateY(-4px);
}
.filters { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip { border: 1px solid var(--c-border); background: #fff; color: var(--c-text2); font-size: 13px; padding: 6px 14px; border-radius: 999px; cursor: pointer; transition: all 0.15s; }
.chip:hover { border-color: var(--c-primary); color: var(--c-primary); }
.chip.on { background: var(--c-primary); color: #fff; border-color: var(--c-primary); }
.case-metrics { display: flex; gap: 8px; flex-wrap: wrap; margin: 0 0 14px; }
.metric { font-size: 12px; font-weight: 600; color: var(--c-primary); background: #E8EFE6; padding: 3px 10px; border-radius: 6px; }
.card { cursor: pointer; }
.case-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: var(--c-primary);
  background: #E8EFE6;
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
}
.case-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-dark);
  margin: 0 0 8px;
}
.case-desc {
  font-size: 13px;
  color: var(--c-text2);
  line-height: 1.6;
  margin: 0 0 16px;
  min-height: 60px;
}
.case-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--c-border);
  padding-top: 14px;
}
.case-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.case-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.case-avatar.blue { background: #2F5D46; }
.case-avatar.green { background: #10b981; }
.case-avatar.purple { background: #8B6F47; }
.case-avatar.orange { background: #f59e0b; }
.case-name { font-size: 13px; font-weight: 600; color: var(--c-dark); }
.case-school { font-size: 12px; color: var(--c-text3); }
.case-rating { font-size: 13px; color: #f59e0b; font-weight: 700; }
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  text-align: center;
}
.stat-num {
  font-size: 36px;
  font-weight: 800;
  color: var(--c-primary);
}
.stat-label {
  font-size: 14px;
  color: var(--c-text2);
  margin-top: 6px;
}
@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr; }
  .stats { grid-template-columns: 1fr 1fr; }
}
</style>
