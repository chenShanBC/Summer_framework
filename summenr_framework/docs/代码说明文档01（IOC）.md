# Summer Framework 代码说明文档

## 一、项目概述

### 1.1 项目简介

Summer Framework 是一个轻量级的 IoC（控制反转）容器框架，参考 Spring Framework 的核心设计思想实现。项目代码约 1000 行，包含了依赖注入、生命周期管理、扩展机制等核心功能。

### 1.2 核心特性

- **依赖注入**：支持构造器、Setter、字段三种注入方式
- **配置方式**：支持 XML 和注解两种配置方式
- **生命周期管理**：完整的 Bean 生命周期管理（扫描、注册、实例化、注入、后置处理）
- **线程安全**：采用 ConcurrentHashMap + 双重检查锁定保证多线程安全
- **扩展机制**：BeanPostProcessor 扩展点支持 Bean 动态增强
- **循环依赖检测**：检测构造器注入的循环依赖

### 1.3 技术栈

- Java 21
- Maven
- Java 反射 API
- 注解处理
- 并发编程（ConcurrentHashMap、synchronized）

---

## 二、项目结构

```
src/main/java/com/wubai/summer/
├── annotation/              # 注解定义
│   ├── Autowired.java      # 依赖注入注解
│   ├── Component.java      # 组件注解
│   ├── Configuration.java  # 配置类注解
│   ├── Bean.java           # Bean 方法注解
│   ├── ComponentScan.java  # 包扫描注解
│   └── Order.java          # 排序注解
├── core/                    # 核心容器
│   ├── AnnotationConfigApplicationContext.java  # 注解配置容器
│   ├── ClassPathXmlApplicationContext.java      # XML 配置容器
│   ├── BeanDefinition.java                      # Bean 定义
│   ├── BeanPostProcessor.java                   # Bean 后置处理器接口
│   └── ResourceResolver.java                    # 资源扫描器
├── test/                    # 测试代码
│   ├── Services/           # 测试服务类
│   ├── pojo/               # 测试实体类
│   ├── processor/          # 测试 BeanPostProcessor
│   └── *Test.java          # 测试类
└── resources/
    └── beans.xml           # XML 配置文件
```

---

## 三、核心架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    ApplicationContext                    │
│  (AnnotationConfigApplicationContext / ClassPathXml...) │
└─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ BeanDefinition│   │ResourceResolver│   │BeanPostProcessor│
│   (Bean元数据) │   │  (包扫描器)   │   │  (扩展点)    │
└──────────────┘   └──────────────┘   └──────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│                    三个核心缓存                          │
│  beanDefinitionMap  │ singletonObjects │ creatingBeanNames│
│   (Bean定义映射)     │   (单例池)       │  (创建中标记)    │
└─────────────────────────────────────────────────────────┘
```

### 3.2 核心流程

```
1. 容器启动
   ├─ 扫描/解析配置 (XML 或注解)
   ├─ 注册 BeanDefinition
   └─ 调用 refresh()

2. refresh() 刷新容器
   ├─ 第一阶段：实例化所有 BeanPostProcessor
   └─ 第二阶段：实例化其他普通 Bean

3. getBean(beanName) 获取 Bean
   ├─ 检查单例池 (第一次检查)
   ├─ 加锁 (synchronized)
   ├─ 检查单例池 (第二次检查)
   ├─ 实例化 Bean
   │  ├─ 选择构造器
   │  ├─ 解析构造器参数 (递归获取依赖)
   │  └─ 反射创建实例
   ├─ 依赖注入 (Setter/字段)
   ├─ BeanPostProcessor 前置处理
   ├─ BeanPostProcessor 后置处理
   ├─ 放入单例池
   └─ 返回 Bean
```

---

## 四、核心类详解

### 4.1 BeanDefinition（Bean 定义）

**作用：** 存储 Bean 的元数据信息

**核心字段：**
```java
public class BeanDefinition {
    private String beanName;              // Bean 名称
    private Class<?> beanClass;           // Bean 类型
    private Constructor<?> constructor;   // 构造器
    private List<String> constructorArgRefs;  // 构造器参数引用
    private Map<String, String> propertyRefs; // 属性引用
    private String factoryBeanName;       // 工厂 Bean 名称
    private Method factoryMethod;         // 工厂方法
    private Object instance;              // Bean 实例
}
```

**关键点：**
- 统一表示 XML 和注解配置的 Bean
- 区分普通 Bean（@Component）和工厂 Bean（@Bean）
- 存储依赖关系，用于依赖注入

---

### 4.2 ApplicationContext（应用上下文）

#### 4.2.1 AnnotationConfigApplicationContext

**作用：** 基于注解配置的 IoC 容器

**核心字段：**
```java
// 三个核心缓存
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
private final Set<String> creatingBeanNames = ConcurrentHashMap.newKeySet();

// BeanPostProcessor 列表
private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
```

**核心方法：**
```java
// 构造器：启动容器
public AnnotationConfigApplicationContext(Class<?> configClass)

// 包扫描
private List<String> scan(Class<?> configClass)

// 注册 BeanDefinition
private void registerBeanDefinitions(List<String> classNames)

// 刷新容器
private void refresh()

// 获取 Bean
public Object getBean(String beanName)
public <T> T getBeanByType(Class<T> type)
```

**启动流程：**
```java
public AnnotationConfigApplicationContext(Class<?> configClass) {
    // 1. 扫描包，获取所有类名
    List<String> classNames = scan(configClass);

    // 2. 解析类，注册 BeanDefinition
    registerBeanDefinitions(classNames);

    // 3. 实例化所有单例 Bean
    refresh();
}
```

#### 4.2.2 ClassPathXmlApplicationContext

**作用：** 基于 XML 配置的 IoC 容器

**核心方法：**
```java
// 构造器：加载 XML 配置
public ClassPathXmlApplicationContext(String configLocation)

// 解析 XML
private void parseXml(String configLocation)

// 其他方法与 AnnotationConfigApplicationContext 类似
```

**XML 解析流程：**
```java
private void parseXml(String configLocation) {
    // 1. 加载 XML 文件
    Document doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(inputStream);

    // 2. 遍历 <bean> 标签
    NodeList beanNodes = doc.getElementsByTagName("bean");
    for (int i = 0; i < beanNodes.getLength(); i++) {
        Element beanElement = (Element) beanNodes.item(i);

        // 3. 提取属性
        String id = beanElement.getAttribute("id");
        String className = beanElement.getAttribute("class");

        // 4. 创建 BeanDefinition
        BeanDefinition beanDef = new BeanDefinition();
        beanDef.setBeanName(id);
        beanDef.setBeanClass(Class.forName(className));

        // 5. 解析 <constructor-arg> 和 <property>
        // ...

        // 6. 注册
        beanDefinitionMap.put(id, beanDef);
    }
}
```

---

### 4.3 ResourceResolver（资源扫描器）

**作用：** 扫描指定包下的所有类

**核心方法：**
```java
public List<String> scanClassNames()
```

**实现原理：**
```java
public List<String> scanClassNames() {
    // 1. 将包名转换为路径 (com.wubai.summer → com/wubai/summer)
    String path = packageName.replace('.', '/');

    // 2. 获取类加载器
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // 3. 获取资源 URL
    Enumeration<URL> resources = classLoader.getResources(path);

    // 4. 遍历目录，找到所有 .class 文件
    while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        File dir = new File(url.toURI());

        // 递归扫描
        scanDirectory(dir, packageName, classNames);
    }

    return classNames;
}
```

---

### 4.4 BeanPostProcessor（Bean 后置处理器）

**作用：** 在 Bean 初始化前后插入自定义逻辑

**接口定义：**
```java
public interface BeanPostProcessor {
    // 初始化前调用
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    // 初始化后调用（常用于创建代理）
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

**调用时机：**
```java
// 在 getBean() 方法中
Object instance = createInstance();           // 1. 实例化
autowireBean(instance);                       // 2. 依赖注入

Object wrappedBean = instance;
wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);  // 3. 前置处理
// 这里可以调用 init-method
wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);   // 4. 后置处理

singletonObjects.put(beanName, wrappedBean);  // 5. 放入单例池
```

**典型应用：动态代理**
```java
@Component
public class ProxyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().getSimpleName().endsWith("Service")) {
            // 为 Service 类创建代理
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    System.out.println("方法调用前：" + method.getName());
                    Object result = method.invoke(bean, args);
                    System.out.println("方法调用后");
                    return result;
                }
            );
        }
        return bean;
    }
}
```

---

## 五、核心流程详解

### 5.1 容器启动流程

**注解配置方式：**
```java
// 1. 创建容器
AnnotationConfigApplicationContext context =
    new AnnotationConfigApplicationContext(AppConfig.class);

// 内部执行流程：
// ├─ scan(AppConfig.class)
// │  ├─ 解析 @ComponentScan 注解
// │  ├─ 调用 ResourceResolver 扫描包
// │  └─ 返回所有类名列表
// │
// ├─ registerBeanDefinitions(classNames)
// │  ├─ 遍历所有类
// │  ├─ 检查是否有 @Component/@Configuration
// │  ├─ 创建 BeanDefinition
// │  ├─ 如果是 @Configuration，解析 @Bean 方法
// │  └─ 注册到 beanDefinitionMap
// │
// └─ refresh()
//    ├─ 第一阶段：实例化所有 BeanPostProcessor
//    │  ├─ 遍历 beanDefinitionMap
//    │  ├─ 找到实现 BeanPostProcessor 的类
//    │  ├─ 调用 getBean() 实例化
//    │  ├─ 添加到 beanPostProcessors 列表
//    │  └─ 按 @Order 排序
//    │
//    └─ 第二阶段：实例化其他普通 Bean
//       └─ 遍历 beanDefinitionMap，调用 getBean()
```

**XML 配置方式：**
```java
// 1. 创建容器
ClassPathXmlApplicationContext context =
    new ClassPathXmlApplicationContext("beans.xml");

// 内部执行流程：
// ├─ parseXml("beans.xml")
// │  ├─ 加载 XML 文件
// │  ├─ 解析 <bean> 标签
// │  ├─ 提取 id、class、constructor-arg、property
// │  ├─ 创建 BeanDefinition
// │  └─ 注册到 beanDefinitionMap
// │
// └─ refresh()
//    └─ (同注解方式)
```

---

### 5.2 依赖注入流程

#### 5.2.1 构造器注入

**示例代码：**
```java
@Component
public class OrderService {
    private final UserService userService;

    @Autowired
    public OrderService(UserService userService) {
        this.userService = userService;
    }
}
```

**注入流程：**
```java
// 1. 选择构造器
Constructor<?> constructor = selectConstructor(OrderService.class);
// 返回：OrderService(UserService)

// 2. 解析构造器参数
Class<?>[] paramTypes = constructor.getParameterTypes();
// paramTypes = [UserService.class]

Object[] params = new Object[paramTypes.length];
for (int i = 0; i < paramTypes.length; i++) {
    // 递归获取依赖的 Bean
    params[i] = getBeanByType(paramTypes[i]);
    // params[0] = getBean("userService")
}

// 3. 反射创建实例
Object instance = constructor.newInstance(params);
// 相当于：new OrderService(userService)
```

**循环依赖检测：**
```java
// 在 getBean() 方法中
if (!creatingBeanNames.add(beanName)) {
    throw new RuntimeException("检测到循环依赖：" + beanName);
}

// 示例：A 依赖 B，B 依赖 A
// getBean("A") → creatingBeanNames.add("A") → 创建 A
//   → 需要 B → getBean("B") → creatingBeanNames.add("B") → 创建 B
//     → 需要 A → getBean("A") → creatingBeanNames.add("A") 失败！
//       → 抛出异常
```

#### 5.2.2 Setter 注入

**示例代码：**
```java
@Component
public class UserService {
    private User user;

    @Autowired
    public void setUser(User user) {
        this.user = user;
    }
}
```

**注入流程：**
```java
// 1. 遍历所有方法
for (Method method : clazz.getMethods()) {
    // 2. 检查是否有 @Autowired
    if (method.isAnnotationPresent(Autowired.class)) {
        // 3. 检查是否是 setter 方法
        if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
            // 4. 获取参数类型
            Class<?> paramType = method.getParameterTypes()[0];

            // 5. 获取依赖的 Bean
            Object dependency = getBeanByType(paramType);

            // 6. 反射调用 setter
            method.invoke(instance, dependency);
            // 相当于：userService.setUser(user)
        }
    }
}
```

#### 5.2.3 字段注入

**示例代码：**
```java
@Component
public class UserService {
    @Autowired
    private User user;
}
```

**注入流程：**
```java
// 1. 遍历所有字段
for (Field field : clazz.getDeclaredFields()) {
    // 2. 检查是否有 @Autowired
    if (field.isAnnotationPresent(Autowired.class)) {
        // 3. 获取字段类型
        Class<?> fieldType = field.getType();

        // 4. 获取依赖的 Bean
        Object dependency = getBeanByType(fieldType);

        // 5. 设置可访问（绕过 private）
        field.setAccessible(true);

        // 6. 反射设置字段值
        field.set(instance, dependency);
        // 相当于：userService.user = user
    }
}
```

---

### 5.3 线程安全机制

#### 5.3.1 问题分析

**场景：** 多个线程同时调用 `getBean("userService")`

**不加锁的问题：**
```java
// 线程 A 和线程 B 同时执行
public Object getBean(String beanName) {
    if (singletonObjects.containsKey(beanName)) {
        return singletonObjects.get(beanName);
    }

    // 线程 A 和 B 都发现单例池里没有
    // 都会创建 Bean，导致重复创建！
    Object instance = createBean(beanName);
    singletonObjects.put(beanName, instance);
    return instance;
}
```

#### 5.3.2 解决方案：双重检查锁定（DCL）

```java
public Object getBean(String beanName) {
    // 第一次检查：无锁，快速返回（大多数情况）
    Object bean = singletonObjects.get(beanName);
    if (bean != null) {
        return bean;
    }

    BeanDefinition beanDef = beanDefinitionMap.get(beanName);
    if (beanDef == null) {
        throw new RuntimeException("Bean不存在：" + beanName);
    }

    // 同步块：按 BeanDefinition 加锁（细粒度锁）
    synchronized (beanDef) {
        // 第二次检查：防止重复创建
        bean = singletonObjects.get(beanName);
        if (bean != null) {
            return bean;  // 其他线程已经创建了
        }

        // 创建 Bean（只有一个线程能执行到这里）
        Object instance = createBean(beanName);
        singletonObjects.put(beanName, instance);
        return instance;
    }
}
```

**关键点：**
1. **第一次检查**：无锁，快速返回已存在的 Bean（性能优化）
2. **synchronized (beanDef)**：细粒度锁，不同 Bean 可以并发创建
3. **第二次检查**：防止多个线程同时进入同步块后重复创建

**为什么用 ConcurrentHashMap？**
- 支持高并发读操作
- 单个操作（get/put）是原子的
- 但多步操作（check-then-act）仍需要加锁

---

## 六、使用示例

### 6.1 注解配置方式

**步骤 1：创建配置类**
```java
@Configuration
@ComponentScan("com.wubai.summer.test")
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        return new DataSource();
    }
}
```

**步骤 2：创建组件**
```java
@Component
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
```

**步骤 3：启动容器**
```java
public class Main {
    public static void main(String[] args) {
        // 创建容器
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取 Bean
        UserService userService = context.getBeanByType(UserService.class);

        // 使用 Bean
        userService.saveUser(new User("张三"));
    }
}
```

### 6.2 XML 配置方式

**步骤 1：创建 beans.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean id="user" class="com.wubai.summer.test.pojo.User"/>

    <bean id="userService" class="com.wubai.summer.test.Services.UserService">
        <property name="user" ref="user"/>
    </bean>

    <bean id="orderService" class="com.wubai.summer.test.Services.OrderService">
        <constructor-arg ref="userService"/>
        <property name="user" ref="user"/>
    </bean>
</beans>
```

**步骤 2：启动容器**
```java
public class Main {
    public static void main(String[] args) {
        // 创建容器
        ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("beans.xml");

        // 获取 Bean
        UserService userService = (UserService) context.getBean("userService");

        // 使用 Bean
        userService.sayHello();
    }
}
```

---

## 七、扩展示例

### 7.1 自定义 BeanPostProcessor

**示例：为所有 Service 添加日志**
```java
@Component
@Order(1)
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().getSimpleName().endsWith("Service")) {
            System.out.println("✅ Service Bean 创建完成：" + beanName);
        }
        return bean;
    }
}
```

### 7.2 动态代理示例

**示例：为 Service 添加性能监控**
```java
@Component
@Order(2)
public class PerformanceBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().getSimpleName().endsWith("Service")) {
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        long start = System.currentTimeMillis();
                        Object result = method.invoke(bean, args);
                        long end = System.currentTimeMillis();
                        System.out.println("⏱️ " + method.getName() +
                            " 耗时：" + (end - start) + "ms");
                        return result;
                    }
                }
            );
        }
        return bean;
    }
}
```

---

## 八、常见问题

### 8.1 为什么默认是单例？

**原因：**
1. 大多数 Bean 是无状态的（Service、DAO、Controller）
2. 节省内存和创建开销
3. 简化依赖管理

**注意：** 单例 Bean 的线程安全由开发者保证（避免可变的实例变量）

### 8.2 循环依赖如何解决？

**当前实现：** 只检测，不解决（抛出异常）

**Spring 的解决方案：** 三级缓存
- 一级缓存：完整的 Bean
- 二级缓存：半成品 Bean（已实例化，未注入）
- 三级缓存：Bean 工厂

**限制：** 只能解决 Setter 注入的循环依赖，构造器注入无法解决

### 8.3 如何支持原型模式？

**需要修改：**
1. 添加 @Scope 注解
2. BeanDefinition 添加 scope 字段
3. getBean() 中判断 scope，如果是 prototype 则每次创建新实例

---

## 九、与 Spring 的对比

| 特性 | Summer Framework | Spring Framework |
|------|-----------------|------------------|
| 依赖注入 | ✅ 三种方式 | ✅ 三种方式 |
| 配置方式 | ✅ XML + 注解 | ✅ XML + 注解 + Java Config |
| 单例模式 | ✅ 支持 | ✅ 支持 |
| 原型模式 | ❌ 不支持 | ✅ 支持 |
| 循环依赖 | ⚠️ 只检测 | ✅ 三级缓存解决 |
| BeanPostProcessor | ✅ 支持 | ✅ 支持 |
| AOP | ❌ 不支持 | ✅ 支持 |
| 生命周期回调 | ❌ 不支持 | ✅ @PostConstruct/@PreDestroy |
| 条件装配 | ❌ 不支持 | ✅ @Conditional |
| 属性注入 | ❌ 不支持 | ✅ @Value |
| 线程安全 | ✅ DCL | ✅ DCL |

---

## 十、总结

### 10.1 核心价值

1. **完整性**：实现了 IoC 容器的核心功能
2. **扩展性**：BeanPostProcessor 提供了强大的扩展能力
3. **工程化**：考虑了线程安全、性能优化等实际问题
4. **可读性**：代码结构清晰，易于理解和学习

### 10.2 学习收获

通过实现这个项目，可以深入理解：
- 依赖注入的实现原理
- Java 反射和注解的应用
- 并发编程和线程安全
- 设计模式（工厂、单例、代理）
- 框架设计思想

### 10.3 未来扩展方向

1. **三级缓存**：解决 Setter 注入的循环依赖
2. **AOP 支持**：实现切面编程
3. **作用域支持**：原型、请求、会话等
4. **生命周期回调**：@PostConstruct/@PreDestroy
5. **属性注入**：@Value 和占位符替换
6. **条件装配**：@Conditional 注解

---

**文档版本：** 1.0
**最后更新：** 2026-03-06
