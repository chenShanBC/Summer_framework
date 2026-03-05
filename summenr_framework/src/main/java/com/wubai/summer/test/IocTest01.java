package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.OrderService;
import com.wubai.summer.test.Services.UserService;
import com.wubai.summer.test.config.AppConfig;
import com.wubai.summer.test.config.DataSource;

/**
 * @Author：fs
 * @Date:2026/3/515:12
 */
public class IocTest01 {
    public static void main(String[] args) {
        // 1. 启动IoC容器，传入配置类
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        System.out.println("===== IoC容器启动完成 =====");

        // 2. 根据名称获取Bean
        UserService userService = (UserService) context.getBean("userService");
        userService.sayHello();

        // 3. 根据类型获取Bean
        OrderService orderService = context.getBeanByType(OrderService.class);
        orderService.createOrder();

        // 4. 获取第三方Bean（@Bean创建）
        DataSource dataSource = context.getBeanByType(DataSource.class);
        System.out.println("===== 第三方Bean =====");
        System.out.println(dataSource);

        // 5. 验证单例：多次获取同一Bean，实例相同
        UserService userService2 = context.getBeanByType(UserService.class);
        System.out.println("===== 单例验证 =====");
        System.out.println(userService == userService2); // true
    }
}
