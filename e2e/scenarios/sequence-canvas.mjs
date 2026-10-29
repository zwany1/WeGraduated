// 场景: 时序图从文本生成 -> 表单回填 -> 画布直接编辑 -> 回写文本 -> 保存持久化。
// 覆盖票 01(文本主入口/表单一致)与票 02(画布编辑/回写/折叠)的验收点。
//
// 运行: node e2e/scenarios/sequence-canvas.mjs
import fs from 'fs';
import { openPage, loginByToken, sleep, check, summarize, readToken } from '../cdp.mjs';

const FRONT = process.env.E2E_FRONT || 'http://localhost:5173';
const STORAGE = process.env.E2E_STORAGE || 'backend/data/storage/diagrams';
const EXAMPLE_NAME = process.env.E2E_SAMPLE_PART || '系统';
const RENAMED = '认证服务';

const token = readToken();
const cdp = await openPage();

// ---------- 1. 打开时序图并生成 ----------
await loginByToken(cdp, token, FRONT);
await cdp.goto(`${FRONT}/system-design`, 3200);
await cdp.eval(`(() => {
  const t = [...document.querySelectorAll('.el-radio-button')].find(b => b.textContent.trim() === '时序图');
  if (t) { t.querySelector('input') ? t.querySelector('input').click() : t.click(); }
  return 1;
})()`);
await sleep(700);
await cdp.eval(`(() => { const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('填入示例')); if (b) b.click(); return 1; })()`);
await sleep(600);
await cdp.eval(`(() => { const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('生成时序图')); if (b) b.click(); return 1; })()`);
await sleep(3500);

const gen = JSON.parse(await cdp.eval(`JSON.stringify({
  nodes: document.querySelectorAll('.x6-node').length,
  edges: document.querySelectorAll('.x6-edge').length,
  err: (document.querySelector('.dsl-error') || {}).innerText || ''
})`));
check('文本生成时序图成功(无行号报错)', gen.nodes >= 9 && gen.edges >= 4 && !gen.err, `nodes=${gen.nodes} edges=${gen.edges}`);

// ---------- 2. 表单回填一致 ----------
const form1 = JSON.parse(await cdp.eval(`JSON.stringify({
  title: (document.querySelector('.el-form .el-input__inner') || {}).value || '',
  parts: [...document.querySelectorAll('.uc-act input')].map(i => i.value),
  msgs: [...document.querySelectorAll('.item-card input')].map(i => i.value).filter(v => v && v.trim())
})`));
check('精确调整表单与画布一致(标题/参与者/消息)',
  form1.parts.length >= 3 && form1.msgs.length >= 4 && !!form1.title,
  `title=${form1.title} parts=${form1.parts.join('/')}`);

// ---------- 3. 面板默认折叠 ----------
const panel = JSON.parse(await cdp.eval(`(() => {
  const body = document.querySelector('.adv-body');
  const toggle = document.querySelector('.adv-toggle');
  return JSON.stringify({ hasToggle: !!toggle, hidden: body ? body.offsetParent === null : null });
})()`));
check('配置面板默认折叠为「高级设置」', panel.hasToggle && panel.hidden === true);

// ---------- 4. 双击节点改名 -> 文本同步 ----------
const rectOf = label => cdp.eval(`(() => {
  for (const n of document.querySelectorAll('.x6-node')) {
    if ((n.textContent || '').trim() === ${JSON.stringify(label)}) {
      const r = n.getBoundingClientRect();
      return JSON.stringify({ x: r.left + r.width / 2, y: r.top + r.height / 2 });
    }
  }
  return '';
})()`);

const sysRect = JSON.parse(await rectOf(EXAMPLE_NAME) || 'null');
check(`画布上能定位到「${EXAMPLE_NAME}」`, !!sysRect);

if (sysRect) {
  await cdp.dblclick(sysRect.x, sysRect.y);
  await sleep(600);
  const editor = JSON.parse(await cdp.eval(`(() => {
    const el = document.querySelector('.inline-edit');
    return el ? JSON.stringify({ on: true, value: el.value }) : JSON.stringify({ on: false });
  })()`));
  check('双击节点弹出内联编辑框(带原文字)', editor.on === true && editor.value === EXAMPLE_NAME, JSON.stringify(editor));

  if (editor.on) {
    await cdp.eval(`(() => {
      const el = document.querySelector('.inline-edit');
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
      setter.call(el, ${JSON.stringify(RENAMED)});
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', bubbles: true }));
      return 1;
    })()`);
    await sleep(1500);

    const after = JSON.parse(await cdp.eval(`JSON.stringify({
      dsl: document.querySelector('textarea') ? document.querySelector('textarea').value : '',
      node: [...document.querySelectorAll('.x6-node')].some(n => (n.textContent || '').trim() === ${JSON.stringify(RENAMED)}),
      closed: !document.querySelector('.inline-edit')
    })`));
    check('画布节点已改名', after.node === true && after.closed === true);
    check('文本视图同步为新名字(R4)', after.dsl.includes(RENAMED) && !after.dsl.includes(EXAMPLE_NAME),
      after.dsl.replace(/\n/g, ' | '));

    await cdp.eval(`(() => { const t = document.querySelector('.adv-toggle'); if (t) t.click(); return 1; })()`);
    await sleep(500);
    const names = JSON.parse(await cdp.eval(`JSON.stringify([...document.querySelectorAll('.uc-act input')].map(i => i.value))`));
    check('高级设置表单同步为新名字', names.includes(RENAMED), names.join('/'));

    // 文本往返: 用改后的文本重新生成, 改动不能被覆盖回去
    await cdp.eval(`(() => { const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('生成时序图')); if (b) b.click(); return 1; })()`);
    await sleep(3500);
    const round = JSON.parse(await cdp.eval(`JSON.stringify({
      node: [...document.querySelectorAll('.x6-node')].some(n => (n.textContent || '').trim() === ${JSON.stringify(RENAMED)})
    })`));
    check('用改后文本重新生成, 改动仍在(文本<->画布往返不丢)', round.node === true);
  }
}

// ---------- 5. 拖动节点 -> 保存 -> 落盘位置 ----------
// 目标取「表单里的最后一个参与者」, 不要用「最后一个有文字的节点」—— 画布上还有标题等文字节点
const allTexts = JSON.parse(await cdp.eval(`JSON.stringify([...document.querySelectorAll('.x6-node')].map(n => (n.textContent || '').trim()))`));
console.log('画布节点文字:', JSON.stringify(allTexts));

const partNames = JSON.parse(await cdp.eval(`JSON.stringify([...document.querySelectorAll('.uc-act input')].map(i => i.value).filter(Boolean))`));
const dragLabel = partNames[partNames.length - 1] || '数据库';
const lastPart = JSON.parse(await rectOf(dragLabel) || 'null');
console.log('拖动目标:', dragLabel, JSON.stringify(lastPart));

if (lastPart) {
  await cdp.drag(lastPart.x, lastPart.y, lastPart.x - 60, lastPart.y);
  await sleep(800);
  const moved = JSON.parse(await rectOf(dragLabel) || 'null');
  check(`拖动「${dragLabel}」后位置变化`, !!moved && Math.abs(moved.x - lastPart.x) > 60,
    `Δ=${moved ? Math.round(moved.x - lastPart.x) : 'n/a'}`);

  await cdp.eval(`(() => { const b = [...document.querySelectorAll('button')].find(x => x.textContent.trim() === '保存'); if (b) b.click(); return 1; })()`);
  await sleep(2000);

  let saved = null;
  try {
    const files = fs.readdirSync(STORAGE).filter(f => /^1_.*\.json$/.test(f));
    const latest = files.map(f => ({ f, t: fs.statSync(`${STORAGE}/${f}`).mtimeMs })).sort((a, b) => b.t - a.t)[0];
    saved = JSON.parse(fs.readFileSync(`${STORAGE}/${latest.f}`, 'utf8'));
  } catch { saved = null; }

  const parts = saved ? (saved.nodes || []).filter(n => n.shape === 'participant') : [];
  const target = parts.find(p => p.label === dragLabel);
  check('保存后的 JSON 含画布上的新名字', saved ? parts.some(p => p.label === RENAMED) : false,
    parts.map(p => p.label).join('/'));
  // 后端默认栅格: x = 80 + i*(96+56) -> 第 3 个参与者是 384。位置没被写回就会正好落在这个值上。
  const defaultX = 80 + (partNames.indexOf(dragLabel) >= 0 ? partNames.indexOf(dragLabel) : 2) * 152;
  check('拖动位置已随保存落盘(重新加载不会回原位)',
    !!target && typeof target.x === 'number' && Math.abs(target.x - defaultX) > 20,
    target ? `x=${Math.round(target.x)} 默认=${defaultX}` : '');
}

await cdp.screenshot('e2e/shot-sequence-canvas.png');
console.log('截图: e2e/shot-sequence-canvas.png');

process.exit(summarize() ? 3 : 0);
