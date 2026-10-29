// 场景: 存量数据兼容与降级通道(票 06)。
//   1) 纯表单生成的图(无 DSL 文本) -> 保存 -> 打开时用 config 反向回填文本
//   2) 改造前的老图(既无 dsl 也无 config) -> 打开不报错、文本留空、表单可用
//   3) DSL 解析失败时 -> 展开表单能正常生成(降级通道)
//
// 运行: node e2e/scenarios/compat-load.mjs
import fs from 'fs';
import path from 'path';
import { openPage, loginByToken, sleep, check, summarize, readToken } from '../cdp.mjs';

const FRONT = process.env.E2E_FRONT || 'http://localhost:5173';
const STORAGE = process.env.E2E_STORAGE || 'backend/data/storage/diagrams';
const token = readToken();
const cdp = await openPage();

const clickVisibleBtn = text => cdp.eval(`(() => {
  const b = [...document.querySelectorAll('button')]
    .filter(x => x.offsetParent !== null && x.textContent.includes(${JSON.stringify(text)}))[0];
  if (b) { b.click(); return 'ok: ' + b.textContent.trim(); }
  return 'NOT FOUND';
})()`);

const setTextarea = text => cdp.eval(`(() => {
  const tas = [...document.querySelectorAll('textarea')].filter(t => t.offsetParent !== null);
  const ta = tas[0];
  if (!ta) return 'NO TEXTAREA';
  const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;
  setter.call(ta, ${JSON.stringify(text)});
  ta.dispatchEvent(new Event('input', { bubbles: true }));
  return 'ok';
})()`);

const textareaValue = () => cdp.eval(`(() => {
  const ta = [...document.querySelectorAll('textarea')].filter(t => t.offsetParent !== null)[0];
  return ta ? ta.value : '';
})()`);

const pageState = () => cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({
    err: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 160) : '',
    nodeCount: document.querySelectorAll('.x6-node').length,
    advOpen: (() => { const b = document.querySelector('.adv-body'); return b ? b.offsetParent !== null : null })()
  });
})()`);

await loginByToken(cdp, token, FRONT);
await cdp.goto(`${FRONT}/system-design`, 3200);

// ---------- 1. 纯表单生成(无文本) -> 保存 ----------
await cdp.eval(`(() => {
  const t = [...document.querySelectorAll('.el-radio-button')].find(b => b.textContent.trim() === '时序图');
  if (t) { t.querySelector('input') ? t.querySelector('input').click() : t.click(); }
  return 1;
})()`);
await sleep(900);
await clickVisibleBtn('填入示例');            // 先让 DSL 有内容, 再清空 -> 走表单路径
await sleep(400);
await setTextarea('');
await sleep(300);
console.log('生成(表单路径):', await clickVisibleBtn('生成时序图'));
await sleep(3200);
const formGen = JSON.parse(await pageState());
check('清空文本后仍能用表单生成(降级通道)', formGen.nodeCount > 0 && !formGen.err, `nodes=${formGen.nodeCount}`);

console.log('保存:', await clickVisibleBtn('保存'));
await sleep(1800);

// 磁盘上的这份 JSON 应有 config、但没有 dsl
let savedWithConfig = null;
try {
  const files = fs.readdirSync(STORAGE).filter(f => /^1_\d+\.json$/.test(f));
  const latest = files.map(f => ({ f, t: fs.statSync(path.join(STORAGE, f)).mtimeMs })).sort((a, b) => b.t - a.t)[0];
  savedWithConfig = { file: latest.f, data: JSON.parse(fs.readFileSync(path.join(STORAGE, latest.f), 'utf8')) };
} catch { savedWithConfig = null; }
check('保存的 JSON 里带了结构化配置(为回填做准备)',
  !!savedWithConfig && !!savedWithConfig.data.config && !savedWithConfig.data.dsl,
  savedWithConfig ? `config=${!!savedWithConfig.data.config} dsl=${!!savedWithConfig.data.dsl}` : '读不到文件');

// ---------- 2. 打开它 -> 文本应被 render 回填 ----------
console.log('打开列表:', await clickVisibleBtn('打开'));
await sleep(1500);
const listInfo = await cdp.eval(`JSON.stringify([...document.querySelectorAll('.saved-item')].map(x => x.innerText.replace(/\\s+/g, ' ').trim()).slice(0, 5))`);
console.log('列表:', listInfo);
check('打开列表列出了已保存的图', JSON.parse(listInfo).length > 0, listInfo);

const opened = await cdp.eval(`(() => {
  const items = [...document.querySelectorAll('.saved-item')];
  const target = items.find(x => x.innerText.includes('可回填文本')) || items[0];
  if (!target) return 'NO ITEM';
  target.click();
  return 'clicked: ' + target.innerText.replace(/\\s+/g, ' ').slice(0, 40);
})()`);
console.log('打开:', opened);
await sleep(2500);
const backfilled = await textareaValue();
console.log('回填文本:', backfilled.split('\n').slice(0, 4).join(' | '));
check('老图(有配置无文本)打开后文本被反向生成回填', backfilled.trim().length > 0 && backfilled.includes('->'), backfilled.slice(0, 80));
const afterOpen = JSON.parse(await pageState());
check('打开后画布正常渲染', afterOpen.nodeCount > 0 && !afterOpen.err, `nodes=${afterOpen.nodeCount}`);

// ---------- 3. 造一张真正的老图(只有 nodes, 没有 config/dsl) ----------
const legacyId = 990001;
const legacyFile = path.join(STORAGE, `1_${legacyId}.json`);
const legacyVo = {
  id: legacyId,
  name: '改造前的老图',
  type: 'SEQUENCE',
  nodes: [
    { id: 'P1', label: '旧用户', shape: 'participant', x: 80, y: 40, width: 96, height: 44 },
    { id: 'P2', label: '旧系统', shape: 'participant', x: 232, y: 40, width: 96, height: 44 },
    { id: 'act_P1', label: '', shape: 'activation', x: 128, y: 120, width: 10, height: 60 },
    { id: 'act_P2', label: '', shape: 'activation', x: 280, y: 120, width: 10, height: 60 }
  ],
  edges: [
    { id: 'm1', source: 'P1', target: 'P2', label: '旧请求', style: 'request', sourceX: 133, sourceY: 136, targetX: 275, targetY: 136 }
  ],
  lanes: [
    { id: 'line1', name: '旧用户', x: 128, y: 84, width: 0, height: 120 },
    { id: 'line2', name: '旧系统', x: 280, y: 84, width: 0, height: 120 }
  ],
  width: 400,
  height: 220
};
fs.writeFileSync(legacyFile, JSON.stringify(legacyVo), 'utf8');

console.log('再打开列表:', await clickVisibleBtn('打开'));
await sleep(1500);
const legacyClicked = await cdp.eval(`(() => {
  const items = [...document.querySelectorAll('.saved-item')];
  const target = items.find(x => x.innerText.includes('改造前的老图'));
  if (!target) return 'NOT FOUND: ' + items.map(i => i.innerText.replace(/\\s+/g, ' ')).join(' / ');
  target.click();
  return 'clicked';
})()`);
console.log('打开老图:', legacyClicked);
await sleep(2500);
const legacyState = JSON.parse(await pageState());
const legacyText = await textareaValue();
console.log('老图状态:', JSON.stringify(legacyState), 'text="' + legacyText.slice(0, 40) + '"');
check('改造前的老图能正常打开(画面渲染, 不报错)', legacyState.nodeCount > 0 && !legacyState.err, `nodes=${legacyState.nodeCount}`);
check('老图文本留空但不阻断(没有 config 可回填)', legacyText.trim() === '', `text="${legacyText.slice(0, 30)}"`);

console.log('展开高级设置:', await cdp.eval(`(() => { const t = document.querySelector('.adv-toggle'); if (t) { t.click(); return 1; } return 0; })()`));
await sleep(600);
const advState = JSON.parse(await pageState());
check('老图打开后高级设置能展开(表单可用)', advState.advOpen === true);

// ---------- 4. 解析失败 -> 展开表单仍能生成 ----------
await setTextarea('用户 >> 系统: 登录');
await sleep(300);
await clickVisibleBtn('生成时序图');
await sleep(2000);
const badState = JSON.parse(await pageState());
check('非法 DSL 给出结构化报错', !!badState.err && badState.err.includes('第 1 行'), badState.err.slice(0, 60));

console.log('表单里生成:', await clickVisibleBtn('生成时序图'));
await sleep(3000);
const formAfterBad = JSON.parse(await pageState());
check('文本报错后仍可用表单完成生成(降级通道)', formAfterBad.nodeCount > 0, `nodes=${formAfterBad.nodeCount}`);

try { fs.unlinkSync(legacyFile); } catch { /* 清理失败不影响结论 */ }
await cdp.screenshot('e2e/shot-compat.png');
console.log('截图: e2e/shot-compat.png');
process.exit(summarize() ? 3 : 0);
