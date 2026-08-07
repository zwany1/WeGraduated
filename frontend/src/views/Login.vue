<template>
  <div class="login-page">
    <div class="login-box">
      <h2>论文格式助手</h2>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名(3-32位)" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码(6位以上)" size="large" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          {{ tab === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>
      <div class="back" @click="$router.push('/')">返回首页</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/user'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const fn = tab.value === 'login' ? login : register
    const data = await fn({ username: form.username, password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    ElMessage.success(tab.value === 'login' ? '登录成功' : '注册成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  width: 380px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.login-box h2 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 20px;
}
.back {
  margin-top: 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
}
.back:hover {
  color: #409eff;
}
</style>
