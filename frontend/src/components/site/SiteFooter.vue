<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <div class="logo-w">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h4l3 12 3-12h4"/></svg>
        </div>
        <span>Word 排版助手</span>
      </div>
      <div class="footer-links">
        <a @click="go('/features')">功能</a>
        <a @click="go('/template-market')">模板</a>
        <a @click="go('/guide')">使用教程</a>
        <a @click="go('/cases')">案例</a>
        <a @click="go('/pricing')">价格</a>
        <a href="mailto:2651896126@qq.com">联系我们</a>
      </div>
      <div class="footer-copy">&copy; 2026 Word 排版助手. All rights reserved.</div>
    </div>
  </footer>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function go(path) {
  router.push(path)
}
</script>

<style scoped>
.site-footer {
  border-top: 1px solid var(--c-border, #e5e7eb);
  background: #fff;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--c-dark, #1a1a2e);
  font-weight: 700;
  font-size: 15px;
}
.logo-w {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: linear-gradient(135deg, #3B6BFF, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
}
.footer-links {
  display: flex;
  gap: 24px;
}
.footer-links a {
  color: var(--c-text2, #6b7280);
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
}
.footer-links a:hover {
  color: var(--c-primary, #3B6BFF);
}
.footer-copy {
  color: var(--c-text3, #9ca3af);
  font-size: 13px;
}
</style>
