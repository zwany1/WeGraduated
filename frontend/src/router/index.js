import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, hasPerm, getMenus, loadUserMenus, flattenMenus } from '../utils/perm'

// 无需登录即可访问的白名单页面
const PUBLIC_PAGES = [
  '/', '/home',
  '/login', '/register', '/forgot-password',
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
  'admin/FeedbackManage': () => import('../views/admin/FeedbackManage.vue'),
  'admin/ComingSoon': () => import('../views/admin/ComingSoon.vue')
}

const routes = [
  { path: '/', component: () => import('../views/Welcome.vue'), meta: { title: '论文格式助手' } },
  { path: '/home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  { path: '/forgot-password', component: () => import('../views/ForgotPassword.vue'), meta: { title: '找回密码' } },
  { path: '/templates', component: () => import('../views/TemplateList.vue'), meta: { title: '我的格式方案' } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人资料' } },
  { path: '/template/:id', component: () => import('../views/TemplateConfig.vue'), meta: { title: '格式方案配置' } },
  { path: '/tasks', component: () => import('../views/FormatTask.vue'), meta: { title: '排版任务' } },
  { path: '/team', component: () => import('../views/TeamManage.vue'), meta: { title: '团队协作' } },
  { path: '/table3', component: () => import('../views/ThreeTable.vue'), meta: { title: '三线表' } },
  { path: '/er', component: () => import('../views/ErDiagram.vue'), meta: { title: 'ER 图' } },
  { path: '/system-design', component: () => import('../views/SystemDesign.vue'), meta: { title: '系统图设计' } },
  { path: '/free-draw', component: () => import('../components/FreeDraw.vue'), meta: { title: '自由绘画' } },
  { path: '/features', component: () => import('../views/Features.vue'), meta: { title: '功能特性' } },
  { path: '/template-market', component: () => import('../views/TemplateMarket.vue'), meta: { title: '模板市场' } },
  { path: '/guide', component: () => import('../views/Guide.vue'), meta: { title: '使用指南' } },
  { path: '/cases', component: () => import('../views/Cases.vue'), meta: { title: '案例' } },
  { path: '/pricing', component: () => import('../views/Pricing.vue'), meta: { title: '价格' } },
  { path: '/privacy', component: () => import('../views/Privacy.vue'), meta: { title: '隐私政策' } },
  { path: '/about', component: () => import('../views/About.vue'), meta: { title: '关于我们' } },
  { path: '/terms', component: () => import('../views/Terms.vue'), meta: { title: '服务条款' } },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true, title: '管理后台' },
    children: []
  },
  { path: '/:pathMatch(.*)*', component: () => import('../views/NotFound.vue'), meta: { title: '页面不存在' } }
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

router.afterEach((to) => {
  if (to.meta?.title) document.title = to.meta.title
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

// 前端权限校验仅用于交互引导, 越权访问由服务端 @RequiresPerms 兜底拦截
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
