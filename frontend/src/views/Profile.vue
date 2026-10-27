<template>
  <WorkbenchShell title="个人资料">
    <template #meta>
      @{{ form.username || '—' }} · {{ form.email || '未绑定邮箱' }}<br />
      带 * 的为公开信息，其余仅你可见
    </template>

    <template #actions>
      <span v-if="dirty" class="dirty-flag">有未保存的修改</span>
      <button class="wbtn ghost" type="button" :disabled="!dirty" @click="resetDirty">放弃修改</button>
      <button class="wbtn solid" type="button" :disabled="!dirty || saving" @click="save">
        {{ saving ? '保存中…' : '保存修改' }}
      </button>
    </template>

    <!-- 档案头 -->
    <section class="ident">
      <button class="ident-avatar" type="button" @click="pickAvatar" :title="avatarPreview ? '点击更换头像' : '上传头像'">
        <img v-if="avatarPreview" :src="avatarPreview" alt="头像" />
        <span v-else>{{ avatarText }}</span>
        <span class="ident-mask">更换</span>
      </button>
      <input ref="fileInput" type="file" accept="image/*" class="hidden-file" @change="onFileChange" />

      <div class="ident-main">
        <h2 class="ident-name">{{ form.nickname || form.username || '未命名用户' }}</h2>
        <p class="ident-sub">
          <span>@{{ form.username }}</span>
          <span class="sep">·</span>
          <span>{{ form.email || '未绑定邮箱' }}</span>
          <span class="sep">·</span>
          <span>{{ form.school || '未填写学校' }}</span>
        </p>
        <p class="ident-bio">{{ form.bio || '这个人很懒，还没有写简介。' }}</p>
      </div>
    </section>

    <!-- 基本信息 -->
    <section class="band">
      <div class="band-head">
        <h3 class="band-title">基本信息</h3>
        <span class="band-note">昵称、学校、学院为公开信息，会显示在模板与团队里</span>
      </div>

      <el-form :model="form" label-position="top" class="form-grid">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称 *">
          <el-input v-model="form.nickname" maxlength="30" placeholder="自定义昵称" show-word-limit />
        </el-form-item>
        <el-form-item label="学校 / 单位 *">
          <el-input v-model="form.school" maxlength="80" placeholder="如：示例大学" show-word-limit />
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
        <el-form-item label="性别" class="span-2">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">不填</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介" class="span-2">
          <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="一句话介绍自己（最多 200 字）" />
        </el-form-item>
      </el-form>
    </section>

    <!-- 账号安全 -->
    <section class="band">
      <div class="band-head">
        <h3 class="band-title">账号安全</h3>
        <span class="band-note">换绑邮箱与改密都需要邮箱验证码</span>
      </div>

      <ul class="sec-list">
        <li>
          <span class="sec-k">邮箱</span>
          <span class="sec-v">{{ form.email || '未绑定' }}</span>
          <button class="wbtn ghost" type="button" @click="openChangeEmail">
            {{ form.email ? '更换邮箱' : '绑定邮箱' }}
          </button>
        </li>
        <li>
          <span class="sec-k">密码</span>
          <span class="sec-v muted">定期更换密码可提升账号安全</span>
          <button class="wbtn ghost" type="button" @click="openChangePassword">修改密码</button>
        </li>
      </ul>
    </section>

    <!-- 危险操作 -->
    <section class="band danger">
      <div class="band-head">
        <h3 class="band-title">注销账号</h3>
        <span class="band-note">注销后账号、格式方案、排版任务与上传的论文文件将被永久删除，且无法恢复</span>
      </div>
      <button class="wbtn ghost danger" type="button" :disabled="deleting" @click="confirmDelete">
        {{ deleting ? '处理中…' : '注销账号' }}
      </button>
    </section>

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
  </WorkbenchShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import WorkbenchShell from '../components/WorkbenchShell.vue'
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

/* 少量仍被复用的基元 */
.muted {
  color: var(--ink-3);
  font-size: 12.5px;
}

 /* ============================================================
   个人资料：档案头 + 两列表单 + 安全行 + 危险区
   ============================================================ */
.hidden-file {
  display: none;
}
.dirty-flag {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--red);
  font-weight: 700;
}

/* 档案头 */
.ident {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  padding: 28px 0 32px;
  border-bottom: 1px solid var(--hair);
}
.ident-avatar {
  position: relative;
  width: 104px;
  height: 104px;
  flex: none;
  border: 1px solid var(--ink);
  background: #fff;
  font-family: inherit;
  font-size: 40px;
  font-weight: 700;
  color: var(--ink);
  cursor: pointer;
  overflow: hidden;
  padding: 0;
}
.ident-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.ident-mask {
  position: absolute;
  inset: auto 0 0 0;
  height: 26px;
  display: grid;
  place-items: center;
  background: var(--ink);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.08em;
  opacity: 0;
  transition: opacity 0.16s ease;
}
.ident-avatar:hover .ident-mask {
  opacity: 1;
}
.ident-main {
  min-width: 0;
}
.ident-name {
  margin: 2px 0 8px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.ident-sub {
  margin: 0 0 10px;
  font-size: 13.5px;
  color: var(--ink-2);
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ident-sub .sep {
  color: var(--hair);
}
.ident-bio {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-2);
  max-width: 640px;
}

/* 区块 */
.band {
  padding: 32px 0;
  border-bottom: 1px solid var(--hair);
}
.band-head {
  display: flex;
  align-items: baseline;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.band-title {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.band-note {
  font-size: 12.5px;
  color: var(--ink-3);
  letter-spacing: 0.03em;
}

/* 表单：两列，标签在上 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 40px;
  row-gap: 4px;
}
.form-grid :deep(.el-form-item) {
  margin-bottom: 18px;
}
.form-grid :deep(.span-2) {
  grid-column: 1 / -1;
}
.form-grid :deep(.el-form-item__label) {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
  padding: 0 0 6px;
  line-height: 1.4;
}
.form-grid :deep(.el-input__wrapper),
.form-grid :deep(.el-textarea__inner) {
  border-radius: 0;
  box-shadow: 0 0 0 1px var(--hair) inset;
  background: #fff;
  padding: 6px 12px;
}
.form-grid :deep(.el-input__wrapper.is-focus),
.form-grid :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--ink) inset;
}
.form-grid :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f2f1ef;
  box-shadow: 0 0 0 1px var(--hair-soft) inset;
}
.form-grid :deep(.el-radio) {
  margin-right: 22px;
}
.form-grid :deep(.el-radio__inner) {
  border-radius: 0;
}

/* 安全行 */
.sec-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--ink);
}
.sec-list li {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  align-items: center;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid var(--hair-soft);
}
.sec-k {
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--ink-3);
}
.sec-v {
  font-size: 15px;
  color: var(--ink);
}
.sec-v.muted {
  color: var(--ink-3);
  font-size: 13.5px;
}

/* 危险区 */
.band.danger {
  border-bottom: 0;
}
.band.danger .band-title {
  color: var(--red);
}
.band.danger .wbtn.ghost.danger {
  border-color: var(--red);
  color: var(--red);
}
.band.danger .wbtn.ghost.danger:hover {
  background: var(--red);
  color: #fff;
}

/* 弹窗里的表单标签与页内一致 */
:deep(.el-dialog .el-form-item__label) {
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--ink-3);
}
:deep(.el-dialog .el-input__wrapper) {
  border-radius: 0;
  box-shadow: 0 0 0 1px var(--hair) inset;
}
:deep(.el-dialog .el-button) {
  border-radius: 0;
}
:deep(.el-dialog__footer .el-button--primary) {
  background: var(--red);
  border-color: var(--red);
  color: #fff;
  font-weight: 700;
}

@media (max-width: 900px) {
  .ident {
    gap: 16px;
    padding: 18px 0 22px;
  }
  .ident-avatar {
    width: 72px;
    height: 72px;
    font-size: 28px;
  }
  .ident-name {
    font-size: 22px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .sec-list li {
    grid-template-columns: 1fr auto;
    row-gap: 6px;
  }
  .sec-k {
    grid-column: 1 / -1;
  }
  .band-title {
    font-size: 17px;
  }
}

</style>
