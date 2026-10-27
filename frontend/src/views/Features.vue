<template>
  <div class="page">
    <PublicNav />

    <!-- 首屏：样张墙 · 批注式 -->
    <section class="wall" aria-label="功能总览">
      <article class="sheet">
        <h1 class="chapter">第三章 系统设计</h1>
        <h2 class="sec-h">3.1 系统概述</h2>
        <div class="body-p">
          <p>系统采用规则驱动的排版架构：解析器提取文档结构，规则引擎匹配高校格式规则，排版器套用页边距、字体与行距。</p>
          <p>表 3-1 列出核心模块的职责划分，图 3-1 说明系统总体架构，页边距、字号、行距与编号格式均由规则引擎统一控制。</p>
        </div>
        <div class="tbl">
          <p class="tbl-cap">表 3-1 各模块职责</p>
          <table>
            <colgroup>
              <col style="width: 18%" /><col style="width: 26%" /><col style="width: 18%" /><col style="width: 18%" /><col style="width: 20%" />
            </colgroup>
            <thead>
              <tr><th>模块</th><th>职责</th><th>输入</th><th>输出</th><th>状态</th></tr>
            </thead>
            <tbody>
              <tr><td>解析器</td><td>提取章节段落结构</td><td>DOCX</td><td>结构树</td><td>已上线</td></tr>
              <tr><td>规则引擎</td><td>匹配高校格式规则</td><td>规则集</td><td>标记结果</td><td>已上线</td></tr>
              <tr><td>排版器</td><td>套用字体行距页边距</td><td>结构树</td><td>DOCX</td><td>已上线</td></tr>
            </tbody>
          </table>
        </div>
        <p class="fig-cap">图 3-1 系统总体架构</p>
        <div class="seal" aria-hidden="true"><span>合格</span></div>
      </article>

      <aside class="anns" aria-label="引擎处理要点">
        <div class="ann" v-for="(a, i) in anns" :key="i">
          <span class="ann-n">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="ann-t">{{ a }}</span>
        </div>
      </aside>

      <nav class="f-band" aria-label="页面导航">
        <a
          v-for="l in bandLinks"
          :key="l.label"
          class="f-link"
          @click="goBand(l.to)"
        >{{ l.label }}</a>
      </nav>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import PublicNav from '../components/PublicNav.vue'

const router = useRouter()

const anns = [
  '标题层级自动识别',
  '三线表 顶线 1.5pt',
  '图表题注连续编号',
  '行距固定 22 磅',
  '参考文献 GB/T 7714',
  '目录自动生成'
]

const bandLinks = [
  { label: '自动排版', to: '/features' },
  { label: '智能校对', to: '/features' },
  { label: '格式模板', to: '/template-market' },
  { label: '引用管理', to: '/features' },
  { label: '论文检测', to: '/features' },
  { label: '学术资源', to: '/features' },
  { label: '客户评价', to: '/features' },
  { label: '关于我们', to: '/home' }
]

function goBand(to) {
  if (to && to !== '/features') router.push(to)
}
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

/* ---------- 首屏画布：1536 x 1024 ---------- */
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

/* ---------- 样张纸面 ---------- */
.sheet {
  position: absolute;
  left: calc(153.6 * var(--u));
  top: calc(0.4 * var(--u));
  width: calc(921.6 * var(--u));
  height: calc(819.2 * var(--u));
  background: #fff;
  border: 1px solid var(--hair-2);
}
.chapter {
  position: absolute;
  left: calc(18 * var(--u));
  top: calc(68 * var(--u));
  margin: 0;
  font-size: calc(38 * var(--u));
  font-weight: 900;
  line-height: 1.1;
  letter-spacing: 0;
  color: var(--ink);
}
.sec-h {
  position: absolute;
  left: calc(18 * var(--u));
  top: calc(140 * var(--u));
  margin: 0;
  font-size: calc(26 * var(--u));
  font-weight: 900;
  line-height: 1.2;
  color: var(--ink);
}
.body-p {
  position: absolute;
  left: calc(18 * var(--u));
  top: calc(190 * var(--u));
  width: calc(740 * var(--u));
}
.body-p p {
  margin: 0;
  font-size: calc(18 * var(--u));
  font-weight: 500;
  line-height: 1.78;
  text-indent: 2em;
  color: #212122;
}

/* 三线表 */
.tbl {
  position: absolute;
  left: calc(23 * var(--u));
  top: calc(380 * var(--u));
  width: calc(743 * var(--u));
}
.tbl-cap {
  margin: 0 0 calc(8 * var(--u));
  text-align: center;
  font-size: calc(20 * var(--u));
  font-weight: 700;
  color: var(--ink);
}
.tbl table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: calc(19 * var(--u));
}
.tbl th,
.tbl td {
  padding: calc(8 * var(--u)) calc(6 * var(--u));
  text-align: center;
  color: var(--ink);
}
.tbl th {
  font-weight: 700;
}
.tbl thead tr {
  border-top: calc(2.4 * var(--u)) solid var(--ink);
  border-bottom: calc(1.2 * var(--u)) solid var(--ink);
}
.tbl tbody tr:last-child {
  border-bottom: calc(2.4 * var(--u)) solid var(--ink);
}

.fig-cap {
  position: absolute;
  left: calc(26 * var(--u));
  top: calc(606 * var(--u));
  margin: 0;
  font-size: calc(21 * var(--u));
  font-weight: 400;
  color: var(--ink);
}

/* 合格章 */
.seal {
  position: absolute;
  left: calc(610 * var(--u));
  top: calc(643 * var(--u));
  width: calc(160 * var(--u));
  height: calc(74 * var(--u));
  display: grid;
  place-items: center;
  border: calc(3.5 * var(--u)) solid var(--red);
  border-radius: calc(14 * var(--u));
  color: var(--red);
  transform: rotate(-10deg);
}
.seal span {
  font-size: calc(32 * var(--u));
  font-weight: 900;
  letter-spacing: 0.12em;
}

/* ---------- 右侧批注列 ---------- */
.anns {
  position: absolute;
  left: calc(1085 * var(--u));
  top: calc(94 * var(--u));
  width: calc(420 * var(--u));
}
.ann {
  position: relative;
  height: calc(97 * var(--u));
  display: flex;
  align-items: center;
  gap: calc(24 * var(--u));
}
.ann::before {
  content: '';
  position: absolute;
  left: calc(-142 * var(--u));
  top: 50%;
  width: calc(140 * var(--u));
  border-top: calc(2 * var(--u)) solid var(--red);
}
.ann::after {
  content: '';
  position: absolute;
  left: calc(-148 * var(--u));
  top: calc(50% - 3.5 * var(--u));
  width: calc(8 * var(--u));
  height: calc(8 * var(--u));
  background: var(--red);
  border-radius: 50%;
}
.ann-n {
  font-size: calc(34 * var(--u));
  font-weight: 900;
  color: var(--red);
  letter-spacing: 0.02em;
}
.ann-t {
  font-size: calc(24 * var(--u));
  font-weight: 500;
  color: var(--ink);
}

/* ---------- 底部链接带 ---------- */
.f-band {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(102.4 * var(--u));
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: calc(14 * var(--u)) calc(128 * var(--u)) 0;
}
.f-link {
  font-size: calc(20 * var(--u));
  font-weight: 500;
  color: var(--ink);
  cursor: pointer;
}
.f-link:hover {
  color: var(--red);
}

/* ---------- 焦点可达 ---------- */
.f-link:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: 2px;
}

::selection {
  background: var(--red);
  color: #fff;
}

/* ---------- 窄屏基础兜底（响应式阶段细化） ---------- */
@media (max-width: 900px) {
  .wall {
    height: auto;
    padding: 20px 16px 0;
  }
  .sheet,
  .anns,
  .f-band {
    position: static;
  }
  .sheet {
    width: 100%;
    height: auto;
    padding: 24px 16px 40px;
  }
  .chapter, .sec-h, .body-p, .tbl, .fig-cap, .seal {
    position: static;
    font-size: 18px;
  }
  .body-p { width: 100%; margin: 12px 0; }
  .body-p p { font-size: 14px; }
  .tbl { width: 100%; margin: 16px 0; }
  .tbl table { font-size: 13px; }
  .anns { width: 100%; margin: 20px 0; }
  .ann { height: 44px; gap: 10px; }
  .ann::before, .ann::after { display: none; }
  .ann-n { font-size: 18px; }
  .ann-t { font-size: 14px; }
  .f-band {
    height: auto;
    flex-wrap: wrap;
    gap: 12px;
    padding: 14px 16px;
    justify-content: flex-start;
  }
  .f-link { font-size: 13px; }
}
</style>
