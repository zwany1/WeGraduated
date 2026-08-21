package com.graduate.thesis.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析: 信任反代覆写的 X-Real-IP, 缺失时回退直连地址.
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
