<template>
  <nav class="site-nav">
    <div class="nav-inner">
      <div class="nav-brand" @click="go('/home')">
        <div class="logo-w">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h4l3 12 3-12h4"/></svg>
        </div>
        <span class="brand-text">Word 排版助手</span>
        <span class="ai-badge">AI</span>
      </div>
      <div class="nav-links">
        <a v-for="l in visibleLinks" :key="l.path" class="nav-link" :class="{ active: isActive(l.path) }" @click="go(l.path)">{{ l.label }}</a>
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
          <button class="btn-ghost" @click="go('/login')">登录</button>
          <button class="btn-primary" @click="go('/register')">免费注册</button>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../../api/user'

const router = useRouter()
const links = [
  { label: '首页', path: '/home' },
  { label: '功能', path: '/features' },
  { label: '模板', path: '/template-market' },
  { label: '团队协作', path: '/team', needLogin: true },
  { label: '使用教程', path: '/guide' },
  { label: '案例', path: '/cases' },
  { label: '价格', path: '/pricing' }
]

const visibleLinks = computed(() => links.filter(l => !l.needLogin || isLoggedIn.value))

const isLoggedIn = ref(false)
const userName = ref('')
const userAvatar = ref('')
const avatarText = computed(() => (userName.value || 'U').slice(0, 1).toUpperCase())

onMounted(async () => {
  isLoggedIn.value = !!localStorage.getItem('token')
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
      userName.value = ''
      userAvatar.value = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
    }
  }
})

function isActive(path) {
  return router.currentRoute.value.path === path
}

function go(path) {
  router.push(path)
}

function handleUserCommand(cmd) {
  if (cmd === 'templates') router.push('/templates')
  else if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' }).then(async () => {
      try { await logout() } catch (e) {}
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
      router.push('/home')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.site-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--c-border, #e5e7eb);
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
  cursor: pointer;
}
.logo-w {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--c-primary, #3B6BFF);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3B6BFF, #7c3aed);
}
.brand-text {
  font-size: 17px;
  font-weight: 700;
  color: var(--c-dark, #1a1a2e);
}
.ai-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #3B6BFF, #7c3aed);
  color: #fff;
  font-weight: 700;
}
.nav-links {
  display: flex;
  gap: 28px;
}
.nav-link {
  font-size: 14px;
  color: var(--c-text2, #6b7280);
  cursor: pointer;
  position: relative;
  padding: 6px 0;
  transition: color 0.2s;
  text-decoration: none;
}
.nav-link:hover {
  color: var(--c-primary, #3B6BFF);
}
.nav-link.active {
  color: var(--c-primary, #3B6BFF);
  font-weight: 600;
}
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--c-primary, #3B6BFF);
}
.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.btn-ghost {
  border: 1px solid var(--c-border, #e5e7eb);
  background: #fff;
  color: var(--c-text, #374151);
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover {
  border-color: var(--c-primary, #3B6BFF);
  color: var(--c-primary, #3B6BFF);
}
.btn-primary {
  background: var(--c-primary, #3B6BFF);
  color: #fff;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: background 0.2s;
}
.btn-primary:hover {
  background: var(--c-primary-dark, #2D52CC);
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--c-bg2, #f8f9fc);
  border: 1px solid var(--c-border, #e5e7eb);
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3B6BFF, #7c3aed);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
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
  font-size: 14px;
  color: var(--c-text, #374151);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 900px) {
  .nav-links {
    display: none;
  }
}
</style>
