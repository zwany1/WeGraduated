<template>
  <el-dialog v-model="visible" title="Mermaid 导出" width="680px" top="6vh" destroy-on-close>
    <div class="mmd-tip">
      以下为生成的 Mermaid 代码，可复制后粘贴到
      <a href="https://mermaid.live" target="_blank" rel="noopener">mermaid.live</a>
      在线预览，或下载为 .mmd 文件。
    </div>
    <el-input :model-value="code" type="textarea" :rows="16" readonly class="mmd-input" />
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="copy">复制</el-button>
      <el-button type="success" @click="download">下载 .mmd</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadText, copyText } from '../utils/download'

const visible = ref(false)
const code = ref('')

function open(mmd, filename) {
  code.value = mmd || ''
  fileName.value = filename || 'diagram.mmd'
  visible.value = true
}

async function copy() {
  const ok = await copyText(code.value)
  if (ok) ElMessage.success('已复制到剪贴板')
  else ElMessage.warning('复制失败，请手动选择文本复制')
}

function download() {
  downloadText(code.value, fileName.value)
  ElMessage.success('已下载')
}

const fileName = ref('diagram.mmd')

defineExpose({ open })
</script>

<style scoped>
.mmd-tip {
  font-size: 12.5px;
  color: #8a8d99;
  margin-bottom: 10px;
  line-height: 1.6;
}
.mmd-tip a {
  color: #3a6ea5;
}
.mmd-input :deep(textarea) {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #24312A;
}
</style>
