<template>
  <div class="page">
    <header class="bar">
      <div class="brand">
        <el-button text @click="goBack">‹ 返回</el-button>
        <span>使用条款</span>
      </div>
    </header>

    <main class="content">
      <div class="card">
        <h1 class="title">使用条款</h1>
        <p class="updated">更新日期：2026年8月</p>

        <section v-for="(s, i) in sections" :key="i" class="section">
          <h2>{{ s.title }}</h2>
          <p v-for="(p, j) in s.paragraphs" :key="j" class="para">{{ p }}</p>
          <ul v-if="s.list" class="list">
            <li v-for="(li, k) in s.list" :key="k">{{ li }}</li>
          </ul>
        </section>

        <p class="contact">
          如对本条款有任何疑问，可通过邮箱
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
    title: '1. 服务说明',
    paragraphs: [
      '本平台为用户提供论文排版、三线表生成、ER 图绘制、系统设计图生成等在线工具服务。使用本服务即表示你同意本条款的全部内容。'
    ]
  },
  {
    title: '2. 账号与安全',
    list: [
      '注册需提供真实有效的邮箱，用于接收验证码与重要通知',
      '你应妥善保管账号密码，对账号下的所有操作负责',
      '不得利用平台从事任何违法违规活动，包括但不限于盗用他人论文、传播违规内容',
      '如发现账号异常，请及时通过邮箱联系我们'
    ]
  },
  {
    title: '3. 内容使用与知识产权',
    list: [
      '你上传的论文与配置数据归你所有，平台仅用于排版处理',
      '平台生成的排版成果与图表可供你在学术范围内自由使用',
      '平台界面、代码、文档等知识产权归项目方所有，未经许可不得用于商业用途',
      '引用本平台相关工具成果时，建议在论文中注明来源'
    ]
  },
  {
    title: '4. 服务限制与免责',
    list: [
      '排版引擎基于规则自动处理，可能无法覆盖所有学校的特殊格式要求，最终格式请以学校规定为准',
      '请在上传前备份你的原始论文，平台不对因使用本服务造成的文档损失负责',
      '平台可能不定期进行维护，维护期间部分功能可能暂时不可用'
    ]
  },
  {
    title: '5. 服务的变更与终止',
    paragraphs: [
      '我们可能根据实际情况调整、暂停或终止部分或全部服务，并会尽量通过合理方式提前通知。条款如有更新，我们会在本页公布最新版本。'
    ]
  },
  {
    title: '6. 适用法律',
    paragraphs: [
      '本条款的解释与适用遵循中华人民共和国相关法律法规。'
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
