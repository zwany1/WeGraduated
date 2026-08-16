<template>
  <div class="mgmt">
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9a917d" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input v-model="keyword" placeholder="搜索模板名称" @keyup.enter="load(1)" />
      </div>
      <el-button type="primary" plain @click="load(1)">查询</el-button>
      <span class="toolbar-note">将优质模板上架到前台模板市场，供所有用户一键复制使用</span>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="模板名称" min-width="180">
          <template #default="{ row }"><span class="cell-name">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="所属用户" min-width="130">
          <template #default="{ row }"><span class="cell-user-tag">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column label="规则数" width="80" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.ruleCount }}</span></template>
        </el-table-column>
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }"><span class="cell-user-tag">{{ row.category || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="下载/评分" width="120" align="center">
          <template #default="{ row }">
            <span class="cell-num">{{ row.downloadCount || 0 }} 次 · {{ (row.ratingAvg || 0).toFixed(1) }} ({{ row.ratingCount || 0 }})</span>
          </template>
        </el-table-column>
        <el-table-column label="被引用" width="90" align="center">
          <template #default="{ row }"><span class="cell-num">{{ row.taskCount }}</span></template>
        </el-table-column>
        <el-table-column label="推荐" width="80" align="center">
          <template #default="{ row }">
            <span class="tag-rec" :class="{ on: row.recommended }">{{ row.recommended ? '推荐' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.isPublic ? 'on' : 'off'">{{ row.isPublic ? '已上架' : '未上架' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架时间" width="150">
          <template #default="{ row }"><span class="cell-muted">{{ fmtTime(row.publicTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <span class="op-cell">
              <el-button v-perm="'system:market:list'" size="small" plain type="primary" @click="openDetail(row)">查看</el-button>
              <el-button v-perm="'system:market:edit'" size="small" plain
                :type="row.isPublic ? 'warning' : 'primary'" @click="togglePublic(row)">
                {{ row.isPublic ? '下架' : '上架' }}
              </el-button>
              <el-button v-if="row.isPublic" v-perm="'system:market:edit'" size="small" plain
                :type="row.recommended ? 'danger' : 'success'" @click="toggleRecommended(row)">
                {{ row.recommended ? '取消推荐' : '推荐' }}
              </el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 模板详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="d ? '模板详情 - ' + d.name : '模板详情'" width="780px"
      class="admin-dialog" modal-class="admin-overlay" destroy-on-close append-to-body>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="d">
          <section class="d-sec">
            <h4 class="d-sec-title">基本信息</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">所属用户</span><span class="d-value">{{ d.username }}</span></div>
              <div class="d-item"><span class="d-label">规则数</span><span class="d-value">{{ d.rules.length }}</span></div>
              <div class="d-item"><span class="d-label">分类</span><span class="d-value">{{ d.category || '—' }}</span></div>
              <div class="d-item"><span class="d-label">下载量</span><span class="d-value">{{ d.downloadCount || 0 }} 次</span></div>
              <div class="d-item"><span class="d-label">评分</span><span class="d-value">{{ (d.ratingAvg || 0).toFixed(1) }} / 5（{{ d.ratingCount || 0 }} 人）</span></div>
              <div class="d-item"><span class="d-label">上架状态</span><span class="d-value">{{ d.isPublic ? '已上架' : '未上架' }}</span></div>
              <div class="d-item"><span class="d-label">推荐</span><span class="d-value">{{ d.recommended ? '是' : '否' }}</span></div>
              <div class="d-item"><span class="d-label">上架时间</span><span class="d-value">{{ fmtTime(d.publicTime) }}</span></div>
            </div>
          </section>

          <section v-if="pageCfg.margin" class="d-sec">
            <h4 class="d-sec-title">页面设置</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">纸张</span><span class="d-value">{{ pageCfg.paper || 'A4' }}</span></div>
              <div class="d-item"><span class="d-label">页边距</span>
                <span class="d-value">上 {{ pageCfg.margin.top }} / 下 {{ pageCfg.margin.bottom }} / 左 {{ pageCfg.margin.left }} / 右 {{ pageCfg.margin.right }} cm</span>
              </div>
              <div class="d-item"><span class="d-label">页眉</span>
                <span class="d-value">{{ pageCfg.header.text ? pageCfg.header.text + '（高 ' + pageCfg.header.height + 'cm）' : '无内容（高 ' + pageCfg.header.height + 'cm）' }}</span>
              </div>
              <div class="d-item"><span class="d-label">页脚页码</span><span class="d-value">{{ footerLabel(pageCfg.footer.pageNumber) }}</span></div>
            </div>
          </section>

          <section v-if="cover" class="d-sec">
            <h4 class="d-sec-title">封面配置</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">生成封面</span><span class="d-value">{{ cover.enabled ? '是' : '否' }}</span></div>
              <template v-if="cover.enabled">
                <div class="d-item"><span class="d-label">论文题目</span><span class="d-value">{{ cover.title || '—' }}</span></div>
                <div class="d-item"><span class="d-label">学院</span><span class="d-value">{{ cover.college || '—' }}</span></div>
                <div class="d-item"><span class="d-label">专业</span><span class="d-value">{{ cover.major || '—' }}</span></div>
                <div class="d-item"><span class="d-label">学生姓名</span><span class="d-value">{{ cover.studentName || '—' }}</span></div>
                <div class="d-item"><span class="d-label">学号</span><span class="d-value">{{ cover.studentNo || '—' }}</span></div>
                <div class="d-item"><span class="d-label">指导教师</span><span class="d-value">{{ (cover.teacher || '—') + (cover.teacherTitle ? '（' + cover.teacherTitle + '）' : '') + (cover.teacherUnit ? ' · ' + cover.teacherUnit : '') }}</span></div>
                <div class="d-item"><span class="d-label">课题类型</span><span class="d-value">{{ cover.topicType || '—' }}</span></div>
                <div class="d-item"><span class="d-label">日期</span><span class="d-value">{{ cover.date || '—' }}</span></div>
              </template>
            </div>
          </section>

          <section v-if="headings" class="d-sec">
            <h4 class="d-sec-title">标题识别规则</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">一级标题</span><span class="d-value mono">{{ headings.heading1 || '—' }}</span></div>
              <div class="d-item"><span class="d-label">二级标题</span><span class="d-value mono">{{ headings.heading2 || '—' }}</span></div>
              <div class="d-item"><span class="d-label">三级标题</span><span class="d-value mono">{{ headings.heading3 || '—' }}</span></div>
            </div>
          </section>

          <section class="d-sec">
            <h4 class="d-sec-title">目录</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">自动生成目录</span><span class="d-value">{{ d.generateToc ? '是' : '否' }}</span></div>
            </div>
          </section>

          <section v-if="refCfg" class="d-sec">
            <h4 class="d-sec-title">参考文献</h4>
            <div class="d-grid">
              <div class="d-item"><span class="d-label">自动格式化</span><span class="d-value">{{ refCfg.enabled ? '是' : '否' }}</span></div>
              <div class="d-item"><span class="d-label">标题</span><span class="d-value">{{ refCfg.title || '—' }}<span v-if="refCfg.titleFont" class="muted">（{{ refCfg.titleFont }} {{ refCfg.titleFontSize }}号）</span></span></div>
              <div class="d-item"><span class="d-label">条目字体</span><span class="d-value">{{ refCfg.itemFont || '—' }}<span v-if="refCfg.itemFontLatin" class="muted"> / {{ refCfg.itemFontLatin }} {{ refCfg.itemFontSize }}号</span></span></div>
              <div class="d-item"><span class="d-label">最多作者数</span><span class="d-value">{{ refCfg.maxAuthors }}</span></div>
              <div class="d-item"><span class="d-label">去除 DOI</span><span class="d-value">{{ refCfg.removeDoi ? '是' : '否' }}</span></div>
              <div class="d-item"><span class="d-label">重新编号</span><span class="d-value">{{ refCfg.renumber ? '是' : '否' }}</span></div>
            </div>
          </section>

          <section class="d-sec">
            <h4 class="d-sec-title">格式规则（{{ d.rules.length }} 条）</h4>
            <el-table :data="d.rules" size="small" border class="d-rule-table">
              <el-table-column label="规则类型" width="92">
                <template #default="{ row }">{{ ruleTypeLabel(row.ruleType) }}</template>
              </el-table-column>
              <el-table-column label="字体" min-width="140">
                <template #default="{ row }">{{ row.font || '—' }}<span v-if="row.fontLatin" class="muted"> / {{ row.fontLatin }}</span></template>
              </el-table-column>
              <el-table-column label="字号" width="66" align="center">
                <template #default="{ row }">{{ row.fontSize ? row.fontSize + '号' : '—' }}</template>
              </el-table-column>
              <el-table-column label="加粗" width="58" align="center">
                <template #default="{ row }">{{ row.bold ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="对齐" width="76" align="center">
                <template #default="{ row }">{{ alignLabel(row.align) }}</template>
              </el-table-column>
              <el-table-column label="行距" width="104">
                <template #default="{ row }">{{ lineSpacingLabel(row) }}</template>
              </el-table-column>
              <el-table-column label="首行缩进" width="86" align="center">
                <template #default="{ row }">{{ row.firstLineIndent ? row.firstLineIndent + '字符' : '—' }}</template>
              </el-table-column>
              <el-table-column label="段前/段后" width="92" align="center">
                <template #default="{ row }">{{ row.spaceBefore || 0 }} / {{ row.spaceAfter || 0 }}pt</template>
              </el-table-column>
              <el-table-column label="图表题注" min-width="150">
                <template #default="{ row }">
                  <span v-if="row.ruleType === 'figure' || row.ruleType === 'table'">
                    <span class="cap-tag" :class="{ on: row.captionEnabled }">{{ row.captionEnabled ? (row.captionPosition === 'above' ? '题注在上' : '题注在下') : '未启用' }}</span>
                    <span v-if="row.numberingPattern" class="muted">（{{ row.numberingPattern }}）</span>
                  </span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
        <el-empty v-if="!d && !detailLoading" description="模板不存在" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listMarketTemplates, setMarketTemplate, getMarketTemplateDetail } from '../../api/admin'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

// ===== 模板详情 =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const d = ref(null)

const parseJson = (s, fb) => {
  try { return s ? JSON.parse(s) : fb } catch (e) { return fb }
}
const pageCfg = computed(() => parseJson(d.value?.pageConfig, {}))
const cover = computed(() => parseJson(d.value?.coverConfig, null))
const headings = computed(() => parseJson(d.value?.headingPatterns, null))
const refCfg = computed(() => parseJson(d.value?.referenceConfig, null))

const ruleTypeLabel = t => ({
  heading1: '一级标题', heading2: '二级标题', heading3: '三级标题',
  body: '正文', figure: '图题注', table: '表题注'
}[t] || t || '—')

const alignLabel = a => ({
  left: '左对齐', center: '居中', right: '右对齐', justify: '两端对齐'
}[a] || a || '—')

const lineSpacingLabel = r => {
  if (r.lineSpacingType === 'exact') return r.lineSpacingExact ? '固定 ' + r.lineSpacingExact + ' 磅' : '—'
  if (r.lineSpacing) return r.lineSpacing + ' 倍'
  return '—'
}

const footerLabel = f => ({ none: '不显示', left: '左侧', center: '居中', right: '右侧' }[f] || f || '不显示')

async function openDetail(row) {
  d.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    d.value = await getMarketTemplateDetail(row.id)
  } catch (e) {
  } finally {
    detailLoading.value = false
  }
}

const fmtTime = t => {
  if (!t) return '—'
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const data = await listMarketTemplates({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function togglePublic(row) {
  let body = { isPublic: !row.isPublic }
  if (!row.isPublic) {
    // 上架时选择分类
    const c = window.prompt('请选择上架分类（毕业论文 / 期刊论文 / 报告文档 / 其他）：', row.category || '毕业论文')
    if (c === null) return
    body = { isPublic: true, category: (c.trim() || '毕业论文') }
  }
  try {
    await setMarketTemplate(row.id, body)
    ElMessage.success(row.isPublic ? '已下架' : '已上架到模板市场')
    await load()
  } catch (e) {}
}

async function toggleRecommended(row) {
  try {
    await setMarketTemplate(row.id, { recommended: !row.recommended })
    ElMessage.success(row.recommended ? '已取消推荐' : '已设为推荐')
    await load()
  } catch (e) {}
}

onMounted(() => load())
</script>

<style scoped>
.mgmt { display: flex; flex-direction: column; gap: 18px; animation: mgmt-in 0.35s ease both; }
@keyframes mgmt-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.toolbar { display: flex; align-items: center; gap: 12px; }
.search-box { display: flex; align-items: center; gap: 8px; width: 280px; padding: 9px 14px; background: #fffdf9; border: 1px solid #e6ded0; border-radius: 9px; }
.search-box:focus-within { border-color: #3a6ea5; box-shadow: 0 0 0 3px rgba(58, 110, 165, 0.12); }
.search-box input { border: none; outline: none; flex: 1; background: transparent; font-size: 13.5px; color: #2c3140; }
.search-box input::placeholder { color: #b3a583; }
.toolbar-note { margin-left: auto; font-size: 12px; color: #9a917d; }
.table-card { background: #fffdf9; border: 1px solid #e6ded0; border-radius: 14px; padding: 6px 16px 14px; box-shadow: 0 1px 2px rgba(13, 27, 46, 0.04); }
.pager { display: flex; justify-content: flex-end; padding-top: 14px; }
.cell-name { font-size: 13.5px; color: #2c3140; font-weight: 500; }
.cell-user-tag { display: inline-block; font-size: 12px; color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; padding: 2px 8px; border-radius: 4px; }
.cell-muted { color: #8a8d99; font-size: 12.5px; }
.cell-num { font-variant-numeric: tabular-nums; color: #4a4f5e; }
.tag-rec { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; color: #8a6a25; background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.tag-rec.on { color: #b23a2e; background: rgba(178, 58, 46, 0.08); border: 1px solid rgba(178, 58, 46, 0.3); }
.status-tag { display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.on { color: #2e7d4f; background: rgba(46, 125, 79, 0.1); border: 1px solid rgba(46, 125, 79, 0.3); }
.status-tag.off { color: #6b6f7d; background: #f4f0e6; border: 1px solid #e6ded0; }
/* 操作列按钮组: 强制同一行 */
.op-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.op-cell .el-button + .el-button {
  margin-left: 0;
}

/* ===== 模板详情弹窗 ===== */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.d-sec {
  border: 1px solid #efe8dc;
  border-radius: 10px;
  padding: 12px 14px 14px;
  background: #fffdf9;
}
.d-sec-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #0d1b2e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.d-sec-title::before {
  content: '';
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: #3a6ea5;
}
.d-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 20px;
}
.d-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12.5px;
  line-height: 1.6;
}
.d-label {
  flex-shrink: 0;
  color: #8a8d99;
  min-width: 64px;
}
.d-value {
  color: #2c3140;
  word-break: break-all;
}
.d-value.mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #3a6ea5;
}
.muted {
  color: #9a917d;
  font-size: 12px;
}
.cap-tag {
  display: inline-block;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 4px;
  color: #8a8d99;
  background: #f4f0e6;
  border: 1px solid #e6ded0;
}
.cap-tag.on {
  color: #2e7d4f;
  background: rgba(46, 125, 79, 0.1);
  border: 1px solid rgba(46, 125, 79, 0.3);
}
.d-rule-table {
  margin-top: 2px;
}
</style>
