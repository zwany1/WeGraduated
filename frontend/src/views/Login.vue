<template>
  <div class="min-h-screen max-h-screen overflow-hidden grid lg:grid-cols-2">
    <!-- Left Content Section with Animated Characters (照抄 careercompass page.tsx) -->
    <div class="relative hidden lg:flex flex-col justify-between bg-gradient-to-br from-gray-400 via-gray-500 to-gray-600 p-12 text-white">
      <div class="relative z-20">
        <div class="flex items-center gap-2 text-lg font-semibold">
          <span class="inline-flex items-center justify-center size-8 bg-white/10 backdrop-blur-sm p-1 rounded-lg text-base">📄</span>
          <span>论文格式助手</span>
        </div>
      </div>

      <div class="relative z-20 flex items-end justify-center h-[500px]">
        <AnimatedCharacters
          :is-typing="isTyping"
          :show-password="showPassword"
          :password-length="password.length"
        />
      </div>

      <div class="relative z-20 flex items-center gap-8 text-sm text-gray-600">
        <span class="hover:text-gray-900 transition-colors">Privacy Policy</span>
        <span class="hover:text-gray-900 transition-colors">Terms of Service</span>
      </div>

      <!-- Decorative elements -->
      <div class="absolute inset-0 bg-grid-white/[0.05] bg-[size:20px_20px]"></div>
      <div class="absolute top-1/4 right-1/4 size-64 bg-gray-400/20 rounded-full blur-3xl"></div>
      <div class="absolute bottom-1/4 left-1/4 size-96 bg-gray-300/20 rounded-full blur-3xl"></div>
    </div>

    <!-- Right Login Section -->
    <div class="flex items-center justify-center p-8 bg-background">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo -->
        <div class="lg:hidden flex items-center justify-center gap-2 text-lg font-semibold mb-12">📄 论文格式助手</div>

        <!-- Header -->
        <div class="text-center mb-10">
          <h1 class="text-3xl font-bold tracking-tight mb-2">Welcome back!</h1>
          <p class="text-muted-foreground text-sm">请输入你的详细信息</p>
        </div>

        <!-- Login Form -->
        <form @submit.prevent="submit" class="space-y-5">
          <div class="space-y-2">
            <label class="text-sm font-medium">Email / 用户名</label>
            <input
              v-model="form.username"
              type="text"
              placeholder="you@example.com 或用户名"
              autocomplete="off"
              @focus="isTyping = true"
              @blur="isTyping = false"
              class="h-12 bg-background border border-border/60 focus:border-primary w-full rounded-full px-5 outline-none transition-colors"
            />
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">Password</label>
            <div class="relative">
              <input
                ref="pwdRef"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                @keyup.enter="submit"
                class="h-12 pr-10 bg-background border border-border/60 focus:border-primary w-full rounded-full px-5 outline-none transition-colors"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              >
                <svg v-if="showPassword" viewBox="0 0 24 24" class="size-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><path d="M1 1l22 22" /></svg>
                <svg v-else viewBox="0 0 24 24" class="size-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
              </button>
            </div>
          </div>

          <label class="flex items-center space-x-2 cursor-pointer select-none" @click="rememberMe = !rememberMe">
            <span :class="['size-4 rounded border flex items-center justify-center transition-colors', rememberMe ? 'bg-primary border-primary' : 'border-input']">
              <svg v-if="rememberMe" viewBox="0 0 24 24" class="size-3 text-white" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
            </span>
            <label class="text-sm font-normal cursor-pointer">记住我 30 天</label>
          </label>

          <InteractiveHoverButton
            type="submit"
            :text="loading ? '...' : 'Log in'"
            class="w-full h-12 text-base font-medium"
            :disabled="loading"
          />

          <p class="text-center text-sm text-muted-foreground min-h-5">{{ hint }}</p>
        </form>

        <div class="text-center text-sm text-muted-foreground mt-8">
          还没有账号？
          <span class="text-foreground font-medium hover:underline cursor-pointer" @click="goRegister">Sign Up</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, getProfile } from '../api/user'
import AnimatedCharacters from '../components/auth/AnimatedCharacters.vue'
import InteractiveHoverButton from '../components/auth/InteractiveHoverButton.vue'

// ===== 登录页状态 =====
const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)
const isTyping = ref(false)
const pwdRef = ref(null)
const form = reactive({ username: '', password: '' })
const hint = ref('')
const password = computed(() => form.password)

onMounted(async () => {
  if (localStorage.getItem('token')) {
    router.replace('/home')
    return
  }
  const savedUser = localStorage.getItem('remembered_user')
  const savedPwd = localStorage.getItem('remembered_pwd')
  if (savedUser) {
    form.username = savedUser
    rememberMe.value = true
    if (savedPwd) {
      try { form.password = decodeURIComponent(atob(savedPwd)) } catch (e) {}
    }
  }
})

function goRegister() {
  router.push('/register')
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim()) { ElMessage.warning('请输入用户名或邮箱'); return }
  if (!form.password) { ElMessage.warning('请输入密码'); return }
  loading.value = true
  hint.value = '请稍候...'
  try {
    const data = await login({ email: form.username.trim(), username: form.username.trim(), password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    try {
      const p = await getProfile()
      if (p) {
        localStorage.setItem('username', p.nickname || p.username || data.username)
        localStorage.setItem('avatar', p.avatar || '')
      }
    } catch (e) {}
    if (rememberMe.value) {
      localStorage.setItem('remembered_user', form.username.trim())
      localStorage.setItem('remembered_pwd', btoa(encodeURIComponent(form.password)))
    } else {
      localStorage.removeItem('remembered_user')
      localStorage.removeItem('remembered_pwd')
    }
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    hint.value = e.message || ''
  } finally {
    loading.value = false
  }
}
</script>
