package com.graduate.thesis.dsl;

import lombok.Data;

/**
 * DSL 解析结果。
 *
 * <p>失败时携带结构化信息（行号 / 无法识别的片段 / 该图语法提示），
 * 由 Controller 原样返回给前端渲染；前端不再自己猜错在哪。</p>
 */
@Data
public class DslParseResult {

    private boolean ok;
    /** 成功时的图配置（按图类型的 Config，如 SequenceConfig） */
    private Object config;
    /** 失败时的结构化错误 */
    private DslError error;

    public static DslParseResult success(Object config) {
        DslParseResult r = new DslParseResult();
        r.setOk(true);
        r.setConfig(config);
        return r;
    }

    public static DslParseResult fail(int line, String snippet, String hint, String message) {
        DslParseResult r = new DslParseResult();
        r.setOk(false);
        DslError e = new DslError();
        e.setLine(line);
        e.setSnippet(snippet);
        e.setHint(hint);
        e.setMessage(message);
        r.setError(e);
        return r;
    }

    @Data
    public static class DslError {
        /** 出错行号(1 起)；0 表示与具体行无关（如类型不支持、整体为空） */
        private int line;
        /** 无法识别的片段，原样返回供前端高亮 */
        private String snippet;
        /** 该图的语法提示（多行文本） */
        private String hint;
        /** 面向用户的一句话错误说明 */
        private String message;
    }
}
