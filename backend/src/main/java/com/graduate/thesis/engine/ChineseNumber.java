package com.graduate.thesis.engine;

/**
 * 中文数字转换
 */
public final class ChineseNumber {

    private static final char[] DIGITS = {'零', '一', '二', '三', '四', '五', '六', '七', '八', '九'};
    private static final char[] BIG_DIGITS = {'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static final java.util.regex.Pattern ARABIC = java.util.regex.Pattern.compile("\\d+");
    private static final java.util.regex.Pattern CHINESE =
            java.util.regex.Pattern.compile("[一二三四五六七八九十百千]+");

    private ChineseNumber() {
    }

    /**
     * 从标题文本中提取章节号(阿拉伯数字优先, 其次中文数字):
     * "第一章" -> 1, "1 绪论" -> 1, "一、绪论" -> 1, "1.1 小节" -> 1
     */
    public static int extract(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        java.util.regex.Matcher m = ARABIC.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignore) {
                return 0;
            }
        }
        m = CHINESE.matcher(text);
        if (m.find()) {
            return parse(m.group());
        }
        return 0;
    }

    /**
     * 中文数字转阿拉伯数字, 支持 一~九十九(如 第十二章 -> 12)
     */
    public static int parse(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int result = 0;
        int section = 0;
        boolean big = containsAny(s, BIG_DIGITS);
        char[] digitTable = big ? BIG_DIGITS : DIGITS;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int num = indexOf(digitTable, c);
            if (num >= 0) {
                section = num;
            } else if (c == '十') {
                section = section == 0 ? 10 : section * 10;
                result += section;
                section = 0;
            } else if (c == '百') {
                section = Math.max(section, 1) * 100;
                result += section;
                section = 0;
            } else if (c == '千') {
                section = Math.max(section, 1) * 1000;
                result += section;
                section = 0;
            }
        }
        result += section;
        return result;
    }

    private static boolean containsAny(String s, char[] table) {
        for (char c : s.toCharArray()) {
            if (indexOf(table, c) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 阿拉伯数字转中文数字(1-99): 1 -> 一, 12 -> 十二, 99 -> 九十九
     */
    public static String toChinese(int n) {
        if (n <= 0 || n >= 100) {
            return String.valueOf(n);
        }
        if (n < 10) {
            return String.valueOf(DIGITS[n]);
        }
        int ten = n / 10;
        int one = n % 10;
        StringBuilder sb = new StringBuilder();
        if (ten > 1) {
            sb.append(DIGITS[ten]);
        }
        sb.append('十');
        if (one > 0) {
            sb.append(DIGITS[one]);
        }
        return sb.toString();
    }

    private static int indexOf(char[] table, char c) {
        for (int i = 0; i < table.length; i++) {
            if (table[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
