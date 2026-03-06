# @Order 注解实现指南

## 一、功能说明

@Order 注解用于控制 BeanPostProcessor 的执行顺序。

**规则：**
- 数字越小，优先级越高
- 默认值为 Integer.MAX_VALUE（最低优先级）
- 未标注 @Order 的 BeanPostProcessor 优先级最低

**示例：**
```java
@Component
@Order(1)  // 第一个执行
public class FirstProcessor implements BeanPostProcessor { ... }

@Component
@Order(2)  // 第二个执行
public class SecondProcessor implements BeanPostProcessor { ... }

@Component  // 最后执行（默认 Integer.MAX_VALUE）
public class LastProcessor implements BeanPostProcessor { ... }
```

---

## 二、实现步骤

### 步骤 1：创建 @Order 注解

**文件位置：** `src/main/java/com/wubai/summer/annotation/Order.java`

**代码：**
```java
package com.wubai.summer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定Bean的执行顺序（数字越小，优先级越高）
 * 主要用于 BeanPostProcessor 的排序
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    /**
     * 顺序值（数字越小，优先级越高）
     * 默认为 Integer.MAX_VALUE（最低优先级）
     */
    int value() default Integer.MAX_VALUE;
}
```

---

### 步骤 2：修改 ClassPathXmlApplicationContext

**位置：** `refresh()` 方法中，第一阶段末尾（约第 54 行后）

**原代码：**
```java
// 第一阶段：先实例化所有 BeanPostProcessor
for (String beanName : beanDefinitionMap.keySet()) {
    BeanDefinition beanDef = beanDefinitionMap.get(beanName);
    if (BeanPostProcessor.class.isAssignableFrom(beanDef.getBeanClass())) {
        BeanPostProcessor processor = (BeanPostProcessor) getBean(beanName);
        beanPostProcessors.add(processor);
        System.out.println("✅ 注册BeanPostProcessor：" + beanName);
    }
}
```

**改为：**
```java
// 第一阶段：先实例化所有 BeanPostProcessor
for (String beanName : beanDefinitionMap.keySet()) {
    BeanDefinition beanDef = beanDefinitionMap.get(beanName);
    if (BeanPostProcessor.class.isAssignableFrom(beanDef.getBeanClass())) {
        BeanPostProcessor processor = (BeanPostProcessor) getBean(beanName);
        beanPostProcessors.add(processor);
        System.out.println("✅ 注册BeanPostProcessor：" + beanName);
    }
}

// 按 @Order 排序（数字越小，优先级越高）
beanPostProcessors.sort((p1, p2) -> {
    int order1 = getOrder(p1);
    int order2 = getOrder(p2);
    return Integer.compare(order1, order2);
});
```

---

### 步骤 3：添加 getOrder() 辅助方法

在 ClassPathXmlApplicationContext 类末尾添加：

```java
/**
 * 获取 BeanPostProcessor 的 @Order 值
 */
private int getOrder(BeanPostProcessor processor) {
    Order order = processor.getClass().getAnnotation(Order.class);
    return order != null ? order.value() : Integer.MAX_VALUE;
}
```

**导入语句：**
```java
import com.wubai.summer.annotation.Order;
```

---

### 步骤 4：同样修改 AnnotationConfigApplicationContext

对 `AnnotationConfigApplicationContext.java` 做相同的修改：
1. 在 refresh() 方法的第一阶段末尾添加排序逻辑
2. 添加 getOrder() 辅助方法
3. 添加 import 语句

---

## 三、测试示例

### 创建多个 BeanPostProcessor

**文件 1：** `FirstProcessor.java`
```java
package com.wubai.summer.test.processor;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.Order;
import com.wubai.summer.core.BeanPostProcessor;

@Component
@Order(1)
public class FirstProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("1️⃣ FirstProcessor 处理：" + beanName);
        return bean;
    }
}
```

**文件 2：** `SecondProcessor.java`
```java
package com.wubai.summer.test.processor;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.Order;
import com.wubai.summer.core.BeanPostProcessor;

@Component
@Order(2)
public class SecondProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("2️⃣ SecondProcessor 处理：" + beanName);
        return bean;
    }
}
```

**文件 3：** `ThirdProcessor.java`（无 @Order）
```java
package com.wubai.summer.test.processor;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.core.BeanPostProcessor;

@Component
public class ThirdProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("3️⃣ ThirdProcessor 处理（无@Order）：" + beanName);
        return bean;
    }
}
```

---

### 预期输出

运行测试时，应该看到按顺序执行：

```
✅ 注册BeanPostProcessor：firstProcessor
✅ 注册BeanPostProcessor：secondProcessor
✅ 注册BeanPostProcessor：thirdProcessor

🔨 实例化Bean：userService
1️⃣ FirstProcessor 处理：userService
2️⃣ SecondProcessor 处理：userService
3️⃣ ThirdProcessor 处理（无@Order）：userService
```

---

## 四、进阶扩展

### 4.1 支持负数优先级

@Order 可以使用负数，表示更高的优先级：

```java
@Order(-1)  // 最高优先级
public class HighPriorityProcessor implements BeanPostProcessor { ... }
```

当前实现已经支持，无需修改。

### 4.2 支持 Ordered 接口

除了 @Order 注解，还可以实现 Ordered 接口：

```java
public interface Ordered {
    int getOrder();
}

@Component
public class MyProcessor implements BeanPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return 1;
    }
}
```

需要修改 getOrder() 方法：

```java
private int getOrder(BeanPostProcessor processor) {
    // 1. 优先使用 Ordered 接口
    if (processor instanceof Ordered) {
        return ((Ordered) processor).getOrder();
    }
    // 2. 其次使用 @Order 注解
    Order order = processor.getClass().getAnnotation(Order.class);
    return order != null ? order.value() : Integer.MAX_VALUE;
}
```

---

## 五、常见问题

### Q1: 为什么默认值是 Integer.MAX_VALUE？

这样未标注 @Order 的 BeanPostProcessor 会排在最后，符合直觉。

### Q2: 如果两个 BeanPostProcessor 的 @Order 值相同？

按照注册顺序执行（即扫描到的顺序）。

### Q3: @Order 可以用在普通 Bean 上吗？

可以，但目前只对 BeanPostProcessor 生效。如果需要对普通 Bean 排序，需要在其他地方实现排序逻辑。

---

## 六、总结

### 实现清单

1. ✅ 创建 @Order 注解
2. ✅ 在 refresh() 中添加排序逻辑
3. ✅ 添加 getOrder() 辅助方法
4. ✅ 两个容器都要修改
5. ✅ 创建测试验证

### 核心代码

```java
// 排序逻辑
beanPostProcessors.sort((p1, p2) -> {
    int order1 = getOrder(p1);
    int order2 = getOrder(p2);
    return Integer.compare(order1, order2);
});

// 获取顺序值
private int getOrder(BeanPostProcessor processor) {
    Order order = processor.getClass().getAnnotation(Order.class);
    return order != null ? order.value() : Integer.MAX_VALUE;
}
```

---

**祝你实现顺利！**
