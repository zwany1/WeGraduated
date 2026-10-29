// 场景: 流程图 / 架构图 的文本入口(票 03)。
// 断言: 文本能出图、判断节点是菱形、分支标签出现、架构分层正确、语法错误能定位到行。
//
// 运行: node e2e/scenarios/flow-arch.mjs
import { openPage, loginByToken, sleep, check, summarize, readToken } from '../cdp.mjs';

const FRONT = process.env.E2E_FRONT || 'http://localhost:5173';
const token = readToken();
const cdp = await openPage();

const clickTab = async name => cdp.eval(`(() => {
  const t = [...document.querySelectorAll('.el-radio-button')].find(b => b.textContent.trim() === ${JSON.stringify(name)});
  if (t) { t.querySelector('input') ? t.querySelector('input').click() : t.click(); return 'ok'; }
  return 'NOT FOUND';
})()`);

const clickBtn = async text => cdp.eval(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(text)}));
  if (b) { b.click(); return 'ok'; }
  return 'NOT FOUND';
})()`);

const setTextarea = async (text) => cdp.eval(`(() => {
  const tas = [...document.querySelectorAll('textarea')].filter(t => t.offsetParent !== null);
  const ta = tas[0];
  if (!ta) return 'NO TEXTAREA';
  const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;
  setter.call(ta, ${JSON.stringify(text)});
  ta.dispatchEvent(new Event('input', { bubbles: true }));
  return 'ok';
})()`);

await loginByToken(cdp, token, FRONT);
await cdp.goto(`${FRONT}/system-design`, 3200);

// ---------- 流程图 ----------
console.log('tab:', await clickTab('流程图'));
await sleep(700);
console.log('example:', await clickBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickBtn('生成流程图'));
await sleep(3000);

const flow = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  const shapes = [...document.querySelectorAll('.x6-node')].map(n => n.getAttribute('data-shape'));
  const edgeTexts = [...document.querySelectorAll('.x6-edge')].map(e => (e.textContent || '').trim()).filter(Boolean);
  return JSON.stringify({
    err: err ? err.innerText.slice(0, 200) : '',
    nodes: shapes.length,
    polygons: shapes.filter(s => s === 'polygon').length,
    edges: document.querySelectorAll('.x6-edge').length,
    edgeTexts
  });
})()`));
console.log('流程图:', JSON.stringify(flow));
check('流程图文本生成成功且无报错', flow.nodes >= 5 && !flow.err, `nodes=${flow.nodes}`);
check('「合格?」渲染为菱形(判断)', flow.polygons >= 1, `polygons=${flow.polygons}`);
check('分支标签「是」「否」出现在连线上',
  flow.edgeTexts.some(t => t.includes('是')) && flow.edgeTexts.some(t => t.includes('否')),
  flow.edgeTexts.join('|'));

// 语法错误定位
await setTextarea('开始 -> 提交\n提交 > 审核');
await sleep(300);
await clickBtn('生成流程图');
await sleep(2000);
const flowErr = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({ on: !!err, text: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 160) : '' });
})()`));
console.log('流程图报错:', JSON.stringify(flowErr));
check('流程图语法错误能指到第 2 行', flowErr.on && flowErr.text.includes('第 2 行'), flowErr.text);

// ---------- 架构图 ----------
console.log('tab:', await clickTab('架构图'));
await sleep(700);
console.log('example:', await clickBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickBtn('生成架构图'));
await sleep(2500);

const arch = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  const layers = [...document.querySelectorAll('.arch-layer')].map(l => ({
    name: (l.querySelector('.arch-layer-name') || {}).textContent || '',
    comps: [...l.querySelectorAll('.arch-comp')].map(c => (c.textContent || '').trim())
  }));
  return JSON.stringify({ err: err ? err.innerText.slice(0, 200) : '', title: (document.querySelector('.arch-title') || {}).textContent || '', layers });
})()`));
console.log('架构图:', JSON.stringify(arch));
check('架构图文本生成成功且无报错', !!arch.layers.length && !arch.err, `layers=${arch.layers.length}`);
check('三层按文本顺序出现', arch.layers.map(l => l.name.trim()).join('>') === '表现层>服务层>数据层',
  arch.layers.map(l => l.name.trim()).join('>'));
check('第一层组件为 Vue3 前端',
  arch.layers[0] && arch.layers[0].comps.join('/') === 'Vue3 前端',
  arch.layers[0] ? arch.layers[0].comps.join('/') : 'n/a');
check('数据层含 MySQL 与 Redis',
  arch.layers[2] && arch.layers[2].comps.includes('MySQL') && arch.layers[2].comps.includes('Redis'),
  arch.layers[2] ? arch.layers[2].comps.join('/') : 'n/a');

// 架构图缩进错误定位
await clickTab('架构图');
await sleep(400);
await setTextarea('表现层\n  Vue3 前端\n服务层\n   SpringBoot\n  MySQL');
await sleep(300);
await clickBtn('生成架构图');
await sleep(2000);
const archErr = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({ on: !!err, text: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 200) : '' });
})()`));
console.log('架构图报错:', JSON.stringify(archErr));
check('架构图缩进不一致能定位到行', archErr.on && /第 \d+ 行/.test(archErr.text), archErr.text);

await cdp.screenshot('e2e/shot-flow-arch.png');
console.log('截图: e2e/shot-flow-arch.png');
process.exit(summarize() ? 3 : 0);
