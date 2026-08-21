<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="backOrHome($router)">‹ 返回</el-button>
        <span>三线表生成</span>
      </div>
      <div class="actions">
        <el-button @click="resetExample">重置示例</el-button>
        <el-button type="primary" :loading="generating" @click="generate">
          生成 Word 文档
        </el-button>
      </div>
    </header>

    <main class="content">
      <section class="panel settings">
        <h3>表题设置</h3>
        <el-form label-width="90px" style="max-width: 460px">
          <el-form-item label="表题文字">
            <el-input v-model="title" placeholder="如：实验数据统计" />
          </el-form-item>
          <el-form-item label="自动编号">
            <el-switch v-model="autoNumber" />
            <span class="tip-inline">开启后生成 "表x-x 标题" 前缀</span>
          </el-form-item>
          <template v-if="autoNumber">
            <el-form-item label="章节号">
              <el-input-number v-model="chapterNo" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="表序号">
              <el-input-number v-model="tableNo" :min="1" :max="99" />
            </el-form-item>
          </template>
        </el-form>

        <h3>表格样式</h3>
        <el-form label-width="90px" style="max-width: 460px">
          <el-form-item label="字号">
            <el-select v-model="fontSize" style="width: 180px">
              <el-option label="小五 (9pt)" :value="9" />
              <el-option label="五号 (10pt)" :value="10" />
              <el-option label="小四 (12pt)" :value="12" />
            </el-select>
          </el-form-item>
          <el-form-item label="对齐">
            <el-radio-group v-model="align">
              <el-radio value="center">居中</el-radio>
              <el-radio value="left">左对齐</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <h3>表格数据</h3>
        <div class="sql-box">
          <div class="sql-bar">
            <span class="sql-label">从建表 SQL 导入</span>
            <el-button size="small" type="primary" plain :loading="parsing" @click="importFromSql">解析导入</el-button>
          </div>
          <el-input
            v-model="sqlText"
            type="textarea"
            :rows="5"
            placeholder="粘贴 CREATE TABLE 语句，自动解析列生成三线表。示例：&#10;CREATE TABLE `user` (&#10;  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',&#10;  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '姓名',&#10;  `age` int(11) DEFAULT NULL COMMENT '年龄',&#10;  PRIMARY KEY (`id`)&#10;);"
          />
        </div>
        <div class="cols-bar">
          <span class="cols-label">列数</span>
          <el-input-number v-model="cols" :min="1" :max="10" @change="onColsChange" />
          <el-button size="small" @click="addRow">+ 添加行</el-button>
        </div>
        <div class="editor-wrap">
          <table class="editor-table">
            <thead>
              <tr>
                <th class="th-idx">#</th>
                <th v-for="(h, i) in headers" :key="'h' + i">
                  <el-input v-model="headers[i]" placeholder="列名" size="small" />
                </th>
                <th class="th-del"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in rows" :key="'r' + ri">
                <td class="row-idx">{{ ri + 1 }}</td>
                <td v-for="(c, ci) in row" :key="'c' + ri + '-' + ci">
                  <el-input v-model="rows[ri][ci]" size="small" />
                </td>
                <td class="row-del">
                  <el-button size="small" text type="danger" @click="removeRow(ri)">删除</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel preview">
        <h3>实时预览</h3>
        <div class="preview-caption">{{ captionPreview }}</div>
        <div class="three-table-wrap">
          <table class="three-table">
            <thead>
              <tr>
                <th v-for="(h, i) in headers" :key="'ph' + i">{{ h || '　' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in rows" :key="'pr' + ri">
                <td v-for="(c, ci) in row" :key="'pc' + ri + '-' + ci">{{ c || '　' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="preview-tip">顶线/底线为 1.5pt 粗线，栏目线为 0.75pt 细线，无竖线</div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { generateTable3, parseSql } from '../api/table3'
import { backOrHome } from '../utils/nav'

const title = ref('实验数据统计')
const autoNumber = ref(true)
const chapterNo = ref(3)
const tableNo = ref(1)
const fontSize = ref(10)
const align = ref('center')
const generating = ref(false)
const parsing = ref(false)
const sqlText = ref('')

const headers = ref(['项目', '样本数', '平均值', '标准差'])
const rows = ref([
  ['对照组', '50', '12.3', '2.1'],
  ['实验组', '50', '15.6', '1.8'],
  ['差异显著性', '', 'P < 0.05', '']
])

const cols = ref(headers.value.length)

function onColsChange(n) {
  const cur = headers.value.length
  if (n > cur) {
    for (let i = cur; i < n; i++) headers.value.push('')
    rows.value.forEach(r => { while (r.length < n) r.push('') })
  } else if (n < cur) {
    headers.value.splice(n)
    rows.value.forEach(r => r.splice(n))
  }
}

function addRow() {
  rows.value.push(new Array(headers.value.length).fill(''))
}

function removeRow(i) {
  if (rows.value.length <= 1) {
    ElMessage.warning('至少保留一行数据')
    return
  }
  rows.value.splice(i, 1)
}

async function importFromSql() {
  if (!sqlText.value.trim()) {
    ElMessage.warning('请先粘贴建表 SQL 语句')
    return
  }
  parsing.value = true
  try {
    const info = await parseSql(sqlText.value)
    const cols = info.columns || []
    if (cols.length === 0) {
      ElMessage.warning('未解析到列信息')
      return
    }
    if (info.tableName && !title.value) {
      title.value = info.tableName
    }
    headers.value = ['字段名', '数据类型', '是否为空', '默认值', '说明']
    rows.value = cols.map(c => [
      c.name || '',
      c.type || '',
      c.nullable ? '是' : '否',
      c.defaultValue || '',
      c.comment || ''
    ])
    cols.value = headers.value.length
    ElMessage.success(`解析成功，共 ${cols.length} 个字段`)
  } catch (e) {
    ElMessage.error(e.message || 'SQL 解析失败')
  } finally {
    parsing.value = false
  }
}

function resetExample() {
  title.value = '实验数据统计'
  autoNumber.value = true
  chapterNo.value = 3
  tableNo.value = 1
  fontSize.value = 10
  align.value = 'center'
  headers.value = ['项目', '样本数', '平均值', '标准差']
  rows.value = [
    ['对照组', '50', '12.3', '2.1'],
    ['实验组', '50', '15.6', '1.8'],
    ['差异显著性', '', 'P < 0.05', '']
  ]
  cols.value = headers.value.length
}

const captionPreview = computed(() => {
  const prefix = autoNumber.value ? `表${chapterNo}-${tableNo} ` : ''
  return prefix + title.value
})

async function generate() {
  const hs = headers.value.map(h => h.trim())
  if (!hs.some(h => h)) {
    ElMessage.warning('请至少填写一个列名')
    return
  }
  const dataRows = rows.value
    .map(r => r.map(c => (c || '').trim()))
    .filter(r => r.some(c => c))
  if (dataRows.length === 0) {
    ElMessage.warning('请至少填写一行数据')
    return
  }
  generating.value = true
  try {
    const blob = await generateTable3({
      title: title.value.trim(),
      autoNumber: autoNumber.value,
      chapterNo: chapterNo.value,
      tableNo: tableNo.value,
      headers: hs,
      rows: dataRows,
      fontSize: fontSize.value,
      align: align.value
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '三线表.docx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('三线表已生成并下载')
  } catch (e) {
    ElMessage.error('生成失败，请检查数据')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
}
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 40px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.panel h3 {
  color: #303133;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.panel h3:not(:first-child) {
  margin-top: 22px;
}
.tip-inline {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
.cols-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.sql-box {
  margin-bottom: 14px;
  padding: 12px;
  background: #f8f9fb;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.sql-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.sql-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.cols-label {
  font-size: 13px;
  color: #606266;
}
.editor-wrap {
  overflow-x: auto;
}
.editor-table {
  width: 100%;
  border-collapse: collapse;
}
.editor-table th,
.editor-table td {
  border: 1px solid #ebeef5;
  padding: 4px;
  min-width: 100px;
}
.th-idx,
.row-idx {
  width: 36px;
  text-align: center;
  color: #909399;
  font-size: 12px;
  background: #fafafa;
}
.th-del,
.row-del {
  width: 52px;
  text-align: center;
}
.row-del {
  background: #fafafa;
}
.preview-caption {
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
}
.three-table-wrap {
  overflow-x: auto;
}
.three-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.three-table th {
  border-top: 2px solid #000;
  border-bottom: 1px solid #000;
  padding: 7px 10px;
  font-weight: 700;
  color: #303133;
  background: transparent;
}
.three-table td {
  padding: 7px 10px;
  color: #606266;
}
.three-table tbody tr:last-child td {
  border-bottom: 2px solid #000;
}
.preview-tip {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
@media (max-width: 900px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>
