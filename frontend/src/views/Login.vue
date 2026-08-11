<template>
  <div class="login-page">
    <!-- 视差星空背景 -->
    <div id="stars"></div>
    <div id="stars2"></div>
    <div id="stars3"></div>

    <div class="form-wrap">
      <form class="form" :class="formClass" @submit.prevent="submit">
        <!-- 邮箱(用户名) -->
        <div class="field email">
          <span class="icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#555" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16v12H4z" /><path d="M4 7l8 6 8-6" /></svg>
          </span>
          <input v-model="form.username" class="input" type="text" placeholder="Username" autocomplete="off" @focus="onFocus" @blur="onBlur" @input="onInput" />
        </div>

        <!-- 密码 -->
        <div class="field password">
          <span class="icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#555" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></svg>
          </span>
          <input v-model="form.password" class="input" :type="showPwd ? 'text' : 'password'" placeholder="Password" @focus="onFocus" @blur="onBlur" @input="onInput" />
          <span class="eye" @mousedown.prevent="showPwd = !showPwd">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#999" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
          </span>
        </div>

        <!-- 记住我 -->
        <label class="remember" @click="rememberMe = !rememberMe">
          <span class="checkbox" :class="{ checked: rememberMe }">
            <svg v-if="rememberMe" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 13l4 4L19 7" /></svg>
          </span>
          记住我
        </label>

        <!-- 提交按钮(3D 立体) -->
        <button class="button" type="submit" :disabled="loading">
          <span class="side-top-bottom"></span>
          <span class="side-left-right"></span>
          {{ loading ? '提交中...' : (tab === 'login' ? 'LOGIN' : 'REGISTER') }}
        </button>

        <small>{{ hint }}</small>
      </form>
    </div>

    <!-- 登录/注册切换 -->
    <div class="tab-switch">
      <span :class="{ active: tab === 'login' }" @click="switchTab('login')">登录</span>
      <span class="divider">|</span>
      <span :class="{ active: tab === 'register' }" @click="switchTab('register')">注册</span>
    </div>
    <div class="back" @click="$router.push('/')">返回首页</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getProfile } from '../api/user'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const showPwd = ref(false)
const rememberMe = ref(false)
const anim = ref('')   // face-up-left / face-up-right / form-complete / form-error
const form = reactive({ username: '', password: '' })

const formClass = computed(() => anim.value)

const hint = computed(() => {
  if (loading.value) return '请稍候...'
  if (tab.value === 'login') return 'Fill in the form to login'
  return 'Create a new account'
})

onMounted(() => {
  // 加载记住的账密
  const savedUser = localStorage.getItem('remembered_user')
  const savedPwd = localStorage.getItem('remembered_pwd')
  if (savedUser) {
    form.username = savedUser
    rememberMe.value = true
    if (savedPwd) {
      try {
        form.password = decodeURIComponent(atob(savedPwd))
      } catch (e) {
        form.password = ''
      }
    }
  }
})

function switchTab(t) {
  tab.value = t
  form.password = ''
  anim.value = ''
}

function onFocus() {
  anim.value = formCompleted() ? 'face-up-right' : 'face-up-left'
}
function onBlur() {
  anim.value = ''
}
function onInput() {
  anim.value = formCompleted() ? 'face-up-right' : 'face-up-left'
}

function formCompleted() {
  return form.username.trim() !== '' && form.password !== ''
}

function saveRemembered() {
  if (rememberMe.value) {
    localStorage.setItem('remembered_user', form.username.trim())
    localStorage.setItem('remembered_pwd', btoa(encodeURIComponent(form.password)))
  } else {
    localStorage.removeItem('remembered_user')
    localStorage.removeItem('remembered_pwd')
  }
}

async function submit() {
  if (loading.value) return
  if (!formCompleted()) {
    anim.value = ''
    ElMessage.warning('请输入用户名和密码')
    // 失败抖动
    setTimeout(() => { anim.value = 'form-error' }, 200)
    setTimeout(() => { anim.value = '' }, 2600)
    return
  }
  loading.value = true
  try {
    const fn = tab.value === 'login' ? login : register
    const data = await fn({ username: form.username.trim(), password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    // 登录后同步资料(昵称/头像), 以数据库为准
    try {
      const p = await getProfile()
      if (p) {
        localStorage.setItem('username', p.nickname || p.username || data.username)
        localStorage.setItem('avatar', p.avatar || '')
      }
    } catch (e) {
      // 资料拉取失败不阻塞登录
    }
    saveRemembered()
    ElMessage.success(tab.value === 'login' ? '登录成功' : '注册成功')
    // 成功翻转动画
    anim.value = 'form-complete'
    setTimeout(() => {
      router.push('/')
    }, 1600)
  } catch (e) {
    anim.value = ''
    setTimeout(() => { anim.value = 'form-error' }, 200)
    setTimeout(() => { anim.value = '' }, 2600)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: radial-gradient(ellipse at bottom, #1B2735 0%, #090A0F 100%);
  overflow: hidden;
  font-family: "Helvetica Neue", Arial, sans-serif;
  z-index: 1;
}
*,
*::before,
*::after {
  box-sizing: border-box;
}

/* ===== 视差星空背景 ===== */
#stars, #stars2, #stars3 {
  position: fixed;
  top: 0;
  left: 0;
  width: 1px;
  height: 1px;
  border-radius: 50%;
}
#stars {
  width: 1px;
  height: 1px;
  box-shadow: 1310px 229px #FFF, 52px 1519px #FFF, 564px 502px #FFF, 458px 286px #FFF, 1509px 210px #FFF, 1386px 1517px #FFF, 1828px 1117px #FFF, 179px 1210px #FFF, 865px 66px #FFF, 62px 192px #FFF, 448px 477px #FFF, 1035px 1233px #FFF, 55px 1150px #FFF, 408px 1467px #FFF, 1331px 1437px #FFF, 1117px 860px #FFF, 452px 920px #FFF, 1207px 570px #FFF, 1658px 1781px #FFF, 14px 1555px #FFF, 1651px 327px #FFF, 1430px 866px #FFF, 697px 570px #FFF, 319px 441px #FFF, 1961px 1564px #FFF, 690px 210px #FFF, 190px 779px #FFF, 199px 736px #FFF, 1736px 705px #FFF, 1237px 542px #FFF, 1653px 89px #FFF, 1495px 941px #FFF, 1099px 256px #FFF, 1993px 1889px #FFF, 776px 162px #FFF, 1131px 601px #FFF, 1699px 1288px #FFF, 1267px 1814px #FFF, 1765px 741px #FFF, 1183px 394px #FFF, 1443px 143px #FFF, 94px 1355px #FFF, 467px 1584px #FFF, 593px 164px #FFF, 1752px 477px #FFF, 1775px 207px #FFF, 779px 570px #FFF, 929px 1302px #FFF, 1709px 748px #FFF, 334px 759px #FFF, 728px 430px #FFF, 1373px 547px #FFF, 1438px 1919px #FFF, 1400px 1328px #FFF, 147px 1248px #FFF, 1301px 351px #FFF, 1094px 1494px #FFF, 502px 335px #FFF, 947px 778px #FFF, 553px 1896px #FFF, 1311px 1410px #FFF, 1141px 450px #FFF, 1403px 665px #FFF, 1727px 1574px #FFF, 1590px 115px #FFF, 470px 1684px #FFF, 66px 1649px #FFF, 647px 822px #FFF, 549px 136px #FFF, 433px 1871px #FFF, 1932px 1162px #FFF, 1795px 1471px #FFF, 645px 436px #FFF, 1343px 1023px #FFF, 811px 1812px #FFF, 1873px 1317px #FFF, 940px 293px #FFF, 543px 286px #FFF, 506px 1526px #FFF, 1150px 1104px #FFF, 539px 1530px #FFF, 1198px 878px #FFF, 1839px 1196px #FFF, 818px 742px #FFF, 450px 284px #FFF, 1044px 1011px #FFF, 187px 1548px #FFF, 97px 1764px #FFF, 225px 314px #FFF, 1286px 328px #FFF, 1623px 1394px #FFF, 865px 1222px #FFF, 131px 789px #FFF, 782px 1221px #FFF, 959px 1084px #FFF, 515px 1989px #FFF, 1134px 1763px #FFF, 1931px 24px #FFF, 1394px 1477px #FFF, 235px 1397px #FFF, 1813px 1100px #FFF, 1538px 547px #FFF, 1575px 1313px #FFF, 697px 229px #FFF, 602px 891px #FFF, 324px 930px #FFF, 7px 1954px #FFF, 1479px 1794px #FFF, 1474px 540px #FFF, 1991px 1026px #FFF, 1561px 366px #FFF, 1040px 1870px #FFF, 218px 1783px #FFF, 1281px 612px #FFF, 1724px 1309px #FFF, 1040px 1248px #FFF, 408px 314px #FFF, 766px 1562px #FFF, 331px 1105px #FFF, 1953px 1595px #FFF, 1890px 1087px #FFF, 1882px 2px #FFF, 1227px 664px #FFF, 1001px 40px #FFF, 230px 1903px #FFF, 744px 1800px #FFF, 1704px 1653px #FFF, 630px 491px #FFF, 119px 494px #FFF, 1799px 1162px #FFF, 1940px 162px #FFF, 176px 1499px #FFF, 996px 1672px #FFF, 142px 1558px #FFF, 1091px 1569px #FFF, 258px 263px #FFF, 1352px 974px #FFF, 1940px 1126px #FFF, 339px 543px #FFF, 1081px 1787px #FFF, 1243px 867px #FFF, 1976px 434px #FFF, 1903px 1105px #FFF, 1547px 1495px #FFF, 1413px 412px #FFF, 1461px 639px #FFF, 818px 1376px #FFF, 1331px 765px #FFF, 898px 1843px #FFF, 1060px 925px #FFF, 248px 508px #FFF, 461px 132px #FFF, 693px 44px #FFF, 1205px 1135px #FFF, 472px 1206px #FFF, 452px 15px #FFF, 146px 1450px #FFF, 1293px 121px #FFF, 469px 139px #FFF, 1855px 65px #FFF, 1761px 677px #FFF, 146px 1053px #FFF, 488px 571px #FFF, 1371px 995px #FFF, 439px 1105px #FFF, 271px 1482px #FFF, 1916px 1808px #FFF, 1170px 1181px #FFF, 969px 498px #FFF, 1607px 969px #FFF, 1654px 834px #FFF, 390px 194px #FFF, 199px 1350px #FFF, 883px 726px #FFF, 868px 842px #FFF, 957px 1770px #FFF, 1494px 111px #FFF, 1380px 1339px #FFF, 1324px 202px #FFF, 125px 825px #FFF, 1492px 695px #FFF, 1640px 1765px #FFF, 224px 510px #FFF, 393px 390px #FFF, 1099px 919px #FFF, 288px 865px #FFF, 376px 571px #FFF, 948px 512px #FFF, 1791px 1891px #FFF, 155px 908px #FFF, 1655px 1765px #FFF, 1754px 1128px #FFF, 201px 104px #FFF, 1336px 1108px #FFF, 1713px 31px #FFF, 1985px 192px #FFF, 1898px 1544px #FFF, 1739px 485px #FFF, 341px 833px #FFF, 995px 986px #FFF, 438px 1771px #FFF, 822px 1849px #FFF, 121px 338px #FFF, 777px 5px #FFF, 800px 544px #FFF, 1898px 1606px #FFF, 1608px 932px #FFF, 585px 867px #FFF, 1427px 1961px #FFF, 1497px 1605px #FFF, 1139px 1356px #FFF, 1472px 997px #FFF, 318px 389px #FFF, 608px 446px #FFF, 1984px 120px #FFF, 1187px 1507px #FFF, 1111px 125px #FFF, 1532px 643px #FFF, 118px 103px #FFF, 1197px 977px #FFF, 1030px 1883px #FFF, 1747px 1088px #FFF, 323px 117px #FFF, 1968px 1041px #FFF, 165px 1744px #FFF, 381px 141px #FFF, 1219px 140px #FFF, 1383px 1766px #FFF, 482px 827px #FFF, 246px 1929px #FFF, 1824px 1167px #FFF, 505px 1186px #FFF, 1218px 82px #FFF, 1269px 168px #FFF, 859px 1347px #FFF, 1196px 1158px #FFF, 1071px 648px #FFF, 1915px 535px #FFF, 419px 1372px #FFF, 1467px 644px #FFF, 489px 544px #FFF, 811px 269px #FFF, 1376px 1322px #FFF, 615px 937px #FFF, 648px 1903px #FFF, 1540px 1917px #FFF, 149px 20px #FFF, 939px 1273px #FFF, 1154px 205px #FFF, 151px 1102px #FFF, 437px 1037px #FFF, 544px 272px #FFF, 1912px 715px #FFF, 1805px 141px #FFF, 1801px 501px #FFF, 757px 584px #FFF, 324px 898px #FFF, 1708px 1113px #FFF, 1441px 620px #FFF, 1253px 1653px #FFF, 1340px 1084px #FFF, 17px 1368px #FFF, 1674px 1136px #FFF, 614px 1909px #FFF, 1359px 213px #FFF, 1923px 1799px #FFF, 276px 542px #FFF, 237px 1823px #FFF, 220px 1521px #FFF, 1134px 319px #FFF, 558px 578px #FFF, 1239px 432px #FFF, 1470px 703px #FFF, 417px 1408px #FFF, 1299px 1747px #FFF, 541px 1036px #FFF, 1001px 515px #FFF, 1855px 1860px #FFF, 1733px 105px #FFF, 190px 1300px #FFF, 868px 1699px #FFF, 567px 91px #FFF, 8px 684px #FFF, 1580px 268px #FFF, 1305px 537px #FFF, 331px 1519px #FFF, 905px 1130px #FFF, 1446px 876px #FFF, 1149px 20px #FFF, 230px 155px #FFF, 1936px 1809px #FFF, 1416px 1852px #FFF, 306px 1118px #FFF, 74px 1710px #FFF, 757px 1193px #FFF, 1132px 304px #FFF, 881px 262px #FFF, 86px 632px #FFF, 747px 1842px #FFF, 1911px 1631px #FFF, 1992px 1763px #FFF, 82px 1841px #FFF, 733px 431px #FFF, 1397px 512px #FFF, 1366px 211px #FFF, 725px 1598px #FFF, 1147px 1811px #FFF, 1792px 833px #FFF, 1995px 1272px #FFF, 1535px 317px #FFF, 1896px 1905px #FFF, 485px 1771px #FFF, 333px 2000px #FFF, 1639px 1661px #FFF, 363px 1806px #FFF, 845px 51px #FFF, 368px 1509px #FFF, 1894px 681px #FFF, 1603px 1907px #FFF, 844px 1643px #FFF, 1372px 1770px #FFF, 1506px 1661px #FFF, 509px 547px #FFF, 327px 1613px #FFF, 1437px 222px #FFF, 784px 1787px #FFF, 80px 1759px #FFF, 964px 456px #FFF, 409px 1673px #FFF, 1881px 943px #FFF, 717px 626px #FFF, 1681px 1629px #FFF, 1785px 467px #FFF, 457px 49px #FFF, 1352px 396px #FFF, 817px 673px #FFF, 571px 1771px #FFF, 143px 1981px #FFF, 1584px 572px #FFF, 720px 1314px #FFF, 1044px 819px #FFF, 1392px 1728px #FFF, 1099px 679px #FFF, 1924px 57px #FFF, 237px 1797px #FFF, 1988px 535px #FFF, 366px 1190px #FFF, 1972px 544px #FFF, 79px 223px #FFF, 1222px 890px #FFF, 708px 1493px #FFF, 1611px 643px #FFF, 894px 1242px #FFF, 1048px 237px #FFF, 789px 1843px #FFF, 1181px 390px #FFF, 522px 91px #FFF, 1452px 894px #FFF, 4px 1065px #FFF, 1896px 1652px #FFF, 1103px 1407px #FFF, 1474px 1926px #FFF, 1520px 1510px #FFF, 1374px 404px #FFF, 746px 884px #FFF, 144px 1944px #FFF, 1361px 1886px #FFF, 677px 1277px #FFF, 643px 1359px #FFF, 1737px 256px #FFF, 1475px 1844px #FFF, 616px 1039px #FFF, 634px 1366px #FFF, 837px 669px #FFF, 825px 1428px #FFF, 606px 1136px #FFF, 261px 393px #FFF, 862px 1362px #FFF, 1926px 777px #FFF, 1388px 1533px #FFF, 1849px 357px #FFF, 1261px 1166px #FFF, 617px 832px #FFF, 1123px 1708px #FFF, 1px 623px #FFF, 588px 431px #FFF, 881px 1610px #FFF, 1188px 1243px #FFF, 1341px 660px #FFF, 953px 905px #FFF, 906px 1384px #FFF, 438px 1047px #FFF, 970px 1626px #FFF, 1846px 1966px #FFF, 1631px 1508px #FFF, 348px 1350px #FFF, 174px 582px #FFF, 1056px 1360px #FFF, 1297px 1269px #FFF, 687px 192px #FFF, 1677px 1950px #FFF, 1539px 482px #FFF, 1378px 636px #FFF, 461px 1652px #FFF, 408px 302px #FFF, 51px 95px #FFF, 502px 974px #FFF, 1252px 1741px #FFF, 1574px 150px #FFF, 933px 849px #FFF, 1815px 1290px #FFF, 1179px 399px #FFF, 1472px 1427px #FFF, 787px 1013px #FFF, 819px 500px #FFF, 303px 1344px #FFF, 1409px 12px #FFF, 1829px 1538px #FFF, 1763px 1578px #FFF, 1814px 219px #FFF, 1595px 871px #FFF, 449px 361px #FFF, 1647px 1962px #FFF, 1426px 1061px #FFF, 952px 103px #FFF, 1142px 511px #FFF, 1879px 1738px #FFF, 249px 935px #FFF, 274px 1642px #FFF, 952px 1368px #FFF, 1088px 1145px #FFF, 1220px 650px #FFF, 1946px 1547px #FFF, 1826px 907px #FFF, 1255px 1670px #FFF, 1474px 1827px #FFF, 1034px 874px #FFF, 1702px 1858px #FFF, 1123px 914px #FFF, 1838px 326px #FFF, 1524px 1765px #FFF, 973px 922px #FFF, 531px 1540px #FFF, 507px 1721px #FFF, 1306px 568px #FFF, 1569px 1593px #FFF, 1068px 993px #FFF, 1284px 490px #FFF, 563px 901px #FFF, 159px 1462px #FFF, 586px 481px #FFF, 557px 688px #FFF, 655px 1829px #FFF, 1107px 166px #FFF, 284px 309px #FFF, 474px 785px #FFF, 1422px 313px #FFF, 1447px 439px #FFF, 132px 850px #FFF, 835px 678px #FFF, 1112px 955px #FFF, 852px 128px #FFF, 424px 1706px #FFF, 861px 798px #FFF, 1854px 1577px #FFF, 1197px 1938px #FFF, 1425px 41px #FFF, 1755px 1804px #FFF, 1568px 1180px #FFF, 780px 977px #FFF, 13px 1931px #FFF, 721px 612px #FFF, 1544px 799px #FFF, 1748px 1827px #FFF, 1953px 1712px #FFF, 859px 1103px #FFF, 1531px 1505px #FFF, 1119px 1639px #FFF, 1236px 1839px #FFF, 452px 1000px #FFF, 450px 559px #FFF, 893px 995px #FFF, 60px 797px #FFF, 689px 1370px #FFF, 1391px 1635px #FFF, 829px 1484px #FFF, 339px 1722px #FFF, 958px 1884px #FFF, 262px 1275px #FFF, 1094px 56px #FFF, 1858px 807px #FFF, 1213px 1156px #FFF, 1358px 56px #FFF, 172px 1317px #FFF, 878px 278px #FFF, 1776px 946px #FFF, 373px 103px #FFF, 533px 777px #FFF, 671px 434px #FFF, 932px 670px #FFF, 692px 1559px #FFF, 1802px 777px #FFF, 570px 1541px #FFF, 1949px 1704px #FFF, 864px 517px #FFF, 1710px 168px #FFF, 964px 40px #FFF, 1535px 1105px #FFF, 107px 1950px #FFF, 717px 460px #FFF, 1332px 141px #FFF, 1600px 1961px #FFF, 1335px 83px #FFF, 1545px 64px #FFF, 1945px 507px #FFF, 409px 1719px #FFF, 42px 1273px #FFF, 313px 489px #FFF, 259px 970px #FFF, 1372px 235px #FFF, 1156px 1942px #FFF, 447px 953px #FFF, 1433px 525px #FFF, 1571px 756px #FFF, 344px 1241px #FFF, 1244px 1974px #FFF, 1532px 1472px #FFF, 235px 1593px #FFF, 1678px 336px #FFF, 1976px 638px #FFF, 222px 1186px #FFF, 53px 1903px #FFF, 639px 1180px #FFF, 1388px 1860px #FFF, 1963px 769px #FFF, 813px 1929px #FFF, 1465px 407px #FFF, 156px 1213px #FFF, 1415px 1702px #FFF, 1285px 498px #FFF, 209px 1428px #FFF, 1583px 618px #FFF, 1742px 1402px #FFF, 1230px 1650px #FFF, 248px 1631px #FFF, 1160px 1603px #FFF, 85px 712px #FFF, 1092px 878px #FFF, 1355px 759px #FFF, 142px 1037px #FFF, 1327px 699px #FFF, 26px 1740px #FFF, 861px 1685px #FFF, 1004px 217px #FFF, 888px 1968px #FFF, 742px 1302px #FFF, 1826px 1698px #FFF, 942px 1449px #FFF, 314px 892px #FFF, 361px 1503px #FFF, 1069px 1978px #FFF, 1333px 554px #FFF, 1262px 1656px #FFF, 1884px 1103px #FFF, 1587px 991px #FFF, 953px 893px #FFF, 1692px 1498px #FFF, 1214px 550px #FFF, 661px 1745px #FFF, 503px 1702px #FFF, 1915px 178px #FFF, 572px 1806px #FFF, 924px 500px #FFF, 1537px 952px #FFF, 1168px 1250px #FFF, 1369px 777px #FFF, 689px 59px #FFF, 1013px 1743px #FFF, 666px 373px #FFF, 999px 435px #FFF, 727px 1634px #FFF, 530px 698px #FFF, 573px 1803px #FFF, 1221px 1437px #FFF, 1803px 566px #FFF, 1139px 21px #FFF, 1059px 1942px #FFF, 392px 176px #FFF, 495px 1475px #FFF, 833px 1001px #FFF, 1138px 1553px #FFF, 493px 1415px #FFF, 976px 1323px #FFF, 1458px 1006px #FFF, 918px 1624px #FFF, 36px 191px #FFF, 603px 454px #FFF, 829px 1417px #FFF, 499px 628px #FFF, 1360px 1192px #FFF, 756px 970px #FFF, 1134px 1088px #FFF, 705px 872px #FFF, 1528px 1128px #FFF, 678px 721px #FFF, 1440px 930px #FFF, 555px 628px #FFF, 515px 473px #FFF, 248px 1478px #FFF, 395px 647px #FFF, 245px 1522px #FFF, 1098px 1948px #FFF, 1561px 1414px #FFF, 380px 393px #FFF, 444px 1513px #FFF, 992px 567px #FFF, 1484px 1208px #FFF, 1557px 1075px #FFF, 1223px 580px #FFF, 206px 1706px #FFF, 398px 607px #FFF, 466px 740px #FFF, 368px 620px #FFF, 29px 1451px #FFF, 1094px 260px #FFF, 562px 94px #FFF, 1996px 112px #FFF, 1134px 599px #FFF, 1429px 1934px #FFF, 259px 1307px #FFF, 1779px 1542px #FFF, 1006px 211px #FFF, 1788px 26px #FFF, 1176px 583px #FFF, 962px 981px #FFF, 903px 698px #FFF, 378px 1978px #FFF, 106px 518px #FFF, 1928px 1765px #FFF, 979px 234px #FFF, 1684px 134px #FFF, 821px 1008px #FFF, 152px 1182px #FFF, 1290px 1406px #FFF, 110px 311px #FFF, 306px 1662px #FFF, 1153px 1944px #FFF, 623px 175px #FFF, 509px 243px #FFF, 1143px 1566px #FFF, 853px 1242px #FFF, 1221px 1620px #FFF, 1267px 463px #FFF, 1589px 1071px #FFF, 780px 923px #FFF, 1861px 907px #FFF, 609px 1762px #FFF, 1206px 879px #FFF, 626px 1165px #FFF, 1272px 124px #FFF, 1249px 1967px #FFF, 1516px 204px #FFF, 1941px 1563px #FFF, 426px 1282px #FFF, 433px 542px #FFF, 1353px 167px #FFF, 322px 492px #FFF, 356px 1131px #FFF, 154px 321px #FFF, 6px 837px #FFF, 923px 1412px #FFF, 1217px 963px #FFF, 597px 67px #FFF, 475px 591px #FFF, 1448px 580px #FFF, 1440px 1761px #FFF, 930px 146px #FFF, 1408px 479px #FFF, 1893px 542px #FFF, 1614px 1621px #FFF, 1281px 1208px #FFF, 1355px 1647px #FFF, 1913px 406px #FFF, 871px 236px #FFF, 1116px 461px #FFF, 1327px 306px #FFF, 1861px 545px #FFF, 1693px 292px #FFF, 147px 123px #FFF, 340px 1624px #FFF, 630px 1219px #FFF, 1534px 1689px #FFF, 1166px 1887px #FFF, 592px 900px #FFF, 255px 960px #FFF, 1411px 623px #FFF, 1434px 825px #FFF, 1931px 558px #FFF, 1025px 1106px #FFF, 1012px 897px #FFF;
  animation: animStar 50s linear infinite;
  z-index: 0;
}
#stars::after {
  content: " ";
  position: absolute;
  top: 2000px;
  width: 1px;
  height: 1px;
  box-shadow: 1310px 229px #FFF, 52px 1519px #FFF, 564px 502px #FFF, 458px 286px #FFF, 1509px 210px #FFF, 1386px 1517px #FFF, 1828px 1117px #FFF, 179px 1210px #FFF, 865px 66px #FFF, 62px 192px #FFF, 448px 477px #FFF, 1035px 1233px #FFF, 55px 1150px #FFF, 408px 1467px #FFF, 1331px 1437px #FFF, 1117px 860px #FFF, 452px 920px #FFF, 1207px 570px #FFF, 1658px 1781px #FFF, 14px 1555px #FFF, 1651px 327px #FFF, 1430px 866px #FFF, 697px 570px #FFF, 319px 441px #FFF, 1961px 1564px #FFF, 690px 210px #FFF, 190px 779px #FFF, 199px 736px #FFF, 1736px 705px #FFF, 1237px 542px #FFF, 1653px 89px #FFF, 1495px 941px #FFF, 1099px 256px #FFF, 1993px 1889px #FFF, 776px 162px #FFF, 1131px 601px #FFF, 1699px 1288px #FFF, 1267px 1814px #FFF, 1765px 741px #FFF, 1183px 394px #FFF, 1443px 143px #FFF, 94px 1355px #FFF, 467px 1584px #FFF, 593px 164px #FFF, 1752px 477px #FFF, 1775px 207px #FFF, 779px 570px #FFF, 929px 1302px #FFF, 1709px 748px #FFF, 334px 759px #FFF, 728px 430px #FFF, 1373px 547px #FFF, 1438px 1919px #FFF, 1400px 1328px #FFF, 147px 1248px #FFF, 1301px 351px #FFF, 1094px 1494px #FFF, 502px 335px #FFF, 947px 778px #FFF, 553px 1896px #FFF, 1311px 1410px #FFF, 1141px 450px #FFF, 1403px 665px #FFF, 1727px 1574px #FFF, 1590px 115px #FFF, 470px 1684px #FFF, 66px 1649px #FFF, 647px 822px #FFF, 549px 136px #FFF, 433px 1871px #FFF, 1932px 1162px #FFF, 1795px 1471px #FFF, 645px 436px #FFF, 1343px 1023px #FFF, 811px 1812px #FFF, 1873px 1317px #FFF, 940px 293px #FFF, 543px 286px #FFF, 506px 1526px #FFF, 1150px 1104px #FFF, 539px 1530px #FFF, 1198px 878px #FFF, 1839px 1196px #FFF, 818px 742px #FFF, 450px 284px #FFF, 1044px 1011px #FFF, 187px 1548px #FFF, 97px 1764px #FFF, 225px 314px #FFF, 1286px 328px #FFF, 1623px 1394px #FFF, 865px 1222px #FFF, 131px 789px #FFF, 782px 1221px #FFF, 959px 1084px #FFF, 515px 1989px #FFF, 1134px 1763px #FFF, 1931px 24px #FFF, 1394px 1477px #FFF, 235px 1397px #FFF, 1813px 1100px #FFF, 1538px 547px #FFF, 1575px 1313px #FFF, 697px 229px #FFF, 602px 891px #FFF, 324px 930px #FFF, 7px 1954px #FFF, 1479px 1794px #FFF, 1474px 540px #FFF, 1991px 1026px #FFF, 1561px 366px #FFF, 1040px 1870px #FFF, 218px 1783px #FFF, 1281px 612px #FFF, 1724px 1309px #FFF, 1040px 1248px #FFF, 408px 314px #FFF, 766px 1562px #FFF, 331px 1105px #FFF, 1953px 1595px #FFF, 1890px 1087px #FFF, 1882px 2px #FFF, 1227px 664px #FFF, 1001px 40px #FFF, 230px 1903px #FFF, 744px 1800px #FFF, 1704px 1653px #FFF, 630px 491px #FFF, 119px 494px #FFF, 1799px 1162px #FFF, 1940px 162px #FFF, 176px 1499px #FFF, 996px 1672px #FFF, 142px 1558px #FFF, 1091px 1569px #FFF, 258px 263px #FFF, 1352px 974px #FFF, 1940px 1126px #FFF, 339px 543px #FFF, 1081px 1787px #FFF, 1243px 867px #FFF, 1976px 434px #FFF, 1903px 1105px #FFF, 1547px 1495px #FFF, 1413px 412px #FFF, 1461px 639px #FFF, 818px 1376px #FFF, 1331px 765px #FFF, 898px 1843px #FFF, 1060px 925px #FFF, 248px 508px #FFF, 461px 132px #FFF, 693px 44px #FFF, 1205px 1135px #FFF, 472px 1206px #FFF, 452px 15px #FFF, 146px 1450px #FFF, 1293px 121px #FFF, 469px 139px #FFF, 1855px 65px #FFF, 1761px 677px #FFF, 146px 1053px #FFF, 488px 571px #FFF, 1371px 995px #FFF, 439px 1105px #FFF, 271px 1482px #FFF, 1916px 1808px #FFF, 1170px 1181px #FFF, 969px 498px #FFF, 1607px 969px #FFF, 1654px 834px #FFF, 390px 194px #FFF, 199px 1350px #FFF, 883px 726px #FFF, 868px 842px #FFF, 957px 1770px #FFF, 1494px 111px #FFF, 1380px 1339px #FFF, 1324px 202px #FFF, 125px 825px #FFF, 1492px 695px #FFF, 1640px 1765px #FFF, 224px 510px #FFF, 393px 390px #FFF, 1099px 919px #FFF, 288px 865px #FFF, 376px 571px #FFF, 948px 512px #FFF, 1791px 1891px #FFF, 155px 908px #FFF, 1655px 1765px #FFF, 1754px 1128px #FFF, 201px 104px #FFF, 1336px 1108px #FFF, 1713px 31px #FFF, 1985px 192px #FFF, 1898px 1544px #FFF, 1739px 485px #FFF, 341px 833px #FFF, 995px 986px #FFF, 438px 1771px #FFF, 822px 1849px #FFF, 121px 338px #FFF, 777px 5px #FFF, 800px 544px #FFF, 1898px 1606px #FFF, 1608px 932px #FFF, 585px 867px #FFF, 1427px 1961px #FFF, 1497px 1605px #FFF, 1139px 1356px #FFF, 1472px 997px #FFF, 318px 389px #FFF, 608px 446px #FFF, 1984px 120px #FFF, 1187px 1507px #FFF, 1111px 125px #FFF, 1532px 643px #FFF, 118px 103px #FFF, 1197px 977px #FFF, 1030px 1883px #FFF, 1747px 1088px #FFF, 323px 117px #FFF, 1968px 1041px #FFF, 165px 1744px #FFF, 381px 141px #FFF, 1219px 140px #FFF, 1383px 1766px #FFF, 482px 827px #FFF, 246px 1929px #FFF, 1824px 1167px #FFF, 505px 1186px #FFF, 1218px 82px #FFF, 1269px 168px #FFF, 859px 1347px #FFF, 1196px 1158px #FFF, 1071px 648px #FFF, 1915px 535px #FFF, 419px 1372px #FFF, 1467px 644px #FFF, 489px 544px #FFF, 811px 269px #FFF, 1376px 1322px #FFF, 615px 937px #FFF, 648px 1903px #FFF, 1540px 1917px #FFF, 149px 20px #FFF, 939px 1273px #FFF, 1154px 205px #FFF, 151px 1102px #FFF, 437px 1037px #FFF, 544px 272px #FFF, 1912px 715px #FFF, 1805px 141px #FFF, 1801px 501px #FFF, 757px 584px #FFF, 324px 898px #FFF, 1708px 1113px #FFF, 1441px 620px #FFF, 1253px 1653px #FFF, 1340px 1084px #FFF, 17px 1368px #FFF, 1674px 1136px #FFF, 614px 1909px #FFF, 1359px 213px #FFF, 1923px 1799px #FFF, 276px 542px #FFF, 237px 1823px #FFF, 220px 1521px #FFF, 1134px 319px #FFF, 558px 578px #FFF, 1239px 432px #FFF, 1470px 703px #FFF, 417px 1408px #FFF, 1299px 1747px #FFF, 541px 1036px #FFF, 1001px 515px #FFF, 1855px 1860px #FFF, 1733px 105px #FFF, 190px 1300px #FFF, 868px 1699px #FFF, 567px 91px #FFF, 8px 684px #FFF, 1580px 268px #FFF, 1305px 537px #FFF, 331px 1519px #FFF, 905px 1130px #FFF, 1446px 876px #FFF, 1149px 20px #FFF, 230px 155px #FFF, 1936px 1809px #FFF, 1416px 1852px #FFF, 306px 1118px #FFF, 74px 1710px #FFF, 757px 1193px #FFF, 1132px 304px #FFF, 881px 262px #FFF, 86px 632px #FFF, 747px 1842px #FFF, 1911px 1631px #FFF, 1992px 1763px #FFF, 82px 1841px #FFF, 733px 431px #FFF, 1397px 512px #FFF, 1366px 211px #FFF, 725px 1598px #FFF, 1147px 1811px #FFF, 1792px 833px #FFF, 1995px 1272px #FFF, 1535px 317px #FFF, 1896px 1905px #FFF, 485px 1771px #FFF, 333px 2000px #FFF, 1639px 1661px #FFF, 363px 1806px #FFF, 845px 51px #FFF, 368px 1509px #FFF, 1894px 681px #FFF, 1603px 1907px #FFF, 844px 1643px #FFF, 1372px 1770px #FFF, 1506px 1661px #FFF, 509px 547px #FFF, 327px 1613px #FFF, 1437px 222px #FFF, 784px 1787px #FFF, 80px 1759px #FFF, 964px 456px #FFF, 409px 1673px #FFF, 1881px 943px #FFF, 717px 626px #FFF, 1681px 1629px #FFF, 1785px 467px #FFF, 457px 49px #FFF, 1352px 396px #FFF, 817px 673px #FFF, 571px 1771px #FFF, 143px 1981px #FFF, 1584px 572px #FFF, 720px 1314px #FFF, 1044px 819px #FFF, 1392px 1728px #FFF, 1099px 679px #FFF, 1924px 57px #FFF, 237px 1797px #FFF, 1988px 535px #FFF, 366px 1190px #FFF, 1972px 544px #FFF, 79px 223px #FFF, 1222px 890px #FFF, 708px 1493px #FFF, 1611px 643px #FFF, 894px 1242px #FFF, 1048px 237px #FFF, 789px 1843px #FFF, 1181px 390px #FFF, 522px 91px #FFF, 1452px 894px #FFF, 4px 1065px #FFF, 1896px 1652px #FFF, 1103px 1407px #FFF, 1474px 1926px #FFF, 1520px 1510px #FFF, 1374px 404px #FFF, 746px 884px #FFF, 144px 1944px #FFF, 1361px 1886px #FFF, 677px 1277px #FFF, 643px 1359px #FFF, 1737px 256px #FFF, 1475px 1844px #FFF, 616px 1039px #FFF, 634px 1366px #FFF, 837px 669px #FFF, 825px 1428px #FFF, 606px 1136px #FFF, 261px 393px #FFF, 862px 1362px #FFF, 1926px 777px #FFF, 1388px 1533px #FFF, 1849px 357px #FFF, 1261px 1166px #FFF, 617px 832px #FFF, 1123px 1708px #FFF, 1px 623px #FFF, 588px 431px #FFF, 881px 1610px #FFF, 1188px 1243px #FFF, 1341px 660px #FFF, 953px 905px #FFF, 906px 1384px #FFF, 438px 1047px #FFF, 970px 1626px #FFF, 1846px 1966px #FFF, 1631px 1508px #FFF, 348px 1350px #FFF, 174px 582px #FFF, 1056px 1360px #FFF, 1297px 1269px #FFF, 687px 192px #FFF, 1677px 1950px #FFF, 1539px 482px #FFF, 1378px 636px #FFF, 461px 1652px #FFF, 408px 302px #FFF, 51px 95px #FFF, 502px 974px #FFF, 1252px 1741px #FFF, 1574px 150px #FFF, 933px 849px #FFF, 1815px 1290px #FFF, 1179px 399px #FFF, 1472px 1427px #FFF, 787px 1013px #FFF, 819px 500px #FFF, 303px 1344px #FFF, 1409px 12px #FFF, 1829px 1538px #FFF, 1763px 1578px #FFF, 1814px 219px #FFF, 1595px 871px #FFF, 449px 361px #FFF, 1647px 1962px #FFF, 1426px 1061px #FFF, 952px 103px #FFF, 1142px 511px #FFF, 1879px 1738px #FFF, 249px 935px #FFF, 274px 1642px #FFF, 952px 1368px #FFF, 1088px 1145px #FFF, 1220px 650px #FFF, 1946px 1547px #FFF, 1826px 907px #FFF, 1255px 1670px #FFF, 1474px 1827px #FFF, 1034px 874px #FFF, 1702px 1858px #FFF, 1123px 914px #FFF, 1838px 326px #FFF, 1524px 1765px #FFF, 973px 922px #FFF, 531px 1540px #FFF, 507px 1721px #FFF, 1306px 568px #FFF, 1569px 1593px #FFF, 1068px 993px #FFF, 1284px 490px #FFF, 563px 901px #FFF, 159px 1462px #FFF, 586px 481px #FFF, 557px 688px #FFF, 655px 1829px #FFF, 1107px 166px #FFF, 284px 309px #FFF, 474px 785px #FFF, 1422px 313px #FFF, 1447px 439px #FFF, 132px 850px #FFF, 835px 678px #FFF, 1112px 955px #FFF, 852px 128px #FFF, 424px 1706px #FFF, 861px 798px #FFF, 1854px 1577px #FFF, 1197px 1938px #FFF, 1425px 41px #FFF, 1755px 1804px #FFF, 1568px 1180px #FFF, 780px 977px #FFF, 13px 1931px #FFF, 721px 612px #FFF, 1544px 799px #FFF, 1748px 1827px #FFF, 1953px 1712px #FFF, 859px 1103px #FFF, 1531px 1505px #FFF, 1119px 1639px #FFF, 1236px 1839px #FFF, 452px 1000px #FFF, 450px 559px #FFF, 893px 995px #FFF, 60px 797px #FFF, 689px 1370px #FFF, 1391px 1635px #FFF, 829px 1484px #FFF, 339px 1722px #FFF, 958px 1884px #FFF, 262px 1275px #FFF, 1094px 56px #FFF, 1858px 807px #FFF, 1213px 1156px #FFF, 1358px 56px #FFF, 172px 1317px #FFF, 878px 278px #FFF, 1776px 946px #FFF, 373px 103px #FFF, 533px 777px #FFF, 671px 434px #FFF, 932px 670px #FFF, 692px 1559px #FFF, 1802px 777px #FFF, 570px 1541px #FFF, 1949px 1704px #FFF, 864px 517px #FFF, 1710px 168px #FFF, 964px 40px #FFF, 1535px 1105px #FFF, 107px 1950px #FFF, 717px 460px #FFF, 1332px 141px #FFF, 1600px 1961px #FFF, 1335px 83px #FFF, 1545px 64px #FFF, 1945px 507px #FFF, 409px 1719px #FFF, 42px 1273px #FFF, 313px 489px #FFF, 259px 970px #FFF, 1372px 235px #FFF, 1156px 1942px #FFF, 447px 953px #FFF, 1433px 525px #FFF, 1571px 756px #FFF, 344px 1241px #FFF, 1244px 1974px #FFF, 1532px 1472px #FFF, 235px 1593px #FFF, 1678px 336px #FFF, 1976px 638px #FFF, 222px 1186px #FFF, 53px 1903px #FFF, 639px 1180px #FFF, 1388px 1860px #FFF, 1963px 769px #FFF, 813px 1929px #FFF, 1465px 407px #FFF, 156px 1213px #FFF, 1415px 1702px #FFF, 1285px 498px #FFF, 209px 1428px #FFF, 1583px 618px #FFF, 1742px 1402px #FFF, 1230px 1650px #FFF, 248px 1631px #FFF, 1160px 1603px #FFF, 85px 712px #FFF, 1092px 878px #FFF, 1355px 759px #FFF, 142px 1037px #FFF, 1327px 699px #FFF, 26px 1740px #FFF, 861px 1685px #FFF, 1004px 217px #FFF, 888px 1968px #FFF, 742px 1302px #FFF, 1826px 1698px #FFF, 942px 1449px #FFF, 314px 892px #FFF, 361px 1503px #FFF, 1069px 1978px #FFF, 1333px 554px #FFF, 1262px 1656px #FFF, 1884px 1103px #FFF, 1587px 991px #FFF, 953px 893px #FFF, 1692px 1498px #FFF, 1214px 550px #FFF, 661px 1745px #FFF, 503px 1702px #FFF, 1915px 178px #FFF, 572px 1806px #FFF, 924px 500px #FFF, 1537px 952px #FFF, 1168px 1250px #FFF, 1369px 777px #FFF, 689px 59px #FFF, 1013px 1743px #FFF, 666px 373px #FFF, 999px 435px #FFF, 727px 1634px #FFF, 530px 698px #FFF, 573px 1803px #FFF, 1221px 1437px #FFF, 1803px 566px #FFF, 1139px 21px #FFF, 1059px 1942px #FFF, 392px 176px #FFF, 495px 1475px #FFF, 833px 1001px #FFF, 1138px 1553px #FFF, 493px 1415px #FFF, 976px 1323px #FFF, 1458px 1006px #FFF, 918px 1624px #FFF, 36px 191px #FFF, 603px 454px #FFF, 829px 1417px #FFF, 499px 628px #FFF, 1360px 1192px #FFF, 756px 970px #FFF, 1134px 1088px #FFF, 705px 872px #FFF, 1528px 1128px #FFF, 678px 721px #FFF, 1440px 930px #FFF, 555px 628px #FFF, 515px 473px #FFF, 248px 1478px #FFF, 395px 647px #FFF, 245px 1522px #FFF, 1098px 1948px #FFF, 1561px 1414px #FFF, 380px 393px #FFF, 444px 1513px #FFF, 992px 567px #FFF, 1484px 1208px #FFF, 1557px 1075px #FFF, 1223px 580px #FFF, 206px 1706px #FFF, 398px 607px #FFF, 466px 740px #FFF, 368px 620px #FFF, 29px 1451px #FFF, 1094px 260px #FFF, 562px 94px #FFF, 1996px 112px #FFF, 1134px 599px #FFF, 1429px 1934px #FFF, 259px 1307px #FFF, 1779px 1542px #FFF, 1006px 211px #FFF, 1788px 26px #FFF, 1176px 583px #FFF, 962px 981px #FFF, 903px 698px #FFF, 378px 1978px #FFF, 106px 518px #FFF, 1928px 1765px #FFF, 979px 234px #FFF, 1684px 134px #FFF, 821px 1008px #FFF, 152px 1182px #FFF, 1290px 1406px #FFF, 110px 311px #FFF, 306px 1662px #FFF, 1153px 1944px #FFF, 623px 175px #FFF, 509px 243px #FFF, 1143px 1566px #FFF, 853px 1242px #FFF, 1221px 1620px #FFF, 1267px 463px #FFF, 1589px 1071px #FFF, 780px 923px #FFF, 1861px 907px #FFF, 609px 1762px #FFF, 1206px 879px #FFF, 626px 1165px #FFF, 1272px 124px #FFF, 1249px 1967px #FFF, 1516px 204px #FFF, 1941px 1563px #FFF, 426px 1282px #FFF, 433px 542px #FFF, 1353px 167px #FFF, 322px 492px #FFF, 356px 1131px #FFF, 154px 321px #FFF, 6px 837px #FFF, 923px 1412px #FFF, 1217px 963px #FFF, 597px 67px #FFF, 475px 591px #FFF, 1448px 580px #FFF, 1440px 1761px #FFF, 930px 146px #FFF, 1408px 479px #FFF, 1893px 542px #FFF, 1614px 1621px #FFF, 1281px 1208px #FFF, 1355px 1647px #FFF, 1913px 406px #FFF, 871px 236px #FFF, 1116px 461px #FFF, 1327px 306px #FFF, 1861px 545px #FFF, 1693px 292px #FFF, 147px 123px #FFF, 340px 1624px #FFF, 630px 1219px #FFF, 1534px 1689px #FFF, 1166px 1887px #FFF, 592px 900px #FFF, 255px 960px #FFF, 1411px 623px #FFF, 1434px 825px #FFF, 1931px 558px #FFF, 1025px 1106px #FFF, 1012px 897px #FFF;
}
#stars2 {
  width: 2px;
  height: 2px;
  box-shadow: 165px 1225px #FFF, 82px 1822px #FFF, 885px 1505px #FFF, 661px 1237px #FFF, 513px 53px #FFF, 188px 469px #FFF, 1970px 1381px #FFF, 1713px 1763px #FFF, 1179px 1203px #FFF, 1948px 43px #FFF, 1567px 1377px #FFF, 1683px 552px #FFF, 1181px 83px #FFF, 1563px 1549px #FFF, 359px 964px #FFF, 1063px 1335px #FFF, 906px 1877px #FFF, 570px 372px #FFF, 1199px 893px #FFF, 1301px 1668px #FFF, 1008px 1985px #FFF, 187px 963px #FFF, 713px 837px #FFF, 683px 658px #FFF, 1373px 215px #FFF, 1757px 330px #FFF, 676px 844px #FFF, 1421px 1015px #FFF, 591px 1357px #FFF, 1936px 821px #FFF, 1667px 1558px #FFF, 1127px 76px #FFF, 932px 181px #FFF, 645px 517px #FFF, 663px 238px #FFF, 1987px 1583px #FFF, 828px 1772px #FFF, 1054px 1690px #FFF, 3px 1347px #FFF, 1781px 1112px #FFF, 947px 847px #FFF, 112px 385px #FFF, 1062px 741px #FFF, 1276px 1550px #FFF, 1021px 1281px #FFF, 906px 1557px #FFF, 106px 417px #FFF, 547px 1125px #FFF, 269px 1900px #FFF, 590px 898px #FFF, 1804px 1431px #FFF, 993px 249px #FFF, 60px 1994px #FFF, 1291px 1247px #FFF, 1637px 491px #FFF, 1454px 325px #FFF, 637px 1129px #FFF, 29px 1132px #FFF, 836px 191px #FFF, 461px 1724px #FFF, 1870px 233px #FFF, 946px 1940px #FFF, 241px 1327px #FFF, 1705px 316px #FFF, 1021px 1911px #FFF, 1468px 598px #FFF, 1043px 1445px #FFF, 560px 851px #FFF, 1710px 989px #FFF, 968px 500px #FFF, 936px 1129px #FFF, 297px 786px #FFF, 391px 1888px #FFF, 1228px 1041px #FFF, 1529px 1801px #FFF, 280px 1770px #FFF, 143px 566px #FFF, 1583px 1618px #FFF, 1750px 850px #FFF, 697px 1914px #FFF, 1614px 1040px #FFF, 548px 1681px #FFF, 6px 580px #FFF, 1487px 612px #FFF, 1716px 1203px #FFF, 1188px 1352px #FFF, 1003px 1773px #FFF, 305px 915px #FFF, 1104px 992px #FFF, 707px 681px #FFF, 1131px 1563px #FFF, 1113px 773px #FFF, 933px 1911px #FFF, 660px 1781px #FFF, 387px 1429px #FFF, 490px 1172px #FFF, 785px 479px #FFF, 1754px 1589px #FFF, 842px 90px #FFF, 652px 1526px #FFF, 969px 1445px #FFF, 1871px 1662px #FFF, 781px 791px #FFF, 1360px 1623px #FFF, 1681px 1336px #FFF, 312px 1015px #FFF, 1976px 76px #FFF, 259px 1029px #FFF, 1978px 1209px #FFF, 680px 1782px #FFF, 206px 1791px #FFF, 1731px 902px #FFF, 205px 1078px #FFF, 1865px 936px #FFF, 32px 1480px #FFF, 296px 840px #FFF, 1784px 1341px #FFF, 1987px 317px #FFF, 154px 962px #FFF, 1601px 1985px #FFF, 543px 694px #FFF, 1277px 1419px #FFF, 815px 1331px #FFF, 165px 1745px #FFF, 673px 1746px #FFF, 1382px 1760px #FFF, 1093px 779px #FFF, 1955px 649px #FFF, 1284px 1472px #FFF, 1818px 1556px #FFF, 1000px 1787px #FFF, 1109px 74px #FFF, 1265px 141px #FFF, 481px 1293px #FFF, 1402px 1887px #FFF, 589px 466px #FFF, 1530px 186px #FFF, 889px 202px #FFF, 1558px 1298px #FFF, 1442px 1788px #FFF, 206px 909px #FFF, 341px 1422px #FFF, 614px 1851px #FFF, 60px 95px #FFF, 665px 1631px #FFF, 115px 601px #FFF, 735px 768px #FFF, 883px 299px #FFF, 501px 1088px #FFF, 844px 1160px #FFF, 1397px 1624px #FFF, 369px 349px #FFF, 359px 162px #FFF, 1249px 1784px #FFF, 784px 1270px #FFF, 1400px 494px #FFF, 1020px 1870px #FFF, 1195px 294px #FFF, 476px 945px #FFF, 1307px 521px #FFF, 942px 523px #FFF, 1366px 20px #FFF, 1841px 1648px #FFF, 953px 1846px #FFF, 590px 1388px #FFF, 1120px 324px #FFF, 152px 905px #FFF, 1936px 708px #FFF, 1204px 613px #FFF, 1309px 1971px #FFF, 869px 1414px #FFF, 513px 936px #FFF, 1732px 619px #FFF, 408px 788px #FFF, 1748px 990px #FFF, 219px 486px #FFF, 782px 1172px #FFF, 736px 1178px #FFF, 606px 1433px #FFF, 605px 45px #FFF, 1700px 1349px #FFF, 811px 563px #FFF, 17px 1160px #FFF, 1772px 1405px #FFF, 1594px 1526px #FFF, 1994px 101px #FFF, 1865px 1242px #FFF, 1527px 1018px #FFF, 1706px 1852px #FFF, 1850px 587px #FFF, 1589px 1637px #FFF, 472px 1244px #FFF, 1643px 722px #FFF, 449px 1304px #FFF, 390px 1272px #FFF, 514px 1389px #FFF, 1548px 1477px #FFF, 1572px 1351px #FFF, 1395px 1717px #FFF, 281px 1287px #FFF;
  animation: animStar 100s linear infinite;
  z-index: 0;
}
#stars2::after {
  content: " ";
  position: absolute;
  top: 2000px;
  width: 2px;
  height: 2px;
  box-shadow: 165px 1225px #FFF, 82px 1822px #FFF, 885px 1505px #FFF, 661px 1237px #FFF, 513px 53px #FFF, 188px 469px #FFF, 1970px 1381px #FFF, 1713px 1763px #FFF, 1179px 1203px #FFF, 1948px 43px #FFF, 1567px 1377px #FFF, 1683px 552px #FFF, 1181px 83px #FFF, 1563px 1549px #FFF, 359px 964px #FFF, 1063px 1335px #FFF, 906px 1877px #FFF, 570px 372px #FFF, 1199px 893px #FFF, 1301px 1668px #FFF, 1008px 1985px #FFF, 187px 963px #FFF, 713px 837px #FFF, 683px 658px #FFF, 1373px 215px #FFF, 1757px 330px #FFF, 676px 844px #FFF, 1421px 1015px #FFF, 591px 1357px #FFF, 1936px 821px #FFF, 1667px 1558px #FFF, 1127px 76px #FFF, 932px 181px #FFF, 645px 517px #FFF, 663px 238px #FFF, 1987px 1583px #FFF, 828px 1772px #FFF, 1054px 1690px #FFF, 3px 1347px #FFF, 1781px 1112px #FFF, 947px 847px #FFF, 112px 385px #FFF, 1062px 741px #FFF, 1276px 1550px #FFF, 1021px 1281px #FFF, 906px 1557px #FFF, 106px 417px #FFF, 547px 1125px #FFF, 269px 1900px #FFF, 590px 898px #FFF, 1804px 1431px #FFF, 993px 249px #FFF, 60px 1994px #FFF, 1291px 1247px #FFF, 1637px 491px #FFF, 1454px 325px #FFF, 637px 1129px #FFF, 29px 1132px #FFF, 836px 191px #FFF, 461px 1724px #FFF, 1870px 233px #FFF, 946px 1940px #FFF, 241px 1327px #FFF, 1705px 316px #FFF, 1021px 1911px #FFF, 1468px 598px #FFF, 1043px 1445px #FFF, 560px 851px #FFF, 1710px 989px #FFF, 968px 500px #FFF, 936px 1129px #FFF, 297px 786px #FFF, 391px 1888px #FFF, 1228px 1041px #FFF, 1529px 1801px #FFF, 280px 1770px #FFF, 143px 566px #FFF, 1583px 1618px #FFF, 1750px 850px #FFF, 697px 1914px #FFF, 1614px 1040px #FFF, 548px 1681px #FFF, 6px 580px #FFF, 1487px 612px #FFF, 1716px 1203px #FFF, 1188px 1352px #FFF, 1003px 1773px #FFF, 305px 915px #FFF, 1104px 992px #FFF, 707px 681px #FFF, 1131px 1563px #FFF, 1113px 773px #FFF, 933px 1911px #FFF, 660px 1781px #FFF, 387px 1429px #FFF, 490px 1172px #FFF, 785px 479px #FFF, 1754px 1589px #FFF, 842px 90px #FFF, 652px 1526px #FFF, 969px 1445px #FFF, 1871px 1662px #FFF, 781px 791px #FFF, 1360px 1623px #FFF, 1681px 1336px #FFF, 312px 1015px #FFF, 1976px 76px #FFF, 259px 1029px #FFF, 1978px 1209px #FFF, 680px 1782px #FFF, 206px 1791px #FFF, 1731px 902px #FFF, 205px 1078px #FFF, 1865px 936px #FFF, 32px 1480px #FFF, 296px 840px #FFF, 1784px 1341px #FFF, 1987px 317px #FFF, 154px 962px #FFF, 1601px 1985px #FFF, 543px 694px #FFF, 1277px 1419px #FFF, 815px 1331px #FFF, 165px 1745px #FFF, 673px 1746px #FFF, 1382px 1760px #FFF, 1093px 779px #FFF, 1955px 649px #FFF, 1284px 1472px #FFF, 1818px 1556px #FFF, 1000px 1787px #FFF, 1109px 74px #FFF, 1265px 141px #FFF, 481px 1293px #FFF, 1402px 1887px #FFF, 589px 466px #FFF, 1530px 186px #FFF, 889px 202px #FFF, 1558px 1298px #FFF, 1442px 1788px #FFF, 206px 909px #FFF, 341px 1422px #FFF, 614px 1851px #FFF, 60px 95px #FFF, 665px 1631px #FFF, 115px 601px #FFF, 735px 768px #FFF, 883px 299px #FFF, 501px 1088px #FFF, 844px 1160px #FFF, 1397px 1624px #FFF, 369px 349px #FFF, 359px 162px #FFF, 1249px 1784px #FFF, 784px 1270px #FFF, 1400px 494px #FFF, 1020px 1870px #FFF, 1195px 294px #FFF, 476px 945px #FFF, 1307px 521px #FFF, 942px 523px #FFF, 1366px 20px #FFF, 1841px 1648px #FFF, 953px 1846px #FFF, 590px 1388px #FFF, 1120px 324px #FFF, 152px 905px #FFF, 1936px 708px #FFF, 1204px 613px #FFF, 1309px 1971px #FFF, 869px 1414px #FFF, 513px 936px #FFF, 1732px 619px #FFF, 408px 788px #FFF, 1748px 990px #FFF, 219px 486px #FFF, 782px 1172px #FFF, 736px 1178px #FFF, 606px 1433px #FFF, 605px 45px #FFF, 1700px 1349px #FFF, 811px 563px #FFF, 17px 1160px #FFF, 1772px 1405px #FFF, 1594px 1526px #FFF, 1994px 101px #FFF, 1865px 1242px #FFF, 1527px 1018px #FFF, 1706px 1852px #FFF, 1850px 587px #FFF, 1589px 1637px #FFF, 472px 1244px #FFF, 1643px 722px #FFF, 449px 1304px #FFF, 390px 1272px #FFF, 514px 1389px #FFF, 1548px 1477px #FFF, 1572px 1351px #FFF, 1395px 1717px #FFF, 281px 1287px #FFF;
}
#stars3 {
  width: 3px;
  height: 3px;
  box-shadow: 199px 1851px #FFF, 1286px 1324px #FFF, 81px 633px #FFF, 1616px 903px #FFF, 69px 1187px #FFF, 748px 1500px #FFF, 270px 185px #FFF, 1863px 605px #FFF, 670px 1531px #FFF, 851px 360px #FFF, 412px 271px #FFF, 1611px 1105px #FFF, 1795px 1990px #FFF, 750px 1088px #FFF, 1028px 1871px #FFF, 559px 1701px #FFF, 337px 527px #FFF, 1872px 1689px #FFF, 1934px 987px #FFF, 1982px 1652px #FFF, 605px 1529px #FFF, 1783px 694px #FFF, 1648px 236px #FFF, 960px 1973px #FFF, 155px 289px #FFF, 1545px 1980px #FFF, 463px 1762px #FFF, 1386px 1484px #FFF, 1382px 814px #FFF, 1981px 1733px #FFF, 1648px 1142px #FFF, 750px 185px #FFF, 1620px 809px #FFF, 29px 542px #FFF, 1099px 254px #FFF, 932px 755px #FFF, 1378px 1534px #FFF, 1377px 537px #FFF, 1198px 781px #FFF, 1685px 1308px #FFF, 1926px 761px #FFF, 222px 1383px #FFF, 479px 966px #FFF, 52px 1269px #FFF, 1811px 1932px #FFF, 1150px 672px #FFF, 1875px 1250px #FFF, 454px 1327px #FFF, 130px 1302px #FFF, 1688px 951px #FFF, 1863px 1436px #FFF, 619px 1330px #FFF, 837px 239px #FFF, 287px 93px #FFF, 1938px 77px #FFF, 624px 1009px #FFF, 238px 200px #FFF, 481px 1818px #FFF, 1101px 278px #FFF, 796px 930px #FFF, 760px 1374px #FFF, 1945px 1522px #FFF, 1427px 1938px #FFF, 1107px 859px #FFF, 1203px 1521px #FFF, 1489px 317px #FFF, 1814px 850px #FFF, 1342px 203px #FFF, 1708px 1003px #FFF, 1261px 836px #FFF, 1924px 1949px #FFF, 573px 68px #FFF, 1414px 759px #FFF, 445px 909px #FFF, 911px 1947px #FFF, 484px 1752px #FFF, 743px 204px #FFF, 1405px 753px #FFF, 1116px 1847px #FFF, 1321px 735px #FFF, 125px 816px #FFF, 566px 389px #FFF, 1983px 251px #FFF, 1944px 1737px #FFF, 1688px 932px #FFF, 188px 1358px #FFF, 435px 1315px #FFF, 1310px 1223px #FFF, 1989px 44px #FFF, 104px 1612px #FFF, 684px 499px #FFF, 258px 1612px #FFF, 1157px 421px #FFF, 141px 1700px #FFF, 1568px 1136px #FFF, 425px 1201px #FFF, 443px 1666px #FFF, 1782px 478px #FFF, 673px 1586px #FFF, 303px 1616px #FFF;
  animation: animStar 150s linear infinite;
  z-index: 0;
}
#stars3::after {
  content: " ";
  position: absolute;
  top: 2000px;
  width: 3px;
  height: 3px;
  box-shadow: 199px 1851px #FFF, 1286px 1324px #FFF, 81px 633px #FFF, 1616px 903px #FFF, 69px 1187px #FFF, 748px 1500px #FFF, 270px 185px #FFF, 1863px 605px #FFF, 670px 1531px #FFF, 851px 360px #FFF, 412px 271px #FFF, 1611px 1105px #FFF, 1795px 1990px #FFF, 750px 1088px #FFF, 1028px 1871px #FFF, 559px 1701px #FFF, 337px 527px #FFF, 1872px 1689px #FFF, 1934px 987px #FFF, 1982px 1652px #FFF, 605px 1529px #FFF, 1783px 694px #FFF, 1648px 236px #FFF, 960px 1973px #FFF, 155px 289px #FFF, 1545px 1980px #FFF, 463px 1762px #FFF, 1386px 1484px #FFF, 1382px 814px #FFF, 1981px 1733px #FFF, 1648px 1142px #FFF, 750px 185px #FFF, 1620px 809px #FFF, 29px 542px #FFF, 1099px 254px #FFF, 932px 755px #FFF, 1378px 1534px #FFF, 1377px 537px #FFF, 1198px 781px #FFF, 1685px 1308px #FFF, 1926px 761px #FFF, 222px 1383px #FFF, 479px 966px #FFF, 52px 1269px #FFF, 1811px 1932px #FFF, 1150px 672px #FFF, 1875px 1250px #FFF, 454px 1327px #FFF, 130px 1302px #FFF, 1688px 951px #FFF, 1863px 1436px #FFF, 619px 1330px #FFF, 837px 239px #FFF, 287px 93px #FFF, 1938px 77px #FFF, 624px 1009px #FFF, 238px 200px #FFF, 481px 1818px #FFF, 1101px 278px #FFF, 796px 930px #FFF, 760px 1374px #FFF, 1945px 1522px #FFF, 1427px 1938px #FFF, 1107px 859px #FFF, 1203px 1521px #FFF, 1489px 317px #FFF, 1814px 850px #FFF, 1342px 203px #FFF, 1708px 1003px #FFF, 1261px 836px #FFF, 1924px 1949px #FFF, 573px 68px #FFF, 1414px 759px #FFF, 445px 909px #FFF, 911px 1947px #FFF, 484px 1752px #FFF, 743px 204px #FFF, 1405px 753px #FFF, 1116px 1847px #FFF, 1321px 735px #FFF, 125px 816px #FFF, 566px 389px #FFF, 1983px 251px #FFF, 1944px 1737px #FFF, 1688px 932px #FFF, 188px 1358px #FFF, 435px 1315px #FFF, 1310px 1223px #FFF, 1989px 44px #FFF, 104px 1612px #FFF, 684px 499px #FFF, 258px 1612px #FFF, 1157px 421px #FFF, 141px 1700px #FFF, 1568px 1136px #FFF, 425px 1201px #FFF, 443px 1666px #FFF, 1782px 478px #FFF, 673px 1586px #FFF, 303px 1616px #FFF;
}
@keyframes animStar {
  from { transform: translateY(0px); }
  to { transform: translateY(-2000px); }
}
.form-wrap,
.tab-switch,
.back {
  position: relative;
  z-index: 2;
}

.form-wrap {
  perspective: 2000px;
  perspective-origin: 50px center;
}

.form {
  position: relative;
  margin: auto;
  width: 400px;
  padding: 20px 30px;
  background: #fff;
  border: 1px solid #dfdfdf;
  transform-style: preserve-3d;
  perspective-origin: 50px center;
  perspective: 2000px;
  transition: transform 1s ease;
}
.form::before,
.form::after {
  content: '';
  position: absolute;
  width: 100%;
  left: 0;
}
.form::before {
  height: 100%;
  top: 0;
  transform: translateZ(-100px);
  background: #333;
  opacity: 0.3;
}
.form::after {
  content: 'SUCCESS!';
  transform: translateY(-50%) translateZ(-101px) scaleX(-1);
  top: 50%;
  color: #fff;
  text-align: center;
  font-weight: bold;
}

.field {
  position: relative;
  background: #cfcfcf;
  transform-style: preserve-3d;
}
.field + .field {
  margin-top: 10px;
}

.icon {
  width: 24px;
  height: 24px;
  position: absolute;
  top: calc(50% - 12px);
  left: 12px;
  transform: translateZ(50px);
  transform-style: preserve-3d;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.input {
  border: 1px solid #dfdfdf;
  background: #fff;
  height: 48px;
  line-height: 48px;
  padding: 0 38px 0 48px;
  width: 100%;
  transform: translateZ(26px);
  font-size: 14px;
}
.input::placeholder {
  color: #aaa;
}
.input:focus,
.input:active {
  outline: none;
  border: 1px solid #e35d5b;
}

.eye {
  position: absolute;
  right: 12px;
  top: calc(50% - 12px);
  transform: translateZ(50px);
  cursor: pointer;
  opacity: 0.7;
  display: flex;
}
.eye:hover {
  opacity: 1;
}

.button {
  display: block;
  width: 100%;
  border: 0;
  text-align: center;
  font-weight: bold;
  color: #fff;
  background: linear-gradient(45deg, #e53935, #e35d5b);
  margin-top: 20px;
  padding: 14px;
  position: relative;
  transform-style: preserve-3d;
  transform: translateZ(26px);
  transition: transform 0.3s ease;
  cursor: pointer;
  font-size: 15px;
  font-family: inherit;
}
.button:hover:not(:disabled) {
  transform: translateZ(13px);
}
.button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.side-top-bottom {
  width: 100%;
}
.side-top-bottom::before,
.side-top-bottom::after {
  content: '';
  width: 100%;
  height: 26px;
  background: linear-gradient(45deg, #d3322e, #d54c4a);
  position: absolute;
  left: 0;
}
.side-top-bottom::before {
  transform-origin: center top;
  transform: translateZ(-26px) rotateX(90deg);
  top: 0;
}
.side-top-bottom::after {
  transform-origin: center bottom;
  transform: translateZ(-26px) rotateX(-90deg);
  bottom: 0;
}
.side-left-right {
  height: 100%;
}
.side-left-right::before,
.side-left-right::after {
  content: '';
  height: 100%;
  width: 26px;
  position: absolute;
  top: 0;
}
.side-left-right::before {
  background: #e53935;
  transform-origin: left center;
  transform: rotateY(90deg);
  left: 0;
}
.side-left-right::after {
  background: #e35d5b;
  transform-origin: right center;
  transform: rotateY(-90deg);
  right: 0;
}

.face-up-left {
  transform: rotateY(-30deg) rotateX(30deg);
}
.face-up-right {
  transform: rotateY(30deg) rotateX(30deg);
}
.face-down-left {
  transform: rotateY(-30deg) rotateX(-30deg);
}
.face-down-right {
  transform: rotateY(30deg) rotateX(-30deg);
}
.form-complete {
  animation: formComplete 1.6s ease;
}
.form-error {
  animation: formError 2.4s ease;
}

@keyframes formComplete {
  50%, 55% {
    transform: rotateX(30deg) rotateY(180deg);
  }
  100% {
    transform: rotateX(0deg) rotateY(1turn);
  }
}
@keyframes formError {
  0%, 100% {
    transform: rotateX(0deg) rotateY(0deg);
  }
  25% {
    transform: rotateX(-25deg);
  }
  33% {
    transform: rotateX(-25deg) rotateY(45deg);
  }
  66% {
    transform: rotateX(-25deg) rotateY(-30deg);
  }
}

small {
  color: #999;
  text-align: center;
  display: block;
  margin-top: 20px;
  backface-visibility: hidden;
}

.remember {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  user-select: none;
  transform: translateZ(26px);
  position: relative;
}
.checkbox {
  width: 15px;
  height: 15px;
  border: 1.5px solid #bbb;
  border-radius: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background: #fff;
}
.checkbox.checked {
  background: #e35d5b;
  border-color: #e35d5b;
}

.tab-switch {
  margin-top: 22px;
  color: #2c3e50;
  font-size: 14px;
  display: flex;
  gap: 12px;
}
.tab-switch span {
  cursor: pointer;
  padding: 2px 4px;
  color: rgba(255, 255, 255, 0.85);
  transition: color 0.2s;
}
.tab-switch span.active {
  font-weight: 700;
  color: #fff;
  text-decoration: underline;
}
.tab-switch .divider {
  cursor: default;
  opacity: 0.6;
}
.back {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
}
.back:hover {
  color: #fff;
}
</style>
