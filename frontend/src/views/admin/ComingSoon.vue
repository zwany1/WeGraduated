<template>
  <div class="coming-soon">
    <div class="card">
      <div class="mark">
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#3a6ea5" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
      </div>
      <h2 class="title">页面建设中</h2>
      <p class="desc">该菜单对应的页面组件尚未配置或已停用，请联系管理员。</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.coming-soon {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card {
  text-align: center;
  padding: 44px 56px;
  background: #fffdf9;
  border: 1px solid #e6ded0;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04);
}
.mark {
  width: 52px;
  height: 52px;
  margin: 0 auto 16px;
  border-radius: 14px;
  background: rgba(58, 110, 165, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.title {
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: 18px;
  color: #0d1b2e;
  margin-bottom: 8px;
}
.desc {
  font-size: 13px;
  color: #8a8d99;
}
</style>
