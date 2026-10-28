<template>
  <!-- 404：墨块告示。错误页即时呈现，不做入场动画（避免观察器依赖导致内容隐形） -->
  <div class="nf">
    <div class="nf-top">
      <span class="nf-brand">论文格式助手</span>
      <span class="nf-code">404 /<br />PAGE NOT FOUND</span>
    </div>

    <div class="slab">
      <p class="s-kicker">NOTICE</p>
      <h1 class="s-title">页面不存在</h1>
      <p class="s-sub">或已被移除</p>
      <div class="s-rule" aria-hidden="true"></div>
      <button class="s-btn" type="button" @click="goHome">返回首页</button>
    </div>

    <p class="nf-foot">
      <span>论文格式助手</span>
      <i aria-hidden="true">·</i>
      <a class="nf-home" @click="goHome">返回 /home</a>
    </p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/home')
}
</script>

<style scoped>
.nf {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --red: #dd2627;
  --hair: #c1c1c3;
  --u: calc(min(100vw, 1536px) / 1536);
  min-height: 100vh;
  background: var(--paper);
  color: var(--ink);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  display: flex;
  flex-direction: column;
}

/* ---------- 顶行 ---------- */
.nf-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: calc(52 * var(--u)) calc(97 * var(--u)) 0;
}
.nf-brand {
  font-size: calc(24 * var(--u));
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--ink);
}
.nf-code {
  text-align: right;
  font-size: calc(16 * var(--u));
  font-weight: 500;
  line-height: 1.5;
  letter-spacing: 0.12em;
  color: #8a8b8b;
}

/* ---------- 墨块 ---------- */
.slab {
  margin: calc(40 * var(--u)) calc(97 * var(--u)) 0;
  background: var(--ink);
  color: #fff;
  text-align: center;
  padding: calc(100 * var(--u)) calc(60 * var(--u)) calc(90 * var(--u));
}
.s-kicker {
  margin: 0;
  font-size: calc(26 * var(--u));
  font-weight: 700;
  letter-spacing: 0.45em;
  text-indent: 0.45em;
  color: #fff;
}
.s-title {
  margin: calc(44 * var(--u)) 0 0;
  font-size: calc(178 * var(--u));
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.02em;
  color: #fff;
}
.s-sub {
  margin: calc(38 * var(--u)) 0 0;
  font-size: calc(32 * var(--u));
  font-weight: 400;
  letter-spacing: 0.3em;
  text-indent: 0.3em;
  color: rgba(255, 255, 255, 0.6);
}
.s-rule {
  margin-top: calc(84 * var(--u));
  height: calc(5 * var(--u));
  background: var(--red);
}
.s-btn {
  margin-top: calc(62 * var(--u));
  height: calc(64 * var(--u));
  padding: 0 calc(56 * var(--u));
  background: transparent;
  border: 1px solid #fff;
  color: #fff;
  font-family: inherit;
  font-size: calc(22 * var(--u));
  font-weight: 700;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.s-btn:hover {
  background: #fff;
  color: var(--ink);
}
.s-btn:focus-visible {
  outline: calc(2 * var(--u)) solid var(--red);
  outline-offset: calc(3 * var(--u));
}

/* ---------- 底行 ---------- */
.nf-foot {
  margin: auto calc(97 * var(--u)) calc(48 * var(--u));
  padding: 0;
  display: flex;
  align-items: center;
  gap: calc(12 * var(--u));
  font-size: calc(17 * var(--u));
  color: #8a8b8b;
}
.nf-foot i {
  font-style: normal;
}
.nf-home {
  color: var(--ink-2);
  cursor: pointer;
  background-image: linear-gradient(var(--red), var(--red));
  background-repeat: no-repeat;
  background-position: 0 100%;
  background-size: 100% calc(2 * var(--u));
  padding-bottom: calc(3 * var(--u));
  transition: background-size 0.15s ease;
}
.nf-home:hover {
  background-size: 100% 100%;
}

::selection {
  background: var(--red);
  color: #fff;
}

/* ---------- 窄屏 ---------- */
@media (max-width: 900px) {
  .nf-top {
    padding: 18px 16px 0;
  }
  .nf-brand {
    font-size: 16px;
  }
  .nf-code {
    font-size: 11px;
  }
  .slab {
    margin: 20px 16px 0;
    padding: 48px 20px 44px;
  }
  .s-kicker {
    font-size: 13px;
  }
  .s-title {
    margin-top: 22px;
    font-size: 44px;
  }
  .s-sub {
    margin-top: 16px;
    font-size: 15px;
  }
  .s-rule {
    margin-top: 36px;
    height: 3px;
  }
  .s-btn {
    margin-top: 30px;
    height: 44px;
    padding: 0 28px;
    font-size: 15px;
  }
  .nf-foot {
    margin: auto 16px 24px;
    font-size: 13px;
  }
}
</style>
