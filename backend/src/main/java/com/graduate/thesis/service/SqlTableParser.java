package com.graduate.thesis.service;

import com.graduate.thesis.dto.SqlTableInfo;
import com.graduate.thesis.dto.TableColumnInfo;
import com.graduate.thesis.dto.ErParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** 列级主键标记 */
    private static final Pattern COLUMN_PRIMARY_KEY = Pattern.compile("(?i)\\bprimary\\s+key\\b");

    /** 表级主键: primary key (col1, col2) */
    private static final Pattern TABLE_PRIMARY_KEY = Pattern.compile(
            "(?is)^\\s*primary\\s+key\\s*\\((.*?)\\)");

    /** 外键: foreign key (cols) references tbl(cols) */
    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "(?is)foreign\\s+key\\s*\\([^)]*\\)\\s*references\\s+[`\\[\\\"]?([\\w$]+)[`\\]\\\"]?(?:\\s*\\(([^)]*)\\))?");

    /** 多表语句分割: 以 create table 开头 */
    private static final Pattern CREATE_TABLE_START = Pattern.compile("(?i)^\\s*create\\s+table");

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
     * 解析多条 CREATE TABLE 为 ER 图数据: 实体(表+字段+主键) + 关系(外键引用)
     */
    public static ErParseResult parseEr(String sql) {
        ErParseResult result = new ErParseResult();
        if (sql == null || sql.trim().isEmpty()) {
            return result;
        }
        String clean = stripComments(sql);
        String[] parts = clean.split("(?i)(?=create\\s+table)");
        for (String part : parts) {
            if (!CREATE_TABLE_START.matcher(part).find()) {
                continue;
            }
            Matcher tm = TABLE_NAME.matcher(part);
            if (!tm.find()) {
                continue;
            }
            String table = tm.group(1);
            int start = part.indexOf('(');
            int end = part.lastIndexOf(')');
            String body = (start >= 0 && end > start) ? part.substring(start + 1, end) : "";

            Set<String> pks = new HashSet<>();
            List<String> refs = new ArrayList<>();
            List<Map<String, Object>> attrs = new ArrayList<>();
            for (String seg : splitTopLevel(body)) {
                String s = seg.trim();
                if (s.isEmpty()) {
                    continue;
                }
                if (TABLE_CONSTRAINT.matcher(s).find()) {
                    Matcher pm = TABLE_PRIMARY_KEY.matcher(s);
                    if (pm.find()) {
                        pks.addAll(colsIn(pm.group(1)));
                    }
                    Matcher fm = FOREIGN_KEY.matcher(s);
                    if (fm.find() && fm.group(1) != null) {
                        refs.add(fm.group(1));
                    }
                    continue;
                }
                TableColumnInfo col = parseColumn(s);
                if (col == null) {
                    continue;
                }
                boolean key = pks.contains(col.getName()) || COLUMN_PRIMARY_KEY.matcher(s).find();
                Map<String, Object> attr = new HashMap<>();
                attr.put("name", col.getName());
                attr.put("key", key);
                attrs.add(attr);
                if (key) {
                    pks.add(col.getName());
                }
            }
            Map<String, Object> entity = new HashMap<>();
            entity.put("name", table);
            entity.put("attrs", attrs);
            result.getEntities().add(entity);
            for (String ref : refs) {
                if (ref.equalsIgnoreCase(table)) {
                    continue;
                }
                Map<String, Object> rel = new HashMap<>();
                rel.put("from", table);
                rel.put("to", ref);
                rel.put("label", "引用");
                result.getRelations().add(rel);
            }
        }
        return result;
    }

    /** 解析 "(a, b, c)" 中逗号分隔的列名 */
    private static List<String> colsIn(String expr) {
        List<String> list = new ArrayList<>();
        if (expr == null) {
            return list;
        }
        for (String c : expr.split(",")) {
            String t = c.trim().replace("`", "").replace("\"", "").replace("'", "");
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
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
