<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <FeedbackButton v-if="!isLanding" />
    <BackendStatus v-if="!isLanding" />
  </el-config-provider>
</template>

<script setup>
import { watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import FeedbackButton from './components/FeedbackButton.vue'
import BackendStatus from './components/BackendStatus.vue'

const route = useRoute()
const isLanding = ref(true)
watch(
  () => route.path,
  p => {
    // 首页已迁移到瑞士海报墙世界观；其余页面暂时沿用学术沉静风，随重设计逐个切换
    const swiss = p === '/home' || p === '/'
    isLanding.value = swiss
    document.body.classList.toggle('theme-swiss', swiss)
    document.body.classList.toggle('theme-academic', !swiss && !p.startsWith('/admin'))
  },
  { immediate: true }
)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
}
</style>
