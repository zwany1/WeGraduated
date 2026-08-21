<template>
  <button v-show="!hideOnAdmin" class="feedback-fab" title="用户反馈" @click="open">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
    </svg>
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
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #3b6bff, #5b8cff);
  box-shadow: 0 6px 18px rgba(59, 107, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.feedback-fab:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(59, 107, 255, 0.45);
}
</style>
