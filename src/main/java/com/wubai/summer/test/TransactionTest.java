package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.config.AppConfig;
import com.wubai.summer.test.pojo.User;

public class TransactionTest {
    public static void main(String[] args) {
        System.out.println("========== 事务功能测试 ==========\n");

        //创建ioc核心容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);


        IUserService userService = context.getBeanByType(IUserService.class);

        // 测试1：正常提交
        System.out.println("\n--- 测试1：正常保存（应该提交） ---");
        try {
            User user1 = new User();
            user1.setName("张三");
            user1.setAge(25);
            userService.saveUser(user1);
            System.out.println("✅ 保存成功");
        } catch (Exception e) {
            System.out.println("❌ 保存失败：" + e.getMessage());
        }

        // 测试2：异常回滚
        System.out.println("\n--- 测试2：异常保存（应该回滚） ---");
        try {
            User user2 = new User();
            user2.setName("李四");
            user2.setAge(30);
            userService.saveUserWithError(user2);
        } catch (Exception e) {
            System.out.println("❌ 保存失败（预期行为）：" + e.getMessage());
        }

        System.out.println("\n========== 测试完成 ==========");
    }
}