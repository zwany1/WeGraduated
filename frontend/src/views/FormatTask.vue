<template>
  <WorkbenchShell title="排版任务">
    <template #meta>
      共 {{ tasks.length }} 个任务 · 进行中 {{ runningCount }} / 已完成 {{ doneCount }} / 失败 {{ failedCount }}<br />
      上传论文 → 选方案 → 批量排版，进度实时推送
    </template>

    <template #toolbar>
      <div class="flt">
        <div class="flt-chips">
          <button :class="['chip', { on: statusFilter === '' }]" type="button" @click="statusFilter = ''">全部</button>
          <button :class="['chip', { on: statusFilter === 'processing' }]" type="button" @click="statusFilter = 'processing'">处理中</button>
          <button :class="['chip', { on: statusFilter === 'SUCCESS' }]" type="button" @click="statusFilter = 'SUCCESS'">已完成</button>
          <button :class="['chip', { on: statusFilter === 'FAILED' }]" type="button" @click="statusFilter = 'FAILED'">失败</button>
          <button :class="['chip', { on: groupByPaper }]" type="button" @click="groupByPaper = !groupByPaper">按论文分组</button>
          <button :class="['chip', { on: notifyEnabled }]" type="button" @click="toggleNotify(!notifyEnabled)">桌面通知</button>
        </div>
        <label class="flt-search">
          <span class="flt-k">搜索</span>
          <input v-model="taskKeyword" type="search" placeholder="论文文件名" />
        </label>
        <span class="flt-count">{{ filteredTasks.length }} / {{ tasks.length }}</span>
      </div>
    </template>

      <section ref="uploadSectionRef" class="upload">
        <div class="upload-head">
          <h2>把论文拖进来</h2>
          <p>仅支持 .docx，一次最多 10 篇。选好格式方案后点「批量排版」，进度会实时推到这里。</p>
        </div>

        <div class="upload-drop">
          <el-upload
            ref="uploadRef"
            drag
            multiple
            :auto-upload="false"
            :limit="10"
            accept=".docx"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :file-list="fileList"
            :show-file-list="false"
            class="drop-el"
          >
            <div class="drop-inner">
              <svg viewBox="0 0 24 24" aria-hidden="true" class="drop-icon">
                <path d="M12 3v10m0 0l-4-4m4 4l4-4" />
                <path d="M4 16v3h16v-3" />
              </svg>
              <span class="drop-k">拖到这里，或点击选择文件</span>
              <span class="drop-d">.docx · 单篇不超过 50 MB</span>
            </div>
          </el-upload>
        </div>

        <div class="upload-set">
          <label class="set">
            <span class="set-k">格式方案</span>
            <el-select v-model="templateId" placeholder="选一套校规配置" style="width: 100%">
              <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
              <template #empty>
                <div class="sel-empty">
                  <div>还没有格式方案</div>
                  <router-link to="/templates">去创建一个 →</router-link>
                </div>
              </template>
            </el-select>
          </label>
          <div class="set-act">
            <span class="set-count">已选 {{ selectedFiles.length }} 篇</span>
            <button class="wbtn solid" type="button" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中…' : '批量排版' }}
            </button>
          </div>
        </div>

        <ul v-if="fileList.length" class="picked">
          <li v-for="(f, i) in fileList" :key="f.uid || i">
            <span class="picked-name">{{ f.name || f.raw?.name }}</span>
            <span class="picked-size">{{ formatSize(f.size || f.raw?.size) }}</span>
            <span class="picked-state">待提交</span>
            <button class="picked-del" type="button" title="从待提交列表移除" @click="removePicked(f)">✕</button>
          </li>
        </ul>
      </section>

      <section class="task-list">
        <div class="sec-head">
          <h2 class="sec-h">历史任务</h2>
          <div class="sec-acts">
            <button class="wbtn ghost" type="button" :disabled="!selectedTasks.length || batchDownloading" @click="downloadBatch">
              批量下载{{ selectedTasks.length ? `（${selectedTasks.length}）` : '' }}
            </button>
            <button class="wbtn ghost danger" type="button" :disabled="!selectedTasks.length || batchDeleting" @click="removeTasks">
              批量删除{{ selectedTasks.length ? `（${selectedTasks.length}）` : '' }}
            </button>
            <button class="wbtn solid" type="button" @click="scrollToUpload">上传论文</button>
          </div>
        </div>
        <el-table :data="pagedTasks" :span-method="taskSpan" @selection-change="handleSelectionChange">
          <template #empty>
            <div class="t-empty">
              <p class="t-empty-k">还没有排版任务</p>
              <p class="t-empty-d">先在上方上传论文，选一套格式方案，点「批量排版」。跑完可以预览、对比前后差异、下载成稿。</p>
              <div class="t-empty-a">
                <button class="wbtn solid" type="button" @click="scrollToUpload">去上传论文</button>
                <button class="wbtn ghost" type="button" @click="$router.push('/templates')">去配置格式方案</button>
              </div>
            </div>
          </template>
          <el-table-column type="selection" width="44" />
          <el-table-column label="论文" min-width="220">
            <template #default="{ row }">
              {{ fileName(row) }}
              <div v-if="groupByPaper" class="version-sub">{{ paperVersionLabel(row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="格式方案" min-width="180">
            <template #default="{ row }">{{ templateName(row) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="84">
            <template #default="{ row }">
              <span v-if="row.teamId && teamMap[row.teamId]" class="src-team">团队</span>
              <span v-else class="src-mine">个人</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="180">
            <template #default="{ row }">
              <div class="st-cell">
                <el-tag v-if="row.status === 'SUCCESS'" type="success">已完成</el-tag>
                <el-tag v-else-if="row.status === 'FAILED'" type="danger" class="fail-tag" @click="showError(row)">失败</el-tag>
                <el-tag v-else type="warning">处理中</el-tag>
                <el-progress
                  v-if="row.status === 'PROCESSING' || row.status === 'PENDING'"
                  :percentage="row.progress"
                  :stroke-width="6"
                />
                <div v-if="row.stageText && (row.status === 'PROCESSING' || row.status === 'PENDING')" class="stage-text">{{ row.stageText }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="300">
            <template #default="{ row }">
              <div class="op-row">
                <el-button v-if="row.status === 'SUCCESS'" size="small" @click="preview(row)">预览</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="success" @click="compare(row)">对比</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="warning" plain @click="openReport(row)">报告</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" type="primary" @click="download(row)">下载</el-button>
                <el-button v-if="row.status === 'SUCCESS'" size="small" plain @click="reformatSame(row)" title="用同一篇论文和同一模板再排一次">再排一次</el-button>
                <el-button v-if="row.status === 'FAILED'" size="small" type="primary" @click="showError(row)">查看原因</el-button>
                <el-button v-if="row.status === 'FAILED'" size="small" @click="retry(row)">重试</el-button>
                <el-button v-if="row.status === 'SUCCESS' || row.status === 'FAILED'" size="small" plain @click="openRerun(row)">换模板</el-button>
                <el-button size="small" type="danger" plain @click="removeTask(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="filteredTasks.length > taskPageSize" v-model:current-page="taskPage" :page-size="taskPageSize" :total="filteredTasks.length" layout="prev, pager, next" class="pager" />
      </section>

      <section class="file-list">
        <h2 class="sec-h">我的上传文档</h2>
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
    <template #footer>
      <span>任务完成后可预览、对比前后差异、下载成稿；失败任务支持重试与换模板重排</span>
      <span v-if="filteredTasks.length > taskPageSize" class="pager">
        <button type="button" :disabled="taskPage === 1" @click="taskPage--">上一页</button>
        <b>{{ taskPage }}</b>
        <span>/ {{ Math.ceil(filteredTasks.length / taskPageSize) }}</span>
        <button type="button" :disabled="taskPage >= Math.ceil(filteredTasks.length / taskPageSize)" @click="taskPage++">下一页</button>
      </span>
    </template>
  </WorkbenchShell>

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
                  <div v-for="d in shownItems(g)" :key="d.index" class="diff-item" :class="{ active: activeDiff === d.index }" @click="gotoDiff(d)">
                    <div class="diff-item-top">
                      <span class="diff-index">#{{ d.index }}</span>
                      <span v-if="d.where" class="diff-where">{{ d.where }}</span>
                    </div>
                    <div class="diff-change" v-if="d.change">{{ d.change }}</div>
                    <div class="diff-text">{{ d.text }}</div>
                  </div>
                  <div v-if="g.items.length > DIFF_PREVIEW" class="diff-more" @click="toggleExpand(g.key)">
                    {{ expandedGroups[g.key] ? '收起' : `展开全部 ${g.items.length} 条` }}
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
          <!-- 结构告警: 判定与文案均来自后端 structureLevel/structureMessage, 前端只渲染 -->
          <div v-if="structureAlert" class="structure-alert" :class="structureAlert.level === 'EMPTY' ? 'is-danger' : 'is-warn'">
            {{ structureAlert.message }}
          </div>
          <div v-if="reportData.autoFixedHeadings" class="report-note ok-note">
            引擎已按无编号标题启发式自动识别 {{ reportData.autoFixedHeadings }} 处下级标题(如"研究背景"等短行)。
          </div>
          <!-- 题注改号: 编号随章节顺序变了, 但正文里手写的交叉引用不会跟着变 -->
          <div v-if="reportData.renumberedCaptions" class="report-note warn-note">
            有 {{ reportData.renumberedCaptions }} 个图表题注按新的章节顺序重新编号<span
              v-if="reportData.captionRenumberSamples && reportData.captionRenumberSamples.length"
            >（{{ reportData.captionRenumberSamples.join('、') }}<span
              v-if="reportData.renumberedCaptions > reportData.captionRenumberSamples.length"> 等</span>）</span>。
            正文里"如图3-1所示"这类交叉引用<strong>不会自动更新</strong>，请逐处核对。
          </div>
          <!-- 题注位置不符: 引擎只重编号不移动段落(移动会打乱图文关系与分页), 所以提示人工调整 -->
          <div v-if="reportData.captionPositionMismatches" class="report-note warn-note">
            有 {{ reportData.captionPositionMismatches }} 处图表题注的位置与模板设置不符<span
              v-if="reportData.captionPositionSamples && reportData.captionPositionSamples.length"
            >（{{ reportData.captionPositionSamples.join('、') }}<span
              v-if="reportData.captionPositionMismatches > reportData.captionPositionSamples.length"> 等</span>）</span>。
            引擎不会自动移动题注（会打乱图文关系与分页），请手动调整，或改方案里的「题注位置」。
          </div>
          <!-- 自动识别的标题逐条列出, 作为可回溯的"改动清单"(只给数量无法核对改了哪几段) -->
          <el-collapse v-if="reportData.autoFixedHeadingItems && reportData.autoFixedHeadingItems.length" class="auto-fixed-list">
            <el-collapse-item :title="`查看被自动识别为标题的 ${reportData.autoFixedHeadingItems.length} 段(核对用)`">
              <el-table :data="reportData.autoFixedHeadingItems" size="small" max-height="220">
                <el-table-column prop="index" label="段落序号" width="90" />
                <el-table-column prop="text" label="段落内容" min-width="260" show-overflow-tooltip />
                <el-table-column label="识别级别" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ levelName(row.level) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <div class="report-hint">若这些行其实是正文，请在模板配置页调整「标题正则」后重新排版。</div>
            </el-collapse-item>
          </el-collapse>
          <!-- 无题注图表: 引擎不生成"只有编号没有题名"的裸题注, 需要作者补写题名 -->
          <div v-if="reportData.uncaptionedFigures || reportData.uncaptionedTables || reportData.orphanCaptions" class="report-note warn-note">
            <template v-if="reportData.uncaptionedFigures || reportData.uncaptionedTables">
              有 {{ reportData.uncaptionedFigures || 0 }} 张图、{{ reportData.uncaptionedTables || 0 }} 张表没有自动编号：
              引擎不再生成「图7-1」这类只有编号没有题名的题注，补写题名后再排版即可自动编号。
            </template>
            <template v-if="reportData.orphanCaptions">
              另有 {{ reportData.orphanCaptions }} 处题注文字与对应图表之间隔了空行/段落，未参与编号（已按正文保留）。
            </template>
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
          <el-empty v-else-if="!reportLoading && !structureAlert" description="未发现疑似标题问题，结构识别良好" :image-size="60" />
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
import WorkbenchShell from '../components/WorkbenchShell.vue'

const route = useRoute()
const templates = ref([])
const templateId = ref(route.query.templateId ? Number(route.query.templateId) : null)
const teamMap = ref({})
const fileList = ref([])
const selectedFile = ref(null)
const selectedFiles = ref([])
const uploadRef = ref(null)
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
const runningCount = computed(() => tasks.value.filter(t => t.status === 'PENDING' || t.status === 'PROCESSING').length)
const doneCount = computed(() => tasks.value.filter(t => t.status === 'SUCCESS').length)
const failedCount = computed(() => tasks.value.filter(t => t.status === 'FAILED').length)

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
// 差异分组排序: 内容级(丢图/吞段)最先, 其后是影响观感的字号字体行距, 目录条目的对齐/间距放最后
const DIFF_TYPE_ORDER = ['内容', '字号', '字体', '加粗', '行距', '对齐', '首行缩进', '段前距', '段后距']
// 单组默认最多展示条数: 目录 78 条同类调整不该把整屏占满
const DIFF_PREVIEW = 8
const expandedGroups = ref({})
function toggleExpand(key) {
  expandedGroups.value[key] = !expandedGroups.value[key]
}
function shownItems(g) {
  return expandedGroups.value[g.key] ? g.items : g.items.slice(0, DIFF_PREVIEW)
}
const diffGroups = computed(() => {
  const list = selectedType.value ? diffItems.value.filter(d => d.type === selectedType.value) : diffItems.value
  const map = {}
  list.forEach(d => { (map[d.type] = map[d.type] || []).push(d) })
  return Object.keys(map)
    .sort((a, b) => {
      const ia = DIFF_TYPE_ORDER.indexOf(a)
      const ib = DIFF_TYPE_ORDER.indexOf(b)
      return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib)
    })
    .map(k => ({ key: k, items: map[k] }))
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
  selectedFile.value = selectedFiles.value[0] || null
}

/** 移除待提交列表里的某个文件(传错了可以直接撤掉, 不用刷新页面) */
function removePicked(file) {
  // 走 el-upload 的 handleRemove, 保证其内部列表同步, 下次选择文件时不会把已移除的文件带回来
  if (uploadRef.value && typeof uploadRef.value.handleRemove === 'function') {
    uploadRef.value.handleRemove(file)
    // handleRemove 会触发 on-remove -> onFileRemove 完成列表同步
    return
  }
  fileList.value = fileList.value.filter(f => f !== file)
  selectedFiles.value = fileList.value.map(f => f.raw || f)
  selectedFile.value = selectedFiles.value[0] || null
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
    const failed = []
    // 逐篇容错: 某一篇上传失败不得中断整批, 也不让已上传成功的文件变成「有文件无任务」的孤儿
    for (const f of selectedFiles.value) {
      try {
        const paperFile = await uploadPaper(f)
        fileIds.push(paperFile.id)
      } catch (e) {
        const reason = (e && e.response && e.response.data && e.response.data.message)
          || (e && e.message) || '上传失败'
        failed.push({ file: f, name: (f && f.name) || '未命名文件', reason })
      }
    }

    // 先把上传成功的文件移出待提交区, 再建任务:
    // 即便建任务失败, 也不会让用户误以为整批没提交而重复上传
    const failedSet = new Set(failed.map(x => x.file))
    const remain = fileList.value.filter(f => failedSet.has(f.raw || f))
    fileList.value = remain
    selectedFiles.value = remain.map(f => f.raw || f)
    selectedFile.value = selectedFiles.value[0] || null

    let batchError = null
    if (fileIds.length) {
      try {
        await startFormatBatch(templateId.value, fileIds)
      } catch (e) {
        batchError = (e && e.message) || '创建排版任务失败'
      }
    }

    if (fileIds.length && !batchError) {
      ElMessage.success(`已提交 ${fileIds.length} 篇论文的排版任务`)
      await loadTasks(true)
      await loadFiles()
      startSse()
    }
    if (batchError) {
      ElMessage.error(`文件已上传，但创建排版任务失败：${batchError}`)
      await loadFiles()
    }
    if (failed.length) {
      const shown = failed.slice(0, 3).map(x => `${x.name}（${x.reason}）`).join('；')
      const more = failed.length > 3 ? ` 等 ${failed.length} 篇` : ''
      ElMessage.error(`${failed.length} 篇未能提交：${shown}${more}`)
    }
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
  expandedGroups.value = {}
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

// 结构告警: 是否展示与展示什么完全由后端 structureLevel / structureMessage 决定。
// 旧任务的报告里没有这两个字段(null), 此时返回 null, 回退到原来的渲染逻辑, 不会误报。
const structureAlert = computed(() => {
  const lv = reportData.value && reportData.value.structureLevel
  if (lv !== 'EMPTY' && lv !== 'WARN') return null
  return {
    level: lv,
    message: reportData.value.structureMessage || '文档结构可能存在问题，建议检查标题与正文内容'
  }
})

function levelName(lv) {
  const names = { 1: '一级', 2: '二级', 3: '三级', 4: '四级', 5: '五级' }
  return names[lv] || (lv || '?') + ' 级'
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
    if (r.autoFixedHeadingItems && r.autoFixedHeadingItems.length) {
      r.autoFixedHeadingItems.forEach(a => {
        lines.push(`    - 第 ${a.index} 段 [${levelName(a.level)}] ${a.text}`)
      })
    }
  }
  if (r.uncaptionedFigures || r.uncaptionedTables) {
    lines.push(`  无题注未编号: 图 ${r.uncaptionedFigures || 0} 张 / 表 ${r.uncaptionedTables || 0} 张(补写题名后可自动编号)`)
  }
  if (r.orphanCaptions) {
    lines.push(`  题注未紧邻图表: ${r.orphanCaptions} 处(已按正文保留, 编号未重排)`)
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
  if (m.includes('过大') || m.includes('拆分')) {
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
  color: #24312A;
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
  color: #5C6B60;
}
.file-count {
  font-size: 13px;
  color: #2F5D46;
}
.stage-text {
  font-size: 12px;
  color: #8B968D;
  margin-top: 2px;
}
.rerun-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #5C6B60;
}
.src-team {
  display: inline-block;
  font-size: 11px;
  color: #2F5D46;
  background: #E8EFE6;
  border-radius: 999px;
  padding: 2px 9px;
}
.src-mine {
  font-size: 12px;
  color: #B3BCB2;
}
.task-list {
  margin-top: 24px;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.task-list h3 {
  color: #24312A;
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
  color: #8B968D;
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
  color: #24312A;
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
  color: #8B968D;
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
  background: #F6F4EE;
  font-size: 12px;
  font-weight: 600;
  color: #5C6B60;
  user-select: none;
}
.diff-group-title:hover {
  background: #EFEDE4;
}
.diff-group-name {
  color: #b91c1c;
}
.diff-group-count {
  color: #8B968D;
  font-weight: 400;
}
.diff-group-arrow {
  margin-left: auto;
  color: #8B968D;
}
.diff-group-items {
  padding-top: 6px;
}
.diff-item {
  border: 1px solid #E3E0D5;
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
/* 组内默认只显示前 N 条(目录条目这类批量同类调整会占满整屏) */
.diff-more {
  text-align: center;
  font-size: 12px;
  color: #5C6B60;
  background: #F6F4EE;
  border: 1px dashed #D6D1C5;
  border-radius: 6px;
  padding: 5px 0;
  margin-bottom: 8px;
  cursor: pointer;
  user-select: none;
}
.diff-more:hover {
  color: #24312A;
  border-color: #B9B2A2;
  background: #EFEBE1;
}
.diff-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.diff-index {
  font-size: 11px;
  color: #5C6B60;
  background: #EFEDE4;
  padding: 1px 6px;
  border-radius: 4px;
}
/* 位置标识: 同名段落(多个"本章小结"、多个表格里的"10")靠它区分, 不能再让人以为是重复条目 */
.diff-where {
  font-size: 11px;
  color: #9AA69C;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-left: 8px;
  flex: 1;
  text-align: right;
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
  color: #24312A;
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
  background: #F6F4EE;
  border-bottom: 1px solid #E3E0D5;
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
  color: #5C6B60;
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
  background: #E3E0D5;
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
  color: #5C6B60;
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
  color: #8B968D;
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
  border: 1px solid #E3E0D5;
  border-radius: 8px;
  background: #F6F4EE;
}
.stat-chip b {
  display: block;
  font-size: 22px;
  color: #2F5D46;
}
.stat-chip span {
  font-size: 12px;
  color: #8B968D;
}
.report-note {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 10px;
}
/* 被自动识别为标题的段落清单 */
.auto-fixed-list {
  margin-bottom: 10px;
  border-top: none;
}
.report-hint {
  font-size: 12px;
  color: #8B968D;
  padding: 6px 2px 0;
  line-height: 1.6;
}
.structure-alert {
  margin: 12px 0;
  padding: 10px 14px;
  border-radius: 8px;
  border-left: 4px solid transparent;
  font-size: 13px;
  line-height: 1.7;
}
.structure-alert.is-warn {
  background: #fdf6ec;
  border-left-color: #e6a23c;
  color: #a06a1e;
}
.structure-alert.is-danger {
  background: #fef0f0;
  border-left-color: #f56c6c;
  color: #b93a3a;
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
  color: #5C6B60;
  line-height: 1.9;
}
/* 版本历史: 按论文分组的版本标注 */
.version-sub {
  font-size: 12px;
  color: #8B968D;
  margin-top: 2px;
}
/* 报告: 排版参数快照 */
.snapshot-block {
  font-size: 13px;
  color: #5C6B60;
}
.snapshot-row {
  line-height: 2;
}
.snapshot-key {
  display: inline-block;
  width: 80px;
  color: #8B968D;
}

/* ============================================================
   瑞士体系覆盖层（保留原有弹窗样式，只重做页级与表格）
   ============================================================ */
.flt {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--hair);
}
.flt-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  height: 38px;
  padding: 0 14px;
  background: none;
  border: 1px solid var(--hair);
  font-family: inherit;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  white-space: nowrap;
}
.chip:hover {
  border-color: var(--ink);
  color: var(--ink);
}
.chip.on {
  border-color: var(--ink);
  color: var(--ink);
  font-weight: 700;
}
.flt-search {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--ink);
  min-width: 240px;
}
.flt-k {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
}
.flt-search input {
  flex: 1;
  border: 0;
  outline: none;
  background: none;
  font-family: inherit;
  font-size: 14px;
  color: var(--ink);
}
.flt-count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

.wbtn {
  height: 38px;
  padding: 0 18px;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  border: 0;
  white-space: nowrap;
}
.wbtn.solid {
  background: var(--red);
  color: #fff;
  font-weight: 700;
}
.wbtn.solid:disabled {
  background: var(--hair);
  cursor: not-allowed;
}
.wbtn.ghost {
  background: none;
  border: 1px solid var(--ink);
  color: var(--ink);
}
.wbtn.ghost:disabled {
  border-color: var(--hair);
  color: var(--hair);
  cursor: not-allowed;
}
.wbtn.ghost.danger:not(:disabled):hover {
  background: var(--red);
  border-color: var(--red);
  color: #fff;
}

/* ---------------- 上传区 ---------------- */
.upload {
  margin: 8px 0 40px;
}
.upload-head h2 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.upload-head p {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 720px;
}
.upload-drop :deep(.el-upload) {
  width: 100%;
}
.upload-drop :deep(.el-upload-dragger) {
  border: 1px solid var(--ink);
  border-radius: 0;
  background: #fff;
  padding: 0;
  transition: background 0.16s ease;
}
.upload-drop :deep(.el-upload-dragger:hover),
.upload-drop :deep(.el-upload-dragger.is-dragover) {
  background: #fcecec;
  border-color: var(--red);
}
.drop-inner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 34px 28px;
  text-align: left;
}
.drop-icon {
  width: 28px;
  height: 28px;
  stroke: var(--ink);
  stroke-width: 1.5;
  fill: none;
  margin-bottom: 6px;
}
.drop-k {
  font-size: 19px;
  font-weight: 700;
  color: var(--ink);
}
.drop-d {
  font-size: 13px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
}

.upload-set {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  margin-top: 18px;
  flex-wrap: wrap;
}
.set {
  display: grid;
  gap: 8px;
  min-width: 320px;
  flex: 1;
  max-width: 480px;
}
.set-k {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
}
.set-act {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}
.set-count {
  font-size: 13px;
  color: var(--ink-2);
}
.sel-empty {
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-3);
}
.sel-empty a {
  color: var(--red);
  font-weight: 700;
}

.picked {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
  border-top: 1px solid var(--ink);
}
.picked li {
  display: grid;
  grid-template-columns: 1fr 120px 90px 32px;
  gap: 16px;
  align-items: baseline;
  padding: 12px 0;
  border-bottom: 1px solid var(--hair-soft);
  font-size: 14px;
}
.picked-name {
  color: var(--ink);
  font-weight: 500;
  word-break: break-all;
}
.picked-size {
  color: var(--ink-3);
  font-size: 13px;
}
.picked-state {
  color: var(--ink-3);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  text-align: right;
}
.picked-del {
  width: 26px;
  height: 26px;
  padding: 0;
  border: 1px solid var(--hair);
  border-radius: 50%;
  background: none;
  color: var(--ink-3);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  align-self: center;
  transition: color 0.15s, border-color 0.15s;
}
.picked-del:hover {
  color: #c0392b;
  border-color: #c0392b;
}

/* ---------------- 区块标题 ---------------- */
.t-empty {
  padding: 48px 0 56px;
  text-align: left;
}
.t-empty-k {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 700;
}
.t-empty-d {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 520px;
}
.t-empty-a {
  display: flex;
  gap: 12px;
}

.sec-h {
  margin: 0 0 14px;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.sec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.sec-head .sec-h {
  margin: 0;
}
.sec-acts {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.task-list,
.file-list {
  margin-bottom: 40px;
  background: none;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
}
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}
:deep(.el-table__inner-wrapper) {
  background: transparent;
}
:deep(.el-table__empty-block) {
  background: transparent;
}
:deep(.op-row .el-button) {
  height: auto;
  padding: 0 0 2px;
  border: 0;
  background: none !important;
  color: var(--ink-2) !important;
  font-size: 13.5px;
  border-bottom: 1px solid transparent;
}
:deep(.op-row .el-button:hover) {
  color: var(--ink) !important;
  border-bottom-color: var(--hair);
}
:deep(.op-row .el-button--primary) {
  color: var(--ink) !important;
  font-weight: 700;
  border-bottom: 2px solid var(--red);
}
:deep(.op-row .el-button--success) {
  color: var(--ink) !important;
}
:deep(.op-row .el-button--danger:hover) {
  color: var(--red) !important;
  border-bottom-color: var(--red);
}

/* ---------------- Element 表格：硬线、透明表头、无圆角 ---------------- */
:deep(.el-table) {
  --el-table-border-color: var(--hair-soft);
  --el-table-header-text-color: var(--ink-3);
  --el-table-text-color: var(--ink-2);
  --el-table-row-hover-bg-color: #fff;
  background: transparent;
  font-size: 14px;
}
:deep(.el-table tr),
:deep(.el-table th.el-table__cell) {
  background: transparent;
}
:deep(.el-table th.el-table__cell) {
  font-weight: 400;
  font-size: 12px;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--ink);
  padding: 6px 0;
}
:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid var(--hair-soft);
  padding: 14px 0;
  vertical-align: top;
}
:deep(.el-table__inner-wrapper::before) {
  display: none;
}
:deep(.el-table .cell) {
  padding: 0 12px 0 0;
}
:deep(.el-tag) {
  border-radius: 0;
  background: none;
  font-weight: 500;
}
:deep(.el-tag--success) {
  border-color: var(--ink);
  color: var(--ink);
}
:deep(.el-tag--danger) {
  border-color: var(--red);
  color: var(--red);
}
:deep(.el-tag--warning) {
  border-color: var(--hair);
  color: var(--ink-2);
}
:deep(.el-progress-bar__outer) {
  background: var(--hair-soft);
  border-radius: 0;
}
:deep(.el-progress-bar__inner) {
  background: var(--red);
  border-radius: 0;
}
:deep(.el-progress__text) {
  display: none;
}
:deep(.el-checkbox__inner) {
  border-radius: 0;
}
:deep(.el-pagination) {
  display: none;
}

.stage-text {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.5;
}
.version-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-3);
}
.src-team,
.src-mine {
  font-size: 12.5px;
  letter-spacing: 0.05em;
}
.src-team {
  color: var(--ink);
  font-weight: 700;
}
.src-mine {
  color: var(--ink-3);
}
.op-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}
:deep(.op-row .el-button) {
  border-radius: 0;
  margin: 0;
}

.pager {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--ink-2);
}
.pager button {
  background: none;
  border: 1px solid var(--hair);
  height: 30px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
}
.pager button:disabled {
  color: var(--hair);
  border-color: var(--hair-soft);
  cursor: not-allowed;
}
.pager b {
  color: var(--ink);
}

@media (max-width: 900px) {
  .task-list,
  .file-list {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .task-list :deep(.el-table),
  .file-list :deep(.el-table) {
    min-width: 760px;
  }
  .flt {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .flt-search {
    width: 100%;
    min-width: 0;
  }
  .flt-chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }
  .chip {
    flex: none;
  }
  .flt-count {
    margin-left: 0;
  }
  .upload-head h2 {
    font-size: 22px;
  }
  .drop-inner {
    padding: 24px 18px;
  }
  .drop-k {
    font-size: 16px;
  }
  .set {
    min-width: 0;
  }
  .set-act {
    width: 100%;
    margin-left: 0;
    justify-content: space-between;
  }
  .picked li {
    grid-template-columns: 1fr 80px 32px;
  }
  .picked-state {
    display: none;
  }
  .t-empty {
  padding: 48px 0 56px;
  text-align: left;
}
.t-empty-k {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 700;
}
.t-empty-d {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 520px;
}
.t-empty-a {
  display: flex;
  gap: 12px;
}

.sec-h {
    font-size: 18px;
  }
}


/* 状态与来源：去掉旧主题的绿色胶囊 */
:deep(.src-team),
:deep(.src-mine) {
  background: none;
  padding: 0;
  border-radius: 0;
}
:deep(.src-team) {
  color: var(--ink);
  font-weight: 700;
}
:deep(.src-mine) {
  color: var(--ink-3);
}
:deep(.el-tag) {
  background: none !important;
  border: 1px solid var(--hair);
  color: var(--ink-2);
  height: 24px;
  padding: 0 8px;
  line-height: 22px;
}
:deep(.el-tag--danger) {
  border-color: var(--red);
  color: var(--red);
}
:deep(.el-tag--success) {
  border-color: var(--ink);
  color: var(--ink);
}
.st-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.st-cell :deep(.el-progress) {
  display: block;
  width: 100%;
  margin: 0;
}
.st-cell :deep(.el-progress-bar) {
  padding: 0;
  margin: 0;
}
:deep(.el-table .el-progress-bar) {
  padding-right: 0;
  margin-right: 0;
}
/* 上传文档的删除按钮同样走文字链 */
:deep(.file-list .el-button) {
  height: auto;
  padding: 0 0 2px;
  border: 0;
  background: none !important;
  color: var(--ink-3) !important;
  font-size: 13.5px;
  border-bottom: 1px solid transparent;
}
:deep(.file-list .el-button:hover) {
  color: var(--red) !important;
  border-bottom-color: var(--red);
}

</style>
