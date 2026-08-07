<template>
  <div class="page">
    <header class="bar">
      <div class="brand">论文格式助手</div>
      <div class="actions">
        <el-button @click="$router.push('/')">首页</el-button>
        <el-button @click="$router.push('/tasks')">排版任务</el-button>
        <el-button type="primary" @click="showCreate = true">新建格式方案</el-button>
      </div>
    </header>

    <main class="content">
      <h2>我的格式方案</h2>
      <div v-if="templates.length === 0" class="empty">
        <p>还没有格式方案，点击右上角「新建格式方案」创建</p>
      </div>
      <div class="grid">
        <div v-for="t in templates" :key="t.id" class="tpl-card">
          <h3>{{ t.name }}</h3>
          <p class="time">创建于 {{ formatTime(t.createTime) }}</p>
          <div class="ops">
            <el-button size="small" type="primary" @click="$router.push(`/template/${t.id}`)">配置格式</el-button>
            <el-button size="small" @click="$router.push('/tasks')">使用排版</el-button>
            <el-button size="small" type="danger" plain @click="remove(t.id)">删除</el-button>
          </div>
        </div>
      </div>
    </main>

    <el-dialog v-model="showCreate" title="新建格式方案" width="420px">
      <el-input v-model="newName" placeholder="请输入格式方案名称，如：我的毕业论文格式" />
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTemplates, createTemplate, deleteTemplate } from '../api/template'

const router = useRouter()
const templates = ref([])
const showCreate = ref(false)
const newName = ref('')
const creating = ref(false)

onMounted(load)

async function load() {
  templates.value = await listTemplates()
}

async function create() {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  creating.value = true
  try {
    const t = await createTemplate(newName.value.trim())
    ElMessage.success('创建成功')
    showCreate.value = false
    router.push(`/template/${t.id}`)
  } finally {
    creating.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该格式方案？', '提示', { type: 'warning' })
  await deleteTemplate(id)
  ElMessage.success('已删除')
  load()
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 40px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
}
.content h2 {
  color: #303133;
  margin-bottom: 20px;
}
.empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  background: #fff;
  border-radius: 10px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.tpl-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.tpl-card h3 {
  color: #303133;
  margin-bottom: 8px;
}
.tpl-card .time {
  color: #909399;
  font-size: 12px;
  margin-bottom: 16px;
}
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
