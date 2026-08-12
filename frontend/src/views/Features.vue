<template>
  <div class="page">
    <NavBar />

    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">强大的论文排版工具箱</h1>
        <p class="hero-desc">从智能排版到图表生成，一站式满足毕业论文全部需求</p>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="section">
      <div class="section-inner">
        <h2 class="sec-title">核心能力</h2>
        <p class="sec-sub">覆盖排版、表格、绘图全流程</p>
        <div class="grid">
          <div class="card" v-for="(f, i) in features" :key="i">
            <div class="card-icon" :class="f.color">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="f.icon"></svg>
            </div>
            <h3 class="card-title">{{ f.title }}</h3>
            <p class="card-desc">{{ f.desc }}</p>
            <ul class="card-list">
              <li v-for="(item, j) in f.points" :key="j">
                <svg viewBox="0 0 16 16" width="14" height="14" fill="none"><circle cx="8" cy="8" r="7" fill="#3B6BFF"/><path d="M5 8l2 2 4-4" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/></svg>
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- 工具入口 -->
    <section class="section alt">
      <div class="section-inner">
        <h2 class="sec-title">实用工具</h2>
        <p class="sec-sub">立即体验</p>
        <div class="tools-grid">
          <div class="tool-card" v-for="(t, i) in tools" :key="i" @click="goStart(t.route)">
            <div class="tool-icon" :class="t.color">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="t.icon"></svg>
            </div>
            <div class="tool-name">{{ t.name }}</div>
            <div class="tool-desc">{{ t.desc }}</div>
            <span class="tool-link">立即使用 →</span>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'

const router = useRouter()

const features = [
  {
    title: 'Word 智能排版',
    color: 'blue',
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/>',
    desc: '自动识别论文结构，统一字体字号行距，快速生成规范文档。',
    points: ['章节结构智能识别', '标题/正文格式一键统一', '自动生成目录', '图表题注自动编号']
  },
  {
    title: '规则配置驱动',
    color: 'purple',
    icon: '<line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="8" cy="6" r="2"/><circle cx="16" cy="12" r="2"/><circle cx="10" cy="18" r="2"/>',
    desc: '可视化配置页面、标题、正文、图表编号等全部排版规则。',
    points: ['页面/页边距/页眉页脚', '各级标题格式', '正文行距缩进对齐', '模板保存复用']
  },
  {
    title: '三线表生成',
    color: 'green',
    icon: '<rect x="3" y="3" width="18" height="18" rx="1"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/>',
    desc: '一键生成符合学术规范的三线表 Word 文档。',
    points: ['顶线/底线 1.5pt 栏目线 0.75pt', 'SQL CREATE TABLE 解析', '实时预览导出']
  },
  {
    title: 'ER 图生成',
    color: 'orange',
    icon: '<circle cx="6" cy="6" r="3"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><line x1="9" y1="6" x2="15" y2="6"/><line x1="6" y1="9" x2="6" y2="15"/><line x1="18" y1="9" x2="18" y2="15"/><line x1="9" y1="18" x2="15" y2="18"/>',
    desc: 'Chen 记法实体关系图，自动布局，专业美观。',
    points: ['实体/属性/关系建模', '主键下划线标注', '力导向自动布局', 'SVG/PNG 导出']
  },
  {
    title: '系统设计图',
    color: 'purple',
    icon: '<rect x="3" y="3" width="7" height="5" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="8" y="14" width="7" height="5" rx="1"/><line x1="6.5" y1="8" x2="6.5" y2="11" stroke-dasharray="2"/><line x1="17.5" y1="8" x2="17.5" y2="11" stroke-dasharray="2"/><line x1="6.5" y1="11" x2="11.5" y2="14"/><line x1="17.5" y1="11" x2="11.5" y2="14"/>',
    desc: '流程图、泳道图、架构图、用例图、时序图等一键生成。',
    points: ['流程 DSL 解析', '泳道自动分组', '分层架构图', '类图/活动图/用例图']
  },
  {
    title: '参考文献格式化',
    color: 'green',
    icon: '<path d="M5 3C5 3 3 5 3 8C3 11 5 13 5 13M11 3C11 3 13 5 13 8C13 11 11 13 11 13"/><path d="M7 5h2M7 8h2M7 11h2"/>',
    desc: '支持 GB/T 7714、APA、MLA 等主流参考文献格式。',
    points: ['多格式支持', '作者截断规范', '自动排版生成', '模板内置']
  }
]

const tools = [
  { name: 'Word 智能排版', color: 'blue', route: '/templates', desc: '上传论文自动排版', icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/>' },
  { name: '三线表生成', color: 'green', route: '/table3', desc: '规范三线表一键生成', icon: '<rect x="3" y="3" width="18" height="18" rx="1"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/>' },
  { name: 'ER 图生成', color: 'orange', route: '/er', desc: '实体关系图快速绘制', icon: '<circle cx="6" cy="6" r="3"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><line x1="9" y1="6" x2="15" y2="6"/><line x1="6" y1="9" x2="6" y2="15"/><line x1="18" y1="9" x2="18" y2="15"/><line x1="9" y1="18" x2="15" y2="18"/>' },
  { name: '系统设计图', color: 'purple', route: '/system-design', desc: '多种系统设计图', icon: '<rect x="3" y="3" width="7" height="5" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="8" y="14" width="7" height="5" rx="1"/><line x1="6.5" y1="8" x2="6.5" y2="11" stroke-dasharray="2"/><line x1="17.5" y1="8" x2="17.5" y2="11" stroke-dasharray="2"/><line x1="6.5" y1="11" x2="11.5" y2="14"/><line x1="17.5" y1="11" x2="11.5" y2="14"/>' }
]

function goStart(route) {
  if (!localStorage.getItem('token')) {
    router.push({ path: '/login', query: { redirect: route } })
  } else {
    router.push(route)
  }
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
  padding: 72px 32px;
  text-align: center;
}
.hero-inner {
  max-width: 800px;
  margin: 0 auto;
}
.hero-title {
  font-size: 40px;
  font-weight: 800;
  color: var(--c-dark);
  margin: 0 0 12px;
}
.hero-desc {
  font-size: 17px;
  color: var(--c-text2);
  margin: 0;
}
.section {
  padding: 64px 32px;
}
.section.alt {
  background: var(--c-bg2);
}
.section-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.sec-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--c-dark);
  text-align: center;
  margin: 0 0 8px;
}
.sec-sub {
  text-align: center;
  color: var(--c-text2);
  margin: 0 0 40px;
  font-size: 15px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  padding: 28px;
  transition: box-shadow 0.2s, transform 0.2s;
}
.card:hover {
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
  transform: translateY(-4px);
}
.card-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.card-icon.blue { background: #EEF1FF; color: #3B6BFF; }
.card-icon.green { background: #ECFDF5; color: #10b981; }
.card-icon.purple { background: #F5F3FF; color: #7c3aed; }
.card-icon.orange { background: #FFF7ED; color: #f59e0b; }
.card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-dark);
  margin: 0 0 8px;
}
.card-desc {
  font-size: 14px;
  color: var(--c-text2);
  margin: 0 0 16px;
  line-height: 1.6;
}
.card-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.card-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--c-text);
}
.tools-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.tool-card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 14px;
  padding: 24px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.tool-card:hover {
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
  transform: translateY(-4px);
}
.tool-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.tool-icon.blue { background: #EEF1FF; color: #3B6BFF; }
.tool-icon.green { background: #ECFDF5; color: #10b981; }
.tool-icon.purple { background: #F5F3FF; color: #7c3aed; }
.tool-icon.orange { background: #FFF7ED; color: #f59e0b; }
.tool-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-dark);
  margin-bottom: 6px;
}
.tool-desc {
  font-size: 13px;
  color: var(--c-text2);
  margin-bottom: 12px;
}
.tool-link {
  color: var(--c-primary);
  font-size: 14px;
  font-weight: 600;
}
@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr; }
  .tools-grid { grid-template-columns: 1fr 1fr; }
}
</style>
