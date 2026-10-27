<template>
  <header class="pn">
    <a class="pn-brand" @click="go('/home')">论文格式助手</a>
    <nav class="pn-links" aria-label="主导航">
      <a
        v-for="l in links"
        :key="l.path"
        :class="['pn-link', { on: route.path === l.path }]"
        @click="go(l.path)"
      >{{ l.label }}</a>
    </nav>
    <div class="pn-right">
      <el-dropdown v-if="isLoggedIn" trigger="click" @command="onCommand">
        <button class="pn-user" type="button">
          <span class="pn-avatar">{{ initial }}</span>
          <span class="pn-name">{{ nickname }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="templates">我的格式方案</el-dropdown-item>
            <el-dropdown-item command="tasks">排版任务</el-dropdown-item>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <a v-else class="pn-login" @click="go('/login')">登录</a>
      <button class="pn-cta" type="button" @click="start">免费试用</button>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'
import { clearAuth } from '../utils/perm'

const router = useRouter()
const route = useRoute()

const links = [
  { label: '首页', path: '/home' },
  { label: '功能', path: '/features' },
  { label: '模板', path: '/template-market' },
  { label: '教程', path: '/guide' },
  { label: '案例', path: '/cases' },
  { label: '价格', path: '/pricing' }
]

const isLoggedIn = ref(false)
const nickname = ref('同学')
const initial = computed(() => (nickname.value || 'U').slice(0, 1).toUpperCase())

function go(path) {
  router.push(path)
}
function start() {
  router.push(localStorage.getItem('token') ? '/templates' : '/login')
}

onMounted(async () => {
  isLoggedIn.value = !!localStorage.getItem('token')
  nickname.value = localStorage.getItem('username') || '同学'
  if (isLoggedIn.value) {
    try {
      const p = await getProfile()
      if (p) nickname.value = p.nickname || p.username || nickname.value
    } catch (e) {}
  }
})

async function onCommand(cmd) {
  if (cmd === 'templates') return go('/templates')
  if (cmd === 'tasks') return go('/tasks')
  if (cmd === 'profile') return go('/profile')
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      try {
        await logout()
      } catch (e) {}
      clearAuth()
      isLoggedIn.value = false
      ElMessage.success('已退出登录')
      go('/home')
    } catch (e) {}
  }
}
</script>

<style scoped>
.pn {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --red: #dd2627;
  --hair: #c1c1c3;
  display: flex;
  align-items: center;
  gap: 34px;
  height: 72px;
  padding: 0 48px;
  background: var(--paper);
  border-bottom: 1px solid var(--hair);
  position: sticky;
  top: 0;
  z-index: 30;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.pn-brand {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
}
.pn-links {
  display: flex;
  gap: 28px;
  margin-left: 20px;
}
.pn-link {
  font-size: 14px;
  color: var(--ink-2);
  cursor: pointer;
  padding-bottom: 3px;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}
.pn-link:hover {
  color: var(--ink);
}
.pn-link.on {
  color: var(--ink);
  font-weight: 700;
  border-bottom-color: var(--red);
}
.pn-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}
.pn-login {
  font-size: 14px;
  color: var(--ink);
  cursor: pointer;
}
.pn-login:hover {
  color: var(--red);
}
.pn-cta {
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
.pn-cta:hover {
  background: #b91d1e;
}
.pn-user {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  font-family: inherit;
}
.pn-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  background: var(--ink);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.pn-name {
  font-size: 13px;
  color: var(--ink);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .pn {
    height: auto;
    padding: 14px 18px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .pn-links {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 18px;
    overflow-x: auto;
  }
}
</style>
