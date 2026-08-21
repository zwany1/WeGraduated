package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程 DSL 解析器:
 *   普通文本 = 普通节点
 *   if(条件)  = 判断节点(菱形)
 *   else      = 否定分支
 *   缩进      = 节点归属(if 块 / else 块)
 *
 * 递归解析缩进块, 构建 AST, 生成节点与边(是/否标签)
 */
@Service
public class FlowParser {

    private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

    public DiagramVO parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "请输入流程脚本");
        }
        List<Line> lines = readLines(content);
        GraphBuilder gb = new GraphBuilder();
        // 开始节点
        String startId = gb.addNode("开始", "start");
        int[] idx = {0};
        BlockResult res = parseBlock(lines, idx, 0, gb);
        if (res.firstId != null) {
            gb.connect(startId, res.firstId, "");
        }
        // 结束节点
        String endId = gb.addNode("结束", "end");
        for (String t : res.terminals) {
            gb.connect(t, endId, "");
        }
        // 布局: 纵向排列
        layout(gb);
        return gb.toVO();
    }

    // ============ 递归解析块 ============

    /**
     * 解析一个缩进块(缩进 == indent 的行, 含子 if 递归), 返回块首节点与终端节点
     */
    private BlockResult parseBlock(List<Line> lines, int[] idx, int indent, GraphBuilder gb) {
        String firstId = null;
        List<String> pending = new ArrayList<>();
        while (idx[0] < lines.size()) {
            Line l = lines.get(idx[0]);
            if (l.indent < indent) {
                break; // 块结束(缩进回退)
            }
            if (l.indent > indent) {
                idx[0]++; // 内层缩进已由递归处理, 跳过
                continue;
            }
            if (isElse(l.text)) {
                break; // else 边界由外层处理
            }

            if (isIf(l.text)) {
                idx[0]++;
                String cond = extractCondition(l.text);
                String ifId = gb.addNode(cond, "condition");
                if (firstId == null) {
                    firstId = ifId;
                }
                gb.connectAll(pending, ifId, "");
                // true 分支
                BlockResult trueRes = parseBlock(lines, idx, indent + 1, gb);
                if (trueRes.firstId != null) {
                    gb.connect(ifId, trueRes.firstId, "是");
                }
                // else 分支
                List<String> falseTerms = new ArrayList<>();
                if (idx[0] < lines.size() && isElse(lines.get(idx[0]).text) && lines.get(idx[0]).indent == indent) {
                    idx[0]++;
                    BlockResult falseRes = parseBlock(lines, idx, indent + 1, gb);
                    if (falseRes.firstId != null) {
                        gb.connect(ifId, falseRes.firstId, "否");
                    }
                    falseTerms = falseRes.terminals;
                }
                // 汇合: 该块的终端 = true 分支终端 + false 分支终端
                pending.clear();
                pending.addAll(trueRes.terminals);
                pending.addAll(falseTerms);
                if (pending.isEmpty()) {
                    pending.add(ifId);
                }
            } else {
                // 普通节点
                idx[0]++;
                String nid = gb.addNode(l.text, "normal");
                if (firstId == null) {
                    firstId = nid;
                }
                gb.connectAll(pending, nid, "");
                pending.clear();
                pending.add(nid);
            }
        }
        return new BlockResult(firstId, pending);
    }

    // ============ 工具 ============

    private boolean isIf(String text) {
        return text.startsWith("if(") || text.startsWith("if (");
    }

    private boolean isElse(String text) {
        return "else".equals(text.trim());
    }

    private String extractCondition(String text) {
        int s = text.indexOf('(');
        int e = text.indexOf(')');
        if (s >= 0 && e > s) {
            return text.substring(s + 1, e).trim();
        }
        return text.replaceFirst("(?i)if\\s*", "").trim();
    }

    private List<Line> readLines(String content) {
        List<Line> lines = new ArrayList<>();
        String[] raw = content.split("\n");
        for (String r : raw) {
            String s = r == null ? "" : r.replace("\r", "");
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 跳过开始/结束(自动生成)
            if ("开始".equals(trimmed) || "结束".equals(trimmed)) {
                continue;
            }
            int indent = countIndent(s);
            lines.add(new Line(indent, trimmed));
        }
        return lines;
    }

    private int countIndent(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                n++;
            } else if (c == '\t') {
                n += 4;
            } else {
                break;
            }
        }
        // 4 空格 = 1 级
        return n / 4;
    }

    /** 纵向布局: 按节点顺序 y 递增, 同层简单排列 */
    private void layout(GraphBuilder gb) {
        int y = 60;
        int gapY = 90;
        for (DiagramNode n : gb.nodes) {
            n.setX(120);
            n.setY(y);
            y += gapY;
        }
        int maxY = y;
        gb.width = 400;
        gb.height = maxY + 40;
    }

    private DiagramNode node(String label, String shape) {
        DiagramNode n = new DiagramNode();
        n.setId("n" + (seq.getAndIncrement()));
        n.setLabel(label);
        n.setShape(shape);
        return n;
    }

    // ============ 内部类 ============

    private static class Line {
        int indent;
        String text;

        Line(int indent, String text) {
            this.indent = indent;
            this.text = text;
        }
    }

    private static class BlockResult {
        String firstId;
        List<String> terminals;

        BlockResult(String firstId, List<String> terminals) {
            this.firstId = firstId;
            this.terminals = terminals;
        }
    }

    private class GraphBuilder {
        List<DiagramNode> nodes = new ArrayList<>();
        List<DiagramEdge> edges = new ArrayList<>();
        int width = 400;
        int height = 400;

        String addNode(String label, String shape) {
            DiagramNode n = node(label, shape);
            nodes.add(n);
            return n.getId();
        }

        void connect(String from, String to, String label) {
            DiagramEdge e = new DiagramEdge();
            e.setId("e" + (seq.getAndIncrement()));
            e.setSource(from);
            e.setTarget(to);
            e.setLabel(label);
            edges.add(e);
        }

        void connectAll(List<String> froms, String to, String label) {
            if (froms.isEmpty()) {
                return;
            }
            for (String f : froms) {
                connect(f, to, label);
            }
        }

        DiagramVO toVO() {
            DiagramVO vo = new DiagramVO();
            vo.setWidth(width);
            vo.setHeight(height);
            vo.setType("FLOW");
            vo.getNodes().addAll(nodes);
            vo.getEdges().addAll(edges);
            return vo;
        }
    }
}
