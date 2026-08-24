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
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.back-btn {
  font-size: 20px;
  padding: 0 4px;
}
.brand {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  cursor: pointer;
  white-space: nowrap;
}
.links {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
.links a {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  text-decoration: none;
  transition: all 0.2s;
}
.links a:hover {
  background: #f5f7fa;
  color: #3B6BFF;
}
.links a.router-link-active {
  background: #EEF1FF;
  color: #3B6BFF;
  font-weight: 600;
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
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.user-chip:hover {
  border-color: #3B6BFF;
  background: #f5f7ff;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-text {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #3B6BFF;
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.name {
  font-size: 13px;
  color: #303133;
}
</style>
