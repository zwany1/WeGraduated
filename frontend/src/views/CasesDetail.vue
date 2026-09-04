<template>
  <div class="page">
    <NavBar />

    <section class="hero">
      <div class="hero-inner">
        <div class="tag-row">
          <span class="case-tag" v-if="c?.tag">{{ c.tag }}</span>
          <span class="src-tag" :class="c?.sourceType" v-if="c?.sourceType">{{ c.sourceType === 'real' ? '真实成果' : '示范案例' }}</span>
        </div>
        <h1 class="hero-title">{{ c?.title || '案例详情' }}</h1>
        <div class="hero-meta" v-if="c">
          <span>{{ displayName }}</span>
          <span class="dot" v-if="displaySub">·</span>
          <span v-if="displaySub">{{ displaySub }}</span>
          <span class="dot" v-if="c.rating">·</span>
          <span class="rating" v-if="c.rating">★ {{ c.rating }}</span>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-inner" v-loading="loading">
        <template v-if="c">
          <CasePreview :tag="c.tag" :color="c.color" big />
          <div class="case-metrics" v-if="metrics.length">
            <span v-for="m in metrics" :key="m.label" class="metric">{{ m.text }}</span>
          </div>
          <div class="detail" v-if="c.detail">{{ c.detail }}</div>

          <div v-if="c.hasDoc" class="doc-block">
            <h3 class="block-title">排版成果文档</h3>
            <div v-if="docError" class="doc-fallback">文档已归档,暂不可预览。</div>
            <div v-else ref="docRef" class="doc-view" v-loading="docLoading"></div>
            <a class="doc-download" :href="`/api/public/case/${c.id}/doc`" target="_blank">下载文档</a>
          </div>

          <el-carousel v-else-if="images.length" height="440px" indicator-position="outside" class="shots" arrow="hover">
            <el-carousel-item v-for="(img, i) in images" :key="i">
              <img :src="img" class="shot" alt="案例截图" />
            </el-carousel-item>
          </el-carousel>

          <div class="ref" v-if="c.publicTemplateId">
            <div class="ref-text">
              <div class="ref-label">一键试用同款模板</div>
              <div class="ref-hint">复制该案例使用的模板到「我的格式方案」,立即体验同类排版</div>
            </div>
            <el-button type="primary" @click="tryTemplate">试用同款模板</el-button>
          </div>
        </template>
        <el-empty v-if="!c && !loading" description="案例不存在" :image-size="80" />
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { renderAsync } from 'docx-preview'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'
import CasePreview from '../components/CasePreview.vue'
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

const displayName = computed(() => {
  const x = c.value
  if (!x) return ''
  return x.sourceType === 'real' ? (x.username || '匿名用户') : (x.author || '匿名用户')
})
const displaySub = computed(() => {
  const x = c.value
  if (!x) return ''
  if (x.sourceType === 'real') return x.templateName ? ('模板：' + x.templateName) : ''
  return x.school || ''
})
const metrics = computed(() => {
  const x = c.value
  if (!x) return []
  const out = []
  if (x.minutes != null) out.push({ label: 'minutes', text: `${x.minutes}分钟` })
  let m = {}
  try { m = JSON.parse(x.metrics || '{}') } catch (e) { m = {} }
  if (m.pages != null && m.pages !== '') out.push({ label: 'pages', text: `${m.pages}页` })
  if (m.matchRate != null && m.matchRate !== '') out.push({ label: 'matchRate', text: `${m.matchRate}%匹配` })
  return out
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

onMounted(async () => {
  loading.value = true
  try {
    c.value = await getPublicCase(route.params.id)
    await loadDoc()
  } catch (e) {
    c.value = null
  } finally {
    loading.value = false
  }
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
  padding: 56px 32px 40px;
  text-align: center;
}
.hero-inner { max-width: 820px; margin: 0 auto; }
.tag-row { display: flex; gap: 8px; justify-content: center; margin-bottom: 12px; }
.case-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: var(--c-primary);
  background: #E8EFE6;
  padding: 4px 12px;
  border-radius: 999px;
}
.src-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
}
.src-tag.real { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.src-tag.manual { color: #8a6a25; background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.hero-title { font-size: 34px; font-weight: 800; color: var(--c-dark); margin: 0 0 12px; }
.hero-meta { font-size: 14px; color: var(--c-text2); display: flex; gap: 8px; justify-content: center; align-items: center; flex-wrap: wrap; }
.hero-meta .rating { color: #f59e0b; font-weight: 700; }
.hero-meta .dot { color: var(--c-text3); }
.section { padding: 32px 32px 56px; }
.section-inner { max-width: 820px; margin: 0 auto; }
.case-metrics { display: flex; gap: 10px; flex-wrap: wrap; margin: 16px 0 8px; }
.metric { font-size: 13px; font-weight: 600; color: var(--c-primary); background: #E8EFE6; padding: 5px 12px; border-radius: 6px; }
.detail {
  font-size: 15px;
  line-height: 1.85;
  color: var(--c-text);
  background: var(--c-bg2);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  padding: 24px 28px;
  margin: 20px 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.block-title { font-size: 16px; font-weight: 700; color: var(--c-dark); margin: 24px 0 12px; }
.doc-block { margin: 16px 0; }
.doc-view {
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: #fafafa;
  padding: 16px;
  min-height: 200px;
  max-height: 720px;
  overflow: auto;
}
.doc-fallback { padding: 32px; text-align: center; color: var(--c-text3); background: var(--c-bg2); border: 1px solid var(--c-border); border-radius: 12px; }
.doc-download {
  display: inline-block;
  margin-top: 12px;
  color: var(--c-primary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
}
.doc-download:hover { text-decoration: underline; }
.shots { margin: 24px 0; border-radius: 12px; overflow: hidden; }
.shot { width: 100%; height: 100%; object-fit: contain; background: #F6F4EE; }
.ref {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  background: linear-gradient(135deg, #E8EFE6, #F1F4EF);
  border: 1px solid #D6E3D6;
  border-radius: 14px;
  margin-top: 24px;
}
.ref-text { flex: 1; }
.ref-label { font-size: 15px; font-weight: 700; color: var(--c-dark); margin-bottom: 4px; }
.ref-hint { font-size: 13px; color: var(--c-text2); }
</style>
