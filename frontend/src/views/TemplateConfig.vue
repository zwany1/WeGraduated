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

        <!-- 效果预览: 按当前规则用 CSS 模拟排版样式 -->
        <template v-else-if="active === 'preview'">
          <h3>排版效果预览</h3>
          <p class="tip">按当前规则的格式样式实时预览（字体/字号/行距/缩进/对齐/段距/页眉页脚/图表/参考文献）。标题识别正则、题注自动编号、参考文献序号重排等结构处理需实际排版后体现。</p>

          <!-- 模拟论文纸张 -->
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
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteNav from '../components/SiteNav.vue'
import { ElMessage } from 'element-plus'
import { getTemplateDetail, saveGenerateAbstract, saveGenerateToc, savePageConfig, saveHeadingPatterns, saveReferenceConfig, saveRule, saveTocConfig } from '../api/template'

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

/** 按规则生成内联样式对象, 供预览区实时反映当前配置 */
function ruleStyle(key) {
  const r = rules[key]
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
  const r = rules[key]
  if (!r) return {}
  return {
    fontFamily: `'${r.font || '宋体'}', '${r.fontLatin || 'Times New Roman'}', serif`,
    fontSize: (r.fontSize || 10) + 'pt',
    textAlign: 'center',
    margin: '6pt 0'
  }
}

function refTitleStyle() {
  return {
    fontFamily: `'${refConfig.titleFont || '黑体'}', serif`,
    fontSize: (refConfig.titleFontSize || 14) + 'pt',
    fontWeight: '700',
    textAlign: 'left',
    marginTop: '12pt',
    marginBottom: '6pt'
  }
}

function refItemStyle() {
  return {
    fontFamily: `'${refConfig.itemFont || '宋体'}', '${refConfig.itemFontLatin || 'Times New Roman'}', serif`,
    fontSize: (refConfig.itemFontSize || 10) + 'pt',
    textAlign: 'left',
    lineHeight: 1.5,
    marginBottom: '2pt'
  }
}

// ===== 预览区新增样式 =====

/** 页眉样式 */
const headerBarStyle = computed(() => ({
  fontFamily: `'${rules.body.font || '宋体'}', '${rules.body.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (rules.body.fontSize || 12) + 'pt',
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
  const r = rules.body
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
  const r = rules.body
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
  fontFamily: `'${rules.table.font || '宋体'}', '${rules.table.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (rules.table.fontSize || 10) + 'pt',
  fontWeight: '600',
  border: '1px solid #999',
  padding: '4pt 8pt',
  textAlign: 'center',
  background: '#f5f7fa'
}))

/** 表格单元格样式 */
const tdStyle = computed(() => ({
  fontFamily: `'${rules.table.font || '宋体'}', '${rules.table.fontLatin || 'Times New Roman'}', serif`,
  fontSize: (rules.table.fontSize || 10) + 'pt',
  border: '1px solid #999',
  padding: '4pt 8pt',
  textAlign: 'center'
}))

/** 表格示例数据 */
const tableRows = computed(() => [
  { type: '一级标题', font: rules.heading1.font, fontLatin: rules.heading1.fontLatin, size: sizeMap[rules.heading1.fontSize] + '(' + rules.heading1.fontSize + 'pt)', align: alignMap[rules.heading1.align] },
  { type: '二级标题', font: rules.heading2.font, fontLatin: rules.heading2.fontLatin, size: sizeMap[rules.heading2.fontSize] + '(' + rules.heading2.fontSize + 'pt)', align: alignMap[rules.heading2.align] },
  { type: '正文', font: rules.body.font, fontLatin: rules.body.fontLatin, size: sizeMap[rules.body.fontSize] + '(' + rules.body.fontSize + 'pt)', align: alignMap[rules.body.align] },
  { type: '图表题注', font: rules.figure.font, fontLatin: rules.figure.fontLatin, size: sizeMap[rules.figure.fontSize] + '(' + rules.figure.fontSize + 'pt)', align: '居中' }
])

const alignMap = { left: '左对齐', center: '居中', right: '右对齐', justify: '两端对齐' }

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
  try {
    await saveTocConfig(currentId, { lineSpacing: tocConfig.lineSpacing, leader: tocConfig.leader, toc1: { ...tocConfig.toc1 }, toc2: { ...tocConfig.toc2 }, toc3: { ...tocConfig.toc3 } })
  } catch (e) { failed++; console.error('保存目录样式失败', e) }
  try {
    await saveGenerateToc(currentId, genToc.value)
  } catch (e) { failed++; console.error('保存目录开关失败', e) }
  try {
    await saveGenerateAbstract(currentId, genAbstract.value)
  } catch (e) { failed++; console.error('保存摘要开关失败', e) }
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
    return false
  } else {
    ElMessage.success('配置保存成功')
    return true
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
