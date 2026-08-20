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
        <span class="hover:text-white transition-colors cursor-pointer" @click="goPrivacy">隐私政策</span>
        <span class="hover:text-white transition-colors cursor-pointer" @click="goTerms">服务条款</span>
      </div>

      <!-- Decorative elements -->
      <div class="absolute inset-0 bg-grid-white/[0.05] bg-[size:20px_20px]"></div>
      <div class="absolute -top-20 -right-20 size-80 bg-white/10 rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 -left-24 size-96 bg-white/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-24 right-1/4 size-80 bg-white/10 rounded-full blur-3xl"></div>
    </div>

    <!-- Right Reset Password Section -->
    <div class="flex items-center justify-center p-8" style="background:#f0f5fa">
      <div class="w-full max-w-[420px] bg-white rounded-2xl shadow-[0_10px_40px_rgba(64,158,255,0.12)] p-10">
        <!-- Mobile Logo -->
        <div class="lg:hidden flex items-center justify-center gap-2 text-lg font-semibold mb-8">
          <span class="inline-flex items-center justify-center size-8 bg-[#409eff] text-white rounded-lg">📄</span>
          论文格式助手
        </div>

        <!-- Header -->
        <div class="text-center mb-12">
          <h1 class="text-[26px] font-semibold tracking-tight mb-1" style="color:#303133">重置密码</h1>
          <p class="text-sm mb-4 text-center" style="color:#909399">通过邮箱验证码重置你的密码</p>
        </div>

        <!-- Reset Form -->
        <form @submit.prevent="submit" class="space-y-12">
          <div class="input-block">
            <label for="email" class="input-label">邮箱 <span style="color:#e74c3c">*</span></label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱"
              autocomplete="off"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </div>

          <div class="input-block">
            <label for="emailCode" class="input-label">邮箱验证码 <span style="color:#e74c3c">*</span></label>
            <div class="flex items-center gap-3">
              <input
                id="emailCode"
                v-model="form.emailCode"
                type="text"
                maxlength="6"
                placeholder="6位验证码"
                autocomplete="off"
                class="flex-1"
              />
              <button
                type="button"
                @click="sendCode"
                :disabled="sending || countdown > 0"
                class="shrink-0 h-10 px-4 rounded-md text-sm font-semibold border border-[#dcdfe6] bg-white transition-colors disabled:opacity-50"
                :class="countdown > 0 ? 'cursor-not-allowed text-[#909399]' : 'cursor-pointer text-[#409eff] hover:border-[#409eff] hover:text-[#409eff]'"
              >
                {{ countdown > 0 ? countdown + '秒后重发' : '发送验证码' }}
              </button>
            </div>
          </div>

          <div class="input-block">
            <label for="newPassword" class="input-label">新密码 <span style="color:#e74c3c">*</span></label>
            <div class="relative">
              <input
                id="newPassword"
                ref="pwdRef"
                v-model="form.newPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="6-64位，需含字母和数字"
                @focus="isTyping = true"
                @blur="isTyping = false"
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

          <button type="submit" :disabled="loading" class="input-button w-full">
            {{ loading ? '...' : '重置密码' }}
          </button>

          <p class="text-center text-sm min-h-5" style="color:#909399">{{ hint }}</p>
        </form>

        <p class="sign-up">
          想起密码了？
          <a class="cursor-pointer" @click="goLogin">返回登录</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword, sendEmailCode } from '../api/user'
import AnimatedCharacters from '../components/auth/AnimatedCharacters.vue'

// ===== 重置密码状态 =====
const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const pwdRef = ref(null)
const countdown = ref(0)
let countdownTimer = null
const form = reactive({ email: '', emailCode: '', newPassword: '' })
const hint = ref('')
const password = computed(() => form.newPassword)

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
  }
})

onBeforeUnmount(() => {
  clearInterval(countdownTimer)
})

function goLogin() {
  router.push('/login')
}

function goPrivacy() {
  router.push('/privacy')
}

function goTerms() {
  router.push('/terms')
}

async function sendCode() {
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!form.email.trim()) { ElMessage.warning('请先输入邮箱'); return }
  if (!emailRe.test(form.email.trim())) { ElMessage.warning('邮箱格式不正确'); return }
  sending.value = true
  try {
    await sendEmailCode({ email: form.email.trim() })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
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
  if (!form.newPassword) { ElMessage.warning('请输入新密码'); return }
  if (form.newPassword.length < 6 || form.newPassword.length > 64) { ElMessage.warning('密码长度为6-64位'); return }
  const hasLetter = /[a-zA-Z]/.test(form.newPassword)
  const hasDigit = /\d/.test(form.newPassword)
  if (!(hasLetter && hasDigit)) { ElMessage.warning('密码必须同时包含字母和数字'); return }
  loading.value = true
  hint.value = '请稍候...'
  try {
    await resetPassword({
      email: form.email.trim(),
      emailCode: form.emailCode.trim(),
      newPassword: form.newPassword
    })
    // 重置成功后不自动登录, 清除本地状态回登录页用新密码登录
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('avatar')
    ElMessage.success('密码重置成功，请用新密码登录')
    router.replace('/login')
  } catch (e) {
    hint.value = e.message || ''
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
