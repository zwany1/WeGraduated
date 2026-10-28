<template>
  <button v-show="!hideOnAdmin" class="feedback-fab" title="用户反馈" @click="open">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
    </svg>
    <span class="fab-label">反馈</span>
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
  width: 46px;
  height: 46px;
  border-radius: 0;
  border: 1px solid #c1c1c3;
  cursor: pointer;
  color: #0a0909;
  background: #f7f6f6;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}
.feedback-fab:hover {
  background: #dd2627;
  border-color: #dd2627;
  color: #fff;
}
.feedback-fab:active {
  background: #0a0909;
  border-color: #0a0909;
  color: #fff;
}
.feedback-fab svg {
  width: 18px;
  height: 18px;
}
.fab-label {
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.08em;
}
</style>
