# 04 · 对齐不再强制左对齐（题注/公式居中、正文两端对齐）

## What to build

删除 `:deep(.docx p){ text-align: start !important }`。改为渲染后按段检测：只有"被拆成多个 span 且 justify 产生 >0.5em 可视空洞"的段落降级为 `start` 并打 `data-justify-fallback="1"`，其余保持文档原本的 `center / justify / right`。

## Blocked by

03（先确认字体不再是导致散架的变量，避免把"字体替换引起的测量偏差"误判成 justify 问题）。

## 验收标准

- 给定 t320，当预览渲染完成，那么图题注段落的 `text-align` 计算值为 `center`（断言 4 转绿）。
- 给定正文段落，那么 `text-align` 为 `justify`，或该段带 `data-justify-fallback="1"`。
- 给定居中的公式段（t322 注入公式），那么公式在页面上水平居中，不贴左。
- 给定"散架"段落（若仍出现），那么视觉上不再出现跨 span 的大空洞。

## 改动面

`DocxCompare.vue`（删 CSS 强制 + 新增按段检测）。
