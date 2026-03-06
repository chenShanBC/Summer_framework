package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.Services.IOrderService;

/**
 * AOP 功能测试
 */
public class AopTest {
    public static void main(String[] args) {
        System.out.println("========== AOP 功能测试 ==========\n");

        // 1. 启动容器（会自动扫描并注册切面）
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(
                com.wubai.summer.test.config.AppConfig.class
            );

        System.out.println("\n========== 测试 AOP 切面 ==========");

        // 2. 获取 Bean（应该是代理对象）
        IUserService userService = context.getBeanByType(IUserService.class);
        System.out.println("获取到的 Bean 类型：" + userService.getClass().getName());

        // 3. 调用方法（应该触发 @Before 切面）
        System.out.println("\n--- 调用 userService.sayHello() ---");
        userService.sayHello();

        // 4. 测试 OrderService
        System.out.println("\n--- 调用 orderService.createOrder() ---");
        IOrderService orderService = context.getBeanByType(IOrderService.class);
        orderService.createOrder();

        System.out.println("\n========== 测试完成 ==========");
    }
}
