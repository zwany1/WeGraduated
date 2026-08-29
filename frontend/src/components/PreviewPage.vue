<template>
  <div class="preview-page">
    <!-- 页眉 -->
    <div v-if="page.header.text" class="preview-header" :style="headerBarStyle">
      {{ page.header.text }}
    </div>

    <div class="preview-content">
      <!-- ===== 摘要区（启用时展示） ===== -->
      <template v-if="genAbstract">
        <div :style="abstractTitleStyle">摘  要</div>
        <div :style="bodyStyle">随着人工智能技术的快速发展，PLC（Programmable Logic Controller）在工业自动化控制领域的应用日益广泛。本设计针对图书馆模块化仓库物料定位系统，采用分层控制架构，实现了从物料入库、定位到出库的全流程自动化管理，具有高可靠性与易维护性的特点。</div>
        <div :style="kwStyle"><b style="font-family: 黑体, sans-serif;">关键词：</b>PLC控制系统；模块化设计；物料定位；梯形图编程</div>
        <div style="height: 16px;"></div>
      </template>

      <!-- ===== 正文 ===== -->
      <div :style="ruleStyle('heading1')">第一章 绪论</div>
      <div :style="ruleStyle('heading2')">1.1 研究背景与意义</div>
      <div :style="ruleStyle('body')">随着信息技术的快速发展，学术论文的排版规范日益重要。本研究针对毕业论文排版中的标题层级、正文格式、图表题注与参考文献等关键要素，提出一套自动化排版方案。系统基于文档结构识别技术，自动应用格式规则，减少人工排版工作量。</div>
      <div :style="ruleStyle('body')">本设计面向桂林信息科技学院（Guilin University of Information Technology）图书馆模块化仓库，采用 PLC（可编程逻辑控制器）技术，实现物料精准定位与高效管理。系统硬件选用三菱 FX3U 系列，编程语言为梯形图（Ladder Diagram），通信协议支持 Modbus TCP。</div>

      <div :style="ruleStyle('heading3')">1.1.1 国内外研究现状</div>
      <div :style="ruleStyle('body')">国外在智能仓储领域的研究起步较早，德国西门子（Siemens）和日本欧姆龙（OMRON）等企业在 PLC 仓储系统方面已有成熟方案。国内近年来在 GB/T 7714-2015 等论文格式规范的推广下，自动化排版需求显著增长。</div>

      <div :style="ruleStyle('heading2')">1.2 研究目的</div>
      <div :style="ruleStyle('body')">本研究旨在设计一套符合 GBT 1.1-2020 标准的论文自动排版系统，支持标题自动识别、格式一键应用、图表智能编号及参考文献规范排版，为高校毕业生提供高效的论文格式化工具。</div>

      <!-- ===== 图表 ===== -->
      <div :style="captionStyle('figure')">图1-1 系统架构设计图</div>
      <div class="mock-image">
        <span style="color:#909399;font-size:11px;">[图片占位]</span>
      </div>

      <div :style="captionStyle('table')">表1-1 排版规则配置示例</div>
      <table class="mock-table">
        <thead>
          <tr>
            <th :style="thStyle">规则类型</th>
            <th :style="thStyle">中文字体</th>
            <th :style="thStyle">西文字体</th>
            <th :style="thStyle">字号</th>
            <th :style="thStyle">对齐</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in tableRows" :key="i">
            <td :style="tdStyle">{{ row.type }}</td>
            <td :style="tdStyle">{{ row.font }}</td>
            <td :style="tdStyle">{{ row.fontLatin }}</td>
            <td :style="tdStyle">{{ row.size }}</td>
            <td :style="tdStyle">{{ row.align }}</td>
          </tr>
        </tbody>
      </table>

      <!-- ===== 参考文献（启用时展示） ===== -->
      <template v-if="refConfig.enabled">
        <div style="height: 12px;"></div>
        <div :style="refTitleStyle()">{{ refConfig.title || '参考文献' }}</div>
        <div class="ref-hanging" :style="refItemStyle()">
          <span class="ref-num">[1]</span>
          <span>张三, 李四, 王五. 基于 Apache POI 的论文自动排版系统研究与实现[J]. 计算机工程与应用, 2024, 60(3): 120-128.</span>
        </div>
        <div class="ref-hanging" :style="refItemStyle()">
          <span class="ref-num">[2]</span>
          <span>Smith J, Brown A, Lee C. Automated document formatting using rule-based systems[C]//Proc. of ICDM. 2023: 45-52.</span>
        </div>
        <div class="ref-hanging" :style="refItemStyle()">
          <span class="ref-num">[3]</span>
          <span>王六. 基于 Word 文档解析技术的排版引擎设计[D]. 北京: 清华大学, 2023.</span>
        </div>
      </template>
    </div>

    <!-- 页脚/页码 -->
    <div class="preview-footer">
      <span v-if="page.footer.pageNumber === 'center'" style="text-align:center;display:block;">— 1 —</span>
      <span v-else-if="page.footer.pageNumber === 'left'" style="text-align:left;display:block;">— 1 —</span>
      <span v-else-if="page.footer.pageNumber === 'right'" style="text-align:right;display:block;">— 1 —</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Object, required: true },
  rules: { type: Object, required: true },
  refConfig: { type: Object, required: true },
  genAbstract: { type: Boolean, default: false }
})

const sizeMap = { 9: '小五', 10: '五号', 12: '小四', 14: '四号', 15: '小三', 16: '三号', 18: '小二', 22: '二号', 24: '小一', 26: '一号' }
const alignMap = { left: '左对齐', center: '居中', right: '右对齐', justify: '两端对齐' }

/** 按规则生成内联样式对象, 供预览区实时反映当前配置 */
function ruleStyle(key) {
  const r = props.rules[key]
  if (!r) return {}
  return {
    fontFamily: `'${r.font || '宋体'}', '${r.fontLatin || 'Times New Roman'}', serif`,
    fontSize: (r.fontSize || 12) + 'pt',
    fontWeight: r.bold ? '700' : '400',
    textAlign: r.align || 'left',
    lineHeight: r.lineSpacingType === 'exact' ? (r.lineSpacingExact || 20) + 'pt' : (r.lineSpacing || 1.5),
    textIndent: r.firstLineIndent ? r.firstLineIndent + 'em' : '0',
    marginTop: (r.spaceBefore || 0) + 'pt',
    marginBottom: (r.spaceAfter || 0) + 'pt'
  }
}

function captionStyle(key) {
  const r = props.rules[key]
  if (!r) return {}
  return {
    fontFamily: `'${r.font || '宋体'}', '${r.fontLatin || 'Times New Roman'}', serif`,
    fontSize: (r.fontSize || 10) + 'pt',
    textAlign: 'center',
    margin: '6pt 0'
  }
}

function refTitleStyle() {
  const rc = props.refConfig
  return {
    fontFamily: `'${rc.titleFont || '黑体'}', serif`,
    fontSize: (rc.titleFontSize || 14) + 'pt',
    fontWeight: '700',
    textAlign: 'left',
    marginTop: '12pt',
    marginBottom: '6pt'
  }
}

function refItemStyle() {
  const rc = props.refConfig
  return {
    fontFamily: `'${rc.itemFont || '宋体'}', '${rc.itemFontLatin || 'Times New Roman'}', serif`,
    fontSize: (rc.itemFontSize || 10) + 'pt',
    textAlign: 'left',
    lineHeight: 1.5,
    marginBottom: '2pt'
  }
}

/** 页眉样式 */
const headerBarStyle = computed(() => ({
  fontFamily: `'${props.rules.body.font || '宋体'}', '${props.rules.body.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (props.rules.body.fontSize || 12) + 'pt',
  textAlign: 'center',
  borderBottom: '1px solid #333',
  paddingBottom: '4pt',
  marginBottom: '16pt'
}))

/** 摘要标题样式（黑体三号居中加粗） */
const abstractTitleStyle = computed(() => ({
  fontFamily: "'黑体', 'SimHei', sans-serif",
  fontSize: '16pt',
  fontWeight: '700',
  textAlign: 'center',
  marginTop: '0',
  marginBottom: '12pt'
}))

/** 正文段落样式（从 body 规则提取，简化版） */
const bodyStyle = computed(() => {
  const r = props.rules.body
  return {
    fontFamily: `'${r.font || '宋体'}', '${r.fontLatin || 'Times New Roman'}', serif`,
    fontSize: (r.fontSize || 12) + 'pt',
    fontWeight: '400',
    textAlign: r.align || 'justify',
    lineHeight: r.lineSpacingType === 'exact' ? (r.lineSpacingExact || 20) + 'pt' : (r.lineSpacing || 1.5),
    textIndent: r.firstLineIndent ? r.firstLineIndent + 'em' : '0',
    marginBottom: '6pt'
  }
})

/** 关键词样式 */
const kwStyle = computed(() => {
  const r = props.rules.body
  return {
    fontFamily: `'${r.font || '宋体'}', '${r.fontLatin || 'Times New Roman'}', serif`,
    fontSize: (r.fontSize || 12) + 'pt',
    textAlign: 'left',
    marginTop: '4pt',
    marginBottom: '12pt'
  }
})

/** 表头样式 */
const thStyle = computed(() => ({
  fontFamily: `'${props.rules.tableText.font || '宋体'}', '${props.rules.tableText.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (props.rules.tableText.fontSize || 10) + 'pt',
  fontWeight: (props.rules.tableText.bold ? '700' : '400') || '600',
  border: '1px solid #999',
  padding: '4pt 8pt',
  textAlign: props.rules.tableText.align || 'center',
  background: '#f5f7fa'
}))

/** 表格单元格样式 */
const tdStyle = computed(() => ({
  fontFamily: `'${props.rules.tableText.font || '宋体'}', '${props.rules.tableText.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (props.rules.tableText.fontSize || 10) + 'pt',
  fontWeight: props.rules.tableText.bold ? '700' : '400',
  border: '1px solid #999',
  padding: '4pt 8pt',
  textAlign: props.rules.tableText.align || 'center'
}))

/** 表格示例数据 */
const tableRows = computed(() => [
  { type: '一级标题', font: props.rules.heading1.font, fontLatin: props.rules.heading1.fontLatin, size: sizeMap[props.rules.heading1.fontSize] + '(' + props.rules.heading1.fontSize + 'pt)', align: alignMap[props.rules.heading1.align] },
  { type: '二级标题', font: props.rules.heading2.font, fontLatin: props.rules.heading2.fontLatin, size: sizeMap[props.rules.heading2.fontSize] + '(' + props.rules.heading2.fontSize + 'pt)', align: alignMap[props.rules.heading2.align] },
  { type: '正文', font: props.rules.body.font, fontLatin: props.rules.body.fontLatin, size: sizeMap[props.rules.body.fontSize] + '(' + props.rules.body.fontSize + 'pt)', align: alignMap[props.rules.body.align] },
  { type: '图表题注', font: props.rules.figure.font, fontLatin: props.rules.figure.fontLatin, size: sizeMap[props.rules.figure.fontSize] + '(' + props.rules.figure.fontSize + 'pt)', align: '居中' }
])
</script>

<style scoped>
/* 预览区纸张模拟 */
.preview-page {
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  max-width: 720px;
  margin-top: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
.preview-header {
  padding: 6pt 0 4pt;
  font-size: 10.5pt;
  color: #333;
}
.preview-content {
  padding: 24pt 48pt;
}
.preview-footer {
  border-top: 1px solid #d0d7de;
  padding: 6pt 48pt;
  font-size: 10.5pt;
  color: #606266;
}
.mock-image {
  width: 100%;
  height: 80px;
  background: #f5f5f5;
  border: 1px dashed #ccc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4pt;
}
.mock-table {
  width: 100%;
  border-collapse: collapse;
  margin: 4pt 0 8pt;
}
.ref-hanging {
  padding-left: 0;
  text-indent: -2.5em;
  margin-left: 2.5em;
}
</style>
