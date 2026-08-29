<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <FeedbackButton />
    <BackendStatus />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import FeedbackButton from './components/FeedbackButton.vue'
import BackendStatus from './components/BackendStatus.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  background: #ffffff;
}
</style>
