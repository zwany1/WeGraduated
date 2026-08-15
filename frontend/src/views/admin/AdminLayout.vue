<template>
  <div class="admin-shell">
    <!-- ===== Sidebar ===== -->
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#0d1b2e" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 4h4l3 12 3-12h4" />
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">论文排版助手</span>
          <span class="brand-sub">管理后台</span>
        </div>
      </div>

      <nav class="menu">
        <div v-if="menus.length === 0" class="menu-empty">
          暂无后台菜单权限<br /><span class="menu-empty-sub">请联系系统管理员分配角色</span>
        </div>
        <template v-for="m in menus" :key="m.id">
          <!-- 目录: 可展开 -->
          <div v-if="m.menuType === 'M' && m.children && m.children.length" class="menu-group">
            <div class="menu-group-title" :class="{ open: opened[m.id] }" @click="toggle(m.id)">
              <span class="menu-icon" v-html="iconOf(m)"></span>
              <span class="menu-label">{{ m.menuName }}</span>
              <svg class="group-arrow" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
            <div v-show="opened[m.id]" class="menu-children">
              <router-link v-for="c in visibleChildren(m)" :key="c.id"
                :to="childPath(m, c)" class="menu-item sub" :class="{ active: isActive(childPath(m, c)) }">
                <span class="menu-dot"></span>
                <span class="menu-label">{{ c.menuName }}</span>
              </router-link>
            </div>
          </div>
          <!-- 菜单: 直接链接 -->
          <router-link v-else-if="m.menuType === 'C' && m.path" :key="m.id" :to="'/admin/' + m.path"
            class="menu-item" :class="{ active: isActive(m.path) }">
            <span class="menu-icon" v-html="iconOf(m)"></span>
            <span class="menu-label">{{ m.menuName }}</span>
            <span class="menu-indicator"></span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-foot">
        <router-link to="/home" class="foot-link">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          返回前台
        </router-link>
        <div class="foot-link" @click="handleLogout">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          退出登录
        </div>
      </div>
    </aside>

    <!-- ===== Main ===== -->
    <div class="main">
      <header class="topbar">
        <div class="topbar-title">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <span class="page-kicker">THESIS FORMAT · ADMINISTRATION</span>
        </div>
        <div class="topbar-right">
          <div class="admin-chip">
            <span class="admin-avatar">{{ avatarText }}</span>
            <span class="admin-name">{{ adminName }}</span>
            <span class="admin-tag">管理员</span>
          </div>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { logout } from '../../api/user'
import { getMenus, isAdmin, getToken } from '../../utils/perm'

const route = useRoute()
const router = useRouter()

const menus = ref([])
const opened = ref({})

const adminName = ref('管理员')
const avatarText = ref('A')

const pageTitle = computed(() => route.meta.title || '运营概览')

const isActive = p => {
  if (!p) return false
  return route.path === '/admin/' + p || route.path.startsWith('/admin/' + p + '/')
}

const iconOf = m => {
  if (m.icon) {
    return `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${iconPaths[m.icon] || iconPaths.default}</svg>`
  }
  return `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${iconPaths.default}</svg>`
}

const iconPaths = {
  chart: '<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
  doc: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>',
  document: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>',
  task: '<path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
  setting: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  user: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  role: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  default: '<path d="M4 4h16v16H4z"/>'
}

const visibleChildren = m => (m.children || []).filter(c => c.menuType === 'C')

const childPath = (parent, child) => '/admin/' + (parent.path ? parent.path + '/' : '') + child.path

const toggle = id => {
  opened.value[id] = !opened.value[id]
}

async function loadMenus() {
  // 缓存优先, 后台拉取失败时回退缓存
  try {
    menus.value = await getMenus()
  } catch (e) {
    try {
      menus.value = JSON.parse(localStorage.getItem('menus') || '[]')
    } catch (e2) {
      menus.value = []
    }
  }
  // 默认展开第一个目录
  menus.value.forEach(m => {
    if (m.menuType === 'M') {
      opened.value[m.id] = true
    }
  })
}

watch(
  () => route.path,
  () => {
    // 自动展开包含当前路由的目录
    menus.value.forEach(m => {
      if (m.menuType === 'M' && m.children) {
        const inGroup = m.children.some(c => isActive(childPath(m, c)))
        if (inGroup) opened.value[m.id] = true
      }
    })
  }
)

onMounted(() => {
  const nick = localStorage.getItem('username') || '管理员'
  adminName.value = nick
  avatarText.value = nick.slice(0, 1).toUpperCase()
  loadMenus()
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出管理后台？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try { await logout() } catch (e) {}
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('avatar')
  localStorage.removeItem('role')
  localStorage.removeItem('roles')
  localStorage.removeItem('perms')
  localStorage.removeItem('menus')
  router.push('/home')
}
</script>

<style scoped>
.admin-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
  --ink: #0d1b2e;
  --ink-2: #152a4d;
  --ink-3: #1d3a68;
  --gold: #c9a45c;
  --gold-soft: rgba(201, 164, 92, 0.22);
  --cinnabar: #b23a2e;
  --paper: #f6f2ea;
  --paper-2: #fbf9f5;
  --card: #fffdf9;
  --line: #e6ded0;
  --line-soft: #efe8dc;
  --text: #2c3140;
  --text-2: #7a7d8a;
  --serif: 'Songti SC', 'STSong', 'SimSun', serif;
}

/* ===== Sidebar ===== */
.sidebar {
  width: 248px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0c1a2e 0%, #101f38 60%, #122642 100%);
  color: #cfd8e8;
  position: relative;
  overflow: hidden;
}
.sidebar::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(60% 40% at 100% 0%, rgba(201, 164, 92, 0.08), transparent 60%),
    repeating-linear-gradient(0deg, transparent 0 3px, rgba(255, 255, 255, 0.012) 3px 4px);
  pointer-events: none;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 26px 22px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  position: relative;
  z-index: 1;
}
.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #d9bd82, #c9a45c);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(201, 164, 92, 0.35);
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.01em;
}
.brand-sub {
  font-size: 11px;
  letter-spacing: 0.24em;
  color: var(--gold);
  margin-top: 2px;
}

.menu {
  flex: 1;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  z-index: 1;
  overflow-y: auto;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #93a3bd;
  font-size: 14px;
  text-decoration: none;
  position: relative;
  transition: all 0.22s ease;
  cursor: pointer;
}
.menu-item:hover {
  color: #e8eef8;
  background: rgba(255, 255, 255, 0.05);
}
.menu-item.active {
  color: #fff;
  background: linear-gradient(90deg, rgba(201, 164, 92, 0.16), rgba(201, 164, 92, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
.menu-icon {
  display: flex;
  align-items: center;
  color: currentColor;
  transition: color 0.2s;
}
.menu-item.active .menu-icon {
  color: var(--gold);
}
.menu-label {
  font-weight: 500;
}
.menu-indicator {
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--cinnabar);
  opacity: 0;
  transform: scaleY(0.4);
  transition: all 0.25s ease;
}
.menu-item.active .menu-indicator {
  opacity: 1;
  transform: scaleY(1);
}

/* ===== 目录(可展开) ===== */
.menu-empty {
  padding: 28px 16px;
  text-align: center;
  font-size: 13px;
  color: #7d8bab;
  line-height: 1.7;
}
.menu-empty-sub {
  font-size: 11.5px;
  color: #56688a;
}
.menu-group-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #93a3bd;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.22s ease;
}
.menu-group-title:hover {
  color: #e8eef8;
  background: rgba(255, 255, 255, 0.05);
}
.menu-group-title.open {
  color: #e8eef8;
}
.group-arrow {
  margin-left: auto;
  transition: transform 0.22s ease;
  opacity: 0.6;
}
.menu-group-title.open .group-arrow {
  transform: rotate(180deg);
}
.menu-children {
  margin: 2px 0 4px 18px;
  padding-left: 14px;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.menu-item.sub {
  padding: 9px 12px;
  font-size: 13px;
}
.menu-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.45;
  flex-shrink: 0;
}
.menu-item.sub.active .menu-dot {
  opacity: 1;
  background: var(--gold);
}

.sidebar-foot {
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
  z-index: 1;
}
.foot-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #93a3bd;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}
.foot-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}

/* ===== Main ===== */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--paper);
  background-image:
    linear-gradient(rgba(13, 27, 46, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(13, 27, 46, 0.025) 1px, transparent 1px);
  background-size: 26px 26px;
}
.topbar {
  height: 76px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(251, 249, 245, 0.9);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
  position: relative;
  z-index: 5;
}
.topbar-title {
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.page-title {
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.04em;
}
.page-kicker {
  font-size: 10px;
  letter-spacing: 0.28em;
  color: #b3a583;
  font-weight: 500;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.admin-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  border: 1px solid var(--line);
  border-radius: 30px;
  background: var(--card);
}
.admin-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--ink-2), var(--ink));
  color: var(--gold);
  font-family: var(--serif);
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.admin-name {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}
.admin-tag {
  font-size: 11px;
  color: var(--cinnabar);
  background: rgba(178, 58, 46, 0.1);
  border: 1px solid rgba(178, 58, 46, 0.28);
  border-radius: 4px;
  padding: 1px 7px;
  letter-spacing: 0.06em;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 28px 32px 40px;
}

/* ===== Transition ===== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

::-webkit-scrollbar {
  width: 9px;
  height: 9px;
}
::-webkit-scrollbar-thumb {
  background: #d6cdbb;
  border-radius: 6px;
  border: 2px solid var(--paper);
}
::-webkit-scrollbar-thumb:hover {
  background: #c4b998;
}
</style>

<style>
/* ===== Element Plus theme inside admin shell ===== */
.admin-shell {
  --el-color-primary: #3a6ea5;
  --el-color-primary-light-3: #6d92c4;
  --el-color-primary-light-5: #a4bddd;
  --el-color-primary-light-7: #cbd7ec;
  --el-color-primary-light-8: #dfe7f3;
  --el-color-primary-light-9: #eef2f8;
  --el-color-primary-dark-2: #2c5682;
  --el-color-danger: #b23a2e;
  --el-color-danger-light-3: #cf756c;
  --el-color-danger-light-5: #e0a49e;
  --el-color-danger-light-7: #efd0cc;
  --el-color-danger-light-8: #f5e2e0;
  --el-color-danger-light-9: #fbf1f0;
  --el-color-danger-dark-2: #8e2e25;
  --el-border-color: #e6ded0;
  --el-border-color-light: #efe8dc;
  --el-border-color-lighter: #f4efe6;
  --el-fill-color-light: #f7f4ec;
  --el-fill-color-lighter: #faf8f2;
  --el-bg-color: #fffdf9;
  --el-bg-color-overlay: #fffdf9;
  --el-text-color-primary: #2c3140;
  --el-text-color-regular: #4a4f5e;
  --el-text-color-secondary: #8a8d99;
  --el-font-size-base: 13px;
}

.admin-shell .el-table {
  --el-table-border-color: #efe8dc;
  --el-table-header-bg-color: #f3eee3;
  --el-table-header-text-color: #6b6f7d;
  --el-table-row-hover-bg-color: #f8f4ea;
  --el-table-striped-row-bg-color: #faf7f0;
  font-size: 13px;
}
.admin-shell .el-table th.el-table__cell {
  font-weight: 600;
  letter-spacing: 0.04em;
  font-size: 12.5px;
}
.admin-shell .el-table__cell {
  padding: 11px 0;
}

.admin-shell .el-button {
  --el-button-font-weight: 500;
}
.admin-shell .el-button--small {
  padding: 5px 11px;
  border-radius: 7px;
}
.admin-shell .el-button.is-plain:hover {
  background: var(--el-fill-color-light);
}

.admin-shell .el-pagination {
  --el-pagination-bg-color: #fffdf9;
  --el-pagination-button-bg-color: #fffdf9;
  --el-pagination-hover-color: #3a6ea5;
}
.admin-shell .el-pagination.is-background .el-pager li {
  background: #f4efe6;
  color: #6b6f7d;
  border-radius: 7px;
  margin: 0 2px;
}
.admin-shell .el-pagination.is-background .el-pager li.is-active {
  background: #3a6ea5;
  color: #fff;
}

.admin-shell .el-loading-mask {
  background-color: rgba(255, 253, 249, 0.7);
}
.admin-shell .el-loading-spinner .path {
  stroke: #3a6ea5;
}

.admin-shell .el-message-box {
  border-radius: 12px;
  border-color: #e6ded0;
}
.admin-shell .el-message-box__title {
  color: #0d1b2e;
  font-weight: 600;
}
</style>
