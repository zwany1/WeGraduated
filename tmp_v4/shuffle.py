# -*- coding: utf-8 -*-
"""基于原始 docx 生成两个"内容顺序被打乱"的样本, 用于排版压力测试。

S1 章级乱序: 封面/声明/摘要/目录保持原样, 9 个章节整体随机重排(章内顺序不变)。
S2 段级乱序: 前置部分保持原样, 正文 168..586 全部块随机重排(标题被打散),
             并额外注入一个 OMML 公式块(原文无公式), 用于验证公式排版。
仅重排 w:body 的直接子元素, 其余 zip 条目原样复制。
"""
import random, re, shutil, sys, zipfile
import xml.etree.ElementTree as ET

W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
NS_ALL = {
    'w': W,
    'r': 'http://schemas.openxmlformats.org/officeDocument/2006/relationships',
    'm': 'http://schemas.openxmlformats.org/officeDocument/2006/math',
    'wp': 'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing',
    'a': 'http://schemas.openxmlformats.org/drawingml/2006/main',
    'pic': 'http://schemas.openxmlformats.org/drawingml/2006/picture',
    'v': 'urn:schemas-microsoft-com:vml',
    'o': 'urn:schemas-microsoft-com:office:office',
    'w10': 'urn:schemas-microsoft-com:office:word',
    'mc': 'http://schemas.openxmlformats.org/markup-compatibility/2006',
    'wps': 'http://schemas.microsoft.com/office/word/2010/wordprocessingShape',
    'wpg': 'http://schemas.microsoft.com/office/word/2010/wordprocessingGroup',
    'w14': 'http://schemas.microsoft.com/office/word/2010/wordml',
    'w15': 'http://schemas.microsoft.com/office/word/2012/wordml',
    'wp14': 'http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing',
}
for k, v in NS_ALL.items():
    ET.register_namespace(k, v)

FORMULA_XML = '''<w:p xmlns:w="%s" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math">
  <w:pPr><w:jc w:val="center"/></w:pPr>
  <m:oMathPara><m:oMath>
    <m:r><m:t>t</m:t></m:r><m:r><m:t>=</m:t></m:r>
    <m:f><m:num><m:r><m:t>n</m:t></m:r></m:num><m:den><m:r><m:t>v</m:t></m:r></m:den></m:f>
    <m:r><m:t>+</m:t></m:r>
    <m:nary><m:naryPr><m:chr m:val="&#8721;"/><m:limLoc m:val="undOvr"/></m:naryPr>
      <m:sub><m:r><m:t>i=1</m:t></m:r></m:sub><m:sup><m:r><m:t>n</m:t></m:r></m:sup>
      <m:e><m:sSub><m:e><m:r><m:t>x</m:t></m:r></m:e><m:sub><m:r><m:t>i</m:t></m:r></m:sub></m:sSub></m:e>
    </m:nary>
  </m:oMath></m:oMathPara>
  <w:r><w:t>（3-1）</w:t></w:r>
</w:p>''' % W


def read_doc(path):
    z = zipfile.ZipFile(path)
    return z, z.read('word/document.xml').decode('utf-8')


def split_body(xml):
    """把 document.xml 拆成 head / body 子元素列表 / tail。按顶层 <w:p>/<w:tbl>/<w:sectPr> 深度扫描。"""
    start = xml.index('<w:body>') + len('<w:body>')
    end = xml.rindex('</w:body>')
    head, inner, tail = xml[:start], xml[start:end], xml[end:]
    blocks = []
    i = 0
    n = len(inner)
    pat = re.compile(r'<w:(p|tbl|sectPr|bookmarkStart|bookmarkEnd)(\s|/|>)')
    while i < n:
        m = pat.search(inner, i)
        if not m:
            break
        tag = m.group(1)
        # 找该元素的结束位置(处理自闭合与嵌套)
        j = m.start()
        # 计算标签深度
        depth = 0
        k = j
        open_re = re.compile(r'<(/?)w:%s(\s[^>]*?)?(/?)>' % tag)
        while True:
            mm = open_re.search(inner, k)
            if not mm:
                raise ValueError('unbalanced ' + tag)
            closing, attrs, selfclose = mm.group(1), mm.group(2) or '', mm.group(3)
            if selfclose:
                if closing == '':
                    endpos = mm.end()
                    if depth == 0:
                        break
            elif closing == '/':
                depth -= 1
                if depth == 0:
                    endpos = mm.end()
                    break
            else:
                depth += 1
            k = mm.end()
        blocks.append((tag, inner[j:endpos]))
        i = endpos
    return head, blocks, tail


def rebuild(path, out, blocks, extra=None):
    z, xml = read_doc(path)
    head, old_blocks, tail = split_body(xml)
    body = ''.join(b[1] for b in blocks)
    if extra:
        body += extra
    new_xml = head + body + tail
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zo:
        for item in z.infolist():
            if item.filename == 'word/document.xml':
                continue
            zo.writestr(item, z.read(item.filename))
        zo.writestr('word/document.xml', new_xml.encode('utf-8'))
    return len(blocks)


def main():
    import sys
    src = sys.argv[1] if len(sys.argv) > 1 else 'tmp_v4/orig/orig.docx'
    out = sys.argv[2] if len(sys.argv) > 2 else 'tmp_v4'
    import os
    os.makedirs(out, exist_ok=True)
    z, xml = read_doc(src)
    head, blocks, tail = split_body(xml)
    print('blocks parsed:', len(blocks), [b[0] for b in blocks[:5]])
    # 校验: 顶层块数应与解析脚本一致(584 p + 3 tbl + 1 sectPr)
    tags = [b[0] for b in blocks]
    from collections import Counter
    print(Counter(tags))

    # 前置部分: 从第一个一级标题(pStyle=2)之前算起 —— 不能写死块数, 换文档会错位
    h1_pos = [i for i, (t, x) in enumerate(blocks[:-1]) if t == 'p' and 'w:pStyle w:val="2"' in x]
    front_end = h1_pos[0] if h1_pos else 168
    print('front_end(before first h1):', front_end, ' h1 count:', len(h1_pos))
    front, body, sect = blocks[:front_end], blocks[front_end:-1], blocks[-1:]
    assert sect[0][0] == 'sectPr', sect[0][0]

    # --- S1 章级乱序 ---
    # 章起点: 按 <w:pStyle w:val="2"> (heading 1) 切分
    starts = [i for i, (t, x) in enumerate(body) if t == 'p' and 'w:pStyle w:val="2"' in x]
    print('heading1 positions(in body):', starts)
    bounds = starts + [len(body)]
    chapters = [body[bounds[k]:bounds[k + 1]] for k in range(len(starts))]
    names = [''.join(re.findall(r'<w:t[^>]*>([^<]*)</w:t>', c[0][1]))[:20] for c in chapters]
    print('chapters:', len(chapters), names)
    rnd = random.Random(20261029)
    order = list(range(len(chapters)))
    rnd.shuffle(order)
    print('S1 order:', order, [names[i] for i in order])
    s1 = front + [blk for i in order for blk in chapters[i]] + sect
    rebuild(src, f'{out}/s1_chapter_shuffle.docx', s1)

    # --- S2 段级乱序 ---
    rnd2 = random.Random(777)
    shuffled = body[:]
    rnd2.shuffle(shuffled)
    s2 = front + shuffled + sect
    rebuild(src, f'{out}/s2_block_shuffle.docx', s2, extra=FORMULA_XML)

    # 记录重排映射, 便于定位问题
    import json
    def idx_map(orig_blocks, new_blocks):
        ids = {id(b): i for i, b in enumerate(orig_blocks)}
        return [ids[id(b)] for b in new_blocks]
    json.dump({'s1': idx_map(blocks, s1), 's2': idx_map(blocks, s2)},
              open(f'{out}/shuffle_map.json', 'w'), ensure_ascii=False)
    print('written')


if __name__ == '__main__':
    main()
