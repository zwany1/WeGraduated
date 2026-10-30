<template>
  <header class="hdr" :class="[`hdr-${variant}`]">
    <button v-if="back" class="hdr-back" type="button" aria-label="返回" @click="goBack">‹</button>
    <a class="hdr-brand" @click="go('/home')">论文格式助手</a>

    <nav class="hdr-nav" aria-label="主导航">
      <a
        v-for="l in links"
        :key="l.path"
        :class="['hdr-link', { on: isOn(l) }]"
        @click="go(l.path)"
      >{{ l.label }}</a>
    </nav>

    <div class="hdr-right">
      <NotificationBell v-if="loggedIn" />
      <slot name="actions" />
      <el-dropdown v-if="loggedIn" trigger="click" @command="onCommand">
        <button class="hdr-user" type="button">
          <img v-if="avatar" :src="avatar" class="hdr-avatar-img" alt="" />
          <span v-else class="hdr-avatar">{{ initial }}</span>
          <span class="hdr-name">{{ nickname }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="templates">我的格式方案</el-dropdown-item>
            <el-dropdown-item command="tasks">排版任务</el-dropdown-item>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="admin" divided>管理后台</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <a v-else class="hdr-login" @click="go('/login')">登录</a>
      <button v-if="variant === 'public'" class="hdr-cta" type="button" @click="start">免费试用</button>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'
import { clearAuth } from '../utils/perm'
import { backOrHome } from '../utils/nav'
import NotificationBell from './NotificationBell.vue'

const props = defineProps({
  /** public = 营销页链接集；workbench = 工作台链接集 */
  variant: { type: String, default: 'public' },
  /** 是否显示返回按钮（工具页/详情页用） */
  back: { type: Boolean, default: false }
})

const router = useRouter()
const route = useRoute()

const PUBLIC_LINKS = [
  { label: '首页', path: '/home' },
  { label: '功能', path: '/features' },
  { label: '模板', path: '/template-market' },
  { label: '教程', path: '/guide' },
  { label: '案例', path: '/cases' },
  { label: '价格', path: '/pricing' }
]
const WORKBENCH_LINKS = [
  { label: '首页', path: '/home' },
  { label: '我的方案', path: '/templates' },
  { label: '排版任务', path: '/tasks' },
  { label: '团队', path: '/team' },
  { label: '市场', path: '/template-market' },
  { label: '工具', path: '/system-design' }
]

const links = computed(() => (props.variant === 'workbench' ? WORKBENCH_LINKS : PUBLIC_LINKS))

const loggedIn = ref(false)
const nickname = ref('同学')
const avatar = ref('')
const isAdmin = ref(false)
const initial = computed(() => (nickname.value || 'U').slice(0, 1).toUpperCase())

function isOn(l) {
  if (l.path === '/templates') return route.path === '/templates' || route.path.startsWith('/template/')
  if (l.path === '/template-market') return route.path === '/template-market'
  return route.path === l.path
}

function go(path) {
  router.push(path)
}
function goBack() {
  backOrHome(router)
}
function start() {
  router.push(localStorage.getItem('token') ? '/templates' : '/login')
}

onMounted(async () => {
  loggedIn.value = !!localStorage.getItem('token')
  nickname.value = localStorage.getItem('username') || '同学'
  avatar.value = localStorage.getItem('avatar') || ''
  isAdmin.value = localStorage.getItem('role') === 'ADMIN'
  if (!isAdmin.value) {
    try {
      const menus = JSON.parse(localStorage.getItem('menus') || '[]')
      if (menus.length) isAdmin.value = true
    } catch (e) {}
  }
  if (!loggedIn.value) return
  try {
    const p = await getProfile()
    if (p) {
      nickname.value = p.nickname || p.username || nickname.value
      avatar.value = p.avatar || ''
      localStorage.setItem('username', nickname.value)
      localStorage.setItem('avatar', avatar.value)
    }
  } catch (e) {}
})

async function onCommand(cmd) {
  if (cmd === 'templates') return go('/templates')
  if (cmd === 'tasks') return go('/tasks')
  if (cmd === 'profile') return go('/profile')
  if (cmd === 'admin') return go('/admin/dashboard')
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      try {
        await logout()
      } catch (e) {}
      clearAuth()
      loggedIn.value = false
      ElMessage.success('已退出登录')
      go('/home')
    } catch (e) {}
  }
}
</script>

<style scoped>
/* 全站唯一导航规格：品牌 20/900 · 链接 14/间距28 · 右侧间距18 · 高72 · 左右48 */
.hdr {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --red: #dd2627;
  --hair: #c1c1c3;
  display: flex;
  align-items: center;
  gap: 28px;
  height: 72px;
  padding: 0 48px;
  background: var(--paper);
  border-bottom: 1px solid var(--hair);
  position: sticky;
  top: 0;
  z-index: 100;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  box-sizing: border-box;
}
/* 返回按钮绝对定位：不占流，保证品牌在所有页面都从 48px 起 */
.hdr-back {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  padding: 0;
  background: none;
  border: 0;
  color: var(--ink);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}
.hdr-back:hover {
  color: var(--red);
}
.hdr-brand {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
}
.hdr-nav {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-left: 20px;
}
.hdr-link {
  font-size: 14px;
  color: var(--ink-2);
  cursor: pointer;
  padding-bottom: 3px;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}
.hdr-link:hover {
  color: var(--ink);
}
.hdr-link.on {
  color: var(--ink);
  font-weight: 700;
  border-bottom-color: var(--red);
}
.hdr-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}
.hdr-login {
  font-size: 14px;
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
}
.hdr-login:hover {
  color: var(--red);
}
.hdr-cta {
  height: 38px;
  padding: 0 18px;
  background: var(--red);
  color: #fff;
  border: 0;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.hdr-cta:hover {
  background: #b91d1e;
}
.hdr-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  background: none;
  border: 1px solid var(--hair);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s;
}
.hdr-user:hover {
  border-color: var(--ink);
}
.hdr-avatar,
.hdr-avatar-img {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  background: var(--ink);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  object-fit: cover;
  flex-shrink: 0;
}
.hdr-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .hdr {
    height: auto;
    padding: 12px 18px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .hdr-back {
    position: static;
    transform: none;
    margin-right: -6px;
  }
  .hdr-brand {
    font-size: 17px;
  }
  .hdr-nav {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 18px;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .hdr-nav::-webkit-scrollbar {
    display: none;
  }
  .hdr-right {
    gap: 10px;
  }
  .hdr-cta {
    height: 34px;
    padding: 0 12px;
    font-size: 13px;
  }
  .hdr-name {
    display: none;
  }
}
</style>
