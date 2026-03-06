# IoC 容器实现讲解大纲（5-10分钟）

## 开场（30秒）

"我实现了一个轻量级的 IoC 容器，支持 XML 和注解两种配置方式，包含依赖注入、生命周期管理、线程安全和扩展点设计等核心特性。接下来我从架构、核心流程和技术亮点三个方面介绍。"

---

## 一、整体架构（2分钟）

### 1.1 核心组件

**三个核心类：**
- **ApplicationContext**：容器入口，负责启动和管理
  - ClassPathXmlApplicationContext（XML配置）
  - AnnotationConfigApplicationContext（注解配置）

- **BeanDefinition**：Bean 的元数据
  - 存储 Bean 的类信息、构造器、依赖关系等

- **ResourceResolver**：资源扫描器
  - 扫描指定包下的所有类

**三个核心缓存：**
```java
beanDefinitionMap  // Bean名称 → BeanDefinition
singletonObjects   // Bean名称 → Bean实例（单例池）
creatingBeanNames  // 正在创建的Bean（循环依赖检测）
```

### 1.2 架构图

```
用户代码
   ↓
ApplicationContext（容器入口）
   ↓
1. 扫描/解析配置 → BeanDefinition
2. 实例化 Bean → 依赖注入
3. BeanPostProcessor 处理
4. 放入单例池
```

---

## 二、核心流程（4-5分钟）

### 2.1 容器启动流程

**三个阶段：**

**阶段1：扫描和注册（Scan & Register）**
```
XML方式：解析 beans.xml → 提取 <bean> 标签
注解方式：扫描包 → 找到 @Component/@Configuration 类
↓
创建 BeanDefinition 并注册到 beanDefinitionMap
```

**关键代码：**
```java
// 注解方式：包扫描
ResourceResolver resolver = new ResourceResolver(pkg);
List<String> classNames = resolver.scanClassNames();

// 解析类，创建 BeanDefinition
for (String className : classNames) {
    Class<?> clazz = Class.forName(className);
    if (clazz.isAnnotationPresent(Component.class)) {
        BeanDefinition beanDef = new BeanDefinition();
        beanDef.setBeanClass(clazz);
        beanDefinitionMap.put(beanName, beanDef);
    }
}
```

**阶段2：实例化和依赖注入（Instantiate & Inject）**
```
遍历 beanDefinitionMap
↓
调用 getBean(beanName) 触发实例化
↓
1. 选择构造器（优先 @Autowired 构造器）
2. 递归获取构造器参数（依赖的 Bean）
3. 反射调用构造器创建实例
4. Setter/字段注入（@Autowired）
```

**关键代码：**
```java
// 构造器注入
Constructor<?> constructor = selectConstructor(clazz);
Object[] params = resolveConstructorParams(constructor);
Object instance = constructor.newInstance(params);

// 字段注入
for (Field field : clazz.getDeclaredFields()) {
    if (field.isAnnotationPresent(Autowired.class)) {
        Object dependency = getBeanByType(field.getType());
        field.set(instance, dependency);
    }
}
```

**阶段3：后置处理和缓存（PostProcess & Cache）**
```
应用 BeanPostProcessor（初始化前）
↓
调用 init-method（如果有）
↓
应用 BeanPostProcessor（初始化后）← 可以替换为代理对象
↓
放入单例池 singletonObjects
```

### 2.2 依赖注入的三种方式

**1. 构造器注入（推荐）**
```java
@Component
public class UserService {
    private final OrderService orderService;

    @Autowired
    public UserService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```
- 优点：依赖不可变，强制注入
- 实现：反射调用构造器

**2. Setter 注入**
```java
@Autowired
public void setOrderService(OrderService orderService) {
    this.orderService = orderService;
}
```
- 优点：可选依赖
- 实现：反射调用 setter 方法

**3. 字段注入**
```java
@Autowired
private OrderService orderService;
```
- 优点：代码简洁
- 缺点：破坏封装性
- 实现：反射设置字段值

---

## 三、技术亮点（2-3分钟）

### 3.1 线程安全设计

**问题：** 多线程并发调用 getBean() 可能重复创建 Bean

**解决方案：双重检查锁定（DCL）**
```java
public Object getBean(String beanName) {
    // 第一次检查：无锁，快速返回
    Object bean = singletonObjects.get(beanName);
    if (bean != null) return bean;

    // 同步块：按 BeanDefinition 加锁（细粒度锁）
    synchronized (beanDef) {
        // 第二次检查：防止重复创建
        bean = singletonObjects.get(beanName);
        if (bean != null) return bean;

        // 创建 Bean...
    }
}
```

**关键点：**
- ConcurrentHashMap 保证读操作线程安全
- synchronized (beanDef) 保证创建操作原子性
- 不同 Bean 可以并发创建（细粒度锁）

### 3.2 BeanPostProcessor 扩展点

**设计思想：** 开闭原则，允许在不修改容器代码的情况下扩展功能

**应用场景：**
- AOP 动态代理
- 属性值替换
- Bean 验证

**实现：**
```java
// 接口定义
public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Object bean, String beanName);
    Object postProcessAfterInitialization(Object bean, String beanName);
}

// 容器调用
Object wrappedBean = bean;
for (BeanPostProcessor processor : beanPostProcessors) {
    wrappedBean = processor.postProcessAfterInitialization(wrappedBean, beanName);
}
singletonObjects.put(beanName, wrappedBean);  // 放入处理后的 Bean
```

**示例：动态代理**
```java
@Component
public class ProxyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().getSimpleName().endsWith("Service")) {
            return Proxy.newProxyInstance(...);  // 返回代理对象
        }
        return bean;
    }
}
```

### 3.3 循环依赖检测

**问题：** A 依赖 B，B 依赖 A（构造器注入）

**解决方案：**
```java
Set<String> creatingBeanNames = ConcurrentHashMap.newKeySet();

if (!creatingBeanNames.add(beanName)) {
    throw new RuntimeException("检测到循环依赖：" + beanName);
}
```

**说明：**
- 当前只支持检测，不支持解决
- Spring 的三级缓存可以解决 Setter 注入的循环依赖
- 构造器注入的循环依赖无法解决（设计问题）

### 3.4 @Order 排序支持

**功能：** 控制 BeanPostProcessor 的执行顺序

**实现：**
```java
beanPostProcessors.sort((p1, p2) -> {
    int order1 = getOrder(p1);  // 读取 @Order 注解
    int order2 = getOrder(p2);
    return Integer.compare(order1, order2);  // 数字越小，优先级越高
});
```

---

## 四、难点和解决方案（1分钟）

### 4.1 类型匹配问题

**问题：** 按类型注入时，如何处理接口和实现类？

**解决：**
```java
if (type.isAssignableFrom(beanClass)) {
    // 支持接口/父类注入
}
```

### 4.2 工厂方法支持

**问题：** @Bean 方法如何实例化？

**解决：**
```java
Object factoryBean = getBean(factoryBeanName);  // 获取配置类实例
Object instance = factoryMethod.invoke(factoryBean);  // 调用 @Bean 方法
```

### 4.3 XML 和注解的统一

**问题：** 两种配置方式如何复用代码？

**解决：**
- 统一使用 BeanDefinition 作为中间表示
- XML 解析器和注解扫描器都生成 BeanDefinition
- 后续流程完全一致

---

## 五、总结（30秒）

**核心价值：**
1. **完整性** - 支持 XML 和注解两种方式，覆盖主流使用场景
2. **扩展性** - BeanPostProcessor 提供了强大的扩展能力
3. **工程化** - 考虑了线程安全、性能优化等实际问题

**和 Spring 的对比：**
- 核心流程一致：扫描 → 注册 → 实例化 → 注入 → 缓存
- 简化了部分高级特性（作用域、AOP、三级缓存等）
- 代码量约 1000 行，易于理解和学习

**未来扩展方向：**
- 三级缓存解决循环依赖
- AOP 切面支持
- @Value 属性注入

---

## 六、常见面试问题准备

**Q1: 为什么用 ConcurrentHashMap？**
A: 支持高并发读，避免锁竞争，提升性能。

**Q2: 双重检查锁定为什么要检查两次？**
A: 第一次无锁快速返回，第二次在锁内防止重复创建。

**Q3: BeanPostProcessor 的应用场景？**
A: AOP 动态代理、属性替换、Bean 验证等。

**Q4: 如何解决循环依赖？**
A: 当前只检测不解决。Spring 用三级缓存解决 Setter 注入的循环依赖。

**Q5: 构造器注入和 Setter 注入的区别？**
A: 构造器注入依赖不可变，强制注入；Setter 注入支持可选依赖。

---

## 讲解技巧

1. **控制时间** - 根据面试官反应调整深度
2. **画图辅助** - 画出核心流程图和架构图
3. **举例说明** - 用具体代码片段说明关键点
4. **对比 Spring** - 体现你对 Spring 的理解
5. **准备追问** - 提前想好可能的追问和答案
