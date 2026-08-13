<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>关于我们</span>
      </div>
    </header>

    <!-- ===== 核心人物 (参考 ain.alem QWGNzYm Pop-Out Effect) ===== -->
    <section class="team-section">
      <h2 class="team-heading">核心团队</h2>
      <p class="team-sub">一群热爱技术的同学，致力于让排版变得简单</p>
      <div class="team">
        <div class="person" v-for="(m, i) in members" :key="i">
          <div class="container">
            <div class="container-inner">
              <div class="circle" :style="{ background: m.bg }"></div>
              <img class="img" :class="'img' + (i + 1)" :src="m.img" :alt="m.name" />
            </div>
          </div>
          <div class="divider"></div>
          <div class="name">{{ m.name }}</div>
          <div class="title">{{ m.role }}</div>
        </div>
      </div>
    </section>

    <!-- ===== 产品介绍 Slider (参考 damianmuti OgBWej) ===== -->
    <section class="product-section">
      <h2 class="team-heading">产品介绍</h2>
      <p class="team-sub">一站式论文排版与图表生成工具</p>
      <div class="slider">
        <div class="slide" :class="{ active: currentSlide === 0 }" id="prod-1">
          <div class="slide__bg" :style="{ background: 'linear-gradient(135deg,#3B6BFF,#7c3aed)' }"></div>
          <div class="slide__images">
            <div class="slide__image slide__image--left" :style="slideImgStyle(0)">
              <div class="card-mock mock-a">
                <div class="mock-line" style="width:60%"></div>
                <div class="mock-line" style="width:80%"></div>
                <div class="mock-line" style="width:45%"></div>
              </div>
            </div>
            <div class="slide__image slide__image--right" :style="slideImgStyle(0)">
              <div class="card-mock mock-b">
                <div class="mock-title"></div>
                <div class="mock-line" style="width:70%"></div>
                <div class="mock-line" style="width:50%"></div>
              </div>
            </div>
          </div>
          <div class="slide__text">
            <h3>智能排版</h3>
            <p>自动识别章节结构，统一标题、正文、图表编号，一键生成规范论文。</p>
          </div>
        </div>

        <div class="slide" :class="{ active: currentSlide === 1 }" id="prod-2">
          <div class="slide__bg" :style="{ background: 'linear-gradient(135deg,#10b981,#0ea5e9)' }"></div>
          <div class="slide__images">
            <div class="slide__image slide__image--left" :style="slideImgStyle(1)">
              <div class="card-mock mock-a">
                <div class="mock-line" style="width:55%"></div>
                <div class="mock-line" style="width:75%"></div>
                <div class="mock-line" style="width:40%"></div>
              </div>
            </div>
            <div class="slide__image slide__image--right" :style="slideImgStyle(1)">
              <div class="card-mock mock-b">
                <div class="mock-title"></div>
                <div class="mock-line" style="width:65%"></div>
                <div class="mock-line" style="width:48%"></div>
              </div>
            </div>
          </div>
          <div class="slide__text">
            <h3>图表工具</h3>
            <p>三线表、ER 图、系统架构图、流程图等专业图表一键生成。</p>
          </div>
        </div>

        <div class="slider__pagination">
          <button class="dot" :class="{ active: currentSlide === 0 }" @click="currentSlide = 0">智能排版</button>
          <button class="dot" :class="{ active: currentSlide === 1 }" @click="currentSlide = 1">图表工具</button>
        </div>
      </div>
    </section>

    <!-- ===== 底部信息 ===== -->
    <main class="content">
      <div class="card">
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()

function goBack() {
  router.back()
}

const currentSlide = ref(0)

const members = [
  { name: 'keysqiu', role: 'Product Manager', bg: '#d7ecff', img: '/keysqiu.png' },
  { name: 'zwany1', role: 'Senior Developer', bg: '#fee7d3', img: '/zwany1.jpg' }
]

const gradient = ['#3B6BFF', '#7c3aed', '#10b981']

function slideImgStyle(i) {
  const g = gradient[i]
  return { background: `linear-gradient(135deg, ${g}, ${g}99)` }
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
    title: '设计理念',
    paragraphs: [
      '· 规则配置驱动：用清晰直观的规则描述代替手工逐段调整；',
      '· 保留内容聚焦内容：排版引擎尊重你的论文内容，只处理格式；',
      '· 安全可信：数据加密存储、参数化查询防注入、全程登录认证。'
    ]
  }
]
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f2f2f2;
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

/* ===== 核心人物 (参考 QWGNzYm) ===== */
.team-section {
  padding: 56px 32px 40px;
  text-align: center;
}
.team-heading {
  font-size: 28px;
  font-weight: 700;
  color: #404245;
  margin: 0 0 8px;
}
.team-sub {
  color: #6e6e6e;
  font-size: 15px;
  margin: 0 0 36px;
}
.team {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
}
.person {
  align-items: center;
  display: flex;
  flex-direction: column;
  width: 280px;
}
.container {
  border-radius: 50%;
  height: 312px;
  transform: scale(0.48);
  transition: transform 250ms cubic-bezier(0.4, 0, 0.2, 1);
  width: 400px;
  position: relative;
}
.container:hover {
  transform: scale(0.54);
}
.container-inner {
  clip-path: path("M 390,400 C 390,504.9341 304.9341,590 200,590 95.065898,590 10,504.9341 10,400 V 10 H 200 390 Z");
  position: relative;
  transform-origin: 50%;
  top: -200px;
}
.circle {
  border-radius: 50%;
  height: 380px;
  left: 10px;
  position: absolute;
  top: 210px;
  width: 380px;
}
.img {
  position: relative;
  transform: translateY(20px) scale(1.15);
  transform-origin: 50% bottom;
  transition: transform 300ms cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}
.container:hover .img {
  transform: translateY(0) scale(1.2);
}
.img1 {
  left: 22px;
  top: 164px;
  width: 340px;
  height: 400px;
  object-fit: cover;
  border-radius: 50% 50% 8px 8px;
  filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.2));
}
.img2 {
  left: 30px;
  top: 164px;
  width: 340px;
  height: 400px;
  object-fit: cover;
  border-radius: 50% 50% 8px 8px;
  filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.2));
}
.divider {
  background-color: #ca6060;
  height: 1px;
  width: 160px;
}
.name {
  color: #404245;
  font-size: 30px;
  font-weight: 600;
  margin-top: 16px;
  text-align: center;
}
.title {
  color: #6e6e6e;
  font-size: 14px;
  font-style: italic;
  margin-top: 4px;
}

/* ===== 产品 Slider (参考 OgBWej) ===== */
.product-section {
  padding: 40px 0 60px;
  background: #fff;
  text-align: center;
}
.slider {
  position: relative;
  max-width: 1000px;
  margin: 0 auto;
  height: 460px;
}
.slide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s ease;
}
.slide.active {
  opacity: 1;
  pointer-events: auto;
}
.slide__bg {
  position: absolute;
  z-index: 0;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(2);
  filter: blur(50px);
  width: 100%;
  height: 100%;
  opacity: 0.25;
}
.slide__images {
  position: relative;
  width: 100%;
  max-width: 900px;
  height: 365px;
  margin: 0 20px;
  z-index: 1;
}
.slide__image {
  position: absolute;
  width: 100%;
  opacity: 0;
  transform: translate(30px, 0);
  transition: all 0.6s ease-in-out;
  filter: blur(8px);
}
.slide.active .slide__image {
  opacity: 1;
  transform: translate(0, 0);
  filter: blur(0) drop-shadow(0px 10px 30px rgba(0, 0, 0, 0.25));
  transition-delay: 0.3s;
}
.slide__image--left {
  clip-path: polygon(0 0, 100% 0, 60% 100%, 0 100%);
  transform: translate(0.75%, -10px);
}
.slide__image--right {
  top: 5vmin;
  clip-path: polygon(40% 0, 100% 0, 100% 100%, 0 100%);
  transform: translate(-0.75%, 10px);
}
.slide.active .slide__image--left {
  transform: translate(0.75%, -10px);
}
.slide.active .slide__image--right {
  transform: translate(-0.75%, 10px);
}
.card-mock {
  height: 365px;
  border-radius: 12px;
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  justify-content: center;
}
.mock-line {
  height: 14px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.9);
}
.mock-title {
  height: 26px;
  width: 50%;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.95);
  margin-bottom: 8px;
}
.slide__text {
  position: relative;
  z-index: 2;
  margin-top: 20px;
  max-width: 500px;
}
.slide__text h3 {
  font-size: 22px;
  color: #303133;
  margin: 0 0 6px;
}
.slide__text p {
  font-size: 14px;
  color: #6e6e6e;
  margin: 0;
}
.slider__pagination {
  position: absolute;
  bottom: -10px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 16px;
  z-index: 3;
}
.dot {
  padding: 8px 20px;
  border: 2px solid #d5d5d5;
  border-radius: 999px;
  background: transparent;
  color: #6e6e6e;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.dot:hover {
  border-color: #3B6BFF;
  color: #3B6BFF;
}
.dot.active {
  background: #3B6BFF;
  border-color: #3B6BFF;
  color: #fff;
}

/* ===== 底部 ===== */
.content {
  max-width: 760px;
  margin: 0 auto;
  padding: 48px 20px 40px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
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
