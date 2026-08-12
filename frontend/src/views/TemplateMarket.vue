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
        <!-- 预设模板 -->
        <h2 class="sec-title">预设模板</h2>
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

        <!-- 我的模板 -->
        <template v-if="isLoggedIn">
          <h2 class="sec-title mt">我的模板</h2>
          <p class="sec-sub">你创建的排版模板</p>
          <div v-if="myTemplates.length" class="grid">
            <div class="card mine" v-for="t in myTemplates" :key="'m'+t.id">
              <h3 class="card-title">{{ t.name }}</h3>
              <div class="card-meta">创建于 {{ formatTime(t.createTime) }}</div>
              <button class="btn-use" @click="goMy(t.id)">进入配置</button>
            </div>
          </div>
          <p v-else class="empty">还没有模板，去创建一个吧</p>
        </template>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'
import { listTemplates, createTemplate } from '../api/template'

const router = useRouter()
const isLoggedIn = ref(false)
const myTemplates = ref([])

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
  isLoggedIn.value = !!localStorage.getItem('token')
  if (isLoggedIn.value) {
    try {
      myTemplates.value = (await listTemplates()) || []
    } catch (e) {}
  }
})

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
.tag {
  font-size: 11px;
  color: var(--c-text2);
  background: var(--c-bg2);
  border: 1px solid var(--c-border);
  padding: 2px 8px;
  border-radius: 999px;
}
.btn-use {
  width: 100%;
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
.card.mine .card-meta {
  font-size: 12px;
  color: var(--c-text3);
  margin-bottom: 14px;
}
.empty {
  text-align: center;
  color: var(--c-text3);
  padding: 40px 0;
}
@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr 1fr; }
}
</style>
