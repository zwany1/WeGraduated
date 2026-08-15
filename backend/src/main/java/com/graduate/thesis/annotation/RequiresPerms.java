package com.graduate.thesis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限标识校验(按钮级权限).
 * 标注在 Controller 方法或类上, 由 {@link com.graduate.thesis.config.PermInterceptor} 校验.
 * 超级管理员(admin 角色)拥有所有权限.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPerms {

    /** 所需权限标识, 如 system:user:add */
    String[] value();

    /**
     * 多个权限的关系: ANY 满足任意一个即可, ALL 需全部满足
     */
    Logical logical() default Logical.ANY;

    enum Logical {
        ANY, ALL
    }
}
