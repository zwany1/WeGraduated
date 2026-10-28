<template>
  <header class="site-nav">
    <el-button text class="back-btn" @click="backOrHome($router)">‹</el-button>
    <div class="brand" @click="goHome">论文格式助手</div>
    <nav class="links">
      <router-link to="/home">首页</router-link>
      <router-link to="/templates">我的方案</router-link>
      <router-link to="/tasks">排版任务</router-link>
      <router-link to="/team">团队</router-link>
      <router-link to="/template-market">市场</router-link>
    </nav>
    <div class="right">
      <slot />
      <el-dropdown v-if="loggedIn" trigger="click" @command="onCommand">
        <div class="user-chip">
          <img v-if="avatar" :src="avatar" class="avatar" alt="" />
          <span v-else class="avatar-text">{{ avatarText }}</span>
          <span class="name">{{ nickname }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button v-else type="primary" @click="$router.push('/login')">登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, logout } from '../api/user'
import { backOrHome } from '../utils/nav'

const router = useRouter()
const loggedIn = ref(false)
const nickname = ref('用户')
const avatar = ref('')
const avatarText = computed(() => (nickname.value || 'U').slice(0, 1).toUpperCase())

onMounted(async () => {
  const token = localStorage.getItem('token')
  loggedIn.value = !!token
  if (!token) return
  nickname.value = localStorage.getItem('username') || '用户'
  avatar.value = localStorage.getItem('avatar') || ''
  try {
    const p = await getProfile()
    if (p) {
      nickname.value = p.nickname || p.username || '用户'
      avatar.value = p.avatar || ''
      localStorage.setItem('username', nickname.value)
      localStorage.setItem('avatar', avatar.value)
    }
  } catch (e) {}
})

function goHome() {
  router.push('/home')
}

async function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      try { await logout() } catch (e) {}
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('avatar')
      router.push('/home')
    } catch (e) {}
  }
}
</script>

<style scoped>
.site-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 30px;
  background: #f7f6f6;
  border-bottom: 1px solid #c1c1c3;
  position: sticky;
  top: 0;
  z-index: 100;
}
.back-btn {
  font-size: 20px;
  padding: 0 4px;
  color: #0a0909;
}
.brand {
  font-size: 18px;
  font-weight: 900;
  color: #0a0909;
  cursor: pointer;
  white-space: nowrap;
  letter-spacing: -0.01em;
}
.links {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
.links a {
  padding: 6px 12px;
  font-size: 14px;
  color: #6b6b6e;
  text-decoration: none;
  position: relative;
  transition: color 0.2s;
}
.links a:hover {
  color: #0a0909;
}
.links a.router-link-active {
  color: #0a0909;
  font-weight: 700;
}
.links a.router-link-active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 3px;
  background: #dd2627;
}
.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: 0;
  border: 1px solid #c1c1c3;
  background: transparent;
  transition: border-color 0.2s;
}
.user-chip:hover {
  border-color: #0a0909;
}
.avatar {
  width: 26px;
  height: 26px;
  border-radius: 0;
  object-fit: cover;
  display: block;
}
.avatar-text {
  width: 26px;
  height: 26px;
  border-radius: 0;
  background: #0a0909;
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.name {
  font-size: 13px;
  color: #0a0909;
  font-weight: 500;
}
</style>
