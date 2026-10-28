<template>
  <div class="page">
    <PublicNav />

    <!-- 首屏：自查清单 · 勾选式 -->
    <section class="wall" aria-label="使用教程">
      <h1 class="g-title" data-reveal>使用教程</h1>
      <p class="g-sub" data-reveal>照着勾，四步交卷</p>

      <ol class="clist">
        <li v-for="(s, i) in steps" :key="s.name" class="crow" data-reveal :style="{ '--ri': i }">
          <span class="cbox" aria-hidden="true"></span>
          <div class="cmain">
            <h2 class="cname">{{ s.name }}</h2>
            <p class="cdesc">{{ s.desc }}</p>
          </div>
          <button class="cact" type="button" @click="go(s.to)">去做这步</button>
        </li>
      </ol>

      <button class="g-cta" data-reveal type="button" @click="start">免费试用</button>
      <div class="sec-rule" aria-hidden="true"></div>
      <h2 class="faq-h">常见问题</h2>
    </section>

    <!-- 下折：常见问题 -->
    <section class="faq" aria-label="常见问题">
      <ul class="faq-list">
        <li v-for="(f, i) in faqs" :key="i" class="frow" data-reveal :class="{ open: openIdx === i }">
          <button class="fq" type="button" @click="toggle(i)" :aria-expanded="openIdx === i" :aria-controls="'faq-a-' + i">
            <span class="fqt">{{ f.q }}</span>
            <span class="fplus" aria-hidden="true"></span>
          </button>
          <p v-show="openIdx === i" :id="'faq-a-' + i" class="fa">{{ f.a }}</p>
        </li>
      </ul>
    </section>

    <PublicFooter />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'

const router = useRouter()
const openIdx = ref(0)

const steps = [
  { name: '注册登录', desc: '拿到工作台账号', to: '/login' },
  { name: '创建模板', desc: '存好你的校规方案', to: '/template-market' },
  { name: '上传排版', desc: '得到排版结果', to: '/tasks' },
  { name: '下载交付', desc: '下载合格论文', to: '/tasks' }
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

function go(to) {
  router.push(to)
}
function start() {
  router.push(localStorage.getItem('token') ? '/tasks' : '/login')
}

onMounted(() => {
  const els = document.querySelectorAll('[data-reveal]')
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

/* 导航对齐样张：102 高、无下边线 */
.page :deep(.pn) {
  height: calc(102 * var(--u));
  border-bottom: 0;
  padding: calc(14 * var(--u)) calc(80 * var(--u)) 0;
}

.wall {
  position: relative;
  height: calc(922 * var(--u));
  overflow: hidden;
}

.g-title {
  position: absolute;
  left: calc(179 * var(--u));
  top: calc(52 * var(--u));
  margin: 0;
  font-size: calc(80 * var(--u));
  font-weight: 900;
  line-height: 1.1;
  letter-spacing: 0;
  color: var(--ink);
}
.g-sub {
  position: absolute;
  left: calc(179 * var(--u));
  top: calc(152 * var(--u));
  margin: 0;
  font-size: calc(30 * var(--u));
  font-weight: 400;
  letter-spacing: 0.05em;
  color: var(--ink);
}

/* 清单：四行，行间 1px 细线 */
.clist {
  position: absolute;
  left: calc(160 * var(--u));
  top: calc(233 * var(--u));
  width: calc(1250 * var(--u));
  height: calc(570 * var(--u));
  margin: 0;
  padding: 0;
  list-style: none;
}
.crow {
  position: relative;
  height: calc(137.5 * var(--u));
}
.crow + .crow {
  border-top: 1px solid var(--hair-2);
}
.cbox {
  position: absolute;
  left: calc(34 * var(--u));
  top: calc(31 * var(--u));
  width: calc(66 * var(--u));
  height: calc(66 * var(--u));
  border: calc(2.5 * var(--u)) solid var(--ink);
  background: #fff;
}
.cmain {
  position: absolute;
  left: calc(132 * var(--u));
  top: calc(27 * var(--u));
}
.cname {
  margin: 0;
  font-size: calc(33 * var(--u));
  font-weight: 700;
  line-height: 1.3;
  color: var(--ink);
}
.cdesc {
  margin: calc(6 * var(--u)) 0 0;
  font-size: calc(14 * var(--u));
  font-weight: 400;
  color: var(--ink-2);
}
.cact {
  background: none;
  border: 0;
  font-family: inherit;
  padding: 0 0 calc(3 * var(--u));
  position: absolute;
  right: calc(26 * var(--u));
  top: calc(54 * var(--u));
  font-size: calc(25 * var(--u));
  line-height: 1.2;
  font-weight: 500;
  color: var(--red);
  border-bottom: calc(3 * var(--u)) solid var(--red);
  padding-bottom: calc(3 * var(--u));
  cursor: pointer;
}
.cact:hover {
  color: #b91d1e;
}

.g-cta {
  position: absolute;
  left: calc(179 * var(--u));
  top: calc(758 * var(--u));
  width: calc(220 * var(--u));
  height: calc(76 * var(--u));
  display: grid;
  place-items: center;
  background: var(--red);
  color: #fff;
  border: 0;
  font-family: inherit;
  font-size: calc(24 * var(--u));
  font-weight: 700;
  cursor: pointer;
}
.g-cta:hover {
  background: #b91d1e;
}
.sec-rule {
  position: absolute;
  left: calc(160 * var(--u));
  top: calc(858 * var(--u));
  width: calc(1250 * var(--u));
  border-top: 1px solid var(--hair-2);
}
.faq-h {
  position: absolute;
  left: calc(179 * var(--u));
  top: calc(883 * var(--u));
  margin: 0;
  font-size: calc(30 * var(--u));
  font-weight: 900;
  color: var(--ink);
}

/* ---------- 动效：入场 reveal + 行悬停 ---------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(14px);
}
[data-reveal][data-in="1"] {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.crow[data-in="1"] {
  transition-delay: calc(var(--ri, 0) * 60ms);
}
.crow:hover {
  background: #ffffff;
}
.crow:hover .cbox {
  border-color: var(--red);
}
.crow:hover .cbox::after {
  content: '';
  position: absolute;
  inset: calc(12 * var(--u));
  background: var(--red);
}
.cbox {
  transition: border-color 0.15s ease;
}
@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
  .fplus::after {
    transition: none;
  }
}

/* ---------- 焦点可达 ---------- */
.cact:focus-visible,
.g-cta:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: 2px;
}

::selection {
  background: var(--red);
  color: #fff;
}

/* ---------- 下折：常见问题 ---------- */
.faq {
  padding: calc(40 * var(--u)) calc(160 * var(--u)) calc(90 * var(--u));
}
.faq-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.frow {
  border-top: 1px solid var(--hair-2);
}
.frow:last-child {
  border-bottom: 1px solid var(--hair-2);
}
.fq {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: calc(24 * var(--u));
  background: none;
  border: 0;
  padding: calc(26 * var(--u)) calc(4 * var(--u));
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.fqt {
  font-size: calc(22 * var(--u));
  font-weight: 700;
  color: var(--ink);
}
.fq:hover .fqt {
  color: var(--red);
}
.fplus {
  position: relative;
  flex: none;
  width: calc(22 * var(--u));
  height: calc(22 * var(--u));
}
.fplus::before,
.fplus::after {
  content: '';
  position: absolute;
  background: var(--red);
}
.fplus::before {
  left: 0;
  right: 0;
  top: calc(50% - calc(1.5 * var(--u)));
  height: calc(3 * var(--u));
}
.fplus::after {
  top: 0;
  bottom: 0;
  left: calc(50% - calc(1.5 * var(--u)));
  width: calc(3 * var(--u));
  transition: transform 0.15s ease;
}
.frow.open .fplus::after {
  transform: scaleY(0);
}
.fa {
  margin: 0;
  padding: 0 calc(4 * var(--u)) calc(28 * var(--u));
  max-width: calc(980 * var(--u));
  font-size: calc(17 * var(--u));
  line-height: 1.9;
  color: var(--ink-2);
}
.fq:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: 2px;
}

/* ---------- 窄屏基础兜底（响应式阶段细化） ---------- */
@media (max-width: 900px) {
  .page :deep(.pn) {
    height: auto;
    padding: 10px 16px;
    border-bottom: 1px solid var(--hair);
  }
  .wall {
    height: auto;
    padding: 16px;
  }
  .g-title, .g-sub, .clist, .g-cta, .faq-h {
    position: static;
  }
  .g-title { font-size: 34px; }
  .g-sub { font-size: 15px; margin: 8px 0 20px; }
  .clist { width: 100%; height: auto; }
  .crow { height: auto; padding: 14px 0; display: flex; align-items: center; gap: 12px; }
  .cbox { position: static; width: 22px; height: 22px; border-width: 1.5px; flex: none; }
  .cmain { position: static; flex: 1; }
  .cname { font-size: 17px; }
  .cdesc { font-size: 13px; margin-top: 2px; }
  .cact { position: static; font-size: 14px; border-bottom-width: 1px; }
  .g-cta { width: 100%; height: 44px; font-size: 15px; margin: 18px 0; }
  .faq-h { font-size: 20px; }
  .faq { padding: 24px 16px 48px; }
  .fqt { font-size: 15px; }
  .fa { font-size: 13px; max-width: 100%; }
  .fplus { width: 14px; height: 14px; }
}
</style>
