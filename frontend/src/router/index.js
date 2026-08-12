import { createRouter, createWebHistory } from 'vue-router'

// 无需登录即可访问的白名单页面
const PUBLIC_PAGES = ['/login', '/register', '/forgot-password']

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
  { path: '/system-design', component: () => import('../views/SystemDesign.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
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
