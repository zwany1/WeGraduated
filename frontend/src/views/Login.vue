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

        <!-- Tabs -->
        <div class="flex justify-center gap-4 mb-8 text-sm">
          <span :class="['cursor-pointer', tab === 'login' ? 'text-primary font-semibold' : 'text-muted-foreground']" @click="switchTab('login')">登录</span>
          <span class="text-muted-foreground/40">|</span>
          <span :class="['cursor-pointer', tab === 'register' ? 'text-primary font-semibold' : 'text-muted-foreground']" @click="switchTab('register')">注册</span>
        </div>

        <!-- Login Form -->
        <form @submit.prevent="submit" class="space-y-5">
          <div class="space-y-2">
            <label class="text-sm font-medium">Email / 用户名</label>
            <input
              v-model="form.username"
              type="text"
              placeholder="you@example.com"
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
            :text="loading ? '...' : (tab === 'login' ? 'Log in' : 'Register')"
            class="w-full h-12 text-base font-medium"
            :disabled="loading"
          />

          <p class="text-center text-sm text-muted-foreground min-h-5">{{ hint }}</p>
        </form>

        <div class="text-center text-sm text-muted-foreground mt-8">
          还没有账号？
          <span class="text-foreground font-medium hover:underline cursor-pointer" @click="switchTab('register')">Sign Up</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getProfile } from '../api/user'

// ===== Pupil (照抄 careercompass animated-characters.tsx) =====
const Pupil = defineComponent({
  props: {
    size: { type: Number, default: 12 },
    maxDistance: { type: Number, default: 5 },
    pupilColor: { type: String, default: 'black' },
    forceLookX: { type: Number, default: null },
    forceLookY: { type: Number, default: null }
  },
  setup(props) {
    const pupilRef = ref(null)
    const mouse = reactive({ x: 0, y: 0 })
    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))
    const pos = computed(() => {
      if (props.forceLookX != null && props.forceLookY != null) {
        return { x: props.forceLookX, y: props.forceLookY }
      }
      const el = pupilRef.value
      if (!el) return { x: 0, y: 0 }
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      const dist = Math.min(Math.sqrt(dx * dx + dy * dy), props.maxDistance)
      const angle = Math.atan2(dy, dx)
      return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
    })
    return () => h('div', {
      ref: pupilRef,
      class: 'rounded-full',
      style: {
        width: `${props.size}px`, height: `${props.size}px`,
        backgroundColor: props.pupilColor,
        transform: `translate(${pos.value.x}px, ${pos.value.y}px)`,
        transition: 'transform 0.1s ease-out'
      }
    })
  }
})

// ===== EyeBall (照抄 careercompass) =====
const EyeBall = defineComponent({
  props: {
    size: { type: Number, default: 48 },
    pupilSize: { type: Number, default: 16 },
    maxDistance: { type: Number, default: 10 },
    eyeColor: { type: String, default: 'white' },
    pupilColor: { type: String, default: 'black' },
    isBlinking: { type: Boolean, default: false },
    forceLookX: { type: Number, default: null },
    forceLookY: { type: Number, default: null }
  },
  setup(props) {
    const eyeRef = ref(null)
    const mouse = reactive({ x: 0, y: 0 })
    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))
    const pos = computed(() => {
      if (props.forceLookX != null && props.forceLookY != null) {
        return { x: props.forceLookX, y: props.forceLookY }
      }
      const el = eyeRef.value
      if (!el) return { x: 0, y: 0 }
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      const dist = Math.min(Math.sqrt(dx * dx + dy * dy), props.maxDistance)
      const angle = Math.atan2(dy, dx)
      return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
    })
    return () => h('div', {
      ref: eyeRef,
      class: 'rounded-full flex items-center justify-center transition-all duration-150',
      style: {
        width: `${props.size}px`,
        height: props.isBlinking ? '2px' : `${props.size}px`,
        backgroundColor: props.eyeColor,
        overflow: 'hidden'
      }
    }, !props.isBlinking ? [h('div', {
      class: 'rounded-full',
      style: {
        width: `${props.pupilSize}px`, height: `${props.pupilSize}px`,
        backgroundColor: props.pupilColor,
        transform: `translate(${pos.value.x}px, ${pos.value.y}px)`,
        transition: 'transform 0.1s ease-out'
      }
    })] : [])
  }
})

// ===== AnimatedCharacters (照抄 careercompass) =====
const AnimatedCharacters = defineComponent({
  name: 'AnimatedCharacters',
  props: {
    isTyping: { type: Boolean, default: false },
    showPassword: { type: Boolean, default: false },
    passwordLength: { type: Number, default: 0 }
  },
  setup(props) {
    const mouse = reactive({ x: 0, y: 0 })
    const isPurpleBlinking = ref(false)
    const isBlackBlinking = ref(false)
    const isLookingAtEachOther = ref(false)
    const isPurplePeeking = ref(false)
    const purpleRef = ref(null)
    const blackRef = ref(null)
    const yellowRef = ref(null)
    const orangeRef = ref(null)

    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))

    // Blinking - purple
    onMounted(() => {
      const schedule = () => setTimeout(() => {
        isPurpleBlinking.value = true
        setTimeout(() => { isPurpleBlinking.value = false; schedule() }, 150)
      }, Math.random() * 4000 + 3000)
      schedule()
    })
    // Blinking - black
    onMounted(() => {
      const schedule = () => setTimeout(() => {
        isBlackBlinking.value = true
        setTimeout(() => { isBlackBlinking.value = false; schedule() }, 150)
      }, Math.random() * 4000 + 3000)
      schedule()
    })

    // Looking at each other when typing
    let lookTimer = null
    watch(() => props.isTyping, (v) => {
      if (v) {
        isLookingAtEachOther.value = true
        clearTimeout(lookTimer)
        lookTimer = setTimeout(() => { isLookingAtEachOther.value = false }, 800)
      } else {
        isLookingAtEachOther.value = false
      }
    })

    // Purple peeking when password visible
    let peekTimer = null
    watch([() => props.passwordLength, () => props.showPassword, isPurplePeeking], () => {
      if (props.passwordLength > 0 && props.showPassword) {
        clearTimeout(peekTimer)
        peekTimer = setTimeout(() => {
          isPurplePeeking.value = true
          setTimeout(() => { isPurplePeeking.value = false }, 800)
        }, Math.random() * 3000 + 2000)
      } else {
        isPurplePeeking.value = false
        clearTimeout(peekTimer)
      }
    })

    const calcPos = (refEl) => {
      if (!refEl.value) return { faceX: 0, faceY: 0, bodySkew: 0 }
      const r = refEl.value.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 3
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      return {
        faceX: Math.max(-15, Math.min(15, dx / 20)),
        faceY: Math.max(-10, Math.min(10, dy / 30)),
        bodySkew: Math.max(-6, Math.min(6, -dx / 120))
      }
    }

    const purplePos = computed(() => calcPos(purpleRef))
    const blackPos = computed(() => calcPos(blackRef))
    const yellowPos = computed(() => calcPos(yellowRef))
    const orangePos = computed(() => calcPos(orangeRef))

    const isHidingPassword = computed(() => props.passwordLength > 0 && !props.showPassword)
    const pwdVisible = computed(() => props.passwordLength > 0 && props.showPassword)

    const purpleStyle = computed(() => ({
      left: '70px', width: '180px',
      height: (props.isTyping || isHidingPassword.value) ? '440px' : '400px',
      backgroundColor: '#6C3FF5', borderRadius: '10px 10px 0 0', zIndex: 1,
      transform: pwdVisible.value ? 'skewX(0deg)'
        : (props.isTyping || isHidingPassword.value)
          ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
          : `skewX(${purplePos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const purpleEyesStyle = computed(() => ({
      left: pwdVisible.value ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + purplePos.value.faceX}px`,
      top: pwdVisible.value ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + purplePos.value.faceY}px`
    }))
    const purpleLookX = computed(() => pwdVisible.value ? (isPurplePeeking.value ? 4 : -4) : isLookingAtEachOther.value ? 3 : null)
    const purpleLookY = computed(() => pwdVisible.value ? (isPurplePeeking.value ? 5 : -4) : isLookingAtEachOther.value ? 4 : null)

    const blackStyle = computed(() => ({
      left: '240px', width: '120px', height: '310px',
      backgroundColor: '#2D2D2D', borderRadius: '8px 8px 0 0', zIndex: 2,
      transform: pwdVisible.value ? 'skewX(0deg)'
        : isLookingAtEachOther.value ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
          : (props.isTyping || isHidingPassword.value) ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
            : `skewX(${blackPos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const blackEyesStyle = computed(() => ({
      left: pwdVisible.value ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + blackPos.value.faceX}px`,
      top: pwdVisible.value ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + blackPos.value.faceY}px`
    }))
    const blackLookX = computed(() => pwdVisible.value ? -4 : isLookingAtEachOther.value ? 0 : null)
    const blackLookY = computed(() => pwdVisible.value ? -4 : isLookingAtEachOther.value ? -4 : null)

    const orangeStyle = computed(() => ({
      left: '0px', width: '240px', height: '200px', zIndex: 3,
      backgroundColor: '#FF9B6B', borderRadius: '120px 120px 0 0',
      transform: pwdVisible.value ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const orangeEyesStyle = computed(() => ({
      left: pwdVisible.value ? '50px' : `${82 + (orangePos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '85px' : `${90 + (orangePos.value.faceY || 0)}px`
    }))
    const orangeLookX = computed(() => pwdVisible.value ? -5 : null)
    const orangeLookY = computed(() => pwdVisible.value ? -4 : null)

    const yellowStyle = computed(() => ({
      left: '310px', width: '140px', height: '230px',
      backgroundColor: '#E8D754', borderRadius: '70px 70px 0 0', zIndex: 4,
      transform: pwdVisible.value ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const yellowEyesStyle = computed(() => ({
      left: pwdVisible.value ? '20px' : `${52 + (yellowPos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '35px' : `${40 + (yellowPos.value.faceY || 0)}px`
    }))
    const yellowLookX = computed(() => pwdVisible.value ? -5 : null)
    const yellowLookY = computed(() => pwdVisible.value ? -4 : null)
    const yellowMouthStyle = computed(() => ({
      left: pwdVisible.value ? '10px' : `${40 + (yellowPos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '88px' : `${88 + (yellowPos.value.faceY || 0)}px`
    }))

    const eyeBall = (s, ps, md, blink, lx, ly) => h(EyeBall, {
      size: s, pupilSize: ps, maxDistance: md, eyeColor: 'white', pupilColor: '#2D2D2D',
      isBlinking: blink, forceLookX: lx, forceLookY: ly
    })
    const pupil = (lx, ly) => h(Pupil, { size: 12, maxDistance: 5, pupilColor: '#2D2D2D', forceLookX: lx, forceLookY: ly })

    return () => h('div', { class: 'relative', style: { width: '550px', height: '400px' } }, [
      // Purple
      h('div', { ref: purpleRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: purpleStyle.value }, [
        h('div', { class: 'absolute flex gap-8 transition-all duration-700 ease-in-out', style: purpleEyesStyle.value }, [
          eyeBall(18, 7, 5, isPurpleBlinking.value, purpleLookX.value, purpleLookY.value),
          eyeBall(18, 7, 5, isPurpleBlinking.value, purpleLookX.value, purpleLookY.value)
        ])
      ]),
      // Black
      h('div', { ref: blackRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: blackStyle.value }, [
        h('div', { class: 'absolute flex gap-6 transition-all duration-700 ease-in-out', style: blackEyesStyle.value }, [
          eyeBall(16, 6, 4, isBlackBlinking.value, blackLookX.value, blackLookY.value),
          eyeBall(16, 6, 4, isBlackBlinking.value, blackLookX.value, blackLookY.value)
        ])
      ]),
      // Orange
      h('div', { ref: orangeRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: orangeStyle.value }, [
        h('div', { class: 'absolute flex gap-8 transition-all duration-200 ease-out', style: orangeEyesStyle.value }, [
          pupil(orangeLookX.value, orangeLookY.value),
          pupil(orangeLookX.value, orangeLookY.value)
        ])
      ]),
      // Yellow
      h('div', { ref: yellowRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: yellowStyle.value }, [
        h('div', { class: 'absolute flex gap-6 transition-all duration-200 ease-out', style: yellowEyesStyle.value }, [
          pupil(yellowLookX.value, yellowLookY.value),
          pupil(yellowLookX.value, yellowLookY.value)
        ]),
        h('div', { class: 'absolute w-20 h-[4px] bg-[#2D2D2D] rounded-full transition-all duration-200 ease-out', style: yellowMouthStyle.value })
      ])
    ])
  }
})

// ===== InteractiveHoverButton (照抄 careercompass) =====
const InteractiveHoverButton = defineComponent({
  props: {
    type: { type: String, default: 'button' },
    text: { type: String, default: 'Button' },
    disabled: { type: Boolean, default: false }
  },
  setup(props, { attrs }) {
    return () => h('button', {
      type: props.type,
      disabled: props.disabled,
      class: ['group relative w-full cursor-pointer overflow-hidden rounded-full border bg-background px-6 py-2 text-center font-semibold', attrs.class],
      style: 'height: 48px'
    }, [
      h('span', { class: 'inline-block transition-all duration-300 group-hover:translate-x-12 group-hover:opacity-0' }, props.text),
      h('div', { class: 'absolute inset-0 z-10 flex items-center justify-center gap-2 bg-primary text-primary-foreground opacity-0 transition-all duration-300 group-hover:opacity-100 rounded-full' }, [
        h('span', {}, props.text),
        h('svg', { viewBox: '0 0 24 24', class: 'h-4 w-4', fill: 'none', stroke: 'currentColor', strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round' }, [h('path', { d: 'M5 12h14M12 5l7 7-7 7' })])
      ])
    ])
  }
})

// ===== 登录页状态 =====
const router = useRouter()
const tab = ref('login')
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

function switchTab(t) {
  tab.value = t
  form.password = ''
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim()) { ElMessage.warning('请输入用户名'); return }
  if (!form.password) { ElMessage.warning('请输入密码'); return }
  loading.value = true
  hint.value = '请稍候...'
  try {
    const fn = tab.value === 'login' ? login : register
    const data = await fn({ username: form.username.trim(), password: form.password })
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
    ElMessage.success(tab.value === 'login' ? '登录成功' : '注册成功')
    router.push('/home')
  } catch (e) {
  } finally {
    loading.value = false
    hint.value = ''
  }
}
</script>
