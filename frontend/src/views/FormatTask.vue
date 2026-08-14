<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/home')">‹ 返回首页</el-button>
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
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'SUCCESS'" type="success">已完成</el-tag>
              <el-tag v-else-if="row.status === 'FAILED'" type="danger" class="fail-tag" @click="showError(row)">失败</el-tag>
              <el-tag v-else type="warning">处理中</el-tag>
              <el-progress
                v-if="row.status === 'PROCESSING' || row.status === 'PENDING'"
                :percentage="row.progress"
                :stroke-width="6"
                style="width: 120px; margin-top: 4px"
              />
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="200">
            <template #default="{ row }">
              <el-button v-if="row.status === 'FAILED' && row.errorMsg" text type="danger" size="small" @click="showError(row)">
                查看原因
              </el-button>
              <span v-else-if="row.status === 'FAILED'" style="color:#c0c4cc">无</span>
              <span v-else style="color:#c0c4cc">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <el-button v-if="row.status === 'SUCCESS'" size="small" @click="preview(row)">预览</el-button>
              <el-button v-if="row.status === 'SUCCESS'" size="small" type="success" @click="compare(row)">对比</el-button>
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

    <el-dialog v-model="previewVisible" title="排版结果预览" width="80%" top="4vh" destroy-on-close @closed="onPreviewClosed">
      <div style="height: 72vh">
        <PdfViewer v-if="previewData.length" :data="previewData" />
        <el-empty v-else description="预览加载中..." />
      </div>
    </el-dialog>

    <el-dialog v-model="compareVisible" title="排版前后对比" width="94%" top="3vh" destroy-on-close @closed="onCompareClosed">
      <div class="compare-layout">
        <div class="compare-diff">
          <div class="compare-header">
            <span class="compare-badge diff">差异索引</span>
            <span class="diff-count">{{ diffItems.length }} 处</span>
          </div>
          <div class="diff-list">
            <div v-for="(d, i) in diffItems" :key="i" class="diff-item" :class="{ active: activeDiff === i }" @click="gotoDiff(d, i)">
              <div class="diff-item-top">
                <span class="diff-type">{{ d.type }}</span>
                <span class="diff-page">P{{ d.page }}</span>
              </div>
              <div class="diff-text">{{ d.text }}</div>
            </div>
            <el-empty v-if="!diffItems.length" description="未发现差异" :image-size="60" />
          </div>
        </div>
        <div class="compare-divider"></div>
        <div class="compare-col">
          <div class="compare-header">
            <span class="compare-badge before">排版前</span>
            <span class="compare-name">{{ compareName }}</span>
          </div>
          <div class="compare-body">
            <DocxCompare v-if="compareBefore.length" ref="docxCompareRef" :data="compareBefore" />
            <el-empty v-else description="加载中..." />
          </div>
        </div>
        <div class="compare-divider"></div>
        <div class="compare-col">
          <div class="compare-header">
            <span class="compare-badge after">排版后</span>
            <span class="compare-name">{{ compareName }}</span>
          </div>
          <div class="compare-body">
            <PdfViewer v-if="compareAfter.length" ref="pdfViewerRef" :data="compareAfter" :highlights="diffItems" />
            <el-empty v-else description="加载中..." />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="errorVisible" title="排版失败原因" width="520px" top="10vh">
      <div class="error-detail">
        <p class="error-label">任务 #{{ errorTaskId }} 排版失败，原因如下：</p>
        <div class="error-box">{{ errorMsg }}</div>
        <p class="error-tip">提示：可检查模板规则是否配置完整，或确认 Word 文档格式正常后点击「重试」。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listTemplates } from '../api/template'
import { uploadPaper, startFormat, listTasks, getTask, downloadPaper, previewPaper, downloadPaperOriginal, getDiff } from '../api/paper'
import PdfViewer from '../components/PdfViewer.vue'
import DocxCompare from '../components/DocxCompare.vue'

const templates = ref([])
const templateId = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const submitting = ref(false)
const tasks = ref([])
const previewVisible = ref(false)
const previewData = ref([])
const compareVisible = ref(false)
const compareBefore = ref([])
const compareAfter = ref([])
const compareName = ref('')
const diffItems = ref([])
const activeDiff = ref(-1)
const pdfViewerRef = ref(null)
const docxCompareRef = ref(null)
const errorVisible = ref(false)
const errorMsg = ref('')
const errorTaskId = ref(null)
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
    await loadTasks(true)
    if (tasks.value.some(t => t.status === 'PROCESSING' || t.status === 'PENDING')) {
      startPolling()
    }
  } finally {
    submitting.value = false
  }
}

function startPolling() {
  clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    await loadTasks(true)
  }, 1000)
}

async function loadTasks(silent) {
  try {
    tasks.value = await listTasks()
  } catch (e) {
    if (!silent) ElMessage.error('任务列表加载失败')
    return
  }
  const running = tasks.value.some(t => t.status === 'PROCESSING' || t.status === 'PENDING')
  if (running && !pollTimer) {
    startPolling()
  } else if (!running && pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
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

let previewSeq = 0

async function preview(row) {
  compareVisible.value = false // 与对比弹窗互斥
  const mySeq = ++previewSeq
  previewData.value = []
  previewVisible.value = true
  try {
    const blob = await previewPaper(row.id)
    if (mySeq !== previewSeq) return // 已被更新的预览请求取代
    const buf = await blob.arrayBuffer()
    previewData.value = Array.from(new Uint8Array(buf))
  } catch (e) {
    if (mySeq !== previewSeq) return
    ElMessage.error('预览失败')
    previewVisible.value = false
  }
}

function onPreviewClosed() {
  previewData.value = []
}

let compareSeq = 0

async function compare(row) {
  previewVisible.value = false // 与预览弹窗互斥
  const mySeq = ++compareSeq
  compareBefore.value = []
  compareAfter.value = []
  diffItems.value = []
  activeDiff.value = -1
  compareName.value = fileName(row)
  compareVisible.value = true
  try {
    const [beforeBlob, afterBlob] = await Promise.all([
      downloadPaperOriginal(row.id),
      previewPaper(row.id)
    ])
    if (mySeq !== compareSeq) return
    const [beforeBuf, afterBuf] = await Promise.all([
      beforeBlob.arrayBuffer(),
      afterBlob.arrayBuffer()
    ])
    if (mySeq !== compareSeq) return
    compareBefore.value = Array.from(new Uint8Array(beforeBuf))
    compareAfter.value = Array.from(new Uint8Array(afterBuf))
    // 差异分析: 失败不阻塞对比预览
    getDiff(row.id).then(res => {
      if (mySeq !== compareSeq) return
      diffItems.value = res || []
    }).catch(() => {})
  } catch (e) {
    if (mySeq !== compareSeq) return
    ElMessage.error('对比加载失败：' + (e.message || ''))
    compareVisible.value = false
  }
}

function gotoDiff(d, i) {
  activeDiff.value = i
  if (pdfViewerRef.value && d.page) pdfViewerRef.value.gotoPage(d.page)
  // 排版前内容同步定位到对应段落(格式未变文本相同)
  if (docxCompareRef.value && d.text) docxCompareRef.value.scrollToText(d.text)
}

function onCompareClosed() {
  compareBefore.value = []
  compareAfter.value = []
  diffItems.value = []
  activeDiff.value = -1
}

function showError(row) {
  errorTaskId.value = row.id
  errorMsg.value = row.errorMsg || '未知错误'
  errorVisible.value = true
}

async function retry(row) {
  try {
    await startFormat(row.fileId, row.templateId)
    ElMessage.success('已重新提交')
    // 立即刷新任务列表显示新任务
    await loadTasks(true)
    // 若有处理中的任务则轮询, 否则等待完成后刷新
    if (tasks.value.some(t => t.status === 'PROCESSING' || t.status === 'PENDING')) {
      startPolling()
    }
  } catch (e) {
    ElMessage.error('重试失败：' + (e.message || ''))
  }
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
.compare-layout {
  display: flex;
  gap: 0;
  height: 72vh;
}
.compare-diff {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}
.compare-badge.diff {
  background: #fee2e2;
  color: #b91c1c;
}
.diff-count {
  font-size: 12px;
  color: #909399;
}
.diff-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.diff-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.diff-item:hover {
  border-color: #f56c6c;
  background: #fff5f5;
}
.diff-item.active {
  border-color: #f56c6c;
  background: #fef0f0;
  box-shadow: 0 0 0 1px #f56c6c inset;
}
.diff-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.diff-type {
  font-size: 11px;
  color: #b91c1c;
  background: #fee2e2;
  padding: 1px 6px;
  border-radius: 4px;
}
.diff-page {
  font-size: 11px;
  color: #909399;
}
.diff-text {
  font-size: 12px;
  color: #303133;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.compare-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.compare-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}
.compare-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}
.compare-badge.before {
  background: #fef3c7;
  color: #b45309;
}
.compare-badge.after {
  background: #d1fae5;
  color: #047857;
}
.compare-name {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.compare-body {
  flex: 1;
  min-height: 0;
}
.compare-divider {
  width: 2px;
  background: #ebeef5;
  flex-shrink: 0;
}
.fail-tag {
  cursor: pointer;
}
.error-label {
  color: #606266;
  font-size: 14px;
  margin: 0 0 10px;
}
.error-box {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.7;
  padding: 12px 16px;
  word-break: break-all;
  max-height: 240px;
  overflow: auto;
}
.error-tip {
  color: #909399;
  font-size: 13px;
  margin: 12px 0 0;
}
</style>
