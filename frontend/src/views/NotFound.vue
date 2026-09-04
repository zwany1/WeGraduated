<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="desc">页面不存在或已被移除</p>
    <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #F6F4EE;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.code {
  font-size: 96px;
  font-weight: 800;
  color: #2F5D46;
  line-height: 1;
}
.desc {
  font-size: 16px;
  color: #8B968D;
  margin: 0 0 12px;
}
</style>
