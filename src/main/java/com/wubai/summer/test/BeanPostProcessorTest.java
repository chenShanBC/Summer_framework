package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.UserService;
import com.wubai.summer.test.config.AppConfig;

/**
 * @Author：fs
 * @Date:2026/3/519:58
 */
public class BeanPostProcessorTest {
    public static void main(String[] args) {
        System.out.println("========== BeanPostProcessor 测试 ==========\n");

        // 启动容器（会自动注册 BeanPostProcessor）
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("\n========== 获取并调用 Bean ==========");

        // 获取 Bean（可能是代理对象）
        UserService userService = context.getBeanByType(UserService.class);

        // 调用方法（如果有代理，会看到代理日志）
        userService.sayHello();

        System.out.println("\n========== 测试完成 ==========");
    }
}
