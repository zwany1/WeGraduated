<template>
  <div class="page">
    <NavBar />

    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">使用教程</h1>
        <p class="hero-desc">四步上手，轻松完成论文排版</p>
      </div>
    </section>

    <!-- 步骤 -->
    <section class="section">
      <div class="section-inner">
        <div class="steps">
          <div class="step" v-for="(s, i) in steps" :key="i">
            <div class="step-num">{{ i + 1 }}</div>
            <div class="step-icon" :class="s.color">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="s.icon"></svg>
            </div>
            <h3 class="step-title">{{ s.title }}</h3>
            <p class="step-desc">{{ s.desc }}</p>
            <ul class="step-list">
              <li v-for="(p, j) in s.points" :key="j">{{ p }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section class="section alt">
      <div class="section-inner">
        <h2 class="sec-title">常见问题</h2>
        <p class="sec-sub">FAQ</p>
        <div class="faq-list">
          <div class="faq-item" v-for="(f, i) in faqs" :key="i">
            <div class="faq-q" @click="toggle(i)">
              <span>{{ f.q }}</span>
              <svg :class="{ open: openIdx === i }" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 6l4 4 4-4"/></svg>
            </div>
            <div v-if="openIdx === i" class="faq-a">{{ f.a }}</div>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'

const openIdx = ref(0)

const steps = [
  {
    title: '注册登录',
    color: 'blue',
    icon: '<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>',
    desc: '创建账号并登录，邮箱 + 图形双重验证确保安全。',
    points: ['邮箱注册（可选用户名）', '邮箱验证码验证', '登录后进入工作台']
  },
  {
    title: '创建模板',
    color: 'purple',
    icon: '<line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="8" cy="6" r="2"/><circle cx="16" cy="12" r="2"/><circle cx="10" cy="18" r="2"/>',
    desc: '选择预设模板或从零配置页面、标题、正文等全部排版规则。',
    points: ['从模板市场选择预设', '配置页面/页边距/页眉页脚', '设置标题与正文格式', '图表编号与参考文献']
  },
  {
    title: '上传排版',
    color: 'green',
    icon: '<path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
    desc: '上传 .docx 论文，选择模板一键自动排版。',
    points: ['支持 .docx 格式', '自动识别章节结构', '按规则统一格式', '实时查看进度']
  },
  {
    title: '下载交付',
    color: 'orange',
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>',
    desc: '预览排版效果，下载规范论文，还可导出 PDF。',
    points: ['排版结果预览', '下载 .docx 文档', 'PDF 预览导出']
  }
]

const faqs = [
  { q: '支持哪些文档格式？', a: '当前支持 .docx 格式的 Word 文档上传与排版，排版后支持下载 .docx 并在线预览 PDF。' },
  { q: '如何设置标题格式？', a: '在工作台创建/选择模板后，进入「标题格式」配置区，可为一级/二级/三级标题分别设置字体、字号、加粗、对齐方式。' },
  { q: '图表编号如何自动生成？', a: '在模板的「图表编号」配置中开启编号功能，系统会自动识别文档中的图片/表格题注，按"章节号-顺序号"格式自动编号。' },
  { q: '参考文献支持哪些格式？', a: '支持 GB/T 7714-2015、APA、MLA 等主流格式，可在模板配置中切换，系统会按所选规范自动排版条目。' },
  { q: '登录注册为什么要双重验证？', a: '为保障账户安全，登录需通过图形验证码，注册还需邮箱验证码确认邮箱真实性，有效防止恶意注册与暴力破解。' },
  { q: '忘记密码怎么办？', a: '在登录页点击「忘记密码」，输入注册邮箱获取验证码，验证通过后即可设置新密码，重置后旧登录凭证自动失效。' }
]

function toggle(i) {
  openIdx.value = openIdx.value === i ? -1 : i
}
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
.section-inner { max-width: 1000px; margin: 0 auto; }
.sec-title { font-size: 26px; font-weight: 700; color: var(--c-dark); text-align: center; margin: 0 0 8px; }
.sec-sub { text-align: center; color: var(--c-text2); margin: 0 0 32px; font-size: 15px; }
.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.step {
  position: relative;
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  padding: 28px 22px 22px;
  text-align: center;
}
.step-num {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--c-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  margin: 8px auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-icon.blue { background: #EEF1FF; color: #3B6BFF; }
.step-icon.green { background: #ECFDF5; color: #10b981; }
.step-icon.purple { background: #F5F3FF; color: #7c3aed; }
.step-icon.orange { background: #FFF7ED; color: #f59e0b; }
.step-title { font-size: 16px; font-weight: 700; color: var(--c-dark); margin: 0 0 8px; }
.step-desc { font-size: 13px; color: var(--c-text2); margin: 0 0 12px; line-height: 1.6; }
.step-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--c-text);
  text-align: left;
}
.step-list li { position: relative; padding-left: 14px; }
.step-list li::before { content: '•'; position: absolute; left: 2px; color: var(--c-primary); }
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.faq-item {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  overflow: hidden;
}
.faq-q {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--c-dark);
  cursor: pointer;
}
.faq-q svg { transition: transform 0.2s; color: var(--c-text3); }
.faq-q svg.open { transform: rotate(180deg); }
.faq-a {
  padding: 0 20px 16px;
  font-size: 14px;
  color: var(--c-text2);
  line-height: 1.7;
}
@media (max-width: 900px) {
  .steps { grid-template-columns: 1fr 1fr; }
}
</style>
