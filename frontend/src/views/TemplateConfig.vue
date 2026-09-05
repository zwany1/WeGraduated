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
          <h3>校规一键预设</h3>
          <p class="tip">选择贴近你学校要求的预设一键填充全部配置（页面/标题/正文/图表/参考文献/目录），套用后再按校规微调即可。</p>
          <div class="school-presets">
            <div v-for="(p, key) in schoolPresets" :key="key" class="preset-card" @click="applySchoolPreset(key)">
              <div class="preset-name">{{ p.label }}</div>
              <div class="preset-desc">{{ p.desc }}</div>
            </div>
            <div class="preset-card spec-card" @click="openSpecWizard">
              <div class="preset-name">从校规文档导入
                <el-tag size="small" type="success" style="margin-left: 4px">智能</el-tag>
              </div>
              <div class="preset-desc">上传学校《格式规范》.docx，自动抽取字体/字号/行距/边距/标题编号生成配置初稿，可逐项修改</div>
            </div>
          </div>
          <h3 style="margin-top: 20px">排版开关</h3>
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
                <el-input v-model="headingPatterns.heading1" :class="{ 'regex-invalid': !regexValid.heading1 }" placeholder="^第[一二三四五六七八九十百]+章">
                  <template #append>
                    <el-dropdown trigger="click" @command="cmd => insertPattern('heading1', cmd)">
                      <span class="pattern-more">常用<el-icon><ArrowDown /></el-icon></span>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="p in commonPatterns.heading1" :key="p" :command="p">{{ p }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                </el-input>
                <div v-if="!regexValid.heading1" class="regex-error">不是合法的正则表达式</div>
              </el-form-item>
              <el-form-item label="二级标题">
                <el-input v-model="headingPatterns.heading2" :class="{ 'regex-invalid': !regexValid.heading2 }" placeholder="^\d+\.\d+">
                  <template #append>
                    <el-dropdown trigger="click" @command="cmd => insertPattern('heading2', cmd)">
                      <span class="pattern-more">常用<el-icon><ArrowDown /></el-icon></span>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="p in commonPatterns.heading2" :key="p" :command="p">{{ p }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                </el-input>
                <div v-if="!regexValid.heading2" class="regex-error">不是合法的正则表达式</div>
              </el-form-item>
              <el-form-item label="三级标题">
                <el-input v-model="headingPatterns.heading3" :class="{ 'regex-invalid': !regexValid.heading3 }" placeholder="^\d+\.\d+\.\d+">
                  <template #append>
                    <el-dropdown trigger="click" @command="cmd => insertPattern('heading3', cmd)">
                      <span class="pattern-more">常用<el-icon><ArrowDown /></el-icon></span>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="p in commonPatterns.heading3" :key="p" :command="p">{{ p }}</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                </el-input>
                <div v-if="!regexValid.heading3" class="regex-error">不是合法的正则表达式</div>
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
            <el-form-item>
              <template #label>
                <span>字号
                  <el-tooltip content="中文字号对应磅值：三号=16、四号=14、小四=12、五号=10；校规一般标注字号名称" placement="top">
                    <el-icon class="q-mark"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <el-select v-model="rules.body.fontSize">
                <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <template #label>
                <span>行距类型
                  <el-tooltip content="多倍行距随字号缩放；固定值以磅为单位精确控制，研究生规范常用固定 20 磅" placement="top">
                    <el-icon class="q-mark"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
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
              <span style="margin-left:8px;color:#8B968D">磅</span>
            </el-form-item>
            <el-form-item>
              <template #label>
                <span>首行缩进
                  <el-tooltip content="1 字符约一个汉字宽度，本科校规通常要求 2 字符" placement="top">
                    <el-icon class="q-mark"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <el-input-number v-model="rules.body.firstLineIndent" :min="0" :max="8" /><span style="margin-left:8px;color:#8B968D">字符</span>
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
            <h4 style="margin: 16px 0 8px; font-size: 15px; color: #24312A;">{{ lv.label }}</h4>
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

        <!-- 快速试排: 上传文档截取前几段, 用真实排版引擎同步出结果(实时样式预览见右侧 dock) -->
        <template v-else-if="active === 'preview'">
          <h3>快速试排</h3>
          <p class="tip">选一篇 .docx，只取开头部分用真实排版引擎同步排版，几秒即可验证标题正则与格式规则；不会在任务列表产生记录。样式的实时模拟效果见右下角"实时预览"。</p>

          <div class="quick-card">
            <el-upload
              drag
              accept=".docx"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="onQuickUpload"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">将 .docx 拖到此处，或<em>点击选择文件</em></div>
              <template #tip>
                <div class="el-upload__tip">仅取文档开头部分参与排版，大文件也不会很慢</div>
              </template>
            </el-upload>

            <div class="quick-actions">
              <span v-if="quickFile" class="quick-filename" :title="quickFile.name">
                <el-icon style="vertical-align: -2px;"><Document /></el-icon>
                {{ quickFile.name }}
              </span>
              <span v-else class="quick-filename placeholder">未选择文件</span>
              <div class="quick-actions-right">
                <span class="quick-range-label">试排范围</span>
                <el-select v-model="quickParagraphs" size="small" style="width: 130px">
                  <el-option label="前 60 段（最快）" :value="60" />
                  <el-option label="前 150 段（推荐）" :value="150" />
                  <el-option label="前 300 段（更完整）" :value="300" />
                </el-select>
                <el-button type="primary" :loading="quickLoading" :disabled="!quickFile" @click="runQuickFormat">
                  {{ quickLoading ? '排版中…' : '开始试排' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="quickData.length" class="quick-result">
            <div class="quick-result-bar">
              <span class="quick-result-title">
                <span class="dot-ok"></span>试排完成（真实引擎排版，与正式排版同规则）
              </span>
              <el-button size="small" @click="downloadQuick">下载 .docx</el-button>
            </div>
            <div class="quick-result-body">
              <DocxCompare :data="quickData" :headings="quickHeadings" />
            </div>
          </div>
        </template>
      </section>
    </main>

    <!-- 校规文档导入向导 -->
    <el-dialog v-model="specVisible" title="从校规文档导入" width="760px" top="5vh" :close-on-click-modal="false" destroy-on-close>
      <div v-if="!specResult">
        <el-upload drag accept=".docx" :auto-upload="false" :show-file-list="false" :on-change="onSpecFile">
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将学校《格式规范》.docx 拖到此处，或<em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">自动识别字体/字号/行距/页边距/标题编号等表述，识别结果可逐项修改</div>
          </template>
        </el-upload>
        <div v-if="specLoading" v-loading="true" element-loading-text="正在解析校规文档…" style="height: 70px; margin-top: 12px"></div>
      </div>
      <div v-else>
        <el-alert type="info" :closable="false" style="margin-bottom: 12px"
          :title="`已解析 ${specResult.paragraphCount} 个段落，命中 ${Object.keys(specQuotes).length} 项；灰色小字为原文依据，未识别项保持当前值`" />

        <h4 class="spec-group">页面</h4>
        <div class="spec-row">
          <label>纸张/边距</label>
          <el-select v-model="specDraft.paper" size="small" style="width: 90px">
            <el-option label="A4" value="A4" /><el-option label="B5" value="B5" />
          </el-select>
          <el-input-number v-model="specDraft.margin.top" size="small" :min="0" :max="10" :step="0.1" />
          <el-input-number v-model="specDraft.margin.bottom" size="small" :min="0" :max="10" :step="0.1" />
          <el-input-number v-model="specDraft.margin.left" size="small" :min="0" :max="10" :step="0.1" />
          <el-input-number v-model="specDraft.margin.right" size="small" :min="0" :max="10" :step="0.1" />
          <span class="spec-unit">cm（上/下/左/右）</span>
        </div>
        <div class="spec-quote" style="margin-left: 80px">{{ quoteFor('page.paper') || quoteFor('page.margin上下') || quoteFor('page.margin左右') || '未识别，保持当前值' }}</div>

        <h4 class="spec-group">标题（正则 + 字体字号）</h4>
        <div v-for="lv in ['heading1', 'heading2', 'heading3']" :key="lv" class="spec-block">
          <div class="spec-row">
            <label>{{ lvName(lv) }}</label>
            <el-input v-model="specDraft[lv].pattern" size="small" placeholder="识别正则" style="width: 180px" />
            <el-select v-model="specDraft[lv].font" size="small" style="width: 100px" placeholder="字体">
              <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
            </el-select>
            <el-select v-model="specDraft[lv].fontSize" size="small" style="width: 120px" placeholder="字号">
              <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
            </el-select>
            <el-select v-model="specDraft[lv].align" size="small" style="width: 110px" clearable placeholder="对齐">
              <el-option label="居中" value="center" /><el-option label="左对齐" value="left" />
            </el-select>
          </div>
          <div class="spec-quote" style="margin-left: 80px">{{ quoteFor(lv + '.font') || quoteFor(lv + '.pattern') || '未识别，保持当前值' }}</div>
        </div>

        <h4 class="spec-group">正文</h4>
        <div class="spec-row">
          <label>正文</label>
          <el-select v-model="specDraft.body.font" size="small" style="width: 100px" placeholder="字体">
            <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
          </el-select>
          <el-select v-model="specDraft.body.fontSize" size="small" style="width: 120px" placeholder="字号">
            <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
          </el-select>
          <el-select v-model="specDraft.body.lineSpacingType" size="small" style="width: 120px">
            <el-option label="多倍行距" value="multiple" /><el-option label="固定值(磅)" value="exact" />
          </el-select>
          <el-select v-if="specDraft.body.lineSpacingType === 'multiple'" v-model="specDraft.body.lineSpacing" size="small" style="width: 90px">
            <el-option v-for="l in lineSpacings" :key="l" :label="l + ' 倍'" :value="l" />
          </el-select>
          <el-input-number v-else v-model="specDraft.body.lineSpacingExact" size="small" :min="10" :max="40" />
          <span class="spec-unit">缩进</span>
          <el-input-number v-model="specDraft.body.firstLineIndent" size="small" :min="0" :max="4" />
          <span class="spec-unit">字符</span>
        </div>
        <div class="spec-quote" style="margin-left: 80px">{{ quoteFor('body.font') || quoteFor('body.lineSpacing') || '未识别，保持当前值' }}</div>

        <h4 class="spec-group">图表题注</h4>
        <div class="spec-row">
          <label>图题注</label>
          <el-select v-model="specDraft.figure.font" size="small" style="width: 100px" placeholder="字体">
            <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
          </el-select>
          <el-select v-model="specDraft.figure.fontSize" size="small" style="width: 120px" placeholder="字号">
            <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
          </el-select>
        </div>
        <div class="spec-row">
          <label>表题注</label>
          <el-select v-model="specDraft.table.font" size="small" style="width: 100px" placeholder="字体">
            <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
          </el-select>
          <el-select v-model="specDraft.table.fontSize" size="small" style="width: 120px" placeholder="字号">
            <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
          </el-select>
        </div>
        <div class="spec-quote" style="margin-left: 80px">{{ quoteFor('figure.font') || quoteFor('table.font') || '未识别，保持当前值' }}</div>

        <h4 class="spec-group">参考文献</h4>
        <div class="spec-row">
          <label>条目</label>
          <el-select v-model="specDraft.refItemFont" size="small" style="width: 100px" placeholder="字体">
            <el-option v-for="f in fonts" :key="f" :label="f" :value="f" />
          </el-select>
          <el-select v-model="specDraft.refItemFontSize" size="small" style="width: 120px" placeholder="字号">
            <el-option v-for="s in sizes" :key="s.v" :label="s.label" :value="s.v" />
          </el-select>
        </div>
        <div class="spec-quote" style="margin-left: 80px">{{ quoteFor('reference.itemFont') || '未识别，保持当前值' }}</div>

        <div style="text-align: right; margin-top: 16px">
          <el-button @click="specVisible = false">取消</el-button>
          <el-button type="primary" @click="applySpec">套用到当前方案</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 左下角实时预览入口: 在任意配置 tab 调参数即时看效果 -->
    <button class="live-preview-toggle" :class="{ on: livePreview }" @click="toggleLivePreview">
      <el-icon><View /></el-icon>
      {{ livePreview ? '收起实时预览' : '实时预览' }}
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
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteNav from '../components/SiteNav.vue'
import PreviewPage from '../components/PreviewPage.vue'
import DocxCompare from '../components/DocxCompare.vue'
import { extractHeadings } from '../utils/docxHeadings'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { UploadFilled, Document, View, ArrowDown, QuestionFilled } from '@element-plus/icons-vue'
import { getTemplateDetail, saveAllConfig, extractSpec } from '../api/template'
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
  { key: 'preview', label: '快速试排' }
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

// ===== 校规一键预设: 内置常见学校规范, 一键填充全部配置 =====
function presetRules(bodyFont, bodySize, h1Size, h2Size, h3Size, captionFont) {
  return {
    heading1: { ruleType: 'heading1', font: '黑体', fontLatin: 'Times New Roman', fontSize: h1Size, bold: true, align: 'center', spaceBefore: 12, spaceAfter: 12 },
    heading2: { ruleType: 'heading2', font: '黑体', fontLatin: 'Times New Roman', fontSize: h2Size, align: 'left', spaceBefore: 6, spaceAfter: 6 },
    heading3: { ruleType: 'heading3', font: '黑体', fontLatin: 'Times New Roman', fontSize: h3Size, align: 'left', spaceBefore: 6, spaceAfter: 6 },
    body: { ruleType: 'body', font: bodyFont, fontLatin: 'Times New Roman', fontSize: bodySize, lineSpacingType: 'multiple', lineSpacing: 1.5, lineSpacingExact: 20, firstLineIndent: 2, align: 'justify' },
    figure: { ruleType: 'figure', font: captionFont, fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'below', numberingPattern: '图{chapter}-{no}', captionEnabled: true },
    table: { ruleType: 'table', font: captionFont, fontLatin: 'Times New Roman', fontSize: 10, captionPosition: 'above', numberingPattern: '表{chapter}-{no}', captionEnabled: true },
    tableText: { ruleType: 'tableText', font: '宋体', fontLatin: 'Times New Roman', fontSize: 10, bold: false, align: 'center', lineSpacing: 1.5 }
  }
}
function presetRef() {
  return { enabled: false, title: '参考文献', titleFont: '黑体', titleFontSize: 14, itemFont: '宋体', itemFontLatin: 'Times New Roman', itemFontSize: 10, removeDoi: true, maxAuthors: 3, renumber: true }
}
function presetToc() {
  return { lineSpacing: 1.5, leader: 'dot', toc1: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 14 }, toc2: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 }, toc3: { font: '宋体', fontLatin: 'Times New Roman', fontSize: 12 } }
}

const schoolPresets = {
  undergraduate: {
    label: '通用本科规范',
    desc: '正文宋体小四 1.5 倍行距，章标题黑体三号居中，节标题黑体四号',
    page: { paper: 'A4', margin: { top: 2.5, bottom: 2.5, left: 3, right: 2.5 }, header: { height: 1.5, text: '' }, footer: { pageNumber: 'center' } },
    headingPatterns: { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
    rules: presetRules('宋体', 12, 16, 14, 12, '宋体'),
    refConfig: presetRef(),
    tocConfig: presetToc()
  },
  engineering: {
    label: '理工科规范',
    desc: '正文仿宋小四，标题黑体体系，图表题注黑体五号，页眉标注校名',
    page: { paper: 'A4', margin: { top: 2.54, bottom: 2.54, left: 3.17, right: 3.17 }, header: { height: 1.5, text: '' }, footer: { pageNumber: 'center' } },
    headingPatterns: { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
    rules: presetRules('仿宋', 12, 16, 14, 12, '黑体'),
    refConfig: presetRef(),
    tocConfig: presetToc()
  },
  graduate: {
    label: '研究生规范(GB/T 7713)',
    desc: '正文宋体小四固定 20 磅行距，章标题黑体三号，页码底部居中',
    page: { paper: 'A4', margin: { top: 2.6, bottom: 2.6, left: 3, right: 2.6 }, header: { height: 1.5, text: '' }, footer: { pageNumber: 'center' } },
    headingPatterns: { heading1: '^第[一二三四五六七八九十百]+章', heading2: '^\\d+\\.\\d+', heading3: '^\\d+\\.\\d+\\.\\d+' },
    rules: presetRules('宋体', 12, 16, 14, 12, '宋体'),
    refConfig: presetRef(),
    tocConfig: presetToc()
  }
}
// 研究生规范用固定行距覆盖
schoolPresets.graduate.rules.body.lineSpacingType = 'exact'
schoolPresets.graduate.rules.body.lineSpacingExact = 20
schoolPresets.graduate.rules.body.lineSpacing = 1.5

function applySchoolPreset(key) {
  const p = schoolPresets[key]
  if (!p) return
  ElMessageBox.confirm(
    `将按「${p.label}」覆盖当前全部配置（页面/标题/正文/图表/参考文献/目录），是否继续？`,
    '套用校规预设',
    { type: 'warning', confirmButtonText: '套用', cancelButtonText: '取消' }
  ).then(() => {
    Object.assign(page, JSON.parse(JSON.stringify(p.page)))
    Object.assign(rules, JSON.parse(JSON.stringify(p.rules)))
    Object.assign(headingPatterns, JSON.parse(JSON.stringify(p.headingPatterns)))
    Object.assign(refConfig, JSON.parse(JSON.stringify(p.refConfig)))
    Object.assign(tocConfig, JSON.parse(JSON.stringify(p.tocConfig)))
    ElMessage.success('已套用预设：' + p.label)
  }).catch(() => {})
}

// ===== 校规文档导入向导: 抽取结果预填草稿, 逐项确认后套用 =====
const specVisible = ref(false)
const specLoading = ref(false)
const specResult = ref(null)
const specQuotes = ref({})
const specDraft = reactive({
  paper: 'A4',
  margin: { top: 2.5, bottom: 2.5, left: 3, right: 2.5 },
  heading1: { pattern: '', font: '', fontSize: 16, align: '' },
  heading2: { pattern: '', font: '', fontSize: 14, align: '' },
  heading3: { pattern: '', font: '', fontSize: 12, align: '' },
  body: { font: '', fontSize: 12, lineSpacingType: 'multiple', lineSpacing: 1.5, lineSpacingExact: 20, firstLineIndent: 2 },
  figure: { font: '', fontSize: 10 },
  table: { font: '', fontSize: 10 },
  refItemFont: '', refItemFontSize: 10
})

function lvName(lv) {
  return { heading1: '一级标题', heading2: '二级标题', heading3: '三级标题' }[lv]
}

function quoteFor(field) {
  const q = specQuotes.value[field]
  return q ? '依据：' + (q.length > 42 ? q.slice(0, 42) + '…' : q) : ''
}

function openSpecWizard() {
  specResult.value = null
  specQuotes.value = {}
  // 草稿预填当前配置: 未识别的字段套用时保持原值
  specDraft.paper = page.paper || 'A4'
  specDraft.margin = { ...page.margin }
  specDraft.heading1 = { pattern: headingPatterns.heading1, font: rules.heading1.font, fontSize: rules.heading1.fontSize, align: rules.heading1.align || '' }
  specDraft.heading2 = { pattern: headingPatterns.heading2, font: rules.heading2.font, fontSize: rules.heading2.fontSize, align: rules.heading2.align || '' }
  specDraft.heading3 = { pattern: headingPatterns.heading3, font: rules.heading3.font, fontSize: rules.heading3.fontSize, align: rules.heading3.align || '' }
  specDraft.body = {
    font: rules.body.font, fontSize: rules.body.fontSize,
    lineSpacingType: rules.body.lineSpacingType || 'multiple',
    lineSpacing: rules.body.lineSpacing || 1.5,
    lineSpacingExact: rules.body.lineSpacingExact || 20,
    firstLineIndent: rules.body.firstLineIndent || 0
  }
  specDraft.figure = { font: rules.figure.font, fontSize: rules.figure.fontSize }
  specDraft.table = { font: rules.table.font, fontSize: rules.table.fontSize }
  specDraft.refItemFont = refConfig.itemFont
  specDraft.refItemFontSize = refConfig.itemFontSize
  specVisible.value = true
}

async function onSpecFile(f) {
  const file = f && f.raw ? f.raw : null
  if (!file) return
  specLoading.value = true
  try {
    const vo = await extractSpec(file)
    specResult.value = vo
    const q = {}
    ;(vo.evidence || []).forEach(ev => { if (!q[ev.field]) q[ev.field] = ev.quote })
    specQuotes.value = q
    const pc = vo.pageConfig || {}
    if (pc.paper) specDraft.paper = pc.paper
    // 后端边距为嵌套结构 pageConfig.margin.{top,bottom,left,right}
    const mg = pc.margin || {}
    ;['top', 'bottom', 'left', 'right'].forEach(k => { if (mg[k] != null) specDraft.margin[k] = mg[k] })
    const hp = vo.headingPatterns || {}
    ;['heading1', 'heading2', 'heading3'].forEach(k => { if (hp[k]) specDraft[k].pattern = hp[k] })
    const rulesVo = vo.rules || {}
    Object.keys(rulesVo).forEach(k => {
      const r = rulesVo[k]
      const d = specDraft[k]
      if (!d) return
      if (r.font) d.font = r.font
      if (r.fontSize != null) d.fontSize = r.fontSize
      if (r.align) d.align = r.align
    })
    const bv = rulesVo.body
    if (bv) {
      if (bv.lineSpacingType) specDraft.body.lineSpacingType = bv.lineSpacingType
      if (bv.lineSpacing != null) specDraft.body.lineSpacing = bv.lineSpacing
      if (bv.lineSpacingExact != null) specDraft.body.lineSpacingExact = bv.lineSpacingExact
      if (bv.firstLineIndent != null) specDraft.body.firstLineIndent = bv.firstLineIndent
    }
    const rc = vo.refConfig || {}
    if (rc.itemFont) specDraft.refItemFont = rc.itemFont
    if (rc.itemFontSize != null) specDraft.refItemFontSize = rc.itemFontSize
    if (!Object.keys(q).length) {
      ElMessage.warning('未能从文档中识别出格式表述，可手动调整后套用')
    } else {
      ElMessage.success(`识别到 ${Object.keys(q).length} 项配置，请核对后套用`)
    }
  } catch (e) {
    // 具体原因由 api 拦截器提示
  }
  specLoading.value = false
}

function applySpec() {
  const patterns = [specDraft.heading1.pattern, specDraft.heading2.pattern, specDraft.heading3.pattern]
  for (const p of patterns) {
    try {
      if (!p || !p.trim()) throw new Error('empty')
      // eslint-disable-next-line no-new
      new RegExp(p)
    } catch (e) {
      ElMessage.error('标题正则不合法：' + p)
      return
    }
  }
  page.paper = specDraft.paper
  Object.assign(page.margin, specDraft.margin)
  headingPatterns.heading1 = specDraft.heading1.pattern
  headingPatterns.heading2 = specDraft.heading2.pattern
  headingPatterns.heading3 = specDraft.heading3.pattern
  const applyRule = (target, src) => {
    if (src.font) target.font = src.font
    if (src.fontSize != null) target.fontSize = src.fontSize
    if (src.align) target.align = src.align
  }
  applyRule(rules.heading1, specDraft.heading1)
  applyRule(rules.heading2, specDraft.heading2)
  applyRule(rules.heading3, specDraft.heading3)
  applyRule(rules.body, specDraft.body)
  rules.body.lineSpacingType = specDraft.body.lineSpacingType
  rules.body.lineSpacing = specDraft.body.lineSpacing
  rules.body.lineSpacingExact = specDraft.body.lineSpacingExact
  rules.body.firstLineIndent = specDraft.body.firstLineIndent
  applyRule(rules.figure, specDraft.figure)
  applyRule(rules.table, specDraft.table)
  if (specDraft.refItemFont) refConfig.itemFont = specDraft.refItemFont
  if (specDraft.refItemFontSize != null) refConfig.itemFontSize = specDraft.refItemFontSize
  specVisible.value = false
  ElMessage.success('校规配置已套用，按 Ctrl+S 保存')
}

// ===== 防呆: 标题正则合法性实时校验 + 常用模式插入 =====
const regexValid = computed(() => {
  const test = p => {
    try {
      if (!p || !p.trim()) return false
      // eslint-disable-next-line no-new
      new RegExp(p)
      return true
    } catch (e) {
      return false
    }
  }
  return {
    heading1: test(headingPatterns.heading1),
    heading2: test(headingPatterns.heading2),
    heading3: test(headingPatterns.heading3)
  }
})

const commonPatterns = {
  heading1: ['^第[一二三四五六七八九十百]+章', '^第\\d+章', '^\\d+\\s*\\S+', '^[一二三四五六七八九十]+、'],
  heading2: ['^\\d+\\.\\d+', '^[（(][一二三四五六七八九十]+[）)]', '^\\d+\\.\\d+\\s*\\S+'],
  heading3: ['^\\d+\\.\\d+\\.\\d+', '^[（(]\\d+[）)]']
}

function insertPattern(key, pattern) {
  headingPatterns[key] = pattern
}

// ===== 快速试排: 上传文档截取前几段, 用真实排版引擎同步出结果 =====
const quickFile = ref(null)
const quickParagraphs = ref(150)
const quickLoading = ref(false)
const quickData = ref([])
const quickHeadings = ref([])

function onQuickUpload(uploadFile) {
  // auto-upload=false: 只取用户选中的原始文件, 每次选择覆盖上一次
  quickFile.value = uploadFile && uploadFile.raw ? uploadFile.raw : null
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
// Ctrl/Cmd + S 快捷保存: 覆盖浏览器默认"保存网页"
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) {
    e.preventDefault()
    if (!saving.value) saveAll()
  }
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
// 组件复用时按模板 id 重新加载(解决退出后再次进入显示旧配置/默认配置)
watch(() => route.params.id, () => {
  loadConfig()
})

async function saveAll() {
  const currentId = Number(route.params.id)
  // 防呆: 标题正则非法时直接拦截, 不等排版失败
  if (!regexValid.value.heading1 || !regexValid.value.heading2 || !regexValid.value.heading3) {
    ElMessage.error('标题识别规则不是合法的正则表达式（已红框标注），请修正后再保存')
    return false
  }
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
  color: #24312A;
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
  color: #5C6B60;
  border-left: 3px solid transparent;
}
.menu-item:hover {
  color: #2F5D46;
  background: #F6F4EE;
}
.menu-item.active {
  color: #2F5D46;
  background: #EAEFED;
  border-left-color: #2F5D46;
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
  color: #24312A;
  margin: 18px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #E3E0D5;
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
  color: #8B968D;
  font-size: 12px;
  line-height: 1.6;
  margin-top: 4px;
}
.tip-inline {
  color: #8B968D;
  font-size: 12px;
  margin-left: 8px;
}
/* 快速试排 */
/* 校规一键预设 */
.school-presets {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 8px 0 4px;
}
.preset-card {
  width: 240px;
  padding: 12px 14px;
  border: 1px solid #E3E0D5;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.preset-card:hover {
  border-color: #2F5D46;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.15);
}
.preset-name {
  font-size: 14px;
  font-weight: 600;
  color: #24312A;
  margin-bottom: 4px;
}
.preset-desc {
  font-size: 12px;
  color: #8B968D;
  line-height: 1.6;
}
/* 正则防呆 */
.regex-invalid .el-input__inner {
  color: #f56c6c;
}
.regex-invalid .el-input__wrapper {
  box-shadow: 0 0 0 1px #f56c6c inset;
}
.regex-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}
.pattern-more {
  font-size: 12px;
  color: #8B968D;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
}
.q-mark {
  color: #B3BCB2;
  font-size: 13px;
  vertical-align: -2px;
  cursor: help;
}
.q-mark:hover {
  color: #2F5D46;
}
/* 校规导入向导 */
.spec-card {
  border-style: dashed;
}
.spec-group {
  margin: 14px 0 8px;
  font-size: 14px;
  color: #303133;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
.spec-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.spec-row > label {
  display: inline-block;
  width: 72px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.spec-unit {
  font-size: 12px;
  color: #909399;
}
.spec-quote {
  font-size: 12px;
  color: #b0b3b8;
  line-height: 1.6;
  margin: 2px 0 6px;
}
.quick-card {
  margin: 12px 0 4px;
  padding: 18px 18px 14px;
  border: 1px solid #E3E0D5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.quick-card :deep(.el-upload-dragger) {
  padding: 24px 0 20px;
}
.quick-card :deep(.el-upload__tip) {
  color: #8B968D;
  font-size: 12px;
}
.quick-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #E3E0D5;
}
.quick-actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.quick-range-label {
  font-size: 12px;
  color: #8B968D;
}
.quick-filename {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #24312A;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.quick-filename.placeholder {
  color: #B3BCB2;
}
.quick-result {
  margin-top: 18px;
  border: 1px solid #E3E0D5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.quick-result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #E3E0D5;
  background: #F6F4EE;
}
.quick-result-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #24312A;
  font-weight: 600;
}
.dot-ok {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  display: inline-block;
}
.quick-result-body {
  max-height: 70vh;
  overflow: auto;
  padding: 4px;
}
/* 左下角实时预览入口 */
.live-preview-toggle {
  position: fixed;
  left: 20px;
  bottom: 24px;
  z-index: 1500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 22px;
  border: none;
  border-radius: 24px;
  background: #2F5D46;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.45);
  transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
}
.live-preview-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.55);
}
.live-preview-toggle.on {
  background: #67c23a;
  box-shadow: 0 4px 14px rgba(103, 194, 58, 0.45);
}
.live-preview-dock {
  position: fixed;
  right: 0;
  top: 70px;
  bottom: 0;
  width: 480px;
  z-index: 1400;
  background: #F6F4EE;
  border-left: 1px solid #D6D1C5;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}
.live-preview-tip {
  padding: 8px 14px;
  font-size: 12px;
  color: #8B968D;
  border-bottom: 1px solid #E3E0D5;
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
