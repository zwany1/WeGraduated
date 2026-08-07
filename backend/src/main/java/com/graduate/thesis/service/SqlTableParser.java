package com.graduate.thesis.service;

import com.graduate.thesis.dto.SqlTableInfo;
import com.graduate.thesis.dto.TableColumnInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL CREATE TABLE 语句解析器: 提取表名与列信息(列名/类型/是否可空/默认值/注释)
 */
public final class SqlTableParser {

    private static final Pattern TABLE_NAME = Pattern.compile(
            "(?is)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?[`\\[\\\"]?([\\w$]+)[`\\]\\\"]?");

    /** 列定义: 反引号/方括号/引号包裹的列名 + 类型(可带长度/精度) + 可选的 unsigned + 其余属性 */
    private static final Pattern COLUMN = Pattern.compile(
            "(?is)^[`\\[\\\"]?([\\w$]+)[`\\]\\\"]?\\s+" +
            "([a-zA-Z_][\\w]*)(\\s*\\(\\s*\\d+(?:\\s*,\\s*\\d+)?\\s*\\))?(\\s+unsigned)?" +
            "\\s*(.*)$");

    private static final Pattern NOT_NULL = Pattern.compile("(?i)\\bnot\\s+null\\b");
    private static final Pattern DEFAULT_VALUE = Pattern.compile(
            "(?i)default\\s+((?:'[^']*')|(?:[\\w.+-]+)|(?:NULL)|(?:CURRENT_TIMESTAMP(?:\\(\\d+\\))?))");
    private static final Pattern COMMENT = Pattern.compile(
            "(?i)comment\\s*['\"]([^'\"]*)['\"]");

    /** 表级约束行, 不当作列 */
    private static final Pattern TABLE_CONSTRAINT = Pattern.compile(
            "(?i)^\\s*(primary\\s+key|unique(\\s+key|\\s+index)?|key|index|constraint|fulltext|spatial|check|foreign\\s+key|references)\\b");

    private SqlTableParser() {
    }

    public static SqlTableInfo parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new SqlTableInfo("", Collections.emptyList());
        }
        String clean = stripComments(sql);
        Matcher tm = TABLE_NAME.matcher(clean);
        String tableName = tm.find() ? tm.group(1) : "";

        int start = clean.indexOf('(');
        int end = clean.lastIndexOf(')');
        String body = (start >= 0 && end > start) ? clean.substring(start + 1, end) : "";

        List<TableColumnInfo> columns = new ArrayList<>();
        for (String seg : splitTopLevel(body)) {
            String s = seg.trim();
            if (s.isEmpty() || TABLE_CONSTRAINT.matcher(s).find()) {
                continue;
            }
            TableColumnInfo col = parseColumn(s);
            if (col != null) {
                columns.add(col);
            }
        }
        return new SqlTableInfo(tableName, columns);
    }

    private static TableColumnInfo parseColumn(String def) {
        Matcher m = COLUMN.matcher(def);
        if (!m.matches()) {
            return null;
        }
        String name = m.group(1);
        String type = m.group(2);
        String size = m.group(3);
        String unsigned = m.group(4);
        String rest = m.group(5);

        StringBuilder typeBuf = new StringBuilder(type);
        if (size != null) {
            typeBuf.append(size.replaceAll("\\s+", ""));
        }
        if (unsigned != null) {
            typeBuf.append(" unsigned");
        }

        boolean nullable = !NOT_NULL.matcher(rest).find();
        String defaultValue = null;
        Matcher dm = DEFAULT_VALUE.matcher(rest);
        if (dm.find()) {
            String v = dm.group(1);
            if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            if (!"NULL".equalsIgnoreCase(v) && !"CURRENT_TIMESTAMP".equalsIgnoreCase(v)) {
                defaultValue = v;
            } else {
                defaultValue = v;
            }
        }
        String comment = null;
        Matcher cm = COMMENT.matcher(rest);
        if (cm.find()) {
            comment = cm.group(1);
        }
        return new TableColumnInfo(name, typeBuf.toString(), nullable, defaultValue, comment);
    }

    /**
     * 顶层按逗号拆分(忽略括号内逗号)
     */
    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        char quote = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inStr) {
                cur.append(c);
                if (c == quote) {
                    inStr = false;
                }
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                inStr = true;
                quote = c;
                cur.append(c);
            } else if (c == '(') {
                depth++;
                cur.append(c);
            } else if (c == ')') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

    /**
     * 移除 SQL 注释(--、/* * /、#)
     */
    private static String stripComments(String sql) {
        String s = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        s = s.replaceAll("(?m)^[ \\t]*--.*$", " ");
        s = s.replaceAll("(?m)^[ \\t]*#.*$", " ");
        return s;
    }
}
