<template>
  <div class="page">
    <NavBar />

    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">简单透明的定价</h1>
        <p class="hero-desc">现在免费使用，毕业季无需额外花费</p>
      </div>
    </section>

    <section class="section">
      <div class="section-inner">
        <div class="pricing-grid">
          <div class="plan" v-for="(p, i) in plans" :key="i" :class="{ featured: p.featured, 'current': p.current }">
            <div v-if="p.featured" class="plan-badge">最受欢迎</div>
            <h3 class="plan-name">{{ p.name }}</h3>
            <div class="plan-price">
              <span class="price-symbol">¥</span>
              <span class="price-num">{{ p.price }}</span>
              <span class="price-unit">/{{ p.unit }}</span>
            </div>
            <p class="plan-desc">{{ p.desc }}</p>
            <ul class="plan-features">
              <li v-for="(f, j) in p.features" :key="j">
                <svg viewBox="0 0 16 16" width="14" height="14" fill="none"><circle cx="8" cy="8" r="7" :fill="p.featured ? '#3B6BFF' : '#10b981'"/><path d="M5 8l2 2 4-4" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/></svg>
                {{ f }}
              </li>
            </ul>
            <button class="plan-btn" :class="{ primary: p.featured }" @click="choose(p)">{{ p.btn }}</button>
            <p v-if="p.current" class="plan-current-tip">当前计划</p>
          </div>
        </div>

        <div class="notice">
          <p>💡 提示：本项目为学习交流用途，当前所有功能免费开放。如需商业授权请联系我们。</p>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/site/NavBar.vue'
import SiteFooter from '../components/site/SiteFooter.vue'

const router = useRouter()

const plans = [
  {
    name: '免费版',
    price: '0',
    unit: '月',
    desc: '满足个人毕业论文排版需求',
    features: ['论文智能排版', '三线表 / ER 图 / 系统图', '5 个自定义模板', 'PDF 预览'],
    btn: '立即开始',
    current: true
  },
  {
    name: '专业版',
    price: '29',
    unit: '月',
    desc: '面向需要多模板、高频率排版的用户',
    features: ['无限模板数量', '高级排版规则', '批量排版任务', '优先处理队列', '团队协作'],
    btn: '升级专业版',
    featured: true
  },
  {
    name: '团队版',
    price: '99',
    unit: '月',
    desc: '适合实验室 / 课题组统一排版规范',
    features: ['专业版全部功能', '5 个成员席位', '共享模板库', '管理员控制台', '专属支持'],
    btn: '联系商务'
  }
]

function choose(plan) {
  if (plan.current) {
    if (!localStorage.getItem('token')) {
      router.push({ path: '/login', query: { redirect: '/templates' } })
    } else {
      router.push('/templates')
    }
    return
  }
  if (plan.name === '团队版') {
    window.location.href = 'mailto:2651896126@qq.com?subject=' + encodeURIComponent('团队版咨询')
    return
  }
  ElMessage.info(plan.name + ' 即将开放，敬请期待')
}
</script>

<style scoped>
.page {
  --c-primary: #3B6BFF;
  --c-primary-dark: #2D52CC;
  --c-dark: #1a1a2e;
  --c-text: #374151;
  --c-text2: #6b7280;
  --c-text3: #9ca3af;
  --c-border: #e5e7eb;
  --c-bg2: #f8f9fc;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--c-text);
  background: #fff;
  min-height: 100vh;
}
.hero {
  background: linear-gradient(180deg, #f0f4ff 0%, #fff 100%);
  padding: 64px 32px;
  text-align: center;
}
.hero-title { font-size: 38px; font-weight: 800; color: var(--c-dark); margin: 0 0 10px; }
.hero-desc { font-size: 16px; color: var(--c-text2); margin: 0; }
.section { padding: 56px 32px; }
.section-inner { max-width: 1080px; margin: 0 auto; }
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  align-items: stretch;
}
.plan {
  position: relative;
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 18px;
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s, transform 0.2s;
}
.plan:hover {
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
}
.plan.featured {
  border: 2px solid var(--c-primary);
  box-shadow: 0 12px 40px rgba(59,107,255,0.15);
}
.plan-badge {
  position: absolute;
  top: -13px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--c-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 16px;
  border-radius: 999px;
}
.plan-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-dark);
  margin: 0 0 12px;
}
.plan-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 8px;
}
.price-symbol {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-dark);
}
.price-num {
  font-size: 48px;
  font-weight: 800;
  color: var(--c-dark);
  line-height: 1;
}
.price-unit {
  font-size: 14px;
  color: var(--c-text3);
}
.plan-desc {
  font-size: 13px;
  color: var(--c-text2);
  margin: 0 0 20px;
  line-height: 1.6;
}
.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}
.plan-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--c-text);
}
.plan-btn {
  width: 100%;
  padding: 11px 0;
  border: 1px solid var(--c-border);
  background: #fff;
  color: var(--c-text);
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.plan-btn:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}
.plan-btn.primary {
  background: var(--c-primary);
  color: #fff;
  border-color: var(--c-primary);
}
.plan-btn.primary:hover {
  background: var(--c-primary-dark);
}
.plan-current-tip {
  text-align: center;
  font-size: 12px;
  color: var(--c-green, #10b981);
  margin-top: 10px;
}
.notice {
  margin-top: 40px;
  background: #f0f4ff;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
}
.notice p {
  font-size: 14px;
  color: var(--c-text2);
  margin: 0;
}
@media (max-width: 900px) {
  .pricing-grid { grid-template-columns: 1fr; }
}
</style>
