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
        <div class="grid">
          <div class="card" v-for="(c, i) in cases" :key="i">
            <div class="case-preview" :class="c.color">
              <div class="case-preview-inner">
                <div class="case-preview-line w70"></div>
                <div class="case-preview-line w50"></div>
                <div class="case-preview-line w60"></div>
                <div class="case-preview-line w40"></div>
                <div class="case-preview-table">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
            <div class="case-tag">{{ c.tag }}</div>
            <h3 class="case-title">{{ c.title }}</h3>
            <p class="case-desc">{{ c.desc }}</p>
            <div class="case-meta">
              <div class="case-user">
                <div class="case-avatar" :class="c.color">{{ c.user.slice(0,1) }}</div>
                <div>
                  <div class="case-name">{{ c.user }}</div>
                  <div class="case-school">{{ c.school }}</div>
                </div>
              </div>
              <div class="case-rating">
                <span>★</span> {{ c.rating }}
              </div>
            </div>
          </div>
        </div>
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
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'

const cases = [
  {
    tag: '本科毕业论文',
    title: '计算机专业本科论文一键排版',
    desc: '通过模板配置标题、正文、三线表规则，300 页论文 5 分钟完成规范排版，格式匹配率 99%。',
    color: 'blue',
    user: '张同学',
    school: '某工业大学',
    rating: 5.0
  },
  {
    tag: '硕士毕业论文',
    title: '管理学硕士论文全文格式统一',
    desc: '自动识别章节结构，统一各级标题与正文格式，参考文献按 GB/T 7714 自动生成。',
    color: 'purple',
    user: '李同学',
    school: '某财经大学',
    rating: 5.0
  },
  {
    tag: '三线表',
    title: '科研三线表快速生成',
    desc: '从 SQL 表结构一键生成规范三线表，无需手工绘制，表格规范完全符合要求。',
    color: 'green',
    user: '王同学',
    school: '某理工大学',
    rating: 4.8
  },
  {
    tag: 'ER 图',
    title: '数据库课程设计 ER 图绘制',
    desc: '输入实体属性关系，自动生成 Chen 记法 ER 图，主键、关系、基数标注清晰。',
    color: 'orange',
    user: '赵同学',
    school: '某师范大学',
    rating: 4.9
  },
  {
    tag: '系统设计图',
    title: '系统架构图与流程图设计',
    desc: '分层架构图、泳道流程图一键生成，论文系统设计章节轻松完成。',
    color: 'blue',
    user: '陈同学',
    school: '某科技大学',
    rating: 4.9
  },
  {
    tag: '期刊投稿',
    title: '学术论文期刊投稿格式调整',
    desc: '快速调整为期刊要求的版式，作者信息、参考文献、图表编号全部规范。',
    color: 'green',
    user: '刘同学',
    school: '某医学院',
    rating: 4.7
  }
]

const stats = [
  { num: '5,000+', label: '注册用户' },
  { num: '10万+', label: '排版文档' },
  { num: '98%', label: '格式匹配率' },
  { num: '4.9', label: '用户评分' }
]
</script>

<style scoped>
.page {
  --c-primary: #3B6BFF;
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
.case-preview {
  border-radius: 12px;
  height: 140px;
  padding: 24px;
  margin-bottom: 16px;
}
.case-preview.blue { background: linear-gradient(135deg, #EEF1FF, #dbe4ff); }
.case-preview.green { background: linear-gradient(135deg, #ECFDF5, #d1fae5); }
.case-preview.purple { background: linear-gradient(135deg, #F5F3FF, #ede9fe); }
.case-preview.orange { background: linear-gradient(135deg, #FFF7ED, #ffedd5); }
.case-preview-inner { display: flex; flex-direction: column; gap: 10px; }
.case-preview-line { height: 8px; border-radius: 4px; background: #fff; opacity: 0.9; }
.case-preview-line.w70 { width: 70%; }
.case-preview-line.w50 { width: 50%; }
.case-preview-line.w60 { width: 60%; }
.case-preview-line.w40 { width: 40%; }
.case-preview-table {
  display: flex; gap: 6px; margin-top: 8px;
}
.case-preview-table span {
  flex: 1; height: 40px; background: #fff; opacity: 0.85; border-radius: 4px;
}
.case-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: var(--c-primary);
  background: #EEF1FF;
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
.case-avatar.blue { background: #3B6BFF; }
.case-avatar.green { background: #10b981; }
.case-avatar.purple { background: #7c3aed; }
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
