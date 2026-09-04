<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <FeedbackButton />
    <BackendStatus />
  </el-config-provider>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import FeedbackButton from './components/FeedbackButton.vue'
import BackendStatus from './components/BackendStatus.vue'

const route = useRoute()
watch(
  () => route.path,
  p => document.body.classList.toggle('theme-academic', !p.startsWith('/admin')),
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
