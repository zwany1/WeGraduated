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
// 已完成重设计的页面（瑞士海报墙）：首页 + 我的格式方案。其余页面暂时沿用学术沉静风，随重设计逐个切换
const SWISS_ROUTES = ['/', '/home', '/features', '/guide', '/cases', '/pricing', '/terms', '/privacy', '/templates', '/tasks', '/team', '/profile', '/template-market']
// 公共营销页（无登录态工作台上下文）不挂浮动组件，与定稿样张一致
const BARE_PUBLIC_ROUTES = ['/', '/home', '/features', '/guide', '/cases', '/pricing', '/terms', '/privacy']
watch(
  () => [route.path, route.name],
  ([p, name]) => {
    // 404（catch-all）无导航自成一体：纳入瑞士主题、不挂浮动件
    const nf = name === 'notfound'
    const swiss = SWISS_ROUTES.includes(p) || p.startsWith('/cases/') || p.startsWith('/template/') || nf
    isLanding.value = BARE_PUBLIC_ROUTES.includes(p) || p.startsWith('/cases/') || nf
    document.body.classList.toggle('bare-public', isLanding.value)
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
