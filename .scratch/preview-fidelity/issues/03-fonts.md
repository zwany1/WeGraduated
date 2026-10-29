# 03 · 字体按文档渲染 + 缺字形兜底 + 替换提示

## What to build

删掉 `normalizeVisibility()` 里对所有 `p, span` 的 `font-family` 强制覆盖。改为：探测文档声明的字体在本机是否可用；不可用时按族补兜底（宋/仿宋/楷 → `Noto Serif SC`，黑体 → `Noto Sans SC`），保证衬线/无衬线观感差异；被替换的字体清单显示在工具栏（如「字体已替换：宋体 → Noto Serif SC」）。保留字号 0 / 白字 / `display:none` 三种兜底。

## Blocked by

02（页面视图先把版面立起来，字体才好在真实纸面上比对）。

## 验收标准

- 给定 t320，当预览渲染完成，那么正文 span 的 `font-family` 不含 `Arial`，且要么是 `宋体`，要么是 `Noto Serif SC`；标题 span 含 `黑体` 或 `Noto Sans SC`（断言 3 转绿）。
- 给定同一屏，那么正文（衬线）与一级标题（黑体/无衬线）在肉眼上可区分。
- 给定本机确实缺某字体，那么工具栏出现替换提示，且该段文字仍完整显示（不出现空白段）。
- 给定本机字体齐全，那么替换提示不出现，且不产生任何 `font-family` 改写。

## 改动面

`DocxCompare.vue`（后处理 + 工具栏提示）；字体 `@import` 落在组件或 `main.css`。
