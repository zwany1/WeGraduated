# 系统图设计页：输入交互 redesign

- slug: diagram-input-redesign
- 当前站: 04 Execute（票 01 三片已提交，可送审）
- 已过 Gate: [Brainstorm, Design, Plan]
- 已决问题:
  - [x] 输入范式 → 自然语言优先（轻量 DSL）+ 表单微调
  - [x] 改造范围 → 8 种（三线表不动）
  - [x] 三线表是否并入 → 不动
  - [x] 微调方式 → 画布直接编辑 + 表单折叠
  - [x] 测试接缝 → 后端 Service 层 JUnit 单测（唯一接缝）
  - [x] DSL 语法组织 → 统一核心记号 + 每图少量方言
  - [x] 存量数据降级 → 无 DSL 文本时 render(config) 回填；render 也失败则留空但表单可用
- 未决问题:
  - [ ] 票的拆分粒度是否批准（Plan 站）
  - [ ] DSL 文本持久化：DiagramVO / 数据表需新增列，待实现时确认迁移方式
- 回退记录: 无
- Artifact: spec.md（待 Design 产出）/ issues/（待 Plan 产出）

## 已查明的事实（来自本次代码勘察，非推测）

页面：`frontend/src/views/SystemDesign.vue`（2188 行），路由 `/system-design`，标题「系统图设计」。
顶部 radio-group 共 **9 种图**：

| 值 | 名称 | 当前输入方式 |
| --- | --- | --- |
| FLOW | 流程图 | 文本描述（含节点类型下拉：开始/任务/判断/结束） |
| ARCH | 架构图 | 结构化表单：系统名 + 层 + 每层组件 |
| SWIMLANE | 泳道图 | 结构化表单：泳道 + 节点 |
| ACTIVITY | 活动图 | 结构化表单：节点类型 + 连线 |
| USECASE | 用例图 | 结构化表单：系统名 + 参与者/用例 + 关系(关联/包含/扩展) |
| SEQUENCE | 时序图 | 结构化表单：参与者 + 消息(请求/返回) |
| CLASS | 类图 | 结构化表单：类 + 属性 + 方法 + 关系 |
| ER | ER图 | 结构化表单（另有独立页 `/er`） |
| TABLE3 | 三线表 | 表格化输入 + **粘贴建表 SQL 自动解析**（另有独立页 `/table3`） |

后端两套范式并存：

1. **文本/自然语言 → 图**：`DiagramGenerator`（FLOW/ARCH/SWIMLANE，`FlowParser` 辅助）+
   关键词表（ACTORS/CLIENTS/SERVICES/DATABASES/ACTIONS）。类注释写明
   「输入自然语言描述 + 图类型 → 关键词解析 → 生成图模型 → 自动布局」。
2. **结构化配置 → 图**：6 个 `*RuleEngine`，签名统一为 `build(XxxConfig config)`：
   `ArchitectureRuleEngine` / `SequenceRuleEngine` / `ClassDiagramRuleEngine` /
   `UseCaseRuleEngine` / `SwimlaneRuleEngine` / `ActivityDiagramRuleEngine`。

即：**后端本来就有自然语言入口，但 UI 主要只暴露了结构化表单这一条路**。
`DiagramController` 对外只有 `generate/save/list/load/delete` 五个接口。

其他：
- 前端已有 `frontend/src/utils/mermaid.js`（`toMermaid`），页面有「导出 Mermaid」按钮，
  说明图模型与 Mermaid 之间已存在转换能力。
- 三线表有 `SqlTableParser`（`Table3Controller` 注释：生成三线表 docx / 解析建表 SQL），
  这是 9 种图里唯一的「批量导入」入口 —— 推测是它显得不那么繁琐的原因。
- 另有 `SpecExtractService`（可能是从论文文本抽取图表信息），尚未勘察。

### 补充事实（第二轮勘察）

- 系统图内的 TABLE3 tab（415–477 行）与独立页 `/table3`（ThreeTable.vue）**是两套独立实现**，
  SystemDesign.vue 的 import 里没有 ThreeTable / ErDiagram，它自己调 `parseSql`/`generateTable3`。
  ER tab（357 行）与 `/er`（ErDiagram.vue）同理，也是两套。
- **渲染引擎是 AntV X6**（`import { Graph } from '@antv/x6'` + `DagreLayout`），
  **不是** Mermaid —— 「导出 Mermaid」只是导出格式之一。
  ⇒ 推论：画布本身支持图形交互，「微调」不只有表单一条路。

## 用户诉求（原话）

「系统图里面除了三线表其他的功能是不是该重新设计一下？填字段啥的会不会太繁琐了？」

## 用户已拍板的决策（2026-10-23）

1. 输入范式：**自然语言优先 + 表单微调**
2. 实现方式：**轻量文本 DSL**（本地解析，零外部依赖）
3. 微调粒度：**保留现有全部字段，默认折叠**
4. 三线表：**保持现状不动**

⚠️ 冲突待澄清：决策 1/2 的「全部 9 种统一」与决策 4「三线表不动」互斥 ——
三线表本身就是 9 种之一（TABLE3 tab）。按原话「除了三线表」理解，应为 **8 种**。

## 执行进度

- 票 01（时序图全链路）：**3/3 片已提交**，可送审
  - `ae537d1` 后端解析层（parse/render 双向 + 结构化错误）
  - `1404100` 接口接线（GenDTO.dsl / DiagramVO.dsl / Result.payload）
  - `69a282c` 前端文本输入 UI（防抖预览、示例、行号提示）
- 票 02（画布直接编辑 + 回写文本）：**已完成**
  - 后端新增 `POST /diagram/dsl/render`（配置→文本），前端画布改动经它回填文本视图
  - 前端：双击内联改名、拖动写回模型、时序图生命线/激活条/消息端点跟随、面板折叠为「高级设置」
  - 偏差：折叠暂只对 SEQUENCE 生效（其余 7 种尚无文本入口）；FLOW/CLASS 走 Dagre，坐标不持久
- 票 03（流程图 + 架构图方言）：**已完成**
  - 新增 `FlowConfig` + `FlowRuleEngine`；ARCH 复用现有引擎
  - 前端两 tab 顶部加文本入口，自然语言/逐层配置折叠为「高级设置」并保持可用
  - 验收 1 的「5 个节点」与示例文本（6 个名字）矛盾，按语义断言 6，已在票单标注
- 票 04（类图 + 用例图方言）：**已完成**
  - CLASS：`class 论文 { - 标题: String / + 提交() }` + `A -- B [1 n]: 属于`（长箭头优先匹配）
  - USECASE：`(学生): 提交论文, 查看进度` + `-include->` / `-extend->`
  - 唯一模型改动：`ClassRelation` 增 `label`（关系名），引擎把「基数 基数 关系名」拼一起，前端分流到两端与中间
- 票 05（泳道图 + 活动图 + ER 图方言）：**已完成** —— 8 种图文本入口全部打通
  - SWIMLANE/ACTIVITY 共用「泳道块 + 连线」骨架；活动图可不分泳道、节点可由连线引入
  - ER 走 ErDTO + ERGraphService（Chen 记法），不是 DiagramVO；`1--N` 拼成 `1:N`
  - 8 种全实现后「尚未开放」分支对受支持类型不可达，三条旧测试改为「非法类型要被拒」
- 票 06（存量兼容与降级）：**已完成**
  - 补上页面缺失的「打开」入口（原来只有保存），列表标注「含文本 / 可回填文本 / 仅图形」
  - config 无条件随图落盘；打开时三级降级：原文 → render(config) 回填 → 留空但不阻断
  - 用一份只有 nodes 的老格式 JSON 实测「打开不失败」
- **六阶段工作流 Execute 全部完成**（票 01–06）；下一步可送 Verify / Ship

## 附：泳道/活动图样式调整（用户反馈）
- 泳道背景：淡蓝底+绿虚线+圆角 → 白底+极浅墨线直角+顶部表头细线+居中标题
- 节点：rx10 圆角气泡 → 直角墨线；两个图加「允许缩小以看全」的 fit（表头不再被裁）

### 执行中查到的事实（补充 Brainstorm 阶段未覆盖的）

- **图不存数据库**：`DiagramController` 把 DiagramVO 序列化成 JSON 写到
  `{storage}/diagrams/{userId}_{id}.json`。所以持久化 DSL 只需加字段，不需迁移脚本；
  老 JSON 缺字段反序列化后为 null —— 这正是票 06 的 render 回填切入点。
- **`needCaptcha` 恒为 true**（只要 account 非空），登录必须过验证码；
  验证码是点选/滑动/**旋转**三选一随机 → 自动化登录过不去。
- **JWT 自签也不行**：`LoginInterceptor` 还会校验 `t_login_session` 会话行存在。
- `Result` 原先的非 200 分支只 reject message、丢弃 data，已修（挂到 error.payload）。

## 业务规则（Brainstorm 产物，待用户确认）

| # | 规则（可断言） | 来源 |
| --- | --- | --- |
| R1 | 本轮改造 **8 种**图：FLOW / ARCH / SWIMLANE / ACTIVITY / USECASE / SEQUENCE / CLASS / ER；三线表保持现状 | 用户拍板（冲突已澄清） |
| R2 | 每种图以文本输入框为主入口，写完即可生成，**不要求先填任何表单字段** | 用户拍板 |
| R3 | 文本 → 图模型的解析在本地完成，运行期不得调用任何外部大模型 API | 用户拍板（选 DSL） |
| R4 | DSL 与结构化配置**双向可转换**：表单里的修改切回文本视图时不丢失 | 反直觉细节（单向必丢工作） |
| R5 | 改造前已保存的图能被新界面打开、编辑、再次保存 | 兼容性底线 |
| R6 | 现有表单字段一个不删，高级字段默认折叠 | 用户拍板 |
| R10 | 生成后可在**画布上直接编辑**（拖节点/双击改文案/拉连线），表单折叠在旁作为精确调整 | 用户拍板（X6 已支持） |
| R7 | DSL 解析失败须指出行号与不认识的片段并给语法示例，禁止静默丢弃整段输入 | 可用性底线 |
| R8 | 每种图内置至少一个真实论文场景示例，一键填充 | 降低上手成本 |
| R9 | SVG / PNG / Mermaid 导出行为与现状一致，渲染引擎（X6）与布局算法不变 | 控制爆炸半径 |

### 明确不做

- 不引入大模型 / 外部 API
- 不改渲染引擎（AntV X6）与自动布局算法
- 不改后端 6 个 RuleEngine 产出的图模型结构（只在其上新增「DSL → Config」解析层）
- 不合并 `/table3`、`/er` 独立页与系统图内的 tab（重复入口本轮只澄清、不处理）
