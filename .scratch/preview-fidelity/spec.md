# 浏览器预览保真度（preview-fidelity）· Spec

## 1. Problem Statement

`DocxCompare.vue` 是排版结果的唯一验收界面（预览弹窗、对比视图左右栏、配置页试排共用）。它现在的渲染参数与后处理把「字体、对齐、纸张、分页」四项全部抹平：

- `ignoreWidth:true / ignoreHeight:true / breakPages:false` → 任何纸张都渲染成 900px 宽的连续流，页边距、分页、页眉页脚都不可见；
- `normalizeVisibility()` 对所有 `p, span` 用 `!important` 写 `Arial, "Microsoft YaHei"`，宋体/仿宋/黑体的差异消失；
- `:deep(.docx p){ text-align:start !important }` 把题注居中、正文两端对齐、公式居中全改成左对齐；
- 标题字号/加粗取样式默认值（实测 14pt/400），不跟模板的 15/16pt 走。

后果是：一次真实测试里，产物目录被拉成 48 页、5 张图被吞掉，预览里都"看起来正常"。用户在这个界面上做出的验收判断是不可信的。

## 2. Solution

把预览拆成两种模式，共用一套渲染后处理：

- **页面视图（默认）**：按文档 `pgSz/pgMar` 渲染真实纸张宽度、页边距、页眉页脚、分页；每个分页块底部标「第 N 页 / 共 M 页」。因为 docx-preview 只按显式分页符切块、不按行高重排，页面块用 `min-height: 页高` 而非固定高度，内容更长时向下生长（宁可露出一块"超长页"，也不裁切文字），并在 UI 标注「分页为近似」。
- **连续视图**：保留现在的整篇滚动（仍有真实纸宽），用于长文快速浏览；由工具栏切换，选择记忆在 `localStorage`。
- **字体**：删除无差别覆盖。仅当"文档声明的字体在本机不可用"时，按族补一条兜底（宋/仿宋/楷 → Noto Serif SC，黑体 → Noto Sans SC），保留衬线/无衬线观感差异，并在工具栏显示"字体已替换"。
- **对齐**：删除 `text-align:start !important`。只有当段落被 docx-preview 拆成多个 span、且测量到 justify 产生 >0.5em 的字间空洞时，才对**那一段**降级为 `start`。
- **缩放**：从 `transform: scale` 改为 CSS `zoom`，消除 200% 时左右被裁切。

## 3. User Stories

1. 作为学生，我打开排版结果预览，看到的纸张宽度和我选的 A4/B5 一致，能判断页边距是否符合校规。
2. 作为学生，我能看到"每章另起一页"，能数出成品的页数，不必下载后用 Word 打开才发现目录有 48 页。
3. 作为学生，我看到正文是宋体、标题是黑体、题注居中——预览与下载的 docx 一致。
4. 作为学生，我切到连续视图快速翻完整篇，切回页面视图继续逐页检查。
5. 作为学生，我放大到 200% 看字号和缩进，画面不被裁切。
6. 作为学生，当本机缺字体、预览用了替代字体时，界面明确告诉我"哪些字体被替换了"。

## 4. Implementation Decisions

**渲染参数（按模式）**

| 选项 | 页面视图 | 连续视图 |
| --- | --- | --- |
| `inWrapper` | true | true |
| `ignoreWidth` | **false** | **false** |
| `ignoreHeight` | false | true |
| `breakPages` | **true** | false |
| `renderHeaders` / `renderFooters` | true | false |
| `experimental`（OMML） | true | true |

**纸张与分页**
- 纸宽/页高由 docx-preview 按 `pgSz` 计算；组件不再写死 `max-width: 900px`，改为 `max-width: none` + 容器居中。
- 页面块样式：`min-height: 页高; height: auto; overflow: visible;`（去掉现有的 `display:block;height:auto` 硬覆盖，改为只在连续视图生效）。
- 页码角标由组件在每块 `section` 后插入 `<div class="sheet-no">第 N 页 / 共 M 页</div>`，`M` 为块数；不写进 docx。
- 分页精度提示固定显示在工具栏：「分页按文档分页符近似，与 Word 可能有 1 页内偏差」。

**字体**
- 可用性探测：canvas `measureText` 对比 `"<family>"` 与一个必然缺失的族，宽度相同即视为不可用（CJK 样本串 `汉字测量样本`）。
- 兜底映射：宋体/仿宋/楷体/`SimSun/FangSong/KaiTi` → `"Noto Serif SC", serif`；黑体/`SimHei` → `"Noto Sans SC", sans-serif`；其余保持原族 + `serif` 兜底。
- 字体文件：`@fontsource/noto-serif-sc` / `noto-sans-sc` 已在 dependencies，按需 `@import` 对应 400/700 字重。
- 保留的"隐形文字"兜底（只这三种情况）：`font-size<=0` → 12pt；颜色为白/透明 → `#000`；`display:none` → `inline`。
- 替换清单通过一个响应式数组返回给工具栏（如 `宋体 → Noto Serif SC`），只在非空时显示。

**对齐**
- 删除 `:deep(.docx p){text-align:start!important}`。
- 渲染后逐段检测：段落内只有 1 个文本 span 或 `text-align` 非 `justify` → 不动；`justify` 且存在相邻 span 之间的可视空洞 > 0.5em → 该段设 `text-align: start`，并打 `data-justify-fallback="1"` 便于排查。

**标题字号/加粗**
- `renderAsync` 返回的 `WordDocument`（`d.ts` 里是 `any`，运行时探测 `sections/paragraphs`）与 DOM 段落按顺序一一对应。
- 纠偏只做两件事：把模型里的 `fontSize`（半磅→px）与 `bold` 写到对应 `p` 的 `style` 上，仅当与计算值不一致时写；写不上（模型结构不符）就跳过并计数，不抛错。
- 这条是"兜底纠偏"，不是主要渲染路径——docx-preview 正常时它应当是 0 次写入。

**缩放**
- `.docx-body { zoom: var(--doc-scale, 1) }`；`scale` 仍存 0.5–2，工具栏按钮不变。
- 保留 `transform` 分支作为 `@supports not (zoom: 1)` 的降级。

**兼容/降级**
- 连续视图对老文档行为与今天一致（除纸宽变真实）。
- 任意后处理异常都只记录到 `window.__previewDiag`，不得让预览白屏（现有 `__docxErr` 保留）。
- 组件 props 不变，4 处调用点无需改动；`TemplateConfig` 的试排预览自动继承页面视图。

## 5. Testing Decisions

**唯一接缝**：`e2e/scenarios/preview-fidelity.mjs`（复用 `e2e/cdp.mjs`，`process.exit(3)` 即门禁失败）。

固定样本：`tmp_v4/out/t320.docx`（校规 A4，14pt 标题 / 宋体 12pt / 题注居中）与 `tmp_v4/out/t326.docx`（A4 五号单倍，15pt 标题）。样本上传一次，断言跑在真实预览弹窗上。

断言（当前全红，改完逐条转绿）：

1. 页面视图：`section` 宽度 ≈ 文档 `pgSz` 换算值（A4 794±2px，B5 665±2px），且两套版式**不相等**。
2. 分页：`section` 数量 > 1，且与 XML 里 `pageBreakBefore + w:br page` 的分页点数同阶（±2）。
3. 字体：正文 span 的 `font-family` 不含 `Arial`；含 `宋体`/`Noto Serif SC` 之一；标题含 `黑体`/`Noto Sans SC`。
4. 对齐：题注 `text-align` 计算值为 `center`；正文段落为 `justify`（或该段带 `data-justify-fallback="1"`）。
5. 标题字号：t320 一级标题 ≈ 18.67px（14pt），t326 ≈ 20px（15pt），权重 700（t326）。
6. 缩放：点击 + 到 200% 后，`.docx-body` 的 `scrollWidth <= wrap.clientWidth + 2`。

不写单测：前端无 vitest/jest，且这些属性只在真实渲染后才存在，DOM 断言是最高层接缝。

## 6. Out of Scope

- 产物侧缺陷：P1（参考文献区吞段丢图）、P2（目录 48 页分页）、P3（首行缩进 0 失效）、P5（三线表未应用）、P9（页眉丢失）、P10（报告计数）——另开一轮。
- 差异对比的高亮/定位逻辑（只继承新的渲染参数，不改算法）。
- 导出 PDF / 打印样式、移动端布局。
- 真实分页排版（按行高把文字重新分配到页）——本轮明确接受"分页符近似"。

## 7. Further Notes

- 风险 1：docx-preview 页面视图下，超长内容可能溢出页块。已用 `min-height` 兜底，但"一页里塞 1.5 页内容"仍可能出现；若实测频繁，下一轮考虑按 `页高` 做 CSS `columns` 或分页测量后再切。
- 风险 2：`zoom` 在 Firefox <126 不支持，走 `@supports` 回退到 transform（裁切问题只在老 Firefox 重现）。
- 风险 3：字体探测用 canvas 宽度启发式，少数等宽字体可能误判；误判的后果只是"多补一条兜底族"，不影响可读性。
- 已在 `state.md` 记录：本次不碰后端，改动集中在 `DocxCompare.vue` + 新增 1 个 e2e 场景。
