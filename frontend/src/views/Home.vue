<template>
  <div class="landing">
    <!-- ========== Navbar ========== -->
    <nav class="navbar">
      <div class="nav-inner">
        <div class="nav-brand">
          <div class="logo-w">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h4l3 12 3-12h4"/></svg>
          </div>
          <span class="brand-text">Word 排版助手</span>
          <span class="ai-badge">AI</span>
          <span class="brand-sub">文档工具箱</span>
        </div>
        <div class="nav-links">
          <a class="nav-link active" @click="goPage('/home')">首页</a>
          <a class="nav-link" @click="goPage('/features')">功能</a>
          <a class="nav-link" @click="goPage('/template-market')">模板</a>
          <a class="nav-link" @click="goPage('/guide')">使用教程</a>
          <a class="nav-link" @click="goPage('/cases')">案例</a>
          <a class="nav-link" @click="goPage('/pricing')">价格</a>
        </div>
        <div class="nav-actions">
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-chip">
                <img v-if="userAvatar" :src="userAvatar" class="avatar-img" alt="" />
                <span v-else class="avatar">{{ avatarText }}</span>
                <span class="username">{{ userName }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="templates">我的工作台</el-dropdown-item>
                  <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <button class="btn-ghost" @click="goLogin">登录</button>
          </template>
          <button class="btn-primary" @click="goStart">免费开始使用</button>
        </div>
      </div>
    </nav>

    <!-- ========== Hero ========== -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-blob blob-1"></div>
        <div class="hero-blob blob-2"></div>
      </div>
      <div class="hero-inner">
        <div class="hero-left">
          <div class="hero-badge">
            <svg viewBox="0 0 16 16" width="14" height="14" fill="none"><path d="M8 1l1.5 4.5L14 7l-4.5 1.5L8 13l-1.5-4.5L2 7l4.5-1.5z" fill="#3B6BFF"/></svg>
            专为论文排版设计，让复杂排版变得简单
          </div>
          <h1 class="hero-title">
            让论文排版，<br/>
            从几个小时<br/>
            <span class="highlight">变成几分钟</span>
          </h1>
          <p class="hero-desc">
            支持 Word 智能排版、三线表生成、ER 图绘制、系统图设计等，<br/>
            一站式解决论文排版难题，符合学校规范要求。
          </p>
          <div class="hero-cta">
            <button class="btn-cta-primary" @click="goStart">
              开始智能排版
              <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 8h10M9 4l4 4-4 4"/></svg>
            </button>
            <button class="btn-cta-ghost">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor"><polygon points="6,3 13,8 6,13"/></svg>
              查看演示视频
            </button>
          </div>
          <div class="hero-features">
            <span class="feat-tag">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#3B6BFF" stroke-width="1.8" stroke-linecap="round"><path d="M5 8l2 2 4-4"/></svg>
              无需安装
            </span>
            <span class="feat-tag">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#3B6BFF" stroke-width="1.8" stroke-linecap="round"><path d="M5 8l2 2 4-4"/></svg>
              在线使用
            </span>
            <span class="feat-tag">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#3B6BFF" stroke-width="1.8" stroke-linecap="round"><path d="M5 8l2 2 4-4"/></svg>
              数据安全
            </span>
            <span class="feat-tag">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#3B6BFF" stroke-width="1.8" stroke-linecap="round"><path d="M5 8l2 2 4-4"/></svg>
              多格式导出
            </span>
          </div>
        </div>

        <!-- Mock UI Panel -->
        <div class="hero-right">
          <div class="mock-panel">
            <div class="mock-titlebar">
              <div class="mock-dots">
                <span class="dot dot-red"></span>
                <span class="dot dot-yellow"></span>
                <span class="dot dot-green"></span>
              </div>
              <div class="mock-titlebar-text">
                <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><rect x="3" y="2" width="10" height="12" rx="1"/><path d="M6 6h4M6 9h4"/></svg>
                我的工作台
              </div>
            </div>
            <div class="mock-body">
              <div class="mock-sidebar">
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><rect x="3" y="2" width="10" height="12" rx="1"/><path d="M6 6h4M6 9h2"/></svg>
                  文档管理
                </div>
                <div class="mock-sidebar-item active">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#3B6BFF" stroke-width="1.5"><rect x="2" y="3" width="12" height="10" rx="1.5"/><path d="M5 7h6M5 10h4"/></svg>
                  规则配置
                </div>
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><path d="M3 3h10v2H3zM3 7h10v2H3zM3 11h10v2H3z"/></svg>
                  排版任务
                </div>
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><rect x="2" y="3" width="12" height="10" rx="1"/><path d="M5 7h6M5 10h3"/></svg>
                  模板库
                </div>
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><circle cx="8" cy="8" r="5"/><path d="M8 5v3l2 2"/></svg>
                  图表工具
                </div>
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><circle cx="8" cy="8" r="5"/><path d="M8 5v6M5 8h6"/></svg>
                  历史记录
                </div>
                <div class="mock-sidebar-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="#6b7280" stroke-width="1.5"><path d="M4 4l8 8M12 4l-8 8"/></svg>
                  回收站
                </div>
              </div>
              <div class="mock-content">
                <div class="mock-content-header">
                  <div class="mock-content-title">
                    <strong>规则配置</strong>
                    <span class="mock-content-sub">根据学校要求自定义排版规则，系统将严格按照规则执行排版。</span>
                  </div>
                  <div class="mock-badge-ok">
                    <svg viewBox="0 0 14 14" width="12" height="12" fill="#10b981"><circle cx="7" cy="7" r="6"/><path d="M4.5 7l2 2 3.5-3.5" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg>
                    规则已保存
                  </div>
                </div>
                <div class="mock-rules-list">
                  <div class="mock-rule-row" v-for="(r, i) in mockRules" :key="i">
                    <div class="mock-rule-icon" :class="r.color">
                      <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5" v-html="r.icon"></svg>
                    </div>
                    <div class="mock-rule-info">
                      <div class="mock-rule-name">{{ r.name }}</div>
                      <div class="mock-rule-desc">{{ r.desc }}</div>
                    </div>
                    <button class="mock-rule-btn">编辑</button>
                  </div>
                </div>
                <div class="mock-preview">
                  <div class="mock-preview-title">规则预览</div>
                  <div class="mock-preview-heading">一级标题（黑体三号）</div>
                  <div class="mock-preview-sub1">1.1 二级标题（黑体小三）</div>
                  <div class="mock-preview-sub2">1.1.1 三级标题（黑体四号）</div>
                  <div class="mock-preview-lines">
                    <div class="mp-line" v-for="n in 5" :key="n"></div>
                  </div>
                  <div class="mock-preview-lines">
                    <div class="mp-line" v-for="n in 5" :key="'b'+n"></div>
                  </div>
                  <div class="mock-preview-caption">图 1-1 图表示例（宋体小五，居中）</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== Stats ========== -->
    <section class="stats">
      <div class="stats-inner">
        <div class="stat-item" v-for="(s, i) in stats" :key="i">
          <div class="stat-icon" :class="s.color">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="s.icon"></svg>
          </div>
          <div class="stat-number">{{ s.num }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </section>

    <!-- ========== Features ========== -->
    <section class="features">
      <div class="features-inner">
        <div class="feat-left">
          <h2 class="feat-title">用户配置规则</h2>
          <p class="feat-subtitle">根据学校或个人要求，自定义排版规范</p>
          <ul class="feat-checks">
            <li v-for="(c, i) in featChecks" :key="i">
              <svg viewBox="0 0 16 16" width="16" height="16" fill="none"><circle cx="8" cy="8" r="7" fill="#3B6BFF"/><path d="M5 8l2 2 4-4" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/></svg>
              {{ c }}
            </li>
          </ul>
          <div class="feat-buttons">
            <button class="btn-cta-primary" @click="goStart">立即配置规则</button>
            <button class="btn-cta-ghost">使用预设模板</button>
          </div>
        </div>
        <div class="feat-right">
          <div class="feat-icons-row">
            <div class="feat-icon-card" v-for="(f, i) in featIcons" :key="i">
              <div class="fic-icon" :class="f.color">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" v-html="f.icon"></svg>
              </div>
              <div class="fic-label">{{ f.label }}</div>
            </div>
          </div>
          <div class="feat-example">
            <div class="feat-example-title">规则设置示例</div>
            <div class="feat-example-cols">
              <div class="feat-example-col">
                <div class="ex-item" v-for="(e, i) in featExamplesLeft" :key="i">
                  <span class="ex-dot" :style="{background: i===0?'#3B6BFF':i===1?'#10b981':'#f59e0b'}"></span>
                  {{ e }}
                </div>
              </div>
              <div class="feat-example-col">
                <div class="ex-item" v-for="(e, i) in featExamplesRight" :key="i">
                  <span class="ex-dot" :style="{background: i===0?'#3B6BFF':i===1?'#10b981':'#f59e0b'}"></span>
                  {{ e }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== Steps ========== -->
    <section class="steps-section">
      <h2 class="steps-title">三步完成规范排版</h2>
      <div class="steps-inner">
        <div class="step-card" v-for="(s, i) in steps" :key="i">
          <div class="step-icon" :class="s.color">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" v-html="s.icon"></svg>
          </div>
          <div class="step-title">{{ s.title }}</div>
          <div class="step-desc" v-html="s.desc"></div>
        </div>
        <template v-if="false">
          <div class="step-arrow" v-for="n in 2" :key="'a'+n">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#c7d2fe" stroke-width="2" stroke-linecap="round"><path d="M4 12h16M14 6l6 6-6 6"/></svg>
          </div>
        </template>
        <div class="step-arrows">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#c7d2fe" stroke-width="2" stroke-linecap="round"><path d="M4 12h16M14 6l6 6-6 6"/></svg>
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#c7d2fe" stroke-width="2" stroke-linecap="round"><path d="M4 12h16M14 6l6 6-6 6"/></svg>
        </div>
      </div>
    </section>

    <!-- ========== Tools ========== -->
    <section class="tools-section">
      <h2 class="tools-title">丰富的排版与图表工具</h2>
      <div class="tools-grid">
        <div class="tool-card" v-for="(t, i) in tools" :key="i">
          <div class="tool-icon" :class="t.color">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" v-html="t.icon"></svg>
          </div>
          <div class="tool-name">{{ t.name }}</div>
          <div class="tool-desc">{{ t.desc }}</div>
          <a v-if="t.route" class="tool-link" href="#" @click.prevent="$router.push(t.route)">立即使用 <svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M2 6h8M7 3l3 3-3 3"/></svg></a>
          <a v-else class="tool-link" href="#">了解更多 <svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M2 6h8M7 3l3 3-3 3"/></svg></a>
        </div>
      </div>
    </section>

    <!-- ========== Footer ========== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="logo-w sm">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h4l3 12 3-12h4"/></svg>
          </div>
          <span>Word 排版助手</span>
        </div>
        <div class="footer-links">
          <a @click="goPage('/about')">关于我们</a>
          <a @click="goPage('/terms')">使用条款</a>
          <a @click="goPage('/privacy')">隐私政策</a>
          <a href="mailto:2651896126@qq.com">联系我们</a>
        </div>
        <div class="footer-copy">&copy; 2026 Word 排版助手. All rights reserved.</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'

const router = useRouter()

const isLoggedIn = ref(false)
const userName = ref('')
const userAvatar = ref('')

const avatarText = computed(() => (userName.value || 'U').slice(0, 1).toUpperCase())

onMounted(async () => {
  isLoggedIn.value = !!localStorage.getItem('token')
  userName.value = localStorage.getItem('username') || '用户'
  userAvatar.value = localStorage.getItem('avatar') || ''
  // 已登录时以数据库为准同步昵称/头像; token 失效则清除本地状态
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
      // token 失效: 清除本地登录状态, 显示未登录
      isLoggedIn.value = false
      userName.value = ''
      userAvatar.value = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
    }
  }
})

function goLogin() {
  router.push('/login')
}
function goPage(path) {
  router.push(path)
}
function goStart() {
  if (localStorage.getItem('token')) {
    router.push('/templates')
  } else {
    router.push('/login')
  }
}
async function handleUserCommand(cmd) {
  if (cmd === 'templates') {
    router.push('/templates')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      try { await logout() } catch (e) {}
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
      isLoggedIn.value = false
      router.push('/home')
    } catch (e) {
      // 取消
    }
  }
}

const mockRules = [
  { name: '标题样式', desc: '设置各级标题的字体、字号、加粗、对齐方式等', color: 'blue', icon: '<rect x="3" y="3" width="10" height="2" rx="0.5"/><rect x="3" y="7" width="7" height="2" rx="0.5"/><rect x="3" y="11" width="9" height="2" rx="0.5"/>' },
  { name: '段落格式', desc: '设置行距、段前段后、首行缩进、对齐方式等', color: 'green', icon: '<path d="M3 4h10M3 8h10M3 12h7"/>' },
  { name: '字体规范', desc: '设置正文、英文、数字等字体的格式要求', color: 'purple', icon: '<text x="3" y="13" font-size="10" font-weight="bold" fill="currentColor" stroke="none">Aa</text>' },
  { name: '图表编号', desc: '设置图表编号格式、位置和样式', color: 'blue', icon: '<rect x="3" y="4" width="8" height="7" rx="0.5"/><path d="M5 8l2 2 3-3"/><rect x="13" y="4" width="6" height="7" rx="0.5"/><path d="M14 7h4"/>' },
  { name: '页眉页脚', desc: '设置页眉页脚内容、页码格式和位置', color: 'green', icon: '<rect x="3" y="3" width="10" height="10" rx="0.5"/><path d="M3 7h10"/><rect x="13" y="5" width="7" height="10" rx="0.5"/><path d="M13 9h7"/>' },
  { name: '参考文献', desc: '设置参考文献格式（GB/T 7714、APA、MLA 等）', color: 'purple', icon: '<path d="M5 3C5 3 3 5 3 8C3 11 5 13 5 13M11 3C11 3 13 5 13 8C13 11 11 13 11 13"/><path d="M7 5h2M7 8h2M7 11h2"/>' }
]

const stats = [
  { num: '5,000+', label: '用户信赖选择', color: 'blue', icon: '<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>' },
  { num: '98%', label: '格式匹配准确率', color: 'green', icon: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>' },
  { num: '10万+', label: '文档已智能排版', color: 'blue', icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>' },
  { num: '99.9%', label: '数据安全保障', color: 'green', icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>' }
]

const featChecks = [
  '完全自定义各级标题、字体、段落格式',
  '设置图表编号、目录生成规则',
  '支持多种参考文献格式',
  '保存为模板，重复使用'
]

const featIcons = [
  { label: '标题样式', color: 'blue', icon: '<text x="2" y="18" font-size="16" font-weight="bold" fill="currentColor" stroke="none">H1</text>' },
  { label: '段落格式', color: 'green', icon: '<path d="M4 4h12M4 8h12M4 12h8"/><path d="M16 8l-2-2v4z"/>' },
  { label: '字体规范', color: 'purple', icon: '<text x="1" y="18" font-size="14" font-weight="bold" fill="currentColor" stroke="none">Aa</text>' },
  { label: '图表编号', color: 'blue', icon: '<rect x="3" y="3" width="18" height="14" rx="2"/><circle cx="12" cy="10" r="3"/><path d="M7 17l3-3 2 2 4-4"/>' },
  { label: '页眉页脚', color: 'green', icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="12" x2="12" y2="18"/><line x1="9" y1="15" x2="15" y2="15"/>' },
  { label: '参考文献', color: 'purple', icon: '<path d="M6 3c-2 2-2 6 0 8M18 3c2 2 2 6 0 8"/><path d="M9 5h6M9 8h6M9 11h4"/>' }
]

const featExamplesLeft = [
  '一级标题：黑体 三号 加粗 居中',
  '二级标题：黑体 小三 加粗 左对齐',
  '正文：宋体 小四 行距1.5倍 首行缩进2字符'
]
const featExamplesRight = [
  '图表编号：章-图顺序号（如：图 1-1）',
  '表格样式：三线表 格式规范',
  '参考文献：GB/T 7714 - 2015'
]

const steps = [
  {
    title: '上传论文',
    desc: '支持 .docx 格式<br/>智能识别论文结构',
    color: 'blue',
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>'
  },
  {
    title: '应用规则排版',
    desc: '系统按您配置的规则<br/>自动排版和格式调整',
    color: 'purple',
    icon: '<line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="8" cy="6" r="2"/><circle cx="16" cy="12" r="2"/><circle cx="10" cy="18" r="2"/>'
  },
  {
    title: '生成规范文档',
    desc: '生成符合要求的论文<br/>支持多种格式导出',
    color: 'green',
    icon: '<path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>'
  }
]

const tools = [
  {
    name: 'Word 智能排版',
    desc: '识别标题层级、统一字体、字号、行距等自动生成总页边距目录，优化文档格式。',
    color: 'blue',
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><path d="M10 9l-1 1 1 1"/>'
  },
  {
    name: '三线表生成',
    desc: '快速生成规范的三线表格<br/>支持表头合并、自动编号。',
    color: 'green',
    route: '/table3',
    icon: '<rect x="3" y="3" width="18" height="18" rx="1"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/>'
  },
  {
    name: 'ER 图生成',
    desc: '输入实体、属性和关系<br/>自动生成清晰的 ER 图。',
    color: 'orange',
    route: '/er',
    icon: '<circle cx="6" cy="6" r="3"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><line x1="9" y1="6" x2="15" y2="6"/><line x1="6" y1="9" x2="6" y2="15"/><line x1="18" y1="9" x2="18" y2="15"/><line x1="9" y1="18" x2="15" y2="18"/>'
  },
  {
    name: '系统图设计',
    desc: '支持流程图、泳道图、架构图等<br/>一键生成专业系统设计图。',
    color: 'purple',
    route: '/system-design',
    icon: '<rect x="3" y="3" width="7" height="5" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="8" y="14" width="7" height="5" rx="1"/><line x1="6.5" y1="8" x2="6.5" y2="11" stroke-dasharray="2"/><line x1="17.5" y1="8" x2="17.5" y2="11" stroke-dasharray="2"/><line x1="6.5" y1="11" x2="11.5" y2="14"/><line x1="17.5" y1="11" x2="11.5" y2="14"/>'
  },
  {
    name: '自由绘画',
    desc: '自由画板 · UML 设计<br/>拖拽节点、连线、导出 PNG。',
    color: 'orange',
    route: '/free-draw',
    icon: '<path d="M17 3a2.83 2.83 0 114 4L7.5 20.5 2 22l1.5-5.5z"/><path d="M15 5l4 4"/>'
  }
]
</script>

<style scoped>
/* ===== Variables ===== */
.landing {
  --c-primary: #3B6BFF;
  --c-primary-light: #EEF1FF;
  --c-primary-dark: #2D52CC;
  --c-dark: #1a1a2e;
  --c-text: #374151;
  --c-text2: #6b7280;
  --c-text3: #9ca3af;
  --c-border: #e5e7eb;
  --c-bg: #ffffff;
  --c-bg2: #f8f9fc;
  --c-bg3: #f0f4ff;
  --c-green: #10b981;
  --c-purple: #7c3aed;
  --c-orange: #f59e0b;
  --radius: 12px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.10);
  --shadow-xl: 0 16px 48px rgba(0,0,0,0.12);
  --font: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;

  font-family: var(--font);
  color: var(--c-text);
  background: var(--c-bg);
  overflow-x: hidden;
}

/* ===== Navbar ===== */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--c-border);
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 64px;
}
.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-w {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.logo-w.sm {
  width: 26px;
  height: 26px;
  border-radius: 6px;
}
.brand-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-dark);
}
.ai-badge {
  font-size: 11px;
  font-weight: 600;
  color: var(--c-primary);
  background: var(--c-primary-light);
  border: 1px solid #c7d2fe;
  border-radius: 4px;
  padding: 1px 6px;
}
.brand-sub {
  font-size: 13px;
  color: var(--c-text2);
  margin-left: 4px;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  text-decoration: none;
  font-size: 14px;
  color: var(--c-text2);
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.nav-link:hover {
  color: var(--c-primary);
  background: var(--c-primary-light);
}
.nav-link.active {
  color: var(--c-primary);
  font-weight: 600;
}
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: var(--c-primary);
  border-radius: 1px;
}
.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 5px;
  border: 1.5px solid var(--c-border);
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
}
.user-chip:hover {
  border-color: var(--c-primary);
  background: var(--c-primary-light);
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--c-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.username {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== Buttons ===== */
.btn-ghost {
  padding: 8px 20px;
  border: 1.5px solid var(--c-border);
  border-radius: 8px;
  background: #fff;
  color: var(--c-text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}
.btn-primary {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: var(--c-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary:hover {
  background: var(--c-primary-dark);
  box-shadow: 0 4px 12px rgba(59,107,255,0.35);
}

/* ===== Hero ===== */
.hero {
  position: relative;
  padding: 80px 32px 100px;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #f0f4ff 0%, #ffffff 100%);
  z-index: 0;
}
.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.blob-1 {
  width: 500px;
  height: 500px;
  background: #c7d2fe;
  top: -150px;
  right: -100px;
}
.blob-2 {
  width: 400px;
  height: 400px;
  background: #e0e7ff;
  bottom: -100px;
  left: -100px;
}
.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 60px;
}
.hero-left {
  flex: 1;
  min-width: 0;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-primary);
  background: var(--c-primary-light);
  border: 1px solid #c7d2fe;
  border-radius: 20px;
  padding: 6px 16px;
  margin-bottom: 24px;
}
.hero-title {
  font-size: 52px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--c-dark);
  margin: 0 0 20px;
  letter-spacing: -0.02em;
}
.hero-title .highlight {
  color: var(--c-primary);
  position: relative;
}
.hero-title .highlight::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 0;
  right: 0;
  height: 8px;
  background: rgba(59,107,255,0.12);
  border-radius: 4px;
  z-index: -1;
}
.hero-desc {
  font-size: 16px;
  line-height: 1.7;
  color: var(--c-text2);
  margin: 0 0 32px;
}
.hero-cta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}
.btn-cta-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: var(--c-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 16px rgba(59,107,255,0.3);
}
.btn-cta-primary:hover {
  background: var(--c-primary-dark);
  box-shadow: 0 6px 24px rgba(59,107,255,0.4);
  transform: translateY(-1px);
}
.btn-cta-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: #fff;
  color: var(--c-text);
  border: 1.5px solid var(--c-border);
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
}
.btn-cta-ghost:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}
.hero-features {
  display: flex;
  gap: 24px;
}
.feat-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-text2);
}

/* ===== Mock Panel ===== */
.hero-right {
  flex: 1.1;
  min-width: 0;
}
.mock-panel {
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--c-border);
  overflow: hidden;
}
.mock-titlebar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: #fafbfc;
  border-bottom: 1px solid var(--c-border);
}
.mock-dots {
  display: flex;
  gap: 6px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot-red { background: #ff5f57; }
.dot-yellow { background: #ffbd2e; }
.dot-green { background: #28c840; }
.mock-titlebar-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-text2);
  font-weight: 500;
}
.mock-body {
  display: flex;
  height: 380px;
}
.mock-sidebar {
  width: 160px;
  flex-shrink: 0;
  border-right: 1px solid var(--c-border);
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mock-sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 13px;
  color: var(--c-text2);
  cursor: default;
  transition: all 0.15s;
}
.mock-sidebar-item.active {
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-weight: 600;
}
.mock-content {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mock-content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.mock-content-title strong {
  font-size: 15px;
  color: var(--c-dark);
  display: block;
  margin-bottom: 2px;
}
.mock-content-sub {
  font-size: 11px;
  color: var(--c-text3);
  line-height: 1.4;
}
.mock-badge-ok {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--c-green);
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 12px;
  padding: 3px 10px;
  flex-shrink: 0;
}
.mock-rules-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow: hidden;
}
.mock-rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fafbfc;
  border: 1px solid #f0f0f0;
}
.mock-rule-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mock-rule-icon.blue { background: #EEF1FF; color: var(--c-primary); }
.mock-rule-icon.green { background: #ecfdf5; color: var(--c-green); }
.mock-rule-icon.purple { background: #f5f3ff; color: var(--c-purple); }
.mock-rule-info {
  flex: 1;
  min-width: 0;
}
.mock-rule-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-dark);
}
.mock-rule-desc {
  font-size: 10px;
  color: var(--c-text3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mock-rule-btn {
  padding: 3px 12px;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  background: #fff;
  font-size: 11px;
  color: var(--c-text2);
  cursor: default;
  flex-shrink: 0;
}
.mock-preview {
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 12px;
  background: #fafbfc;
}
.mock-preview-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--c-text3);
  margin-bottom: 8px;
  text-align: center;
}
.mock-preview-heading {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-dark);
  margin-bottom: 4px;
}
.mock-preview-sub1 {
  font-size: 11px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 3px;
}
.mock-preview-sub2 {
  font-size: 10px;
  font-weight: 500;
  color: var(--c-text2);
  margin-bottom: 8px;
}
.mock-preview-lines {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 8px;
}
.mp-line {
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
}
.mp-line:nth-child(1) { width: 100%; }
.mp-line:nth-child(2) { width: 92%; }
.mp-line:nth-child(3) { width: 96%; }
.mp-line:nth-child(4) { width: 88%; }
.mp-line:nth-child(5) { width: 60%; }
.mock-preview-caption {
  font-size: 10px;
  color: var(--c-text3);
  text-align: center;
}

/* ===== Stats ===== */
.stats {
  padding: 48px 32px;
  border-top: 1px solid var(--c-border);
  border-bottom: 1px solid var(--c-border);
  background: #fff;
}
.stats-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}
.stat-item {
  text-align: center;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.stat-icon.blue { background: var(--c-primary-light); color: var(--c-primary); }
.stat-icon.green { background: #ecfdf5; color: var(--c-green); }
.stat-number {
  font-size: 28px;
  font-weight: 800;
  color: var(--c-dark);
  margin-bottom: 4px;
  letter-spacing: -0.02em;
}
.stat-label {
  font-size: 13px;
  color: var(--c-text2);
}

/* ===== Features ===== */
.features {
  padding: 80px 32px;
  background: var(--c-bg3);
}
.features-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 60px;
  align-items: flex-start;
}
.feat-left {
  flex: 1;
  min-width: 0;
}
.feat-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--c-dark);
  margin: 0 0 12px;
  letter-spacing: -0.02em;
}
.feat-subtitle {
  font-size: 15px;
  color: var(--c-text2);
  margin: 0 0 28px;
  line-height: 1.6;
}
.feat-checks {
  list-style: none;
  padding: 0;
  margin: 0 0 32px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.feat-checks li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--c-text);
}
.feat-buttons {
  display: flex;
  gap: 12px;
}
.feat-right {
  flex: 1.2;
  min-width: 0;
}
.feat-icons-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.feat-icon-card {
  text-align: center;
}
.fic-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  background: #fff;
  box-shadow: var(--shadow-sm);
}
.fic-icon.blue { color: var(--c-primary); }
.fic-icon.green { color: var(--c-green); }
.fic-icon.purple { color: var(--c-purple); }
.fic-label {
  font-size: 12px;
  color: var(--c-text2);
}
.feat-example {
  background: #fff;
  border-radius: var(--radius);
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border);
}
.feat-example-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-dark);
  margin-bottom: 14px;
}
.feat-example-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 32px;
}
.ex-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--c-text);
  margin-bottom: 6px;
}
.ex-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ===== Steps ===== */
.steps-section {
  padding: 80px 32px;
  background: #fff;
  text-align: center;
}
.steps-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--c-dark);
  margin: 0 0 48px;
  letter-spacing: -0.02em;
}
.steps-inner {
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 0;
}
.step-card {
  flex: 1;
  text-align: center;
  padding: 0 16px;
}
.step-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.step-icon.blue { background: var(--c-primary-light); color: var(--c-primary); }
.step-icon.purple { background: #f5f3ff; color: var(--c-purple); }
.step-icon.green { background: #ecfdf5; color: var(--c-green); }
.step-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--c-dark);
  margin-bottom: 8px;
}
.step-desc {
  font-size: 13px;
  color: var(--c-text2);
  line-height: 1.6;
}
.step-arrows {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 80px;
  padding: 0 8px;
}

/* ===== Tools ===== */
.tools-section {
  padding: 80px 32px 100px;
  background: var(--c-bg2);
}
.tools-title {
  text-align: center;
  font-size: 32px;
  font-weight: 800;
  color: var(--c-dark);
  margin: 0 0 48px;
  letter-spacing: -0.02em;
}
.tools-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.tool-card {
  background: #fff;
  border-radius: var(--radius);
  padding: 28px 24px;
  border: 1px solid var(--c-border);
  transition: all 0.25s;
}
.tool-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: #c7d2fe;
}
.tool-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.tool-icon.blue { background: var(--c-primary-light); color: var(--c-primary); }
.tool-icon.green { background: #ecfdf5; color: var(--c-green); }
.tool-icon.orange { background: #fffbeb; color: var(--c-orange); }
.tool-icon.purple { background: #f5f3ff; color: var(--c-purple); }
.tool-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-dark);
  margin-bottom: 8px;
}
.tool-desc {
  font-size: 13px;
  color: var(--c-text2);
  line-height: 1.6;
  margin-bottom: 16px;
  min-height: 42px;
}
.tool-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-primary);
  text-decoration: none;
  transition: gap 0.2s;
}
.tool-link:hover {
  gap: 8px;
}

/* ===== Footer ===== */
.footer {
  background: var(--c-dark);
  padding: 32px;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.footer-links {
  display: flex;
  gap: 24px;
}
.footer-links a {
  color: #9ca3af;
  text-decoration: none;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: #fff;
}
.footer-copy {
  color: #6b7280;
  font-size: 12px;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .hero-inner { flex-direction: column; gap: 40px; }
  .hero-title { font-size: 36px; }
  .stats-inner { grid-template-columns: repeat(2, 1fr); }
  .features-inner { flex-direction: column; }
  .feat-icons-row { grid-template-columns: repeat(3, 1fr); }
  .tools-grid { grid-template-columns: repeat(2, 1fr); }
  .nav-links { display: none; }
  .step-arrows { display: none; }
}
</style>
