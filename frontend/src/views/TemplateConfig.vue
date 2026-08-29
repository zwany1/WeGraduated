<template>
  <div class="config">
    <SiteNav>
      <el-button type="success" @click="goFormat">用此方案排版</el-button>
      <el-button type="primary" :loading="saving" @click="saveAll">保存配置</el-button>
    </SiteNav>

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
          <h3>排版开关</h3>
          <el-form label-width="120px" style="max-width: 520px">
            <el-form-item label="目录排版">
              <el-switch v-model="genToc" />
              <span class="tip-inline">启用则对已有目录补样式、纯文本目录套字体；未启用则目录原样不动</span>
            </el-form-item>
            <el-form-item label="摘要排版">
              <el-switch v-model="genAbstract" />
              <span class="tip-inline">启用则对摘要/Abstract/关键词套规范格式；未启用则原样不动</span>
            </el-form-item>
          </el-form>
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
          <div class="block">
            <h3>表格文字（单元格内）</h3>
            <p class="tip">设置表格单元格内正文的字体、字号、对齐等；区别于表格题注（上方的"表X-X"标注）</p>
            <el-form label-width="100px" style="max-width: 560px">
              <el-form-item label="字体">
                <el-select v-model="rules.tableText.font">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="西文字体">
                <el-select v-model="rules.tableText.fontLatin">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="字号">
                <el-select v-model="rules.tableText.fontSize">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
              <el-form-item label="加粗"><el-switch v-model="rules.tableText.bold" /></el-form-item>
              <el-form-item label="对齐">
                <el-select v-model="rules.tableText.align">
                  <el-option v-for="a in aligns" :key="a.v" :label="a.label" :value="a.v" />
                </el-select>
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

        <!-- 目录样式设置: 行距/前导符三级共用, 字体/字号各级独立 -->
        <template v-else-if="active === 'toc'">
          <el-form label-width="130px" style="max-width: 620px">
            <el-form-item label="行距">
              <el-select v-model="tocConfig.lineSpacing" style="max-width: 220px">
                <el-option v-for="ls in lineSpacings" :key="ls" :label="ls + ' 倍'" :value="ls" />
              </el-select>
              <span class="tip-inline">各级目录条目共用</span>
            </el-form-item>
            <el-form-item label="制表位前导符">
              <el-select v-model="tocConfig.leader" style="max-width: 220px">
                <el-option label="保留原文" value="" />
                <el-option label="无" value="none" />
                <el-option label="点线(……)" value="dot" />
                <el-option label="短划线(——)" value="hyphen" />
                <el-option label="下划线(___)" value="underscore" />
                <el-option label="中点(···)" value="middleDot" />
              </el-select>
              <span class="tip-inline">各级共用;选「保留原文」则不改文档已有的制表符</span>
            </el-form-item>
          </el-form>
          <template v-for="lv in tocLevels" :key="lv.key">
            <h4 style="margin: 16px 0 8px; font-size: 15px; color: #303133;">{{ lv.label }}</h4>
            <el-form label-width="130px" style="max-width: 620px">
              <el-form-item label="中文字体">
                <el-select v-model="tocConfig[lv.key].font" style="max-width: 220px">
                  <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="西文字体">
                <el-select v-model="tocConfig[lv.key].fontLatin" style="max-width: 220px">
                  <el-option v-for="f in latinFonts" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="字号">
                <el-select v-model="tocConfig[lv.key].fontSize" style="max-width: 220px">
                  <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
                </el-select>
              </el-form-item>
            </el-form>
          </template>
          <div class="tip">行距/前导符三级共用,字体/字号各级独立;作用于 Word 自动目录的 toc1/toc2/toc3 样式,不改目录域的自动更新标识——用户在 Word 按 F9 仍可重新生成目录。</div>
        </template>

        <!-- 效果预览: 实时 CSS 模拟 + 上传文档快速试排 -->
        <template v-else-if="active === 'preview'">
          <h3>排版效果预览</h3>
          <p class="tip">下方是按当前规则的实时模拟（字体/字号/行距/缩进/对齐/段距/页眉页脚/图表/参考文献），改配置立即生效。要看真实排版引擎的结果，用"快速试排"上传文档即可，几秒出结果，不产生排版任务。</p>
          <PreviewPage :page="page" :rules="rules" :ref-config="refConfig" :gen-abstract="genAbstract" />

          <h3 style="margin-top: 28px;">快速试排（前几页真实排版）</h3>
          <div class="quick-format">
            <input ref="quickFileRef" type="file" accept=".docx" class="quick-file" @change="onQuickFile" />
            <el-button type="primary" size="small" :loading="quickLoading" :disabled="!quickFile" @click="runQuickFormat">试排前 {{ quickParagraphs }} 段</el-button>
            <el-select v-model="quickParagraphs" size="small" style="width: 120px; margin-left: 8px;">
              <el-option label="前 60 段" :value="60" />
              <el-option label="前 150 段" :value="150" />
              <el-option label="前 300 段" :value="300" />
            </el-select>
            <span v-if="quickFile" class="quick-filename">{{ quickFile.name }}</span>
          </div>
          <p class="tip">试排只取文档开头部分同步排版，与正式排版使用同一引擎，用于快速验证标题正则与格式规则；不会在任务列表产生记录。</p>
          <div v-if="quickData.length" class="quick-result">
            <div class="quick-result-bar">
              <span>试排结果（真实引擎排版）</span>
              <el-button size="small" @click="downloadQuick">下载 .docx</el-button>
            </div>
            <DocxCompare :data="quickData" :headings="quickHeadings" />
          </div>
        </template>
      </section>
    </main>

    <!-- 右侧实时预览 dock: 在任意配置 tab 调参数即时看效果 -->
    <button class="live-preview-toggle" :class="{ on: livePreview }" @click="toggleLivePreview">
      {{ livePreview ? '收起预览' : '实时预览' }}
    </button>
    <div v-if="livePreview" class="live-preview-dock">
      <div class="live-preview-tip">实时预览（跟随当前配置）</div>
      <div class="live-preview-scroll">
        <div class="live-preview-scale">
          <PreviewPage :page="page" :rules="rules" :ref-config="refConfig" :gen-abstract="genAbstract" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteNav from '../components/SiteNav.vue'
import PreviewPage from '../components/PreviewPage.vue'
import DocxCompare from '../components/DocxCompare.vue'
import { extractHeadings } from '../utils/docxHeadings'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { getTemplateDetail, saveAllConfig } from '../api/template'
import { quickFormat } from '../api/paper'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const menus = [
  { key: 'page', label: '页面设置' },
  { key: 'heading', label: '标题格式' },
  { key: 'body', label: '正文格式' },
  { key: 'figure', label: '图表格式' },
  { key: 'reference', label: '参考文献' },
  { key: 'toc', label: '目录样式' },
  { key: 'preview', label: '效果预览' }
]
const active = ref('page')
const templateName = ref('格式方案')
const saving = ref(false)
const loading = ref(false)
const genToc = ref(false)
const genAbstract = ref(false)

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
  table: { ruleType: 'table', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'above', numberingPattern: '表{chapter}-{no}', captionEnabled: true },
  tableText: { ruleType: 'tableText', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, bold: false, align: 'center', lineSpacing: 1.5 }
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

const tocConfig = reactive({
  lineSpacing: 1.5,
  leader: 'dot',
  toc1: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 14 },
  toc2: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 },
  toc3: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 }
})
const tocLevels = [
  { key: 'toc1', label: '一级标题条目' },
  { key: 'toc2', label: '二级标题条目' },
  { key: 'toc3', label: '三级标题条目' }
]

const presets = {
  chinese: { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
  number: { heading1: '^\\d+', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
  cn: { heading1: '^[一二三四五六七八九十百]+', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' }
}

function applyPreset(name) {
  Object.assign(headingPatterns, presets[name])
}

// ===== 快速试排: 上传文档截取前几段, 用真实排版引擎同步出结果 =====
const quickFileRef = ref(null)
const quickFile = ref(null)
const quickParagraphs = ref(150)
const quickLoading = ref(false)
const quickData = ref([])
const quickHeadings = ref([])

function onQuickFile(e) {
  quickFile.value = e.target.files && e.target.files[0] ? e.target.files[0] : null
  quickData.value = []
  quickHeadings.value = []
}

async function runQuickFormat() {
  if (!quickFile.value) return
  quickLoading.value = true
  try {
    const blob = await quickFormat(id, quickFile.value, quickParagraphs.value)
    const buf = await blob.arrayBuffer()
    quickData.value = Array.from(new Uint8Array(buf))
    quickHeadings.value = await extractHeadings(buf)
    ElMessage.success('试排完成')
  } catch (e) {
    // 具体原因由 api 拦截器提示
  } finally {
    quickLoading.value = false
  }
}

function downloadQuick() {
  if (!quickData.value.length) return
  const blob = new Blob([new Uint8Array(quickData.value)], {
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '试排预览.docx'
  a.click()
  URL.revokeObjectURL(url)
}

// ===== 右侧实时预览 dock =====
const livePreview = ref(localStorage.getItem('tc-live-preview') === '1')

function toggleLivePreview() {
  livePreview.value = !livePreview.value
  localStorage.setItem('tc-live-preview', livePreview.value ? '1' : '0')
}

const alignMap = { left: '左对齐', center: '居中', right: '右对齐', justify: '两端对齐' }

async function loadConfig() {
  const currentId = Number(route.params.id)
  loading.value = true
  // 重置为默认值, 避免组件复用/切换模板时残留上次配置
  Object.assign(page, { paper: 'A4', margin: { top: 2.5, bottom: 2.5, left: 3, right: 2.5 }, header: { height: 1.5, text: '' }, footer: { pageNumber: 'center' } })
  Object.assign(rules, {
    heading1: { ruleType: 'heading1', font: '黑体', fontLatin: 'Times New Roman', fontSize: 16, bold: true, align: 'center', spaceBefore: 12, spaceAfter: 12 },
    heading2: { ruleType: 'heading2', font: '黑体', fontLatin: 'Times New Roman', fontSize: 14, align: 'left' },
    heading3: { ruleType: 'heading3', font: '楷体', fontLatin: 'Times New Roman', fontSize: 12, align: 'left' },
    body: { ruleType: 'body', font: '宋体', fontLatin: 'Times New Roman', fontSize: 12, lineSpacingType: 'multiple', lineSpacing: 1.5, lineSpacingExact: 20, firstLineIndent: 2, align: 'justify' },
    figure: { ruleType: 'figure', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'below', numberingPattern: '图{chapter}-{no}', captionEnabled: true },
    table: { ruleType: 'table', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'above', numberingPattern: '表{chapter}-{no}', captionEnabled: true },
    tableText: { ruleType: 'tableText', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, bold: false, align: 'center', lineSpacing: 1.5 }
  })
  Object.assign(headingPatterns, { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' })
  Object.assign(refConfig, { enabled: false, title: '参考文献', titleFont: '黑体', titleFontSize: 14, itemFont: '宋体', itemFontLatin: 'Times New Roman', itemFontSize: 10, removeDoi: true, maxAuthors: 3, renumber: true })
  tocConfig.lineSpacing = 1.5
  tocConfig.leader = 'dot'
  Object.assign(tocConfig.toc1, { font: '宋体', fontLatin: 'Times New Roman', fontSize: 14 })
  Object.assign(tocConfig.toc2, { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 })
  Object.assign(tocConfig.toc3, { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 })
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
    const tc = parseJson(t.tocConfig)
    if (tc) {
      if (tc.lineSpacing != null) tocConfig.lineSpacing = tc.lineSpacing
      if (tc.leader != null) tocConfig.leader = tc.leader
      if (tc.toc1) Object.assign(tocConfig.toc1, tc.toc1)
      if (tc.toc2) Object.assign(tocConfig.toc2, tc.toc2)
      if (tc.toc3) Object.assign(tocConfig.toc3, tc.toc3)
    }
    genToc.value = !!t.generateToc
    genAbstract.value = !!t.generateAbstract
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
  await maybeRestoreDraft(currentId)
  startDraftAutosave()
  loading.value = false
}

// ===== 草稿自动暂存: 编辑内容实时写入 localStorage, 后端不可用/登录过期时避免丢失 =====
const DRAFT_PREFIX = 'template-config-draft-'
let draftWatchStarted = false

function draftKey(templateId) {
  return DRAFT_PREFIX + templateId
}

function persistDraft(templateId) {
  try {
    localStorage.setItem(draftKey(templateId), JSON.stringify({
      page: JSON.parse(JSON.stringify(page)),
      rules: JSON.parse(JSON.stringify(rules)),
      headingPatterns: { ...headingPatterns },
      refConfig: { ...refConfig },
      tocConfig: JSON.parse(JSON.stringify(tocConfig)),
      genToc: genToc.value,
      genAbstract: genAbstract.value,
      savedAt: Date.now()
    }))
  } catch (e) {
    // 存储满/序列化失败等异常忽略, 不影响正常编辑
  }
}

function clearDraft(templateId) {
  localStorage.removeItem(draftKey(templateId))
}

async function maybeRestoreDraft(templateId) {
  let draft = null
  try {
    draft = JSON.parse(localStorage.getItem(draftKey(templateId)) || 'null')
  } catch (e) {
    draft = null
  }
  if (!draft || !draft.savedAt) return
  try {
    await ElMessageBox.confirm(
      `检测到 ${new Date(draft.savedAt).toLocaleString()} 的未保存编辑（上次保存可能未成功）。是否恢复？`,
      '恢复未保存的配置',
      { confirmButtonText: '恢复', cancelButtonText: '丢弃', type: 'info' }
    )
    if (draft.page) Object.assign(page, draft.page)
    if (draft.rules) Object.assign(rules, draft.rules)
    if (draft.headingPatterns) Object.assign(headingPatterns, draft.headingPatterns)
    if (draft.refConfig) Object.assign(refConfig, draft.refConfig)
    const tc = draft.tocConfig
    if (tc) {
      if (tc.lineSpacing != null) tocConfig.lineSpacing = tc.lineSpacing
      if (tc.leader != null) tocConfig.leader = tc.leader
      if (tc.toc1) Object.assign(tocConfig.toc1, tc.toc1)
      if (tc.toc2) Object.assign(tocConfig.toc2, tc.toc2)
      if (tc.toc3) Object.assign(tocConfig.toc3, tc.toc3)
    }
    genToc.value = !!draft.genToc
    genAbstract.value = !!draft.genAbstract
  } catch (e) {
    clearDraft(templateId)
  }
}

function startDraftAutosave() {
  if (draftWatchStarted) return
  draftWatchStarted = true
  watch(
    [page, rules, headingPatterns, refConfig, tocConfig, genToc, genAbstract],
    () => {
      if (!loading.value) persistDraft(Number(route.params.id))
    },
    { deep: true }
  )
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

async function saveAll() {
  const currentId = Number(route.params.id)
  saving.value = true
  try {
    await saveAllConfig(currentId, {
      pageConfig: JSON.stringify(page),
      heading1: headingPatterns.heading1,
      heading2: headingPatterns.heading2,
      heading3: headingPatterns.heading3,
      generateToc: genToc.value,
      generateAbstract: genAbstract.value,
      referenceConfig: JSON.stringify(refConfig),
      tocConfig: JSON.stringify({ lineSpacing: tocConfig.lineSpacing, leader: tocConfig.leader, toc1: { ...tocConfig.toc1 }, toc2: { ...tocConfig.toc2 }, toc3: { ...tocConfig.toc3 } }),
      rules: Object.keys(rules).map(key => ({ ...rules[key], templateId: currentId }))
    })
    clearDraft(currentId)
    ElMessage.success('配置保存成功')
    return true
  } catch (e) {
    // api 拦截器已 toast 具体原因(如 后端未启动/正在重启), 这里再弹出汇总通知
    ElNotification({
      title: '保存失败',
      message: (e && e.message) ? String(e.message) : '请稍后重试；后端可能正在重启',
      type: 'error',
      duration: 6000
    })
    return false
  } finally {
    saving.value = false
  }
}

/** 保存当前配置后跳转任务页并预选本方案 */
async function goFormat() {
  const ok = await saveAll()
  if (ok) router.push({ path: '/tasks', query: { templateId: String(id) } })
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
/* 快速试排 */
.quick-format {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}
.quick-file {
  font-size: 12px;
}
.quick-filename {
  font-size: 12px;
  color: #606266;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.quick-result {
  margin-top: 14px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  background: #fff;
}
.quick-result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  color: #303133;
}
/* 右侧实时预览 dock */
.live-preview-toggle {
  position: fixed;
  right: 14px;
  bottom: 56px;
  z-index: 1500;
  padding: 7px 14px;
  border: 1px solid #d0d7de;
  border-radius: 16px;
  background: #fff;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.live-preview-toggle.on {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.live-preview-dock {
  position: fixed;
  right: 0;
  top: 70px;
  bottom: 0;
  width: 480px;
  z-index: 1400;
  background: #f5f7fa;
  border-left: 1px solid #d0d7de;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}
.live-preview-tip {
  padding: 8px 14px;
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}
.live-preview-scroll {
  flex: 1;
  overflow: auto;
  padding: 14px;
}
.live-preview-scale {
  transform: scale(0.62);
  transform-origin: top left;
  width: 760px;
}
</style>
