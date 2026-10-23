# Spec：自动排版输入校验与结果反馈

## Problem Statement

浏览器测试暴露三个问题，本质是同一件事：**系统对「不合格的输入」既不拦也不说清**。

1. 空文档排版后任务状态是 `SUCCESS`，报告显示 6 个 0，结论却是「结构识别良好」——用户以为排好了。
2. 批量上传时其中一篇是 0 字节，整批中断；已上传成功的文件留在服务器上但永远不会有任务，成为孤儿数据。
3. 文本文件改名 `.docx` 能通过上传，直到排版阶段才失败，报错是「排版失败：排版失败：处理文档时发生错误（ZipException: Unexpected record signature: 0xe499bfe8）」。

## Solution

三处收敛，都遵循「谁有数据谁判断」：

- **上传阶段**：`upload` 校验 ZIP 魔数，坏文件当场拒绝，不落存储、不入库。
- **报告阶段**：`FormatReport` 新增结构评级（`OK` / `WARN` / `EMPTY`），由后端计算，前端只渲染。
- **提交阶段**：前端逐篇容错，成功的照常建任务，失败的留在待提交区并汇总提示。

## User Stories

- 作为学生，我把一篇空白文档传上去时，希望**立刻**看到「未检测到任何正文内容」，而不是等我下载了才发现是白纸。
- 作为学生，我批量传 5 篇其中 1 篇坏了，希望其余 4 篇照常排版，不用全部重来。
- 作为学生，文档损坏时我希望看到「文件已损坏或不是有效的 .docx，请用 Word 重新另存」，而不是 `ZipException`。
- 作为老师，我在报告里看到「未识别到章节标题」时，能直接知道该去检查标题样式或编号。

## Implementation Decisions

> 下面只记决策，不写文件路径（会过期）。判定归属是本节重点。

### D1 结构评级由后端计算，前端零判断

`FormatReport` 增加两个字段：`structureLevel`（`OK` / `WARN` / `EMPTY`）与 `structureMessage`（用户可读文案）。

判定收敛到一个方法 `FormatReport.evaluateStructure()`，规则：

| 条件 | level | 文案要点 |
| --- | --- | --- |
| `bodyParagraphs == 0 && chapterCount == 0` | `EMPTY` | 未检测到任何正文内容，请确认文档是否正确 |
| `chapterCount == 0 && bodyParagraphs > 0` | `WARN` | 未识别到章节标题，提示检查标题样式或 1/1.1 编号 |
| 其他 | `OK` | 无文案 |

**唯一调用点**：`PaperService` 在报告序列化落库之前调用一次。前端不重复实现这套条件。

### D2 兼容旧报告（关键降级规则）

历史任务的 `report` JSON 里没有新字段，反序列化后为 `null`。

- `structureLevel == null` → 前端按**旧行为**渲染（只看 `suspects` 是否为空），不显示任何新告警。
- 这样存量任务不会因为升级而集体变成「告警」，也不会出现「结构识别良好」以外的新结论。

同一条降级逻辑同时覆盖「新前端 + 旧后端」：字段缺失即回退。

### D3 前端只做展示映射

报告弹窗顶部按 `structureLevel` 渲染提示条：

- `EMPTY` → 危险级提示，且**不再**显示「结构识别良好」
- `WARN` → 警告级提示，其余（suspects 引导）照常展示
- `OK` / `null` → 保持现有渲染

文案全部来自后端 `structureMessage`，前端不硬编码业务措辞（除兜底文案）。

### D4 批量提交改为逐篇容错

`submit()` 由「串行 await，首个失败即跳出」改为：

1. 逐篇 `try/catch`，成功的收集 `fileIds`，失败的收集 `{name, reason}`；
2. `fileIds` 非空时照常 `startFormatBatch`；
3. 提交后：成功的文件移出待提交区，**失败的留在列表**供用户处理；
4. 结束给两条独立提示：成功数、失败清单（文件名 + 原因）。

若 `fileIds` 为空（全失败），不调用建任务接口，只报失败清单。

### D5 上传入口校验 ZIP 魔数

`upload` 在「非空 / 扩展名 / 大小」之后，读文件前 4 字节，必须等于 `PK\x03\x04`。

- 不匹配 → 抛业务异常「文件已损坏或不是有效的 .docx，请用 Word 重新另存后再上传」
- 校验发生在 `storageService.store` 与 `paperFileMapper.insert` **之前**，因此不会留下任何存储文件或数据库记录
- 读取失败（IO 异常）单独给「文件读取失败，请重新上传」

### D6 失败文案归一

`friendlyFormatError` 两处修正：

1. 入口先剥离已有的 `排版失败：` / `排版失败:` 前缀，消除重复；
2. 关键词表补充 `ZipException`、`Unexpected record signature`、`not a zip`、`invalid block`、`Too large` 等 ZIP/解析类错误，统一归入「文档无法解析」的友好文案。

技术细节（类名、十六进制）只进日志，不进 `error_msg`。

## Testing Decisions

测试接缝按「取最高层、越少越好」原则定为 3 个：

| 接缝 | 层级 | 观察什么 |
| --- | --- | --- |
| S1 `PaperService.upload` | 后端 service（最高层，绕过 Controller 的验证码/鉴权噪声） | 传入伪 docx 是否抛业务异常，且 `t_paper_file` 无新增记录 |
| S2 `FormatReport.evaluateStructure` | 纯模型单测（不起 Spring） | 三种计数组合产出的 level/message |
| S3 浏览器 UI | agent-browser 回归 | 空文档报告是否出现告警；批量含坏文件时好文件是否仍提交 |

S1 用 `MockMultipartFile` 构造，不启 Web 容器；S2 不读数据库；S3 复用上一轮已有的浏览器脚本骨架。

已知代价：S3 依赖服务与浏览器进程，属于手工/半自动回归，不纳入 `mvn test`。

## Out of Scope

- 不改排版引擎的格式逻辑、不改模板配置（P1/P2/P3 都是输入侧与反馈侧问题）
- 不做「部分失败自动重试」或「只重传失败文件」的按钮（汇总提示已够用）
- 不清理存量孤儿文件（历史数据，另行处理）
- 不放宽/收紧上传大小限制
- 不做前端单元测试框架的引入

## Further Notes

- **反直觉点**：空文档的排版任务状态是 `SUCCESS`（引擎不报错）。任何试图用「任务是否失败」来提示用户的做法都是错的。
- **反直觉点**：业务失败时 HTTP 仍是 200，错误码在 `body.code`。前端判断上传成败必须看 `code`。
- P3 修复后，0 字节与伪 docx 都在 `upload` 阶段被拒，P2 的「孤儿文件」在正常路径下不再产生；仅当 `startFormatBatch` 本身失败时仍可能出现，属遗留风险，本轮不处理。
