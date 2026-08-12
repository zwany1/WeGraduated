import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Welcome.vue') },
  { path: '/home', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/forgot-password', component: () => import('../views/ForgotPassword.vue') },
  { path: '/templates', component: () => import('../views/TemplateList.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/template/:id', component: () => import('../views/TemplateConfig.vue'), meta: { requiresAuth: true } },
  { path: '/tasks', component: () => import('../views/FormatTask.vue'), meta: { requiresAuth: true } },
  { path: '/table3', component: () => import('../views/ThreeTable.vue'), meta: { requiresAuth: true } },
  { path: '/er', component: () => import('../views/ErDiagram.vue'), meta: { requiresAuth: true } },
  { path: '/system-design', component: () => import('../views/SystemDesign.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
