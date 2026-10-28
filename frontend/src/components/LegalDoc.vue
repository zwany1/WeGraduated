<template>
  <div class="page">
    <PublicNav />

    <!-- 公共公文版式（B·左锚右文）：左侧编号目录锚点常驻，右侧长文流；条款文字一字不改 -->
    <div class="wrap">
      <aside class="lside-col">
        <div class="lside">
          <p class="l-kicker">{{ kicker }}</p>
          <h1 class="l-title">{{ title }}</h1>
          <p class="l-updated">更新日期：{{ updated }}</p>

          <nav class="toc" aria-label="章节目录">
            <button
              v-for="(s, i) in sections"
              :key="i"
              class="toc-item"
              :class="{ on: active === i }"
              type="button"
              @click="goSection(i)"
            >
              <span class="toc-num">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="toc-name">{{ s.title.replace(/^\d+\.\s*/, '') }}</span>
            </button>
          </nav>
        </div>
      </aside>

      <main class="rside">
        <section
          v-for="(s, i) in sections"
          :key="i"
          :id="'sec-' + i"
          class="sec"
          :class="{ on: active === i }"
          data-reveal
        >
          <h2 class="s-h">{{ s.title }}</h2>
          <p v-for="(p, j) in s.paragraphs" :key="'p' + j" class="s-para">{{ p }}</p>
          <ul v-if="s.list" class="s-list">
            <li v-for="(li, k) in s.list" :key="k">{{ li }}</li>
          </ul>
        </section>

        <p class="s-contact" data-reveal>
          {{ contactLead }}
          <a :href="'mailto:2651896126@qq.com'">2651896126@qq.com</a>
          与我们联系。
        </p>
      </main>
    </div>

    <PublicFooter />
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import PublicNav from '../components/PublicNav.vue'
import PublicFooter from '../components/PublicFooter.vue'

const props = defineProps({
  kicker: { type: String, required: true },
  title: { type: String, required: true },
  updated: { type: String, default: '2026年8月' },
  contactLead: { type: String, required: true },
  sections: { type: Array, required: true }
})

const active = ref(0)
let sectionObserver = null

function goSection(i) {
  const el = document.getElementById('sec-' + i)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 当前章高亮：滚动时以视口上部 1/3 处所在章节为准
function setupSectionObserver() {
  if (!('IntersectionObserver' in window)) return
  sectionObserver?.disconnect()
  sectionObserver = new IntersectionObserver(
    entries => {
      entries.forEach(en => {
        if (en.isIntersecting) {
          active.value = Number(en.target.id.split('-')[1])
        }
      })
    },
    { rootMargin: '-20% 0px -70% 0px', threshold: 0 }
  )
  props.sections.forEach((_, i) => {
    const el = document.getElementById('sec-' + i)
    if (el) sectionObserver.observe(el)
  })
}

// 入场动画：挂载后收集（内容同步渲染，无异步补收集需求；保留兜底）
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
  }, { threshold: 0.08 })
  els.forEach(e => io.observe(e))
}

onMounted(() => {
  setupReveal()
  setupSectionObserver()
})

onBeforeUnmount(() => {
  sectionObserver?.disconnect()
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

.wrap {
  display: flex;
  align-items: stretch;
  min-height: calc(100vh - 72px);
}

/* ---------- 左栏：目录锚 ---------- */
.lside-col {
  width: 30%;
  flex: none;
  border-right: 1px solid var(--hair);
  padding: calc(72 * var(--u)) calc(48 * var(--u)) calc(80 * var(--u)) calc(112 * var(--u));
}
.lside {
  position: sticky;
  top: calc(72px + 40 * var(--u));
}
.l-kicker {
  margin: 0;
  font-size: calc(15 * var(--u));
  font-weight: 700;
  letter-spacing: 0.3em;
  color: #8a8b8b;
}
.l-title {
  margin: calc(20 * var(--u)) 0 0;
  font-size: calc(44 * var(--u));
  font-weight: 900;
  line-height: 1.1;
  color: var(--ink);
}
.l-updated {
  margin: calc(14 * var(--u)) 0 0;
  font-size: calc(16 * var(--u));
  color: #8a8b8b;
}

.toc {
  margin-top: calc(48 * var(--u));
  display: flex;
  flex-direction: column;
}
.toc-item {
  display: flex;
  align-items: baseline;
  gap: calc(16 * var(--u));
  padding: calc(16 * var(--u)) 0;
  border: 0;
  border-top: 1px solid var(--hair-2);
  background: none;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease;
}
.toc-item:hover .toc-name {
  color: var(--red);
}
.toc-num {
  position: relative;
  flex: none;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  line-height: 1;
  color: var(--red);
  font-variant-numeric: tabular-nums;
}
.toc-item.on .toc-num::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: calc(-7 * var(--u));
  width: 100%;
  height: calc(4 * var(--u));
  background: var(--red);
}
.toc-name {
  font-size: calc(17 * var(--u));
  font-weight: 500;
  line-height: 1.4;
  color: var(--ink);
  transition: color 0.15s ease;
}

/* ---------- 右栏：长文流 ---------- */
.rside {
  flex: 1;
  min-width: 0;
  padding: calc(72 * var(--u)) calc(112 * var(--u)) calc(80 * var(--u)) calc(64 * var(--u));
}
.sec {
  padding-bottom: calc(44 * var(--u));
  margin-bottom: calc(44 * var(--u));
  border-bottom: 1px solid var(--hair-2);
  scroll-margin-top: calc(72px + 24 * var(--u));
}
.sec:last-of-type {
  border-bottom: 0;
}
.s-h {
  margin: 0 0 calc(24 * var(--u));
  font-size: calc(28 * var(--u));
  font-weight: 900;
  line-height: 1.3;
  color: var(--ink);
}
.sec.on .s-h {
  color: var(--ink);
}
.s-para {
  margin: 0 0 calc(16 * var(--u));
  font-size: calc(17 * var(--u));
  font-weight: 400;
  line-height: 1.85;
  color: var(--ink-2);
}
.s-list {
  margin: calc(6 * var(--u)) 0 0;
  padding: 0;
  list-style: none;
}
.s-list li {
  position: relative;
  padding-left: calc(24 * var(--u));
  margin-bottom: calc(14 * var(--u));
  font-size: calc(17 * var(--u));
  font-weight: 400;
  line-height: 1.8;
  color: var(--ink-2);
}
.s-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: calc(10 * var(--u));
  width: calc(8 * var(--u));
  height: calc(8 * var(--u));
  background: var(--ink);
}
.s-contact {
  margin: calc(56 * var(--u)) 0 0;
  padding-top: calc(24 * var(--u));
  border-top: 1px solid var(--hair-2);
  font-size: calc(16 * var(--u));
  color: #8a8b8b;
}
.s-contact a {
  color: var(--red);
  text-decoration: none;
  background-image: linear-gradient(var(--red), var(--red));
  background-repeat: no-repeat;
  background-position: 0 100%;
  background-size: 100% calc(2 * var(--u));
  padding-bottom: calc(3 * var(--u));
  transition: background-size 0.15s ease;
}
.s-contact a:hover {
  background-size: 100% 100%;
}

/* ---------- 动效 ---------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(14px);
}
[data-reveal][data-in="1"] {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
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

/* ---------- 窄屏 ---------- */
@media (max-width: 900px) {
  .wrap {
    flex-direction: column;
  }
  .lside-col {
    width: auto;
    border-right: 0;
    border-bottom: 1px solid var(--hair);
    padding: 24px 16px 20px;
  }
  .lside {
    position: static;
  }
  .l-kicker {
    font-size: 11px;
  }
  .l-title {
    margin-top: 10px;
    font-size: 28px;
  }
  .l-updated {
    margin-top: 8px;
    font-size: 13px;
  }
  .toc {
    margin-top: 18px;
  }
  .toc-item {
    padding: 10px 0;
    gap: 12px;
  }
  .toc-num {
    font-size: 14px;
  }
  .toc-item.on .toc-num::after {
    bottom: -5px;
    height: 3px;
  }
  .toc-name {
    font-size: 14px;
  }
  .rside {
    padding: 28px 16px 48px;
  }
  .sec {
    padding-bottom: 28px;
    margin-bottom: 28px;
    scroll-margin-top: 80px;
  }
  .s-h {
    margin-bottom: 14px;
    font-size: 20px;
  }
  .s-para {
    margin-bottom: 10px;
    font-size: 14px;
  }
  .s-list li {
    padding-left: 16px;
    margin-bottom: 10px;
    font-size: 14px;
  }
  .s-list li::before {
    top: 7px;
    width: 6px;
    height: 6px;
  }
  .s-contact {
    margin-top: 32px;
    padding-top: 16px;
    font-size: 13px;
  }
}
</style>
