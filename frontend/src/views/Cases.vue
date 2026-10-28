<template>
  <div class="page">
    <PublicNav />

    <!-- 首屏：档案卷宗 —— 内容完全由后台公开案例驱动，后台上架多少展示多少 -->
    <section class="wall" aria-label="成功案例">
      <h1 class="c-title" data-reveal>成功案例</h1>
      <p class="c-sub" data-reveal>案例即档案：产出可查、指标可验</p>

      <div class="a-filters" data-reveal>
        <p class="a-count">共 {{ shownCases.length }} 份公开档案</p>
        <div class="a-chips">
          <button
            v-for="t in tags"
            :key="t"
            class="chip"
            :class="{ on: filterTag === t }"
            type="button"
            @click="filterTag = filterTag === t ? '' : t"
          >{{ t }}</button>
          <button class="chip" :class="{ on: sortKey === 'rating' }" type="button" @click="sortKey = 'rating'">评分</button>
          <button class="chip" :class="{ on: sortKey === 'newest' }" type="button" @click="sortKey = 'newest'">最新</button>
        </div>
      </div>

      <div v-if="shownCases.length" class="dgrid">
        <article
          v-for="(c, i) in shownCases"
          :key="c.id"
          class="dcard"
          data-reveal
          :style="{ '--ri': i }"
          tabindex="0"
          @click="goDetail(c)"
          @keydown.enter="goDetail(c)"
        >
          <p class="dlabel">档案 {{ String(i + 1).padStart(3, '0') }}</p>
          <h2 class="dtitle">{{ cleanTitle(c.title) }}</h2>
          <p class="ddesc">{{ c.description }}</p>
          <div class="dline" aria-hidden="true"></div>
          <p class="dmetrics">{{ metricsLine(c) || '—' }}</p>
          <p class="dfoot">
            <span class="dwho">{{ displayName(c) }}<template v-if="displaySub(c)"> · {{ displaySub(c) }}</template></span>
            <span class="dmore">调阅全文</span>
          </p>
        </article>
      </div>
      <div v-else class="a-empty-box" data-reveal>
        <p class="a-empty">暂无公开档案</p>
      </div>
    </section>

    <PublicFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'
import { listPublicCases } from '../api/admin'

const router = useRouter()
const cases = ref([])
const filterTag = ref('')
const sortKey = ref('rating')

// 案例内容完全来自后台公开案例接口：后台上架多少展示多少，不做任何示例/占位内容
const shownCases = computed(() =>
  [...cases.value]
    .filter(c => !filterTag.value || c.tag === filterTag.value)
    .sort((a, b) => {
      if (sortKey.value === 'newest') {
        return new Date(b.createTime || 0) - new Date(a.createTime || 0)
      }
      return Number(b.rating || 0) - Number(a.rating || 0)
    })
)

const tags = computed(() => [...new Set(cases.value.map(c => c.tag).filter(Boolean))])

function cleanTitle(t) {
  return (t || '').replace(/\.docx$/i, '')
}

function metricsLine(c) {
  const parts = []
  if (c.minutes != null && c.minutes !== '') parts.push(`用时 ${c.minutes}分钟`)
  let m = {}
  try { m = JSON.parse(c.metrics || '{}') } catch (e) { m = {} }
  if (m.pages != null && m.pages !== '') parts.push(`${m.pages}页`)
  if (m.matchRate != null && m.matchRate !== '') parts.push(`${m.matchRate}%匹配`)
  if (c.rating != null && c.rating !== '') parts.push(`评分 ${c.rating}`)
  return parts.join(' · ')
}

function displayName(c) {
  return c.sourceType === 'real' ? (c.username || '匿名用户') : (c.author || '匿名用户')
}

function displaySub(c) {
  if (c.sourceType === 'real') return c.templateName ? ('模板：' + c.templateName) : ''
  return c.school || ''
}

function goDetail(c) {
  if (c.id) router.push('/cases/' + c.id)
}

onMounted(async () => {
  try {
    cases.value = await listPublicCases(50)
  } catch (e) {}
  const els = document.querySelectorAll('[data-reveal]')
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
    els.forEach(e => e.setAttribute('data-in', '1'))
    return
  }
  const io = new IntersectionObserver(entries => {
    entries.forEach(en => {
      if (en.isIntersecting) {
        en.target.setAttribute('data-in', '1')
        io.unobserve(en.target)
      }
    })
  }, { threshold: 0.15 })
  els.forEach(e => io.observe(e))
})
</script>

<style scoped>
.page {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --red: #dd2627;
  --hair: #c1c1c3;
  --hair-2: #e4e4e4;
  --u: calc(min(100vw, 1536px) / 1536);
  min-height: 100vh;
  background: var(--paper);
  color: var(--ink);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 导航对齐样张：102 高、底部保留细线 */
.page :deep(.pn) {
  height: calc(102 * var(--u));
  padding: calc(14 * var(--u)) calc(80 * var(--u)) 0;
}

.wall {
  position: relative;
  min-height: calc(922 * var(--u));
  padding: calc(202 * var(--u)) calc(113 * var(--u)) calc(80 * var(--u)) calc(112 * var(--u));
}

.c-title {
  position: absolute;
  left: calc(112 * var(--u));
  top: calc(27 * var(--u));
  margin: 0;
  font-size: calc(93 * var(--u));
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  color: var(--ink);
}
.c-sub {
  position: absolute;
  left: calc(112 * var(--u));
  top: calc(139 * var(--u));
  margin: 0;
  font-size: calc(30 * var(--u));
  font-weight: 400;
  letter-spacing: 0.05em;
  color: var(--ink);
}

/* 档案卡阵列：2 列，卡 640x322，列距 29 行距 26；数量由后台数据决定 */
.dgrid {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: calc(29 * var(--u));
  row-gap: calc(26 * var(--u));
}
.dcard {
  position: relative;
  height: calc(322 * var(--u));
  background: #fff;
  border: 1px solid var(--ink);
  cursor: pointer;
}
.dcard::before {
  content: '';
  position: absolute;
  top: calc(1 * var(--u));
  left: calc(1 * var(--u));
  right: calc(1 * var(--u));
  height: calc(4 * var(--u));
  background: var(--red);
  transition: height 0.18s ease;
}
.dcard:hover {
  border-color: var(--red);
}
.dcard:hover::before {
  height: calc(8 * var(--u));
}
.dcard:hover .dmore {
  background-size: 100% 100%;
}

.dlabel {
  position: absolute;
  left: calc(29 * var(--u));
  top: calc(33 * var(--u));
  margin: 0;
  font-size: calc(23 * var(--u));
  font-weight: 700;
  line-height: 1;
  color: var(--red);
}
.dtitle {
  position: absolute;
  left: calc(29 * var(--u));
  top: calc(71 * var(--u));
  margin: 0;
  max-width: calc(560 * var(--u));
  font-size: calc(34 * var(--u));
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.01em;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ddesc {
  position: absolute;
  left: calc(29 * var(--u));
  top: calc(124 * var(--u));
  margin: 0;
  width: calc(450 * var(--u));
  font-size: calc(19 * var(--u));
  font-weight: 400;
  line-height: calc(30 * var(--u));
  color: #8a8b8b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.dline {
  position: absolute;
  left: calc(29 * var(--u));
  right: calc(29 * var(--u));
  top: calc(201 * var(--u));
  border-top: 1px solid var(--hair-2);
}
.dmetrics {
  position: absolute;
  left: calc(29 * var(--u));
  top: calc(219 * var(--u));
  margin: 0;
  font-size: calc(17 * var(--u));
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.02em;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
}
.dfoot {
  position: absolute;
  left: calc(29 * var(--u));
  right: calc(29 * var(--u));
  top: calc(265 * var(--u));
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: calc(18 * var(--u));
  font-size: calc(19 * var(--u));
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
}
.dwho {
  font-weight: 400;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
}
.dmore {
  flex: none;
  font-weight: 500;
  color: var(--red);
  background-image: linear-gradient(var(--red), var(--red));
  background-repeat: no-repeat;
  background-position: 0 100%;
  background-size: 100% calc(2 * var(--u));
  padding-bottom: calc(4 * var(--u));
  transition: background-size 0.15s ease;
}
.dcard:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: 2px;
}

/* ---------- 筛选行与空态 ---------- */
.a-count {
  margin: 0;
  font-size: calc(16 * var(--u));
  font-weight: 400;
  color: #8a8b8b;
  letter-spacing: 0.04em;
}
.a-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: calc(20 * var(--u));
  margin: 0 0 calc(34 * var(--u));
  flex-wrap: wrap;
}
.a-chips {
  display: flex;
  gap: calc(10 * var(--u));
  flex-wrap: wrap;
}
.chip {
  background: none;
  border: 1px solid var(--hair);
  color: var(--ink-2);
  font-family: inherit;
  font-size: calc(15 * var(--u));
  font-weight: 500;
  line-height: 1;
  padding: calc(10 * var(--u)) calc(18 * var(--u));
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}
.chip:hover {
  border-color: var(--red);
  color: var(--red);
}
.chip.on {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}
.a-empty-box {
  border: 1px solid var(--hair-2);
  background: #fff;
  padding: calc(56 * var(--u)) calc(29 * var(--u));
}
.a-empty {
  margin: 0;
  font-size: calc(17 * var(--u));
  color: #8a8b8b;
}

/* ---------- 动效：入场 reveal ---------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(14px);
}
[data-reveal][data-in="1"] {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.dcard[data-in="1"] {
  transition-delay: calc(var(--ri, 0) * 60ms);
}
@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
}

::selection {
  background: var(--red);
  color: #fff;
}

/* ---------- 窄屏基础兜底（响应式阶段细化） ---------- */
@media (max-width: 900px) {
  .page :deep(.pn) {
    height: auto;
    padding: 10px 16px;
    border-bottom: 1px solid var(--hair);
  }
  .wall {
    min-height: 0;
    padding: 16px;
  }
  .c-title {
    position: static;
    font-size: 34px;
  }
  .c-sub {
    position: static;
    font-size: 15px;
    margin: 8px 0 20px;
  }
  .dgrid {
    position: static;
    width: 100%;
    grid-template-columns: 1fr;
    row-gap: 16px;
  }
  .dcard {
    height: auto;
    padding: 16px;
  }
  .dlabel, .dtitle, .ddesc, .dmetrics, .dfoot {
    position: static;
    width: auto;
    max-width: 100%;
    white-space: normal;
  }
  .dlabel { font-size: 13px; }
  .dtitle { font-size: 19px; margin: 6px 0; }
  .ddesc { font-size: 14px; line-height: 1.6; margin-bottom: 12px; -webkit-line-clamp: 3; }
  .dline { position: static; margin: 0 0 10px; }
  .dmetrics { font-size: 13px; margin-bottom: 10px; }
  .dfoot { font-size: 14px; }
  .a-count { font-size: 13px; }
  .a-filters { margin: 0 0 20px; }
  .chip { font-size: 13px; padding: 7px 12px; }
  .a-empty-box { padding: 32px 16px; }
  .a-empty { font-size: 14px; }
}
</style>
