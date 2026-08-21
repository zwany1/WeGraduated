<template>
  <div class="wrapper">
    <svg class="parallax" viewBox="0 0 750 500" preserveAspectRatio="xMidYMax slice" xmlns="http://www.w3.org/2000/svg" v-html="sceneSvg"></svg>
    <div class="scrollElement"></div>
    <div class="scroll-hint">
      <div class="mouse">
        <div class="wheel"></div>
      </div>
      <span class="hint-arrow">↓</span>
    </div>
    <div class="scroll-back" :class="{ show: showLogin }" @click="goHome">
      <span class="sb-arrow">↓</span>
      <span class="sb-text">进入首页</span>
    </div>
    <transition name="slide-up">
      <div v-if="showLoginPanel" class="login-overlay" @click.self="showLoginPanel = false">
        <div class="login-card">
          <button class="close-btn" @click="showLoginPanel = false">×</button>
          <div class="tabs">
            <span :class="{ active: tab === 'login' }" @click="switchTab('login')">登录</span>
            <span class="divider">|</span>
            <span :class="{ active: tab === 'register' }" @click="switchTab('register')">注册</span>
          </div>
          <div class="field">
            <input v-model="form.username" type="text" placeholder="用户名(3-32位)" @keyup.enter="focusPwd" />
          </div>
          <div class="field">
            <input ref="pwdRef" v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="密码(6位以上)" @keyup.enter="submit" />
            <span class="eye" @click="showPwd = !showPwd">👁</span>
          </div>
          <label class="remember" @click="rememberMe = !rememberMe">
            <span class="checkbox" :class="{ checked: rememberMe }"></span>
            记住我
          </label>
          <button class="submit-btn" :disabled="loading" @click="submit">
            {{ loading ? '提交中...' : (tab === 'login' ? '登 录' : '注 册') }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getProfile } from '../api/user'
import { getMenus } from '../api/admin'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()
const sceneSvg = ref('')
const showLogin = ref(false)
const showLoginPanel = ref(false)
const tab = ref('login')
const loading = ref(false)
const showPwd = ref(false)
const rememberMe = ref(false)
const pwdRef = ref(null)
const form = reactive({ username: '', password: '' })

let ctx = null

onMounted(async () => {
  if (localStorage.getItem('token')) {
    router.replace('/home')
    return
  }
  const savedUser = localStorage.getItem('remembered_user')
  localStorage.removeItem('remembered_pwd')
  if (savedUser) {
    form.username = savedUser
    rememberMe.value = true
  }
  try {
    const resp = await fetch('/welcome-scene.svg')
    let svgStr = await resp.text()
    // 提取 <svg> 内部内容, 注入到模板的 svg 容器内(避免嵌套 svg)
    const m = svgStr.match(/<svg[^>]*>([\s\S]*)<\/svg>/i)
    sceneSvg.value = m ? m[1] : svgStr
  } catch (e) {}
  initParallax()
})

onBeforeUnmount(() => {
  if (ctx) ctx.revert()
})

function initParallax() {
  setTimeout(() => {
    const svg = document.querySelector('svg.parallax')
    if (!svg) return
    const speed = 100
    const height = svg.getBBox().height
    ctx = gsap.context(() => {
      gsap.set('#h2-1', { opacity: 0 })
      gsap.set('#bg_grad', { attr: { cy: '-50' } })
      gsap.set(['#dinoL', '#dinoR'], { y: 80 })
      gsap.set('#dinoL', { x: -10 })

      let scene1 = gsap.timeline()
      ScrollTrigger.create({ animation: scene1, trigger: '.scrollElement', start: 'top top', end: '45% 100%', scrub: 3 })
      scene1.to('#h1-1', { y: 3 * speed, x: 1 * speed, scale: 0.9, ease: 'power1.in' }, 0)
      scene1.to('#h1-2', { y: 2.6 * speed, x: -0.6 * speed, ease: 'power1.in' }, 0)
      scene1.to('#h1-3', { y: 1.7 * speed, x: 1.2 * speed }, 0.03)
      scene1.to('#h1-4', { y: 3 * speed, x: 1 * speed }, 0.03)
      scene1.to('#h1-5', { y: 2 * speed, x: 1 * speed }, 0.03)
      scene1.to('#h1-6', { y: 2.3 * speed, x: -2.5 * speed }, 0)
      scene1.to('#h1-7', { y: 5 * speed, x: 1.6 * speed }, 0)
      scene1.to('#h1-8', { y: 3.5 * speed, x: 0.2 * speed }, 0)
      scene1.to('#h1-9', { y: 3.5 * speed, x: -0.2 * speed }, 0)
      scene1.to('#cloudsBig-L', { y: 4.5 * speed, x: -0.2 * speed }, 0)
      scene1.to('#cloudsBig-R', { y: 4.5 * speed, x: -0.2 * speed }, 0)
      scene1.to('#cloudStart-L', { x: -300 }, 0)
      scene1.to('#cloudStart-R', { x: 300 }, 0)
      scene1.to('#info', { y: 8 * speed }, 0)

      gsap.fromTo('#bird', { opacity: 1 }, {
        y: -250, x: 800, ease: 'power2.out',
        scrollTrigger: {
          trigger: '.scrollElement', start: '15% top', end: '60% 100%', scrub: 4,
          onEnter: () => gsap.to('#bird', { scaleX: 1, rotation: 0 }),
          onLeave: () => gsap.to('#bird', { scaleX: -1, rotation: -15 })
        }
      })

      let clouds = gsap.timeline()
      ScrollTrigger.create({ animation: clouds, trigger: '.scrollElement', start: 'top top', end: '70% 100%', scrub: 1 })
      clouds.to('#cloud1', { x: 500 }, 0)
      clouds.to('#cloud2', { x: 1000 }, 0)
      clouds.to('#cloud3', { x: -1000 }, 0)
      clouds.to('#cloud4', { x: -700, y: 25 }, 0)

      let sun = gsap.timeline()
      ScrollTrigger.create({ animation: sun, trigger: '.scrollElement', start: '1% top', end: '2150 100%', scrub: 2 })
      sun.fromTo('#bg_grad', { attr: { cy: '-50' } }, { attr: { cy: '330' } }, 0)
      sun.to('#bg_grad stop:nth-child(2)', { attr: { offset: '0.15' } }, 0)
      sun.to('#bg_grad stop:nth-child(3)', { attr: { offset: '0.18' } }, 0)
      sun.to('#bg_grad stop:nth-child(4)', { attr: { offset: '0.25' } }, 0)
      sun.to('#bg_grad stop:nth-child(5)', { attr: { offset: '0.46' } }, 0)
      sun.to('#bg_grad stop:nth-child(6)', { attr: { 'stop-color': '#FF9171' } }, 0)

      let scene2 = gsap.timeline()
      ScrollTrigger.create({ animation: scene2, trigger: '.scrollElement', start: '15% top', end: '40% 100%', scrub: 3 })
      scene2.fromTo('#h2-1', { y: 500, opacity: 0 }, { y: 0, opacity: 1 }, 0)
      scene2.fromTo('#h2-2', { y: 500 }, { y: 0 }, 0.1)
      scene2.fromTo('#h2-3', { y: 700 }, { y: 0 }, 0.1)
      scene2.fromTo('#h2-4', { y: 700 }, { y: 0 }, 0.2)
      scene2.fromTo('#h2-5', { y: 800 }, { y: 0 }, 0.3)
      scene2.fromTo('#h2-6', { y: 900 }, { y: 0 }, 0.3)

      gsap.set('#bats', { transformOrigin: '50% 50%' })
      gsap.fromTo('#bats', { opacity: 1, y: 400, scale: 0 }, {
        y: 20, scale: 0.8, ease: 'power3.out',
        scrollTrigger: {
          trigger: '.scrollElement', start: '40% top', end: '70% 100%', scrub: 3,
          onEnter: function () {
            gsap.utils.toArray('#bats path').forEach((item, i) => {
              gsap.to(item, { scaleX: 0.5, yoyo: true, repeat: 9, transformOrigin: '50% 50%', duration: 0.15, delay: 0.7 + i / 10 })
            })
            gsap.set('#bats', { opacity: 1 })
          }
        }
      })

      let sun2 = gsap.timeline()
      ScrollTrigger.create({ animation: sun2, trigger: '.scrollElement', start: '2000 top', end: '5000 100%', scrub: 2 })
      sun2.to('#sun', { attr: { offset: '1.4' } }, 0)
      sun2.to('#bg_grad stop:nth-child(2)', { attr: { offset: '0.7' } }, 0)
      sun2.to('#sun', { attr: { 'stop-color': '#ffff00' } }, 0)
      sun2.to('#lg4 stop:nth-child(1)', { attr: { 'stop-color': '#623951' } }, 0)
      sun2.to('#lg4 stop:nth-child(2)', { attr: { 'stop-color': '#261F36' } }, 0)
      sun2.to('#bg_grad stop:nth-child(6)', { attr: { 'stop-color': '#45224A' } }, 0)

      gsap.set('#scene3', { y: height - 40, visibility: 'visible' })
      let sceneTransition = gsap.timeline()
      ScrollTrigger.create({ animation: sceneTransition, trigger: '.scrollElement', start: '60% top', end: 'bottom 100%', scrub: 3 })
      sceneTransition.to('#h2-1', { y: -height - 100, scale: 1.5, transformOrigin: '50% 50%' }, 0)
      sceneTransition.to('#bg_grad', { attr: { cy: '-80' } }, 0)
      sceneTransition.to('#bg2', { y: 0 }, 0)

      let scene3 = gsap.timeline()
      ScrollTrigger.create({ animation: scene3, trigger: '.scrollElement', start: '70% 50%', end: 'bottom 100%', scrub: 3 })
      scene3.fromTo('#h3-1', { y: 300 }, { y: -550 }, 0)
      scene3.fromTo('#h3-2', { y: 800 }, { y: -550 }, 0.03)
      scene3.fromTo('#h3-3', { y: 600 }, { y: -550 }, 0.06)
      scene3.fromTo('#h3-4', { y: 800 }, { y: -550 }, 0.09)
      scene3.fromTo('#h3-5', { y: 1000 }, { y: -550 }, 0.12)
      scene3.fromTo('#stars', { opacity: 0 }, { opacity: 0.5, y: -500 }, 0)
      scene3.fromTo('#arrow2', { opacity: 0 }, { opacity: 0.7, y: -710 }, 0.25)
      scene3.fromTo('#text2', { opacity: 0 }, { opacity: 0.7, y: -710 }, 0.3)
      scene3.to('#bg2-grad', { attr: { cy: 600 } }, 0)
      scene3.to('#bg2-grad', { attr: { r: 500 } }, 0)

      gsap.set('#fstar', { y: -400 })
      let fstarTL = gsap.timeline()
      ScrollTrigger.create({
        animation: fstarTL, trigger: '.scrollElement', start: '4200 top', end: '6000 bottom', scrub: 2,
        onEnter: () => gsap.set('#fstar', { opacity: 1 }),
        onLeave: () => gsap.set('#fstar', { opacity: 0 })
      })
      fstarTL.to('#fstar', { x: -700, y: -250, ease: 'power2.out' }, 0)

      gsap.utils.toArray('#stars path').slice(0, 48).forEach((el, i) => {
        const delay = [0.8, 1.8, 1, 1.2, 0.5, 2, 1.1, 1.4, 1.1, 0.9, 1.3, 2, 0.8, 1.8, 1][i % 15]
        gsap.fromTo(el, { opacity: 0.3 }, { opacity: 1, duration: 0.3, repeat: -1, repeatDelay: delay })
      })

      ScrollTrigger.create({
        trigger: '.scrollElement',
        start: 'bottom 110%',
        onEnter: () => { showLogin.value = true },
        onLeaveBack: () => { showLogin.value = false }
      })
      // 滚动提示: 滚动后淡出
      gsap.to('.scroll-hint', {
        opacity: 0,
        ease: 'none',
        scrollTrigger: { trigger: '.scrollElement', start: 'top top', end: '15% top', scrub: true }
      })
    })
  }, 300)
}

function switchTab(t) {
  tab.value = t
  form.password = ''
}
function focusPwd() {
  pwdRef.value && pwdRef.value.focus()
}

function goHome() {
  router.push('/home')
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim()) { ElMessage.warning('请输入用户名'); return }
  if (!form.password) { ElMessage.warning('请输入密码'); return }
  loading.value = true
  try {
    const fn = tab.value === 'login' ? login : register
    const data = await fn({ username: form.username.trim(), password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role || 'USER')
    localStorage.setItem('roles', JSON.stringify(data.roles || []))
    localStorage.setItem('perms', JSON.stringify(data.perms || []))
    try {
      const p = await getProfile()
      if (p) {
        localStorage.setItem('username', p.nickname || p.username || data.username)
        localStorage.setItem('avatar', p.avatar || '')
      }
    } catch (e) {}
    try {
      const menus = await getMenus()
      localStorage.setItem('menus', JSON.stringify(menus || []))
    } catch (e) {
      localStorage.removeItem('menus')
    }
    if (rememberMe.value) {
      localStorage.setItem('remembered_user', form.username.trim())
    } else {
      localStorage.removeItem('remembered_user')
    }
    ElMessage.success(tab.value === 'login' ? '登录成功' : '注册成功')
    router.push('/home')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrapper { position: relative; }
svg.parallax {
  display: block;
  width: 100%;
  height: 100vh;
  position: fixed;
  z-index: 3;
  top: 0;
  left: 0;
}
.scrollElement { position: absolute; height: 6000px; width: 100%; top: 0; z-index: 4; }
.scroll-hint {
  position: fixed;
  bottom: 6%;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  z-index: 12;
  pointer-events: none;
}
.mouse {
  width: 26px;
  height: 42px;
  border: 2px solid #fff;
  border-radius: 14px;
  display: flex;
  justify-content: center;
  padding-top: 7px;
  box-sizing: border-box;
}
.wheel {
  width: 4px;
  height: 8px;
  border-radius: 2px;
  background: #fff;
  animation: wheelMove 1.5s ease-in-out infinite;
}
.hint-arrow {
  color: #fff;
  font-size: 14px;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0,0,0,0.4);
  animation: arrowBounce 1.5s ease-in-out infinite;
}
@keyframes wheelMove {
  0% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(12px); }
}
@keyframes arrowBounce {
  0%, 100% { transform: translateY(0); opacity: 0.8; }
  50% { transform: translateY(4px); opacity: 1; }
}
.scroll-back {
  position: fixed;
  bottom: 8%;
  left: 50%;
  transform: translateX(-50%) translateY(30px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: transparent;
  border: none;
  color: #fff;
  font-family: Verdana, Geneva, Tahoma, sans-serif;
  cursor: pointer;
  transition: opacity 0.4s, transform 0.4s;
  z-index: 11;
  opacity: 0;
  pointer-events: none;
  text-shadow: 0 1px 3px rgba(0,0,0,0.5);
}
.scroll-back.show {
  opacity: 0.75;
  pointer-events: auto;
  transform: translateX(-50%) translateY(0);
}
.scroll-back:hover { opacity: 1; }
.sb-arrow {
  font-size: 30px;
  line-height: 1;
  animation: bob 1.6s infinite;
}
.sb-text {
  font-size: 16px;
  letter-spacing: 6px;
  font-weight: 600;
}
@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(6px); }
}
.login-overlay {
  position: fixed; inset: 0; z-index: 100; background: rgba(20,22,40,0.75);
  display: flex; align-items: center; justify-content: center;
}
.login-card {
  width: 360px; background: #fff; border-radius: 16px; padding: 32px;
  box-shadow: 0 16px 50px rgba(0,0,0,0.3); position: relative;
}
.close-btn { position: absolute; top: 12px; right: 16px; border: none; background: none; font-size: 22px; color: #999; cursor: pointer; }
.close-btn:hover { color: #333; }
.tabs { display: flex; justify-content: center; gap: 16px; margin-bottom: 24px; font-size: 15px; }
.tabs span { cursor: pointer; color: #909399; }
.tabs span.active { color: #3B6BFF; font-weight: 700; }
.tabs .divider { cursor: default; color: #d0d4da; }
.field { position: relative; margin-bottom: 14px; }
.field input {
  width: 100%; height: 44px; border: 1.5px solid #e0e4ea; border-radius: 8px;
  padding: 0 40px 0 14px; font-size: 14px; box-sizing: border-box; outline: none; transition: border-color 0.2s;
}
.field input:focus { border-color: #3B6BFF; }
.eye { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); cursor: pointer; opacity: 0.6; }
.remember { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #666; cursor: pointer; margin-bottom: 18px; user-select: none; }
.checkbox { width: 16px; height: 16px; border: 1.5px solid #c0c6ce; border-radius: 4px; transition: all 0.2s; }
.checkbox.checked { background: #3B6BFF; border-color: #3B6BFF; }
.submit-btn {
  width: 100%; height: 46px; border: none; border-radius: 8px;
  background: linear-gradient(45deg, #3B6BFF, #6a8bff); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer; transition: opacity 0.2s;
}
.submit-btn:hover:not(:disabled) { filter: brightness(1.05); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.slide-up-enter-active, .slide-up-leave-active { transition: opacity 0.3s, transform 0.3s; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(40px); }
</style>
