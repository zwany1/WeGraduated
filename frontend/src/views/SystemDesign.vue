<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="$router.push('/home')">‹ 返回</el-button>
        <span>系统图设计</span>
      </div>
      <div class="type-tabs">
        <el-radio-group v-model="type" size="small" @change="onTypeChange">
          <el-radio-button value="FLOW">流程图</el-radio-button>
          <el-radio-button value="ARCH">架构图</el-radio-button>
          <el-radio-button value="SWIMLANE">泳道图</el-radio-button>
          <el-radio-button value="ACTIVITY">活动图</el-radio-button>
          <el-radio-button value="USECASE">用例图</el-radio-button>
          <el-radio-button value="SEQUENCE">时序图</el-radio-button>
          <el-radio-button value="CLASS">类图</el-radio-button>
          <el-radio-button value="ER">ER图</el-radio-button>
          <el-radio-button value="TABLE3">三线表</el-radio-button>
        </el-radio-group>
      </div>
      <div class="actions">
        <el-button size="small" :disabled="!graphReady" @click="save">保存</el-button>
        <el-button size="small" plain :disabled="!graphReady" @click="downloadSvg">SVG</el-button>
        <el-button size="small" type="primary" plain :disabled="!graphReady" @click="downloadPng">导出 PNG</el-button>
        <el-button size="small" type="success" plain @click="exportMermaid">Mermaid</el-button>
      </div>
    </header>

    <MermaidExportDialog ref="mmdDlg" />

    <main class="body">
      <section class="input-panel">
        <!-- 架构图: 结构化配置表单 -->
        <template v-if="type === 'ARCH'">
          <div class="input-title">系统架构配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="系统名称">
              <el-input v-model="config.systemName" placeholder="如：会员卡系统" />
            </el-form-item>
          </el-form>
          <div class="layer-config">
            <div v-for="(layer, li) in config.layers" :key="li" class="layer-config-card">
              <div class="layer-config-head">
                <el-input v-model="layer.name" size="small" placeholder="层名称，如：客户端" class="layer-name-input" />
                <el-button size="small" text type="danger" @click="removeLayer(li)">删除层</el-button>
              </div>
              <div class="layer-comp" v-for="(comp, ci) in layer.components" :key="ci">
                <el-input v-model="comp.name" size="small" placeholder="组件名，如：Vue / SpringBoot / MySQL / Redis" />
                <el-button size="small" text type="danger" @click="removeComp(li, ci)">×</el-button>
              </div>
              <el-button size="small" class="add-comp-btn" @click="addComp(li)">+ 添加组件</el-button>
            </div>
            <el-button size="small" class="add-layer-btn" @click="addLayer">+ 添加层</el-button>
          </div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成架构图</el-button>
            <span class="tip">层名和每层组件可自由编辑</span>
          </div>
        </template>

        <!-- 流程图: 文本描述 -->
        <template v-else-if="type === 'FLOW'">
          <div class="input-title">请输入流程脚本</div>
          <el-input
            v-model="description"
            type="textarea"
            :rows="9"
            placeholder="查询会员余额&#10;if(余额 >= 商品金额)&#10;    扣除余额&#10;    保存订单&#10;else&#10;    返回余额不足"
          />
          <div class="dsl-tip">普通文本 = 节点；if(条件) = 判断(菱形)；else = 否分支；缩进 = 归属</div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成设计图</el-button>
            <span class="tip">含 if( 的脚本走分支解析</span>
          </div>
          <div class="examples">
            <div class="ex-title">示例：</div>
            <div class="ex-item" v-for="(ex, i) in examples" :key="i" @click="useExample(ex)">
              <span class="ex-tag">{{ ex.typeText }}</span>{{ ex.text }}
            </div>
          </div>
        </template>

        <!-- 泳道图: 结构化配置(泳道 + 节点 + 连线) -->
        <template v-else-if="type === 'SWIMLANE'">
          <div class="input-title">泳道图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="业务名称">
              <el-input v-model="swimConfig.flowName" placeholder="如：订单流程" />
            </el-form-item>
          </el-form>
          <div class="layer-config">
            <div v-for="(lane, li) in swimConfig.lanes" :key="li" class="layer-config-card">
              <div class="layer-config-head">
                <el-input v-model="lane.name" size="small" placeholder="参与者，如：用户 / SpringBoot / MySQL" class="layer-name-input" />
                <el-button size="small" text type="danger" @click="removeLane(li)">删除泳道</el-button>
              </div>
              <div class="lane-act" v-for="(node, ni) in lane.nodes" :key="ni">
                <el-select v-model="node.type" size="small" class="act-type" placeholder="类型">
                  <el-option label="开始" value="start" />
                  <el-option label="任务" value="task" />
                  <el-option label="判断" value="gateway" />
                  <el-option label="结束" value="end" />
                </el-select>
                <el-input v-model="node.name" size="small" placeholder="节点，如：提交订单" />
                <el-button size="small" text type="danger" @click="removeNode(li, ni)">×</el-button>
              </div>
              <el-button size="small" class="add-comp-btn" @click="addNode(li)">+ 添加节点</el-button>
            </div>
            <el-button size="small" class="add-layer-btn" @click="addLane">+ 添加泳道</el-button>
          </div>
          <div class="edge-config">
            <div class="edge-title">流程连线</div>
            <div v-for="(eg, ei) in swimConfig.edges" :key="ei" class="item-card">
              <span class="item-num">连线 {{ ei + 1 }}</span>
              <el-select v-model="eg.source" size="small" class="edge-sel" placeholder="从节点">
                <el-option v-for="nd in allNodes" :key="nd.key" :label="nd.label" :value="nd.id" />
              </el-select>
              <span class="edge-arrow">→</span>
              <el-select v-model="eg.target" size="small" class="edge-sel" placeholder="到节点">
                <el-option v-for="nd in allNodes" :key="nd.key" :label="nd.label" :value="nd.id" />
              </el-select>
              <el-button size="small" text type="danger" @click="removeEdge(ei)">×</el-button>
            </div>
            <el-button size="small" class="add-comp-btn" @click="addEdge">+ 添加连线</el-button>
            <div class="tip" style="margin-top:6px">泳道横向排列，流程按连线顺序纵向流动</div>
          </div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成泳道图</el-button>
            <span class="tip">未配连线时按节点顺序自动连</span>
          </div>
        </template>

        <!-- 活动图: 结构化配置(泳道 + 节点 + 连线) -->
        <template v-else-if="type === 'ACTIVITY'">
          <div class="input-title">活动图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="图标题">
              <el-input v-model="actConfig.title" placeholder="如：借阅流程" />
            </el-form-item>
          </el-form>

          <div class="section-divider"></div>
          <div class="input-title">泳道</div>
          <div class="uc-act" v-for="(l, li) in actConfig.lanes" :key="li">
            <el-input v-model="l.name" size="small" placeholder="角色，如：工作人员 / 读者 / 系统" />
            <el-button size="small" text type="danger" @click="removeActLane(li)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addActLane">+ 添加泳道</el-button>

          <div class="section-divider"></div>
          <div class="input-title">节点</div>
          <div v-for="(n, ni) in actConfig.nodes" :key="ni" class="item-card">
            <span class="item-num">节点 {{ ni + 1 }}</span>
            <el-select v-model="n.type" size="small" class="act-type" placeholder="类型">
              <el-option label="开始" value="START" />
              <el-option label="活动" value="ACTION" />
              <el-option label="判断" value="DECISION" />
              <el-option label="结束" value="END" />
            </el-select>
            <el-select v-model="n.laneId" size="small" class="edge-sel" placeholder="泳道">
              <el-option v-for="l in actLanes" :key="l.id" :label="l.name" :value="l.id" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeActNode(ni)">×</el-button>
            <el-input v-model="n.text" size="small" placeholder="节点内容，如：进入借阅界面" style="margin-top:4px;margin-bottom:6px" />
          </div>
          <el-button size="small" class="add-comp-btn" @click="addActNode">+ 添加节点</el-button>

          <div class="section-divider"></div>
          <div class="input-title">连线</div>
          <div v-for="(e, ei) in actConfig.edges" :key="ei" class="item-card">
            <span class="item-num">连线 {{ ei + 1 }}</span>
            <el-select v-model="e.source" size="small" class="edge-sel" placeholder="来源">
              <el-option v-for="nd in actNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="e.target" size="small" class="edge-sel" placeholder="目标">
              <el-option v-for="nd in actNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeActEdge(ei)">×</el-button>
            <el-input v-model="e.label" size="small" placeholder="分支条件(可空)" style="margin-top:4px;margin-bottom:6px" />
          </div>
          <el-button size="small" class="add-comp-btn" @click="addActEdge">+ 添加连线</el-button>
          <div class="tip" style="margin-top:6px">判断节点分支需配置条件；泳道横向排列，流程按连线顺序纵向流动</div>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成活动图</el-button>
          </div>
        </template>

        <!-- 用例图: 结构化配置(参与者 + 用例 + 关系) -->
        <template v-else-if="type === 'USECASE'">
          <div class="input-title">用例图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="系统名称">
              <el-input v-model="ucConfig.system" placeholder="如：人事管理系统" />
            </el-form-item>
          </el-form>

          <div class="section-divider"></div>
          <div class="input-title">参与者 Actor</div>
          <div class="uc-act" v-for="(a, ai) in ucConfig.actors" :key="ai">
            <el-input v-model="a.name" size="small" placeholder="参与者，如：人事管理员" />
            <el-button size="small" text type="danger" @click="removeUcActor(ai)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcActor">+ 添加参与者</el-button>

          <div class="section-divider"></div>
          <div class="input-title">用例 UseCase</div>
          <div class="uc-act" v-for="(u, ui) in ucConfig.usecases" :key="ui">
            <el-input v-model="u.name" size="small" placeholder="用例，如：人员规划" />
            <el-input v-model="u.module" size="small" placeholder="模块(可选)" style="margin-left:6px" />
            <el-button size="small" text type="danger" @click="removeUcUseCase(ui)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcUseCase">+ 添加用例</el-button>

          <div class="section-divider"></div>
          <div class="input-title">关系</div>
          <div v-for="(r, ri) in ucConfig.relations" :key="ri" class="item-card">
            <span class="item-num">关系 {{ ri + 1 }}</span>
            <el-select v-model="r.type" size="small" class="act-type" placeholder="类型">
              <el-option label="关联" value="association" />
              <el-option label="包含" value="include" />
              <el-option label="扩展" value="extend" />
            </el-select>
            <el-select v-model="r.source" size="small" class="edge-sel" placeholder="来源">
              <el-option v-for="nd in ucAllNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="r.target" size="small" class="edge-sel" placeholder="目标">
              <el-option v-for="nd in ucAllNodes" :key="nd.id" :label="nd.label" :value="nd.id" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeUcRelation(ri)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addUcRelation">+ 添加关系</el-button>
          <div class="tip" style="margin-top:6px">关联=实线；包含/扩展=虚线箭头«include»</div>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成用例图</el-button>
          </div>
        </template>

        <!-- 时序图: 结构化配置(参与者 + 消息) -->
        <template v-else-if="type === 'SEQUENCE'">
          <div class="input-title">时序图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="图标题">
              <el-input v-model="seqConfig.title" placeholder="如：用户登录流程" />
            </el-form-item>
          </el-form>

          <div class="section-divider"></div>
          <div class="input-title">参与者 Lifeline</div>
          <div class="uc-act" v-for="(p, pi) in seqConfig.participants" :key="pi">
            <el-input v-model="p.name" size="small" placeholder="参与者，如：用户 / Controller / Service / 数据库" />
            <el-button size="small" text type="danger" @click="removeSeqParticipant(pi)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addSeqParticipant">+ 添加参与者</el-button>

          <div class="section-divider"></div>
          <div class="input-title">消息</div>
          <div v-for="(m, mi) in seqConfig.messages" :key="mi" class="item-card">
            <span class="item-num">消息 {{ mi + 1 }}</span>
            <el-select v-model="m.from" size="small" class="edge-sel" placeholder="来源">
              <el-option v-for="p in seqParticipants" :key="p.id" :label="p.name || p.id" :value="p.id" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="m.to" size="small" class="edge-sel" placeholder="目标">
              <el-option v-for="p in seqParticipants" :key="p.id" :label="p.name || p.id" :value="p.id" />
            </el-select>
            <el-select v-model="m.type" size="small" class="act-type" placeholder="类型">
              <el-option label="请求" value="request" />
              <el-option label="返回" value="return" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeSeqMessage(mi)">×</el-button>
            <el-input v-model="m.text" size="small" placeholder="消息内容，如：登录" style="margin-top:4px;margin-bottom:6px" />
          </div>
          <el-button size="small" class="add-comp-btn" @click="addSeqMessage">+ 添加消息</el-button>
          <div class="tip" style="margin-top:6px">消息按添加顺序纵向排列；请求=实线，返回=虚线</div>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成时序图</el-button>
          </div>
        </template>

        <!-- 类图: 结构化配置(类 + 属性/方法 + 关系) -->
        <template v-else-if="type === 'CLASS'">
          <div class="input-title">类图配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="图标题">
              <el-input v-model="clsConfig.title" placeholder="如：会员系统类图" />
            </el-form-item>
          </el-form>

          <div class="section-divider"></div>
          <div class="input-title">类</div>
          <div v-for="(c, ci) in clsConfig.classes" :key="ci" class="cls-card">
            <div class="cls-head">
              <el-input v-model="c.name" size="small" placeholder="类名，如：用户" class="cls-name-input" />
              <el-button size="small" text type="danger" @click="removeClsClass(ci)">删除类</el-button>
            </div>
            <div class="cls-sub">属性</div>
            <div class="cls-row" v-for="(a, ai) in c.attributes" :key="ai">
              <el-select v-model="a.visibility" size="small" class="vis-sel">
                <el-option label="+" value="+" />
                <el-option label="-" value="-" />
                <el-option label="#" value="#" />
              </el-select>
              <el-input v-model="a.name" size="small" placeholder="属性名" style="margin-left:4px" />
              <el-input v-model="a.type" size="small" placeholder="类型" style="margin-left:4px" />
              <el-button size="small" text type="danger" @click="removeClsAttr(ci, ai)">×</el-button>
            </div>
            <el-button size="small" class="add-comp-btn" @click="addClsAttr(ci)">+ 属性</el-button>
            <div class="cls-sub">方法</div>
            <div class="cls-row" v-for="(m, mi) in c.methods" :key="mi">
              <el-select v-model="m.visibility" size="small" class="vis-sel">
                <el-option label="+" value="+" />
                <el-option label="-" value="-" />
                <el-option label="#" value="#" />
              </el-select>
              <el-input v-model="m.name" size="small" placeholder="方法名" style="margin-left:4px" />
              <el-input v-model="m.returnType" size="small" placeholder="返回" style="margin-left:4px" />
              <el-button size="small" text type="danger" @click="removeClsMethod(ci, mi)">×</el-button>
            </div>
            <el-button size="small" class="add-comp-btn" @click="addClsMethod(ci)">+ 方法</el-button>
          </div>
          <el-button size="small" class="add-layer-btn" @click="addClsClass">+ 添加类</el-button>

          <div class="section-divider"></div>
          <div class="input-title">关系</div>
          <div v-for="(r, ri) in clsConfig.relations" :key="ri" class="item-card">
            <span class="item-num">关系 {{ ri + 1 }}</span>
            <el-select v-model="r.source" size="small" class="edge-sel" placeholder="来源">
              <el-option v-for="c in clsAllClasses" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="r.target" size="small" class="edge-sel" placeholder="目标">
              <el-option v-for="c in clsAllClasses" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="r.type" size="small" class="act-type" placeholder="类型">
              <el-option label="关联" value="association" />
              <el-option label="继承" value="inheritance" />
            </el-select>
            <el-button size="small" text type="danger" @click="removeClsRelation(ri)">×</el-button>
            <div class="cls-row" style="margin-top:4px">
              <el-input v-model="r.left" size="small" placeholder="基数左(1/n)" style="margin-right:4px" />
              <el-input v-model="r.right" size="small" placeholder="基数右(1/n)" />
            </div>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addClsRelation">+ 添加关系</el-button>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成类图</el-button>
          </div>
        </template>

        <!-- ER 图: 实体 + 关系 -->
        <template v-else-if="type === 'ER'">
          <div class="input-title">ER 图配置</div>
          <div class="input-row" style="margin-bottom:10px">
            <el-button size="small" @click="erShowSql = !erShowSql">{{ erShowSql ? '收起 SQL 导入' : '从 SQL 生成' }}</el-button>
            <span class="tip">支持多张建表语句与外键关系</span>
          </div>
          <div v-if="erShowSql">
            <el-input v-model="erSql" type="textarea" :rows="6" placeholder="CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50));&#10;CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, FOREIGN KEY (user_id) REFERENCES users(id));" />
            <div class="input-row">
              <el-button size="small" :loading="erSqlLoading" @click="erImportFromSql">解析 SQL 生成实体</el-button>
            </div>
            <div class="section-divider"></div>
          </div>

          <div class="input-title">实体</div>
          <div v-for="(e, ei) in erConfig.entities" :key="ei" class="cls-card">
            <div class="cls-head">
              <el-input v-model="e.name" size="small" placeholder="实体名，如：用户" class="cls-name-input" />
              <el-button size="small" text type="danger" @click="removeErEntity(ei)">删除实体</el-button>
            </div>
            <div class="cls-sub">属性（勾选为主键）</div>
            <div class="cls-row" v-for="(a, ai) in e.attrs" :key="ai">
              <el-checkbox v-model="a.key" size="small" />
              <el-input v-model="a.name" size="small" placeholder="属性名" style="margin-left:4px" />
              <el-button size="small" text type="danger" @click="removeErAttr(ei, ai)">×</el-button>
            </div>
            <el-button size="small" class="add-comp-btn" @click="addErAttr(ei)">+ 属性</el-button>
          </div>
          <el-button size="small" class="add-layer-btn" @click="addErEntity">+ 添加实体</el-button>

          <div class="section-divider"></div>
          <div class="input-title">关系</div>
          <div v-for="(r, ri) in erConfig.relations" :key="ri" class="item-card">
            <span class="item-num">关系 {{ ri + 1 }}</span>
            <el-select v-model="r.from" size="small" class="edge-sel" placeholder="来源实体">
              <el-option v-for="e in erEntities" :key="e.id" :label="e.label" :value="e.value" />
            </el-select>
            <span class="edge-arrow">→</span>
            <el-select v-model="r.to" size="small" class="edge-sel" placeholder="目标实体">
              <el-option v-for="e in erEntities" :key="e.id" :label="e.label" :value="e.value" />
            </el-select>
            <el-select v-model="r.cardinality" size="small" class="act-type" placeholder="基数">
              <el-option label="一对一" value="1:1" />
              <el-option label="一对多" value="1:n" />
              <el-option label="多对一" value="n:1" />
              <el-option label="多对多" value="n:n" />
            </el-select>
            <el-input v-model="r.label" size="small" placeholder="关系名" class="edge-sel" />
            <el-button size="small" text type="danger" @click="removeErRelation(ri)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="addErRelation">+ 添加关系</el-button>

          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成 ER 图</el-button>
          </div>
        </template>

        <!-- 三线表: 表格内容生成 docx -->
        <template v-else-if="type === 'TABLE3'">
          <div class="input-title">三线表配置</div>
          <el-form label-width="70px" size="small">
            <el-form-item label="表题">
              <el-input v-model="t3.title" placeholder="如：表 1 学生信息表" />
            </el-form-item>
          </el-form>
          <div class="input-row" style="margin-bottom:10px">
            <el-button size="small" @click="t3ShowSql = !t3ShowSql">{{ t3ShowSql ? '收起 SQL 导入' : '从 SQL 导入' }}</el-button>
          </div>
          <div v-if="t3ShowSql">
            <el-input v-model="t3.sqlText" type="textarea" :rows="5" placeholder="CREATE TABLE t (id INT PRIMARY KEY, name VARCHAR(20), age INT);" />
            <div class="input-row">
              <el-button size="small" :loading="t3SqlLoading" @click="t3ImportFromSql">解析建表 SQL</el-button>
            </div>
            <div class="section-divider"></div>
          </div>
          <div class="input-title">表头</div>
          <div class="cls-row" v-for="(h, hi) in t3.headers" :key="hi">
            <el-input v-model="t3.headers[hi]" size="small" placeholder="列名" />
            <el-button size="small" text type="danger" @click="t3.headers.splice(hi, 1)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="t3.headers.push('')">+ 表头</el-button>
          <div class="section-divider"></div>
          <div class="input-title">数据行</div>
          <div v-for="(row, ri) in t3.rows" :key="ri" class="cls-row">
            <el-input v-for="(c, ci) in row" :key="ci" v-model="t3.rows[ri][ci]" size="small" placeholder="值" style="margin-right:4px" />
            <el-button size="small" text type="danger" @click="t3.rows.splice(ri, 1)">×</el-button>
          </div>
          <el-button size="small" class="add-comp-btn" @click="t3.rows.push(Array(t3.headers.length).fill(''))">+ 数据行</el-button>
          <div class="t3-preview">
            <div class="t3-caption">{{ t3.title || '表题' }}</div>
            <table class="t3-table">
              <thead>
                <tr><th v-for="(h, hi) in t3.headers" :key="'th' + hi">{{ h || '　' }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(row, ri) in t3.rows" :key="'tr' + ri">
                  <td v-for="(c, ci) in row" :key="'td' + ri + '-' + ci">{{ c || '　' }}</td>
                </tr>
              </tbody>
            </table>
            <div class="t3-tip">顶线/底线 1.5pt 粗线 · 栏目线 0.75pt 细线 · 无竖线（预览为简化样式，下载为准）</div>
          </div>
          <div class="input-row">
            <el-button type="primary" :loading="generating" @click="generate">生成三线表文档</el-button>
            <span class="tip">生成 .docx 三线表并下载</span>
          </div>
        </template>
      </section>

      <section class="canvas-wrap">
        <div v-if="!graphReady && currentVO?.type !== 'ARCH'" class="canvas-empty">配置后点击「生成」预览</div>
        <div v-if="graphReady && currentVO && currentVO.type === 'ARCH'" class="arch-box" ref="archRef">
          <div class="arch-outer">
            <div class="arch-title">{{ currentVO.name || '系统架构图' }}</div>
            <template v-for="(layer, li) in archLayers" :key="layer.name">
              <div class="arch-divider"></div>
              <div class="arch-layer">
                <div class="arch-layer-name">{{ layer.name }}</div>
                <div class="arch-layer-body">
                  <div v-for="(node, i) in layer.nodes" :key="i" class="arch-comp">
                    {{ node.label }}
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div class="arch-export">
            <el-button size="small" plain @click="downloadArchSvg">SVG</el-button>
            <el-button size="small" type="primary" plain @click="downloadArchPng">PNG</el-button>
          </div>
        </div>
        <!-- FLOW/SWIMLANE/USECASE: X6 画布 -->
        <div v-show="graphReady && currentVO && currentVO.type !== 'ARCH'" ref="container" class="x6-container"></div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Graph } from '@antv/x6'
import { DagreLayout } from '@antv/layout'
import html2canvas from 'html2canvas'
import { generateDiagram, saveDiagram } from '../api/diagram'
import { getErGraph } from '../api/er'
import { parseSql, generateTable3 } from '../api/table3'
import { toMermaid } from '../utils/mermaid'
import MermaidExportDialog from '../components/MermaidExportDialog.vue'

// 注册数据库圆柱形状
Graph.registerNode('db', {
  inherit: 'rect',
  markup: [
    { tagName: 'ellipse', selector: 'top' },
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    top: { cx: 0, cy: 0, rx: 16, ry: 8, fill: '#fff', stroke: '#333', strokeWidth: 1.5, refX: 0.5, refY: 0 },
    body: { refWidth: '100%', refHeight: '100%', fill: '#fff', stroke: '#333', strokeWidth: 1.5 },
    label: { text: '', fontSize: 12, fill: '#333', refX: 0.5, refY: 0.55, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  }
})

// 注册 Actor 小人节点(用例图): 使用 xiaoren.svg 图片渲染
Graph.registerNode('actorNode', {
  inherit: 'rect',
  width: 120,
  height: 120,
  markup: [
    { tagName: 'image', selector: 'img' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    img: {
      'xlink:href': '/xiaoren.svg',
      width: 42,
      height: 42,
      x: 0,
      y: 0,
      refX: 0.5,
      refY: 0.25,
      xAlign: 'middle',
      yAlign: 'middle'
    },
    label: { text: '', fontSize: 12, fill: '#333', refX: 0.5, refY: 0.75, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  }
})

// 注册系统边界框节点(用例图)
Graph.registerNode('systemNode', {
  inherit: 'rect',
  markup: [
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'title' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    body: { refWidth: '100%', refHeight: '100%', fill: 'rgba(255,255,255,0.4)', stroke: '#333', strokeWidth: 2, rx: 4, ry: 4 },
    title: { text: '', fontSize: 13, fontWeight: 700, fill: '#333', refX: 0.5, refY: 0, refY2: 18, textAnchor: 'middle', textVerticalAnchor: 'middle' },
    label: { text: '' }
  }
})

// 注册类图节点(类名/属性/方法三段式, HTML 渲染保证文本不溢出)
Graph.registerNode('classNode', {
  inherit: 'html',
  width: 230,
  height: 120,
  html(cell) {
    const { name, attrs, methods } = cell.getData() || {}
    const esc = s => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    const attrsHtml = attrs
      ? `<div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${esc(attrs)}</div>`
      : '<div style="padding:4px 10px;font-size:12px;color:#999">(无属性)</div>'
    const methodsHtml = methods
      ? `<div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${esc(methods)}</div>`
      : '<div style="padding:4px 10px;font-size:12px;color:#999">(无方法)</div>'
    return `<div style="width:100%;height:100%;display:flex;flex-direction:column;box-sizing:border-box;background:#fff;border:1.5px solid #333;overflow:hidden">
      <div style="text-align:center;font-weight:bold;font-size:14px;padding:6px 8px;border-bottom:1.5px solid #333;background:#f5f6fa;color:#333">${esc(name)}</div>
      ${attrsHtml}
      <div style="border-top:1px solid #999"></div>
      ${methodsHtml}
    </div>`
  }
})

// 活动图开始节点: UML 实心黑圆 ●
Graph.registerNode('activityStart', {
  inherit: 'circle',
  width: 30,
  height: 30,
  attrs: {
    body: { fill: '#000000', stroke: '#000000', strokeWidth: 1.5 }
  }
})

// 活动图结束节点: UML 双圆叠加 ◎(外圈空心 + 内圈实心)
Graph.registerNode('activityEnd', {
  inherit: 'circle',
  width: 34,
  height: 34,
  markup: [
    { tagName: 'circle', selector: 'outer' },
    { tagName: 'circle', selector: 'inner' }
  ],
  attrs: {
    outer: { cx: 17, cy: 17, r: 13, fill: '#fff', stroke: '#000', strokeWidth: 2 },
    inner: { cx: 17, cy: 17, r: 6, fill: '#000' }
  }
})

let graph = null
const container = ref(null)
const archRef = ref(null)
const mmdDlg = ref(null)
const type = ref('SWIMLANE')
const description = ref('')
const generating = ref(false)
const graphReady = ref(false)
const currentVO = ref(null)

// 架构图配置: 动态层结构(默认示例)
const config = ref({
  systemName: '会员卡系统',
  layers: [
    { name: '客户端', components: [{ name: 'Vue' }, { name: '后台管理系统' }] },
    { name: '业务层', components: [{ name: '用户管理API' }, { name: '订单API' }] },
    { name: '运行支持层', components: [{ name: 'Nginx' }, { name: 'Docker' }] },
    { name: '服务应用层', components: [{ name: 'SpringBoot1' }, { name: 'SpringBoot2' }] },
    { name: '数据层', components: [{ name: 'Redis' }, { name: 'RabbitMQ' }] },
    { name: '数据库', components: [{ name: 'MySQL 主' }, { name: 'MySQL 从' }] }
  ]
})

// 泳道图配置(BPMN 模型): 泳道(Lane) + 节点(Node) + 连线(Edge)
const swimConfig = ref({
  flowName: '订单流程',
  lanes: [
    { name: '用户', nodes: [
      { id: 'N1', name: '开始', type: 'start' },
      { id: 'N2', name: '提交订单', type: 'task' }
    ]},
    { name: 'Vue客户端', nodes: [
      { id: 'N3', name: '发送请求', type: 'task' },
      { id: 'N4', name: '展示结果', type: 'task' }
    ]},
    { name: 'SpringBoot服务', nodes: [
      { id: 'N5', name: '校验库存', type: 'gateway' },
      { id: 'N6', name: '创建订单', type: 'task' }
    ]},
    { name: 'MySQL数据库', nodes: [
      { id: 'N7', name: '保存订单', type: 'task' },
      { id: 'N8', name: '结束', type: 'end' }
    ]}
  ],
  edges: [
    { source: 'N1', target: 'N2' },
    { source: 'N2', target: 'N3' },
    { source: 'N3', target: 'N5' },
    { source: 'N5', target: 'N6', label: '通过' },
    { source: 'N6', target: 'N7' },
    { source: 'N7', target: 'N4' },
    { source: 'N4', target: 'N8' }
  ]
})
let nodeSeq = 9

// 用例图配置: 参与者 + 用例 + 关系
const ucConfig = ref({
  system: '人事管理系统',
  actors: [
    { id: 'A1', name: '人事管理员' },
    { id: 'A2', name: '普通员工' }
  ],
  usecases: [
    { id: 'U1', name: '人员规划', module: '人员管理' },
    { id: 'U2', name: '人员现状分析', module: '人员管理' },
    { id: 'U3', name: '人员年龄分析', module: '人员管理' },
    { id: 'U4', name: '登录系统', module: '' }
  ],
  relations: [
    { source: 'A1', target: 'U1', type: 'association' },
    { source: 'U1', target: 'U2', type: 'include' },
    { source: 'U1', target: 'U3', type: 'include' },
    { source: 'A2', target: 'U4', type: 'association' }
  ]
})
let ucSeq = 5

// 用例图: 参与者/用例/关系管理
function addUcActor() {
  ucConfig.value.actors.push({ id: 'A' + (ucSeq++), name: '' })
}
function removeUcActor(ai) {
  const id = ucConfig.value.actors[ai].id
  ucConfig.value.actors.splice(ai, 1)
  ucConfig.value.relations = ucConfig.value.relations.filter(r => r.source !== id && r.target !== id)
}
function addUcUseCase() {
  ucConfig.value.usecases.push({ id: 'U' + (ucSeq++), name: '', module: '' })
}
function removeUcUseCase(ui) {
  const id = ucConfig.value.usecases[ui].id
  ucConfig.value.usecases.splice(ui, 1)
  ucConfig.value.relations = ucConfig.value.relations.filter(r => r.source !== id && r.target !== id)
}
function addUcRelation() {
  ucConfig.value.relations.push({ source: '', target: '', type: 'association' })
}
function removeUcRelation(ri) {
  ucConfig.value.relations.splice(ri, 1)
}
// 全部参与者+用例(供关系下拉)
const ucAllNodes = computed(() => {
  const list = []
  ucConfig.value.actors.forEach(a => list.push({ id: a.id, label: a.name || ('参与者 ' + a.id) }))
  ucConfig.value.usecases.forEach(u => list.push({ id: u.id, label: u.name || ('用例 ' + u.id) }))
  return list
})
function buildUseCasePayload() {
  const actors = ucConfig.value.actors.filter(a => a.name && a.name.trim()).map(a => ({ id: a.id, name: a.name.trim() }))
  const usecases = ucConfig.value.usecases.filter(u => u.name && u.name.trim()).map(u => ({ id: u.id, name: u.name.trim(), module: u.module || '' }))
  const relations = (ucConfig.value.relations || [])
    .filter(r => r.source && r.target && r.source !== r.target)
    .map(r => ({ source: r.source, target: r.target, type: r.type || 'association' }))
  return { system: ucConfig.value.system, actors, usecases, relations }
}

// 时序图配置: 参与者 + 消息
const seqConfig = ref({
  title: '用户登录流程',
  participants: [
    { id: 'P1', name: '用户' },
    { id: 'P2', name: 'Vue表现层' },
    { id: 'P3', name: 'Controller' },
    { id: 'P4', name: 'Service' },
    { id: 'P5', name: '数据库' }
  ],
  messages: [
    { id: 'M1', from: 'P1', to: 'P2', text: '登录', type: 'request' },
    { id: 'M2', from: 'P2', to: 'P3', text: '输入账号密码', type: 'request' },
    { id: 'M3', from: 'P3', to: 'P4', text: '封装userLogin', type: 'request' },
    { id: 'M4', from: 'P4', to: 'P5', text: '查询用户', type: 'request' },
    { id: 'M5', from: 'P5', to: 'P4', text: '返回用户信息', type: 'return' },
    { id: 'M6', from: 'P4', to: 'P1', text: '登录成功', type: 'return' }
  ]
})
let seqSeq = 7

function addSeqParticipant() {
  seqConfig.value.participants.push({ id: 'P' + (seqSeq++), name: '' })
}
function removeSeqParticipant(pi) {
  const id = seqConfig.value.participants[pi].id
  seqConfig.value.participants.splice(pi, 1)
  seqConfig.value.messages = seqConfig.value.messages.filter(m => m.from !== id && m.to !== id)
}
function addSeqMessage() {
  const from = seqConfig.value.participants[0]?.id || ''
  const to = seqConfig.value.participants[1]?.id || ''
  seqConfig.value.messages.push({ id: 'M' + (seqSeq++), from, to, text: '', type: 'request' })
}
function removeSeqMessage(mi) {
  seqConfig.value.messages.splice(mi, 1)
}
// 参与者列表(供消息下拉)
const seqParticipants = computed(() =>
  seqConfig.value.participants.filter(p => p.name && p.name.trim()).map(p => ({ id: p.id, name: p.name.trim() }))
)
function buildSequencePayload() {
  const participants = seqConfig.value.participants
    .filter(p => p.name && p.name.trim())
    .map(p => ({ id: p.id, name: p.name.trim() }))
  const messages = (seqConfig.value.messages || [])
    .filter(m => m.from && m.to && m.text && m.text.trim())
    .map(m => ({ id: m.id, from: m.from, to: m.to, text: m.text.trim(), type: m.type || 'request' }))
  return { title: seqConfig.value.title, participants, messages }
}

// 类图配置: 类 + 属性/方法 + 关系
const clsConfig = ref({
  title: '会员系统类图',
  classes: [
    {
      id: 'C1', name: '用户',
      attributes: [
        { name: '用户ID', type: 'int', visibility: '-' },
        { name: '用户名', type: 'string', visibility: '-' },
        { name: '密码', type: 'string', visibility: '-' }
      ],
      methods: [
        { name: '查询', returnType: '用户', visibility: '+' },
        { name: '新增', returnType: 'void', visibility: '+' }
      ]
    },
    {
      id: 'C2', name: '订单',
      attributes: [
        { name: '订单ID', type: 'int', visibility: '-' },
        { name: '用户ID', type: 'int', visibility: '-' }
      ],
      methods: [
        { name: '创建', returnType: 'void', visibility: '+' }
      ]
    },
    {
      id: 'C3', name: '商品',
      attributes: [
        { name: '商品ID', type: 'int', visibility: '-' },
        { name: '名称', type: 'string', visibility: '-' }
      ],
      methods: [
        { name: '查询', returnType: '商品', visibility: '+' }
      ]
    }
  ],
  relations: [
    { source: 'C1', target: 'C2', type: 'association', left: '1', right: 'n' },
    { source: 'C2', target: 'C3', type: 'association', left: '1', right: 'n' }
  ]
})
let clsSeq = 4

function addClsClass() {
  clsConfig.value.classes.push({ id: 'C' + (clsSeq++), name: '', attributes: [], methods: [] })
}
function removeClsClass(ci) {
  const id = clsConfig.value.classes[ci].id
  clsConfig.value.classes.splice(ci, 1)
  clsConfig.value.relations = clsConfig.value.relations.filter(r => r.source !== id && r.target !== id)
}
function addClsAttr(ci) {
  clsConfig.value.classes[ci].attributes.push({ name: '', type: '', visibility: '-' })
}
function removeClsAttr(ci, ai) {
  clsConfig.value.classes[ci].attributes.splice(ai, 1)
}
function addClsMethod(ci) {
  clsConfig.value.classes[ci].methods.push({ name: '', returnType: 'void', visibility: '+' })
}
function removeClsMethod(ci, mi) {
  clsConfig.value.classes[ci].methods.splice(mi, 1)
}
function addClsRelation() {
  const a = clsConfig.value.classes[0]?.id || ''
  const b = clsConfig.value.classes[1]?.id || ''
  clsConfig.value.relations.push({ source: a, target: b, type: 'association', left: '1', right: 'n' })
}
function removeClsRelation(ri) {
  clsConfig.value.relations.splice(ri, 1)
}
const clsAllClasses = computed(() =>
  clsConfig.value.classes.filter(c => c.name && c.name.trim()).map(c => ({ id: c.id, name: c.name.trim() }))
)
function buildClassPayload() {
  const classes = clsConfig.value.classes
    .filter(c => c.name && c.name.trim())
    .map(c => ({
      id: c.id,
      name: c.name.trim(),
      attributes: (c.attributes || []).filter(a => a.name && a.name.trim()).map(a => ({
        name: a.name.trim(), type: a.type || '', visibility: a.visibility || '-'
      })),
      methods: (c.methods || []).filter(m => m.name && m.name.trim()).map(m => ({
        name: m.name.trim(), returnType: m.returnType || 'void', visibility: m.visibility || '+'
      }))
    }))
  const relations = (clsConfig.value.relations || [])
    .filter(r => r.source && r.target && r.source !== r.target)
    .map(r => ({ source: r.source, target: r.target, type: r.type || 'association', left: r.left || '', right: r.right || '' }))
  return { title: clsConfig.value.title, classes, relations }
}

// ==================== ER 图配置 ====================
const erConfig = ref({
  entities: [
    { name: '用户', attrs: [{ name: 'id', key: true }, { name: '姓名', key: false }] },
    { name: '订单', attrs: [{ name: 'id', key: true }, { name: 'user_id', key: false }] }
  ],
  relations: [
    { from: '用户', to: '订单', cardinality: '1:n' }
  ]
})
const erShowSql = ref(false)
const erSql = ref('')
const erSqlLoading = ref(false)
let erSeq = 0

const erEntities = computed(() => erConfig.value.entities.map((e, i) => ({
  value: e.name || ('实体' + i),
  label: e.name || ('实体' + i)
})))

function addErEntity() {
  erConfig.value.entities.push({ name: '', attrs: [{ name: '', key: false }] })
}
function removeErEntity(ei) {
  const name = erConfig.value.entities[ei].name
  erConfig.value.entities.splice(ei, 1)
  erConfig.value.relations = erConfig.value.relations.filter(r => r.from !== name && r.to !== name)
}
function addErAttr(ei) {
  erConfig.value.entities[ei].attrs.push({ name: '', key: false })
}
function removeErAttr(ei, ai) {
  erConfig.value.entities[ei].attrs.splice(ai, 1)
}
function addErRelation() {
  erConfig.value.relations.push({ from: '', to: '', cardinality: '1:n' })
}
function removeErRelation(ri) {
  erConfig.value.relations.splice(ri, 1)
}
/** 从建表 SQL 生成实体与关系 */
async function erImportFromSql() {
  if (!erSql.value.trim()) {
    ElMessage.warning('请输入建表 SQL')
    return
  }
  erSqlLoading.value = true
  try {
    const res = await fetch('/api/er/parse-sql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + localStorage.getItem('token') },
      body: JSON.stringify({ sql: erSql.value })
    })
    const j = await res.json()
    if (j.code !== 200) {
      ElMessage.error(j.message || '解析失败')
      return
    }
    erConfig.value.entities = j.data.entities.map(e => ({ name: e.name, attrs: e.attrs }))
    erConfig.value.relations = (j.data.relations || []).map(r => ({ from: r.from, to: r.to, label: r.label || '关联', cardinality: r.cardinality || '1:n' }))
    erShowSql.value = false
    ElMessage.success(`已解析 ${erConfig.value.entities.length} 个实体、${erConfig.value.relations.length} 个关系`)
  } catch (e) {
    ElMessage.error('解析失败')
  } finally {
    erSqlLoading.value = false
  }
}

// ==================== 三线表配置 ====================
const t3 = ref({
  title: '表 1 数据表',
  headers: ['字段名', '类型', '说明'],
  rows: [['id', 'int', '主键'], ['name', 'varchar', '姓名']],
  sqlText: ''
})
const t3ShowSql = ref(false)
const t3SqlLoading = ref(false)
async function t3ImportFromSql() {
  if (!t3.value.sqlText.trim()) {
    ElMessage.warning('请输入建表 SQL')
    return
  }
  t3SqlLoading.value = true
  try {
    const info = await parseSql(t3.value.sqlText)
    t3.value.headers = ['字段名', '数据类型', '是否可空', '默认值', '说明']
    t3.value.rows = (info.columns || []).map(c => [c.name || '', c.type || '', c.nullable === false ? '否' : '是', c.defaultValue || '', c.comment || ''])
    t3ShowSql.value = false
    ElMessage.success(`已解析 ${t3.value.rows.length} 列`)
  } catch (e) {
    ElMessage.error(e.message || '解析失败')
  } finally {
    t3SqlLoading.value = false
  }
}

// 活动图配置: 泳道 + 节点 + 连线
const actConfig = ref({
  title: '借阅流程',
  lanes: [
    { id: 'L1', name: '图书馆工作人员' },
    { id: 'L2', name: '读者' },
    { id: 'L3', name: '系统' }
  ],
  nodes: [
    { id: 'N1', text: '开始', type: 'START', laneId: 'L1' },
    { id: 'N2', text: '进入借阅界面', type: 'ACTION', laneId: 'L1' },
    { id: 'N3', text: '刷一卡通', type: 'ACTION', laneId: 'L2' },
    { id: 'N4', text: '获取读者信息', type: 'ACTION', laneId: 'L3' },
    { id: 'N5', text: '图书超期或欠款', type: 'DECISION', laneId: 'L3' },
    { id: 'N6', text: '禁止借阅', type: 'ACTION', laneId: 'L3' },
    { id: 'N7', text: '借阅成功', type: 'ACTION', laneId: 'L3' },
    { id: 'N8', text: '结束', type: 'END', laneId: 'L3' }
  ],
  edges: [
    { source: 'N1', target: 'N2', label: '' },
    { source: 'N2', target: 'N3', label: '' },
    { source: 'N3', target: 'N4', label: '' },
    { source: 'N4', target: 'N5', label: '' },
    { source: 'N5', target: 'N6', label: '是' },
    { source: 'N5', target: 'N7', label: '否' },
    { source: 'N6', target: 'N8', label: '' },
    { source: 'N7', target: 'N8', label: '' }
  ]
})
let actSeq = 9

function addActLane() {
  actConfig.value.lanes.push({ id: 'L' + (actSeq++), name: '' })
}
function removeActLane(li) {
  const id = actConfig.value.lanes[li].id
  actConfig.value.lanes.splice(li, 1)
  actConfig.value.nodes = actConfig.value.nodes.filter(n => n.laneId !== id)
  actConfig.value.edges = actConfig.value.edges.filter(e => e.source !== id && e.target !== id)
}
function addActNode() {
  actConfig.value.nodes.push({ id: 'N' + (actSeq++), text: '', type: 'ACTION', laneId: actConfig.value.lanes[0]?.id || '' })
}
function removeActNode(ni) {
  const id = actConfig.value.nodes[ni].id
  actConfig.value.nodes.splice(ni, 1)
  actConfig.value.edges = actConfig.value.edges.filter(e => e.source !== id && e.target !== id)
}
function addActEdge() {
  const a = actConfig.value.nodes[0]?.id || ''
  const b = actConfig.value.nodes[1]?.id || ''
  actConfig.value.edges.push({ source: a, target: b, label: '' })
}
function removeActEdge(ei) {
  actConfig.value.edges.splice(ei, 1)
}
const actLanes = computed(() =>
  actConfig.value.lanes.filter(l => l.name && l.name.trim()).map(l => ({ id: l.id, name: l.name.trim() }))
)
const actNodes = computed(() =>
  actConfig.value.nodes.filter(n => n.text && n.text.trim()).map(n => ({ id: n.id, label: n.text.trim() }))
)
function buildActivityPayload() {
  const lanes = actConfig.value.lanes
    .filter(l => l.name && l.name.trim())
    .map(l => ({ id: l.id, name: l.name.trim() }))
  const nodes = actConfig.value.nodes
    .filter(n => n.text && n.text.trim() && n.laneId)
    .map(n => ({ id: n.id, text: n.text.trim(), type: n.type || 'ACTION', laneId: n.laneId }))
  const edges = (actConfig.value.edges || [])
    .filter(e => e.source && e.target && e.source !== e.target)
    .map(e => ({ source: e.source, target: e.target, label: e.label || '' }))
  return { title: actConfig.value.title, lanes, nodes, edges }
}

function addLayer() {
  config.value.layers.push({ name: '', components: [{ name: '' }] })
}
function removeLayer(li) {
  if (config.value.layers.length <= 1) {
    ElMessage.warning('至少保留一层')
    return
  }
  config.value.layers.splice(li, 1)
}
function addComp(li) {
  config.value.layers[li].components.push({ name: '' })
}
function removeComp(li, ci) {
  if (config.value.layers[li].components.length <= 1) {
    ElMessage.warning('至少保留一个组件')
    return
  }
  config.value.layers[li].components.splice(ci, 1)
}

// 泳道图: 泳道与节点管理
function addLane() {
  swimConfig.value.lanes.push({ name: '', nodes: [{ id: 'N' + (nodeSeq++), name: '', type: 'task' }] })
}
function removeLane(li) {
  if (swimConfig.value.lanes.length <= 1) {
    ElMessage.warning('至少保留一个泳道')
    return
  }
  swimConfig.value.lanes.splice(li, 1)
}
function addNode(li) {
  swimConfig.value.lanes[li].nodes.push({ id: 'N' + (nodeSeq++), name: '', type: 'task' })
}
function removeNode(li, ni) {
  if (swimConfig.value.lanes[li].nodes.length <= 1) {
    ElMessage.warning('至少保留一个节点')
    return
  }
  const id = swimConfig.value.lanes[li].nodes[ni].id
  swimConfig.value.lanes[li].nodes.splice(ni, 1)
  // 清理引用该节点的连线
  swimConfig.value.edges = swimConfig.value.edges.filter(e => e.source !== id && e.target !== id)
}
function addEdge() {
  swimConfig.value.edges.push({ source: '', target: '', label: '' })
}
function removeEdge(ei) {
  swimConfig.value.edges.splice(ei, 1)
}
// 全部节点(供连线下拉)
const allNodes = computed(() => {
  const list = []
  swimConfig.value.lanes.forEach((lane, li) => {
    ;(lane.nodes || []).forEach((nd, ni) => {
      list.push({ id: nd.id, key: li + '-' + ni, label: nd.name || ('节点 ' + nd.id) })
    })
  })
  return list
})
// 泳道配置 -> 后端 SwimlaneConfig (生成唯一 id + 构建 lanes/nodes/edges)
function buildSwimlanePayload() {
  const lanes = []
  const nodes = []
  swimConfig.value.lanes.forEach((lane, li) => {
    const laneId = 'L' + (li + 1)
    if (!lane.name || !lane.name.trim()) return
    lanes.push({ id: laneId, name: lane.name.trim() })
    ;(lane.nodes || []).forEach(nd => {
      if (!nd.name || !nd.name.trim()) return
      nodes.push({ id: nd.id, laneId, name: nd.name.trim(), type: nd.type || 'task' })
    })
  })
  const edges = (swimConfig.value.edges || [])
    .filter(e => e.source && e.target && e.source !== e.target)
    .map(e => ({ source: e.source, target: e.target, label: e.label || '' }))
  return { flowName: swimConfig.value.flowName, lanes, nodes, edges }
}

const examples = [
  { typeText: '流程', type: 'FLOW', text: '查询会员余额\nif(余额 >= 商品金额)\n    扣除余额\n    保存订单\nelse\n    返回余额不足' },
  { typeText: '流程', type: 'FLOW', text: '用户登录\nif(账号存在)\n    验证密码\n    if(密码正确)\n        登录成功\n    else\n        提示密码错误\nelse\n    提示账号不存在' }
]

const archLayers = computed(() => {
  if (!currentVO.value || currentVO.value.type !== 'ARCH') return []
  // 按用户配置的层顺序分组
  const groups = {}
  ;(currentVO.value.nodes || []).forEach(n => {
    if (n.lane) {
      if (!groups[n.lane]) groups[n.lane] = []
      groups[n.lane].push(n)
    }
  })
  // 按 config.layers 顺序输出, 未配置的层不显示
  const order = config.value.layers.map(l => l.name).filter(Boolean)
  const result = []
  order.forEach(name => {
    if (groups[name]) result.push({ name, nodes: groups[name] })
  })
  // 兼容后端返回但前端未列出的层
  Object.keys(groups).forEach(name => {
    if (!order.includes(name)) result.push({ name, nodes: groups[name] })
  })
  return result
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (graph) graph.dispose()
})

function handleResize() {
  if (graph && container.value) {
    graph.resize(container.value.clientWidth, container.value.clientHeight)
  }
}

function onTypeChange() {
  // 切类型后清空, 重新生成
  description.value = ''
  graphReady.value = false
  currentVO.value = null
}

function useExample(ex) {
  type.value = ex.type
  if (ex.type === 'SWIMLANE') {
    description.value = ''
  } else {
    description.value = ex.text
  }
}

function nodeShapeName(shape, type) {
  if (shape === 'start' && type === 'ACTIVITY') return 'activityStart'
  if (shape === 'end' && type === 'ACTIVITY') return 'activityEnd'
  if (shape === 'start' || shape === 'end') return 'ellipse'
  if (shape === 'condition') return 'polygon'
  if (shape === 'database' || shape === 'cache' || shape === 'mq') return 'db'
  if (shape === 'actor' && type === 'ARCH') return 'rect'
  if (shape === 'actor') return 'actorNode'
  if (shape === 'usecase') return 'ellipse'
  if (shape === 'system') return 'systemNode'
  if (shape === 'classNode') return 'classNode'
  return 'rect'
}

function nodeAttrs(node) {
  const color = '#333333'
  const label = { text: node.label, fill: '#333', fontSize: 12, textAnchor: 'middle', textVerticalAnchor: 'middle' }
  if (node.shape === 'activityStart') {
    return { body: { fill: '#000', stroke: '#000', strokeWidth: 1.5 }, label: { text: '' } }
  }
  if (node.shape === 'activityEnd') {
    return {
      outer: { cx: 17, cy: 17, r: 13, fill: '#fff', stroke: '#000', strokeWidth: 2 },
      inner: { cx: 17, cy: 17, r: 6, fill: '#000' },
      label: { text: '' }
    }
  }
  if (node.shape === 'start' || node.shape === 'end') {
    return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5 }, label }
  }
  if (node.shape === 'condition') {
    return {
      body: { refPoints: '0,10 10,0 20,10 10,20', fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  if (node.shape === 'usecase') {
    return {
      body: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  if (node.shape === 'system') {
    return {
      body: { fill: 'rgba(255,255,255,0.4)', stroke: color, strokeWidth: 2, rx: 4, ry: 4 },
      title: { text: node.label },
      label: { text: '' }
    }
  }
  if (node.shape === 'actor') {
    return {
      img: { 'xlink:href': '/xiaoren.svg' },
      label: { text: node.label }
    }
  }
  if (node.shape === 'participant') {
    return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5, rx: 6, ry: 6 }, label }
  }
  if (node.shape === 'activation') {
    return { body: { fill: '#00aa66', stroke: '#008b55', strokeWidth: 1 }, label }
  }
  if (node.shape === 'service' || node.shape === 'web' || node.shape === 'gateway' || node.shape === 'third' ||
      node.shape === 'search' || node.shape === 'storage') {
    return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5, rx: 8, ry: 8 }, label }
  }
  if (node.shape === 'database' || node.shape === 'cache' || node.shape === 'mq') {
    return {
      body: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      top: { fill: '#fff', stroke: color, strokeWidth: 1.5 },
      label
    }
  }
  // action (圆角矩形)
  return { body: { fill: '#fff', stroke: color, strokeWidth: 1.5, rx: 10, ry: 10 }, label }
}

async function renderGraph(vo) {
  if (vo.type === 'ARCH') return // ARCH 用 HTML 渲染, 不用 X6
  graph.clearCells()

  // SWIMLANE / ACTIVITY: 泳道容器为竖列背景, 节点用后端绝对坐标(已含泳道偏移)
  const nodes = []
  const laneIds = new Map() // lane.id -> 容器id
  if ((vo.type === 'SWIMLANE' || vo.type === 'ACTIVITY') && vo.lanes && vo.lanes.length > 0) {
    vo.lanes.forEach(l => {
      const id = 'lane_' + l.id
      laneIds.set(l.id, id)
      const isActivity = vo.type === 'ACTIVITY'
      nodes.push({
        id,
        shape: 'rect',
        x: l.x,
        y: l.y,
        width: l.width,
        height: l.height,
        zIndex: 0,
        attrs: {
          body: isActivity
            ? { fill: '#f7f9fc', stroke: '#909399', strokeWidth: 1.5, rx: 4, ry: 4 }
            : { fill: '#f0f2f7', stroke: '#c0c4cc', strokeWidth: 1.5, strokeDasharray: '6 3', rx: 8, ry: 8 },
          label: { text: l.name, fill: '#3B6BFF', fontSize: 14, fontWeight: 700,
                   textAnchor: isActivity ? 'middle' : 'start',
                   textVerticalAnchor: 'top',
                   refX: isActivity ? 0.5 : 12, refY: 8 }
        }
      })
    })
  }
  // SEQUENCE: 生命线(蓝色竖线)
  if (vo.type === 'SEQUENCE' && vo.lanes && vo.lanes.length > 0) {
    vo.lanes.forEach(l => {
      const id = 'line_' + l.id
      nodes.push({
        id,
        shape: 'rect',
        x: l.x - 1,
        y: l.y,
        width: 2,
        height: l.height,
        zIndex: 0,
        attrs: {
          body: { fill: '#1890ff', stroke: '#1890ff', strokeWidth: 1 }
        }
      })
    })
  }

  vo.nodes.forEach(n => {
    let w, h
    if (vo.type === 'USECASE') {
      w = n.width || (n.shape === 'actor' ? 120 : 170)
      h = n.height || (n.shape === 'actor' ? 120 : (n.shape === 'system' ? 300 : 56))
    } else if (vo.type === 'SEQUENCE') {
      w = n.width || (n.shape === 'activation' ? 12 : 120)
      h = n.height || (n.shape === 'activation' ? 60 : 44)
    } else if (vo.type === 'CLASS') {
      w = n.width || 230
      h = n.height || 120
    } else if (vo.type === 'ACTIVITY') {
      if (n.shape === 'start') { w = 30; h = 30 }
      else if (n.shape === 'end') { w = 34; h = 34 }
      else if (n.shape === 'condition') { w = n.width || 130; h = 72 }
      else { w = n.width || Math.max(120, n.label.length * 14 + 30); h = 48 }
    } else if (vo.type === 'ER') {
      if (n.shape === 'rect') { w = n.width || 150; h = n.height || 60 }
      else if (n.shape === 'ellipse') { w = n.width || 110; h = n.height || 56 }
      else { w = n.width || 120; h = n.height || 72 }
    } else {
      w = Math.max(120, n.label.length * 14 + 30)
      h = (n.shape === 'start' || n.shape === 'end') ? 56 : 48
    }
    const node = {
      id: n.id,
      shape: nodeShapeName(n.shape, vo.type),
      x: n.x,
      y: n.y,
      width: w,
      height: h,
      attrs: nodeAttrs(n),
      zIndex: n.shape === 'system' ? 0 : 10
    }
    // 活动图: 开始=实心黑圆, 结束=双圆叠加
    if (vo.type === 'ER') {
      node.shape = n.shape === 'rect' ? 'rect' : (n.shape === 'rhombus' ? 'polygon' : 'ellipse')
      node.attrs = n.shape === 'rect'
        ? { body: { fill: '#ffffff', stroke: '#000000', strokeWidth: 1.5 }, label: { text: n.label, fill: '#000', fontSize: 12, textAnchor: 'middle', textVerticalAnchor: 'middle' } }
        : n.shape === 'rhombus'
          ? { body: { refPoints: '0,10 10,0 20,10 10,20', fill: '#ffffff', stroke: '#000000', strokeWidth: 1.5 }, label: { text: n.label, fill: '#000', fontSize: 11, textAnchor: 'middle', textVerticalAnchor: 'middle' } }
          : { body: { fill: '#ffffff', stroke: '#000000', strokeWidth: 1.2 }, label: { text: n.label, fill: '#000', fontSize: 10, textAnchor: 'middle', textVerticalAnchor: 'middle' } }
    }
    if (vo.type === 'ACTIVITY' && n.shape === 'start') {
      node.attrs = { body: { fill: '#000', stroke: '#000', strokeWidth: 1.5 }, label: { text: '' } }
    }
    if (vo.type === 'ACTIVITY' && n.shape === 'end') {
      node.attrs = {
        outer: { cx: 17, cy: 17, r: 13, fill: '#fff', stroke: '#000', strokeWidth: 2 },
        inner: { cx: 17, cy: 17, r: 6, fill: '#000' },
        label: { text: '' }
      }
    }
    if (vo.type === 'CLASS' && n.shape === 'classNode') {
      node.data = { name: n.label, attrs: n.attrsText || '', methods: n.methodsText || '' }
      node.attrs = node.attrs || {}
      node.attrs.label = { text: '' }
    }
    nodes.push(node)
  })
  const edges = vo.edges.map(e => {
    const isInclude = e.style === 'include' || e.label === '«include»' || e.label === '«extend»'
    const isReturn = e.style === 'return'
    const edge = {
      id: e.id,
      source: e.source,
      target: e.target,
      router: vo.type === 'SWIMLANE' ? { name: 'manhattan', padding: 12 } : undefined,
      attrs: {
        line: isInclude || isReturn
          ? { stroke: '#333333', strokeWidth: 1.2, strokeDasharray: '6 4', targetMarker: { name: 'block', size: 7 } }
          : { stroke: '#333333', strokeWidth: 1.5, targetMarker: 'block' }
      }
    }
    // ER 图: 关系标签(relationText) + 无箭头
    if (vo.type === 'ER') {
      edge.attrs.line = { stroke: '#000000', strokeWidth: 1.2, targetMarker: null }
      if (e.relationText) {
        edge.labels = [{
          attrs: { label: { text: e.relationText, fill: '#000', fontSize: 12, textAnchor: 'middle', textVerticalAnchor: 'middle' } },
          position: { distance: e.textPosition }
        }]
      }
    }
    // 类图: 关系箭头按类型区分
    if (vo.type === 'CLASS') {
      if (e.style === 'inheritance') {
        edge.attrs.line = { stroke: '#333333', strokeWidth: 1.5, targetMarker: { name: 'block', width: 12, height: 12, fill: '#fff', stroke: '#333' } }
      } else if (e.style === 'composition') {
        edge.attrs.line = { stroke: '#333333', strokeWidth: 1.5, sourceMarker: { name: 'diamond', width: 12, height: 12, fill: '#333' }, targetMarker: null }
      } else if (e.style === 'aggregation') {
        edge.attrs.line = { stroke: '#333333', strokeWidth: 1.5, sourceMarker: { name: 'diamond', width: 12, height: 12, fill: '#fff', stroke: '#333' }, targetMarker: null }
      } else {
        edge.attrs.line = { stroke: '#333333', strokeWidth: 1.5, targetMarker: 'block' }
      }
    }
    // 时序图: 使用坐标定位消息
    if (vo.type === 'SEQUENCE' && e.sourceX != null) {
      edge.source = { x: e.sourceX, y: e.sourceY }
      edge.target = { x: e.targetX, y: e.targetY }
    }
    if (e.label) {
      if (vo.type === 'CLASS' && e.label.includes(' ')) {
        // 基数标签放两端: 左基数靠近源节点, 右基数靠近目标节点
        const parts = e.label.trim().split(/\s+/)
        const labels = []
        if (parts[0]) {
          labels.push({ attrs: { label: { text: parts[0], fill: '#333', fontSize: 11, fontWeight: 700 } }, position: { distance: 20 } })
        }
        if (parts[1]) {
          labels.push({ attrs: { label: { text: parts[1], fill: '#333', fontSize: 11, fontWeight: 700 } }, position: { distance: -20 } })
        }
        edge.labels = labels
      } else {
        edge.labels = [{ attrs: { label: { text: e.label, fill: '#666', fontSize: 11 } } }]
      }
    }
    return edge
  })

  // SWIMLANE / ACTIVITY / USECASE / SEQUENCE / ER: 泳道已定位, 节点用后端坐标, 不需 Dagre
  if (vo.type === 'SWIMLANE' || vo.type === 'ACTIVITY' || vo.type === 'USECASE' || vo.type === 'SEQUENCE' || vo.type === 'ER') {
    graph.fromJSON({ nodes, edges })
    graph.centerContent()
    return
  }

  // FLOW / CLASS: 用 Dagre 布局
  if ((vo.type === 'FLOW' || vo.type === 'CLASS') && nodes.length > 0) {
    try {
      const layout = new DagreLayout({
        type: 'dagre',
        rankdir: 'TB',
        ranksep: vo.type === 'CLASS' ? 280 : 70,
        nodesep: vo.type === 'CLASS' ? 120 : 60
      })
      await layout.execute({
        nodes: nodes.map(n => ({ id: n.id, size: [n.width, n.height] })),
        edges: edges.map(e => ({ source: e.source, target: e.target }))
      })
      layout.forEachNode(node => {
        const n = nodes.find(x => x.id === node.id)
        if (n) {
          n.x = Math.round(node.x - n.width / 2)
          n.y = Math.round(node.y - n.height / 2)
        }
      })
      layout.destroy()
    } catch (e) {
      // Dagre 失败则用后端坐标
    }
  }

  graph.fromJSON({ nodes, edges })
  // CLASS: Dagre 布局后注入类节点 HTML 并居中
  if (vo.type === 'CLASS') {
    // 等待 view 渲染完成
    await nextTick()
    setTimeout(() => {
      vo.nodes.forEach(n => {
        if (n.shape !== 'classNode') return
        const cell = graph.getCellById(n.id)
        if (!cell) return
        const { name, attrs, methods } = { name: n.label, attrs: n.attrsText || '', methods: n.methodsText || '' }
        const esc = s => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        const attrsHtml = attrs
          ? `<div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${esc(attrs)}</div>`
          : '<div style="padding:4px 10px;font-size:12px;color:#999">(无属性)</div>'
        const methodsHtml = methods
          ? `<div style="padding:4px 10px;font-size:12px;color:#333;line-height:1.6;white-space:pre-wrap">${esc(methods)}</div>`
          : '<div style="padding:4px 10px;font-size:12px;color:#999">(无方法)</div>'
        const htmlStr = `<div style="width:100%;height:100%;display:flex;flex-direction:column;box-sizing:border-box;background:#fff;border:1.5px solid #333;overflow:hidden">
          <div style="text-align:center;font-weight:bold;font-size:14px;padding:6px 8px;border-bottom:1.5px solid #333;background:#f5f6fa;color:#333">${esc(name)}</div>
          ${attrsHtml}
          <div style="border-top:1px solid #999"></div>
          ${methodsHtml}
        </div>`
        if (typeof cell.setHTML === 'function') {
          cell.setHTML(htmlStr)
        }
        const view = graph.findViewByCell(cell)
        const container = view ? view.container : null
        if (container) {
          const fo = container.querySelector('foreignObject')
          if (fo) {
            const inner = fo.querySelector('body') || fo
            inner.innerHTML = htmlStr
          }
        }
      })
      graph.centerPoint(0, 0)
    }, 100)
  } else {
    graph.centerContent()
  }
}

async function generate() {
  let payload
  if (type.value === 'ARCH') {
    // 至少配置一个层且一个组件
    const hasAny = config.value.layers.some(l =>
      l.name && (l.components || []).some(c => c.name)
    )
    if (!hasAny) {
      ElMessage.warning('请至少配置一个层和一个组件')
      return
    }
    payload = { type: 'ARCH', config: config.value }
  } else if (type.value === 'SWIMLANE') {
    const swim = buildSwimlanePayload()
    if (swim.lanes.length === 0 || swim.nodes.length === 0) {
      ElMessage.warning('请至少配置一个泳道和一个节点')
      return
    }
    payload = { type: 'SWIMLANE', swimlane: swim }
  } else if (type.value === 'USECASE') {
    const uc = buildUseCasePayload()
    if (uc.usecases.length === 0) {
      ElMessage.warning('请至少配置一个用例')
      return
    }
    payload = { type: 'USECASE', useCase: uc }
  } else if (type.value === 'SEQUENCE') {
    const sq = buildSequencePayload()
    if (sq.participants.length < 2) {
      ElMessage.warning('请至少配置两个参与者')
      return
    }
    if (sq.messages.length === 0) {
      ElMessage.warning('请至少配置一条消息')
      return
    }
    payload = { type: 'SEQUENCE', sequence: sq }
  } else if (type.value === 'CLASS') {
    const cls = buildClassPayload()
    if (cls.classes.length === 0) {
      ElMessage.warning('请至少配置一个类')
      return
    }
    payload = { type: 'CLASS', classConfig: cls }
  } else if (type.value === 'ACTIVITY') {
    const act = buildActivityPayload()
    if (act.lanes.length === 0) {
      ElMessage.warning('请至少配置一个泳道')
      return
    }
    if (act.nodes.length === 0) {
      ElMessage.warning('请至少配置一个节点')
      return
    }
    payload = { type: 'ACTIVITY', activity: act }
  } else if (type.value === 'ER') {
    const ents = erConfig.value.entities.filter(e => e.name && e.name.trim()).map(e => ({
      name: e.name.trim(),
      attrs: (e.attrs || []).filter(a => a.name && a.name.trim()).map(a => ({ name: a.name.trim(), key: !!a.key }))
    }))
    if (ents.length === 0) {
      ElMessage.warning('请至少填写一个实体名称')
      return
    }
    const rels = erConfig.value.relations.filter(r => r.from && r.to && r.from !== r.to).map(r => ({
      from: r.from.trim(), to: r.to.trim(), label: (r.label && r.label.trim()) || '关联', cardinality: r.cardinality || '1:n'
    }))
    generating.value = true
    try {
      const vo = await getErGraph({ fontSize: 12, entities: ents, relations: rels })
      vo.type = 'ER' // 后端 ER 返回无 type, 补上以便渲染/保存
      currentVO.value = vo
      graphReady.value = true
      await nextTick()
      if (!graph) initGraph()
      renderGraph(vo)
    } catch (e) {
      ElMessage.error(e.message || 'ER 图生成失败')
    } finally {
      generating.value = false
    }
    return
  } else if (type.value === 'TABLE3') {
    const hs = t3.value.headers.map(h => h.trim()).filter(Boolean)
    if (hs.length === 0) {
      ElMessage.warning('请填写表头')
      return
    }
    const rows = t3.value.rows.map(r => r.map(c => (c || '').trim()))
    generating.value = true
    try {
      const blob = await generateTable3({ title: t3.value.title.trim(), headers: hs, rows })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = (t3.value.title.trim() || '三线表') + '.docx'
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('三线表已生成并下载')
    } catch (e) {
      ElMessage.error(e.message || '生成失败')
    } finally {
      generating.value = false
    }
    return
  } else {
    if (!description.value.trim()) {
      ElMessage.warning('请输入系统描述')
      return
    }
    payload = { type: type.value, description: description.value }
  }
  generating.value = true
  try {
    const vo = await generateDiagram(payload)
    currentVO.value = vo
    graphReady.value = true
    await nextTick()
    if (vo.type !== 'ARCH') {
      if (!graph) initGraph()
      renderGraph(vo)
    }
  } catch (e) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = false
  }
}

function initGraph() {
  graph = new Graph({
    container: container.value,
    width: container.value.clientWidth || 800,
    height: container.value.clientHeight || 600,
    background: { color: '#fafbfc' },
    grid: { size: 10, visible: true },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    autoResize: true
  })
}

async function save() {
  if (!currentVO.value) return
  try {
    // 用图真实名称(后端已带业务名/标题), 无则退回类型名, 便于保存列表区分
    const name = (currentVO.value && currentVO.value.name) || type.value
    const data = { ...currentVO.value, name }
    const saved = await saveDiagram(data)
    currentVO.value = saved
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

// 内联图片资源缓存: 导出时把相对路径图片(如 /xiaoren.svg)替换为 data URL
const svgImageCache = {}
async function inlineSvgImages(svgClone) {
  const images = svgClone.querySelectorAll('image')
  for (const img of images) {
    const href = img.getAttribute('href') || img.getAttribute('xlink:href')
    if (!href) continue
    if (href.startsWith('data:')) continue
    let dataUrl = svgImageCache[href]
    if (!dataUrl) {
      try {
        const resp = await fetch(href)
        const blob = await resp.blob()
        dataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result)
          reader.onerror = reject
          reader.readAsDataURL(blob)
        })
        svgImageCache[href] = dataUrl
      } catch (e) {
        continue
      }
    }
    img.setAttribute('href', dataUrl)
    img.removeAttribute('xlink:href')
  }
}

function getSvgString() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return ''
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  return '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
}

async function downloadSvg() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  await inlineSvgImages(clone)
  const svg = '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
      a.download = (currentVO.value?.name || '系统图') + '.svg'
  a.click()
  URL.revokeObjectURL(url)
}

/** 导出当前图为 Mermaid(打开预览弹窗, 支持复制/下载) */
function exportMermaid() {
  // 优先根据生成的图数据导出(与画布内容一致); 未生成时退回输入配置
  if (type.value !== 'ER' && type.value !== 'TABLE3' && currentVO.value && currentVO.value.nodes && currentVO.value.nodes.length) {
    mmdDlg.value.open(toMermaid('VO', currentVO.value), type.value.toLowerCase() + '.mmd')
    return
  }
  let mmd = ''
  switch (type.value) {
    case 'ARCH': mmd = toMermaid('ARCH', config.value); break
    case 'FLOW': mmd = toMermaid('FLOW', description.value); break
    case 'SWIMLANE': mmd = toMermaid('SWIMLANE', swimConfig.value); break
    case 'ACTIVITY': mmd = toMermaid('ACTIVITY', actConfig.value); break
    case 'USECASE': mmd = toMermaid('USECASE', ucConfig.value); break
    case 'SEQUENCE': mmd = toMermaid('SEQUENCE', seqConfig.value); break
    case 'CLASS': mmd = toMermaid('CLASS', clsConfig.value); break
    case 'ER': {
      const ents = erConfig.value.entities.filter(e => e.name && e.name.trim()).map(e => ({
        name: e.name.trim(),
        attrs: (e.attrs || []).filter(a => a.name && a.name.trim()).map(a => ({ name: a.name.trim(), key: !!a.key }))
      }))
      const rels = erConfig.value.relations.filter(r => r.from && r.to && r.from !== r.to).map(r => ({
        from: r.from.trim(), to: r.to.trim(), label: (r.label && r.label.trim()) || '关联', cardinality: r.cardinality || '1:n'
      }))
      mmd = toMermaid('ER', ents, rels)
      break
    }
    case 'TABLE3': ElMessage.warning('三线表为表格文档，暂不支持 Mermaid 导出'); return
    default: ElMessage.warning('暂不支持导出该类型'); return
  }
  mmdDlg.value.open(mmd, type.value.toLowerCase() + '.mmd')
}

async function downloadPng() {
  const svgNode = container.value.querySelector('svg')
  if (!svgNode) return
  const clone = svgNode.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  await inlineSvgImages(clone)
  const svg = '<?xml version="1.0" encoding="UTF-8"?>\n' + new XMLSerializer().serializeToString(clone)
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const img = new Image()
  img.onload = () => {
    const canvas = document.createElement('canvas')
    const box = svg.match(/viewBox="([^"]+)"/)
    let w = 800, h = 600
    if (box && box[1]) {
      const p = box[1].split(/\s+/).map(Number)
      if (p.length === 4) {
        w = Math.max(200, Math.round(p[2] - p[0]))
        h = Math.max(200, Math.round(p[3] - p[1]))
      }
    }
    const scale = 2
    canvas.width = w * scale
    canvas.height = h * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    URL.revokeObjectURL(url)
    canvas.toBlob(b => {
      if (!b) return
      const a = document.createElement('a')
      a.href = URL.createObjectURL(b)
      a.download = (currentVO.value?.name || '系统图') + '.png'
      a.click()
      URL.revokeObjectURL(a.href)
      ElMessage.success('PNG 已导出')
    }, 'image/png')
  }
  img.onerror = () => {
    URL.revokeObjectURL(url)
    ElMessage.error('PNG 导出失败')
  }
  img.src = url
}

// ARCH 导出: SVG
// ARCH 导出: 用 html2canvas 截图 DOM
async function archCanvas() {
  if (!archRef.value) return null
  const el = archRef.value.querySelector('.arch-outer')
  if (!el) return null
  const canvas = await html2canvas(el, {
    backgroundColor: '#ffffff',
    scale: 2,
    useCORS: true
  })
  return canvas
}

async function downloadArchPng() {
  const canvas = await archCanvas()
  if (!canvas) {
    ElMessage.error('导出失败')
    return
  }
  canvas.toBlob(b => {
    if (!b) return
    const a = document.createElement('a')
    a.href = URL.createObjectURL(b)
    a.download = (currentVO.value?.name || '系统架构图') + '.png'
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('PNG 已导出')
  }, 'image/png')
}

async function downloadArchSvg() {
  const canvas = await archCanvas()
  if (!canvas) {
    ElMessage.error('导出失败')
    return
  }
  const dataUrl = canvas.toDataURL('image/png')
  // 用 PNG 数据包一层 SVG, 保证可打开
  const svgStr = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${canvas.width}" height="${canvas.height}">
  <image href="${dataUrl}" x="0" y="0" width="${canvas.width}" height="${canvas.height}"/>
</svg>`
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = (currentVO.value?.name || '系统架构图') + '.svg'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #2c3e50;
}
.type-tabs {
  flex: 1;
  display: flex;
  justify-content: center;
}
.actions {
  display: flex;
  gap: 8px;
}
.body {
  flex: 1;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 0;
  min-height: 0;
}
.input-panel {
  background: #fff;
  border-right: 1px solid #ebeef5;
  padding: 20px 16px;
  overflow-y: auto;
}
.input-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.section-divider {
  height: 1px;
  background: linear-gradient(to right, #dcdde0, #f0f1f3);
  margin: 16px 0 12px;
}
.item-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 10px 4px;
  margin-bottom: 10px;
  background: #fafbfc;
  position: relative;
}
.item-card .item-num {
  position: absolute;
  top: -7px;
  left: 10px;
  font-size: 11px;
  color: #3B6BFF;
  background: #fff;
  padding: 0 6px;
  border-radius: 8px;
  border: 1px solid #d6e0ff;
}
.dsl-tip {
  font-size: 11px;
  color: #909399;
  line-height: 1.5;
  margin-top: 6px;
  background: #f8f9fb;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 6px 10px;
}
.layer-config {
  margin-top: 4px;
}
.layer-config-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.layer-config-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.layer-name-input {
  flex: 1;
}
.layer-comp {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.comp-type {
  width: 90px;
}
.lane-act {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.act-type {
  width: 86px;
  flex-shrink: 0;
}
.cls-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.cls-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.cls-name-input {
  flex: 1;
}
.cls-sub {
  font-size: 12px;
  color: #909399;
  margin: 6px 0 4px;
  font-weight: 600;
}
.cls-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.vis-sel {
  width: 56px;
  flex-shrink: 0;
}
.add-comp-btn {
  width: 100%;
}
.add-layer-btn {
  width: 100%;
}
.input-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.tip {
  font-size: 11px;
  color: #909399;
  line-height: 1.5;
}
.examples {
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.ex-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.ex-item {
  font-size: 12px;
  color: #606266;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  line-height: 1.5;
  transition: all 0.15s;
}
.ex-item:hover {
  border-color: #3B6BFF;
  color: #3B6BFF;
}
.ex-tag {
  display: inline-block;
  font-size: 11px;
  color: #3B6BFF;
  background: #eef1ff;
  border-radius: 3px;
  padding: 0 5px;
  margin-right: 6px;
}
.canvas-wrap {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.arch-export {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.arch-box {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 20px;
  background: #fafbfc;
}
.arch-outer {
  border: 2px solid #333;
  padding: 0;
  background: #fff;
}
.arch-title {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #333;
  padding: 12px 0;
  border-bottom: 2px solid #333;
}
.arch-divider {
  height: 0;
  border-top: 2px dashed #333;
  margin: 0;
}
.arch-layer {
  display: flex;
  align-items: stretch;
  min-height: 60px;
}
.arch-layer-name {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  background: #f5f5f5;
  border-right: 1px solid #ddd;
  flex-shrink: 0;
  padding: 8px 4px;
}
.arch-layer-body {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}
.arch-comp {
  border: 1.5px solid #333;
  border-radius: 6px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  background: #fff;
}
.x6-container {
  width: 100%;
  height: 100%;
}
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background: #fafbfc;
}
.t3-preview {
  margin-top: 14px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  background: #fafbfc;
}
.t3-caption {
  text-align: center;
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}
.t3-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.t3-table th,
.t3-table td {
  padding: 6px 10px;
  text-align: center;
}
.t3-table thead th {
  border-top: 1.5px solid #000;
  border-bottom: 0.75px solid #000;
  font-weight: 600;
}
.t3-table tbody tr:last-child td {
  border-bottom: 1.5px solid #000;
}
.t3-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
