// 生成与后端 JWT_SECRET 匹配的 token(HS256), 并打印插入会话行的 SQL。
//
// 用法:
//   JWT_SECRET=<后端同一个密钥> node e2e/gen-token.mjs > e2e/.token   # 只落 token
//   JWT_SECRET=<...> node e2e/gen-token.mjs --sql                      # 打印可直接执行的 SQL
//
// 为什么必须这么绕: 登录接口的验证码是点选/滑动/旋转三选一随机, 自动化过不去;
// 而 LoginInterceptor 除了校验 JWT 签名, 还要求 t_login_session 里有对应会话行。
import crypto from 'crypto';

const SECRET = process.env.JWT_SECRET;
const USER_ID = process.env.E2E_USER_ID || '1';
const USERNAME = process.env.E2E_USERNAME || 'testuser';

if (!SECRET || SECRET.length < 32) {
  console.error('请设置 JWT_SECRET(与后端一致的密钥, HS256 至少 32 字节)');
  process.exit(1);
}

const b64url = o => Buffer.from(JSON.stringify(o)).toString('base64url');
const now = Math.floor(Date.now() / 1000);
const header = b64url({ alg: 'HS256' });
const payload = b64url({ sub: String(USER_ID), iat: now, exp: now + 86400 });
const sig = crypto.createHmac('sha256', SECRET).update(`${header}.${payload}`).digest('base64url');
const token = `${header}.${payload}.${sig}`;

if (process.argv.includes('--sql')) {
  const expireMillis = (now + 86400) * 1000;
  console.log(`DELETE FROM t_login_session WHERE user_id = ${USER_ID};`);
  console.log(
    `INSERT INTO t_login_session (user_id, username, token, ip, login_time, expire_time)\n` +
    `VALUES (${USER_ID}, '${USERNAME}', '${token}', '127.0.0.1', NOW(), ${expireMillis});`
  );
} else {
  console.log(token);
}
