package com.graduate.thesis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置: CORS + 拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final PermInterceptor permInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(LoginInterceptor loginInterceptor,
                     AdminInterceptor adminInterceptor,
                     PermInterceptor permInterceptor,
                     @Value("${thesis.cors.allowed-origins}") String allowedOrigins) {
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.permInterceptor = permInterceptor;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/send-email-code",
                        "/user/reset-password",
                        "/captcha/**",
                        "/health",
                        "/file/download/**",
                        "/public/**",
                        "/auth/**",
                        "/template/market/list",
                        "/template/market/categories",
                        "/template/market/*/detail",
                        "/paper/task/*/progress",
                        "/error"
                );
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
        registry.addInterceptor(permInterceptor)
                .addPathPatterns("/**");
    }
}
