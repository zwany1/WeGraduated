# 07 · 「段前分页」在预览中不生效（章节不另起一页）

## 现状（2026-10-29 复查修正）

引擎把分页写成**段落直接格式** `<w:pageBreakBefore/>`；docx-preview 0.4.0 的
`splitBySection()` 只认 `findStyle(elem.styleName)?.paragraphProps?.pageBreakBefore`
（`dist/docx-preview.js:3206`），段落级属性被丢弃。

**修正上一版的过度结论**：并不是"完全没有分页"。t320 产物里还有 11 个显式
`<w:br w:type="page"/>` + 3 个 `sectPr`，它们切出了 14 个分页块，其中 7 个恰好落在章标题上
（绪论 / 相关理论基础与技术 / 系统总体设计 / 系统硬件与仿真环境设计 / 系统控制程序设计 /
人机交互界面设计与系统集成 / 参考文献）。**9 个一级标题里漏了 2 个**
（系统测试与结果分析、附录），所以"每章另起一页"这条校规在预览里不可验收。

## 影响

- 学生看到的页数与成品不一致；"每章另起一页"无法在预览里验收。
- 目录 78 段误带 `pageBreakBefore`（产物缺陷 P2）在预览里也看不出来——同一件事。

## 三条路径（评估后取第 3 条，已落地）

1. **引擎侧**：`HeadingFormatter` 把 `pageBreakBefore` 同时写进 heading 样式定义。
   ~~推荐~~ → **否决**：`SectionFormatter:62` 的分页开关是**逐规则**的，
   `TocFormatter:156/412` 又显式把目录条目的 `pageBreakBefore` 置 0；写进样式会覆盖逐段开关，
   把"目录不另起页"这类修法整体废掉。
2. **预览侧 · 改 docx 二进制**：解包 → 改写段落 → 重打包。需要手写 ZIP（deflate-raw + CRC + 目录偏移），
   ~120 行且易错。后来发现不需要动字节。
3. **预览侧 · 改解析树（采用）**：`parseAsync()` 拿到的 `WordDocument` 上，
   `documentPart.body.children` 就是完整的渲染树，且**段落级 `paragraphProps.pageBreakBefore` 完好**。
   渲染前给这些段落 `children.unshift({type:'run',children:[{type:'break',break:'page'}]})`：
   - `splitBySection` 的 `isPageBreakElement` 认 `type==='break' && break==='page'` → 正常切页；
   - `renderBreak()` 对非 `textWrapping` 的 break **返回 `null`**（`docx-preview.js:3630`），
     所以不会多出任何 `<br>` 或空行。
   最后用 `renderDocument()` + 6 行手写挂载代替 `renderAsync()`（两者是同一个内部流程，
   `renderAsync = parseAsync + renderDocument + append`）。

   注入点只在 **页面视图** 生效；连续视图不注入，保持整篇流畅滚动。

## 验收标准

- 给定 t320 产物，当在页面视图打开预览，那么**每个一级标题都是其所在页的第一个有文字的段落**。
- 给定同一份产物，那么分页块数 ≥ 15（当前 14）。

## 已知后果（2026-10-29 实测更新）

- A4 样本页数 **14 → 16**：多出的 2 页正是原先"不另起页"的「系统测试与结果分析」和「附录」，
  与 XML 层结论一致（产物里 `pageBreakBefore` 开着的恰好 11 个：目录标题 + 9 个一级标题）。
- **复核纠正**：之前记录的"目录 78 段误带段前分页（P2）"是统计口径错误——那 78 个全是
  `w:val="0"`（显式关闭），`TocFormatter` 本来就逐条关掉了。测试报告的 P2 已撤回。
- **遗留（另立待办）**：「摘要→Abstract」「目录→绪论」之间多出全白页，是 docx-preview 对
  `sectPr` 节边界的 `pageSize` 有无不一致时多切一次页（`groupByPageBreaks.isPageBreakSection`
  在 props 缺失时也判换页），修复前就存在，与本票无关。
- 工具栏 `title` 的免责说明已改写：只保留"预览不做纵向排版回流（超页内容不接到下一页）"这一真实限制。
