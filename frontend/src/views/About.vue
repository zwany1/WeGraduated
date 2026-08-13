<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>关于我们</span>
      </div>
    </header>

    <!-- ===== 核心人物 (原版 QWGNzYm 照抄) ===== -->
    <section class="team-section">
      <h2 class="team-heading">核心团队</h2>
      <p class="team-sub">一群热爱技术的同学，致力于让排版变得简单</p>
      <div class="team">
        <div class="person">
          <div class="container">
            <div class="container-inner">
              <img class="circle" src="/circle_1.jpg" alt="" />
              <img class="img img1" src="/keysqiu.png" alt="keysqiu" />
            </div>
          </div>
          <div class="divider"></div>
          <div class="name">keysqiu</div>
          <div class="title">Product Manager</div>
        </div>
        <div class="person">
          <div class="container">
            <div class="container-inner">
              <img class="circle" src="/circle_2.jpg" alt="" />
              <img class="img img2" src="/zwany1.png" alt="zwany1" />
            </div>
          </div>
          <div class="divider"></div>
          <div class="name">zwany1</div>
          <div class="title">Senior Developer</div>
        </div>
      </div>
    </section>

    <!-- ===== 产品介绍 3D Cover Flow (参考 keyframers rNxmVZN) ===== -->
    <section class="product-section">
      <h2 class="team-heading">产品介绍</h2>
      <p class="team-sub">一站式论文排版与图表生成工具</p>
      <div class="cf">
        <div class="slides">
          <button class="nav-btn prev" @click="prevSlide">‹</button>
          <button class="nav-btn next" @click="nextSlide">›</button>
          <div
            class="slide"
            v-for="(s, i) in cfSlides"
            :key="i"
            :data-active="i === cfIndex ? true : null"
            :style="{ '--offset': i - cfIndex, '--dir': i === cfIndex ? 0 : (i - cfIndex > 0 ? 1 : -1) }"
          >
            <div class="slideBackground" :style="{ backgroundImage: 'url(' + s.image + ')' }"></div>
            <div class="slideContent" :style="{ backgroundImage: 'url(' + s.image + ')' }" @mousemove="onTilt($event, i)">
              <div class="slideContentInner">
                <div class="slideSubtitle">{{ s.subtitle }}</div>
                <div class="slideTitle">{{ s.title }}</div>
                <div class="slideDescription">{{ s.desc }}</div>
              </div>
            </div>
          </div>
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

const cfIndex = ref(0)

const cfSlides = [
  { title: '智能排版', subtitle: '格式助手', desc: '自动识别章节结构，统一格式', image: '/1.png' },
  { title: '三线表', subtitle: '表格工具', desc: '一键生成规范三线表', image: '/2.png' },
  { title: 'ER 图', subtitle: '绘图工具', desc: 'Chen 记法实体关系图', image: '/3.png' },
  { title: '系统设计', subtitle: '设计工具', desc: '流程图/架构图一键生成', image: '/4.png' }
]

function prevSlide() {
  cfIndex.value = (cfIndex.value - 1 + cfSlides.length) % cfSlides.length
}
function nextSlide() {
  cfIndex.value = (cfIndex.value + 1) % cfSlides.length
}
function onTilt(e, i) {
  if (i !== cfIndex.value) return
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  const px = (e.clientX - rect.left) / rect.width
  const py = (e.clientY - rect.top) / rect.height
  el.style.setProperty('--px', px)
  el.style.setProperty('--py', py)
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

/* ===== 核心人物 (原版 QWGNzYm 照抄) ===== */
.team-section {
  padding: 56px 32px 40px;
  text-align: center;
  background-color: #f2f2f2;
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
  align-items: center;
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
  -webkit-tap-highlight-color: transparent;
  transform: scale(0.48);
  transition: transform 250ms cubic-bezier(0.4, 0, 0.2, 1);
  width: 400px;
  position: relative;
}
.container:after {
  background-color: #f2f2f2;
  content: "";
  height: 10px;
  position: absolute;
  top: 390px;
  width: 100%;
  left: 0;
}
.container:hover {
  transform: scale(0.54);
}
.container-inner {
  clip-path: path("M 390,400 C 390,504.9341 304.9341,590 200,590 95.065898,590 10,504.9341 10,400 V 10 H 200 390 Z");
  position: relative;
  transform-origin: 50%;
  top: -200px;
  height: 590px;
  width: 400px;
}
.circle {
  background-color: #fee7d3;
  border-radius: 50%;
  cursor: pointer;
  height: 380px;
  left: 10px;
  pointer-events: none;
  position: absolute;
  top: 210px;
  width: 380px;
}
.img {
  pointer-events: none;
  position: relative;
  transform: translateY(20px) scale(1.15);
  transform-origin: 50% bottom;
  transition: transform 300ms cubic-bezier(0.4, 0, 0.2, 1);
}
.container:hover .img {
  transform: translateY(0) scale(1.2);
}
.img1 {
  left: 10px;
  top: 60px;
  width: 380px;
  height: 530px;
  object-fit: cover;
  object-position: center 10%;
}
.img2 {
  left: -20px;
  top: 60px;
  width: 440px;
  height: 530px;
  object-fit: cover;
  object-position: center 10%;
}
.divider {
  background-color: #ca6060;
  height: 1px;
  width: 160px;
}
.name {
  color: #404245;
  font-size: 36px;
  font-weight: 600;
  margin-top: 16px;
  text-align: center;
}
.title {
  color: #6e6e6e;
  font-family: arial;
  font-size: 14px;
  font-style: italic;
  margin-top: 4px;
}

/* ===== 产品 Slider (参考 OgBWej) ===== */
.product-section {
  padding: 60px 0 80px;
  background: #151515;
  color: #fff;
  text-align: center;
  overflow: hidden;
}
.product-section .team-heading {
  color: #fff;
}
.product-section .team-sub {
  color: #aaa;
}
.cf {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  perspective: 1200px;
}
.slides {
  display: grid;
  position: relative;
  width: 100%;
  height: 100%;
}
.slides > .slide {
  grid-area: 1/-1;
  display: flex;
  justify-content: center;
  align-items: center;
}
.slides > button {
  -webkit-appearance: none;
     -moz-appearance: none;
          appearance: none;
  background: transparent;
  border: none;
  color: white;
  position: absolute;
  font-size: 3rem;
  width: 3.5rem;
  height: 3.5rem;
  top: 35%;
  transition: opacity 0.3s;
  opacity: 0.7;
  z-index: 5;
  cursor: pointer;
  line-height: 1;
}
.slides > button:hover {
  opacity: 1;
}
.slides > button:focus {
  outline: none;
}
.nav-btn.prev {
  left: 6%;
}
.nav-btn.next {
  right: 6%;
}
.slideContent {
  width: 30vw;
  height: 40vw;
  max-width: 420px;
  max-height: 560px;
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  transition: transform 0.5s ease-in-out;
  opacity: 0.7;
  display: grid;
  align-content: center;
  transform-style: preserve-3d;
  transform: perspective(1000px) translateX(calc(100% * var(--offset))) rotateY(calc(-45deg * var(--dir)));
}
.slideContentInner {
  transform-style: preserve-3d;
  transform: translateZ(2rem);
  transition: opacity 0.3s linear;
  text-shadow: 0 0.1rem 1rem #000;
  opacity: 0;
  padding: 0 2rem;
}
.slideContentInner .slideSubtitle,
.slideContentInner .slideTitle {
  font-size: 1.6rem;
  font-weight: normal;
  letter-spacing: 0.2ch;
  text-transform: uppercase;
  margin: 0;
}
.slideContentInner .slideSubtitle::before {
  content: "— ";
}
.slideContentInner .slideDescription {
  margin: 0.5rem 0 0;
  font-size: 0.85rem;
  letter-spacing: 0.1ch;
}
.slideBackground {
  position: fixed;
  top: 0;
  left: -10%;
  right: -10%;
  bottom: 0;
  background-size: cover;
  background-position: center center;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.3s linear, transform 0.3s ease-in-out;
  pointer-events: none;
  transform: translateX(calc(10% * var(--dir)));
}
.slide[data-active] {
  z-index: 2;
  pointer-events: auto;
}
.slide[data-active] .slideBackground {
  opacity: 0.18;
  transform: none;
}
.slide[data-active] .slideContentInner {
  opacity: 1;
}
.slide[data-active] .slideContent {
  --x: calc(var(--px) - 0.5);
  --y: calc(var(--py) - 0.5);
  opacity: 1;
  transform: perspective(1000px);
}
.slide[data-active] .slideContent:hover {
  transition: none;
  transform: perspective(1000px) rotateY(calc(var(--x) * 45deg)) rotateX(calc(var(--y) * -45deg));
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
