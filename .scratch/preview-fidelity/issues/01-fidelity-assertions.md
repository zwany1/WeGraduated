# 01 · 预览保真度断言先行（红）

## What to build

新增 `e2e/scenarios/preview-fidelity.mjs`：用现有 CDP 驱动打开排版任务页，对 `t320`（A4/校规）与 `t326`（A4/五号单倍）两份产物各打开一次「排版结果预览」，断言 spec 第 5 节的 6 条指标，失败退出码 3。

脚本自身要能在当前代码上跑出「6 条全红」的结果，并把每条的实际值打印出来（宽度/字号/对齐/分页数/font-family），作为修复前的基线快照。

## Blocked by

无（第一票）。

## 验收标准

- 给定当前未修改的 `DocxCompare.vue`，当执行 `node e2e/scenarios/preview-fidelity.mjs`，那么输出 6 条 FAIL、退出码 3，且每条都带实际值。
- 给定脚本输出，当我读日志就能知道"纸宽 900 / 字号 18.67 vs 20 / 对齐 start / 分页 3"这些现状数字。

## 改动面

`e2e/scenarios/preview-fidelity.mjs`（新增）；不改产品代码。
