package com.wubai.summer.annotation;

import java.lang.annotation.*;

/**
 * @Author：fs
 * @Date:2026/3/512:40
 */
//@Configuration ：标识配置类（特殊的 Component）
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component // 配置类本身也是Bean，被容器管理
public @interface Configuration {
}
