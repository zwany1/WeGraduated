<template>
  <el-dialog v-model="visible" title="欢迎使用论文格式助手" width="480px" :close-on-click-modal="false" align-center>
    <div class="steps">
      <div class="step">
        <span class="num">1</span>
        <div>
          <b>创建格式方案</b>
          <p>在「我的方案」新建一个符合学校要求的排版方案，可归属团队共享。</p>
        </div>
      </div>
      <div class="step">
        <span class="num">2</span>
        <div>
          <b>配置排版规则</b>
          <p>设置标题、正文、图表题注、参考文献等格式，切换「效果预览」实时查看样式。</p>
        </div>
      </div>
      <div class="step">
        <span class="num">3</span>
        <div>
          <b>上传论文一键排版</b>
          <p>上传 .docx 论文，选择方案批量排版，实时查看进度与失败原因。</p>
        </div>
      </div>
      <div class="step">
        <span class="num">4</span>
        <div>
          <b>预览·对比·下载</b>
          <p>排版完成后在线预览、对比前后差异、下载结果文档。</p>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="dismiss">稍后看看</el-button>
      <el-button type="primary" @click="start">开始创建方案</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const visible = ref(false)

onMounted(() => {
  if (localStorage.getItem('token') && !localStorage.getItem('onboarding_done')) {
    visible.value = true
  }
})

function dismiss() {
  localStorage.setItem('onboarding_done', '1')
  visible.value = false
}

function start() {
  localStorage.setItem('onboarding_done', '1')
  visible.value = false
  router.push('/templates')
}
</script>

<style scoped>
.steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0;
}
.step {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #2F5D46;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step b {
  font-size: 15px;
  color: #24312A;
}
.step p {
  font-size: 13px;
  color: #8B968D;
  margin: 4px 0 0;
  line-height: 1.6;
}
</style>
