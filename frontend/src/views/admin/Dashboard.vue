<template>
  <div class="dashboard">
    <!-- Greeting -->
    <div class="greeting">
      <div>
        <h2 class="greet-title">运营概览</h2>
        <p class="greet-desc">{{ greeting }}，这是论文排版系统当前的运行概况。</p>
      </div>
      <div class="greet-date">
        <span class="date-cn">{{ dateCn }}</span>
        <span class="date-en">{{ dateEn }}</span>
      </div>
    </div>

    <!-- Stat cards -->
    <div class="stat-grid">
      <div v-for="(c, i) in cards" :key="c.key" class="stat-card" :style="{ '--d': i * 60 + 'ms' }">
        <div class="stat-head">
          <span class="stat-kicker">{{ c.kicker }}</span>
          <span class="stat-icon" :style="{ color: c.color, background: c.tint }" v-html="c.icon"></span>
        </div>
        <div class="stat-num" :style="{ color: c.color }">{{ fmt(c.value) }}</div>
        <div class="stat-foot">
          <span class="stat-caption">{{ c.caption }}</span>
          <span class="stat-delta" :style="{ color: c.deltaColor }">{{ c.delta }}</span>
        </div>
      </div>
    </div>

    <!-- Charts -->
    <div class="chart-grid">
      <!-- Register trend -->
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">近七日注册趋势</h3>
            <span class="panel-sub">近 7 天每日新增用户数</span>
          </div>
          <span class="panel-badge">USER</span>
        </div>
        <div class="bars" v-if="registerTrend.length">
          <div v-for="(b, i) in registerTrend" :key="i" class="bar-col">
            <div class="bar-track">
              <div class="bar-fill" :class="{ hot: b.value === maxReg }"
                :style="{ height: barH(b.value, maxReg), '--delay': i * 70 + 'ms' }"></div>
            </div>
            <span class="bar-label">{{ b.label }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无数据</div>
      </section>

      <!-- Task trend -->
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">近七日任务趋势</h3>
            <span class="panel-sub">近 7 天每日提交的排版任务数</span>
          </div>
          <span class="panel-badge">TASK</span>
        </div>
        <div class="bars" v-if="taskTrend.length">
          <div v-for="(b, i) in taskTrend" :key="i" class="bar-col">
            <div class="bar-track">
              <div class="bar-fill gold" :class="{ hot: b.value === maxTask }"
                :style="{ height: barH(b.value, maxTask), '--delay': i * 70 + 'ms' }"></div>
            </div>
            <span class="bar-label">{{ b.label }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无数据</div>
      </section>

      <!-- Task status ring -->
      <section class="panel ring-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">任务状态分布</h3>
            <span class="panel-sub">全部排版任务的成功与失败概况</span>
          </div>
          <span class="panel-badge">STATUS</span>
        </div>
        <div class="ring-wrap">
          <div class="ring-box">
            <svg viewBox="0 0 120 120" class="ring-svg">
              <circle class="ring-bg" cx="60" cy="60" r="48" />
              <circle v-for="seg in ringSegs" :key="seg.key"
                class="ring-seg"
                :cx="60" :cy="60" :r="48"
                :stroke="seg.color"
                :stroke-dasharray="seg.dash"
                :stroke-dashoffset="seg.offset"
                :style="{ '--delay': seg.index * 160 + 'ms' }" />
            </svg>
            <div class="ring-center">
              <div class="ring-total">{{ fmt(stats.taskCount) }}</div>
              <div class="ring-total-label">任务总数</div>
            </div>
          </div>
          <div class="legend">
            <div v-for="seg in ringSegs" :key="'l' + seg.key" class="legend-item">
              <span class="legend-dot" :style="{ background: seg.color }"></span>
              <span class="legend-name">{{ seg.label }}</span>
              <span class="legend-val">{{ fmt(seg.value) }}</span>
              <span class="legend-pct">{{ seg.pct }}%</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview } from '../../api/admin'

const stats = ref({
  userCount: 0, adminCount: 0, templateCount: 0, taskCount: 0, paperCount: 0,
  taskStatus: { PENDING: 0, PROCESSING: 0, SUCCESS: 0, FAILED: 0 },
  registerTrend: [], taskTrend: []
})

const fmt = n => (n || 0).toLocaleString()

const cards = computed(() => [
  {
    key: 'user', kicker: '注册用户', value: stats.value.userCount,
    caption: `其中管理员 ${fmt(stats.value.adminCount)} 名`,
    delta: 'USERS', deltaColor: '#3a6ea5',
    color: '#3a6ea5', tint: 'rgba(58,110,165,0.1)',
    icon: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>'
  },
  {
    key: 'template', kicker: '格式模板', value: stats.value.templateCount,
    caption: '可视化配置的排版模板',
    delta: 'TEMPLATES', deltaColor: '#b08a3e',
    color: '#b08a3e', tint: 'rgba(201,164,92,0.14)',
    icon: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>'
  },
  {
    key: 'task', kicker: '排版任务', value: stats.value.taskCount,
    caption: `成功 ${fmt(stats.value.taskStatus.SUCCESS)} 次`,
    delta: pct(stats.value.taskCount, stats.value.taskStatus.SUCCESS), deltaColor: '#3f7d5a',
    color: '#3f7d5a', tint: 'rgba(63,125,90,0.1)',
    icon: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>'
  },
  {
    key: 'paper', kicker: '论文文件', value: stats.value.paperCount,
    caption: '已上传待排版的论文文档',
    delta: 'FILES', deltaColor: '#b23a2e',
    color: '#b23a2e', tint: 'rgba(178,58,46,0.08)',
    icon: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>'
  }
])

const registerTrend = computed(() =>
  (stats.value.registerTrend || []).map(t => ({ label: t.date, value: t.count })))
const taskTrend = computed(() =>
  (stats.value.taskTrend || []).map(t => ({ label: t.date, value: t.count })))

const maxReg = computed(() => Math.max(1, ...registerTrend.value.map(b => b.value)))
const maxTask = computed(() => Math.max(1, ...taskTrend.value.map(b => b.value)))
const barH = (v, m) => Math.max(4, Math.round((v / m) * 100)) + '%'

const STATUS_META = {
  PENDING: { label: '排队中', color: '#9aa3b2' },
  PROCESSING: { label: '排版中', color: '#3a6ea5' },
  SUCCESS: { label: '已完成', color: '#3f7d5a' },
  FAILED: { label: '失败', color: '#b23a2e' }
}

const ringSegs = computed(() => {
  const R = 48
  const C = 2 * Math.PI * R
  const total = Math.max(1, stats.value.taskCount)
  let acc = 0
  const segs = []
  Object.keys(STATUS_META).forEach((k, i) => {
    const v = stats.value.taskStatus[k] || 0
    const frac = v / total
    const len = frac * C
    segs.push({
      key: k, label: STATUS_META[k].label, color: STATUS_META[k].color,
      value: v, pct: Math.round(frac * 100),
      dash: `${len} ${C - len}`,
      offset: -acc,
      index: i
    })
    acc += len
  })
  return segs
})

function pct(total, part) {
  if (!total) return '0%'
  return Math.round((part / total) * 100) + '%'
}

const now = new Date()
const dateCn = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日`
const dateEn = now.toDateString().toUpperCase()
const hour = now.getHours()
const greeting = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

onMounted(async () => {
  try {
    stats.value = await getOverview()
  } catch (e) {}
})
</script>

<style scoped>
.dashboard {
  --ink: #0d1b2e;
  --serif: 'Songti SC', 'STSong', 'SimSun', serif;
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: dash-in 0.4s ease both;
}
@keyframes dash-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: none; }
}

/* Greeting */
.greeting {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 4px 2px 2px;
}
.greet-title {
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.06em;
  margin: 0 0 6px;
}
.greet-desc {
  font-size: 13px;
  color: #7a7d8a;
  margin: 0;
}
.greet-date {
  text-align: right;
  line-height: 1.3;
}
.date-cn {
  display: block;
  font-family: var(--serif);
  font-size: 15px;
  color: #3a3f4d;
}
.date-en {
  display: block;
  font-size: 10px;
  letter-spacing: 0.18em;
  color: #b3a583;
}

/* Stat cards */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.stat-card {
  background: var(--card, #fffdf9);
  border: 1px solid var(--line, #e6ded0);
  border-radius: 14px;
  padding: 18px 20px 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04);
  animation: card-in 0.45s ease both;
  animation-delay: var(--d);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(13, 27, 46, 0.09);
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: var(--card-accent, #3a6ea5);
  opacity: 0;
  transition: opacity 0.25s;
}
.stat-card:nth-child(1) { --card-accent: #3a6ea5; }
.stat-card:nth-child(2) { --card-accent: #c9a45c; }
.stat-card:nth-child(3) { --card-accent: #3f7d5a; }
.stat-card:nth-child(4) { --card-accent: #b23a2e; }
.stat-card:hover::before { opacity: 1; }
@keyframes card-in {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: none; }
}
.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.stat-kicker {
  font-size: 11px;
  letter-spacing: 0.14em;
  color: #9a917d;
}
.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-num {
  font-family: var(--serif);
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
  margin-bottom: 12px;
}
.stat-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px dashed var(--line, #e6ded0);
}
.stat-caption {
  font-size: 12px;
  color: #7a7d8a;
}
.stat-delta {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

/* Panels */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 18px;
}
.panel {
  background: var(--card, #fffdf9);
  border: 1px solid var(--line, #e6ded0);
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04);
}
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.panel-title {
  font-family: var(--serif);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink, #0d1b2e);
  margin: 0 0 4px;
  letter-spacing: 0.03em;
}
.panel-sub {
  font-size: 11px;
  color: #9a917d;
}
.panel-badge {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: #b3a583;
  border: 1px solid var(--line, #e6ded0);
  border-radius: 4px;
  padding: 2px 7px;
  background: var(--paper-2, #fbf9f5);
}

/* Bars */
.bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 168px;
  padding: 4px 2px 0;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.bar-track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: linear-gradient(180deg, transparent 0%, rgba(13, 27, 46, 0.03) 100%);
  border-radius: 4px;
  position: relative;
}
.bar-fill {
  width: 62%;
  max-width: 34px;
  border-radius: 5px 5px 2px 2px;
  background: linear-gradient(180deg, #6d92c4, #3a6ea5);
  animation: bar-grow 0.6s ease both;
  animation-delay: var(--delay);
  transform-origin: bottom;
  box-shadow: inset 0 -2px 0 rgba(13, 27, 46, 0.12);
  transition: filter 0.2s;
}
.bar-fill.gold {
  background: linear-gradient(180deg, #ddc07f, #c9a45c);
}
.bar-fill.hot { filter: saturate(1.25); }
.bar-col:hover .bar-fill { filter: brightness(1.08); }
@keyframes bar-grow {
  from { transform: scaleY(0); }
  to { transform: scaleY(1); }
}
.bar-label {
  font-size: 10.5px;
  color: #8a8d99;
  font-variant-numeric: tabular-nums;
}
.empty {
  height: 168px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b3a583;
  font-size: 13px;
}

/* Ring */
.ring-wrap {
  display: flex;
  align-items: center;
  gap: 22px;
}
.ring-box {
  position: relative;
  width: 132px;
  height: 132px;
  flex-shrink: 0;
}
.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.ring-bg {
  fill: none;
  stroke: rgba(13, 27, 46, 0.06);
  stroke-width: 12;
}
.ring-seg {
  fill: none;
  stroke-width: 12;
  stroke-linecap: butt;
  animation: ring-in 0.7s ease both;
  animation-delay: var(--delay);
}
@keyframes ring-in {
  from { stroke-dasharray: 0 1000; opacity: 0; }
  to { opacity: 1; }
}
.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-total {
  font-family: var(--serif);
  font-size: 26px;
  font-weight: 700;
  color: var(--ink, #0d1b2e);
  line-height: 1;
}
.ring-total-label {
  font-size: 10px;
  color: #9a917d;
  letter-spacing: 0.1em;
  margin-top: 5px;
}
.legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.legend-item {
  display: grid;
  grid-template-columns: 10px 1fr auto auto;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 3px;
}
.legend-name {
  color: #4a4f5e;
}
.legend-val {
  color: #2c3140;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.legend-pct {
  color: #b3a583;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
