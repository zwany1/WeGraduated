// 场景: 泳道图 / 活动图 / ER 图 的文本入口(票 05)，外加「8 种图示例文本全部能出图」的全量冒烟。
//
// 运行: node e2e/scenarios/lane-activity-er.mjs
import { openPage, loginByToken, sleep, check, summarize, readToken } from '../cdp.mjs';

const FRONT = process.env.E2E_FRONT || 'http://localhost:5173';
const token = readToken();
const cdp = await openPage();

const clickTab = name => cdp.eval(`(() => {
  const t = [...document.querySelectorAll('.el-radio-button')].find(b => b.textContent.trim() === ${JSON.stringify(name)});
  if (t) { t.querySelector('input') ? t.querySelector('input').click() : t.click(); return 'ok'; }
  return 'NOT FOUND: ' + [...document.querySelectorAll('.el-radio-button')].map(x => x.textContent.trim()).join('|');
})()`);

/** 点当前面板里可见的、文案包含 text 的按钮（折叠区里的隐藏按钮不算） */
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

const pageState = () => cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  const nodes = [...document.querySelectorAll('.x6-node')].map(n => (n.textContent || '').replace(/\\s+/g, ' ').trim()).filter(Boolean);
  const edges = [...document.querySelectorAll('.x6-edge')].map(e => (e.textContent || '').replace(/\\s+/g, ' ').trim()).filter(Boolean);
  const shapes = [...document.querySelectorAll('.x6-node')].map(n => n.getAttribute('data-shape'));
  return JSON.stringify({
    err: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 200) : '',
    nodes, edges, shapes,
    nodeCount: document.querySelectorAll('.x6-node').length,
    edgeCount: document.querySelectorAll('.x6-edge').length
  });
})()`);

await loginByToken(cdp, token, FRONT);
await cdp.goto(`${FRONT}/system-design`, 3200);

// ---------- 泳道图 ----------
console.log('tab:', await clickTab('泳道图'));
await sleep(900);
console.log('example:', await clickVisibleBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickVisibleBtn('生成泳道图'));
await sleep(3000);
const swim = JSON.parse(await pageState());
console.log('泳道图:', JSON.stringify({ nodes: swim.nodes, edges: swim.edgeCount }));
check('泳道图文本生成成功且无报错', swim.nodeCount >= 3 && !swim.err, `nodes=${swim.nodeCount}`);
check('三条泳道里的节点都上图（提交申请/查看进度/审批申请）',
  ['提交申请', '查看进度', '审批申请'].every(n => swim.nodes.some(t => t.includes(n))), swim.nodes.join('|'));
check('连线条件标签「通过」上屏', swim.edges.some(t => t.includes('通过')), swim.edges.join('|'));

// ---------- 活动图 ----------
console.log('tab:', await clickTab('活动图'));
await sleep(900);
console.log('example:', await clickVisibleBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickVisibleBtn('生成活动图'));
await sleep(3000);
const act = JSON.parse(await pageState());
console.log('活动图:', JSON.stringify({ nodes: act.nodes, edges: act.edges }));
check('活动图文本生成成功且无报错', act.nodeCount >= 5 && !act.err, `nodes=${act.nodeCount}`);
check('判断节点渲染为菱形(是否完整?)', act.shapes.filter(s => s === 'polygon').length >= 1,
  `polygons=${act.shapes.filter(s => s === 'polygon').length}`);
check('分支条件「是」「否」上屏',
  act.edges.some(t => t.includes('是')) && act.edges.some(t => t.includes('否')), act.edges.join('|'));

// ---------- ER 图 ----------
console.log('tab:', await clickTab('ER图'));
await sleep(900);
console.log('example:', await clickVisibleBtn('填入示例'));
await sleep(500);
console.log('textarea:', await cdp.eval(`(() => {
  const ta = [...document.querySelectorAll('textarea')].filter(t => t.offsetParent !== null)[0];
  return ta ? ta.value.split('\\n').slice(0, 2).join(' | ') : 'NONE';
})()`));
console.log('generate:', await clickVisibleBtn('生成ER图'));
await sleep(3000);
const er = JSON.parse(await pageState());
console.log('ER 图:', JSON.stringify({ nodes: er.nodes, edges: er.edges }));
check('ER 图文本生成成功且无报错', er.nodeCount >= 4 && !er.err, `nodes=${er.nodeCount}`);
check('实体与属性上图（学生/学号/论文/论文编号）',
  ['学生', '学号', '论文', '论文编号'].every(n => er.nodes.some(t => t.includes(n))), er.nodes.join('|'));
check('关系基数 1/N 上屏', er.edges.some(t => t.includes('1')) && er.edges.some(t => t.includes('N')),
  er.edges.join('|'));

// ---------- ER 不支持 @title 的报错 ----------
await setTextarea('@title 选题\n学生 (学号*)');
await sleep(300);
await clickVisibleBtn('生成');
await sleep(2000);
const erErr = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({ on: !!err, text: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 120) : '' });
})()`));
console.log('ER 报错:', JSON.stringify(erErr));
check('ER 遇到 @title 明确报错并说明原因', erErr.on && erErr.text.includes('标题'), erErr.text);

// ---------- 8 种图全量冒烟(验收 5) ----------
const ALL = ['流程图', '架构图', '泳道图', '活动图', '用例图', '时序图', '类图', 'ER图'];
for (const tab of ALL) {
  await clickTab(tab);
  await sleep(900);
  const filled = await clickVisibleBtn('填入示例');
  await sleep(500);
  const gen = await clickVisibleBtn('生成');
  await sleep(3200);
  const st = JSON.parse(await pageState());
  const ok = !st.err && st.nodeCount > 0;
  check(`全量冒烟: ${tab} 示例文本能出图`, ok, `nodes=${st.nodeCount}${st.err ? ' err=' + st.err.slice(0, 60) : ''}`);
}

await cdp.screenshot('e2e/shot-lane-activity-er.png');
console.log('截图: e2e/shot-lane-activity-er.png');
process.exit(summarize() ? 3 : 0);
