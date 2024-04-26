package com.coderevolt.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 公众号: CodeRevolt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RPC {
    /**
     * 实现类名称
     * @see java.lang.Class#getSimpleName()
     * @return
     */
    String value();

    /**
     * 目标ip
     * @return
     */
    String ip() default "127.0.0.1";

    /**
     * 目标端口
     * @return
     */
    int port() default 8080;

}
