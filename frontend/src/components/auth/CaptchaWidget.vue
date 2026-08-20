<template>
  <div class="captcha-widget" :class="{ 'is-done': done }">
    <!-- 顶部条: 类型徽章 + 提示 + 刷新 -->
    <div class="captcha-bar">
      <span class="captcha-type-badge" :class="'badge-' + (data.type || '').toLowerCase()" v-if="data.type">{{ typeLabel }}</span>
      <span class="captcha-tip" v-if="data.type === 'CLICK'">{{ data.tip }}</span>
      <span class="captcha-tip" v-else-if="data.type === 'REORDER'">{{ data.tip }}</span>
      <span class="captcha-tip" v-else-if="data.type === 'ROTATE'">拖动图片将图案旋正</span>
      <button type="button" class="refresh-btn" @click="refresh" title="换一个">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        <span>换一个</span>
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="status === 'loading'" class="captcha-skeleton">验证码加载中…</div>

    <!-- 错误 -->
    <div v-else-if="status === 'error'" class="captcha-error" @click="refresh">
      加载失败，点击重试
    </div>

    <!-- 点选 -->
    <div v-else-if="data.type === 'CLICK'" class="captcha-stage">
      <div class="captcha-canvas-wrap" :style="{ width: data.width + 'px', height: data.height + 'px' }">
        <img class="captcha-bg" :src="bgDataUrl" alt="验证码" />
        <canvas
          ref="clickCanvas"
          class="captcha-overlay"
          :width="data.width"
          :height="data.height"
          @click="onCanvasClick"
        ></canvas>
      </div>
      <div class="captcha-meta">{{ done ? '已完成，可提交' : '已选 ' + points.length + ' / ' + data.targetCount }}</div>
    </div>

    <!-- 文字还原拖拽 -->
    <div v-else-if="data.type === 'REORDER'" class="captcha-stage">
      <div ref="reorderWrap" class="captcha-canvas-wrap reorder-wrap" :style="{ width: data.width + 'px', height: data.height + 'px' }">
        <img class="captcha-bg" :src="bgDataUrl" alt="验证码" />
        <div
          v-for="t in tokens"
          :key="t.id"
          class="reorder-token"
          :class="{ 'is-dragging': dragToken && dragToken.id === t.id }"
          :style="{ left: t.x + 'px', top: t.y + 'px', width: data.radius * 2 + 'px', height: data.radius * 2 + 'px', background: t.color }"
          @mousedown="onTokenDown($event, t)"
        >{{ t.char }}</div>
      </div>
      <div class="captcha-meta">{{ done ? '已完成，可提交' : reorderProgress + ' 已就位' }}</div>
    </div>

    <!-- 旋转 -->
    <div v-else-if="data.type === 'ROTATE'" class="captcha-stage">
      <div class="captcha-canvas-wrap rotate-wrap" :style="{ width: data.width + 'px', height: data.height + 'px' }">
        <img
          class="captcha-rotate-img"
          :src="rotateDataUrl"
          :style="{ transform: 'rotate(' + rot + 'deg)' }"
          @mousedown="onRotateDown"
        />
      </div>
      <div class="captcha-meta">{{ done ? '已旋正，可提交' : '拖动图片旋转' }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { generateCaptcha } from '../../api/captcha'

const props = defineProps({
  captchaCode: { type: String, default: '' },
  captchaId: { type: String, default: '' }
})
const emit = defineEmits(['update:captchaCode', 'update:captchaId'])

const status = ref('loading')
const data = reactive({
  type: '', width: 0, height: 0, tip: '', targetCount: 0,
  radius: 18, count: 0
})

// 各类型图片 dataUrl
const bgDataUrl = ref('')
const rotateDataUrl = ref('')

// 点选状态
const clickCanvas = ref(null)
const points = ref([])

// 文字还原状态
const reorderWrap = ref(null)
const tokens = ref([])
const slots = ref([])
const slotAssign = ref([]) // slotAssign[slotIndex] = tokenId, -1 为空
let dragToken = null
let dragOffsetX = 0
let dragOffsetY = 0
let wrapRect = null

// 旋转状态
const rot = ref(0)
let rotStartAngle = 0
let rotStartRot = 0
let rotDragging = false

const done = computed(() => !!props.captchaCode)

const typeLabel = computed(() => ({
  CLICK: '点选验证',
  REORDER: '文字还原',
  ROTATE: '旋转验证'
}[data.type] || ''))

const reorderProgress = computed(() => {
  const filled = slotAssign.value.filter(v => v >= 0).length
  return filled + ' / ' + data.count
})

// ===== 交互完成 → 序列化 payload 写回父组件 =====
function emitClick() {
  emit('update:captchaCode', JSON.stringify({ type: 'CLICK', points: points.value }))
}
function emitReorder() {
  emit('update:captchaCode', JSON.stringify({ type: 'REORDER', slots: [...slotAssign.value] }))
}
function emitRotate() {
  emit('update:captchaCode', JSON.stringify({ type: 'ROTATE', endAngle: Math.round(rot.value) }))
}

// ===== 点选 =====
function onCanvasClick(e) {
  const canvas = clickCanvas.value
  if (!canvas || points.value.length >= data.targetCount) return
  const rect = canvas.getBoundingClientRect()
  const rx = data.width / rect.width
  const ry = data.height / rect.height
  const x = Math.round((e.clientX - rect.left) * rx)
  const y = Math.round((e.clientY - rect.top) * ry)
  points.value.push([x, y])
  drawClickMarks()
  if (points.value.length === data.targetCount) {
    emitClick()
  }
}
function drawClickMarks() {
  const canvas = clickCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  points.value.forEach((p, i) => {
    ctx.beginPath()
    ctx.arc(p[0], p[1], 16, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(64,158,255,0.25)'
    ctx.fill()
    ctx.beginPath()
    ctx.arc(p[0], p[1], 12, 0, Math.PI * 2)
    ctx.fillStyle = '#409eff'
    ctx.fill()
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 14px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(String(i + 1), p[0], p[1])
  })
}

// ===== 文字还原拖拽(全局 mousemove/up) =====
function onTokenDown(e, t) {
  e.preventDefault()
  if (!reorderWrap.value) return
  wrapRect = reorderWrap.value.getBoundingClientRect()
  dragToken = t
  dragOffsetX = e.clientX - wrapRect.left - t.x
  dragOffsetY = e.clientY - wrapRect.top - t.y
  // 从原槽位移出
  const oldSlot = slotAssign.value.indexOf(t.id)
  if (oldSlot >= 0) slotAssign.value[oldSlot] = -1
  emit('update:captchaCode', '')
  window.addEventListener('mousemove', onTokenMove)
  window.addEventListener('mouseup', onTokenUp)
}
function onTokenMove(e) {
  if (!dragToken || !wrapRect) return
  dragToken.x = Math.round(e.clientX - wrapRect.left - dragOffsetX)
  dragToken.y = Math.round(e.clientY - wrapRect.top - dragOffsetY)
}
function onTokenUp() {
  if (!dragToken) return
  const cx = dragToken.x + data.radius
  const cy = dragToken.y + data.radius
  let best = -1
  let bestDist = Infinity
  slots.value.forEach((s, i) => {
    const d = Math.hypot(cx - s.x, cy - s.y)
    if (d < data.radius + 14 && d < bestDist) {
      best = i
      bestDist = d
    }
  })
  if (best >= 0) {
    // 槽被占则把原占用 token 弹回初始位
    const occupied = slotAssign.value[best]
    if (occupied >= 0 && occupied !== dragToken.id) {
      const ot = tokens.value.find(tk => tk.id === occupied)
      if (ot) { ot.x = ot.ix; ot.y = ot.iy }
    }
    slotAssign.value[best] = dragToken.id
    const s = slots.value[best]
    dragToken.x = s.x - data.radius
    dragToken.y = s.y - data.radius
  } else {
    dragToken.x = dragToken.ix
    dragToken.y = dragToken.iy
  }
  dragToken = null
  window.removeEventListener('mousemove', onTokenMove)
  window.removeEventListener('mouseup', onTokenUp)
  if (slotAssign.value.length === data.count && slotAssign.value.every(v => v >= 0)) {
    emitReorder()
  }
}

// ===== 旋转(全局 mousemove/up) =====
const rotateCenter = { x: 0, y: 0 } // mousedown 时记录的图片中心(屏幕坐标, move 时复用)
function onRotateDown(e) {
  e.preventDefault()
  const rect = e.currentTarget.getBoundingClientRect()
  rotateCenter.x = rect.left + rect.width / 2
  rotateCenter.y = rect.top + rect.height / 2
  rotDragging = true
  rotStartAngle = Math.atan2(e.clientY - rotateCenter.y, e.clientX - rotateCenter.x)
  rotStartRot = rot.value
  emit('update:captchaCode', '')
  window.addEventListener('mousemove', onRotateMove)
  window.addEventListener('mouseup', onRotateUp)
}
function onRotateMove(e) {
  if (!rotDragging) return
  const cur = Math.atan2(e.clientY - rotateCenter.y, e.clientX - rotateCenter.x)
  let delta = cur - rotStartAngle
  while (delta > Math.PI) delta -= 2 * Math.PI
  while (delta < -Math.PI) delta += 2 * Math.PI
  rot.value = rotStartRot + (delta * 180) / Math.PI
}
function onRotateUp() {
  if (!rotDragging) return
  rotDragging = false
  window.removeEventListener('mousemove', onRotateMove)
  window.removeEventListener('mouseup', onRotateUp)
  emitRotate()
}

// ===== 加载 / 刷新 =====
async function refresh() {
  status.value = 'loading'
  emit('update:captchaCode', '')
  points.value = []
  tokens.value = []
  slots.value = []
  slotAssign.value = []
  rot.value = 0
  try {
    const res = await generateCaptcha()
    data.type = res.type
    data.width = res.width || 0
    data.height = res.height || 0
    data.tip = res.tip || ''
    data.targetCount = res.targetCount || 0
    data.radius = res.radius || 18
    data.count = res.count || 0
    bgDataUrl.value = res.backgroundImage ? 'data:image/png;base64,' + res.backgroundImage : ''
    rotateDataUrl.value = res.rotateImage ? 'data:image/png;base64,' + res.rotateImage : ''
    // 文字还原棋子: 记录初始位 ix/iy 与当前位 x/y
    const tk = res.tokens || []
    tokens.value = tk.map(t => ({ ...t, ix: t.x, iy: t.y, x: t.x, y: t.y }))
    slots.value = res.slots || []
    slotAssign.value = new Array(data.count).fill(-1)
    emit('update:captchaId', res.captchaId)
    status.value = 'ready'
    if (res.type === 'CLICK') {
      requestAnimationFrame(drawClickMarks)
    }
  } catch (e) {
    status.value = 'error'
  }
}

defineExpose({ refresh })

onMounted(() => {
  if (!props.captchaId) {
    refresh()
  } else {
    status.value = 'ready'
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onTokenMove)
  window.removeEventListener('mouseup', onTokenUp)
  window.removeEventListener('mousemove', onRotateMove)
  window.removeEventListener('mouseup', onRotateUp)
})
</script>

<style scoped>
.captcha-widget {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 顶部条: 徽章 + 提示 + 刷新 */
.captcha-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 20px;
}
.captcha-type-badge {
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #fff;
  background: #909399;
}
.badge-click { background: #409eff; }
.badge-reorder { background: #722ed1; }
.badge-rotate { background: #e6a23c; }
.captcha-tip {
  flex: 1;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.refresh-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.refresh-btn:hover {
  color: #409eff;
  border-color: #409eff;
  background: #ecf5ff;
}
.refresh-btn svg {
  transition: transform 0.4s ease;
}
.refresh-btn:hover svg {
  transform: rotate(180deg);
}

/* 骨架 / 错误 */
.captcha-skeleton,
.captcha-error {
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
}
.captcha-skeleton {
  background: linear-gradient(90deg, #f5f7fa 25%, #ecf5ff 37%, #f5f7fa 63%);
  background-size: 400% 100%;
  animation: captcha-skeleton 1.4s ease infinite;
}
@keyframes captcha-skeleton {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
.captcha-error {
  background: #fef0f0;
  border-color: #fbc4c4;
  color: #f56c6c;
}

.captcha-stage {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

/* 画布容器: 圆角 + 阴影 + 完成态发光 */
.captcha-canvas-wrap {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  user-select: none;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.06);
  transition: border-color 0.3s, box-shadow 0.3s;
}
.captcha-widget.is-done .captcha-canvas-wrap {
  border-color: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.12);
}
.captcha-bg {
  display: block;
  width: 100%;
  height: 100%;
}
.captcha-overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  cursor: crosshair;
}

.captcha-meta {
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.captcha-widget.is-done .captcha-meta {
  color: #67c23a;
  font-weight: 600;
}

/* 文字还原棋子 */
.reorder-token {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  font-family: Arial, sans-serif;
  cursor: grab;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.28), inset 0 1px 1px rgba(255, 255, 255, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.7);
  transition: transform 0.12s, box-shadow 0.12s;
}
.reorder-token:hover {
  transform: scale(1.08);
}
.reorder-token.is-dragging {
  cursor: grabbing;
  z-index: 10;
  transform: scale(1.12);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.35);
}

/* 旋转: 圆形转盘 */
.rotate-wrap {
  border-radius: 50%;
}
.captcha-rotate-img {
  display: block;
  width: 100%;
  height: 100%;
  cursor: grab;
  border-radius: 50%;
}
.captcha-rotate-img:active {
  cursor: grabbing;
}
</style>
