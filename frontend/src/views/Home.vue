<template>
  <div class="sw">
    <OnboardingDialog />

    <!-- ================= 公告告示带 ================= -->
    <div v-if="notices.length" class="notice-bar">
      <span class="nb-mark" aria-hidden="true"></span>
      <span class="nb-kicker">NOTICE</span>
      <div class="nb-track">
        <button
          v-for="(n, i) in notices"
          :key="n.id"
          type="button"
          class="nb-item"
          :class="{ 'nb-more': i > 0 }"
          :title="n.title"
          @click="showNotice(n)"
        >
          <span class="nb-type" :class="{ ann: n.noticeType === '2' }">{{ n.noticeType === '2' ? '公告' : '通知' }}</span>
          <span class="nb-text">{{ n.title }}</span>
        </button>
      </div>
      <span v-if="notices.length > 1" class="nb-count">{{ notices.length }} 条</span>
    </div>

    <!-- ================= 首屏：栅格即规则 ================= -->
    <section class="hero" id="top">
      <div class="grid-lines" aria-hidden="true"></div>

      <header class="nav">
        <a class="brand" @click="goPage('/home')">论文格式助手</a>
        <nav class="links" aria-label="主导航">
          <a :class="['nav-link', { on: route.path === '/home' }]" @click="goPage('/home')">首页</a>
          <a class="nav-link" @click="goPage('/features')">功能</a>
          <a class="nav-link" @click="goPage('/template-market')">模板</a>
          <a class="nav-link" @click="goPage('/guide')">教程</a>
          <a class="nav-link" @click="goPage('/cases')">案例</a>
          <a class="nav-link" @click="goPage('/pricing')">价格</a>
        </nav>
        <div class="nav-actions">
          <NotificationBell v-if="isLoggedIn" />
          <el-dropdown v-if="isLoggedIn" trigger="click" @command="handleUserCommand">
            <button class="user-chip" type="button">
              <img v-if="userAvatar" :src="userAvatar" class="avatar-img" alt="" />
              <span v-else class="avatar">{{ avatarText }}</span>
              <span class="username">{{ userName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="templates">我的工作台</el-dropdown-item>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <a v-else class="login" @click="goLogin">登录</a>
          <button class="btn-solid" type="button" @click="goStart">免费试用</button>
        </div>
      </header>

      <h1 class="hero-title">让论文排版，<br />从几个小时<span class="hl-band">变成几分钟<i class="band" aria-hidden="true"></i></span></h1>
      <p class="hero-sub">上传 Word 论文，套用校规规则，<br />引擎自动排版，一键下载</p>
      <button class="btn-solid btn-hero" type="button" @click="goStart">免费开始使用</button>

      <div class="motifs" aria-hidden="true">
        <span class="arc"></span>
        <span class="vbar"></span>
      </div>

      <ul class="mini-steps">
        <li>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v10m0 0l-4-4m4 4l4-4" /><path d="M4 16v3h16v-3" /></svg>
          <span>上传</span>
        </li>
        <li>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h8l4 4v14H6z" /><path d="M14 3v4h4" /><path d="M9 14l2 2 4-4" /></svg>
          <span>比对校规</span>
        </li>
        <li>
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3.2" /><path d="M12 3v2.4M12 18.6V21M3 12h2.4M18.6 12H21M5.6 5.6l1.7 1.7M16.7 16.7l1.7 1.7M18.4 5.6l-1.7 1.7M7.3 16.7l-1.7 1.7" /></svg>
          <span>自动排版</span>
        </li>
        <li>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v9m0 0l-3.4-3.4M12 12l3.4-3.4" /><path d="M4 15v5h16v-5" /></svg>
          <span>下载</span>
        </li>
      </ul>

      <span class="rule-red" aria-hidden="true"></span>
      <h2 class="steps-title">三步拿到合格论文</h2>

      <div class="steps">
        <article class="step">
          <span class="step-arc" aria-hidden="true"></span>
          <div class="step-body">
            <h3>上传论文</h3>
            <p>上传 Word 文档，支持 DOC、DOCX 格式，快速解析文档结构</p>
          </div>
        </article>
        <article class="step">
          <span class="step-block" aria-hidden="true"></span>
          <div class="step-body">
            <h3>自动比对校规</h3>
            <p>系统内置高校官方论文格式规则，自动比对文档，标记不符合规范的格式</p>
          </div>
        </article>
        <article class="step">
          <span class="step-bar" aria-hidden="true"></span>
          <div class="step-body">
            <h3>一键下载排版</h3>
            <p>完成排版后，一键下载符合规范的论文文档，提交无忧</p>
          </div>
        </article>
      </div>
    </section>

    <!-- ================= 能力矩阵 ================= -->
    <section class="band-sec" id="capability">
      <div class="sec-head" data-reveal>
        <h2 class="sec-title">一套规则，六件事</h2>
        <p class="sec-note">排版、表格、图、流程图——论文里要交的东西，都在同一套规则下产出。</p>
      </div>
      <ul class="cap-list">
        <li class="cap" data-reveal>
          <h3>论文智能排版</h3>
          <p>按校规统一字体、字号、行距、页边距、页码，自动生成目录与题注。</p>
          <span class="cap-tag">DOCX · 规则引擎</span>
        </li>
        <li class="cap" data-reveal>
          <h3>系统图</h3>
          <p>一个工具画完全部图表：分层架构图、流程 DSL 与泳道、Chen 记法 ER 图、三线表（可解析建表语句）。</p>
          <span class="cap-tag">架构 · 流程 · ER · 三线表</span>
        </li>
        <li class="cap" data-reveal>
          <h3>自由绘画</h3>
          <p>内嵌画布，拖拽图形与连线，悬停建图，图纸可保存与导出 PNG。</p>
          <span class="cap-tag">Canvas · PNG</span>
        </li>
        <li class="cap" data-reveal>
          <h3>校规导入向导</h3>
          <p>从学校规范文档里抽取字体、字号、行距、边距，逐项确认后套用。</p>
          <span class="cap-tag">启发式 · 可回溯</span>
        </li>
      </ul>
    </section>


    <!-- ================= 工具直达 ================= -->
    <section class="band-sec tools" id="tools">
      <div class="sec-head" data-reveal>
        <h2 class="sec-title">直接打开工具</h2>
        <p class="sec-note">图表不用另找软件：选一个工具就能画，画完按图注编号直接插进论文。</p>
      </div>
      <ul class="tool-strip" data-reveal>
        <li>
          <a class="tool" @click="goPage('/system-design')">
            <span class="tool-name">系统图</span>
            <span class="tool-desc">分层架构图 · 流程 DSL · 泳道 · ER 图 · 三线表</span>
            <span class="tool-go" aria-hidden="true">→</span>
          </a>
        </li>
        <li>
          <a class="tool" @click="goPage('/free-draw')">
            <span class="tool-name">自由绘画</span>
            <span class="tool-desc">拖拽画布 · 导出 PNG</span>
            <span class="tool-go" aria-hidden="true">→</span>
          </a>
        </li>
      </ul>
    </section>

    <!-- ================= 排版体检 ================= -->
    <section class="band-sec report" id="report">
      <div class="sec-head" data-reveal>
        <h2 class="sec-title">先体检，再交稿</h2>
        <p class="sec-note">排版结果是黑盒，所以每一步都留下可核对的证据：体检报告、逐项差异、可回退的规则来源。</p>
      </div>

      <div class="report-wrap" data-reveal>
        <table class="report-table">
          <caption>排版体检报告 · 示例数据</caption>
          <thead>
            <tr>
              <th scope="col">检查项</th>
              <th scope="col">校规要求</th>
              <th scope="col">当前文档</th>
              <th scope="col">结论</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>一级标题</td>
              <td>黑体 · 三号 · 居中</td>
              <td>黑体 · 三号 · 居中</td>
              <td><span class="ok">已套用</span></td>
            </tr>
            <tr>
              <td>正文行距</td>
              <td>固定值 22 磅</td>
              <td>固定值 22 磅</td>
              <td><span class="ok">已套用</span></td>
            </tr>
            <tr>
              <td>图表题注</td>
              <td>图注在下 · 编号 图 3-1</td>
              <td>图注在上 · 无编号</td>
              <td><span class="flag">待修正</span></td>
            </tr>
            <tr>
              <td>页边距</td>
              <td>上 2.5 cm · 左 3.0 cm</td>
              <td>上 2.5 cm · 左 3.0 cm</td>
              <td><span class="ok">已套用</span></td>
            </tr>
          </tbody>
        </table>

        <aside class="report-side">
          <p class="side-k">可疑标题 3 处</p>
          <ul class="side-list">
            <li><span>2.3.1 系统设计</span><span class="side-tag flag">层级存疑</span></li>
            <li><span>第四章</span><span class="side-tag flag">编号冲突</span></li>
            <li><span>5.1 数据库</span><span class="side-tag flag">格式不一</span></li>
          </ul>
          <p class="side-note">每一处都能点回原文定位，确认后再重排，不会静默改动你的内容。</p>
        </aside>
      </div>
    </section>

    <!-- ================= 协作与模板 ================= -->
    <section class="band-sec" id="team">
      <div class="sec-head" data-reveal>
        <h2 class="sec-title">方案可以复用，也可以共享</h2>
        <p class="sec-note">一个人的校规配置，能成为整个课题组的标准：模板上架、修订提醒、参数两两对比。</p>
      </div>
      <ul class="row-list">
        <li data-reveal>
          <span class="row-name">我的格式方案</span>
          <span class="row-desc">保存多套校规配置，导入导出 JSON，随时切换学校版本</span>
        </li>
        <li data-reveal>
          <span class="row-name">团队空间</span>
          <span class="row-desc">模板、任务、论文归属团队，成员共享可见，权限越界有校验</span>
        </li>
        <li data-reveal>
          <span class="row-name">模板市场</span>
          <span class="row-desc">按学校与学科筛选，下载量、评分、修订记录公开可见</span>
        </li>
        <li data-reveal>
          <span class="row-name">任务队列</span>
          <span class="row-desc">并发上限、超大文档保护、失败重试，进度实时推送</span>
        </li>
      </ul>
    </section>

    <!-- ================= 开始使用 ================= -->
    <section class="cta-band" id="start" data-reveal>
      <h2>把你的论文交给规则</h2>
      <p>上传 Word，套用校规，几分钟后拿到符合规范的成稿。</p>
      <button class="cta-btn" type="button" @click="goStart">免费开始使用</button>
    </section>

    <!-- ================= 页脚 ================= -->
    <footer class="foot">
      <div class="foot-top">
        <span class="foot-brand">论文格式助手</span>
        <nav class="foot-links" aria-label="页脚导航">
          <a @click="goPage('/features')">功能</a>
          <a @click="goPage('/template-market')">模板</a>
          <a @click="goPage('/guide')">教程</a>
          <a @click="goPage('/cases')">案例</a>
          <a @click="goPage('/pricing')">价格</a>
          <a @click="goPage('/about')">关于</a>
          <a @click="goPage('/terms')">服务条款</a>
          <a @click="goPage('/privacy')">隐私政策</a>
        </nav>
      </div>
      <p class="foot-copy">© {{ new Date().getFullYear() }} 论文格式助手 · 为毕业论文排版而生</p>
    </footer>

    <!-- 公告详情 -->
    <el-dialog
      v-model="noticeVisible"
      :title="currentNotice ? currentNotice.title : ''"
      width="560px"
      destroy-on-close
      class="nb-dialog"
    >
      <div v-if="currentNotice" class="nb-detail">
        <div class="nb-detail-meta">
          <span class="nb-type" :class="{ ann: currentNotice.noticeType === '2' }">{{ currentNotice.noticeType === '2' ? '公告' : '通知' }}</span>
          <span class="nb-detail-time">{{ formatNoticeTime(currentNotice.createTime) }}</span>
        </div>
        <p class="nb-detail-body">{{ currentNotice.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'
import NotificationBell from '../components/NotificationBell.vue'
import OnboardingDialog from '../components/OnboardingDialog.vue'
import { clearAuth } from '../utils/perm'
import { listPublicNotices } from '../api/admin'

const router = useRouter()
const route = useRoute()

const isLoggedIn = ref(false)
const isAdmin = ref(false)
const userName = ref('')
const userAvatar = ref('')
const avatarText = computed(() => (userName.value || 'U').slice(0, 1).toUpperCase())

/* ---------------- 公告告示带 ---------------- */
const notices = ref([])
const noticeVisible = ref(false)
const currentNotice = ref(null)

function showNotice(n) {
  currentNotice.value = n
  noticeVisible.value = true
}
function formatNoticeTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}
async function loadNotices() {
  try {
    const r = await listPublicNotices(3)
    notices.value = Array.isArray(r) ? r : []
  } catch (e) {
    notices.value = []
  }
}

const prefersReduced = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
let io = null

function initReveal() {
  if (prefersReduced) {
    document.querySelectorAll('[data-reveal]').forEach(el => el.classList.add('in'))
    return
  }
  io = new IntersectionObserver(
    entries => {
      entries.forEach(en => {
        if (!en.isIntersecting) return
        en.target.classList.add('in')
        io.unobserve(en.target)
      })
    },
    { rootMargin: '0px 0px -12% 0px', threshold: 0.08 }
  )
  document.querySelectorAll('[data-reveal]').forEach(el => io.observe(el))
}

onMounted(async () => {
  initReveal()
  loadNotices()
  isLoggedIn.value = !!localStorage.getItem('token')
  isAdmin.value = localStorage.getItem('role') === 'ADMIN'
  if (!isAdmin.value) {
    try {
      const menus = JSON.parse(localStorage.getItem('menus') || '[]')
      if (menus.length > 0) isAdmin.value = true
    } catch (e) {}
  }
  userName.value = localStorage.getItem('username') || '用户'
  userAvatar.value = localStorage.getItem('avatar') || ''
  if (isLoggedIn.value) {
    try {
      const p = await getProfile()
      if (p) {
        userName.value = p.nickname || p.username || userName.value
        userAvatar.value = p.avatar || ''
        localStorage.setItem('username', userName.value)
        localStorage.setItem('avatar', userAvatar.value)
      }
    } catch (e) {
      isLoggedIn.value = false
      isAdmin.value = false
      userName.value = ''
      userAvatar.value = ''
      ;['token', 'userId', 'username', 'avatar', 'role', 'roles', 'perms', 'menus'].forEach(k =>
        localStorage.removeItem(k)
      )
    }
  }
})

onBeforeUnmount(() => {
  if (io) io.disconnect()
})

function goLogin() {
  router.push('/login')
}
function goPage(path) {
  router.push(path)
}
function goStart() {
  if (localStorage.getItem('token')) router.push('/templates')
  else router.push('/login')
}
async function handleUserCommand(cmd) {
  if (cmd === 'templates') {
    router.push('/templates')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'admin') {
    router.push('/admin/dashboard')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      try {
        await logout()
      } catch (e) {}
      clearAuth()
      isLoggedIn.value = false
      isAdmin.value = false
      ElMessage.success('已退出登录')
      router.push('/home')
    } catch (e) {}
  }
}
</script>

<style scoped>
/* ============================================================
   瑞士海报墙 · 论文格式助手
   纸白地面 / 客观黑 / 单一信号红 / 外露模数栅格 / 硬边无圆角
   一切尺寸以构图 1536×1024 的像素为单位，按视口等比缩放
   ============================================================ */
.sw {
  --u: calc(min(100vw, 1536px) / 1536);
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --ink-3: #6b6b6b;
  --red: #dd2627;
  --hair: #c1c1c3;
  --hair-soft: #e4e4e4;
  --sans: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  background: var(--paper);
  color: var(--ink);
  font-family: var(--sans);
  -webkit-font-smoothing: antialiased;
}

/* ---------------- 公告告示带 ---------------- */
.notice-bar {
  position: relative;
  z-index: 5;
  width: 100%;
  max-width: 1536px;
  margin: 0 auto;
  min-height: calc(46 * var(--u));
  display: flex;
  align-items: center;
  gap: calc(13 * var(--u));
  padding: calc(7 * var(--u)) calc(48 * var(--u));
  box-sizing: border-box;
  background: var(--paper);
  border-bottom: calc(1 * var(--u)) solid var(--hair);
}
.nb-mark {
  width: calc(9 * var(--u));
  height: calc(9 * var(--u));
  background: var(--red);
  flex-shrink: 0;
}
.nb-kicker {
  font-size: calc(11 * var(--u));
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--ink-3);
  flex-shrink: 0;
}
.nb-track {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: calc(26 * var(--u));
  overflow: hidden;
}
.nb-item {
  display: flex;
  align-items: baseline;
  gap: calc(9 * var(--u));
  min-width: 0;
  max-width: calc(430 * var(--u));
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.nb-type {
  flex-shrink: 0;
  font-size: calc(11 * var(--u));
  font-weight: 700;
  line-height: 1.6;
  padding: 0 calc(6 * var(--u));
  color: var(--ink-3);
  border: calc(1 * var(--u)) solid var(--hair);
}
.nb-type.ann {
  color: var(--red);
  border-color: var(--red);
}
.nb-text {
  font-size: calc(14 * var(--u));
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-bottom: calc(2 * var(--u));
  border-bottom: calc(2 * var(--u)) solid transparent;
}
.nb-item:hover .nb-text {
  color: var(--red);
  border-bottom-color: var(--red);
}
.nb-count {
  flex-shrink: 0;
  font-size: calc(12 * var(--u));
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

/* 公告详情弹窗 */
.nb-detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--hair-soft);
}
.nb-detail-time {
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.nb-detail-body {
  margin: 0;
  font-size: 14px;
  line-height: 1.85;
  color: var(--ink-2);
  white-space: pre-wrap;
}
.nb-dialog :deep(.el-dialog__header) {
  border-top: 3px solid var(--red);
  padding-bottom: 12px;
  margin-right: 0;
}
.nb-dialog :deep(.el-dialog__title) {
  font-size: 17px;
  font-weight: 900;
  color: var(--ink);
}

.hero {
  position: relative;
  width: 100%;
  max-width: 1536px;
  margin: 0 auto;
  aspect-ratio: 1536 / 1024;
  overflow: hidden;
}

/* 外露模数栅格：十列发丝线 */
.grid-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: repeating-linear-gradient(
    to right,
    var(--hair-soft) 0,
    var(--hair-soft) calc(1 * var(--u)),
    transparent calc(1 * var(--u)),
    transparent calc(153.6 * var(--u))
  );
}

/* ---------------- 导航 ----------------
   规格与组件 SiteHeader 严格对齐：72 高 / 品牌 20·900 / 链接 14·间距28 /
   品牌到链接 20 / 右侧间距 18。首页导航是海报构成的一部分（绝对定位在 hero 内，
   红圆弧挂在它下沿），因此不共用组件，只共用数值——改这里请同步 SiteHeader。 */
.nav {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: calc(72 * var(--u));
  display: flex;
  align-items: center;
  padding: 0 calc(48 * var(--u));
  box-sizing: border-box;
  border-bottom: calc(1 * var(--u)) solid var(--hair);
  gap: calc(28 * var(--u));
  background: var(--paper);
  z-index: 3;
}
.brand {
  font-size: calc(20 * var(--u));
  font-weight: 900;
  letter-spacing: -0.01em;
  cursor: pointer;
  white-space: nowrap;
}
.links {
  display: flex;
  align-items: center;
  gap: calc(28 * var(--u));
  margin-left: calc(20 * var(--u));
}
.nav-link {
  font-size: calc(14 * var(--u));
  color: var(--ink);
  cursor: pointer;
  padding-bottom: calc(3 * var(--u));
  border-bottom: calc(2 * var(--u)) solid transparent;
  white-space: nowrap;
}
.nav-link:hover,
.nav-link.on {
  border-bottom-color: var(--red);
}
.nav-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: calc(18 * var(--u));
}
.login {
  font-size: calc(14 * var(--u));
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
}
.login:hover {
  color: var(--red);
}
.user-chip {
  display: flex;
  align-items: center;
  gap: calc(8 * var(--u));
  background: none;
  border: calc(1 * var(--u)) solid var(--hair);
  padding: calc(4 * var(--u)) calc(10 * var(--u)) calc(4 * var(--u)) calc(4 * var(--u));
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s;
}
.user-chip:hover {
  border-color: var(--ink);
}
.avatar,
.avatar-img {
  width: calc(26 * var(--u));
  height: calc(26 * var(--u));
  display: grid;
  place-items: center;
  background: var(--ink);
  color: var(--paper);
  font-size: calc(13 * var(--u));
  font-weight: 700;
  object-fit: cover;
  flex-shrink: 0;
}
.username {
  font-size: calc(13 * var(--u));
  color: var(--ink);
  max-width: calc(120 * var(--u));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 导航内的 CTA 与其他页 header 同尺寸（下方 hero 大按钮不受影响） */
.nav-actions .btn-solid {
  height: calc(38 * var(--u));
  font-size: calc(14 * var(--u));
  padding: 0 calc(18 * var(--u));
}

/* ---------------- 按钮 ---------------- */
.btn-solid {
  background: var(--red);
  color: #fff;
  border: 0;
  font-family: inherit;
  font-weight: 700;
  font-size: calc(15 * var(--u));
  height: calc(44 * var(--u));
  padding: 0 calc(24 * var(--u));
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s ease;
}
.btn-solid:hover {
  background: #b91d1e;
}

/* ---------------- 标题 ---------------- */
.hero-title {
  position: absolute;
  left: calc(38 * var(--u));
  top: calc(108 * var(--u));
  width: calc(966 * var(--u));
  margin: 0;
  font-size: calc(96 * var(--u));
  line-height: 0.98;
  font-weight: 900;
  letter-spacing: -0.005em;
  z-index: 2;
}
/* 红色高亮带：压在「变成几分钟」字脚下，文字仍为墨黑 */
.hl-band {
  position: relative;
  display: inline-block;
  z-index: 0;
}
.band {
  position: absolute;
  left: calc(-4 * var(--u));
  right: calc(-4 * var(--u));
  bottom: calc(7 * var(--u));
  height: calc(22 * var(--u));
  background: var(--red);
  z-index: -1;
  pointer-events: none;
}
.hero-sub {
  position: absolute;
  left: calc(28 * var(--u));
  top: calc(348 * var(--u));
  width: calc(430 * var(--u));
  margin: 0;
  font-size: calc(28 * var(--u));
  line-height: 1.3;
  color: var(--ink);
}
.btn-hero {
  position: absolute;
  left: calc(550 * var(--u));
  top: calc(342 * var(--u));
  height: calc(62 * var(--u));
  font-size: calc(21 * var(--u));
  padding: 0 calc(30 * var(--u));
}

/* ---------------- 海报几何 ---------------- */
.motifs {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.arc {
  position: absolute;
  left: calc(830 * var(--u));
  top: calc(78 * var(--u));
  width: calc(360 * var(--u));
  height: calc(360 * var(--u));
  border: calc(2 * var(--u)) solid var(--red);
  border-radius: 50%;
  border-left-color: transparent;
  transform: rotate(-12deg);
}
.vbar {
  position: absolute;
  left: calc(1298 * var(--u));
  top: calc(168 * var(--u));
  width: calc(30 * var(--u));
  height: calc(178 * var(--u));
  background: var(--red);
}

/* ---------------- 2×2 小模块 ---------------- */
.mini-steps {
  position: absolute;
  left: calc(38 * var(--u));
  top: calc(427 * var(--u));
  width: calc(489 * var(--u));
  height: calc(240 * var(--u));
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: calc(14 * var(--u));
}
.mini-steps li {
  border: calc(1 * var(--u)) solid var(--ink);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: calc(14 * var(--u));
  padding: 0;
  font-size: calc(19 * var(--u));
  font-weight: 500;
  text-align: center;
}
.mini-steps svg {
  width: calc(44 * var(--u));
  height: calc(44 * var(--u));
  fill: none;
  stroke: var(--ink);
  stroke-width: 1.5;
  stroke-linecap: square;
  flex: none;
}

/* ---------------- 工作台 ---------------- */
.workbench {
  position: absolute;
  left: calc(828 * var(--u));
  top: calc(238 * var(--u));
  width: calc(426 * var(--u));
  height: calc(453 * var(--u));
  border: calc(1 * var(--u)) solid var(--ink);
  background: var(--paper);
  display: flex;
  z-index: 2;
}
.wb-rail {
  width: calc(120 * var(--u));
  border-right: calc(1 * var(--u)) solid var(--hair);
  display: flex;
  flex-direction: column;
  padding-top: calc(14 * var(--u));
  box-sizing: border-box;
}
.wb-tab {
  font-size: calc(13.5 * var(--u));
  font-weight: 500;
  padding: calc(13 * var(--u)) calc(16 * var(--u));
  color: var(--ink-2);
  border-left: calc(3 * var(--u)) solid transparent;
}
.wb-tab.on {
  color: var(--red);
  border-left-color: var(--red);
  font-weight: 700;
}
.wb-panel {
  flex: 1;
  padding: calc(16 * var(--u));
  min-width: 0;
  box-sizing: border-box;
}
.wb-head {
  font-size: calc(17 * var(--u));
  font-weight: 700;
  padding-bottom: calc(10 * var(--u));
}
.wb-card {
  border: calc(1 * var(--u)) solid var(--hair);
  padding: calc(14 * var(--u));
  height: calc(100% - 46 * var(--u));
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.wb-card-title {
  font-size: calc(15 * var(--u));
  font-weight: 700;
  margin-bottom: calc(8 * var(--u));
}
.wb-line {
  margin: 0 0 calc(12 * var(--u));
  font-size: calc(11.5 * var(--u));
  line-height: 1.6;
  color: var(--ink-3);
}
.wb-rows {
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: calc(1 * var(--u)) solid var(--hair-soft);
}
.wb-rows li {
  display: flex;
  justify-content: space-between;
  gap: calc(8 * var(--u));
  padding: calc(7 * var(--u)) 0;
  border-bottom: calc(1 * var(--u)) solid var(--hair-soft);
  font-size: calc(12 * var(--u));
  color: var(--ink-2);
}
.wb-check {
  color: var(--ink);
  font-weight: 700;
}
.wb-flag {
  color: var(--red);
  font-weight: 700;
}
.wb-foot {
  margin-top: auto;
  text-align: center;
  font-size: calc(11 * var(--u));
  color: var(--ink-3);
}

/* ---------------- 三步 ---------------- */
.rule-red {
  position: absolute;
  left: calc(11 * var(--u));
  right: calc(11 * var(--u));
  top: calc(690 * var(--u));
  height: calc(1 * var(--u));
  background: var(--red);
}
.steps-title {
  position: absolute;
  left: calc(48 * var(--u));
  top: calc(716 * var(--u));
  margin: 0;
  font-size: calc(50 * var(--u));
  font-weight: 900;
  letter-spacing: 0.03em;
  line-height: 1.2;
}
.steps {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.step {
  position: absolute;
  top: calc(809 * var(--u)) !important;
  height: 200px !important;
  border: calc(1 * var(--u)) solid var(--ink);
  padding: calc(18 * var(--u)) calc(20 * var(--u)) calc(18 * var(--u)) calc(112 * var(--u));
  display: flex;
  align-items: center;
  overflow: hidden;
  box-sizing: border-box;
  background: var(--paper);
  pointer-events: auto;
}
.step:nth-of-type(1) {
  left: calc(11 * var(--u));
  width: calc(499 * var(--u));
}
.step:nth-of-type(2) {
  left: calc(542 * var(--u));
  width: calc(530 * var(--u));
}
.step:nth-of-type(3) {
  left: calc(1072 * var(--u));
  width: calc(453 * var(--u));
}
.step::before {
  content: '';
  position: absolute;
  left: calc(170 * var(--u));
  top: 0;
  bottom: 0;
  width: calc(1 * var(--u));
  background: var(--hair);
}
.step-body {
  position: relative;
}
.step-body h3 {
  margin: 0 0 calc(10 * var(--u));
  font-size: calc(30 * var(--u));
  font-weight: 700;
  letter-spacing: -0.01em;
}
.step-body p {
  margin: 0;
  font-size: calc(20 * var(--u));
  line-height: 1.6;
  color: var(--ink);
  max-width: calc(300 * var(--u));
}
.step-arc,
.step-block,
.step-bar {
  position: absolute;
  background: var(--red);
}
.step-arc {
  left: calc(24 * var(--u));
  top: calc(4 * var(--u));
  width: calc(140 * var(--u));
  height: calc(192 * var(--u));
  background: none;
  border: calc(2 * var(--u)) solid var(--red);
  border-radius: 50%;
  border-right-color: transparent;
  border-top-color: transparent;
}
.step-block {
  left: calc(20 * var(--u));
  top: calc(70 * var(--u));
  width: calc(70 * var(--u));
  height: calc(44 * var(--u));
}
.step-bar {
  left: calc(20 * var(--u));
  top: calc(56 * var(--u));
  width: calc(26 * var(--u));
  height: calc(86 * var(--u));
}

/* ============================================================
   首屏之后：同一套栅格与排印继续统治整页
   ============================================================ */
.band-sec {
  max-width: 1536px;
  margin: 0 auto;
  padding: calc(88 * var(--u)) calc(48 * var(--u));
  border-top: calc(1 * var(--u)) solid var(--hair);
}
.sec-head {
  display: grid;
  grid-template-columns: 1fr;
  margin-bottom: calc(48 * var(--u));
}

.sec-title {
  margin: 0 0 calc(16 * var(--u));
  font-size: calc(46 * var(--u));
  line-height: 1.12;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.sec-note {
  margin: 0;
  max-width: calc(560 * var(--u));
  font-size: calc(18 * var(--u));
  line-height: 1.65;
  color: var(--ink-2);
}

/* 能力矩阵：用横线与列宽组织，不做等大卡片 */
.cap-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: calc(1 * var(--u)) solid var(--ink);
}
.cap {
  display: grid;
  grid-template-columns: calc(300 * var(--u)) 1fr calc(200 * var(--u));
  align-items: baseline;
  column-gap: calc(32 * var(--u));
  padding: calc(26 * var(--u)) 0;
  border-bottom: calc(1 * var(--u)) solid var(--hair);
}
.cap h3 {
  margin: 0;
  font-size: calc(26 * var(--u));
  font-weight: 700;
  letter-spacing: -0.01em;
}
.cap p {
  margin: 0;
  font-size: calc(16 * var(--u));
  line-height: 1.65;
  color: var(--ink-2);
  max-width: calc(660 * var(--u));
}
.cap-tag {
  justify-self: end;
  font-size: calc(12.5 * var(--u));
  letter-spacing: 0.08em;
  color: var(--ink-3);
  white-space: nowrap;
}


/* 工具直达：四格横条，竖发丝线分栏，红箭头指向入口 */
.tool-strip {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-top: calc(1 * var(--u)) solid var(--ink);
  border-bottom: calc(1 * var(--u)) solid var(--ink);
}
.tool-strip li {
  border-left: calc(1 * var(--u)) solid var(--hair);
}
.tool-strip li:first-child {
  border-left: 0;
}
.tool {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: calc(10 * var(--u));
  min-height: calc(150 * var(--u));
  padding: calc(24 * var(--u)) calc(44 * var(--u)) calc(22 * var(--u)) calc(24 * var(--u));
  cursor: pointer;
  transition: background 0.18s ease;
}
.tool:hover {
  background: #fff;
}
.tool-name {
  font-size: calc(26 * var(--u));
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
}
.tool-desc {
  font-size: calc(14 * var(--u));
  line-height: 1.6;
  color: var(--ink-2);
}
.tool-go {
  position: absolute;
  right: calc(22 * var(--u));
  top: calc(24 * var(--u));
  font-size: calc(20 * var(--u));
  color: var(--red);
  line-height: 1;
  transition: transform 0.18s ease;
}
.tool:hover .tool-go {
  transform: translateX(calc(4 * var(--u)));
}

/* 体检报告 */
.report-wrap {
  display: grid;
  grid-template-columns: 1fr calc(360 * var(--u));
  gap: calc(32 * var(--u));
  align-items: start;
}
.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: calc(17 * var(--u));
}
.report-table caption {
  text-align: left;
  font-size: calc(13 * var(--u));
  letter-spacing: 0.08em;
  color: var(--ink-3);
  padding-bottom: calc(12 * var(--u));
}
.report-table th,
.report-table td {
  text-align: left;
  padding: calc(16 * var(--u)) calc(14 * var(--u));
  border-bottom: calc(1 * var(--u)) solid var(--hair);
  font-weight: 400;
  color: var(--ink-2);
}
.report-table thead th {
  font-size: calc(13 * var(--u));
  letter-spacing: 0.08em;
  color: var(--ink-3);
  border-bottom: calc(1 * var(--u)) solid var(--ink);
}
.report-table tbody td:first-child {
  color: var(--ink);
  font-weight: 700;
}
.report-table tbody tr:first-child td {
  border-top: calc(2 * var(--u)) solid var(--ink);
}
.ok {
  color: var(--ink);
  font-weight: 700;
}
.flag {
  color: var(--red);
  font-weight: 700;
}
.report-side {
  border: calc(1 * var(--u)) solid var(--ink);
  padding: calc(24 * var(--u));
}
.side-k {
  margin: 0 0 calc(14 * var(--u));
  font-size: calc(14 * var(--u));
  letter-spacing: 0.08em;
  color: var(--red);
  font-weight: 700;
}
.side-list {
  list-style: none;
  margin: 0 0 calc(18 * var(--u));
  padding: 0;
}
.side-list li {
  display: flex;
  justify-content: space-between;
  gap: calc(12 * var(--u));
  padding: calc(12 * var(--u)) 0;
  border-bottom: calc(1 * var(--u)) solid var(--hair-soft);
  font-size: calc(16 * var(--u));
  color: var(--ink);
}
.side-tag {
  font-size: calc(13 * var(--u));
  white-space: nowrap;
}
.side-note {
  margin: 0;
  font-size: calc(14 * var(--u));
  line-height: 1.7;
  color: var(--ink-3);
}

/* 行列表 */
.row-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: calc(1 * var(--u)) solid var(--ink);
}
.row-list li {
  display: grid;
  grid-template-columns: calc(280 * var(--u)) 1fr;
  align-items: baseline;
  gap: calc(24 * var(--u));
  padding: calc(24 * var(--u)) 0;
  border-bottom: calc(1 * var(--u)) solid var(--hair);
}

.row-name {
  font-size: calc(24 * var(--u));
  font-weight: 700;
}
.row-desc {
  font-size: calc(16 * var(--u));
  line-height: 1.7;
  color: var(--ink-2);
}

/* 开始使用 */
.cta-band {
  max-width: 1536px;
  margin: 0 auto;
  padding: calc(96 * var(--u)) calc(48 * var(--u));
  background: var(--red);
  color: #fff;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  column-gap: calc(40 * var(--u));
}
.cta-band h2 {
  grid-column: 1;
  margin: 0 0 calc(14 * var(--u));
  font-size: calc(58 * var(--u));
  font-weight: 900;
  letter-spacing: -0.015em;
  line-height: 1.08;
}
.cta-band p {
  grid-column: 1;
  margin: 0;
  font-size: calc(18 * var(--u));
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.86);
}
.cta-btn {
  grid-column: 2;
  grid-row: 1 / span 2;
  align-self: center;
  background: #fff;
  color: var(--ink);
  border: 0;
  font-family: inherit;
  font-weight: 700;
  font-size: calc(18 * var(--u));
  height: calc(58 * var(--u));
  padding: 0 calc(34 * var(--u));
  cursor: pointer;
  white-space: nowrap;
}
.cta-btn:hover {
  background: var(--ink);
  color: #fff;
}

/* 页脚 */
.foot {
  max-width: 1536px;
  margin: 0 auto;
  padding: calc(56 * var(--u)) calc(48 * var(--u)) calc(64 * var(--u));
  border-top: calc(1 * var(--u)) solid var(--ink);
}
.foot-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: calc(32 * var(--u));
  margin-bottom: calc(40 * var(--u));
}
.foot-brand {
  font-size: calc(22 * var(--u));
  font-weight: 900;
}
.foot-links {
  display: flex;
  flex-wrap: wrap;
  gap: calc(28 * var(--u));
}
.foot-links a {
  font-size: calc(15 * var(--u));
  color: var(--ink-2);
  cursor: pointer;
}
.foot-links a:hover {
  color: var(--red);
}
.foot-copy {
  margin: 0;
  font-size: calc(13 * var(--u));
  color: var(--ink-3);
  letter-spacing: 0.04em;
}


/* 浏览器自己的表面也属于这套系统 */
.sw ::selection {
  background: var(--red);
  color: #fff;
}
.sw a:focus-visible,
.sw button:focus-visible,
.sw [tabindex]:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: calc(3 * var(--u));
}
.sw :focus:not(:focus-visible) {
  outline: none;
}
* {
  scrollbar-color: var(--hair) var(--paper);
  scrollbar-width: thin;
}

/* ============================================================
   动效：海报的入场是「刷上去的一条红」，不是位移特效
   ============================================================ */
.hero-title,
.hero-sub,
.btn-hero,
.mini-steps,
.motifs .arc,
.motifs .vbar,
.steps-title,
.step,
.nav {
  animation: rise 0.72s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
.hero-title { animation-delay: 0.04s; }
.hero-sub { animation-delay: 0.16s; }
.btn-hero { animation-delay: 0.2s; }
.mini-steps { animation-delay: 0.26s; }
.motifs .arc { animation-delay: 0.1s; }
.motifs .vbar { animation-delay: 0.14s; }
.steps-title { animation-delay: 0.3s; }
.step:nth-of-type(1) { animation-delay: 0.34s; }
.step:nth-of-type(2) { animation-delay: 0.38s; }
.step:nth-of-type(3) { animation-delay: 0.42s; }
.nav { animation-delay: 0s; }

.band {
  transform-origin: left center;
  animation: brush 0.55s cubic-bezier(0.65, 0, 0.35, 1) 0.34s both;
}

[data-reveal] {
  opacity: 0;
  transform: translateY(calc(14 * var(--u)));
  transition: opacity 0.6s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1);
}
[data-reveal].in {
  opacity: 1;
  transform: none;
}
.cap:nth-child(2),
.row-list li:nth-child(2) { transition-delay: 0.06s; }
.cap:nth-child(3),
.row-list li:nth-child(3) { transition-delay: 0.12s; }
.cap:nth-child(4),
.row-list li:nth-child(4) { transition-delay: 0.18s; }
.cap:nth-child(5) { transition-delay: 0.08s; }
.cap:nth-child(6) { transition-delay: 0.14s; }

@keyframes rise {
  from { opacity: 0; transform: translateY(calc(18 * var(--u))); }
  to { opacity: 1; transform: none; }
}
@keyframes brush {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}
@media (prefers-reduced-motion: reduce) {
  .hero-title, .hero-sub, .btn-hero, .mini-steps, .motifs .arc, .motifs .vbar,
  .steps-title, .step, .nav, .band { animation: none !important; }
  [data-reveal] { opacity: 1 !important; transform: none !important; transition: none !important; }
}

/* ============================================================
   窄屏：海报折成单列，栅格仍在
   ============================================================ */
@media (max-width: 900px) {
  .sw { --u: 1px; }
  .notice-bar {
    padding: 8px 18px;
    gap: 8px;
  }
  .nb-more,
  .nb-count { display: none; }
  .nb-item { max-width: none; }
  .nb-text { font-size: 13px; }
  .hero {
    aspect-ratio: auto;
    padding: 18px 18px 40px;
    display: flex;
    flex-direction: column;
    gap: 18px;
  }
  .grid-lines { display: none; }
  /* 移动端导航与 SiteHeader 同规则：品牌行 + 链接条第二行（可横滑） */
  .nav {
    position: static;
    height: auto;
    padding: 12px 18px;
    flex-wrap: wrap;
    gap: 12px;
    border-bottom: 1px solid var(--hair);
    animation: none;
  }
  .links {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 18px;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .links::-webkit-scrollbar { display: none; }
  .brand { font-size: 17px; }
  .nav-actions { gap: 10px; }
  .btn-solid { height: 40px; font-size: 14px; padding: 0 16px; }
  .nav-actions .btn-solid { height: 34px; font-size: 13px; padding: 0 12px; }
  .username { display: none; }
  .hero-title {
    position: static;
    width: auto;
    font-size: 33px;
    line-height: 1.18;
    letter-spacing: -0.01em;
    order: 1;
  }
  .hl-band { display: inline; }
  .band { display: none; }
  .hero-sub {
    position: static;
    width: auto;
    font-size: 15px;
    line-height: 1.55;
    order: 2;
  }
  .btn-hero {
    position: static;
    align-self: flex-start;
    height: 46px;
    font-size: 15px;
    padding: 0 20px;
    order: 3;
  }
  .motifs { display: none; }
  .mini-steps {
    position: static;
    width: auto;
    height: auto;
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto auto;
    gap: 10px;
    order: 4;
  }
  .mini-steps li { padding: 16px 10px; gap: 8px; }
  .mini-steps svg { width: 28px; height: 28px; }
  .mini-steps li { font-size: 13px; }
  .rule-red { display: none; }
  .steps-title {
    position: static;
    font-size: 27px;
    margin: 12px 0 0;
    letter-spacing: 0.02em;
    order: 5;
  }
  .steps { position: static; order: 6; }
  .step {
    position: relative;
    left: auto !important;
    width: auto !important;
    height: auto !important;
    top: auto !important;
    bottom: auto !important;
    padding: 18px 16px 18px 96px;
    margin-bottom: 10px;
    animation: none;
  }
  .step::before { left: 74px; }
  .step-body h3 { font-size: 19px; }
  .step-body p { font-size: 13.5px; max-width: none; }
  .step-arc { left: 12px; top: 8px; width: 52px; height: 84px; }
  .step-block { left: 14px; top: 34px; width: 36px; height: 26px; }
  .step-bar { left: 14px; top: 26px; width: 14px; height: 46px; }

  .band-sec { padding: 44px 18px; }
  .sec-head { margin-bottom: 26px; }
  .sec-title { font-size: 27px; margin-bottom: 10px; }
  .sec-note { font-size: 14.5px; }

  .cap {
    grid-template-columns: 1fr;
    row-gap: 6px;
    padding: 18px 0;
  }
  .cap h3 { font-size: 19px; }
  .cap p { font-size: 14px; max-width: none; }
  .cap-tag { justify-self: start; }

  .tool-strip { grid-template-columns: 1fr 1fr; }
  .tool-strip li:nth-child(3) { border-left: 0; }
  .tool-strip li:nth-child(n + 3) { border-top: 1px solid var(--hair); }
  .tool { min-height: 0; padding: 16px 14px; }
  .tool-name { font-size: 18px; }
  .tool-desc { font-size: 12.5px; }

  .report-wrap { grid-template-columns: 1fr; gap: 20px; }
  .report-table { font-size: 13px; display: block; overflow-x: auto; }
  .report-table th, .report-table td { padding: 11px 8px; white-space: nowrap; }
  .report-side { padding: 18px; }
  .side-list li { font-size: 14px; }

  .row-list li {
    grid-template-columns: 1fr;
    row-gap: 6px;
    padding: 18px 0;
  }
  .row-desc { font-size: 13.5px; }
  .row-name { font-size: 18px; }

  .cta-band {
    grid-template-columns: 1fr;
    padding: 48px 18px;
    row-gap: 18px;
  }
  .cta-band h2 { font-size: 32px; }
  .cta-band p { font-size: 14.5px; }
  .cta-btn { grid-column: 1; grid-row: auto; height: 50px; font-size: 15px; justify-self: start; }

  .foot { padding: 36px 18px 48px; }
  .foot-top { flex-direction: column; gap: 20px; margin-bottom: 26px; }
  .foot-links { gap: 16px 20px; }
  .foot-copy { font-size: 12px; }
}

</style>
