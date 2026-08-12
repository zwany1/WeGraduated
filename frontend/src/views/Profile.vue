<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>个人资料</span>
      </div>
    </header>

    <main class="content">
      <div class="card">
        <div class="avatar-section">
          <div class="avatar-wrap" @click="pickAvatar">
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" alt="头像" />
            <div v-else class="avatar-placeholder">{{ avatarText }}</div>
            <div class="avatar-mask">点击更换</div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
          <div class="avatar-tip">支持 JPG / PNG，建议 200×200</div>
        </div>

        <el-form label-width="90px" style="max-width: 420px; margin: 0 auto">
          <el-form-item label="用户名">
            <el-input :model-value="username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="nickname" maxlength="30" placeholder="自定义昵称" />
          </el-form-item>
          <el-form-item label="密保问题">
            <el-input v-model="securityQuestion" maxlength="60" placeholder="用于忘记密码时找回，留空则不设置" />
          </el-form-item>
          <el-form-item label="密保答案">
            <el-input v-model="securityAnswer" maxlength="60" :placeholder="hasSecurity ? '填写新答案以修改' : '回答你的密保问题'" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
            <el-button v-if="avatarPreview" @click="removeAvatar">移除头像</el-button>
          </el-form-item>
        </el-form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '../api/user'

const router = useRouter()
const username = ref('')
const nickname = ref('')
const avatar = ref('')
const avatarPreview = ref('')
const securityQuestion = ref('')
const securityAnswer = ref('')
const hasSecurity = ref(false)
const saving = ref(false)
const fileInput = ref(null)

const avatarText = computed(() => (nickname.value || username.value || 'U').slice(0, 1).toUpperCase())

onMounted(async () => {
  try {
    const p = await getProfile()
    username.value = p.username || ''
    nickname.value = p.nickname || ''
    avatar.value = p.avatar || ''
    avatarPreview.value = p.avatar || ''
    securityQuestion.value = p.securityQuestion || ''
    hasSecurity.value = !!(p.securityQuestion)
  } catch (e) {
    // 拦截器已提示
  }
})

function goBack() {
  router.back()
}

function pickAvatar() {
  fileInput.value && fileInput.value.click()
}

function onFileChange(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    avatarPreview.value = reader.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function removeAvatar() {
  avatarPreview.value = ''
}

async function save() {
  if (!nickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (securityQuestion.value.trim() && !securityAnswer.value.trim()) {
    ElMessage.warning('请填写密保答案')
    return
  }
  if (!securityQuestion.value.trim() && securityAnswer.value.trim()) {
    ElMessage.warning('请填写密保问题')
    return
  }
  saving.value = true
  try {
    const p = await updateProfile({
      nickname: nickname.value.trim(),
      avatar: avatarPreview.value,
      securityQuestion: securityQuestion.value.trim(),
      securityAnswer: securityAnswer.value.trim()
    })
    nickname.value = p.nickname
    avatar.value = p.avatar || ''
    avatarPreview.value = p.avatar || ''
    securityQuestion.value = p.securityQuestion || ''
    hasSecurity.value = !!(p.securityQuestion)
    securityAnswer.value = ''
    localStorage.setItem('username', p.nickname || p.username || '用户')
    localStorage.setItem('avatar', p.avatar || '')
    ElMessage.success('资料已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
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
  align-items: center;
  padding: 14px 30px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #2c3e50;
}
.content {
  max-width: 560px;
  margin: 40px auto;
  padding: 0 20px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.avatar-section {
  text-align: center;
  margin-bottom: 24px;
}
.avatar-wrap {
  position: relative;
  width: 110px;
  height: 110px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid #ebeef5;
  background: #f0f2f5;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  font-weight: 700;
  color: #909399;
  background: linear-gradient(135deg, #e8edff, #f5f6fa);
}
.avatar-mask {
  position: absolute;
  inset: auto 0 0 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  text-align: center;
  padding: 4px 0;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}
.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
