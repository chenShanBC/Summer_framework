# AOP 测试说明

## 测试目标

验证 AOP 切面功能是否正常工作，包括：
1. 切面类能否被正确扫描和注册
2. 代理对象能否被正确创建
3. @Before 通知能否在方法执行前触发

## 测试环境

- **切面类**：LoggingAspect（位于 com.wubai.summer.test.aspect）
- **目标类**：UserService、OrderService（以 Service 结尾）
- **切点表达式**：`*Service`（匹配所有以 Service 结尾的类）

## 预期输出

运行 AopTest.java 后，应该看到：

```
========== AOP 功能测试 ==========

✅ 注册切面：LoggingAspect.logBefore -> *Service
🔧 为 UserService 创建 AOP 代理
🔧 为 OrderService 创建 AOP 代理

========== 测试 AOP 切面 ==========
获取到的 Bean 类型：com.sun.proxy.$Proxy...

--- 调用 userService.sayHello() ---
📝 [AOP] 方法执行前
UserService: Hello User{name='testUser', age=20}

--- 调用 orderService.createOrder() ---
📝 [AOP] 方法执行前
OrderService: 创建订单，用户：...

========== 测试完成 ==========
```

## 关键验证点

1. **切面注册**：看到 "✅ 注册切面：LoggingAspect.logBefore"
2. **代理创建**：看到 "🔧 为 UserService 创建 AOP 代理"
3. **代理类型**：Bean 类型是 $Proxy 开头（JDK 动态代理）
4. **切面执行**：方法调用前看到 "📝 [AOP] 方法执行前"

## 运行方式

### 方式 1：IDE 直接运行
右键 AopTest.java → Run 'AopTest.main()'

### 方式 2：Maven 命令
```bash
mvn compile exec:java -Dexec.mainClass="com.wubai.summer.test.AopTest"
```

## 注意事项

⚠️ **重要**：确保 AppConfig 的扫描路径包含：
- `com.wubai.summer.test`（包含切面类和业务类）
- `com.wubai.summer.core`（包含 AopProxyBeanPostProcessor）

如果 AOP 不生效，检查：
1. AopProxyBeanPostProcessor 是否被扫描到（需要 @Component 注解）
2. LoggingAspect 是否有 @Component 和 @Aspect 注解
3. 目标类是否以 Service 结尾
