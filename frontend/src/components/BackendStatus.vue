<template>
  <div v-if="down" class="backend-status" title="排版/保存等功能暂不可用，请确认后端已启动">
    <span class="dot"></span>后端服务未连接
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 用原生 fetch 探测, 任何 HTTP 响应(含 401)都视为后端在线;
// 不走 axios 实例, 避免触发拦截器的全局错误弹窗
const down = ref(false)
let timer = null

async function ping() {
  try {
    const ctrl = new AbortController()
    const t = setTimeout(() => ctrl.abort(), 4000)
    await fetch('/api/health', { signal: ctrl.signal, cache: 'no-store' })
    clearTimeout(t)
    down.value = false
  } catch (e) {
    down.value = true
  }
}

onMounted(() => {
  ping()
  timer = setInterval(ping, 30000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.backend-status {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
  font-size: 12px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}
</style>
