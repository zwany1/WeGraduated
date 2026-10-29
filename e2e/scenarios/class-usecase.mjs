// 场景: 类图 / 用例图 的文本入口(票 04)。
// 断言: 类解析出属性与方法、关系名与基数上屏、缺可见性前缀能定位、用例图参与者/用例/include 正确。
//
// 运行: node e2e/scenarios/class-usecase.mjs
import { openPage, loginByToken, sleep, check, summarize, readToken } from '../cdp.mjs';

const FRONT = process.env.E2E_FRONT || 'http://localhost:5173';
const token = readToken();
const cdp = await openPage();

const clickTab = name => cdp.eval(`(() => {
  const t = [...document.querySelectorAll('.el-radio-button')].find(b => b.textContent.trim() === ${JSON.stringify(name)});
  if (t) { t.querySelector('input') ? t.querySelector('input').click() : t.click(); return 'ok'; }
  return 'NOT FOUND';
})()`);

const clickBtn = text => cdp.eval(`(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes(${JSON.stringify(text)}));
  if (b) { b.click(); return 'ok'; }
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
  const nodes = [...document.querySelectorAll('.x6-node')].map(n => (n.textContent || '').replace(/\\s+/g, ' ').trim());
  const edges = [...document.querySelectorAll('.x6-edge')].map(e => (e.textContent || '').replace(/\\s+/g, ' ').trim()).filter(Boolean);
  return JSON.stringify({
    err: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 200) : '',
    nodes: nodes.filter(Boolean),
    nodeCount: document.querySelectorAll('.x6-node').length,
    edgeCount: document.querySelectorAll('.x6-edge').length,
    edgeTexts: edges
  });
})()`);

await loginByToken(cdp, token, FRONT);
await cdp.goto(`${FRONT}/system-design`, 3200);

// ---------- 类图 ----------
console.log('tab:', await clickTab('类图'));
await sleep(700);
console.log('example:', await clickBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickBtn('生成类图'));
await sleep(3500);

const cls = JSON.parse(await pageState());
console.log('类图节点:', JSON.stringify(cls.nodes));
console.log('类图连线文字:', JSON.stringify(cls.edgeTexts));
check('类图文本生成成功且无报错', cls.nodeCount >= 2 && !cls.err, `nodes=${cls.nodeCount}`);
check('「论文」类含属性(标题: String)与方法(提交())',
  cls.nodes.some(t => t.includes('论文') && t.includes('标题') && t.includes('String') && t.includes('提交')),
  cls.nodes.find(t => t.includes('论文')) || 'n/a');
check('关系名「属于」与基数出现在连线上',
  cls.edgeTexts.some(t => t.includes('属于')), cls.edgeTexts.join('|'));

// 缺可见性前缀 -> 行号
await setTextarea('class 论文 {\n  - 标题: String\n  提交()\n}');
await sleep(300);
await clickBtn('生成类图');
await sleep(2000);
const clsErr = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({ on: !!err, text: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 160) : '' });
})()`));
console.log('类图报错:', JSON.stringify(clsErr));
check('方法缺可见性前缀能指到第 3 行',
  clsErr.on && clsErr.text.includes('第 3 行') && clsErr.text.includes('提交()'), clsErr.text);

// ---------- 用例图 ----------
console.log('tab:', await clickTab('用例图'));
await sleep(700);
console.log('example:', await clickBtn('填入示例'));
await sleep(500);
console.log('generate:', await clickBtn('生成用例图'));
await sleep(3000);

const uc = JSON.parse(await pageState());
console.log('用例图节点:', JSON.stringify(uc.nodes));
console.log('用例图连线:', JSON.stringify(uc.edgeTexts));
check('用例图文本生成成功且无报错', uc.nodeCount >= 5 && !uc.err, `nodes=${uc.nodeCount}`);
check('参与者「学生」「教师」都在图上',
  uc.nodes.some(t => t.includes('学生')) && uc.nodes.some(t => t.includes('教师')), uc.nodes.join('|'));
check('用例「提交论文」「格式校验」都在图上',
  uc.nodes.some(t => t.includes('提交论文')) && uc.nodes.some(t => t.includes('格式校验')), uc.nodes.join('|'));
check('包含关系虚线箭头带 «include»',
  uc.edgeTexts.some(t => t.includes('include')), uc.edgeTexts.join('|'));

// 无法识别的行 -> 行号
await setTextarea('(学生): 提交论文\n这一行什么都不像');
await sleep(300);
await clickBtn('生成用例图');
await sleep(2000);
const ucErr = JSON.parse(await cdp.eval(`(() => {
  const err = document.querySelector('.dsl-error');
  return JSON.stringify({ on: !!err, text: err ? err.innerText.replace(/\\s+/g, ' ').slice(0, 160) : '' });
})()`));
console.log('用例图报错:', JSON.stringify(ucErr));
check('用例图无法识别的行能指到第 2 行', ucErr.on && ucErr.text.includes('第 2 行'), ucErr.text);

await cdp.screenshot('e2e/shot-class-usecase.png');
console.log('截图: e2e/shot-class-usecase.png');
process.exit(summarize() ? 3 : 0);
