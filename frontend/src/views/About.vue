<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>关于我们</span>
      </div>
    </header>

    <main class="content">
      <div class="card">
        <div class="hero">
          <div class="logo-w">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h4l3 12 3-12h4"/></svg>
          </div>
          <h1 class="title">Word 排版助手</h1>
          <p class="slogan">让论文排版，从几个小时变成几分钟</p>
        </div>

        <section v-for="(s, i) in sections" :key="i" class="section">
          <h2>{{ s.title }}</h2>
          <p v-for="(p, j) in s.paragraphs" :key="j" class="para">{{ p }}</p>
          <ul v-if="s.list" class="list">
            <li v-for="(li, k) in s.list" :key="k">{{ li }}</li>
          </ul>
        </section>

        <p class="contact">
          欢迎通过邮箱
          <a href="mailto:2651896126@qq.com">2651896126@qq.com</a>
          与我们联系，期待你的反馈与建议。
        </p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()

function goBack() {
  router.back()
}

const sections = [
  {
    title: '项目简介',
    paragraphs: [
      'Word 排版助手是一套基于规则配置驱动的学术文档排版工具。我们致力于解决毕业论文排版繁琐、格式不统一、反复调整的痛点，让同学们把精力放在论文内容本身，而不是消耗在排版细节上。',
      '系统支持论文智能排版、三线表生成、ER 图绘制、系统设计图生成等实用功能，覆盖毕业论文从撰写到定稿的主要环节。'
    ]
  },
  {
    title: '核心能力',
    list: [
      '智能排版：自动识别章节结构，统一标题、正文、图表编号等格式',
      '三线表生成：一键生成符合学术规范的三线表',
      'ER 图 / 系统设计图：Chen 记法 ER 图、流程图、架构图等专业图表',
      '模板复用：自定义排版规则保存为模板，一次配置长期复用'
    ]
  },
  {
    title: '设计理念',
    paragraphs: [
      '· 规则配置驱动：用清晰直观的规则描述代替手工逐段调整；',
      '· 保留内容聚焦内容：排版引擎尊重你的论文内容，只处理格式；',
      '· 安全可信：数据加密存储、参数化查询防注入、全程登录认证。'
    ]
  },
  {
    title: '适用人群',
    list: [
      '本科 / 硕士 / 博士毕业论文撰写者',
      '需要规范格式的学术论文作者',
      '需要绘制三线表、ER 图、系统设计图的研究人员',
      '希望统一课题组排版规范的学生与导师'
    ]
  }
]
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
}
.bar {
  display: flex;
  align-items: center;
  padding: 14px 30px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 760px;
  margin: 32px auto;
  padding: 0 20px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.hero {
  text-align: center;
  margin-bottom: 32px;
}
.logo-w {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3B6BFF, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.title {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 6px;
}
.slogan {
  color: #909399;
  font-size: 15px;
  margin: 0;
}
.section {
  margin-bottom: 26px;
}
.section h2 {
  font-size: 17px;
  color: #303133;
  margin: 0 0 10px;
}
.para {
  color: #606266;
  font-size: 14px;
  line-height: 1.9;
  margin: 0 0 8px;
}
.list {
  margin: 6px 0 0;
  padding-left: 20px;
  color: #606266;
  font-size: 14px;
  line-height: 1.9;
}
.contact {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  color: #909399;
  font-size: 14px;
}
.contact a {
  color: #3B6BFF;
}
</style>
