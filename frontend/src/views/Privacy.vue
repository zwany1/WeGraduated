<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>隐私政策</span>
      </div>
    </header>

    <main class="content">
      <div class="card">
        <h1 class="title">隐私政策</h1>
        <p class="updated">更新日期：2026年8月</p>

        <section v-for="(s, i) in sections" :key="i" class="section">
          <h2>{{ s.title }}</h2>
          <p v-for="(p, j) in s.paragraphs" :key="j" class="para">{{ p }}</p>
          <ul v-if="s.list" class="list">
            <li v-for="(li, k) in s.list" :key="k">{{ li }}</li>
          </ul>
        </section>

        <p class="contact">
          如对本隐私政策有任何疑问，可通过邮箱
          <a href="mailto:2651896126@qq.com">2651896126@qq.com</a> 与我们联系。
        </p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { backOrHome } from '../utils/nav'
const router = useRouter()

function goBack() {
  backOrHome(router)
}

const sections = [
  {
    title: '1. 我们收集哪些信息',
    paragraphs: [
      '为向你提供论文排版、图表生成等服务，我们会收集并处理以下信息：'
    ],
    list: [
      '注册信息：邮箱、用户名（邮箱用于接收验证码，确保账号真实有效）',
      '论文内容：你上传的 Word 论文文档，仅用于排版处理，不会用于其他用途',
      '配置数据：你创建的排版模板、格式规则、图表配置等',
      '基本使用信息：登录时间、排版任务记录等，用于保障服务正常运行'
    ]
  },
  {
    title: '2. 我们如何使用信息',
    paragraphs: [
      '你的数据仅用于：',
      '· 执行论文排版、三线表、ER 图、系统设计图等功能；',
      '· 发送邮箱验证码与排版完成通知；',
      '· 保障账号安全（如登录验证、防恶意操作）；',
      '· 改进服务质量（不包含可识别个人的内容）。'
    ]
  },
  {
    title: '3. 信息的存储与安全',
    paragraphs: [
      '你的论文文件与数据存储在受控的服务器环境中，我们采用访问控制、传输加密等措施保护数据安全。',
      '密码使用 BCrypt 加盐哈希存储，不保存明文；数据库查询使用参数化方式防止注入攻击。'
    ]
  },
  {
    title: '4. 信息共享与披露',
    paragraphs: [
      '我们不会向任何第三方出售、出租或共享你的个人信息，除非：',
      '· 法律法规要求；',
      '· 为提供核心功能所必需（如通过 SMTP 服务商发送验证码邮件）；',
      '· 获得你的明确授权。'
    ]
  },
  {
    title: '5. 你的权利',
    paragraphs: [
      '你可以随时：',
      '· 修改个人资料（昵称、头像）；',
      '· 查看和管理你的排版模板与任务；',
      '· 在「个人资料 → 注销账号」永久删除你的账号及全部数据（包括论文文件）。'
    ]
  },
  {
    title: '6. 未成年人保护',
    paragraphs: [
      '本服务面向完成学业的在校学生及学术研究人员。若你是未成年人，请在监护人的指导下使用本服务。'
    ]
  }
]
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F6F4EE;
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
  color: #24312A;
}
.content {
  max-width: 760px;
  margin: 32px auto;
  padding: 0 20px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.title {
  font-size: 26px;
  font-weight: 700;
  color: #24312A;
  margin: 0 0 4px;
}
.updated {
  color: #8B968D;
  font-size: 13px;
  margin: 0 0 28px;
}
.section {
  margin-bottom: 26px;
}
.section h2 {
  font-size: 17px;
  color: #24312A;
  margin: 0 0 10px;
}
.para {
  color: #5C6B60;
  font-size: 14px;
  line-height: 1.8;
  margin: 0 0 8px;
}
.list {
  margin: 6px 0 0;
  padding-left: 20px;
  color: #5C6B60;
  font-size: 14px;
  line-height: 1.8;
}
.contact {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #E3E0D5;
  color: #8B968D;
  font-size: 14px;
}
.contact a {
  color: #2F5D46;
}
</style>
