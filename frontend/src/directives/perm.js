import { hasPerm } from '../utils/perm'

/**
 * v-perm 按钮级权限指令.
 * 用法: v-perm="'system:user:add'"  或  v-perm="['system:user:add','system:user:edit']"
 *       v-perm.all="[...]" 表示需全部满足
 * 授权变更时随 perms-changed 事件即时切换显隐.
 */
function apply(el) {
  const s = el._perm
  el.style.display = s && s.need.length !== 0 && !hasPerm(s.need, s.all) ? 'none' : ''
}

function sync(el, { value, arg, modifiers }) {
  el._perm = value == null ? null : {
    need: Array.isArray(value) ? value : [value],
    all: arg === 'all' || !!modifiers.all
  }
  apply(el)
}

const perm = {
  mounted(el, binding) {
    el._permApply = () => apply(el)
    window.addEventListener('perms-changed', el._permApply)
    sync(el, binding)
  },
  updated: sync,
  unmounted(el) {
    window.removeEventListener('perms-changed', el._permApply)
  }
}

export default perm
