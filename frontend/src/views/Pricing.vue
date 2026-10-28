<template>
  <div class="page">
    <PublicNav />

    <!-- 首屏：三联竖柱价目 —— 数字与权益全部来自页面既有真实文案，付费档位标注「待开放」 -->
    <section class="wall" aria-label="价格">
      <p class="p-kicker" data-reveal><i class="p-sq" aria-hidden="true"></i>PRICING</p>
      <div class="p-lead" data-reveal>
        <div class="p-head">
          <h1 class="p-title">简单透明的定价</h1>
          <p class="p-sub">现在免费使用，毕业季无需额外花费</p>
        </div>
        <button class="btn-red" type="button" @click="start">立即开始</button>
      </div>

      <div class="cols">
        <article
          v-for="(p, i) in plans"
          :key="p.name"
          class="col"
          :class="{ 'col-mid': p.featured }"
          data-reveal
          :style="{ '--ri': i }"
        >
          <div class="c-head">
            <h2 class="c-name">{{ p.name }}</h2>
            <span v-if="p.current" class="tag tag-red">当前计划</span>
            <span v-if="p.featured" class="tag tag-ink">最受欢迎</span>
            <span v-if="!p.current" class="tag tag-grey">待开放</span>
          </div>

          <p class="c-price">
            <span class="c-sym">¥</span><span class="c-num">{{ p.price }}</span><span class="c-unit">/{{ p.unit }}</span>
          </p>

          <p class="c-desc">{{ p.desc }}</p>

          <ul class="c-feats">
            <li v-for="(f, j) in p.features" :key="j">{{ f }}</li>
          </ul>

          <div class="c-foot">
            <button
              v-if="p.featured"
              class="btn-ghost"
              type="button"
              @click="choose(p)"
            >{{ p.btn }}</button>
            <a
              v-else-if="p.name === '团队版'"
              class="link-biz"
              :href="'mailto:2651896126@qq.com?subject=' + encodeURIComponent('团队版咨询')"
            >联系商务</a>
          </div>
        </article>
      </div>

      <p class="notice" data-reveal>本项目为学习交流用途，当前所有功能免费开放。如需商业授权请联系我们。</p>
    </section>

    <PublicFooter />
  </div>
</template>

<script setup>
import { nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'

const router = useRouter()

const plans = [
  {
    name: '免费版',
    price: '0',
    unit: '月',
    desc: '满足个人毕业论文排版需求',
    features: ['论文智能排版', '三线表 / ER 图 / 系统图', '5 个自定义模板', 'PDF 预览'],
    btn: '立即开始',
    current: true
  },
  {
    name: '专业版',
    price: '29',
    unit: '月',
    desc: '面向需要多模板、高频率排版的用户',
    features: ['无限模板数量', '高级排版规则', '批量排版任务', '优先处理队列', '团队协作'],
    btn: '升级专业版',
    featured: true
  },
  {
    name: '团队版',
    price: '99',
    unit: '月',
    desc: '适合实验室 / 课题组统一排版规范',
    features: ['专业版全部功能', '5 个成员席位', '共享模板库', '管理员控制台', '专属支持'],
    btn: '联系商务'
  }
]

function start() {
  router.push(localStorage.getItem('token') ? '/templates' : { path: '/login', query: { redirect: '/templates' } })
}

function choose(plan) {
  if (plan.current) {
    start()
    return
  }
  if (plan.name === '团队版') {
    window.location.href = 'mailto:2651896126@qq.com?subject=' + encodeURIComponent('团队版咨询')
    return
  }
  ElMessage.info(plan.name + ' 即将开放，敬请期待')
}

// 入场动画：挂载后收集，缺失兜底（与 Cases 同一套模式）
async function setupReveal() {
  await nextTick()
  const els = [...document.querySelectorAll('[data-reveal]:not([data-in])')]
  if (!els.length) return
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
}

onMounted(() => {
  setupReveal()
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

/* ---------- 标题区 ---------- */
.wall {
  min-height: calc(922 * var(--u));
  display: flex;
  flex-direction: column;
}

.p-kicker {
  margin: calc(64 * var(--u)) calc(112 * var(--u)) 0;
  display: flex;
  align-items: center;
  gap: calc(14 * var(--u));
  font-size: calc(16 * var(--u));
  font-weight: 700;
  letter-spacing: 0.35em;
  color: #8a8b8b;
}
.p-sq {
  width: calc(12 * var(--u));
  height: calc(12 * var(--u));
  background: var(--red);
}

.p-lead {
  margin: calc(28 * var(--u)) calc(112 * var(--u)) 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: calc(40 * var(--u));
}
.p-title {
  margin: 0;
  font-size: calc(84 * var(--u));
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  color: var(--ink);
}
.p-sub {
  margin: calc(22 * var(--u)) 0 0;
  font-size: calc(30 * var(--u));
  font-weight: 400;
  letter-spacing: 0.05em;
  color: var(--ink-2);
}

.btn-red {
  flex: none;
  height: calc(58 * var(--u));
  padding: 0 calc(42 * var(--u));
  background: var(--red);
  color: #fff;
  border: 0;
  font-family: inherit;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s ease;
}
.btn-red:hover {
  background: #b91d1e;
}

/* ---------- 三联竖柱 ---------- */
.cols {
  margin-top: calc(72 * var(--u));
  border-top: 1px solid var(--hair);
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  flex: 1;
}
.col {
  position: relative;
  padding: calc(46 * var(--u)) calc(56 * var(--u)) calc(56 * var(--u));
  display: flex;
  flex-direction: column;
}
.col + .col {
  border-left: 1px solid var(--hair);
}
.col-mid {
  background: var(--ink);
  color: var(--paper);
}

.c-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: calc(14 * var(--u));
}
.c-name {
  margin: 0;
  font-size: calc(30 * var(--u));
  font-weight: 900;
  line-height: 1;
  color: inherit;
}
.tag {
  font-size: calc(14 * var(--u));
  font-weight: 700;
  line-height: 1;
  padding: calc(6 * var(--u)) calc(10 * var(--u));
}
.tag-red {
  border: 1px solid var(--red);
  color: var(--red);
}
.tag-ink {
  background: var(--paper);
  color: var(--ink);
}
.tag-grey {
  border: 1px solid var(--hair);
  color: #8a8b8b;
}
.col-mid .tag-grey {
  border-color: rgba(255, 255, 255, 0.35);
  color: rgba(255, 255, 255, 0.75);
}

.c-price {
  margin: calc(96 * var(--u)) 0 0;
  display: flex;
  align-items: baseline;
  gap: calc(8 * var(--u));
  line-height: 1;
}
.c-sym {
  font-size: calc(36 * var(--u));
  font-weight: 900;
}
.c-num {
  font-size: calc(148 * var(--u));
  font-weight: 900;
  letter-spacing: -0.02em;
}
.c-unit {
  font-size: calc(20 * var(--u));
  font-weight: 400;
  color: #8a8b8b;
}
.col-mid .c-unit {
  color: rgba(255, 255, 255, 0.6);
}

.c-desc {
  margin: calc(28 * var(--u)) 0 0;
  font-size: calc(17 * var(--u));
  font-weight: 400;
  line-height: 1.6;
  color: #8a8b8b;
}
.col-mid .c-desc {
  color: rgba(255, 255, 255, 0.6);
}

.c-feats {
  list-style: none;
  margin: calc(52 * var(--u)) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: calc(20 * var(--u));
}
.c-feats li {
  position: relative;
  padding-left: calc(22 * var(--u));
  font-size: calc(17 * var(--u));
  font-weight: 500;
  line-height: 1.5;
  color: var(--ink-2);
}
.c-feats li::before {
  content: '';
  position: absolute;
  left: 0;
  top: calc(9 * var(--u));
  width: calc(8 * var(--u));
  height: calc(8 * var(--u));
  background: var(--ink);
}
.col-mid .c-feats li {
  color: rgba(255, 255, 255, 0.88);
}
.col-mid .c-feats li::before {
  background: var(--red);
}

.c-foot {
  margin-top: auto;
  padding-top: calc(48 * var(--u));
}
.btn-ghost {
  width: 100%;
  height: calc(56 * var(--u));
  background: var(--paper);
  color: var(--ink);
  border: 0;
  font-family: inherit;
  font-size: calc(19 * var(--u));
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s ease;
}
.btn-ghost:hover {
  background: #fff;
}
.link-biz {
  font-size: calc(19 * var(--u));
  font-weight: 500;
  color: var(--red);
  text-decoration: none;
  background-image: linear-gradient(var(--red), var(--red));
  background-repeat: no-repeat;
  background-position: 0 100%;
  background-size: 100% calc(2 * var(--u));
  padding-bottom: calc(4 * var(--u));
  transition: background-size 0.15s ease;
}
.link-biz:hover {
  background-size: 100% 100%;
}
.col:focus-within {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: calc(-2 * var(--u));
}

/* ---------- 免责 ---------- */
.notice {
  margin: 0;
  border-top: 1px solid var(--hair);
  padding: calc(26 * var(--u)) calc(112 * var(--u));
  font-size: calc(16 * var(--u));
  color: #8a8b8b;
  letter-spacing: 0.02em;
}

/* ---------- 动效：入场 reveal ---------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(14px);
}
[data-reveal][data-in="1"] {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.col[data-in="1"] {
  transition-delay: calc(var(--ri, 0) * 80ms);
}
@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
}

::selection {
  background: var(--red);
  color: #fff;
}

/* ---------- 窄屏兜底 ---------- */
@media (max-width: 900px) {
  .p-kicker {
    margin: 24px 16px 0;
    font-size: 12px;
    gap: 8px;
  }
  .p-sq {
    width: 9px;
    height: 9px;
  }
  .p-lead {
    margin: 14px 16px 0;
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
  }
  .p-title {
    font-size: 34px;
  }
  .p-sub {
    margin-top: 10px;
    font-size: 15px;
  }
  .btn-red {
    height: 44px;
    padding: 0 24px;
    font-size: 15px;
  }
  .cols {
    margin-top: 36px;
    grid-template-columns: 1fr;
  }
  .col {
    padding: 28px 20px 32px;
  }
  .col + .col {
    border-left: 0;
    border-top: 1px solid var(--hair);
  }
  .c-name {
    font-size: 20px;
  }
  .tag {
    font-size: 11px;
    padding: 5px 8px;
  }
  .c-price {
    margin-top: 32px;
    gap: 5px;
  }
  .c-sym {
    font-size: 20px;
  }
  .c-num {
    font-size: 64px;
  }
  .c-unit {
    font-size: 14px;
  }
  .c-desc {
    margin-top: 14px;
    font-size: 14px;
  }
  .c-feats {
    margin-top: 24px;
    gap: 12px;
  }
  .c-feats li {
    padding-left: 16px;
    font-size: 14px;
  }
  .c-feats li::before {
    top: 7px;
    width: 6px;
    height: 6px;
  }
  .c-foot {
    padding-top: 28px;
  }
  .btn-ghost {
    height: 46px;
    font-size: 15px;
  }
  .link-biz {
    font-size: 15px;
  }
  .notice {
    padding: 18px 16px;
    font-size: 13px;
  }
}
</style>
