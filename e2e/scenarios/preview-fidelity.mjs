// 预览保真度门禁: 真实浏览器打开「排版结果预览」, 断言纸张 / 分页 / 字体 / 对齐 / 标题字号 / 缩放六项。
//
// 前置:
//   1) 后端 13355、前端 5173 在跑
//   2) chromium --headless=new --remote-debugging-port=9333 --user-data-dir=... about:blank
//   3) 账号里已有三份产物任务(见 SAMPLES): A4/校规、B5/仿宋、A4/五号单倍
//
// 用法: node e2e/scenarios/preview-fidelity.mjs [--shots]
//   失败退出码 3(可直接当门禁); 截图落在 e2e/preview-shots/
import fs from 'fs';
import path from 'path';
import { openPage, sleep, check, summarize } from '../cdp.mjs';

const FRONT = 'http://localhost:5173';
const SHOT_DIR = 'e2e/preview-shots';
const WANT_SHOTS = process.argv.includes('--shots');

// 三份固定样本: 文件名 + 格式方案名(与 ru 账号下任务一致)
const SAMPLES = {
  a4: { file: 'orig.docx', tpl: '桂林信息科技学院毕业论文格式', label: 'A4/校规' },
  b5: { file: 's1_chapter_shuffle.docx', tpl: '测试版式B·B5-仿宋-1.5倍', label: 'B5/仿宋' },
  a4s: { file: 's1_chapter_shuffle.docx', tpl: '测试版式C·A4-宋体五号-单倍', label: 'A4/五号单倍' }
};

// A4 210mm ≈ 793.7px(96dpi); B5 176mm ≈ 665px
const PAPER = { A4: 794, B5: 665 };

const cdp = await openPage(9333);
await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1000, deviceScaleFactor: 1, mobile: false });

const METRICS = `(() => {
  const wrap = document.querySelector('.docx-wrap');
  const body = document.querySelector('.docx-body');
  if (!wrap || !body) return null;
  const secs = [...document.querySelectorAll('.docx-wrapper section')];
  const ps = [...document.querySelectorAll('.docx-body p')];
  const cs = el => el ? getComputedStyle(el) : null;
  const textSpan = el => el ? (el.querySelector('span') || el) : null;
  const bodyP = ps.find(p => (p.innerText || '').trim().length > 30);
  const headingP = ps.find(p => /docx_2\\b/.test(p.className || ''));
  const captionP = ps.find(p => /^(图|表)\\s*\\d/.test((p.innerText || '').trim()));
  const f = el => {
    if (!el) return null;
    const s = cs(el);
    return { ff: s.fontFamily, fs: parseFloat(s.fontSize), fw: s.fontWeight, ta: s.textAlign,
             lh: s.lineHeight, ti: s.textIndent };
  };
  return {
    mode: document.querySelector('[data-mode].on') ? document.querySelector('[data-mode].on').dataset.mode : 'continuous',
    sections: secs.length,
    sectionWidth: secs[0] ? Math.round(secs[0].getBoundingClientRect().width * 10) / 10 : 0,
    hasSheetNo: !!document.querySelector('.sheet-no'),
    body: f(textSpan(bodyP)),
    heading: headingP ? { ...f(textSpan(headingP)), text: (headingP.innerText || '').slice(0, 12) } : null,
    caption: captionP ? { ...f(captionP), text: (captionP.innerText || '').slice(0, 14) } : null,
    zoom: (document.querySelector('.zoom-val') || {}).innerText || '',
    overflowRight: 0, overflowLeft: 0
  };
})()`;

/** 打开某样本的预览; 返回是否成功 */
async function openPreview(sample) {
  await cdp.eval(`document.querySelectorAll('.el-dialog__headerbtn').forEach(b => b.click())`);
  await sleep(500);
  const ok = await cdp.eval(`(() => {
    const rows = [...document.querySelectorAll('.el-table__row')];
    const row = rows.find(r => r.innerText.includes(${JSON.stringify(sample.file)})
                            && r.innerText.includes(${JSON.stringify(sample.tpl)}));
    if (!row) return false;
    const b = [...row.querySelectorAll('button')].find(x => x.innerText.trim() === '预览');
    if (!b) return false;
    b.scrollIntoView({ block: 'center' });
    b.click();
    return true;
  })()`);
  if (!ok) return false;
  for (let i = 0; i < 40; i++) {
    await sleep(500);
    if (await cdp.eval(`document.querySelectorAll('.docx-body p').length`) > 100) break;
  }
  await sleep(1200);
  return true;
}

/** 切到指定模式(页面视图由产品提供 [data-mode] 按钮; 缺省视为连续) */
async function setMode(mode) {
  const has = await cdp.eval(`!!document.querySelector('[data-mode="${mode}"]')`);
  if (!has) return false;
  await cdp.eval(`document.querySelector('[data-mode="${mode}"]').click()`);
  await sleep(1500);
  return true;
}

async function shot(name) {
  if (!WANT_SHOTS) return;
  fs.mkdirSync(SHOT_DIR, { recursive: true });
  await cdp.screenshot(path.join(SHOT_DIR, name + '.png'));
}

async function metrics() {
  return cdp.eval(METRICS);
}

async function zoomTo(target) {
  for (let i = 0; i < 12; i++) {
    const cur = parseInt(await cdp.eval(`(document.querySelector('.zoom-val')||{}).innerText`)) || 100;
    if (cur >= target) break;
    await cdp.eval(`(() => { const b = [...document.querySelectorAll('.el-dialog .zoom button')]; b[1] && b[1].click(); })()`);
    await sleep(150);
  }
  await sleep(600);
  return cdp.eval(`(document.querySelector('.zoom-val')||{}).innerText`);
}

/** 缩放后文本是否被横向裁切: 比较所有文本 span 的包围盒与滚动容器 */
async function overflow() {
  return cdp.eval(`(() => {
    const wrap = document.querySelector('.docx-wrap');
    const wr = wrap.getBoundingClientRect();
    let right = 0, left = 0;
    document.querySelectorAll('.docx-body span').forEach(s => {
      if (!(s.textContent || '').trim()) return;
      const r = s.getBoundingClientRect();
      if (!r.width) return;
      right = Math.max(right, r.right - wr.right);
      left = Math.min(left, r.left - wr.left);
    });
    return { right: Math.round(right), left: Math.round(left) };
  })()`);
}

await cdp.goto(FRONT + '/tasks', 3500);

// ---- A4 样本: 纸宽 / 分页 / 字体 / 对齐 ----
let a4 = null;
if (await openPreview(SAMPLES.a4)) {
  await setMode('page');
  a4 = await metrics();
  await shot('a4-page');
  console.log('A4 实测:', JSON.stringify(a4));
} else {
  console.log('!! 找不到 A4 样本任务行:', JSON.stringify(SAMPLES.a4));
}

check('页面视图: 纸张宽度 = A4 210mm',
  !!a4 && Math.abs(a4.sectionWidth - PAPER.A4) <= 2,
  a4 ? `实测 ${a4.sectionWidth}px` : '未取到');

check('页面视图: 有分页块与页码角标',
  !!a4 && a4.sections >= 10 && a4.hasSheetNo,
  a4 ? `分页块 ${a4.sections}, 角标 ${a4.hasSheetNo}` : '未取到');

check('字体: 正文不再被强制成 Arial, 标题保留黑体',
  !!a4 && a4.body && !/Arial/i.test(a4.body.ff) && /宋体|Noto Serif/i.test(a4.body.ff)
       && !!a4.heading && /黑体|Noto Sans|SimHei/i.test(a4.heading.ff),
  a4 && a4.body ? `正文 ${a4.body.ff.slice(0, 40)} / 标题 ${(a4.heading || {}).ff}` : '未取到');

check('对齐: 题注居中, 正文两端对齐',
  !!a4 && a4.caption && a4.caption.ta === 'center',
  a4 && a4.caption ? `题注 ${a4.caption.ta} (${a4.caption.text})` : '未取到题注');

// ---- B5 样本: 纸宽必须与 A4 不同 ----
let b5 = null;
if (await openPreview(SAMPLES.b5)) {
  await setMode('page');
  b5 = await metrics();
  await shot('b5-page');
  console.log('B5 实测:', JSON.stringify(b5));
} else {
  console.log('!! 找不到 B5 样本任务行:', JSON.stringify(SAMPLES.b5));
}

check('纸宽随版式变化: B5 ≠ A4',
  !!b5 && Math.abs(b5.sectionWidth - PAPER.B5) <= 2 && Math.abs(b5.sectionWidth - PAPER.A4) > 20,
  b5 ? `B5 ${b5.sectionWidth}px vs A4 ${a4 ? a4.sectionWidth : '?'}px` : '未取到');

// ---- A4/五号单倍: 标题字号/加粗 + 缩放不裁切 ----
let small = null;
if (await openPreview(SAMPLES.a4s)) {
  await setMode('page');
  small = await metrics();
  console.log('A4/五号 实测:', JSON.stringify(small));
} else {
  console.log('!! 找不到 A4/五号 样本任务行:', JSON.stringify(SAMPLES.a4s));
}

check('标题字号/加粗跟随模板: 15pt + 700',
  !!small && small.heading && Math.abs(small.heading.fs - 20) <= 0.6 && small.heading.fw === '700',
  small && small.heading ? `实测 ${small.heading.fs}px / ${small.heading.fw}` : '未取到');

const z = await zoomTo(200);
const ov = await overflow();
await shot('a4s-zoom200');
check('缩放 200%: 文字不被横向裁切',
  ov.right <= 2 && ov.left >= -2,
  `缩放 ${z}, 溢出 right=${ov.right}px left=${ov.left}px`);

const failed = summarize();
process.exit(failed ? 3 : 0);
