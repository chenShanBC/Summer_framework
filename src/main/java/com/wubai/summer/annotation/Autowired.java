package com.wubai.summer.annotation;

import java.lang.annotation.*;

/**
 * @Author：fs
 * @Date:2026/3/512:42
 */
//@Autowired：标识依赖注入（构造器 / Setter / 字段）
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
}
