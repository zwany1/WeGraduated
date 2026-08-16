import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, hasPerm, getMenus, loadUserMenus, flattenMenus } from '../utils/perm'

// 无需登录即可访问的白名单页面
const PUBLIC_PAGES = [
  '/', '/home',
  '/login', '/register', '/forgot-password', '/oauth/callback',
  '/features', '/template-market', '/guide', '/cases', '/pricing',
  '/privacy', '/about', '/terms'
]

// 管理端组件映射: 由后端菜单 component 字段决定
const viewMap = {
  'admin/Dashboard': () => import('../views/admin/Dashboard.vue'),
  'admin/UserManage': () => import('../views/admin/UserManage.vue'),
  'admin/TemplateManage': () => import('../views/admin/TemplateManage.vue'),
  'admin/TaskManage': () => import('../views/admin/TaskManage.vue'),
  'admin/RoleManage': () => import('../views/admin/RoleManage.vue'),
  'admin/MenuManage': () => import('../views/admin/MenuManage.vue'),
  'admin/TemplateMarketManage': () => import('../views/admin/TemplateMarketManage.vue'),
  'admin/OperLogManage': () => import('../views/admin/OperLogManage.vue'),
  'admin/LoginLogManage': () => import('../views/admin/LoginLogManage.vue'),
  'admin/DictManage': () => import('../views/admin/DictManage.vue'),
  'admin/ConfigManage': () => import('../views/admin/ConfigManage.vue'),
  'admin/NoticeManage': () => import('../views/admin/NoticeManage.vue'),
  'admin/BackupManage': () => import('../views/admin/BackupManage.vue'),
  'admin/ComingSoon': () => import('../views/admin/ComingSoon.vue')
}

const routes = [
  { path: '/', component: () => import('../views/Welcome.vue') },
  { path: '/home', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/forgot-password', component: () => import('../views/ForgotPassword.vue') },
  { path: '/oauth/callback', component: () => import('../views/OAuthCallback.vue') },
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
  { path: '/terms', component: () => import('../views/Terms.vue') },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: []
  }
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

let lastMenuSignature = null

function menusSignature(menus) {
  return JSON.stringify(flattenMenus(menus || []).map(r => r.path).sort())
}

/** 根据菜单树注册管理端动态子路由; 返回本次是否发生了路由变更 */
export function setupAdminRoutes(menus) {
  const sig = menusSignature(menus)
  if (sig === lastMenuSignature) {
    return false
  }
  // 清空旧子路由
  router.getRoutes()
    .filter(r => r.name && String(r.name).startsWith('Admin_'))
    .forEach(r => router.removeRoute(r.name))
  const flat = flattenMenus(menus)
  flat.forEach(item => {
    const component = viewMap[item.component] || viewMap['admin/ComingSoon']
    router.addRoute('Admin', {
      path: item.path,
      name: 'Admin_' + item.path.replace(/\//g, '_'),
      component,
      meta: { title: item.title, perms: item.perms || [] }
    })
  })
  lastMenuSignature = sig
  return true
}

/** 确保动态路由与当前用户菜单一致; 返回本次是否发生了路由变更 */
export async function ensureAdminRoutes() {
  let menus = getMenus()
  if ((!menus || menus.length === 0) && getToken()) {
    menus = await loadUserMenus()
  }
  return setupAdminRoutes(menus)
}

function firstAdminPath() {
  const flat = flattenMenus(getMenus())
  if (flat.length > 0) {
    return '/admin/' + flat[0].path
  }
  return null
}

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  if (!PUBLIC_PAGES.includes(to.path) && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  const routesChanged = await ensureAdminRoutes()
  if (routesChanged && to.path.startsWith('/admin')) {
    // 动态路由刚注册/变更, 本次 to.matched 尚未包含新路由, 重新导航以正确匹配
    next({ ...to, replace: true })
    return
  }

  if (to.path.startsWith('/admin')) {
    if (to.path === '/admin') {
      next(firstAdminPath() || '/home')
      return
    }
    // 未注册的动态子路由 = 当前用户无该菜单权限
    if (!to.matched.some(r => r.name === 'Admin')) {
      ElMessage.warning('无该页面访问权限')
      next('/home')
      return
    }
  }

  if (to.meta && Array.isArray(to.meta.perms) && to.meta.perms.length) {
    if (!hasPerm(to.meta.perms, true)) {
      ElMessage.warning('无访问权限')
      next('/home')
      return
    }
  }
  next()
})

export default router
