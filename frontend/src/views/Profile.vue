<template>
  <div class="page">
    <SiteNav />

    <main class="content">
      <div class="profile-card">
        <!-- 顶部档案区 -->
        <section class="profile-header">
          <div class="avatar-block" @click="pickAvatar">
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" alt="头像" />
            <div v-else class="avatar-placeholder">{{ avatarText }}</div>
            <div class="avatar-mask">点击更换</div>
            <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
          </div>
          <div class="meta">
            <div class="name-line">
              <span class="nickname">{{ form.nickname || form.username || '未命名用户' }}</span>
              <span v-if="dirty" class="dirty-tag">有未保存的修改</span>
            </div>
            <div class="sub-line">
              <span class="username">@{{ form.username }}</span>
              <span class="dot">·</span>
              <span class="email-text">{{ form.email || '未绑定邮箱' }}</span>
            </div>
            <p class="bio-line" v-if="form.bio">{{ form.bio }}</p>
            <p class="bio-line muted" v-else>这个人很懒，还没有写简介。</p>
          </div>
        </section>

        <!-- 基本信息区 -->
        <section class="section">
          <div class="section-head">
            <h2 class="section-title">基本信息</h2>
            <span class="section-tip">带 * 的为公开信息，其余仅你可见</span>
          </div>
          <el-form :model="form" label-width="100px" class="info-form">
            <el-form-item label="用户名">
              <el-input :model-value="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称 *">
              <el-input v-model="form.nickname" maxlength="30" placeholder="自定义昵称" show-word-limit />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="200" show-word-limit
                placeholder="一句话介绍自己（最多 200 字）" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="0">不填</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="学校 / 单位 *">
              <el-input v-model="form.school" maxlength="80" placeholder="如：清华大学" show-word-limit />
            </el-form-item>
            <el-form-item label="学院 / 专业 *">
              <el-input v-model="form.major" maxlength="80" placeholder="如：计算机科学与技术" show-word-limit />
            </el-form-item>
            <el-form-item label="常驻城市">
              <el-input v-model="form.city" maxlength="40" placeholder="如：北京" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" :disabled="!dirty" @click="save">保存修改</el-button>
              <el-button :disabled="!dirty" @click="resetDirty">放弃修改</el-button>
            </el-form-item>
          </el-form>
        </section>

        <!-- 账号安全区 -->
        <section class="section">
          <div class="section-head">
            <h2 class="section-title">账号安全</h2>
          </div>
          <ul class="security-list">
            <li class="security-item">
              <div class="security-info">
                <div class="security-label">邮箱</div>
                <div class="security-value">{{ form.email || '未绑定' }}</div>
              </div>
              <el-button plain @click="openChangeEmail">{{ form.email ? '更换邮箱' : '绑定邮箱' }}</el-button>
            </li>
            <li class="security-item">
              <div class="security-info">
                <div class="security-label">密码</div>
                <div class="security-value muted">定期更换密码可提升账号安全</div>
              </div>
              <el-button plain @click="openChangePassword">修改密码</el-button>
            </li>
          </ul>
        </section>

        <el-divider />

        <!-- 危险区 -->
        <section class="danger-zone">
          <h3 class="danger-title">危险操作</h3>
          <p class="danger-desc">注销后你的账号、模板、排版任务及上传的论文文件将被永久删除，且无法恢复。</p>
          <el-button type="danger" plain :loading="deleting" @click="confirmDelete">注销账号</el-button>
        </section>
      </div>
    </main>

    <!-- 更换邮箱弹窗 -->
    <el-dialog v-model="emailDlg" title="更换邮箱" width="460px" destroy-on-close>
      <el-form :model="emailForm" label-width="92px">
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="emailForm.code" maxlength="6" placeholder="6 位验证码" />
            <el-button :disabled="emailCooldown > 0" @click="sendEmailCodeFor('email')">
              {{ emailCooldown > 0 ? emailCooldown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="当前密码">
          <el-input v-model="emailForm.currentPassword" type="password" show-password
            autocomplete="current-password"
            placeholder="需要验证当前密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDlg = false">取消</el-button>
        <el-button type="primary" :loading="emailSaving" @click="submitChangeEmail">确认更换</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwDlg" title="修改密码" width="460px" destroy-on-close>
      <el-form :model="pwForm" label-width="92px">
        <el-form-item label="当前密码">
          <el-input v-model="pwForm.currentPassword" type="password" show-password
            autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwForm.newPassword" type="password" show-password
            autocomplete="new-password"
            placeholder="6~32 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwForm.confirmPassword" type="password" show-password
            autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="code-row">
            <el-input v-model="pwForm.emailCode" maxlength="6" placeholder="发到当前邮箱" />
            <el-button :disabled="pwCooldown > 0 || !form.email" @click="sendEmailCodeFor('password')">
              {{ pwCooldown > 0 ? pwCooldown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
          <div v-if="!form.email" class="form-hint">当前账号未绑定邮箱，无法修改密码</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwDlg = false">取消</el-button>
        <el-button type="primary" :loading="pwSaving" @click="submitChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SiteNav from '../components/SiteNav.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProfile, updateProfile, deleteAccount,
  sendEmailCode, changeEmail, changePassword
} from '../api/user'
import { clearAuth } from '../utils/perm'

const router = useRouter()

// 表单数据(双向)
const form = ref({
  username: '',
  email: '',
  nickname: '',
  avatar: '',
  bio: '',
  gender: 0,
  school: '',
  major: '',
  city: '',
  phone: ''
})

// 上次保存的快照, 用于 dirty 检测与重置
const snapshot = ref('')
const fileInput = ref(null)
const avatarPreview = ref('')
const saving = ref(false)
const deleting = ref(false)

const dirty = computed(() => JSON.stringify({
  ...form.value, avatar: avatarPreview.value
}) !== snapshot.value)

const avatarText = computed(() => (form.value.nickname || form.value.username || 'U').slice(0, 1).toUpperCase())

// 邮箱 / 密码弹窗
const emailDlg = ref(false)
const pwDlg = ref(false)
const emailSaving = ref(false)
const pwSaving = ref(false)
const emailCooldown = ref(0)
const pwCooldown = ref(0)
let emailTimer = null
let pwTimer = null

const emailForm = ref({ newEmail: '', code: '', currentPassword: '' })
const pwForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '', emailCode: '' })

onMounted(async () => {
  try {
    const p = await getProfile()
    hydrate(p)
  } catch (e) { /* 拦截器已提示 */ }
})

function hydrate(p) {
  form.value = {
    username: p.username || '',
    email: p.email || '',
    nickname: p.nickname || '',
    avatar: p.avatar || '',
    bio: p.bio || '',
    gender: p.gender || 0,
    school: p.school || '',
    major: p.major || '',
    city: p.city || '',
    phone: p.phone || ''
  }
  avatarPreview.value = p.avatar || ''
  takeSnapshot()
}

function takeSnapshot() {
  snapshot.value = JSON.stringify({ ...form.value, avatar: avatarPreview.value })
}

function resetDirty() {
  const snap = JSON.parse(snapshot.value)
  form.value = { ...form.value, ...snap }
  avatarPreview.value = snap.avatar
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
  reader.onload = () => { avatarPreview.value = reader.result }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function save() {
  if (!form.value.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (form.value.phone && !/^1[3-9]\d{9}$/.test(form.value.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    const p = await updateProfile({
      nickname: form.value.nickname.trim(),
      avatar: avatarPreview.value,
      bio: form.value.bio.trim(),
      gender: form.value.gender,
      school: form.value.school.trim(),
      major: form.value.major.trim(),
      city: form.value.city.trim(),
      phone: form.value.phone.trim()
    })
    hydrate(p)
    localStorage.setItem('username', p.nickname || p.username || '用户')
    localStorage.setItem('avatar', p.avatar || '')
    ElMessage.success('资料已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  try {
    await ElMessageBox.confirm(
      '确定注销账号吗？此操作会永久删除你的账号、模板、排版任务及全部论文文件，且无法恢复。',
      '注销账号',
      { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch (e) { return }
  deleting.value = true
  try {
    await deleteAccount()
    clearAuth()
    ElMessage.success('账号已注销')
    router.replace('/login')
  } catch (e) {
    ElMessage.error(e.message || '注销失败')
  } finally {
    deleting.value = false
  }
}

// ===== 换绑邮箱 =====
function openChangeEmail() {
  emailForm.value = { newEmail: '', code: '', currentPassword: '' }
  emailDlg.value = true
}

async function submitChangeEmail() {
  const { newEmail, code, currentPassword } = emailForm.value
  if (!newEmail || !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(newEmail)) {
    ElMessage.warning('请输入有效的新邮箱')
    return
  }
  if (!code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!currentPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  emailSaving.value = true
  try {
    await changeEmail({ newEmail, code, currentPassword })
    ElMessage.success('邮箱已更换')
    emailDlg.value = false
    const p = await getProfile()
    hydrate(p)
  } catch (e) {
    ElMessage.error(e.message || '更换失败')
  } finally {
    emailSaving.value = false
  }
}

// ===== 修改密码 =====
function openChangePassword() {
  pwForm.value = { currentPassword: '', newPassword: '', confirmPassword: '', emailCode: '' }
  pwDlg.value = true
}

async function submitChangePassword() {
  const { currentPassword, newPassword, confirmPassword, emailCode } = pwForm.value
  if (!currentPassword) return ElMessage.warning('请输入当前密码')
  if (!newPassword || newPassword.length < 6 || newPassword.length > 32) {
    return ElMessage.warning('新密码长度需在 6~32 位之间')
  }
  if (newPassword !== confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  if (!emailCode) return ElMessage.warning('请输入邮箱验证码')
  pwSaving.value = true
  try {
    await changePassword({ currentPassword, newPassword, emailCode })
    ElMessage.success('密码已修改，请重新登录')
    pwDlg.value = false
    clearAuth()
    router.replace('/login')
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    pwSaving.value = false
  }
}

// ===== 发码 (用现有 sendEmailCode 走 'reset' 场景, 与改密同路) =====
async function sendEmailCodeFor(target) {
  const which = target === 'email' ? emailForm : pwForm
  const email = target === 'email' ? which.value.newEmail : form.value.email
  if (!email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  try {
    await sendEmailCode({ email, scene: 'reset' })
    ElMessage.success('验证码已发送')
    startCooldown(target)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}

function startCooldown(target) {
  if (target === 'email') {
    emailCooldown.value = 60
    emailTimer = setInterval(() => {
      if (--emailCooldown.value <= 0) clearInterval(emailTimer)
    }, 1000)
  } else {
    pwCooldown.value = 60
    pwTimer = setInterval(() => {
      if (--pwCooldown.value <= 0) clearInterval(pwTimer)
    }, 1000)
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F6F4EE;
  padding-bottom: 40px;
}
.content {
  max-width: 720px;
  margin: 32px auto;
  padding: 0 20px;
}
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px 36px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #E3E0D5;
}

/* ===== 档案头 ===== */
.profile-header {
  display: flex;
  gap: 24px;
  padding-bottom: 28px;
  border-bottom: 1px solid #E3E0D5;
  margin-bottom: 8px;
}
.avatar-block {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid #E3E0D5;
  background: #EFEDE4;
  flex-shrink: 0;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 48px; font-weight: 700;
  color: #5C6B60;
  background: linear-gradient(135deg, #E8EFE6, #F6F4EE);
}
.avatar-mask {
  position: absolute; left: 0; right: 0; bottom: 0;
  background: rgba(31, 46, 38, 0.6);
  color: #fff; font-size: 12px; text-align: center;
  padding: 4px 0; opacity: 0; transition: opacity 0.2s;
}
.avatar-block:hover .avatar-mask { opacity: 1; }

.meta { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; }
.name-line {
  display: flex; align-items: center; gap: 10px; margin-bottom: 6px;
}
.nickname {
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 24px; font-weight: 700; color: #1F2E26;
  letter-spacing: 0.02em;
}
.dirty-tag {
  font-size: 11px; font-weight: 500; color: #C1621A;
  background: #FBE9D8; border: 1px solid #EBC8A4;
  padding: 1px 8px; border-radius: 10px;
}
.sub-line {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #5C6B60; margin-bottom: 10px;
}
.sub-line .dot { color: #B3BCB2; }
.sub-line .email-text { color: #24312A; font-weight: 500; }
.bio-line {
  font-size: 14px; line-height: 1.7; color: #24312A;
  margin: 0; padding: 8px 12px;
  background: #F6F4EE; border-left: 3px solid #2F5D46;
  border-radius: 0 4px 4px 0;
}
.bio-line.muted { background: #FAF7F2; border-left-color: #D8CCB8; color: #8B968D; font-style: italic; }

/* ===== 区块 ===== */
.section { padding: 24px 0 8px; }
.section-head {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: 16px;
}
.section-title {
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 18px; font-weight: 700; color: #1F2E26;
  margin: 0; letter-spacing: 0.02em;
}
.section-tip { font-size: 12px; color: #8B968D; }
.info-form { max-width: 560px; }

/* ===== 安全区 ===== */
.security-list { list-style: none; margin: 0; padding: 0; }
.security-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border: 1px solid #E3E0D5; border-radius: 8px;
  margin-bottom: 10px; background: #FAF7F2;
}
.security-info { display: flex; flex-direction: column; gap: 2px; }
.security-label { font-size: 13px; color: #5C6B60; }
.security-value { font-size: 15px; color: #24312A; font-weight: 600; }
.security-value.muted { font-weight: 400; color: #8B968D; font-size: 13px; }

/* ===== 危险区 ===== */
.danger-zone {
  text-align: center;
  padding: 12px 0 0;
}
.danger-title {
  color: #b23a2e; font-size: 15px; margin: 0 0 8px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}
.danger-desc {
  color: #8B968D; font-size: 13px; line-height: 1.7; margin: 0 0 14px;
}

/* ===== 弹窗共用 ===== */
.code-row { display: flex; gap: 8px; width: 100%; }
.code-row :deep(.el-input) { flex: 1; }
.form-hint { font-size: 12px; color: #b23a2e; line-height: 1.4; margin-top: 4px; }

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .profile-card { padding: 24px 20px; }
  .profile-header { flex-direction: column; align-items: center; text-align: center; gap: 16px; }
  .sub-line { justify-content: center; flex-wrap: wrap; }
}
</style>
