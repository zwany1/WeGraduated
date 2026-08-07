<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/')">‹ 返回首页</el-button>
        <span>排版任务</span>
      </div>
    </header>

    <main class="content">
      <section class="upload-box">
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".docx"
          :on-change="onFileChange"
          :file-list="fileList"
          style="width: 100%"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将论文拖到此处，或<em>点击上传</em>（.docx）</div>
        </el-upload>
        <div class="format-row">
          <span>选择格式方案：</span>
          <el-select v-model="templateId" placeholder="请选择格式方案" style="width: 260px">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-button type="primary" :loading="submitting" @click="submit">开始排版</el-button>
        </div>
      </section>

      <section class="task-list">
        <h3>历史任务</h3>
        <el-table :data="tasks" empty-text="暂无任务">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="论文" min-width="140">
            <template #default="{ row }">{{ fileName(row) }}</template>
          </el-table-column>
          <el-table-column label="格式方案" min-width="140">
            <template #default="{ row }">{{ templateName(row) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="180">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'SUCCESS'" type="success">已完成</el-tag>
              <el-tag v-else-if="row.status === 'FAILED'" type="danger">失败</el-tag>
              <el-tag v-else type="warning">处理中</el-tag>
              <el-progress
                v-if="row.status === 'PROCESSING' || row.status === 'PENDING'"
                :percentage="row.progress"
                :stroke-width="6"
                style="width: 120px; margin-top: 4px"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button v-if="row.status === 'SUCCESS'" size="small" type="primary" @click="download(row)">下载</el-button>
              <el-button v-if="row.status === 'FAILED'" size="small" @click="retry(row)">重试</el-button>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listTemplates } from '../api/template'
import { uploadPaper, startFormat, listTasks, getTask, downloadPaper } from '../api/paper'

const templates = ref([])
const templateId = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const submitting = ref(false)
const tasks = ref([])
let pollTimer = null

onMounted(async () => {
  templates.value = await listTemplates()
  await loadTasks()
})

onBeforeUnmount(() => clearInterval(pollTimer))

function onFileChange(file) {
  if (file.name && !file.name.toLowerCase().endsWith('.docx')) {
    ElMessage.error('仅支持 .docx 文件')
    fileList.value = []
    selectedFile.value = null
    return
  }
  selectedFile.value = file.raw || file
}

async function submit() {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传论文文件')
    return
  }
  if (!templateId.value) {
    ElMessage.warning('请选择格式方案')
    return
  }
  submitting.value = true
  try {
    const paperFile = await uploadPaper(selectedFile.value)
    const task = await startFormat(paperFile.id, templateId.value)
    ElMessage.success('排版任务已提交')
    selectedFile.value = null
    fileList.value = []
    await loadTasks()
    startPolling()
  } finally {
    submitting.value = false
  }
}

function startPolling() {
  clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    const running = tasks.value.some(t => t.status === 'PROCESSING' || t.status === 'PENDING')
    if (!running) {
      clearInterval(pollTimer)
      return
    }
    await loadTasks(true)
  }, 1000)
}

async function loadTasks(silent) {
  tasks.value = await listTasks()
  const running = tasks.value.some(t => t.status === 'PROCESSING' || t.status === 'PENDING')
  if (running && !pollTimer) {
    startPolling()
  }
}

async function download(row) {
  const blob = await downloadPaper(row.id)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `已排版论文_${row.id}.docx`
  a.click()
  URL.revokeObjectURL(url)
}

async function retry(row) {
  await startFormat(row.fileId, row.templateId)
  ElMessage.success('已重新提交')
  startPolling()
}

function fileName(row) {
  return row.originalName || (row.fileId + '.docx')
}

function templateName(row) {
  const t = templates.value.find(x => x.id === row.templateId)
  return t ? t.name : `#${row.templateId}`
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}
.bar {
  display: flex;
  align-items: center;
  padding: 14px 30px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 20px;
}
.upload-box {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.format-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  color: #606266;
}
.task-list {
  margin-top: 24px;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.task-list h3 {
  color: #303133;
  margin-bottom: 16px;
}
</style>
