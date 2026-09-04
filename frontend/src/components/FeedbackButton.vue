<template>
  <button v-show="!hideOnAdmin" class="feedback-fab" title="用户反馈" @click="open">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
    </svg>
    <span class="fab-label">反馈</span>
    <span class="fab-pulse"></span>
  </button>
  <FeedbackDialog v-model:visible="visible" />
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '../utils/perm'
import FeedbackDialog from './FeedbackDialog.vue'

const route = useRoute()
const router = useRouter()
const visible = ref(false)
const hideOnAdmin = computed(() => route.path.startsWith('/admin'))

function open() {
  if (!getToken()) {
    ElMessage.warning('请先登录后再反馈')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  visible.value = true
}
</script>

<style scoped>
.feedback-fab {
  position: fixed;
  right: 26px;
  bottom: 84px;
  z-index: 150;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 1px solid rgba(47, 93, 70, 0.2);
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #2F5D46, #8B6F47);
  box-shadow: 0 6px 20px rgba(47, 93, 70, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  backdrop-filter: blur(8px);
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.feedback-fab:hover {
  transform: translateY(-4px) scale(1.06);
  box-shadow: 0 12px 32px rgba(47, 93, 70, 0.5);
}
.feedback-fab:active {
  transform: translateY(-2px) scale(1.02);
}
.fab-label {
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.05em;
}
/* 脉冲光圈 */
.fab-pulse {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 2px solid rgba(47, 93, 70, 0.4);
  animation: fabPulse 2.5s cubic-bezier(0.4, 0, 0.2, 1) infinite;
  pointer-events: none;
}
@keyframes fabPulse {
  0% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.3); opacity: 0; }
  100% { transform: scale(1.3); opacity: 0; }
}
</style>
