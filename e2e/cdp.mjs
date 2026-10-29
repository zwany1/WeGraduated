// 零依赖 CDP 驱动: 手写 WebSocket(net + crypto), 用来驱动 headless chromium 做真实端到端验证。
// 不引入 playwright / puppeteer —— 本机装不上也不该为几个冒烟脚本增加依赖。
//
// 前置:
//   1) chromium --headless --remote-debugging-port=9333 --user-data-dir=... about:blank
//   2) 后端在 13355, 前端 vite 在 5173
//   3) 有一个有效 token(e2e/gen-token.mjs 生成, 并插入 t_login_session 会话行)
import net from 'net';
import crypto from 'crypto';
import fs from 'fs';

export const sleep = ms => new Promise(r => setTimeout(r, ms));

/** 取 chromium 调试端点的 JSON。注意它是 keep-alive, 必须按 Content-Length 读完, 等 'end' 会永远挂起。 */
export function getJson(url) {
  return new Promise((res, rej) => {
    const u = new URL(url);
    const s = net.connect(+u.port, u.hostname, () => s.write(`GET ${u.pathname}${u.search} HTTP/1.1\r\nHost: ${u.host}\r\nConnection: close\r\n\r\n`));
    let buf = Buffer.alloc(0);
    s.setTimeout(8000, () => { s.destroy(); rej(new Error('http timeout: ' + url)); });
    s.on('data', c => {
      buf = Buffer.concat([buf, c]);
      const idx = buf.indexOf('\r\n\r\n');
      if (idx < 0) return;
      const m = buf.slice(0, idx).toString().match(/Content-Length:\s*(\d+)/i);
      if (!m) return;
      const total = idx + 4 + parseInt(m[1]);
      if (buf.length >= total) {
        s.destroy();
        try { res(JSON.parse(buf.slice(idx + 4, total).toString())); } catch (e) { rej(e); }
      }
    });
    s.on('error', rej);
  });
}

export class CDP {
  constructor(url) {
    this.url = url;
    this.buf = Buffer.alloc(0);
    this.msgs = [];
    this.cont = [];
    this.closed = false;
    this.id = 0;
  }

  connect() {
    return new Promise((res, rej) => {
      const u = new URL(this.url);
      const key = crypto.randomBytes(16).toString('base64');
      this.sock = net.connect(+u.port, u.hostname, () => this.sock.write(
        `GET ${u.pathname}${u.search} HTTP/1.1\r\nHost: ${u.host}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: ${key}\r\nSec-WebSocket-Version: 13\r\n\r\n`
      ));
      const to = setTimeout(() => rej(new Error('websocket handshake timeout')), 8000);
      this.sock.once('data', d => {
        clearTimeout(to);
        if (!d.toString().includes('101')) return rej(new Error('handshake failed: ' + d.toString().slice(0, 120)));
        this.sock.on('data', c => { this.buf = Buffer.concat([this.buf, c]); this.parse(); });
        res();
      });
      this.sock.once('error', rej);
    });
  }

  parse() {
    for (;;) {
      const b = this.buf;
      if (b.length < 2) return;
      const fin = (b[0] & 0x80) !== 0;
      const opcode = b[0] & 0x0f;
      let len = b[1] & 0x7f;
      let off = 2;
      if (len === 126) { if (b.length < 4) return; len = b.readUInt16BE(2); off = 4; }
      else if (len === 127) { if (b.length < 10) return; len = Number(b.readBigUInt64BE(2)); off = 10; }
      let mask = null;
      if (b[1] & 0x80) { mask = b.slice(off, off + 4); off += 4; }
      if (b.length < off + len) return;
      let payload = b.slice(off, off + len);
      if (mask) payload = Buffer.from(payload.map((x, i) => x ^ mask[i % 4]));
      this.buf = b.slice(off + len);
      if (opcode === 9) { this.raw(10, payload); continue; }
      if (opcode === 8) { this.closed = true; return; }
      if (opcode === 1 || opcode === 2) {
        if (fin) { this.msgs.push(this.cont.length ? Buffer.concat([...this.cont, payload]) : payload); this.cont = []; }
        else this.cont.push(payload);
      } else if (opcode === 0) {
        this.cont.push(payload);
        if (fin) { this.msgs.push(Buffer.concat(this.cont)); this.cont = []; }
      }
    }
  }

  raw(op, payload) {
    const mask = crypto.randomBytes(4);
    const len = payload.length;
    let header;
    if (len < 126) header = Buffer.from([0x80 | op, 0x80 | len]);
    else if (len < 65536) { header = Buffer.alloc(4); header[0] = 0x80 | op; header[1] = 0x80 | 126; header.writeUInt16BE(len, 2); }
    else { header = Buffer.alloc(10); header[0] = 0x80 | op; header[1] = 0x80 | 127; header.writeBigUInt64BE(BigInt(len), 2); }
    const masked = Buffer.from(payload.map((x, i) => x ^ mask[i % 4]));
    this.sock.write(Buffer.concat([header, mask, masked]));
  }

  async send(method, params = {}) {
    const id = ++this.id;
    this.raw(1, Buffer.from(JSON.stringify({ id, method, params })));
    for (let i = 0; i < 600; i++) {
      for (let k = 0; k < this.msgs.length; k++) {
        let m;
        try { m = JSON.parse(this.msgs[k].toString()); } catch { continue; }
        if (m.id === id) { this.msgs.splice(k, 1); return m; }
      }
      if (this.closed) throw new Error('websocket closed');
      await sleep(50);
    }
    throw new Error('CDP timeout: ' + method);
  }

  async eval(expression) {
    const r = await this.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (r.result && r.result.exceptionDetails) {
      throw new Error('page eval error: ' + JSON.stringify(r.result.exceptionDetails).slice(0, 400));
    }
    return r.result && r.result.result ? r.result.result.value : null;
  }

  async goto(url, waitMs = 2500) {
    await this.send('Page.navigate', { url });
    await sleep(waitMs);
  }

  mouse(type, x, y, clickCount = 1) {
    return this.send('Input.dispatchMouseEvent', {
      type, x, y,
      button: type === 'mouseMoved' ? 'none' : 'left',
      clickCount,
      buttons: type === 'mouseReleased' ? 0 : 1
    });
  }

  async dblclick(x, y) {
    await this.mouse('mousePressed', x, y, 1);
    await this.mouse('mouseReleased', x, y, 1);
    await this.mouse('mousePressed', x, y, 2);
    await this.mouse('mouseReleased', x, y, 2);
  }

  async drag(fromX, fromY, toX, toY, steps = 5) {
    await this.mouse('mousePressed', fromX, fromY, 1);
    for (let i = 1; i <= steps; i++) {
      await this.mouse('mouseMoved', fromX + (toX - fromX) * i / steps, fromY + (toY - fromY) * i / steps);
      await sleep(40);
    }
    await this.mouse('mouseReleased', toX, toY, 1);
  }

  async screenshot(path) {
    const shot = await this.send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path, Buffer.from(shot.result.data, 'base64'));
    return path;
  }
}

/** 连上 9333 端口上那个 page target */
export async function openPage(port = 9333) {
  const targets = await getJson(`http://127.0.0.1:${port}/json/list`);
  const page = targets.find(t => t.type === 'page');
  if (!page) throw new Error('没有可用的 page target, 检查 chromium 是否带 --remote-debugging-port 启动');
  const cdp = new CDP(page.webSocketDebuggerUrl);
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  return cdp;
}

/** 注入登录态(验证码过不去, 只能自签 token + 会话行) */
export async function loginByToken(cdp, token, frontUrl = 'http://localhost:5173') {
  await cdp.goto(frontUrl + '/login', 1500);
  await cdp.eval(`
    localStorage.setItem('token', ${JSON.stringify(token)});
    localStorage.setItem('userId', '1');
    localStorage.setItem('username', 'testuser');
    localStorage.setItem('role', 'user');
  `);
}

const results = [];
export function check(name, ok, detail = '') {
  results.push({ name, ok });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  — ' + detail : ''}`);
  return ok;
}

export function summarize() {
  const failed = results.filter(r => !r.ok).length;
  console.log(`\n=== ${results.length - failed}/${results.length} 通过 ===`);
  return failed;
}

export function readToken() {
  const fromEnv = process.env.E2E_TOKEN;
  if (fromEnv) return fromEnv.trim();
  const p = new URL('./.token', import.meta.url);
  if (fs.existsSync(p)) return fs.readFileSync(p, 'utf8').trim();
  throw new Error('缺少 token: 先运行 `JWT_SECRET=... node e2e/gen-token.mjs > e2e/.token` (或设置 E2E_TOKEN)');
}
