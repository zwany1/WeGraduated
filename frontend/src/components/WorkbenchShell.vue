<template>
  <div class="wb">
    <SiteHeader variant="workbench">
      <template #actions>
        <slot name="actions" />
      </template>
    </SiteHeader>

    <div class="wb-head">
      <h1 class="wb-title">{{ title }}</h1>
      <div class="wb-headside">
        <slot name="meta" />
      </div>
    </div>

    <div v-if="$slots.toolbar" class="wb-toolbar">
      <slot name="toolbar" />
    </div>

    <main class="wb-main">
      <slot />
    </main>

    <footer v-if="$slots.footer" class="wb-foot">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup>
import SiteHeader from './SiteHeader.vue'

defineProps({
  title: { type: String, required: true }
})
</script>

<style scoped>
/* 工作台外壳：与首页同一套瑞士体系（纸白 / 硬线 / 单一信号红 / 零圆角） */
.wb {
  --paper: #f7f6f6;
  --ink: #0a0909;
  --ink-2: #3f3f3f;
  --ink-3: #6b6b6b;
  --red: #dd2627;
  --hair: #c1c1c3;
  --hair-soft: #e4e4e4;
  --sans: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  min-height: 100vh;
  background: var(--paper);
  color: var(--ink);
  font-family: var(--sans);
  -webkit-font-smoothing: antialiased;
}

/* ---------------- 页头 ---------------- */
.wb-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 44px 48px 18px;
  border-bottom: 2px solid var(--ink);
  max-width: 1440px;
  margin: 0 auto;
  box-sizing: border-box;
}
.wb-title {
  margin: 0;
  font-size: 52px;
  line-height: 1.06;
  font-weight: 900;
  letter-spacing: -0.02em;
}
.wb-headside {
  text-align: right;
  font-size: 13px;
  line-height: 1.9;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

/* ---------------- 工具条 ---------------- */
.wb-toolbar {
  max-width: 1440px;
  margin: 0 auto;
  padding: 18px 48px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-sizing: border-box;
}

/* ---------------- 内容 ---------------- */
.wb-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 48px 64px;
  box-sizing: border-box;
}
.wb-foot {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 48px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
  box-sizing: border-box;
}

@media (max-width: 900px) {
  .wb-head {
    padding: 26px 18px 14px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .wb-title {
    font-size: 30px;
  }
  .wb-headside {
    text-align: left;
  }
  .wb-toolbar {
    padding: 14px 18px 0;
  }
  .wb-main {
    padding: 0 18px 48px;
  }
  .wb-foot {
    padding: 0 18px 24px;
  }
}
</style>
