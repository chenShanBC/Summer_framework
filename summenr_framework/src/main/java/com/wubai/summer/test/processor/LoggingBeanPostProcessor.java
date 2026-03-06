package com.wubai.summer.test.processor;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.core.BeanPostProcessor;

/**
 * @Author：fs
 * @Date:2026/3/519:49
 */
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Logging----📝 [Before] 初始化Bean：" + beanName + " -> " + bean.getClass().getSimpleName());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("Logging----✅ [After] Bean初始化完成：" + beanName);
        return bean;
    }
}
