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

    <!-- Right Register Section -->
    <div class="flex items-center justify-center p-8 bg-background">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo -->
        <div class="lg:hidden flex items-center justify-center gap-2 text-lg font-semibold mb-12">📄 论文格式助手</div>

        <!-- Header -->
        <div class="text-center mb-10">
          <h1 class="text-3xl font-bold tracking-tight mb-2">Create Account</h1>
          <p class="text-muted-foreground text-sm">请填写你的注册信息</p>
        </div>

        <!-- Register Form -->
        <form @submit.prevent="submit" class="space-y-5">
          <div class="space-y-2">
            <label class="text-sm font-medium">邮箱 <span class="text-red-500">*</span></label>
            <input
              v-model="form.email"
              type="email"
              placeholder="you@example.com"
              autocomplete="off"
              @focus="isTyping = true"
              @blur="isTyping = false"
              class="h-12 bg-background border border-border/60 focus:border-primary w-full rounded-full px-5 outline-none transition-colors"
            />
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">邮箱验证码 <span class="text-red-500">*</span></label>
            <div class="flex gap-3">
              <input
                v-model="form.emailCode"
                type="text"
                maxlength="6"
                placeholder="6位验证码"
                autocomplete="off"
                class="h-12 flex-1 bg-background border border-border/60 focus:border-primary rounded-full px-5 outline-none transition-colors"
              />
              <button
                type="button"
                @click="sendEmailCode"
                :disabled="sending || countdown > 0"
                class="shrink-0 h-12 px-4 rounded-full border border-border/60 text-sm font-medium transition-colors disabled:opacity-50"
                :class="countdown > 0 ? 'cursor-not-allowed text-muted-foreground' : 'cursor-pointer hover:border-primary hover:text-primary'"
              >
                {{ countdown > 0 ? countdown + 's 后重发' : '发送验证码' }}
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">用户名（选填）</label>
            <input
              v-model="form.username"
              type="text"
              placeholder="不填将根据邮箱自动生成"
              autocomplete="off"
              class="h-12 bg-background border border-border/60 focus:border-primary w-full rounded-full px-5 outline-none transition-colors"
            />
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">Password <span class="text-red-500">*</span></label>
            <div class="relative">
              <input
                ref="pwdRef"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="6-64位，需含字母和数字"
                @focus="isTyping = true"
                @blur="isTyping = false"
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
            <p :class="['text-xs min-h-4', strength > 0 ? (strength >= 4 ? 'text-green-600' : strength >= 2 ? 'text-amber-600' : 'text-red-500') : 'text-muted-foreground']">
              {{ strengthText }}
            </p>
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">确认密码 <span class="text-red-500">*</span></label>
            <div class="relative">
              <input
                v-model="form.confirm"
                :type="showPassword ? 'text' : 'password'"
                placeholder="再次输入密码"
                class="h-12 pr-10 bg-background border border-border/60 focus:border-primary w-full rounded-full px-5 outline-none transition-colors"
              />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium">图形验证码 <span class="text-red-500">*</span></label>
            <div class="flex gap-3">
              <input
                v-model="form.captchaCode"
                type="text"
                maxlength="4"
                placeholder="请输入验证码"
                autocomplete="off"
                class="h-12 flex-1 bg-background border border-border/60 focus:border-primary rounded-full px-5 outline-none transition-colors"
              />
              <button
                type="button"
                @click="refreshCaptcha"
                class="shrink-0 h-12 w-[120px] border border-border/60 rounded-full overflow-hidden cursor-pointer bg-background"
                :disabled="!captchaImage"
              >
                <img v-if="captchaImage" :src="'data:image/png;base64,' + captchaImage" alt="验证码" class="w-full h-full object-cover" />
                <span v-else class="text-xs text-muted-foreground">加载中</span>
              </button>
            </div>
          </div>

          <InteractiveHoverButton
            type="submit"
            :text="loading ? '...' : 'Register'"
            class="w-full h-12 text-base font-medium"
            :disabled="loading"
          />

          <p class="text-center text-sm text-muted-foreground min-h-5">{{ hint }}</p>
        </form>

        <div class="text-center text-sm text-muted-foreground mt-8">
          已有账号？
          <span class="text-foreground font-medium hover:underline cursor-pointer" @click="goLogin">Log in</span>
        </div>      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getProfile, sendEmailCode } from '../api/user'
import { generateCaptcha } from '../api/captcha'
import AnimatedCharacters from '../components/auth/AnimatedCharacters.vue'
import InteractiveHoverButton from '../components/auth/InteractiveHoverButton.vue'

// ===== 注册页状态 =====
const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const pwdRef = ref(null)
const captchaImage = ref('')
const sending = ref(false)
const countdown = ref(0)
let countdownTimer = null
const form = reactive({
  email: '',
  username: '',
  password: '',
  confirm: '',
  emailCode: '',
  captchaCode: '',
  captchaId: ''
})
const hint = ref('')
const password = computed(() => form.password)

async function refreshCaptcha() {
  try {
    const data = await generateCaptcha()
    form.captchaId = data.captchaId
    captchaImage.value = data.imageBase64
    form.captchaCode = ''
  } catch (e) {}
}

const strength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 6) score++
  if (p.length >= 12) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^a-zA-Z0-9]/.test(p)) score++
  return score
})

const strengthText = computed(() => {
  if (!form.password) return '密码强度：至少6位，建议字母+数字+符号'
  if (strength.value <= 1) return '密码强度：弱'
  if (strength.value <= 3) return '密码强度：中'
  return '密码强度：强'
})

function startCountdown() {
  countdown.value = 60
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(countdownTimer)
  }, 1000)
}

onMounted(async () => {
  if (localStorage.getItem('token')) {
    router.replace('/home')
    return
  }
  refreshCaptcha()
})

onBeforeUnmount(() => {
  clearInterval(countdownTimer)
})

function goLogin() {
  router.push('/login')
}

async function sendCode() {
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!form.email.trim()) { ElMessage.warning('请先输入邮箱'); return }
  if (!emailRe.test(form.email.trim())) { ElMessage.warning('邮箱格式不正确'); return }
  if (!form.captchaCode.trim()) { ElMessage.warning('请先输入图形验证码'); return }
  sending.value = true
  try {
    await sendEmailCode({
      email: form.email.trim(),
      captchaId: form.captchaId,
      captchaCode: form.captchaCode
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
    refreshCaptcha()
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
    refreshCaptcha()
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (loading.value) return
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!form.email.trim()) { ElMessage.warning('请输入邮箱'); return }
  if (!emailRe.test(form.email.trim())) { ElMessage.warning('邮箱格式不正确'); return }
  if (!form.emailCode.trim()) { ElMessage.warning('请输入邮箱验证码'); return }
  if (form.username.trim() && (form.username.trim().length < 3 || form.username.trim().length > 32)) { ElMessage.warning('用户名长度为3-32位'); return }
  if (!form.password) { ElMessage.warning('请输入密码'); return }
  if (form.password.length < 6 || form.password.length > 64) { ElMessage.warning('密码长度为6-64位'); return }
  if (strength.value < 2) { ElMessage.warning('密码强度过弱，请使用字母+数字组合'); return }
  if (form.password !== form.confirm) { ElMessage.warning('两次输入的密码不一致'); return }
  if (!form.captchaCode.trim()) { ElMessage.warning('请输入图形验证码'); return }
  loading.value = true
  hint.value = '请稍候...'
  try {
    const data = await register({
      email: form.email.trim(),
      username: form.username.trim(),
      password: form.password,
      emailCode: form.emailCode.trim(),
      captchaId: form.captchaId,
      captchaCode: form.captchaCode
    })
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
    ElMessage.success('注册成功')
    router.push('/home')
  } catch (e) {
    hint.value = e.message || ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>
