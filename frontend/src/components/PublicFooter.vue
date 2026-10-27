<template>
  <footer class="pf">
    <div class="pf-top">
      <span class="pf-brand">论文格式助手</span>
      <nav class="pf-links" aria-label="页脚导航">
        <a @click="go('/features')">功能</a>
        <a @click="go('/template-market')">模板</a>
        <a @click="go('/guide')">教程</a>
        <a @click="go('/cases')">案例</a>
        <a @click="go('/pricing')">价格</a>
        <a @click="go('/about')">关于</a>
        <a @click="go('/terms')">服务条款</a>
        <a @click="go('/privacy')">隐私政策</a>
      </nav>
    </div>
    <p class="pf-copy">© {{ year }} 论文格式助手 · 为毕业论文排版而生</p>
  </footer>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const year = new Date().getFullYear()
function go(path) {
  router.push(path)
}
</script>

<style scoped>
.pf {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --ink-3: #6b6b6b;
  --red: #dd2627;
  max-width: 1536px;
  margin: 0 auto;
  padding: 56px 48px 64px;
  border-top: 1px solid var(--ink);
  box-sizing: border-box;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.pf-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 40px;
}
.pf-brand {
  font-size: 22px;
  font-weight: 900;
  color: var(--ink);
}
.pf-links {
  display: flex;
  flex-wrap: wrap;
  gap: 28px;
}
.pf-links a {
  font-size: 15px;
  color: var(--ink-2);
  cursor: pointer;
}
.pf-links a:hover {
  color: var(--red);
}
.pf-copy {
  margin: 0;
  font-size: 13px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
}
@media (max-width: 900px) {
  .pf {
    padding: 36px 18px 48px;
  }
  .pf-top {
    flex-direction: column;
    gap: 20px;
    margin-bottom: 26px;
  }
  .pf-links {
    gap: 16px 20px;
  }
}
</style>
