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

    <!-- ===== 底部信息 (3D 翻卡, 参考 rikanutyy PEJBxX) ===== -->
    <section class="flip-section">
      <h2 class="team-heading">了解更多</h2>
      <p class="team-sub">悬停卡片查看详情</p>
      <div class="flip-row">
        <div class="flip-card" v-for="(s, i) in sections" :key="i">
          <div class="imgBox">
            <div class="bark"></div>
            <div class="flip-front" :style="{ backgroundImage: 'url(' + s.image + ')' }">
              <div class="flip-title">{{ s.title }}</div>
            </div>
          </div>
          <div class="details">
            <h4 class="color1">{{ s.title }}</h4>
            <p v-for="(p, j) in s.paragraphs" :key="j">{{ p }}</p>
          </div>
        </div>
      </div>
      <p class="contact">
        欢迎通过邮箱
        <a href="mailto:2651896126@qq.com">2651896126@qq.com</a>
        与我们联系，期待你的反馈与建议。
      </p>
    </section>
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
    image: '/gugugaga.png',
    paragraphs: [
      'Word 排版助手是一套基于规则配置驱动的学术文档排版工具，解决毕业论文排版繁琐、格式不统一的痛点。',
      '支持论文智能排版、三线表生成、ER 图绘制、系统设计图生成等实用功能。'
    ]
  },
  {
    title: '设计理念',
    image: '/lulu.jpg',
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

/* ===== 3D 翻卡 (参考 rikanutyy PEJBxX) ===== */
.flip-section {
  padding: 60px 32px 60px;
  background: #fde3a7;
  text-align: center;
}
.flip-row {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 80px;
  padding: 40px 0;
}
.flip-card {
  color: #013243;
  position: relative;
  width: 300px;
  height: 400px;
  background: #e0e1dc;
  transform-style: preserve-3d;
  transform: perspective(2000px);
  box-shadow: inset 300px 0 50px rgba(0,0,0,.5), 20px 0 60px rgba(0,0,0,.5);
  transition: 1s;
}
.flip-card:hover {
  transform: perspective(2000px) rotate(15deg) scale(1.2);
  box-shadow: inset 20px 0 50px rgba(0,0,0,.5), 0 10px 100px rgba(0,0,0,.5);
}
.flip-card:before {
  content:'';
  position: absolute;
  top: -5px;
  left: 0;
  width: 100%;
  height: 5px;
  background: #bac1ba;
  transform-origin: bottom;
  transform: skewX(-45deg);
}
.flip-card:after {
  content: '';
  position: absolute;
  top: 0;
  right: -5px;
  width: 5px;
  height: 100%;
  background: #92a29c;
  transform-origin: left;
  transform: skewY(-45deg);
}
.flip-card .imgBox {
  width: 100%;
  height: 100%;
  position: relative;
  transform-origin: left;
  transition: .7s;
}
.flip-card .bark {
  position: absolute;
  background: #e0e1dc;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: .7s;
}
.flip-front {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  background-size: cover;
  background-position: center;
  padding-bottom: 24px;
  box-sizing: border-box;
}
.flip-icon {
  font-size: 56px;
}
.flip-title {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.2ch;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.6);
}
.flip-card:hover .imgBox {
  transform: rotateY(-135deg);
}
.flip-card:hover .bark {
  opacity: 1;
  transition: .6s;
  box-shadow: 300px 200px 100px rgba(0,0,0,.4) inset;
}
.flip-card .details {
  position: absolute;
  top: 0;
  left: 0;
  box-sizing: border-box;
  padding: 60px 40px 0 50px;
  z-index: -1;
}
.flip-card .details h4 {
  font-size: 24px;
  line-height: 1;
  color: #1bbc9b;
  text-align: center;
  margin: 0 0 20px;
}
.flip-card .details p {
  font-size: 15px;
  line-height: 1.8;
  transform: rotate(-10deg);
  padding: 0;
  margin: 0 0 8px;
  color: #013243;
}
.contact {
  margin: 30px auto 0;
  padding-top: 20px;
  border-top: 1px solid #d9c48f;
  color: #8a6d3b;
  font-size: 14px;
  max-width: 600px;
}
.contact a {
  color: #3B6BFF;
}
</style>
