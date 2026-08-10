<template>
  <div class="login-page">
    <div class="form-wrap">
      <form class="form" :class="formClass" @submit.prevent="submit">
        <!-- 邮箱(用户名) -->
        <div class="field email">
          <span class="icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#555" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16v12H4z" /><path d="M4 7l8 6 8-6" /></svg>
          </span>
          <input v-model="form.username" class="input" type="text" placeholder="Username" autocomplete="off" @focus="onFocus" @blur="onBlur" @input="onInput" />
        </div>

        <!-- 密码 -->
        <div class="field password">
          <span class="icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#555" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></svg>
          </span>
          <input v-model="form.password" class="input" :type="showPwd ? 'text' : 'password'" placeholder="Password" @focus="onFocus" @blur="onBlur" @input="onInput" />
          <span class="eye" @mousedown.prevent="showPwd = !showPwd">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#999" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
          </span>
        </div>

        <!-- 记住我 -->
        <label class="remember" @click="rememberMe = !rememberMe">
          <span class="checkbox" :class="{ checked: rememberMe }">
            <svg v-if="rememberMe" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 13l4 4L19 7" /></svg>
          </span>
          记住我
        </label>

        <!-- 提交按钮(3D 立体) -->
        <button class="button" type="submit" :disabled="loading">
          <span class="side-top-bottom"></span>
          <span class="side-left-right"></span>
          {{ loading ? '提交中...' : (tab === 'login' ? 'LOGIN' : 'REGISTER') }}
        </button>

        <small>{{ hint }}</small>
      </form>
    </div>

    <!-- 登录/注册切换 -->
    <div class="tab-switch">
      <span :class="{ active: tab === 'login' }" @click="switchTab('login')">登录</span>
      <span class="divider">|</span>
      <span :class="{ active: tab === 'register' }" @click="switchTab('register')">注册</span>
    </div>
    <div class="back" @click="$router.push('/')">返回首页</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getProfile } from '../api/user'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const showPwd = ref(false)
const rememberMe = ref(false)
const anim = ref('')   // face-up-left / face-up-right / form-complete / form-error
const form = reactive({ username: '', password: '' })

const formClass = computed(() => anim.value)

const hint = computed(() => {
  if (loading.value) return '请稍候...'
  if (tab.value === 'login') return 'Fill in the form to login'
  return 'Create a new account'
})

onMounted(() => {
  // 加载记住的账密
  const savedUser = localStorage.getItem('remembered_user')
  const savedPwd = localStorage.getItem('remembered_pwd')
  if (savedUser) {
    form.username = savedUser
    rememberMe.value = true
    if (savedPwd) {
      try {
        form.password = decodeURIComponent(atob(savedPwd))
      } catch (e) {
        form.password = ''
      }
    }
  }
})

function switchTab(t) {
  tab.value = t
  form.password = ''
  anim.value = ''
}

function onFocus() {
  anim.value = formCompleted() ? 'face-up-right' : 'face-up-left'
}
function onBlur() {
  anim.value = ''
}
function onInput() {
  anim.value = formCompleted() ? 'face-up-right' : 'face-up-left'
}

function formCompleted() {
  return form.username.trim() !== '' && form.password !== ''
}

function saveRemembered() {
  if (rememberMe.value) {
    localStorage.setItem('remembered_user', form.username.trim())
    localStorage.setItem('remembered_pwd', btoa(encodeURIComponent(form.password)))
  } else {
    localStorage.removeItem('remembered_user')
    localStorage.removeItem('remembered_pwd')
  }
}

async function submit() {
  if (loading.value) return
  if (!formCompleted()) {
    anim.value = ''
    ElMessage.warning('请输入用户名和密码')
    // 失败抖动
    setTimeout(() => { anim.value = 'form-error' }, 200)
    setTimeout(() => { anim.value = '' }, 2600)
    return
  }
  loading.value = true
  try {
    const fn = tab.value === 'login' ? login : register
    const data = await fn({ username: form.username.trim(), password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    // 登录后同步资料(昵称/头像), 以数据库为准
    try {
      const p = await getProfile()
      if (p) {
        localStorage.setItem('username', p.nickname || p.username || data.username)
        localStorage.setItem('avatar', p.avatar || '')
      }
    } catch (e) {
      // 资料拉取失败不阻塞登录
    }
    saveRemembered()
    ElMessage.success(tab.value === 'login' ? '登录成功' : '注册成功')
    // 成功翻转动画
    anim.value = 'form-complete'
    setTimeout(() => {
      router.push('/')
    }, 1600)
  } catch (e) {
    anim.value = ''
    setTimeout(() => { anim.value = 'form-error' }, 200)
    setTimeout(() => { anim.value = '' }, 2600)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(45deg, #83a4d4, #b6fbff);
  font-family: "Helvetica Neue", Arial, sans-serif;
}
*,
*::before,
*::after {
  box-sizing: border-box;
}

.form-wrap {
  perspective: 2000px;
  perspective-origin: 50px center;
}

.form {
  position: relative;
  margin: auto;
  width: 400px;
  padding: 20px 30px;
  background: #fff;
  border: 1px solid #dfdfdf;
  transform-style: preserve-3d;
  perspective-origin: 50px center;
  perspective: 2000px;
  transition: transform 1s ease;
}
.form::before,
.form::after {
  content: '';
  position: absolute;
  width: 100%;
  left: 0;
}
.form::before {
  height: 100%;
  top: 0;
  transform: translateZ(-100px);
  background: #333;
  opacity: 0.3;
}
.form::after {
  content: 'SUCCESS!';
  transform: translateY(-50%) translateZ(-101px) scaleX(-1);
  top: 50%;
  color: #fff;
  text-align: center;
  font-weight: bold;
}

.field {
  position: relative;
  background: #cfcfcf;
  transform-style: preserve-3d;
}
.field + .field {
  margin-top: 10px;
}

.icon {
  width: 24px;
  height: 24px;
  position: absolute;
  top: calc(50% - 12px);
  left: 12px;
  transform: translateZ(50px);
  transform-style: preserve-3d;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.input {
  border: 1px solid #dfdfdf;
  background: #fff;
  height: 48px;
  line-height: 48px;
  padding: 0 38px 0 48px;
  width: 100%;
  transform: translateZ(26px);
  font-size: 14px;
}
.input::placeholder {
  color: #aaa;
}
.input:focus,
.input:active {
  outline: none;
  border: 1px solid #e35d5b;
}

.eye {
  position: absolute;
  right: 12px;
  top: calc(50% - 12px);
  transform: translateZ(50px);
  cursor: pointer;
  opacity: 0.7;
  display: flex;
}
.eye:hover {
  opacity: 1;
}

.button {
  display: block;
  width: 100%;
  border: 0;
  text-align: center;
  font-weight: bold;
  color: #fff;
  background: linear-gradient(45deg, #e53935, #e35d5b);
  margin-top: 20px;
  padding: 14px;
  position: relative;
  transform-style: preserve-3d;
  transform: translateZ(26px);
  transition: transform 0.3s ease;
  cursor: pointer;
  font-size: 15px;
  font-family: inherit;
}
.button:hover:not(:disabled) {
  transform: translateZ(13px);
}
.button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.side-top-bottom {
  width: 100%;
}
.side-top-bottom::before,
.side-top-bottom::after {
  content: '';
  width: 100%;
  height: 26px;
  background: linear-gradient(45deg, #d3322e, #d54c4a);
  position: absolute;
  left: 0;
}
.side-top-bottom::before {
  transform-origin: center top;
  transform: translateZ(-26px) rotateX(90deg);
  top: 0;
}
.side-top-bottom::after {
  transform-origin: center bottom;
  transform: translateZ(-26px) rotateX(-90deg);
  bottom: 0;
}
.side-left-right {
  height: 100%;
}
.side-left-right::before,
.side-left-right::after {
  content: '';
  height: 100%;
  width: 26px;
  position: absolute;
  top: 0;
}
.side-left-right::before {
  background: #e53935;
  transform-origin: left center;
  transform: rotateY(90deg);
  left: 0;
}
.side-left-right::after {
  background: #e35d5b;
  transform-origin: right center;
  transform: rotateY(-90deg);
  right: 0;
}

.face-up-left {
  transform: rotateY(-30deg) rotateX(30deg);
}
.face-up-right {
  transform: rotateY(30deg) rotateX(30deg);
}
.face-down-left {
  transform: rotateY(-30deg) rotateX(-30deg);
}
.face-down-right {
  transform: rotateY(30deg) rotateX(-30deg);
}
.form-complete {
  animation: formComplete 1.6s ease;
}
.form-error {
  animation: formError 2.4s ease;
}

@keyframes formComplete {
  50%, 55% {
    transform: rotateX(30deg) rotateY(180deg);
  }
  100% {
    transform: rotateX(0deg) rotateY(1turn);
  }
}
@keyframes formError {
  0%, 100% {
    transform: rotateX(0deg) rotateY(0deg);
  }
  25% {
    transform: rotateX(-25deg);
  }
  33% {
    transform: rotateX(-25deg) rotateY(45deg);
  }
  66% {
    transform: rotateX(-25deg) rotateY(-30deg);
  }
}

small {
  color: #999;
  text-align: center;
  display: block;
  margin-top: 20px;
  backface-visibility: hidden;
}

.remember {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  user-select: none;
  transform: translateZ(26px);
  position: relative;
}
.checkbox {
  width: 15px;
  height: 15px;
  border: 1.5px solid #bbb;
  border-radius: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background: #fff;
}
.checkbox.checked {
  background: #e35d5b;
  border-color: #e35d5b;
}

.tab-switch {
  margin-top: 22px;
  color: #2c3e50;
  font-size: 14px;
  display: flex;
  gap: 12px;
}
.tab-switch span {
  cursor: pointer;
  padding: 2px 4px;
  color: rgba(255, 255, 255, 0.85);
  transition: color 0.2s;
}
.tab-switch span.active {
  font-weight: 700;
  color: #fff;
  text-decoration: underline;
}
.tab-switch .divider {
  cursor: default;
  opacity: 0.6;
}
.back {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
}
.back:hover {
  color: #fff;
}
</style>
