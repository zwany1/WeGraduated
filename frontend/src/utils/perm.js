import { getMenus as fetchMenusApi } from '../api/admin'

// ============ 登录态持久化 ============

const KEYS = ['token', 'userId', 'username', 'avatar', 'role', 'roles', 'perms', 'menus']

export function setAuthData(data) {
  localStorage.setItem('token', data.token || '')
  if (data.userId != null) localStorage.setItem('userId', data.userId)
  if (data.username) localStorage.setItem('username', data.username)
  localStorage.setItem('role', data.role || 'USER')
  localStorage.setItem('roles', JSON.stringify(data.roles || []))
  localStorage.setItem('perms', JSON.stringify(data.perms || []))
}

export function setAuthProfile({ nickname, avatar }) {
  if (nickname) localStorage.setItem('username', nickname)
  if (avatar !== undefined) localStorage.setItem('avatar', avatar || '')
}

export function clearAuth() {
  KEYS.forEach(k => localStorage.removeItem(k))
}

// ============ 读取 ============

export function getToken() {
  return localStorage.getItem('token') || ''
}

export function getRoles() {
  try {
    return JSON.parse(localStorage.getItem('roles') || '[]') || []
  } catch (e) {
    return []
  }
}

export function getPerms() {
  try {
    return JSON.parse(localStorage.getItem('perms') || '[]') || []
  } catch (e) {
    return []
  }
}

export function getMenus() {
  try {
    return JSON.parse(localStorage.getItem('menus') || '[]') || []
  } catch (e) {
    return []
  }
}

export function hasRole(roleKey) {
  return getRoles().includes(roleKey)
}

export function isAdmin() {
  return hasRole('admin') || localStorage.getItem('role') === 'ADMIN'
}

/**
 * 按钮级权限判断. 支持 "*:*:*" 通配(超管).
 * @param {string|string[]} perm 权限标识或数组
 * @param {boolean} [all=false] 数组传 true 表示需全部满足
 */
export function hasPerm(perm, all = false) {
  const perms = getPerms()
  if (perms.includes('*:*:*')) {
    return true
  }
  const need = Array.isArray(perm) ? perm : [perm]
  if (need.length === 0) {
    return true
  }
  if (all) {
    return need.every(p => perms.includes(p))
  }
  return need.some(p => perms.includes(p))
}

// ============ 动态菜单 ============

/** 拉取当前用户可见菜单并缓存(仅管理员需要) */
export async function loadUserMenus() {
  try {
    const menus = await fetchMenusApi()
    localStorage.setItem('menus', JSON.stringify(menus || []))
    return menus || []
  } catch (e) {
    return getMenus()
  }
}

/** 把菜单树展开为扁平路由列表 */
export function flattenMenus(menus, parentPath = '') {
  const result = []
  const walk = (list, base) => {
    for (const m of list || []) {
      if (m.menuType === 'C' && m.path) {
        result.push({
          path: base ? `${base}/${m.path}` : m.path,
          component: m.component,
          title: m.menuName,
          perms: m.perms ? [m.perms] : []
        })
      }
      if (m.children && m.children.length) {
        const nextBase = (m.menuType === 'M' && m.path)
          ? (base ? `${base}/${m.path}` : m.path)
          : base
        walk(m.children, nextBase)
      }
    }
  }
  walk(menus, parentPath)
  return result
}
