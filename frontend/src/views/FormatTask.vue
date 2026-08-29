<template>
  <div class="page">
    <SiteNav />

    <main class="content">
      <section ref="uploadSectionRef" class="upload-box">
        <el-upload
          drag
          multiple
          :auto-upload="false"
          :limit="10"
          accept=".docx"
          :on-change="onFileChange"
          :on-remove="onFileRemove"
          :file-list="fileList"
          style="width: 100%"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将论文拖到此处，或<em>点击上传</em>（仅支持 .docx，可一次选择多篇）</div>
        </el-upload>
        <div class="format-row">
          <span>选择格式方案：</span>
          <el-select v-model="templateId" placeholder="请选择格式方案" style="width: 260px">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
            <template #empty>
              <div style="padding:10px 12px;font-size:13px;color:#909399;line-height:1.6">
                <div>还没有格式方案</div>
                <router-link to="/templates" style="color:#409eff;font-weight:600">去创建一个 →</router-link>
              </div>
            </template>
          </el-select>
          <el-button type="primary" :loading="submitting" @click="submit">批量排版</el-button>
          <span v-if="selectedFiles.length" class="file-count">已选 {{ selectedFiles.length }} 篇</span>
        </div>
      </section>

      <section class="task-list">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0">历史任务</h3>
          <div class="task-batch">
            <el-button size="small" type="success" :disabled="!selectedTasks.length || batchDownloading" @click="downloadBatch">批量下载{{ selectedTasks.length ? `（${selectedTasks.length}）` : '' }}</el-button>
            <el-button size="small" type="danger" plain :disabled="!selectedTasks.length || batchDeleting" @click="removeTasks">批量删除{{ selectedTasks.length ? `（${selectedTasks.length}）` : '' }}</el-button>
          </div>
        </div>
        <div class="task-filter">
          <el-radio-group v-model="statusFilter" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="processing">处理中</el-radio-button>
            <el-radio-button label="SUCCESS">已完成</el-radio-button>
            <el-radio-button label="FAILED">失败</el-radio-button>
          </el-radio-group>
          <el-checkbox v-model="groupByPaper" size="small" style="margin-left: 8px">按论文分组(版本历史)</el-checkbox>
          <el-checkbox :model-value="notifyEnabled" size="small" style="margin-left: 4px" @change="toggleNotify" title="浏览器桌面通知, 排版完成/失败时提醒">桌面通知</el-checkbox>
          <el-input v-model="taskKeyword" placeholder="搜索论文文件名" clearable :prefix-icon="Search" style="width: 220px" />
          <span class="task-count">{{ filteredTasks.length }} / {{ tasks.length }}</span>
        </div>
        <el-table :data="pagedTasks" :span-method="taskSpan" @selection-change="handleSelectionChange">
          <template #empty>
            <el-empty description="还没有排版任务：先在上方上传论文，选一个格式方案开始">
              <el-button type="primary" @click="scrollToUpload">去上传论文</el-button>
              <el-button @click="$router.push('/templates')">去配置格式方案</el-button>
            </el-empty>
          </template>
          <el-table-column type="selection" width="44" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="论文" min-width="140">
            <template #default="{ row }">
              {{ fileName(row) }}
              <div v-if="groupByPaper" class="version-sub">{{ paperVersionLabel(row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="格式方案" min-width="140">
            <template #default="{ row }">{{ templateName(row) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <span v-if="row.teamId && teamMap[row.teamId]" class="src-team">团队</span>
              <span v-else class="src-mine">个人</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
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
              <div v-if="row.stageText && (row.status === 'PROCESSING' || row.status === 'PENDING')" class="stage-text">{{ row.stageText }}</div>
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
          <el-table-column label="操作" width="420" fixed="right">
            <template #default="{ row }">
              <div class="op-row">
                <el-button v-if="row.status === 'SUCCESS'" size="small" @click="preview(row)">预览</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="success" @click="compare(row)">对比</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="warning" plain @click="openReport(row)">报告</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="primary" @click="download(row)">下载</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" plain @click="reformatSame(row)" title="用同一篇论文和同一模板再排一次">再排一次</el-button>
                <el-button v-if="row.status === 'FAILED'" size="small" @click="retry(row)">重试</el-button>
                <el-button v-if="row.status === 'SUCCESS' || row.status === 'FAILED'" size="small" plain @click="openRerun(row)">换模板</el-button>
                <el-button size="small" type="danger" plain @click="removeTask(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="filteredTasks.length > taskPageSize" v-model:current-page="taskPage" :page-size="taskPageSize" :total="filteredTasks.length" layout="prev, pager, next" class="pager" />
      </section>

      <section class="file-list">
        <h3>我的上传文档</h3>
        <el-table :data="files" empty-text="暂无上传文档">
          <el-table-column label="文件名" min-width="180">
            <template #default="{ row }">{{ row.originalName }}</template>
          </el-table-column>
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="关联任务" width="100">
            <template #default="{ row }">{{ row.taskCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="上传时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="removeFile(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

    <el-dialog v-model="previewVisible" title="排版结果预览" width="80%" top="4vh" destroy-on-close @closed="onPreviewClosed">
      <div style="height: 72vh">
        <DocxCompare v-if="previewData.length" :key="'pv' + previewRenderKey" :data="previewData" :headings="previewHeadings" />
        <el-empty v-else description="加载中..." />
      </div>
    </el-dialog>

    <!-- 换模板重排 -->
    <el-dialog v-model="rerunVisible" title="换模板重排" width="440px">
      <p class="rerun-tip">将用同一篇论文，按新选择的模板重新排版，生成一个新任务（原任务保留）。</p>
      <el-select v-model="rerunTemplateId" placeholder="选择模板" style="width: 100%">
        <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="rerunVisible = false">取消</el-button>
        <el-button type="primary" @click="doRerun">提交排版</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="compareVisible" title="排版前后对比" width="94%" top="3vh" destroy-on-close @closed="onCompareClosed">
      <div class="compare-layout">
        <div class="compare-diff">
          <div class="compare-header">
            <span class="compare-badge diff">差异索引</span>
            <span class="diff-count">{{ diffItems.length }} 处</span>
          </div>
          <div class="diff-toolbar">
            <el-select v-model="selectedType" size="small" placeholder="全部类型" clearable style="width: 120px">
              <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <div class="diff-list">
            <div v-if="diffLoading" class="diff-loading">
              <el-icon class="is-loading" :size="24"><Loading /></el-icon>
              <span>差异对比中...</span>
            </div>
            <template v-else>
              <el-empty v-if="!diffGroups.length" description="未发现差异" :image-size="60" />
              <div v-for="g in diffGroups" :key="g.key" class="diff-group">
                <div class="diff-group-title" @click="toggleGroup(g.key)">
                  <span class="diff-group-name">{{ g.key }}</span>
                  <span class="diff-group-count">{{ g.items.length }}</span>
                  <span class="diff-group-arrow">{{ collapsed[g.key] ? '▸' : '▾' }}</span>
                </div>
                <div v-show="!collapsed[g.key]" class="diff-group-items">
                  <div v-for="d in g.items" :key="d.index" class="diff-item" :class="{ active: activeDiff === d.index }" @click="gotoDiff(d)">
                    <div class="diff-item-top">
                      <span class="diff-index">#{{ d.index }}</span>
                    </div>
                    <div class="diff-change" v-if="d.change">{{ d.change }}</div>
                    <div class="diff-text">{{ d.text }}</div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
        <div class="compare-divider"></div>
        <div class="compare-col">
          <div class="compare-header">
            <span class="compare-badge before">排版前</span>
            <span class="compare-name">{{ compareName }}</span>
          </div>
          <div class="compare-body">
            <DocxCompare v-if="compareBefore.length" ref="docxCompareRef" :key="'dc' + compareRenderKey" :data="compareBefore" />
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
            <DocxCompare v-if="compareAfter.length" ref="docxAfterRef" :key="'cp' + compareRenderKey" :data="compareAfter" :headings="compareHeadings" :on-heading-jump="onHeadingJump" />
            <el-empty v-else description="加载中..." />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 排版体检报告 + 疑似标题引导修复 -->
    <el-dialog v-model="reportVisible" title="排版体检报告" width="720px" top="7vh">
      <div v-loading="reportLoading" style="min-height: 120px">
        <template v-if="reportData">
          <div class="report-stats">
            <div class="stat-chip"><b>{{ reportData.chapterCount }}</b><span>章</span></div>
            <div class="stat-chip"><b>{{ reportData.sectionCount }}</b><span>节(二三级标题)</span></div>
            <div class="stat-chip"><b>{{ reportData.figureCount }}</b><span>图</span></div>
            <div class="stat-chip"><b>{{ reportData.tableCount }}</b><span>表</span></div>
            <div class="stat-chip"><b>{{ reportData.referenceCount }}</b><span>参考文献</span></div>
            <div class="stat-chip"><b>{{ reportData.bodyParagraphs }}</b><span>正文段落</span></div>
          </div>
          <div v-if="reportData.autoFixedHeadings" class="report-note ok-note">
            引擎已按无编号标题启发式自动识别 {{ reportData.autoFixedHeadings }} 处下级标题(如"研究背景"等短行)。
          </div>
          <template v-if="reportData.suspects && reportData.suspects.length">
            <div class="report-note warn-note">
              以下 {{ reportData.suspects.length }} 行形似标题，但未匹配模板的标题正则，已按正文排版。可为它们指定标题级别后一键重排：
            </div>
            <el-table :data="reportData.suspects" size="small" max-height="300">
              <el-table-column prop="text" label="段落内容" min-width="260" show-overflow-tooltip />
              <el-table-column label="猜测级别" width="90">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ levelName(row.guessedLevel) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="处理方式" width="150">
                <template #default="{ row }">
                  <el-select v-model="overrideMap[row.index]" size="small" style="width: 130px">
                    <el-option label="保持正文" :value="0" />
                    <el-option label="按一级标题" :value="1" />
                    <el-option label="按二级标题" :value="2" />
                    <el-option label="按三级标题" :value="3" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          <el-collapse v-if="reportData.usedConfig" style="margin-top: 10px">
            <el-collapse-item title="本次排版使用的格式参数(快照)">
              <div class="snapshot-block">
                <div class="snapshot-row"><span class="snapshot-key">标题正则</span>
                  <span>{{ reportData.usedConfig.heading1Pattern }} / {{ reportData.usedConfig.heading2Pattern }} / {{ reportData.usedConfig.heading3Pattern }}</span>
                </div>
                <div v-for="(r, type) in reportData.usedConfig.rules" :key="type" class="snapshot-row">
                  <span class="snapshot-key">{{ ruleTypeName(type) }}</span>
                  <span>{{ r.font || '—' }} {{ r.fontSize ? r.fontSize + 'pt' : '' }}{{ r.bold ? ' 加粗' : '' }}{{ r.align ? ' · ' + r.align : '' }}</span>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
          <div style="text-align: right; margin-top: 12px; display: flex; justify-content: flex-end; gap: 8px">
            <el-button @click="exportReport">导出检查报告(.txt)</el-button>
            <el-button v-if="reportData.suspects && reportData.suspects.length" type="primary" :loading="refineLoading" @click="applyOverrides">按以上设置重新排版(生成新任务)</el-button>
          </div>
          </template>
          <el-empty v-else-if="!reportLoading" description="未发现疑似标题问题，结构识别良好" :image-size="60" />
        </template>
        <el-empty v-else-if="!reportLoading" description="该任务没有体检报告（旧任务生成于报告功能上线前，重新排版即可获得）" />
      </div>
    </el-dialog>

    <el-dialog v-model="errorVisible" title="排版失败原因与自助处理" width="560px" top="10vh">
      <div class="error-detail">
        <p class="error-label">任务 #{{ errorTaskId }} 排版失败，原因如下：</p>
        <div class="error-box">{{ errorMsg }}</div>
        <p class="error-label" style="margin-top: 12px">下一步建议：</p>
        <ul class="error-advice">
          <li v-for="(a, i) in errorAdvice" :key="i">{{ a }}</li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { UploadFilled, Loading, Search } from '@element-plus/icons-vue'
import { listTemplates } from '../api/template'
import { listTeams } from '../api/team'
import { uploadPaper, startFormat, startFormatBatch, listTasks, getTask, downloadPaper, downloadPaperBatch, downloadPaperOriginal, getDiff, progressTicket, deleteTask, deleteTaskBatch, listFiles, deleteFile, getTaskReport, refineTask } from '../api/paper'
import DocxCompare from '../components/DocxCompare.vue'
import { extractHeadings } from '../utils/docxHeadings'
import SiteNav from '../components/SiteNav.vue'

const route = useRoute()
const templates = ref([])
const templateId = ref(route.query.templateId ? Number(route.query.templateId) : null)
const teamMap = ref({})
const fileList = ref([])
const selectedFile = ref(null)
const selectedFiles = ref([])
const submitting = ref(false)
const tasks = ref([])
const files = ref([])
const statusFilter = ref('')
const taskKeyword = ref('')
const groupByPaper = ref(false)
const taskPage = ref(1)
const taskPageSize = 10
const filteredTasks = computed(() => {
  const kw = taskKeyword.value.trim().toLowerCase()
  return tasks.value.filter(t => {
    if (statusFilter.value) {
      if (statusFilter.value === 'processing' && !(t.status === 'PENDING' || t.status === 'PROCESSING')) return false
      if (statusFilter.value === 'SUCCESS' && t.status !== 'SUCCESS') return false
      if (statusFilter.value === 'FAILED' && t.status !== 'FAILED') return false
    }
    if (kw && !(fileName(t) || '').toLowerCase().includes(kw)) return false
    return true
  })
})
const pagedTasks = computed(() => {
  // 按论文分组查看: 同一篇论文的多次排版(版本历史)聚在一起, 新版本在上
  let list = filteredTasks.value
  if (groupByPaper.value) {
    list = [...list].sort((a, b) => (a.fileId - b.fileId) || ((b.createTime || '').localeCompare(a.createTime || '')))
  }
  const start = (taskPage.value - 1) * taskPageSize
  return list.slice(start, start + taskPageSize)
})

// 按论文分组时合并"论文"列单元格
function taskSpan({ row, rowIndex, column }) {
  if (!groupByPaper.value || column.label !== '论文') return
  const list = pagedTasks.value
  if (rowIndex > 0 && list[rowIndex - 1].fileId === row.fileId) {
    return { rowspan: 0, colspan: 0 }
  }
  let span = 1
  for (let i = rowIndex + 1; i < list.length && list[i].fileId === row.fileId; i++) span++
  return { rowspan: span, colspan: 1 }
}
watch([statusFilter, taskKeyword], () => { taskPage.value = 1 })
const previewVisible = ref(false)
const previewData = ref([])
const previewHeadings = ref([])
const compareVisible = ref(false)
const compareBefore = ref([])
const compareAfter = ref([])
const compareHeadings = ref([])
const compareName = ref('')
const diffItems = ref([])
const diffLoading = ref(false)
const activeDiff = ref(-1)
const docxCompareRef = ref(null)
const docxAfterRef = ref(null)
// 差异索引: 筛选 + 分组折叠
const selectedType = ref('')
const collapsed = ref({})
const typeOptions = computed(() => [...new Set(diffItems.value.map(d => d.type))])
const diffGroups = computed(() => {
  const list = selectedType.value ? diffItems.value.filter(d => d.type === selectedType.value) : diffItems.value
  const map = {}
  list.forEach(d => { (map[d.type] = map[d.type] || []).push(d) })
  return Object.keys(map).map(k => ({ key: k, items: map[k] }))
})
function toggleGroup(key) {
  collapsed.value[key] = !collapsed.value[key]
}
const errorVisible = ref(false)
const errorMsg = ref('')
const errorTaskId = ref(null)
// 每次打开预览/对比递增, 强制子组件重建, 避免多次点击残留多个渲染
const previewRenderKey = ref(0)
const compareRenderKey = ref(0)
let pollTimer = null
// SSE 进度推送连接: taskId -> EventSource
const sseMap = new Map()

// ===== 浏览器桌面通知: 长任务结束不用盯着页面 =====
const notifyEnabled = ref(localStorage.getItem('notify-browser') === '1')
const uploadSectionRef = ref(null)

function scrollToUpload() {
  if (uploadSectionRef.value) {
    uploadSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function toggleNotify(v) {
  notifyEnabled.value = !!v
  localStorage.setItem('notify-browser', notifyEnabled.value ? '1' : '0')
  if (notifyEnabled.value && 'Notification' in window && Notification.permission !== 'granted') {
    Notification.requestPermission()
  }
}

function notifyBrowser(title, body) {
  if (!notifyEnabled.value || !('Notification' in window) || Notification.permission !== 'granted') return
  try {
    new Notification(title, { body })
  } catch (e) {}
}

async function subscribeSse(taskId) {  if (sseMap.has(taskId)) return
  let ticket
  try {
    const data = await progressTicket(taskId)
    ticket = data?.ticket
  } catch (e) { return }
  if (!ticket) return
  const es = new EventSource(`/api/paper/task/${taskId}/progress?ticket=${encodeURIComponent(ticket)}`)
  sseMap.set(taskId, es)
  es.addEventListener('progress', ev => {
    try {
      const d = JSON.parse(ev.data)
      const row = tasks.value.find(t => t.id === taskId)
      if (row) {
        if (d.progress !== undefined) row.progress = d.progress
        if (d.status) row.status = d.status
        if (d.stageText) row.stageText = d.stageText
        if (d.error) row.errorMsg = d.error
      }
      // 排版结束: 关闭连接、刷新列表并弹出结果提示
      if (d.status === 'SUCCESS' || d.status === 'FAILED') {
        closeSse(taskId)
        loadTasks(true)
        const name = row ? fileName(row) : ('任务 #' + taskId)
        if (d.status === 'SUCCESS') {
          notifyBrowser('排版完成', `「${name}」已排版完成，可预览或下载`)
          ElNotification({ title: '排版完成', message: `「${name}」已排版完成，可预览或下载`, type: 'success', duration: 6000 })
        } else {
          const errText = (d.error || '请重试').slice(0, 120)
          notifyBrowser('排版失败', errText)
          ElNotification({ title: '排版失败', message: errText, type: 'error', duration: 8000 })
        }
      }
    } catch (e) {}
  })
  es.onerror = () => closeSse(taskId)
}

function closeSse(taskId) {
  const es = sseMap.get(taskId)
  if (es) {
    es.close()
    sseMap.delete(taskId)
  }
}

/** 为所有运行中的任务订阅 SSE 进度 */
function startSse() {
  ;(tasks.value || []).forEach(t => {
    if (t.status === 'PROCESSING' || t.status === 'PENDING') {
      subscribeSse(t.id)
    }
  })
}

// 排版结果/原始 docx 内存缓存: 同一任务重复预览/对比不重复下载, 加速二次打开
const blobCache = new Map()
async function fetchDocxBlob(taskId) {
  const key = 'doc:' + taskId
  if (blobCache.has(key)) return blobCache.get(key)
  const b = await downloadPaper(taskId)
  blobCache.set(key, b)
  return b
}
async function fetchOriginalBlob(taskId) {
  const key = 'ori:' + taskId
  if (blobCache.has(key)) return blobCache.get(key)
  const b = await downloadPaperOriginal(taskId)
  blobCache.set(key, b)
  return b
}

// 差异分析结果缓存: 同一任务重复打开对比不重新计算
const diffCache = new Map()
async function fetchDiff(taskId) {
  const key = 'diff:' + taskId
  if (diffCache.has(key)) return diffCache.get(key)
  const res = await getDiff(taskId)
  diffCache.set(key, res)
  return res
}

onMounted(async () => {
  templates.value = await listTemplates()
  try {
    const ts = await listTeams() || []
    const m = {}
    ts.forEach(t => { m[t.id] = t })
    teamMap.value = m
  } catch (e) {}
  await loadTasks()
  await loadFiles()
  startSse()
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  sseMap.forEach(es => es.close())
  sseMap.clear()
})

function onFileChange(file, uploadFiles) {
  if (file.name && !/\.docx$/i.test(file.name)) {
    ElMessage.error('仅支持 .docx 文件，旧版 .doc 请先用 Word 另存为 .docx 后再上传')
    fileList.value = fileList.value.filter(f => f.uid !== file.uid)
    return
  }
  const raw = file.raw || file
  selectedFile.value = raw
  // el-upload 的 :file-list 为单向绑定, 需用回调传入的最新文件列表
  const files = uploadFiles || fileList.value
  fileList.value = files
  selectedFiles.value = files.map(f => f.raw || f)
}

function onFileRemove(file, uploadFiles) {
  const files = uploadFiles || fileList.value
  fileList.value = files
  selectedFiles.value = files.map(f => f.raw || f)
}

async function submit() {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请先上传论文文件')
    return
  }
  if (!templateId.value) {
    ElMessage.warning('请选择格式方案')
    return
  }
  submitting.value = true
  try {
    const fileIds = []
    for (const f of selectedFiles.value) {
      const paperFile = await uploadPaper(f)
      fileIds.push(paperFile.id)
    }
    await startFormatBatch(templateId.value, fileIds)
    ElMessage.success(`已提交 ${fileIds.length} 篇论文的排版任务`)
    selectedFiles.value = []
    fileList.value = []
    selectedFile.value = null
    await loadTasks(true)
    await loadFiles()
    startSse()
  } finally {
    submitting.value = false
  }
}

// 保留原轮询为 SSE 不可用时的兜底
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
  // SSE 实时推送为主, 轮询作为兜底(SSE 断开时仍能刷新)
  startSse()
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
  // 文件名: 上传文件名_模板名_时间戳.docx
  const base = (row.originalName || fileName(row)).replace(/\.[^.]+$/, '')
  const tpl = templateName(row)
  const ts = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/[\/\s:]/g, '').slice(0, 14)
  a.download = `${base}_${tpl}_${ts}.docx`
  a.click()
  URL.revokeObjectURL(url)
}

const selectedTasks = ref([])
const batchDownloading = ref(false)
const batchDeleting = ref(false)
function handleSelectionChange(rows) {
  selectedTasks.value = rows
}
async function downloadBatch() {
  if (!selectedTasks.value.length) return
  const ids = selectedTasks.value.map(t => t.id)
  batchDownloading.value = true
  try {
    const blob = await downloadPaperBatch(ids)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `已排版论文批量_${new Date().toLocaleString('zh-CN', { hour12: false }).replace(/[\/\s:]/g, '')}.zip`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已批量下载 ${ids.length} 篇`)
  } catch (e) {
    ElMessage.error('批量下载失败')
  } finally {
    batchDownloading.value = false
  }
}

let previewSeq = 0

async function preview(row) {
  compareVisible.value = false // 与对比弹窗互斥
  const mySeq = ++previewSeq
  previewRenderKey.value++
  previewData.value = []
  previewHeadings.value = []
  previewVisible.value = true
  try {
    const blob = await fetchDocxBlob(row.id)
    if (mySeq !== previewSeq) return // 已被更新的预览请求取代
    const buf = await blob.arrayBuffer()
    previewData.value = Array.from(new Uint8Array(buf))
    previewHeadings.value = await extractHeadings(buf)
  } catch (e) {
    if (mySeq !== previewSeq) return
    ElMessage.error('预览失败')
    previewVisible.value = false
  }
}

function onPreviewClosed() {
  previewData.value = []
  previewHeadings.value = []
}

let compareSeq = 0

async function compare(row) {
  previewVisible.value = false // 与预览弹窗互斥
  const mySeq = ++compareSeq
  compareRenderKey.value++
  compareBefore.value = []
  compareAfter.value = []
  diffItems.value = []
  diffLoading.value = true
  activeDiff.value = -1
  compareName.value = fileName(row)
  compareVisible.value = true
  try {
    const [beforeBlob, afterBlob] = await Promise.all([
      fetchOriginalBlob(row.id),
      fetchDocxBlob(row.id)
    ])
    if (mySeq !== compareSeq) return
    const [beforeBuf, afterBuf] = await Promise.all([
      beforeBlob.arrayBuffer(),
      afterBlob.arrayBuffer()
    ])
    if (mySeq !== compareSeq) return
    compareBefore.value = Array.from(new Uint8Array(beforeBuf))
    compareAfter.value = Array.from(new Uint8Array(afterBuf))
    compareHeadings.value = await extractHeadings(afterBuf)
    // 差异分析: 失败不阻塞对比预览, 结果按任务缓存(重复打开不重算)
    fetchDiff(row.id).then(res => {
      if (mySeq !== compareSeq) return
      diffItems.value = res || []
      if (diffItems.value.length) {
        ElMessage({
          type: 'success',
          message: `已发现 ${diffItems.value.length} 处格式差异，见左侧「差异索引」，点击可定位到左右两侧对应位置`,
          duration: 3000
        })
      }
    }).catch(() => {})
      .finally(() => { if (mySeq === compareSeq) diffLoading.value = false })
  } catch (e) {
    if (mySeq !== compareSeq) return
    diffLoading.value = false
    ElMessage.error('对比加载失败：' + (e.message || ''))
    compareVisible.value = false
  }
}

/** 目录跳转: 同时滚动排版前/排版后两侧到对应标题 */
function onHeadingJump(text) {
  if (docxCompareRef.value) docxCompareRef.value.scrollToText(text)
  if (docxAfterRef.value) docxAfterRef.value.scrollToText(text)
}

function gotoDiff(d) {
  activeDiff.value = d.index
  // 排版前 + 排版后两侧都定位到对应段落并高亮(文本相同, 用文本前缀匹配)
  if (docxCompareRef.value && d.text) docxCompareRef.value.scrollToText(d.text)
  if (docxAfterRef.value && d.text) docxAfterRef.value.scrollToText(d.text)
}

function onCompareClosed() {
  compareBefore.value = []
  compareAfter.value = []
  compareHeadings.value = []
  diffItems.value = []
  activeDiff.value = -1
}

function showError(row) {
  errorTaskId.value = row.id
  errorMsg.value = row.errorMsg || '未知错误'
  errorVisible.value = true
}

// ===== 排版体检报告 + 疑似标题引导修复 =====
const reportVisible = ref(false)
const reportLoading = ref(false)
const reportData = ref(null)
const reportTaskId = ref(null)
const overrideMap = ref({})
const refineLoading = ref(false)

function levelName(lv) {
  return lv === 1 ? '一级' : lv === 2 ? '二级' : lv === 3 ? '三级' : (lv || '?') + ' 级'
}

function ruleTypeName(type) {
  const names = { heading1: '一级标题', heading2: '二级标题', heading3: '三级标题', body: '正文', figure: '图题注', table: '表题注', tableText: '表格文字' }
  return names[type] || type
}

/** 导出格式检查报告(.txt): 结构概况 + 疑似标题 + 排版参数快照 */
function exportReport() {
  const r = reportData.value
  if (!r) return
  const task = tasks.value.find(t => t.id === reportTaskId.value)
  const lines = []
  lines.push('==== 排版检查报告 ====')
  lines.push('论文: ' + (task ? (fileName(task) || '') : ''))
  lines.push('格式方案: ' + (task ? templateName(task) : ''))
  lines.push('生成时间: ' + new Date().toLocaleString())
  lines.push('')
  lines.push('一、结构概况')
  lines.push(`  章: ${r.chapterCount}    二三级标题: ${r.sectionCount}    图: ${r.figureCount}    表: ${r.tableCount}`)
  lines.push(`  参考文献条目: ${r.referenceCount}    正文段落: ${r.bodyParagraphs}`)
  if (r.autoFixedHeadings) {
    lines.push(`  已按无编号标题启发式自动识别 ${r.autoFixedHeadings} 处下级标题`)
  }
  lines.push('')
  if (r.suspects && r.suspects.length) {
    lines.push(`二、疑似未匹配标题(${r.suspects.length} 处, 已按正文排版, 建议核对)`)
    r.suspects.forEach((s, i) => {
      lines.push(`  ${i + 1}. [建议${levelName(s.guessedLevel)}] ${s.text}`)
    })
    lines.push('  处理方式: 在本报告中为这些行指定标题级别后点「重新排版」; 或在模板配置页调整标题正则。')
  } else {
    lines.push('二、疑似未匹配标题: 未发现')
  }
  if (r.usedConfig) {
    lines.push('')
    lines.push('三、本次排版使用的格式参数')
    lines.push(`  标题正则: ${r.usedConfig.heading1Pattern} / ${r.usedConfig.heading2Pattern} / ${r.usedConfig.heading3Pattern}`)
    if (r.usedConfig.rules) {
      Object.keys(r.usedConfig.rules).forEach(type => {
        const rr = r.usedConfig.rules[type]
        lines.push(`  ${ruleTypeName(type)}: ${rr.font || '—'} ${rr.fontSize ? rr.fontSize + 'pt' : ''}${rr.bold ? ' 加粗' : ''}${rr.align ? ' ' + rr.align : ''}`)
      })
    }
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '排版检查报告.txt'
  a.click()
  URL.revokeObjectURL(url)
}

async function openReport(row) {
  reportVisible.value = true
  reportLoading.value = true
  reportData.value = null
  overrideMap.value = {}
  reportTaskId.value = row.id
  try {
    const r = await getTaskReport(row.id)
    reportData.value = r
    if (r && r.suspects) {
      // 默认按引擎猜测级别预选, 用户可改
      const m = {}
      r.suspects.forEach(s => { m[s.index] = s.guessedLevel })
      overrideMap.value = m
    }
  } catch (e) {
    // 具体原因由 api 拦截器提示
  }
  reportLoading.value = false
}

async function applyOverrides() {
  const overrides = Object.entries(overrideMap.value)
    .filter(([, lv]) => Number(lv) > 0)
    .map(([idx, lv]) => ({ index: Number(idx), level: Number(lv) }))
  if (!overrides.length) {
    ElMessage.info('未选择任何要改为标题的行')
    return
  }
  refineLoading.value = true
  try {
    await refineTask(reportTaskId.value, overrides)
    ElMessage.success('已创建新任务，正在按指定标题级别重新排版')
    reportVisible.value = false
    loadTasks()
  } catch (e) {
    // 具体原因由 api 拦截器提示
  }
  refineLoading.value = false
}

// ===== 失败原因自助指引: 按错误特征给出下一步建议 =====
const errorAdvice = computed(() => {
  const m = errorMsg.value || ''
  const low = m.toLowerCase()
  const list = []
  if (low.includes('encrypt') || m.includes('密码') || m.includes('加密')) {
    list.push('文档受密码保护：用 Word/WPS「另存为」时取消密码，再重新上传排版')
  }
  if (m.includes('过大') || m.includes('40MB') || m.includes('拆分')) {
    list.push('文档过大：删除不需要的高清图片/附录，或按章节拆分成多个文档分别排版')
  }
  if (m.includes('图片')) {
    list.push('文档中存在无法读取的图片：将图片另存为 JPG/PNG 后重新插入文档再试')
  }
  if (m.includes('docx') || m.includes('解析') || m.includes('有效') || m.includes('损坏')) {
    list.push('文件可能损坏或格式不对：用 Word/WPS 打开后「另存为 .docx」再重新上传')
  }
  if (m.includes('内存') || m.includes('OutOfMemory') || m.includes('超出') || m.includes('过大或过于复杂')) {
    list.push('文档过于复杂：压缩图片尺寸，或按章节拆分后分批排版')
  }
  list.push('可先在模板配置页用「快速试排」上传该文档，秒级定位出问题的段落')
  list.push('若反复失败，可点任务列表的「重试」(系统失败后会自动重试一次)，或更换其他模板')
  return list
})

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

// 换模板重排: 用同一篇论文按新模板生成新任务
const rerunVisible = ref(false)
const rerunRow = ref(null)
const rerunTemplateId = ref(null)
function openRerun(row) {
  rerunRow.value = row
  rerunTemplateId.value = row.templateId
  rerunVisible.value = true
}
async function doRerun() {
  if (!rerunTemplateId.value) {
    ElMessage.warning('请选择模板')
    return
  }
  try {
    await startFormat(rerunRow.value.fileId, rerunTemplateId.value)
    ElMessage.success('已提交新排版任务')
    rerunVisible.value = false
    await loadTasks(true)
    startSse()
  } catch (e) {
    ElMessage.error('提交失败：' + (e.message || ''))
  }
}

async function loadFiles() {
  try {
    files.value = await listFiles()
  } catch (e) {}
}

async function removeTask(row) {
  try {
    await ElMessageBox.confirm(`确定删除任务 #${row.id}（${fileName(row)}）？该任务的结果文件将一并删除，若原文档无其他任务引用也会一并删除。`, '删除任务', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteTask(row.id)
    ElMessage.success('已删除任务')
    await loadTasks(true)
    await loadFiles()
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || ''))
  }
}

async function removeTasks() {
  if (!selectedTasks.value.length) return
  const ids = selectedTasks.value.map(t => t.id)
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${ids.length} 个任务？结果文件将一并删除，若原文档无其他任务引用也会一并删除。`, '批量删除任务', { type: 'warning' })
  } catch (e) { return }
  batchDeleting.value = true
  try {
    await deleteTaskBatch(ids)
    ElMessage.success(`已删除 ${ids.length} 个任务`)
    selectedTasks.value = []
    await loadTasks(true)
    await loadFiles()
  } catch (e) {
    ElMessage.error('批量删除失败：' + (e.message || ''))
  } finally {
    batchDeleting.value = false
  }
}

async function removeFile(row) {
  const n = row.taskCount || 0
  try {
    await ElMessageBox.confirm(n > 0 ? `确定删除文档「${row.originalName}」？该文档有 ${n} 个关联任务，将一并删除（含结果文件）。` : `确定删除文档「${row.originalName}」？`, '删除文档', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteFile(row.id)
    ElMessage.success('已删除文档')
    await loadFiles()
    await loadTasks(true)
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || ''))
  }
}

function formatSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function fileName(row) {
  return row.originalName || (row.fileId + '.docx')
}

function templateName(row) {
  const t = templates.value.find(x => x.id === row.templateId)
  return t ? t.name : `#${row.templateId}`
}

// ===== 版本历史: 按论文分组的版本序号标注 =====
function paperVersionLabel(row) {
  const siblings = tasks.value.filter(t => t.fileId === row.fileId).sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
  const n = siblings.findIndex(t => t.id === row.id) + 1
  return `第 ${n} 次排版 · ${templateName(row)}`
}

/** 用同一篇论文 + 同一模板再排一次 */
async function reformatSame(row) {
  try {
    await ElMessageBox.confirm('将用同一篇论文和同一模板再排一次，生成新任务（原任务保留）。', '再排一次', { type: 'info', confirmButtonText: '开始排版', cancelButtonText: '取消' })
  } catch (e) {
    return
  }
  try {
    await startFormat(row.fileId, row.templateId)
    ElMessage.success('已创建新任务')
    loadTasks()
  } catch (e) {
    // 具体原因由 api 拦截器提示
  }
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
  width: 100%;
  margin: 24px 0;
  padding: 0 30px;
  box-sizing: border-box;
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
.file-count {
  font-size: 13px;
  color: #409eff;
}
.stage-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.rerun-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
}
.src-team {
  display: inline-block;
  font-size: 11px;
  color: #3B6BFF;
  background: #EEF1FF;
  border-radius: 999px;
  padding: 2px 9px;
}
.src-mine {
  font-size: 12px;
  color: #c0c4cc;
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
.task-batch {
  display: flex;
  gap: 8px;
}
.task-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.task-count {
  font-size: 13px;
  color: #909399;
}
.pager {
  margin-top: 16px;
  justify-content: center;
  display: flex;
}
.file-list {
  margin-top: 24px;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.file-list h3 {
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
.diff-toolbar {
  padding: 8px 10px 4px;
}
.diff-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 10px 10px;
}
.diff-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 0;
  color: #b91c1c;
  font-size: 13px;
}
.diff-loading .el-icon {
  animation: rotating 1s linear infinite;
}
@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.diff-group {
  margin-bottom: 6px;
}
.diff-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 6px;
  background: #f7f8fa;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  user-select: none;
}
.diff-group-title:hover {
  background: #f0f2f5;
}
.diff-group-name {
  color: #b91c1c;
}
.diff-group-count {
  color: #909399;
  font-weight: 400;
}
.diff-group-arrow {
  margin-left: auto;
  color: #909399;
}
.diff-group-items {
  padding-top: 6px;
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
.diff-index {
  font-size: 11px;
  color: #606266;
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 4px;
}
.diff-change {
  font-size: 12px;
  color: #b91c1c;
  line-height: 1.5;
  margin-bottom: 3px;
  word-break: break-all;
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
  overflow: hidden;
}
.compare-divider {
  width: 2px;
  background: #ebeef5;
  flex-shrink: 0;
}
.fail-tag {
  cursor: pointer;
}
.op-row {
  display: flex;
  gap: 6px;
  white-space: nowrap;
  flex-wrap: nowrap;
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
/* 排版体检报告 */
.report-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.stat-chip {
  flex: 1;
  min-width: 88px;
  text-align: center;
  padding: 10px 6px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafbfc;
}
.stat-chip b {
  display: block;
  font-size: 22px;
  color: #409eff;
}
.stat-chip span {
  font-size: 12px;
  color: #909399;
}
.report-note {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 10px;
}
.ok-note {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}
.warn-note {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}
/* 失败自助指引 */
.error-advice {
  margin: 6px 0 0;
  padding-left: 18px;
}
.error-advice li {
  font-size: 13px;
  color: #606266;
  line-height: 1.9;
}
/* 版本历史: 按论文分组的版本标注 */
.version-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
/* 报告: 排版参数快照 */
.snapshot-block {
  font-size: 13px;
  color: #606266;
}
.snapshot-row {
  line-height: 2;
}
.snapshot-key {
  display: inline-block;
  width: 80px;
  color: #909399;
}
</style>
