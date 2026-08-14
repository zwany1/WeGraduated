<template>
  <div class="min-h-screen max-h-screen overflow-hidden grid lg:grid-cols-2">
    <!-- Left Content Section with Animated Characters -->
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

      <div class="relative z-20 flex items-center gap-8 text-sm text-white/60">
        <span class="hover:text-white transition-colors cursor-pointer" @click="goPrivacy">Privacy Policy</span>
        <span class="hover:text-white transition-colors cursor-pointer">Terms of Service</span>
      </div>

      <!-- Decorative elements -->
      <div class="absolute inset-0 bg-grid-white/[0.05] bg-[size:20px_20px]"></div>
      <div class="absolute -top-20 -right-20 size-80 bg-white/10 rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 -left-24 size-96 bg-white/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-24 right-1/4 size-80 bg-white/10 rounded-full blur-3xl"></div>
    </div>

    <!-- Right Login Section -->
    <div class="flex items-center justify-center p-8" style="background:#f0f5fa">
      <div class="w-full max-w-[420px] bg-white rounded-2xl shadow-[0_10px_40px_rgba(64,158,255,0.12)] p-10">
        <!-- Mobile Logo -->
        <div class="lg:hidden flex items-center justify-center gap-2 text-lg font-semibold mb-8">
          <span class="inline-flex items-center justify-center size-8 bg-[#409eff] text-white rounded-lg">📄</span>
          论文格式助手
        </div>

        <!-- Header -->
        <div class="text-center mb-12">
          <h1 class="text-[26px] font-semibold tracking-tight mb-1" style="color:#303133">Welcome!</h1>
          <p class="text-sm mb-4 text-center" style="color:#909399">登录论文格式助手，继续你的论文排版</p>
        </div>

        <!-- Login Form -->
        <form @submit.prevent="submit" class="space-y-12">
          <div class="input-block">
            <label for="username" class="input-label">Email / 用户名</label>
            <input
              id="username"
              v-model="form.username"
              type="text"
              placeholder="you@example.com 或用户名"
              autocomplete="off"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </div>

          <div class="input-block">
            <label for="password" class="input-label">Password</label>
            <div class="relative">
              <input
                id="password"
                ref="pwdRef"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                @keyup.enter="submit"
                class="pr-8"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-0 top-1/2 -translate-y-1/2 text-[#a0cfff] hover:text-[#409eff] transition-colors"
              >
                <svg v-if="showPassword" viewBox="0 0 24 24" class="size-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><path d="M1 1l22 22" /></svg>
                <svg v-else viewBox="0 0 24 24" class="size-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
              </button>
            </div>
          </div>

          <div class="input-block">
            <label for="captcha" class="input-label">验证码</label>
            <div class="flex items-center gap-3">
              <input
                id="captcha"
                v-model="form.captchaCode"
                type="text"
                maxlength="4"
                placeholder="请输入验证码"
                autocomplete="off"
                class="flex-1"
              />
              <button
                type="button"
                @click="refreshCaptcha"
                class="shrink-0 h-10 w-[110px] rounded-md overflow-hidden cursor-pointer border border-[#dcdfe6] bg-white"
                :disabled="!captchaImage"
              >
                <img v-if="captchaImage" :src="'data:image/png;base64,' + captchaImage" alt="验证码" class="w-full h-full object-cover" />
                <span v-else class="text-xs text-muted-foreground">加载中</span>
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between" style="margin: 10px;">
            <label class="flex items-center gap-2 cursor-pointer select-none text-sm" style="color:#909399" @click="rememberMe = !rememberMe">
              <span :class="['size-4 rounded border flex items-center justify-center transition-colors', rememberMe ? 'bg-[#409eff] border-[#409eff]' : 'border-[#dcdfe6]']">
                <svg v-if="rememberMe" viewBox="0 0 24 24" class="size-3 text-white" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
              </span>
              记住我 30 天
            </label>
            <span class="text-sm cursor-pointer hover:underline" style="color:#409eff" @click="goForgot">Forgot your password?</span>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="input-button w-full"
          >{{ loading ? '...' : 'Login' }}</button>

          <p class="text-center text-sm min-h-5" style="color:#909399">{{ hint }}</p>
        </form>

        <p class="sign-up">
          还没有账号？
          <a class="cursor-pointer" @click="goRegister">Sign up now</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, getProfile } from '../api/user'
import { generateCaptcha } from '../api/captcha'
import AnimatedCharacters from '../components/auth/AnimatedCharacters.vue'

// ===== 登录页状态 =====
const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)
const isTyping = ref(false)
const pwdRef = ref(null)
const captchaImage = ref('')
const form = reactive({ username: '', password: '', captchaCode: '', captchaId: '' })
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
  refreshCaptcha()
})

function goRegister() {
  router.push('/register')
}

function goForgot() {
  router.push('/forgot-password')
}

function goPrivacy() {
  router.push('/privacy')
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim()) { ElMessage.warning('请输入用户名或邮箱'); return }
  if (!form.password) { ElMessage.warning('请输入密码'); return }
  if (!form.captchaCode.trim()) { ElMessage.warning('请输入图形验证码'); return }
  loading.value = true
  hint.value = '请稍候...'
  try {
    const data = await login({
      email: form.username.trim(),
      username: form.username.trim(),
      password: form.password,
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
    if (rememberMe.value) {
      localStorage.setItem('remembered_user', form.username.trim())
      localStorage.setItem('remembered_pwd', btoa(encodeURIComponent(form.password)))
    } else {
      localStorage.removeItem('remembered_user')
      localStorage.removeItem('remembered_pwd')
    }
    ElMessage.success('登录成功')
    router.push(router.currentRoute.value.query.redirect || '/home')
  } catch (e) {
    hint.value = e.message || ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Element Plus 默认主题蓝表单 */
.input-block {
  display: flex;
  flex-direction: column;
  padding: 10px 10px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  margin: 10px;
  background: #fff;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.input-label {
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.7px;
  color: #909399;
  transition: color 0.3s;
}
.input-block input {
  outline: 0;
  border: 0;
  padding: 4px 0 0;
  font-size: 14px;
  width: 100%;
  color: #303133;
  background: transparent;
}
.input-block input::placeholder {
  color: #c0c4cc;
  opacity: 1;
}
.input-block:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}
.input-block:focus-within .input-label {
  color: #409eff;
}
.input-button {
  padding: 0.9em 1.6em;
  border: none;
  outline: none;
  color: #fff;
  font-family: inherit;
  font-weight: 500;
  font-size: 17px;
  cursor: pointer;
  position: relative;
  z-index: 0;
  border-radius: 12px;
  background: transparent;
}
/* 按钮深色底面 */
.input-button::after {
  content: "";
  z-index: -1;
  position: absolute;
  width: 100%;
  height: 100%;
  background-color: rgb(46, 46, 46);
  left: 0;
  top: 0;
  border-radius: 10px;
}
/* glow 炫光 */
.input-button::before {
  content: "";
  background: linear-gradient(
    45deg,
    #FF0000, #002BFF, #FF00C8, #002BFF,
      #FF0000, #002BFF, #FF00C8, #002BFF
  );
  position: absolute;
  top: -2px;
  left: -2px;
  background-size: 600%;
  z-index: -1;
  width: calc(100% + 4px);
  height: calc(100% + 4px);
  filter: blur(8px);
  animation: glowing 20s linear infinite;
  transition: opacity 0.3s ease-in-out;
  border-radius: 10px;
  opacity: 0;
}
@keyframes glowing {
  0% { background-position: 0 0; }
  50% { background-position: 400% 0; }
  100% { background-position: 0 0; }
}
/* hover 显示炫光 */
.input-button:hover::before {
  opacity: 1;
}
.input-button:active::after {
  background: transparent;
}
.input-button:active {
  color: #000;
  font-weight: bold;
}
.input-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.input-button:disabled::before {
  opacity: 0;
}
.sign-up {
  margin: 56px 0 0;
  font-size: 14px;
  text-align: center;
  color: #909399;
}
.sign-up a {
  color: #409eff;
  font-weight: 600;
}
.sign-up a:hover {
  color: #66b1ff;
}
</style>
