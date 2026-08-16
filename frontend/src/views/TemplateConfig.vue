<template>
  <div class="config">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/templates')">‹ 返回</el-button>
        <span>{{ templateName }}</span>
      </div>
      <div class="brand-actions">
        <el-button @click="exportTpl">导出模板</el-button>
        <el-button type="primary" :loading="saving" @click="saveAll">保存配置</el-button>
      </div>
    </header>

    <main class="body">
      <aside class="menu">
        <div
          v-for="m in menus"
          :key="m.key"
          class="menu-item"
          :class="{ active: active === m.key }"
          @click="active = m.key"
        >
          {{ m.label }}
        </div>
      </aside>

      <section class="panel">
        <!-- 页面设置 -->
        <template v-if="active === 'page'">
          <h3>纸张类型</h3>
          <el-form label-width="120px" style="max-width: 520px">
            <el-form-item label="纸张">
              <el-select v-model="page.paper">
                <el-option label="A4 (21×29.7cm)" value="A4" />
                <el-option label="A3" value="A3" />
                <el-option label="A5" value="A5" />
                <el-option label="B5" value="B5" />
                <el-option label="Letter" value="Letter" />
              </el-select>
            </el-form-item>
          </el-form>
          <h3>页边距 (cm)</h3>
          <el-form label-width="120px" style="max-width: 520px">
            <el-form-item label="上"><el-input-number v-model="page.margin.top" :min="0" :max="10" :step="0.1" /></el-form-item>
            <el-form-item label="下"><el-input-number v-model="page.margin.bottom" :min="0" :max="10" :step="0.1" /></el-form-item>
            <el-form-item label="左"><el-input-number v-model="page.margin.left" :min="0" :max="10" :step="0.1" /></el-form-item>
            <el-form-item label="右"><el-input-number v-model="page.margin.right" :min="0" :max="10" :step="0.1" /></el-form-item>
          </el-form>
          <h3>页眉 / 页脚</h3>
          <el-form label-width="120px" style="max-width: 520px">
            <el-form-item label="页眉高度(cm)"><el-input-number v-model="page.header.height" :min="0" :max="5" :step="0.1" /></el-form-item>
            <el-form-item label="页眉内容">
              <el-input v-model="page.header.text" placeholder="留空则不添加页眉文字" />
            </el-form-item>
            <el-form-item label="页码位置">
              <el-select v-model="page.footer.pageNumber">
                <el-option label="无页码" value="none" />
                <el-option label="底部居中" value="center" />
                <el-option label="底部居左" value="left" />
                <el-option label="底部居右" value="right" />
              </el-select>
            </el-form-item>
          </el-form>
        </template>

        <!-- 标题格式 -->
        <template v-else-if="active === 'heading'">
          <div class="block">
            <h3>标题识别规则 (正则)</h3>
            <p class="tip">排版引擎按此规则识别章节标题并自动编号。例：<code>第1章</code> / <code>1 绪论</code> / <code>一、绪论</code></p>
            <div class="presets">
              <el-button size="small" @click="applyPreset('chinese')">第X章式</el-button>
              <el-button size="small" @click="applyPreset('number')">1 / 1.1 式</el-button>
              <el-button size="small" @click="applyPreset('cn')">一 / 一、式</el-button>
            </div>
            <el-form label-width="100px" style="max-width: 620px">
              <el-form-item label="一级标题">
                <el-input v-model="headingPatterns.heading1" placeholder="^第[一二三四五六七八九十百]+章" />
              </el-form-item>
              <el-form-item label="二级标题">
                <el-input v-model="headingPatterns.heading2" placeholder="^\d+\.\d+" />
              </el-form-item>
              <el-form-item label="三级标题">
                <el-input v-model="headingPatterns.heading3" placeholder="^\d+\.\d+\.\d+" />
              </el-form-item>
            </el-form>
          </div>
          <div v-for="h in headings" :key="h.key" class="block">
            <h3>{{ h.label }}</h3>
            <el-form label-width="100px" style="max-width: 560px">
              <el-form-item label="字体">
                <el-select v-model="rules[h.key].font">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="西文字体">
                <el-select v-model="rules[h.key].fontLatin">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="字号">
                <el-select v-model="rules[h.key].fontSize">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="加粗"><el-switch v-model="rules[h.key].bold" /></el-form-item>
              <el-form-item label="对齐">
                <el-select v-model="rules[h.key].align">
                  <el-option v-for="a in aligns" :key="a.v" :label="a.label" :value="a.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="段前 (pt)"><el-input-number v-model="rules[h.key].spaceBefore" :min="0" :max="100" /></el-form-item>
              <el-form-item label="段后 (pt)"><el-input-number v-model="rules[h.key].spaceAfter" :min="0" :max="100" /></el-form-item>
            </el-form>
          </div>
        </template>

        <!-- 正文格式 -->
        <template v-else-if="active === 'body'">
          <h3>正文格式</h3>
          <el-form label-width="100px" style="max-width: 560px">
            <el-form-item label="字体">
              <el-select v-model="rules.body.font">
                <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
              </el-select>
            </el-form-item>
            <el-form-item label="西文字体">
              <el-select v-model="rules.body.fontLatin">
                <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
              </el-select>
            </el-form-item>
            <el-form-item label="字号">
              <el-select v-model="rules.body.fontSize">
                <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
              </el-select>
            </el-form-item>
            <el-form-item label="行距类型">
              <el-select v-model="rules.body.lineSpacingType">
                <el-option label="多倍行距" value="multiple" />
                <el-option label="固定值(磅)" value="exact" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="rules.body.lineSpacingType !== 'exact'" label="行距倍数">
              <el-select v-model="rules.body.lineSpacing">
                <el-option v-for="l in lineSpacings" :key="l" :label="`${l} 倍`" :value="l" />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="固定值(磅)">
              <el-input-number v-model="rules.body.lineSpacingExact" :min="0" :max="100" :step="0.5" />
              <span style="margin-left:8px;color:#909399">磅</span>
            </el-form-item>
            <el-form-item label="首行缩进">
              <el-input-number v-model="rules.body.firstLineIndent" :min="0" :max="8" /><span style="margin-left:8px;color:#909399">字符</span>
            </el-form-item>
            <el-form-item label="段后空行">
              <el-select v-model="rules.body.spaceAfter">
                <el-option label="不空行" :value="0" />
                <el-option label="空半行 (6pt)" :value="6" />
                <el-option label="空一行 (12pt)" :value="12" />
                <el-option label="空两行 (24pt)" :value="24" />
              </el-select>
            </el-form-item>
            <el-form-item label="对齐">
              <el-select v-model="rules.body.align">
                <el-option v-for="a in aligns" :key="a.v" :label="a.label" :value="a.v" />
              </el-select>
            </el-form-item>
          </el-form>
        </template>

        <!-- 图表格式 -->
        <template v-else-if="active === 'figure'">
          <div class="block">
            <h3>图片题注</h3>
            <el-form label-width="100px" style="max-width: 560px">
              <el-form-item label="启用题注">
                <el-switch v-model="rules.figure.captionEnabled" />
                <span class="tip-inline">关闭后不为图片自动编号/添加下方标注</span>
              </el-form-item>
              <el-form-item label="字体">
                <el-select v-model="rules.figure.font">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="西文字体">
                <el-select v-model="rules.figure.fontLatin">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="字号">
                <el-select v-model="rules.figure.fontSize">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="题注位置">
                <el-select v-model="rules.figure.captionPosition">
                  <el-option label="图片下方" value="below" />
                  <el-option label="图片上方" value="above" />
                </el-select>
              </el-form-item>
              <el-form-item label="编号格式">
                <el-input v-model="rules.figure.numberingPattern" placeholder="图{chapter}-{no}" />
                <div class="tip">占位符: {chapter}章节号, {no}序号 → 如: 图2-1</div>
              </el-form-item>
            </el-form>
          </div>
          <div class="block">
            <h3>表格题注</h3>
            <el-form label-width="100px" style="max-width: 560px">
              <el-form-item label="启用题注">
                <el-switch v-model="rules.table.captionEnabled" />
                <span class="tip-inline">关闭后不为表格自动编号/添加上方标注</span>
              </el-form-item>
              <el-form-item label="字体">
                <el-select v-model="rules.table.font">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="西文字体">
                <el-select v-model="rules.table.fontLatin">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="字号">
                <el-select v-model="rules.table.fontSize">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="题注位置">
                <el-select v-model="rules.table.captionPosition">
                  <el-option label="表格上方" value="above" />
                  <el-option label="表格下方" value="below" />
                </el-select>
              </el-form-item>
              <el-form-item label="编号格式">
                <el-input v-model="rules.table.numberingPattern" placeholder="表{chapter}-{no}" />
                <div class="tip">占位符: {chapter}章节号, {no}序号 → 如: 表2-1</div>
              </el-form-item>
            </el-form>
          </div>
        </template>

        <!-- 参考文献设置 -->
        <template v-else-if="active === 'reference'">
          <el-form label-width="130px" style="max-width: 620px">
            <el-form-item label="启用参考文献排版">
              <el-switch v-model="refConfig.enabled" />
              <span class="tip-inline">关闭则整体排版不处理参考文献</span>
            </el-form-item>
            <template v-if="refConfig.enabled">
              <el-form-item label="标题文字">
                <el-input v-model="refConfig.title" placeholder="参考文献" style="max-width: 220px" />
              </el-form-item>
              <el-form-item label="标题字体">
                <el-select v-model="refConfig.titleFont" style="max-width: 220px">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
                <span class="tip-inline">默认黑体四号、顶格</span>
              </el-form-item>
              <el-form-item label="标题字号">
                <el-select v-model="refConfig.titleFontSize" style="max-width: 220px">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="条目中文字体">
                <el-select v-model="refConfig.itemFont" style="max-width: 220px">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
                <span class="tip-inline">默认宋体五号</span>
              </el-form-item>
              <el-form-item label="条目西文字体">
                <el-select v-model="refConfig.itemFontLatin" style="max-width: 220px">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
                <span class="tip-inline">默认 Times New Roman</span>
              </el-form-item>
              <el-form-item label="条目字号">
                <el-select v-model="refConfig.itemFontSize" style="max-width: 220px">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="作者最多保留">
                <el-input-number v-model="refConfig.maxAuthors" :min="1" :max="10" />
                <span class="tip-inline">超过后中文加"等"，英文加" et al"</span>
              </el-form-item>
              <el-form-item label="删除 DOI">
                <el-switch v-model="refConfig.removeDoi" />
                <span class="tip-inline">删除条目中 DOI 及其后内容</span>
              </el-form-item>
              <el-form-item label="序号重排">
                <el-switch v-model="refConfig.renumber" />
                <span class="tip-inline">按文档顺序重新编号 [1][2]...</span>
              </el-form-item>
            </template>
          </el-form>
          <div class="tip">参考文献另起一页、置于正文后。条目格式：序号 [1] 中括号+空两格，换行第二行对齐序号（悬挂缩进）。作者只写前 3 位，余者"等"/"et al"。</div>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTemplateDetail, savePageConfig, saveHeadingPatterns, saveReferenceConfig, saveRule, exportTemplate } from '../api/template'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const menus = [
  { key: 'page', label: '页面设置' },
  { key: 'heading', label: '标题格式' },
  { key: 'body', label: '正文格式' },
  { key: 'figure', label: '图表格式' },
  { key: 'reference', label: '参考文献' }
]
const active = ref('page')
const templateName = ref('格式方案')
const saving = ref(false)

const fonts = ['宋体', '黑体', '楷体', '仿宋', '微软雅黑', 'Times New Roman']
const latinFonts = ['Times New Roman', 'Arial', 'Cambria', 'Calibri', 'Georgia', '宋体', '黑体']
const sizeMap = { 9: '小五', 10: '五号', 12: '小四', 14: '四号', 15: '小三', 16: '三号', 18: '小二', 22: '二号', 24: '小一', 26: '一号' }
const sizes = Object.entries(sizeMap).map(([v, label]) => ({ v: Number(v), label: `${v}pt (${label})` }))
const aligns = [
  { v: 'left', label: '左对齐' },
  { v: 'center', label: '居中' },
  { v: 'right', label: '右对齐' },
  { v: 'justify', label: '两端对齐' }
]
const lineSpacings = [1, 1.15, 1.25, 1.5, 2, 2.5]

const page = reactive({
  paper: 'A4',
  margin: { top: 2.5, bottom: 2.5, left: 3, right: 2.5 },
  header: { height: 1.5, text: '' },
  footer: { pageNumber: 'center' }
})

const rules = reactive({
  heading1: { ruleType: 'heading1', font: '黑体', fontLatin: 'Times New Roman', fontSize: 16, bold: true, align: 'center', spaceBefore: 12, spaceAfter: 12 },
  heading2: { ruleType: 'heading2', font: '黑体', fontLatin: 'Times New Roman', fontSize: 14, align: 'left' },
  heading3: { ruleType: 'heading3', font: '楷体', fontLatin: 'Times New Roman', fontSize: 12, align: 'left' },
  body: { ruleType: 'body', font: '宋体', fontLatin: 'Times New Roman', fontSize: 12, lineSpacingType: 'multiple', lineSpacing: 1.5, lineSpacingExact: 20, firstLineIndent: 2, align: 'justify' },
  figure: { ruleType: 'figure', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'below', numberingPattern: '图{chapter}-{no}', captionEnabled: true },
  table: { ruleType: 'table', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'above', numberingPattern: '表{chapter}-{no}', captionEnabled: true }
})

const headings = [
  { key: 'heading1', label: '一级标题格式' },
  { key: 'heading2', label: '二级标题格式' },
  { key: 'heading3', label: '三级标题格式' }
]

const headingPatterns = reactive({
  heading1: '^第[一二三四五六七八九十百]+章',
  heading2: '^\\d+\\.\\d+',
  heading3: '^\\d+\\.\\d+\\.\\d+'
})

const refConfig = reactive({
  enabled: false,
  title: '参考文献',
  titleFont: '黑体',
  titleFontSize: 14,
  itemFont: '宋体',
  itemFontLatin: 'Times New Roman',
  itemFontSize: 10,
  removeDoi: true,
  maxAuthors: 3,
  renumber: true
})

const presets = {
  chinese: { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
  number: { heading1: '^\\d+', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
  cn: { heading1: '^[一二三四五六七八九十百]+', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' }
}

function applyPreset(name) {
  Object.assign(headingPatterns, presets[name])
}

async function loadConfig() {
  const currentId = Number(route.params.id)
  // 重置为默认值, 避免组件复用/切换模板时残留上次配置
  Object.assign(page, { paper: 'A4', margin: { top: 2.5, bottom: 2.5, left: 3, right: 2.5 }, header: { height: 1.5, text: '' }, footer: { pageNumber: 'center' } })
  Object.assign(rules, {
    heading1: { ruleType: 'heading1', font: '黑体', fontLatin: 'Times New Roman', fontSize: 16, bold: true, align: 'center', spaceBefore: 12, spaceAfter: 12 },
    heading2: { ruleType: 'heading2', font: '黑体', fontLatin: 'Times New Roman', fontSize: 14, align: 'left' },
    heading3: { ruleType: 'heading3', font: '楷体', fontLatin: 'Times New Roman', fontSize: 12, align: 'left' },
    body: { ruleType: 'body', font: '宋体', fontLatin: 'Times New Roman', fontSize: 12, lineSpacingType: 'multiple', lineSpacing: 1.5, lineSpacingExact: 20, firstLineIndent: 2, align: 'justify' },
    figure: { ruleType: 'figure', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'below', numberingPattern: '图{chapter}-{no}', captionEnabled: true },
    table: { ruleType: 'table', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'above', numberingPattern: '表{chapter}-{no}', captionEnabled: true }
  })
  Object.assign(headingPatterns, { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' })
  Object.assign(refConfig, { enabled: false, title: '参考文献', titleFont: '黑体', titleFontSize: 14, itemFont: '宋体', itemFontLatin: 'Times New Roman', itemFontSize: 10, removeDoi: true, maxAuthors: 3, renumber: true })
  try {
    const detail = await getTemplateDetail(currentId)
    const t = detail.template
    templateName.value = t.name
    const pg = parseJson(t.pageConfig)
    if (pg) Object.assign(page, pg)
    const hp = parseJson(t.headingPatterns)
    if (hp) Object.assign(headingPatterns, hp)
    const rc = parseJson(t.referenceConfig)
    if (rc) Object.assign(refConfig, rc)
    ;(t.rules || []).forEach(r => {
      if (rules[r.ruleType]) {
        const merged = { ...rules[r.ruleType], ...r }
        if (!merged.fontLatin) merged.fontLatin = 'Times New Roman'
        if (!merged.lineSpacingType) merged.lineSpacingType = 'multiple'
        Object.assign(rules[r.ruleType], merged)
      }
    })
  } catch (e) {
    // 具体原因由 api 拦截器提示(如 模板不存在 / 无权访问该模板), 这里不重复覆盖
    console.error('配置加载失败', e)
  }
}

/** 解析可能带 HTML 实体(如 &quot;)的历史 JSON 数据, 失败返回 null */
function parseJson(s) {
  if (!s) return null
  try {
    return JSON.parse(s)
  } catch (e) {
    try {
      return JSON.parse(decodeHtml(s))
    } catch (e2) {
      return null
    }
  }
}

function decodeHtml(s) {
  return String(s)
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
}

onMounted(loadConfig)
// 组件复用时按模板 id 重新加载(解决退出后再次进入显示旧配置/默认配置)
watch(() => route.params.id, () => {
  loadConfig()
})

async function exportTpl() {
  try {
    const data = await exportTemplate(id)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `模板_${data.name || id}.json`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function saveAll() {
  const currentId = Number(route.params.id)
  saving.value = true
  let failed = 0
  // 页面/标题/参考文献: 逐项保存, 单项失败不中断
  try {
    await savePageConfig(currentId, JSON.stringify(page))
  } catch (e) { failed++; console.error('保存页面设置失败', e) }
  try {
    await saveHeadingPatterns(currentId, { ...headingPatterns })
  } catch (e) { failed++; console.error('保存标题规则失败', e) }
  try {
    await saveReferenceConfig(currentId, { ...refConfig })
  } catch (e) { failed++; console.error('保存参考文献配置失败', e) }
  // 格式规则(标题/正文/图表): 逐条保存, 某条失败不影响其余
  for (const key of Object.keys(rules)) {
    try {
      await saveRule({ ...rules[key], templateId: currentId })
    } catch (e) {
      failed++
      console.error('保存规则失败', key, e)
    }
  }
  saving.value = false
  if (failed > 0) {
    ElMessage.error(`有 ${failed} 项配置保存失败，请重试；已成功的配置已保存`)
  } else {
    ElMessage.success('配置保存成功')
  }
}
</script>

<style scoped>
.config {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 30px;
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
.brand-actions {
  display: flex;
  gap: 8px;
}
.body {
  flex: 1;
  display: flex;
  padding: 20px;
  gap: 20px;
}
.menu {
  width: 180px;
  background: #fff;
  border-radius: 10px;
  padding: 12px 0;
  height: fit-content;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.menu-item {
  padding: 14px 24px;
  cursor: pointer;
  color: #606266;
  border-left: 3px solid transparent;
}
.menu-item:hover {
  color: #409eff;
  background: #f5f7fa;
}
.menu-item.active {
  color: #409eff;
  background: #ecf5ff;
  border-left-color: #409eff;
  font-weight: 600;
}
.panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 24px 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.panel h3 {
  color: #303133;
  margin: 18px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.panel h3:first-child {
  margin-top: 0;
}
.block {
  margin-bottom: 10px;
}
.presets {
  margin: 10px 0 16px;
}
.tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  margin-top: 4px;
}
.tip-inline {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
</style>
