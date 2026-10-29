# 05 · 标题字号/加粗 —— 结论：产品无需改动，错的是断言口径

## 结论

票 01 写的断言取样到了**段首的空 wrapper span**（`<span style="...">` 里没有文字，字号来自样式默认值 18.67px/400），
据此判定"标题字号不跟版式"是**误判**。取承载文字的叶子 span 后实测：

- t326（版式 C 一级标题 15pt 加粗）：`font-size: 20px; font-weight: 700` ✅
- t320（版式 A 一级标题 14pt 不加粗）：`18.67px / 400` ✅

docx-preview 本身把 run 的直接格式渲染正确，**不需要后处理纠偏**（原计划的 `renderAsync` 模型对位纠偏已取消）。

## 变更

- `e2e/scenarios/preview-fidelity.mjs`：`leaf(el)` 取最后一个有文字的 span，用于 body/heading/caption 的字号断言。
- 产品代码：无。

## 教训

断言失败先确认"测量点对不对"，再怀疑产品。段落里的空 span 是 docx-preview 的常态（编号/制表位载体），
`p.querySelector('span')` 拿到的不一定是文字载体。
