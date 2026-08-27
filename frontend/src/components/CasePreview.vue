<template>
  <div class="case-preview" :class="[color, { big }]">
    <div class="case-preview-inner">
      <svg v-if="type === 'paper'" viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
        <rect x="20" y="18" width="90" height="9" rx="3" fill="#fff" opacity="0.95"/>
        <rect x="20" y="40" width="160" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="52" width="160" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="64" width="120" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="80" width="60" height="14" rx="3" fill="#fff" opacity="0.85"/>
        <rect x="100" y="80" width="80" height="14" rx="3" fill="#fff" opacity="0.6"/>
      </svg>
      <svg v-else-if="type === 'table'" viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
        <line x1="20" y1="22" x2="180" y2="22" stroke="#fff" stroke-width="2.5" opacity="0.95"/>
        <line x1="20" y1="42" x2="180" y2="42" stroke="#fff" stroke-width="1.2" opacity="0.75"/>
        <line x1="20" y1="82" x2="180" y2="82" stroke="#fff" stroke-width="2.5" opacity="0.95"/>
        <line x1="20" y1="42" x2="20" y2="82" stroke="#fff" stroke-width="1" opacity="0.5"/>
        <line x1="100" y1="42" x2="100" y2="82" stroke="#fff" stroke-width="1" opacity="0.5"/>
        <line x1="180" y1="42" x2="180" y2="82" stroke="#fff" stroke-width="1" opacity="0.5"/>
        <line x1="60" y1="42" x2="60" y2="82" stroke="#fff" stroke-width="1" opacity="0.5"/>
        <line x1="140" y1="42" x2="140" y2="82" stroke="#fff" stroke-width="1" opacity="0.5"/>
      </svg>
      <svg v-else-if="type === 'er'" viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
        <rect x="18" y="32" width="52" height="30" rx="4" fill="#fff" opacity="0.92"/>
        <rect x="130" y="32" width="52" height="30" rx="4" fill="#fff" opacity="0.92"/>
        <polygon points="100,20 124,46 100,72 76,46" fill="#fff" opacity="0.85"/>
        <line x1="70" y1="47" x2="76" y2="46" stroke="#fff" stroke-width="1.2" opacity="0.7"/>
        <line x1="124" y1="46" x2="130" y2="47" stroke="#fff" stroke-width="1.2" opacity="0.7"/>
        <line x1="44" y1="62" x2="44" y2="78" stroke="#fff" stroke-width="1" opacity="0.55"/>
        <line x1="156" y1="62" x2="156" y2="78" stroke="#fff" stroke-width="1" opacity="0.55"/>
      </svg>
      <svg v-else-if="type === 'arch'" viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
        <rect x="20" y="16" width="160" height="16" rx="4" fill="#fff" opacity="0.95"/>
        <rect x="20" y="42" width="160" height="16" rx="4" fill="#fff" opacity="0.78"/>
        <rect x="20" y="68" width="160" height="16" rx="4" fill="#fff" opacity="0.6"/>
      </svg>
      <svg v-else viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
        <rect x="20" y="18" width="70" height="7" rx="3" fill="#fff" opacity="0.9"/>
        <rect x="110" y="18" width="70" height="7" rx="3" fill="#fff" opacity="0.9"/>
        <rect x="20" y="33" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="110" y="33" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="46" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="110" y="46" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="59" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="110" y="59" width="70" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="20" y="72" width="50" height="6" rx="3" fill="#fff" opacity="0.7"/>
        <rect x="110" y="72" width="50" height="6" rx="3" fill="#fff" opacity="0.7"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tag: { type: String, default: '' },
  color: { type: String, default: 'blue' },
  big: { type: Boolean, default: false }
})

const type = computed(() => {
  const t = (props.tag || '')
  if (t.includes('三线表') || t.toLowerCase().includes('table')) return 'table'
  if (t.includes('ER') || t.includes('实体') || t.includes('关系')) return 'er'
  if (t.includes('系统') || t.includes('架构')) return 'arch'
  if (t.includes('期刊')) return 'journal'
  return 'paper'
})
</script>

<style scoped>
.case-preview {
  border-radius: 12px;
  height: 140px;
  padding: 20px;
  margin-bottom: 16px;
}
.case-preview.big { height: 260px; padding: 32px; }
.case-preview.blue { background: linear-gradient(135deg, #EEF1FF, #dbe4ff); }
.case-preview.green { background: linear-gradient(135deg, #ECFDF5, #d1fae5); }
.case-preview.purple { background: linear-gradient(135deg, #F5F3FF, #ede9fe); }
.case-preview.orange { background: linear-gradient(135deg, #FFF7ED, #ffedd5); }
.case-preview-inner { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.case-preview-inner svg { width: 100%; height: 100%; }
</style>
