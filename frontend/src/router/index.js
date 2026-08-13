import { createRouter, createWebHistory } from 'vue-router'

// 无需登录即可访问的白名单页面
const PUBLIC_PAGES = [
  '/', '/home',
  '/login', '/register', '/forgot-password',
  '/features', '/template-market', '/guide', '/cases', '/pricing',
  '/privacy', '/about', '/terms'
]

const routes = [
  { path: '/', component: () => import('../views/Welcome.vue') },
  { path: '/home', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/forgot-password', component: () => import('../views/ForgotPassword.vue') },
  { path: '/templates', component: () => import('../views/TemplateList.vue') },
  { path: '/profile', component: () => import('../views/Profile.vue') },
  { path: '/template/:id', component: () => import('../views/TemplateConfig.vue') },
  { path: '/tasks', component: () => import('../views/FormatTask.vue') },
  { path: '/table3', component: () => import('../views/ThreeTable.vue') },
  { path: '/er', component: () => import('../views/ErDiagram.vue') },
  { path: '/system-design', component: () => import('../views/SystemDesign.vue') },
  { path: '/free-draw', component: () => import('../components/FreeDraw.vue') },
  { path: '/features', component: () => import('../views/Features.vue') },
  { path: '/template-market', component: () => import('../views/TemplateMarket.vue') },
  { path: '/guide', component: () => import('../views/Guide.vue') },
  { path: '/cases', component: () => import('../views/Cases.vue') },
  { path: '/pricing', component: () => import('../views/Pricing.vue') },
  { path: '/privacy', component: () => import('../views/Privacy.vue') },
  { path: '/about', component: () => import('../views/About.vue') },
  { path: '/terms', component: () => import('../views/Terms.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, top: 80 }
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!PUBLIC_PAGES.includes(to.path) && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
