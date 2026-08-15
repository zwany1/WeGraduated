import { hasPerm } from '../utils/perm'

/**
 * v-perm 按钮级权限指令.
 * 用法: v-perm="'system:user:add'"  或  v-perm="['system:user:add','system:user:edit']"
 *       v-perm.all="[...]" 表示需全部满足
 */
const perm = {
  mounted(el, binding) {
    const { value, arg, modifiers } = binding
    const all = arg === 'all' || !!modifiers.all
    if (value !== undefined && value !== null) {
      const need = Array.isArray(value) ? value : [value]
      if (!hasPerm(need, all)) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}

export default perm
