<template>
  <div class="oauth-wrap">
    <div class="box">
      <div v-if="loading" class="tip">正在登录...</div>
      <div v-else-if="error" class="tip err">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(true)
const error = ref('')

onMounted(() => {
  const token = route.query.token
  const err = route.query.error
  if (err) {
    error.value = err
    loading.value = false
    setTimeout(() => router.replace('/login'), 2500)
    return
  }
  if (token) {
    localStorage.setItem('token', token)
    ElMessage.success('登录成功')
    router.replace('/home')
  } else {
    error.value = '登录失败，请重试'
    loading.value = false
    setTimeout(() => router.replace('/login'), 2500)
  }
})
</script>

<style scoped>
.oauth-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4ff;
}
.box {
  background: #fff;
  border-radius: 14px;
  padding: 40px 60px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
.tip {
  font-size: 15px;
  color: #374151;
}
.tip.err {
  color: #dc2626;
}
</style>
