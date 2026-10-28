<template>
  <div class="page">
    <PublicNav />

    <!-- 首屏：台账对开 —— 左元数据台账 · 右成果文档预览，内容全部来自后台公开案例 -->
    <section class="wall" aria-label="案例详情">
      <div class="wrap" v-if="c">
        <aside class="lcard" data-reveal>
          <p class="ltag">{{ c.sourceType === 'real' ? '真实成果' : '示范案例' }}<template v-if="c.tag"> · {{ c.tag }}</template></p>
          <dl class="ledger">
            <div class="lrow" v-for="r in ledgerRows" :key="r.label">
              <dt class="llabel">{{ r.label }}</dt>
              <dd class="lval">{{ r.value }}</dd>
            </div>
          </dl>
          <div class="lact" v-if="c.publicTemplateId || c.hasDoc">
            <button v-if="c.publicTemplateId" class="btn-red" type="button" @click="tryTemplate">试用同款模板</button>
            <a v-if="c.hasDoc" class="btn-ghost" :href="`/api/public/case/${c.id}/doc`" target="_blank" rel="noopener">下载文档</a>
          </div>
        </aside>

        <div class="dpanel-col" data-reveal>
          <div v-if="c.hasDoc" class="dpanel">
            <div v-if="docError" class="doc-fallback">文档已归档，暂不可预览。</div>
            <div v-else ref="docRef" class="doc-view" v-loading="docLoading"></div>
          </div>
          <div v-else-if="images.length" class="dpanel shot-panel">
            <img v-for="(img, i) in images" :key="i" :src="img" class="shot" alt="案例截图" />
          </div>
          <div v-else class="dpanel">
            <p class="doc-none">暂无归档文档</p>
          </div>
          <p class="dcap">排版成果文档 · 在线预览</p>

          <div v-if="c.detail" class="detail-sec">
            <h2 class="dsec-h">排版说明</h2>
            <p class="dsec-p">{{ c.detail }}</p>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="none-box">
        <p class="none-p">案例不存在</p>
      </div>
    </section>

    <PublicFooter />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { renderAsync } from 'docx-preview'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'
import { getPublicCase } from '../api/admin'
import api from '../api/index'
import { getToken } from '../utils/perm'

const route = useRoute()
const router = useRouter()
const c = ref(null)
const loading = ref(true)
const docRef = ref(null)
const docLoading = ref(false)
const docError = ref(false)

function cleanTitle(t) {
  return (t || '').replace(/\.docx$/i, '')
}

const ledgerRows = computed(() => {
  const x = c.value
  if (!x) return []
  let m = {}
  try { m = JSON.parse(x.metrics || '{}') } catch (e) { m = {} }
  const rows = [{ label: '标题', value: cleanTitle(x.title) }]
  const who = x.sourceType === 'real' ? (x.username || '匿名用户') : (x.author || '匿名用户')
  rows.push({ label: '作者', value: who })
  if (x.sourceType === 'real' && x.templateName) rows.push({ label: '模板', value: x.templateName })
  else if (x.school) rows.push({ label: '院校', value: x.school })
  if (x.minutes != null && x.minutes !== '') rows.push({ label: '用时', value: `${x.minutes}分钟` })
  if (m.pages != null && m.pages !== '') rows.push({ label: '页数', value: `${m.pages}页` })
  if (m.matchRate != null && m.matchRate !== '') rows.push({ label: '匹配率', value: `${m.matchRate}%` })
  if (x.rating != null && x.rating !== '') rows.push({ label: '评分', value: `${x.rating}` })
  return rows
})

const images = computed(() => {
  if (!c.value?.images) return []
  try { return JSON.parse(c.value.images) || [] } catch (e) { return [] }
})

async function loadDoc() {
  if (!c.value?.hasDoc) return
  docLoading.value = true
  await nextTick()
  try {
    const blob = await api.get(`/public/case/${c.value.id}/doc`, { responseType: 'blob' })
    if (docRef.value) {
      docRef.value.innerHTML = ''
      await renderAsync(blob, docRef.value, null, { inWrapper: true, breakPages: false })
    }
  } catch (e) {
    docError.value = true
  } finally {
    docLoading.value = false
  }
}

async function tryTemplate() {
  if (!c.value?.publicTemplateId) return
  if (!getToken()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const res = await api.post(`/template/market/${c.value.publicTemplateId}/copy`)
    ElMessage.success('已复制模板到「我的格式方案」')
    router.push('/template/' + (res?.id || ''))
  } catch (e) {}
}

// 入场动画：数据到达、内容渲染后重新收集，避免异步内容停在透明态
async function setupReveal() {
  await nextTick()
  const els = [...document.querySelectorAll('[data-reveal]:not([data-in])')]
  if (!els.length) return
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
}

onMounted(async () => {
  setupReveal()
  loading.value = true
  try {
    c.value = await getPublicCase(route.params.id)
    await loadDoc()
  } catch (e) {
    c.value = null
  } finally {
    loading.value = false
  }
  setupReveal()
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

.page :deep(.pn) {
  height: calc(102 * var(--u));
  padding: calc(14 * var(--u)) calc(80 * var(--u)) 0;
}

.wall {
  position: relative;
  min-height: calc(922 * var(--u));
  padding: calc(15 * var(--u)) calc(32 * var(--u)) calc(80 * var(--u));
}
.wrap {
  display: flex;
  align-items: flex-start;
  gap: calc(36 * var(--u));
}

/* ---------- 左：台账卡 ---------- */
.lcard {
  flex: none;
  width: calc(439 * var(--u));
  background: #fff;
  border: 1px solid var(--hair-2);
  padding: calc(28 * var(--u)) calc(33 * var(--u)) calc(34 * var(--u));
}
.ltag {
  display: inline-block;
  margin: 0;
  background: var(--red);
  color: #fff;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  line-height: 1;
  padding: calc(8 * var(--u)) calc(14 * var(--u));
}
.ledger {
  margin: calc(22 * var(--u)) 0 0;
}
.lrow {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: calc(16 * var(--u));
  padding: calc(19 * var(--u)) 0;
}
.lrow + .lrow {
  border-top: 1px solid var(--hair-2);
}
.llabel {
  flex: none;
  font-size: calc(16 * var(--u));
  font-weight: 400;
  color: #8a8b8b;
}
.lval {
  margin: 0;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  line-height: 1.35;
  color: var(--ink);
  text-align: right;
  word-break: break-all;
}
.lact {
  display: flex;
  gap: calc(20 * var(--u));
  margin-top: calc(30 * var(--u));
}
.btn-red {
  border: 0;
  background: var(--red);
  color: #fff;
  font-family: inherit;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  line-height: 1;
  height: calc(60 * var(--u));
  padding: 0 calc(34 * var(--u));
  cursor: pointer;
  transition: background 0.15s ease;
}
.btn-red:hover {
  background: #b91d1e;
}
.btn-ghost {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--hair);
  background: #fff;
  color: var(--ink);
  text-decoration: none;
  font-size: calc(20 * var(--u));
  font-weight: 500;
  line-height: 1;
  height: calc(60 * var(--u));
  padding: 0 calc(34 * var(--u));
  transition: border-color 0.15s ease, color 0.15s ease;
}
.btn-ghost:hover {
  border-color: var(--red);
  color: var(--red);
}
.btn-red:focus-visible,
.btn-ghost:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: 2px;
}

/* ---------- 右：成果文档预览 ---------- */
.dpanel-col {
  flex: 1;
  min-width: 0;
}
.dpanel {
  position: relative;
  background: #fff;
  border: 1px solid var(--hair-2);
}
.dpanel::before {
  content: '';
  position: absolute;
  top: calc(1 * var(--u));
  left: calc(1 * var(--u));
  right: calc(1 * var(--u));
  height: calc(4 * var(--u));
  background: var(--red);
}
.doc-view {
  padding: calc(30 * var(--u)) calc(40 * var(--u));
  min-height: calc(693 * var(--u));
  max-height: calc(1100 * var(--u));
  overflow: auto;
}
.doc-fallback {
  padding: calc(80 * var(--u)) calc(40 * var(--u));
  text-align: center;
  color: #8a8b8b;
  font-size: calc(17 * var(--u));
}
.doc-none {
  margin: 0;
  padding: calc(80 * var(--u)) calc(40 * var(--u));
  text-align: center;
  color: #8a8b8b;
  font-size: calc(17 * var(--u));
}
.shot-panel {
  padding: calc(30 * var(--u));
  display: flex;
  flex-direction: column;
  gap: calc(20 * var(--u));
}
.shot {
  width: 100%;
  object-fit: contain;
  border: 1px solid var(--hair-2);
  background: #fafafa;
}
.dcap {
  margin: calc(20 * var(--u)) 0 0;
  font-size: calc(16 * var(--u));
  letter-spacing: 0.04em;
  color: #8a8b8b;
}

/* ---------- 排版说明 ---------- */
.detail-sec {
  margin-top: calc(44 * var(--u));
  border-top: 1px solid var(--hair-2);
  padding-top: calc(30 * var(--u));
}
.dsec-h {
  margin: 0 0 calc(16 * var(--u));
  font-size: calc(30 * var(--u));
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--ink);
}
.dsec-p {
  margin: 0;
  font-size: calc(19 * var(--u));
  line-height: 1.9;
  color: var(--ink-2);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 空态 ---------- */
.none-box {
  border: 1px solid var(--hair-2);
  background: #fff;
  padding: calc(80 * var(--u)) calc(40 * var(--u));
}
.none-p {
  margin: 0;
  text-align: center;
  color: #8a8b8b;
  font-size: calc(17 * var(--u));
}

/* ---------- 动效 ---------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(14px);
}
[data-reveal][data-in="1"] {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
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

/* ---------- 窄屏兜底（响应式阶段细化） ---------- */
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
  .wrap {
    flex-direction: column;
    gap: 16px;
  }
  .lcard {
    width: 100%;
    padding: 16px;
  }
  .ltag { font-size: 13px; padding: 6px 10px; }
  .lrow { padding: 12px 0; gap: 12px; }
  .llabel { font-size: 13px; }
  .lval { font-size: 15px; }
  .lact { margin-top: 16px; flex-wrap: wrap; gap: 10px; }
  .btn-red, .btn-ghost { font-size: 14px; height: 40px; padding: 0 18px; }
  .doc-view { padding: 16px; min-height: 320px; max-height: 640px; }
  .doc-fallback, .doc-none { padding: 40px 16px; font-size: 14px; }
  .shot-panel { padding: 16px; gap: 10px; }
  .dcap { font-size: 13px; margin-top: 12px; }
  .detail-sec { margin-top: 24px; padding-top: 18px; }
  .dsec-h { font-size: 19px; }
  .dsec-p { font-size: 14px; }
  .none-box { padding: 40px 16px; }
}
</style>
