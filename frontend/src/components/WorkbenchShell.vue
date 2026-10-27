<template>
  <div class="wb">
    <header class="wb-top">
      <a class="wb-brand" @click="go('/home')">论文格式助手</a>
      <nav class="wb-nav" aria-label="工作台导航">
        <a
          v-for="item in navItems"
          :key="item.path"
          :class="['wb-link', { on: isOn(item) }]"
          @click="go(item.path)"
        >{{ item.label }}</a>
      </nav>
      <div class="wb-right">
        <slot name="actions" />
        <el-dropdown trigger="click" @command="onCommand">
          <button class="wb-user" type="button">
            <span class="wb-avatar">{{ initial }}</span>
            <span class="wb-name">{{ nickname }}</span>
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
      </div>
    </header>

    <div class="wb-head">
      <h1 class="wb-title">{{ title }}</h1>
      <div class="wb-headside">
        <slot name="meta" />
      </div>
    </div>

    <div v-if="$slots.toolbar" class="wb-toolbar">
      <slot name="toolbar" />
    </div>

    <main class="wb-main">
      <slot />
    </main>

    <footer v-if="$slots.footer" class="wb-foot">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'
import { clearAuth } from '../utils/perm'

defineProps({
  title: { type: String, required: true }
})

const router = useRouter()
const route = useRoute()

const navItems = [
  { label: '首页', path: '/home' },
  { label: '我的方案', path: '/templates' },
  { label: '排版任务', path: '/tasks' },
  { label: '团队', path: '/team' },
  { label: '市场', path: '/template-market' },
  { label: '工具', path: '/system-design' }
]

const nickname = ref('同学')
const isAdmin = ref(false)
const initial = computed(() => (nickname.value || 'U').slice(0, 1).toUpperCase())

function isOn(item) {
  if (item.path === '/templates') return route.path === '/templates' || route.path.startsWith('/template/')
  if (item.path === '/team') return route.path === '/team'
  return route.path === item.path
}

function go(path) {
  router.push(path)
}

onMounted(async () => {
  nickname.value = localStorage.getItem('username') || '同学'
  isAdmin.value = localStorage.getItem('role') === 'ADMIN'
  if (!isAdmin.value) {
    try {
      const menus = JSON.parse(localStorage.getItem('menus') || '[]')
      if (menus.length) isAdmin.value = true
    } catch (e) {}
  }
  if (localStorage.getItem('token')) {
    try {
      const p = await getProfile()
      if (p) {
        nickname.value = p.nickname || p.username || nickname.value
        localStorage.setItem('username', nickname.value)
      }
    } catch (e) {}
  }
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
      ElMessage.success('已退出登录')
      router.push('/home')
    } catch (e) {}
  }
}
</script>

<style scoped>
/* 工作台外壳：与首页同一套瑞士体系（纸白 / 硬线 / 单一信号红 / 零圆角） */
.wb {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --ink-3: #6b6b6b;
  --red: #dd2627;
  --hair: #c1c1c3;
  --hair-soft: #e4e4e4;
  --sans: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  min-height: 100vh;
  background: var(--paper);
  color: var(--ink);
  font-family: var(--sans);
  -webkit-font-smoothing: antialiased;
}

/* ---------------- 顶栏 ---------------- */
.wb-top {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 34px;
  padding: 0 48px;
  border-bottom: 1px solid var(--hair);
  background: var(--paper);
  position: sticky;
  top: 0;
  z-index: 20;
}
.wb-brand {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.01em;
  cursor: pointer;
  white-space: nowrap;
}
.wb-nav {
  display: flex;
  gap: 28px;
  margin-left: 20px;
}
.wb-link {
  font-size: 14px;
  color: var(--ink-2);
  cursor: pointer;
  padding-bottom: 3px;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}
.wb-link:hover {
  color: var(--ink);
}
.wb-link.on {
  color: var(--ink);
  font-weight: 700;
  border-bottom-color: var(--red);
}
.wb-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}
.wb-user {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  font-family: inherit;
}
.wb-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  background: var(--ink);
  color: var(--paper);
  font-size: 13px;
  font-weight: 700;
}
.wb-name {
  font-size: 13px;
  color: var(--ink);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------------- 页头 ---------------- */
.wb-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 44px 48px 18px;
  border-bottom: 2px solid var(--ink);
  max-width: 1440px;
  margin: 0 auto;
  box-sizing: border-box;
}
.wb-title {
  margin: 0;
  font-size: 52px;
  line-height: 1.06;
  font-weight: 900;
  letter-spacing: -0.02em;
}
.wb-headside {
  text-align: right;
  font-size: 13px;
  line-height: 1.9;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

/* ---------------- 工具条 ---------------- */
.wb-toolbar {
  max-width: 1440px;
  margin: 0 auto;
  padding: 18px 48px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-sizing: border-box;
}

/* ---------------- 内容 ---------------- */
.wb-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 48px 64px;
  box-sizing: border-box;
}
.wb-foot {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 48px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
  box-sizing: border-box;
}

@media (max-width: 900px) {
  .wb-top {
    height: auto;
    padding: 14px 18px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .wb-nav {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 18px;
    overflow-x: auto;
  }
  .wb-head {
    padding: 26px 18px 14px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .wb-title {
    font-size: 30px;
  }
  .wb-headside {
    text-align: left;
  }
  .wb-toolbar {
    padding: 14px 18px 0;
  }
  .wb-main {
    padding: 0 18px 48px;
  }
  .wb-foot {
    padding: 0 18px 24px;
  }
}
</style>
