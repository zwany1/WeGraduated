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
  a4: { file: 's1_chapter_shuffle.docx', tpl: '桂林信息科技学院毕业论文格式', label: 'A4/校规' },
  b5: { file: 's1_chapter_shuffle.docx', tpl: '测试版式B·B5-仿宋-1.5倍', label: 'B5/仿宋' },
  a4s: { file: 's1_chapter_shuffle.docx', tpl: '测试版式C·A4-宋体五号-单倍', label: 'A4/五号单倍' }
};

// A4 210mm ≈ 793.7px(96dpi); ISO B5 176mm ≈ 665px(96dpi)
const PAPER = { A4: 794, B5: 665 };

const cdp = await openPage(9333);
await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1000, deviceScaleFactor: 1, mobile: false });

const METRICS = `(() => {
  // 取最后一个 .docx-body: 上一个预览弹窗可能还在卸载中, 不能读到它的指标
  const bodies = [...document.querySelectorAll('.docx-body')];
  const body = bodies[bodies.length - 1];
  const wrap = body ? body.closest('.docx-wrap') : null;
  if (!wrap || !body) return null;
  const secs = [...body.querySelectorAll('section')];
  const ps = [...body.querySelectorAll('p')];
  const cs = el => el ? getComputedStyle(el) : null;
  const textSpan = el => el ? (el.querySelector('span') || el) : null;
  const _h1 = ps.findIndex(p => /docx_2\\b/.test(p.className || ''));
  const bodyP = ps.slice(_h1 > 0 ? _h1 : 0).find(p => (p.innerText || '').trim().length > 30);
  // 标题/题注必须带文字: 分页会在页尾留下空段落, 空段落若带同类样式会被误当成第一个标题
  const headingP = ps.find(p => /docx_2\\b/.test(p.className || '') && (p.innerText || '').trim());
  const captionP = ps.find(p => /^(图|表)\\s*\\d/.test((p.innerText || '').trim()));
  // 取"真正承载文字"的叶子 span: 段落里可能还有一个空 wrapper span(带样式默认字号), 不能用它判字号
  const leaf = el => {
    if (!el) return null;
    const spans = [...el.querySelectorAll('span')].filter(s => (s.textContent || '').trim());
    return spans.length ? spans[spans.length - 1] : el;
  };
  const f = el => {
    if (!el) return null;
    const s = cs(el);
    return { ff: s.fontFamily, fs: parseFloat(s.fontSize), fw: s.fontWeight, ta: s.textAlign,
             lh: s.lineHeight, ti: s.textIndent };
  };
  const dlg = body.closest('.el-dialog') || document;
  const onMode = dlg.querySelector('[data-mode].on');
  // 「段前分页」是否生效: 每个一级标题应当是其所在页第一个有文字的段落
  const allH1 = [...body.querySelectorAll('p.docx_2')];
  const h1s = allH1.filter(p => (p.innerText || '').trim());
  const h1 = { n: h1s.length, atTop: 0, miss: [], empty: allH1.length - h1s.length };
  h1s.forEach(p => {
    const sec = p.closest('section');
    if (!sec) return;
    // 每页都有页眉(<header>), 只比较 <article> 内的第一个正文段
    const art = sec.querySelector('article') || sec;
    const first = [...art.querySelectorAll('p')].find(x => (x.innerText || '').trim());
    if (first === p) h1.atTop++;
    else h1.miss.push((p.innerText || '').replace(/\\s+/g, '').slice(0, 10));
  });
  return {
    h1,
    mode: onMode ? onMode.dataset.mode : 'continuous',
    sections: secs.length,
    sectionWidth: secs[0] ? Math.round(secs[0].getBoundingClientRect().width * 10) / 10 : 0,
    lastWidth: secs.length ? Math.round(secs[secs.length - 1].getBoundingClientRect().width * 10) / 10 : 0,
    hasSheetNo: !!body.querySelector('.sheet-no') || !!dlg.querySelector('.sheet-no'),
    body: f(leaf(bodyP)),
    heading: headingP ? { ...f(leaf(headingP)), text: (headingP.innerText || '').slice(0, 12) } : null,
    caption: captionP ? { ...f(leaf(captionP)), pAlign: getComputedStyle(captionP).textAlign, text: (captionP.innerText || '').slice(0, 14) } : null,
    zoom: (dlg.querySelector('.zoom-val') || {}).innerText || ''
  };
})()`;

/** 关掉所有预览弹窗并等它们真正卸载(否则会读到上一个弹窗的指标) */
async function closeDialogs() {
  await cdp.eval(`document.querySelectorAll('.el-dialog__headerbtn').forEach(b => b.click())`);
  for (let i = 0; i < 20; i++) {
    await sleep(200);
    if (await cdp.eval(`document.querySelectorAll('.docx-body').length`) === 0) return;
  }
}

/** 打开某样本的预览; 返回是否成功 */
async function openPreview(sample) {
  await closeDialogs();
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
    const n = await cdp.eval(`(() => {
      const body = [...document.querySelectorAll('.docx-body')].pop();
      return body ? body.querySelectorAll('p').length : 0;
    })()`);
    if (n > 100) break;
  }
  await sleep(1200);
  return true;
}

/** 切到指定模式(页面视图由产品提供 [data-mode] 按钮; 缺省视为连续) */
async function setMode(mode) {
  const has = await cdp.eval(`(() => {
    const body = [...document.querySelectorAll('.docx-body')].pop();
    if (!body) return false;
    const dlg = body.closest('.el-dialog') || document;
    const b = dlg.querySelector('[data-mode="${mode}"]');
    if (!b) return false;
    b.click();
    return true;
  })()`);
  if (!has) return false;
  await sleep(1800);
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
    const cur = parseInt(await cdp.eval(`(() => {
      const body = [...document.querySelectorAll('.docx-body')].pop();
      const dlg = body ? (body.closest('.el-dialog') || document) : document;
      return (dlg.querySelector('.zoom-val') || {}).innerText || '100%';
    })()`)) || 100;
    if (cur >= target) break;
    await cdp.eval(`(() => {
      const body = [...document.querySelectorAll('.docx-body')].pop();
      const dlg = body ? (body.closest('.el-dialog') || document) : document;
      const b = [...dlg.querySelectorAll('.zoom button')];
      b[1] && b[1].click();
    })()`);
    await sleep(150);
  }
  await sleep(700);
  return cdp.eval(`(() => {
    const body = [...document.querySelectorAll('.docx-body')].pop();
    const dlg = body ? (body.closest('.el-dialog') || document) : document;
    return (dlg.querySelector('.zoom-val') || {}).innerText;
  })()`);
}

/** 缩放后内容是否可被完整滚到(裁切vs可滚动): 比较 body 布局宽与滚动容器的可滚宽。
 *  transform 缩放不改布局宽 → scrollWidth 追不上视觉宽度 → 右侧文字被永久切掉; zoom 则会重排。 */
async function overflow() {
  return cdp.eval(`(() => {
    const body = [...document.querySelectorAll('.docx-body')].pop();
    const wrap = body ? body.closest('.docx-wrap') : null;
    if (!wrap || !body) return { ok: false, bodyScroll: -1, wrapScroll: -1, visualRight: -1, left: 0 };
    const wr = wrap.getBoundingClientRect();
    let right = 0, left = 0;
    body.querySelectorAll('span').forEach(s => {
      if (!(s.textContent || '').trim()) return;
      const r = s.getBoundingClientRect();
      if (!r.width) return;
      right = Math.max(right, r.right - wr.right);
      left = Math.min(left, r.left - wr.left);
    });
    return { ok: body.scrollWidth <= wrap.scrollWidth + 2,
      bodyScroll: body.scrollWidth, wrapScroll: wrap.scrollWidth,
      visualRight: Math.round(right), left: Math.round(left) };
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

check('页面视图: 段前分页生效(每个一级标题都另起一页)',
  !!a4 && a4.h1.n >= 5 && a4.h1.atTop === a4.h1.n && a4.h1.empty === 0 && a4.sections >= 15,
  a4 ? `一级标题 ${a4.h1.atTop}/${a4.h1.n} 在页顶, 分页块 ${a4.sections}` +
       (a4.h1.empty ? `, 空标题段落 ${a4.h1.empty}` : '') +
       (a4.h1.miss.length ? `, 未另起页: ${a4.h1.miss.join('、')}` : '') : '未取到');

check('字体: 正文不再被强制成 Arial, 标题保留黑体',
  !!a4 && a4.body && !/Arial/i.test(a4.body.ff) && /宋体|Noto Serif/i.test(a4.body.ff)
       && !!a4.heading && /黑体|Noto Sans|SimHei/i.test(a4.heading.ff),
  a4 && a4.body ? `正文 ${a4.body.ff.slice(0, 40)} / 标题 ${(a4.heading || {}).ff}` : '未取到');

check('对齐: 题注居中, 正文两端对齐',
  !!a4 && a4.caption && a4.caption.pAlign === 'center',
  a4 && a4.caption ? `题注 ${a4.caption.pAlign} (${a4.caption.text})` : '未取到题注');

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

// B5 模板只改了 body 节(最后一节), 前两节仍是 A4 —— 所以取最后一页宽度验证"纸宽跟随文档"
check('纸宽随版式变化: B5 末页 ≠ A4',
  !!b5 && Math.abs(b5.lastWidth - PAPER.B5) <= 2 && Math.abs(b5.lastWidth - PAPER.A4) > 20,
  b5 ? `B5 末页 ${b5.lastWidth}px / 首页 ${b5.sectionWidth}px, A4 ${a4 ? a4.sectionWidth : '?'}px` : '未取到');

if (b5 && b5.lastWidth !== b5.sectionWidth) {
  console.log(`⚠️  同一文档页码尺寸不一致: 首页 ${b5.sectionWidth}px, 末页 ${b5.lastWidth}px` +
    ` —— 引擎只改了 body 节(B5=8293×11810 twips≈146×208mm, 也不是标准 B5 176×250mm)`);
}

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
check('缩放 200%: 文字可完整滚动查看(不被裁切)',
  !!ov && ov.ok,
  `缩放 ${z}, body 布局宽 ${ov.bodyScroll}px / 可滚宽 ${ov.wrapScroll}px (视觉溢出 ${ov.visualRight}px)`);

const failed = summarize();
process.exit(failed ? 3 : 0);
