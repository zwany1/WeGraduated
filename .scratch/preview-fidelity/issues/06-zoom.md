# 06 · 缩放改 CSS zoom，消除 200% 裁切

## What to build

预览缩放从 `transform: scale()` 改为 `zoom`（变量 `--doc-scale`），保持 0.5–2 与工具栏按钮不变；`@supports not (zoom: 1)` 时回退到原 transform 实现。放大后正文换行随宽度重排，不再左右被切。

## Blocked by

02（页面视图的纸块尺寸稳定后，才能断言"放大不裁切"）。

## 验收标准

- 给定页面视图，当我连点 + 到 200%，那么 `.docx-body` 的 `scrollWidth <= .docx-wrap clientWidth + 2`（断言 6 转绿）。
- 给定 200%，那么文字按新宽度重新换行，左右两侧没有半个字被切掉。
- 给定缩放到 150% 再切回 100%，那么纸宽回到基准值、无残留位移。

## 改动面

`DocxCompare.vue`（bodyStyle 计算 + 少量样式）。
